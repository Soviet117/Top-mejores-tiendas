import { Response } from 'express';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middleware/auth';

const prisma = new PrismaClient();

// ─── Esquemas ───────────────────────────────────────────────
const CreateNegocioSchema = z.object({
  nombreNegocio: z.string().min(1, 'El nombre es requerido'),
  rubro: z.string().min(1, 'El rubro es requerido'),
  direccion: z.string().min(1, 'La dirección es requerida'),
  horario: z.string().optional(),
  latitud: z.number().optional(),
  longitud: z.number().optional(),
  descripcion: z.string().optional(),
  precios: z.string().optional(),       // JSON string
  fotoNegocioBase64: z.string().optional(), // Imagen en base64
});

const UpdateNegocioSchema = CreateNegocioSchema.partial();

// ─── GET /api/negocios ──────────────────────────────────────
export const getNegocios = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { rubro, limit = '100' } = req.query;

    const negocios = await prisma.negocio.findMany({
      where: rubro ? { rubro: String(rubro) } : undefined,
      take: parseInt(String(limit)),
      orderBy: { calificacionPromedio: 'desc' },
      include: {
        duenio: {
          select: { id: true, nombreCompleto: true, telefono: true },
        },
        resenas: {
          select: {
            calificacion: true,
            calidadAtencion: true,
            calidadProductos: true,
            costos: true,
          },
        },
      },
    });

    res.status(200).json({ negocios });
  } catch (error) {
    console.error('[NEGOCIOS] getNegocios error:', error);
    res.status(500).json({ error: 'Error al obtener los negocios' });
  }
};

// ─── GET /api/negocios/:id ──────────────────────────────────
export const getNegocioById = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);

    const negocio = await prisma.negocio.findUnique({
      where: { id },
      include: {
        duenio: {
          select: { id: true, nombreCompleto: true, telefono: true, email: true },
        },
        resenas: {
          include: {
            usuario: { select: { id: true, nombreCompleto: true, fotoPerfil: true } },
          },
          orderBy: { fecha: 'desc' },
        },
      },
    });

    if (!negocio) {
      res.status(404).json({ error: 'Negocio no encontrado' });
      return;
    }

    res.status(200).json({ negocio });
  } catch (error) {
    console.error('[NEGOCIOS] getNegocioById error:', error);
    res.status(500).json({ error: 'Error al obtener el negocio' });
  }
};

// ─── POST /api/negocios ──────────────────────────────────────
export const createNegocio = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const data = CreateNegocioSchema.parse(req.body);
    const negocio = await prisma.negocio.create({
      data: {
        nombreNegocio: data.nombreNegocio,
        rubro: data.rubro,
        direccion: data.direccion,
        horario: data.horario,
        latitud: data.latitud,
        longitud: data.longitud,
        descripcion: data.descripcion,
        precios: data.precios,
        fotoNegocio: data.fotoNegocioBase64,
        idDuenio: req.user!.id,
      },
    });

    res.status(201).json({ message: 'Negocio creado exitosamente', negocio });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[NEGOCIOS] createNegocio error:', error);
    res.status(500).json({ error: 'Error al crear el negocio' });
  }
};

// ─── PUT /api/negocios/:id ───────────────────────────────────
export const updateNegocio = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const data = UpdateNegocioSchema.parse(req.body);

    // Verificar propiedad
    const existing = await prisma.negocio.findUnique({ where: { id } });
    if (!existing || existing.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso para editar este negocio' });
      return;
    }

    const updated = await prisma.negocio.update({
      where: { id },
      data: {
        nombreNegocio: data.nombreNegocio,
        rubro: data.rubro,
        direccion: data.direccion,
        horario: data.horario,
        latitud: data.latitud,
        longitud: data.longitud,
        descripcion: data.descripcion,
        precios: data.precios,
        ...(data.fotoNegocioBase64 ? { fotoNegocio: data.fotoNegocioBase64 } : {}),
      },
    });

    res.status(200).json({ message: 'Negocio actualizado', negocio: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[NEGOCIOS] updateNegocio error:', error);
    res.status(500).json({ error: 'Error al actualizar el negocio' });
  }
};

// ─── DELETE /api/negocios/:id ────────────────────────────────
export const deleteNegocio = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);

    const existing = await prisma.negocio.findUnique({ where: { id } });
    if (!existing || existing.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso para eliminar este negocio' });
      return;
    }

    await prisma.negocio.delete({ where: { id } });
    res.status(200).json({ message: 'Negocio eliminado exitosamente' });
  } catch (error) {
    console.error('[NEGOCIOS] deleteNegocio error:', error);
    res.status(500).json({ error: 'Error al eliminar el negocio' });
  }
};

// ─── GET /api/negocios/mios ──────────────────────────────────
// Negocios del dueño autenticado
export const getMisNegocios = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const negocios = await prisma.negocio.findMany({
      where: { idDuenio: req.user!.id },
      include: {
        resenas: { select: { calificacion: true } },
      },
      orderBy: { createdAt: 'desc' },
    });

    res.status(200).json({ negocios });
  } catch (error) {
    console.error('[NEGOCIOS] getMisNegocios error:', error);
    res.status(500).json({ error: 'Error al obtener tus negocios' });
  }
};
