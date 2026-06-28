import { Router } from 'express';
import {
  getReservasCliente,
  getReservasInbox,
  getPendingReservasCount,
  createReserva,
  updateEstadoReserva,
  cancelarReserva,
} from '../controllers/reservasController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const reservasRoutes = Router();

// GET /api/reservas/cliente  (historial del cliente autenticado)
reservasRoutes.get('/cliente', requireAuth, getReservasCliente);

// GET /api/reservas/inbox/pending-count  (conteo de pendientes para badge)
reservasRoutes.get('/inbox/pending-count', requireAuth, requireOwner, getPendingReservasCount);

// GET /api/reservas/inbox  (inbox del dueño autenticado)
reservasRoutes.get('/inbox', requireAuth, requireOwner, getReservasInbox);

// POST /api/reservas  (crear reserva, auth requerida)
reservasRoutes.post('/', requireAuth, createReserva);

// PATCH /api/reservas/:id/estado  (cambiar estado, solo dueño del negocio)
reservasRoutes.patch('/:id/estado', requireAuth, requireOwner, updateEstadoReserva);

// DELETE /api/reservas/:id  (cancelar reserva propia, cliente)
reservasRoutes.delete('/:id', requireAuth, cancelarReserva);
