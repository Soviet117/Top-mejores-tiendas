import { Response } from 'express';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middleware/auth';

const prisma = new PrismaClient();

const CreateResenaSchema = z.object({
  idNegocio: z.number().int().positive(),
  calificacion: z.number().int().min(1).max(5),
  calidadAtencion: z.number().int().min(1).max(5),
  calidadProductos: z.number().int().min(1).max(5),
  costos: z.number().int().min(1).max(5),
  comentario: z.string().optional(),
  qrToken: z.string().uuid(), // Token QR requerido para crear reseña
});

const VerifyQrSchema = z.object({
  qrToken: z.string().uuid(),
});

const UpdateResenaSchema = z.object({
  calificacion: z.number().int().min(1).max(5),
  calidadAtencion: z.number().int().min(1).max(5),
  calidadProductos: z.number().int().min(1).max(5),
  costos: z.number().int().min(1).max(5),
  comentario: z.string().optional(),
  qrToken: z.string().uuid(),
});

const RespuestaDuenioSchema = z.object({
  respuestaDuenio: z.string().min(1),
});

// Helper para recalcular calificación promedio del negocio
const recalcularCalificacion = async (idNegocio: number) => {
  const resenas = await prisma.resena.findMany({
    where: { idNegocio },
    select: { calificacion: true },
  });

  const promedio = resenas.length > 0
    ? resenas.reduce((acc, r) => acc + r.calificacion, 0) / resenas.length
    : 0;

  await prisma.negocio.update({
    where: { id: idNegocio },
    data: { calificacionPromedio: Math.round(promedio * 10) / 10 },
  });
};

// ─── GET /api/resenas?negocioId=X ───────────────────────────
export const getResenas = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const negocioId = req.query.negocioId ? parseInt(String(req.query.negocioId)) : undefined;

    const resenas = await prisma.resena.findMany({
      where: negocioId ? { idNegocio: negocioId } : undefined,
      include: {
        usuario: {
          select: { id: true, nombreCompleto: true, fotoPerfil: true },
        },
      },
      orderBy: { fecha: 'desc' },
    });

    res.status(200).json({ resenas });
  } catch (error) {
    console.error('[RESENAS] getResenas error:', error);
    res.status(500).json({ error: 'Error al obtener las reseñas' });
  }
};

// ─── GET /api/resenas/mias ──────────────────────────────────
export const getMisResenas = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const resenas = await prisma.resena.findMany({
      where: { idUsuario: req.user!.id },
      include: {
        negocio: {
          select: { id: true, nombreNegocio: true, rubro: true, fotoNegocio: true },
        },
      },
      orderBy: { fecha: 'desc' },
    });

    res.status(200).json({ resenas });
  } catch (error) {
    console.error('[RESENAS] getMisResenas error:', error);
    res.status(500).json({ error: 'Error al obtener tus reseñas' });
  }
};

// ─── POST /api/resenas/verify-qr ─────────────────────────────
// Verifica si un token QR es válido y registra el acceso
export const verifyQr = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { qrToken } = VerifyQrSchema.parse(req.body);

    // Buscar el negocio por qrToken
    const negocio = await prisma.negocio.findUnique({
      where: { qrToken },
      select: { id: true, nombreNegocio: true },
    });

    if (!negocio) {
      res.status(404).json({ error: 'QR inválido o negocio no encontrado' });
      return;
    }

    // Registrar el escaneo (o actualizar si ya existía)
    const now = new Date();
    const twentyFourHoursAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

    // Verificar si ya tiene acceso activo
    const existingAccess = await prisma.qrAccess.findUnique({
      where: { idUsuario_idNegocio: { idUsuario: req.user!.id, idNegocio: negocio.id } },
    });

    if (existingAccess && existingAccess.scannedAt > twentyFourHoursAgo) {
      res.status(200).json({
        autorizado: true,
        negocioId: negocio.id,
        nombreNegocio: negocio.nombreNegocio,
        qrToken,
        mensaje: 'Ya tienes acceso activo para dejar reseñas',
      });
      return;
    }

    // Crear o actualizar el registro de acceso
    if (existingAccess) {
      await prisma.qrAccess.update({
        where: { id: existingAccess.id },
        data: { scannedAt: now },
      });
    } else {
      await prisma.qrAccess.create({
        data: {
          idUsuario: req.user!.id,
          idNegocio: negocio.id,
          scannedAt: now,
        },
      });
    }

    res.status(200).json({
      autorizado: true,
      negocioId: negocio.id,
      nombreNegocio: negocio.nombreNegocio,
      qrToken,
      mensaje: 'Acceso habilitado por 24 horas',
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Token QR inválido', details: error.errors });
      return;
    }
    console.error('[RESENAS] verifyQr error:', error);
    res.status(500).json({ error: 'Error al verificar el QR' });
  }
};

