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

// ─── POST /api/resenas ───────────────────────────────────────
export const createResena = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const data = CreateResenaSchema.parse(req.body);

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
