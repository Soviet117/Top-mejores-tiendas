import { Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { sendVerificationCode as sendEmail } from '../services/emailService';

const prisma = new PrismaClient();

// ─── Esquemas de Validación ────────────────────────────────
const RegisterSchema = z.object({
  nombreCompleto: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  email: z.string().email('Email inválido'),
  contrasena: z.string().min(6, 'La contraseña debe tener al menos 6 caracteres'),
  telefono: z.string().optional(),
  esDuenio: z.boolean().optional().default(false),
  ruc: z.string().optional(),
  razonSocial: z.string().optional(),
  fotoPerfil: z.string().optional(), // URL o base64
});

const LoginSchema = z.object({
  email: z.string().email('Email inválido'),
  contrasena: z.string().min(1, 'La contraseña es requerida'),
});

// ─── Generar JWT ────────────────────────────────────────────
const generateToken = (user: { id: number; email: string; esDuenio: boolean }) => {
  return jwt.sign(
    { id: user.id, email: user.email, esDuenio: user.esDuenio },
    process.env.JWT_SECRET!,
    { expiresIn: '30d' }
  );
};

// ─── POST /api/auth/register ────────────────────────────────
export const register = async (req: Request, res: Response): Promise<void> => {
  try {
    const validatedData = RegisterSchema.parse(req.body);

    // Verificar si el email ya existe
    const existingUser = await prisma.usuario.findUnique({
      where: { email: validatedData.email },
    });

    if (existingUser) {
      res.status(409).json({ error: 'El email ya está registrado' });
      return;
    }

    // Hashear contraseña
    const hashedPassword = await bcrypt.hash(validatedData.contrasena, 12);

    // Crear usuario
    const user = await prisma.usuario.create({
      data: {
        nombreCompleto: validatedData.nombreCompleto,
        email: validatedData.email,
        contrasena: hashedPassword,
        telefono: validatedData.telefono,
        esDuenio: validatedData.esDuenio ?? false,
        ruc: validatedData.ruc,
        razonSocial: validatedData.razonSocial,
        fotoPerfil: validatedData.fotoPerfil,
        emailVerificado: true, // Simplificado para el proyecto; en producción se enviaría email
      },
      select: {
        id: true,
        nombreCompleto: true,
        email: true,
        telefono: true,
        fotoPerfil: true,
        esDuenio: true,
        ruc: true,
        razonSocial: true,
        emailVerificado: true,
        createdAt: true,
      },
    });

    const token = generateToken({ id: user.id, email: user.email, esDuenio: user.esDuenio });

    res.status(201).json({
      message: 'Usuario registrado exitosamente',
      token,
      user,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[AUTH] register error:', error);
    res.status(500).json({ error: 'Error al registrar el usuario' });
  }
};

// ─── POST /api/auth/login ───────────────────────────────────
export const login = async (req: Request, res: Response): Promise<void> => {
  try {
    const { email, contrasena } = LoginSchema.parse(req.body);

    // Buscar usuario por email
    const user = await prisma.usuario.findUnique({
      where: { email },
    });

    if (!user) {
      res.status(401).json({ error: 'Credenciales incorrectas' });
      return;
    }

    // Verificar contraseña
    const passwordValid = await bcrypt.compare(contrasena, user.contrasena);
    if (!passwordValid) {
      res.status(401).json({ error: 'Credenciales incorrectas' });
      return;
    }

    const token = generateToken({ id: user.id, email: user.email, esDuenio: user.esDuenio });

    res.status(200).json({
      message: 'Inicio de sesión exitoso',
      token,
      user: {
        id: user.id,
        nombreCompleto: user.nombreCompleto,
        email: user.email,
        telefono: user.telefono,
        fotoPerfil: user.fotoPerfil,
        esDuenio: user.esDuenio,
        ruc: user.ruc,
        razonSocial: user.razonSocial,
        emailVerificado: user.emailVerificado,
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[AUTH] login error:', error);
    res.status(500).json({ error: 'Error al iniciar sesión' });
  }
};

// ─── GET /api/auth/me ───────────────────────────────────────
export const getMe = async (req: Request & { user?: { id: number } }, res: Response): Promise<void> => {
  try {
    const user = await prisma.usuario.findUnique({
      where: { id: req.user!.id },
      select: {
        id: true,
        nombreCompleto: true,
        email: true,
        telefono: true,
        fotoPerfil: true,
        esDuenio: true,
        ruc: true,
        razonSocial: true,
        emailVerificado: true,
        createdAt: true,
      },
    });

    if (!user) {
      res.status(404).json({ error: 'Usuario no encontrado' });
      return;
    }

    res.status(200).json({ user });
  } catch (error) {
    console.error('[AUTH] getMe error:', error);
    res.status(500).json({ error: 'Error al obtener el perfil' });
  }
};

// ─── PUT /api/auth/profile ──────────────────────────────────
export const updateProfile = async (req: Request & { user?: { id: number } }, res: Response): Promise<void> => {
  try {
    const { nombreCompleto, telefono, fotoPerfil, ruc } = req.body;

    const user = await prisma.usuario.update({
      where: { id: req.user!.id },
      data: {
        ...(nombreCompleto && { nombreCompleto }),
        ...(telefono !== undefined && { telefono }),
        ...(fotoPerfil !== undefined && { fotoPerfil }),
        ...(ruc !== undefined && { ruc }),
      },
      select: {
        id: true, nombreCompleto: true, email: true, telefono: true,
        fotoPerfil: true, esDuenio: true, ruc: true, razonSocial: true, emailVerificado: true,
      },
    });

    res.status(200).json({ message: 'Perfil actualizado', user });
  } catch (error) {
    console.error('[AUTH] updateProfile error:', error);
    res.status(500).json({ error: 'Error al actualizar el perfil' });
  }
};

// ─── PUT /api/auth/password ─────────────────────────────────
export const updatePassword = async (req: Request & { user?: { id: number } }, res: Response): Promise<void> => {
  try {
    const { currentPassword, newPassword } = req.body;
    if (!currentPassword || !newPassword) {
      res.status(400).json({ error: 'Se requiere contraseña actual y nueva' });
      return;
    }

    const user = await prisma.usuario.findUnique({ where: { id: req.user!.id } });
    if (!user) { res.status(404).json({ error: 'Usuario no encontrado' }); return; }

    const valid = await bcrypt.compare(currentPassword, user.contrasena);
    if (!valid) { res.status(401).json({ error: 'Contraseña actual incorrecta' }); return; }

    const hashed = await bcrypt.hash(newPassword, 12);
    await prisma.usuario.update({ where: { id: user.id }, data: { contrasena: hashed } });

    res.status(200).json({ message: 'Contraseña actualizada' });
  } catch (error) {
    console.error('[AUTH] updatePassword error:', error);
    res.status(500).json({ error: 'Error al cambiar la contraseña' });
  }
};

// ─── DELETE /api/auth/account ───────────────────────────────
export const deleteAccount = async (req: Request & { user?: { id: number } }, res: Response): Promise<void> => {
  try {
    const { password } = req.body;
    if (!password) { res.status(400).json({ error: 'Se requiere la contraseña' }); return; }

    const user = await prisma.usuario.findUnique({ where: { id: req.user!.id } });
    if (!user) { res.status(404).json({ error: 'Usuario no encontrado' }); return; }

    const valid = await bcrypt.compare(password, user.contrasena);
    if (!valid) { res.status(401).json({ error: 'Contraseña incorrecta' }); return; }

    // Cascada automática por las FK ON DELETE CASCADE
    await prisma.usuario.delete({ where: { id: user.id } });

    res.status(200).json({ message: 'Cuenta eliminada permanentemente' });
  } catch (error) {
    console.error('[AUTH] deleteAccount error:', error);
    res.status(500).json({ error: 'Error al eliminar la cuenta' });
  }
};

// ─── POST /api/auth/send-verification-code ────────────────────
const SendCodeSchema = z.object({
  email: z.string().email('Email inv\u00e1lido'),
});

export const sendVerificationCodeHandler = async (req: Request, res: Response): Promise<void> => {
  try {
    const { email } = SendCodeSchema.parse(req.body);

    const otpCode = await sendEmail(email);

    res.status(200).json({
      message: 'C\u00f3digo enviado a tu correo',
      otpCode,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inv\u00e1lidos', details: error.errors });
      return;
    }
    console.error('[AUTH] sendVerificationCode error:', error);
    res.status(500).json({ error: 'Error al enviar el c\u00f3digo de verificaci\u00f3n' });
  }
};