// ─── POST /api/resenas ───────────────────────────────────────
export const createResena = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const data = CreateResenaSchema.parse(req.body);

    // Validar que el qrToken pertenezca al negocio que se quiere reseñar
    const negocio = await prisma.negocio.findUnique({
      where: { id: data.idNegocio },
      select: { qrToken: true },
    });

    if (!negocio || negocio.qrToken !== data.qrToken) {
      res.status(403).json({ error: 'Token QR inválido para este negocio' });
      return;
    }

    // Verificar que el usuario tenga acceso activo (últimas 24 horas)
    const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const access = await prisma.qrAccess.findUnique({
      where: { idUsuario_idNegocio: { idUsuario: req.user!.id, idNegocio: data.idNegocio } },
    });

    if (!access || access.scannedAt < twentyFourHoursAgo) {
      res.status(403).json({ error: 'Debes escanear el QR del local para poder reseñar (acceso expirado o inexistente)' });
      return;
    }

    // Evitar doble reseña del mismo usuario al mismo negocio
    const existing = await prisma.resena.findFirst({
      where: { idUsuario: req.user!.id, idNegocio: data.idNegocio },
    });

    if (existing) {
      res.status(409).json({ error: 'Ya dejaste una reseña para este negocio' });
      return;
    }

    const resena = await prisma.resena.create({
      data: {
        idUsuario: req.user!.id,
        idNegocio: data.idNegocio,
        calificacion: data.calificacion,
        calidadAtencion: data.calidadAtencion,
        calidadProductos: data.calidadProductos,
        costos: data.costos,
        comentario: data.comentario,
      },
      include: {
        usuario: { select: { id: true, nombreCompleto: true, fotoPerfil: true } },
      },
    });

    // Actualizar calificación promedio del negocio
    await recalcularCalificacion(data.idNegocio);

    res.status(201).json({ message: 'Reseña creada exitosamente', resena });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESENAS] createResena error:', error);
    res.status(500).json({ error: 'Error al crear la reseña' });
  }
};

// ─── PUT /api/resenas/:id ─────────────────────────────────────
export const updateResena = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const data = UpdateResenaSchema.parse(req.body);

    const existing = await prisma.resena.findUnique({ where: { id } });
    if (!existing) {
      res.status(404).json({ error: 'Reseña no encontrada' });
      return;
    }

    if (existing.idUsuario !== req.user!.id) {
      res.status(403).json({ error: 'No puedes editar reseñas ajenas' });
      return;
    }

    // Validar qrToken y acceso activo
    const negocio = await prisma.negocio.findUnique({
      where: { id: existing.idNegocio },
      select: { qrToken: true },
    });

    if (!negocio || negocio.qrToken !== data.qrToken) {
      res.status(403).json({ error: 'Token QR inválido para este negocio' });
      return;
    }

    const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const access = await prisma.qrAccess.findUnique({
      where: { idUsuario_idNegocio: { idUsuario: req.user!.id, idNegocio: existing.idNegocio } },
    });

    if (!access || access.scannedAt < twentyFourHoursAgo) {
      res.status(403).json({ error: 'Acceso expirado. Escanea el QR nuevamente.' });
      return;
    }

    const updated = await prisma.resena.update({
      where: { id },
      data: {
        calificacion: data.calificacion,
        calidadAtencion: data.calidadAtencion,
        calidadProductos: data.calidadProductos,
        costos: data.costos,
        comentario: data.comentario,
      },
      include: {
        usuario: { select: { id: true, nombreCompleto: true, fotoPerfil: true } },
      },
    });

    await recalcularCalificacion(existing.idNegocio);

    res.status(200).json({ message: 'Reseña actualizada exitosamente', resena: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESENAS] updateResena error:', error);
    res.status(500).json({ error: 'Error al actualizar la reseña' });
  }
};

// ─── PATCH /api/resenas/:id/respuesta ────────────────────────
export const responderResena = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const { respuestaDuenio } = RespuestaDuenioSchema.parse(req.body);

    const resena = await prisma.resena.findUnique({
      where: { id },
      include: { negocio: true },
    });

    if (!resena) {
      res.status(404).json({ error: 'Reseña no encontrada' });
      return;
    }

    if (resena.negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso para responder esta reseña' });
      return;
    }

    const updated = await prisma.resena.update({
      where: { id },
      data: { respuestaDuenio },
    });

    res.status(200).json({ message: 'Respuesta guardada', resena: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESENAS] responderResena error:', error);
    res.status(500).json({ error: 'Error al guardar la respuesta' });
  }
};

// ─── DELETE /api/resenas/:id ─────────────────────────────────
export const deleteResena = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const resena = await prisma.resena.findUnique({ where: { id } });

    if (!resena) {
      res.status(404).json({ error: 'Reseña no encontrada' });
      return;
    }

    if (resena.idUsuario !== req.user!.id) {
      res.status(403).json({ error: 'No puedes eliminar reseñas ajenas' });
      return;
    }

    await prisma.resena.delete({ where: { id } });
    await recalcularCalificacion(resena.idNegocio);

    res.status(200).json({ message: 'Reseña eliminada' });
  } catch (error) {
    console.error('[RESENAS] deleteResena error:', error);
    res.status(500).json({ error: 'Error al eliminar la reseña' });
  }
};
