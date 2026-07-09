import { Response } from 'express';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middleware/auth';

const prisma = new PrismaClient();

const UpdateAmbienteSchema = z.object({
  nombre: z.string().min(1, 'Nombre requerido'),
  cantidad: z.number().int().positive(),
  capacidad: z.number().int().positive(),
});

// ─── GET /api/ambientes/negocio/:idNegocio ─────────────────
// Lista TODOS los ambientes de un negocio (para gestión del dueño)
export const getAmbientesByNegocio = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const idNegocio = parseInt(req.params.idNegocio);

    const negocio = await prisma.negocio.findUnique({ where: { id: idNegocio } });
    if (!negocio || negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso' });
      return;
    }

    const ambientes = await prisma.ambiente.findMany({
      where: { idNegocio },
      orderBy: { id: 'asc' },
    });

    res.status(200).json({ ambientes });
  } catch (error) {
    console.error('[AMBIENTES] getAmbientesByNegocio error:', error);
    res.status(500).json({ error: 'Error al obtener ambientes' });
  }
};

// ─── PUT /api/ambientes/:id ─────────────────────────────────
// Editar nombre, cantidad, capacidad de un ambiente
export const updateAmbiente = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const data = UpdateAmbienteSchema.parse(req.body);

    const ambiente = await prisma.ambiente.findUnique({
      where: { id },
      include: { negocio: { select: { idDuenio: true } } },
    });

    if (!ambiente) {
      res.status(404).json({ error: 'Ambiente no encontrado' });
      return;
    }

    if (ambiente.negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso' });
      return;
    }

    const updated = await prisma.ambiente.update({
      where: { id },
      data: {
        nombre: data.nombre,
        cantidad: data.cantidad,
        capacidad: data.capacidad,
      },
    });

    res.status(200).json({ message: 'Ambiente actualizado', ambiente: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[AMBIENTES] updateAmbiente error:', error);
    res.status(500).json({ error: 'Error al actualizar ambiente' });
  }
};

// ─── PATCH /api/ambientes/:id/toggle ────────────────────────
// Activar/desactivar (poner en reposo) un ambiente
export const toggleAmbiente = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);

    const ambiente = await prisma.ambiente.findUnique({
      where: { id },
      include: { negocio: { select: { idDuenio: true } } },
    });

    if (!ambiente) {
      res.status(404).json({ error: 'Ambiente no encontrado' });
      return;
    }

    if (ambiente.negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso' });
      return;
    }

    const updated = await prisma.ambiente.update({
      where: { id },
      data: { activo: !ambiente.activo },
    });

    res.status(200).json({
      message: updated.activo ? 'Ambiente activado' : 'Ambiente en reposo',
      ambiente: updated,
    });
  } catch (error) {
    console.error('[AMBIENTES] toggleAmbiente error:', error);
    res.status(500).json({ error: 'Error al cambiar estado del ambiente' });
  }
};
