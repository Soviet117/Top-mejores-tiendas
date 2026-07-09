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
  personas: z.number().int().positive().optional(),
});

const UpdateEstadoSchema = z.object({
  estado: z.enum(['CONFIRMADA', 'RECHAZADA', 'CANCELADA']),
});

const AsignarAmbienteSchema = z.object({
  idAmbiente: z.number().int().positive(),
  unidadNumero: z.number().int().positive().optional(),
});

// ─── GET /api/negocios/:idNegocio/ambientes-disponibles ─────
// Devuelve ambientes con cuentas de unidades libres/total
export const getAmbientesDisponibles = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const idNegocio = parseInt(req.params.idNegocio);

    // Verificar que el dueño sea el propietario
    const negocio = await prisma.negocio.findUnique({ where: { id: idNegocio } });
    if (!negocio || negocio.idDuenio !== req.user!.id) {
      res.status(403).json({ error: 'No tienes permiso para ver este negocio' });
      return;
    }

    const ambientes = await prisma.ambiente.findMany({
      where: { idNegocio },
    });

    // Para cada ambiente, contar cuántas unidades están ocupadas (excluyendo reservas que ya terminaron)
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    const result = await Promise.all(ambientes.map(async (amb) => {
      const ocupadas = await prisma.reserva.count({
        where: {
          idAmbiente: amb.id,
          estado: { in: ['PENDIENTE', 'CONFIRMADA'] },
          OR: [
            { fecha: { gt: todayStr } },
            {
              fecha: todayStr,
              horaFin: { gt: currentTime },
            },
          ],
        },
      });
      return {
        id: amb.id,
        nombre: amb.nombre,
        cantidad: amb.cantidad,
        capacidad: amb.capacidad,
        libres: amb.cantidad - ocupadas,
      };
    }));

    res.status(200).json({ ambientes: result });
  } catch (error) {
    console.error('[RESERVAS] getAmbientesDisponibles error:', error);
    res.status(500).json({ error: 'Error al obtener ambientes disponibles' });
  }
};

// ─── POST /api/reservas/:idReserva/asignar-ambiente ─────────
export const asignarAmbiente = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const idReserva = parseInt(req.params.idReserva);
    const { idAmbiente, unidadNumero } = AsignarAmbienteSchema.parse(req.body);

    const reserva = await prisma.reserva.findUnique({
      where: { id: idReserva },
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

    if (reserva.estado !== 'PENDIENTE') {
      res.status(400).json({ error: 'Solo se pueden asignar ambientes a reservas pendientes' });
      return;
    }

    const ambiente = await prisma.ambiente.findUnique({ where: { id: idAmbiente } });
    if (!ambiente || ambiente.idNegocio !== reserva.idNegocio) {
      res.status(400).json({ error: 'Ambiente inválido para este negocio' });
      return;
    }

    // Si no se especifica unidadNumero, auto-asignar la primera libre
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    let unidad = unidadNumero;
    if (!unidad) {
      const ocupadas = await prisma.reserva.findMany({
        where: {
          idAmbiente,
          estado: { in: ['PENDIENTE', 'CONFIRMADA'] },
          id: { not: idReserva },
          OR: [
            { fecha: { gt: todayStr } },
            { fecha: todayStr, horaFin: { gt: currentTime } },
          ],
        },
        select: { unidadNumero: true },
      });
      const numsOcupados = new Set(ocupadas.map(r => r.unidadNumero));
      for (let i = 1; i <= ambiente.cantidad; i++) {
        if (!numsOcupados.has(i)) {
          unidad = i;
          break;
        }
      }
      if (!unidad) {
        res.status(409).json({ error: `No hay unidades libres en "${ambiente.nombre}"` });
        return;
      }
    } else {
      if (unidad < 1 || unidad > ambiente.cantidad) {
        res.status(400).json({ error: `Número de unidad inválido (1-${ambiente.cantidad})` });
        return;
      }

      // Verificar que la unidad no esté ocupada
      const ocupada = await prisma.reserva.findFirst({
        where: {
          idAmbiente,
          unidadNumero: unidad,
          estado: { in: ['PENDIENTE', 'CONFIRMADA'] },
          id: { not: idReserva },
          OR: [
            { fecha: { gt: todayStr } },
            { fecha: todayStr, horaFin: { gt: currentTime } },
          ],
        },
      });

      if (ocupada) {
        res.status(409).json({ error: `La unidad ${unidad} de "${ambiente.nombre}" ya está ocupada` });
        return;
      }
    }

    const updated = await prisma.reserva.update({
      where: { id: idReserva },
      data: {
        idAmbiente,
        unidadNumero: unidad,
        nombreAmbiente: ambiente.nombre,
      },
    });

    res.status(200).json({ message: 'Ambiente asignado correctamente', reserva: updated });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[RESERVAS] asignarAmbiente error:', error);
    res.status(500).json({ error: 'Error al asignar ambiente' });
  }
};

// ─── DELETE /api/reservas/:idReserva/quitar-ambiente ─────────
export const quitarAmbiente = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const idReserva = parseInt(req.params.idReserva);

    const reserva = await prisma.reserva.findUnique({
      where: { id: idReserva },
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

    if (!reserva.idAmbiente) {
      res.status(400).json({ error: 'La reserva no tiene ambiente asignado' });
      return;
    }

    const updated = await prisma.reserva.update({
      where: { id: idReserva },
      data: {
        idAmbiente: null,
        unidadNumero: null,
        nombreAmbiente: null,
      },
    });

    res.status(200).json({ message: 'Ambiente desasignado correctamente', reserva: updated });
  } catch (error) {
    console.error('[RESERVAS] quitarAmbiente error:', error);
    res.status(500).json({ error: 'Error al quitar ambiente' });
  }
};

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
        personas: data.personas ?? 1,
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
