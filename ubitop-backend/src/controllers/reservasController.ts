import { Response } from 'express';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middleware/auth';

const prisma = new PrismaClient();

const CreateReservaSchema = z.object({
  idNegocio: z.number().int().positive(),
  fecha: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Formato de fecha inválido (YYYY-MM-DD)'),
  horaInicio: z.string().regex(/^\d{2}:\d{2}$/, 'Formato de hora inválido (HH:MM)'),
  horaFin: z.string().regex(/^\d{2}:\d{2}$/, 'Formato de hora inválido (HH:MM)'),
});

const UpdateEstadoSchema = z.object({
  estado: z.enum(['CONFIRMADA', 'RECHAZADA', 'CANCELADA']),
});

// ─── GET /api/reservas/cliente ───────────────────────────────
// Historial del cliente autenticado
export const getReservasCliente = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const reservas = await prisma.reserva.findMany({
      where: { idUsuario: req.user!.id },
      include: {
        negocio: {
          select: {
            id: true,
            nombreNegocio: true,
            rubro: true,
            fotoNegocio: true,
            direccion: true,
          },
        },
      },
      orderBy: { fechaCreacion: 'desc' },
    });

    res.status(200).json({ reservas });
  } catch (error) {
    console.error('[RESERVAS] getReservasCliente error:', error);
    res.status(500).json({ error: 'Error al obtener tus reservas' });
  }
};

// ─── GET /api/reservas/inbox/pending-count ───────────────────
// Conteo ligero de reservas PENDIENTE para el badge de notificación
export const getPendingReservasCount = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const count = await prisma.reserva.count({
      where: {
        negocio: { idDuenio: req.user!.id },
        estado: 'PENDIENTE',
      },
    });
    res.status(200).json({ count });
  } catch (error) {
    console.error('[RESERVAS] getPendingReservasCount error:', error);
    res.status(500).json({ error: 'Error al obtener conteo de reservas pendientes' });
  }
};

// ─── GET /api/reservas/inbox ─────────────────────────────────
// Inbox del dueño — todas las reservas de sus negocios
export const getReservasInbox = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const reservas = await prisma.reserva.findMany({
      where: {
        negocio: { idDuenio: req.user!.id },
      },
      include: {
        negocio: {
          select: { id: true, nombreNegocio: true, rubro: true },
        },
        usuario: {
          select: {
            id: true,
            nombreCompleto: true,
            email: true,
            telefono: true,
            fotoPerfil: true,
          },
        },
      },
      orderBy: { fechaCreacion: 'desc' },
    });

    res.status(200).json({ reservas });
  } catch (error) {
    console.error('[RESERVAS] getReservasInbox error:', error);
    res.status(500).json({ error: 'Error al obtener el inbox de reservas' });
  }
};

// ─── POST /api/reservas ──────────────────────────────────────
export const createReserva = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const data = CreateReservaSchema.parse(req.body);

    // Verificar que el negocio exista
    const negocio = await prisma.negocio.findUnique({
      where: { id: data.idNegocio },
    });

    if (!negocio) {
      res.status(404).json({ error: 'Negocio no encontrado' });
      return;
    }

    // Verificar que el cliente no sea el dueño del local
    if (negocio.idDuenio === req.user!.id) {
      res.status(400).json({ error: 'No puedes reservar en tu propio negocio' });
      return;
    }

    // Verificar conflicto de horario (misma fecha y negocio con estado PENDIENTE o CONFIRMADA)
    const conflicto = await prisma.reserva.findFirst({
      where: {
        idNegocio: data.idNegocio,
        fecha: data.fecha,
        estado: { in: ['PENDIENTE', 'CONFIRMADA'] },
        OR: [
          { horaInicio: { gte: data.horaInicio, lt: data.horaFin } },
          { horaFin: { gt: data.horaInicio, lte: data.horaFin } },
        ],
      },
    });

    if (conflicto) {
      res.status(409).json({ error: 'El horario solicitado ya está ocupado o pendiente de confirmación' });
      return;
    }

    const reserva = await prisma.reserva.create({
      data: {
        idNegocio: data.idNegocio,
        idUsuario: req.user!.id,
        fecha: data.fecha,
        horaInicio: data.horaInicio,
        horaFin: data.horaFin,
        estado: 'PENDIENTE',
      },
      include: {
        negocio: { select: { id: true, nombreNegocio: true } },
        usuario: { select: { id: true, nombreCompleto: true } },
      },
    });

    res.status(201).json({ message: 'Reserva solicitada exitosamente', reserva });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESERVAS] createReserva error:', error);
    res.status(500).json({ error: 'Error al crear la reserva' });
  }
};

// ─── PATCH /api/reservas/:id/estado ─────────────────────────
// Solo el dueño del negocio puede cambiar el estado
export const updateEstadoReserva = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const { estado } = UpdateEstadoSchema.parse(req.body);

    const reserva = await prisma.reserva.findUnique({
      where: { id },
      include: { negocio: true },
    });

    if (!reserva) {
      res.status(404).json({ error: 'Reserva no encontrada' });
      return;
    }

    if (reserva.negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso para modificar esta reserva' });
      return;
    }

    const updated = await prisma.reserva.update({
      where: { id },
      data: { estado },
    });

    res.status(200).json({ message: `Reserva ${estado.toLowerCase()}`, reserva: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESERVAS] updateEstadoReserva error:', error);
    res.status(500).json({ error: 'Error al actualizar la reserva' });
  }
};

// ─── DELETE /api/reservas/:id ────────────────────────────────
// El cliente puede cancelar su propia reserva pendiente
export const cancelarReserva = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const id = parseInt(req.params.id);
    const reserva = await prisma.reserva.findUnique({ where: { id } });

    if (!reserva) {
      res.status(404).json({ error: 'Reserva no encontrada' });
      return;
    }

    if (reserva.idUsuario !== req.user!.id) {
      res.status(403).json({ error: 'No puedes cancelar reservas ajenas' });
      return;
    }

    if (reserva.estado === 'CONFIRMADA') {
      res.status(400).json({ error: 'No puedes cancelar una reserva ya confirmada. Contacta al dueño.' });
      return;
    }

    await prisma.reserva.update({ where: { id }, data: { estado: 'CANCELADA' } });
    res.status(200).json({ message: 'Reserva cancelada' });
  } catch (error) {
    console.error('[RESERVAS] cancelarReserva error:', error);
    res.status(500).json({ error: 'Error al cancelar la reserva' });
  }
};
