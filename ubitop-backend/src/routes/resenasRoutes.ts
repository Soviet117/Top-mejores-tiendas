import { Router } from 'express';
import {
  getResenas,
  getMisResenas,
  createResena,
  responderResena,
  deleteResena,
  verifyQr,
} from '../controllers/resenasController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const resenasRoutes = Router();

// GET /api/resenas?negocioId=X  (público)
resenasRoutes.get('/', getResenas);

// GET /api/resenas/mias  (requiere auth, historial de reseñas del cliente)
resenasRoutes.get('/mias', requireAuth, getMisResenas);

// POST /api/resenas/verify-qr  (requiere auth, verifica token QR) - ANTES de /:id
resenasRoutes.post('/verify-qr', requireAuth, verifyQr);

// POST /api/resenas  (requiere auth de cliente + qrToken)
resenasRoutes.post('/', requireAuth, createResena);

// PATCH /api/resenas/:id/respuesta  (solo dueño del negocio)
resenasRoutes.patch('/:id/respuesta', requireAuth, requireOwner, responderResena);

// DELETE /api/resenas/:id  (solo el autor de la reseña)
resenasRoutes.delete('/:id', requireAuth, deleteResena);

