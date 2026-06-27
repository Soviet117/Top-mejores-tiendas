import { Router } from 'express';
import {
  getResenas,
  createResena,
  responderResena,
  deleteResena,
} from '../controllers/resenasController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const resenasRoutes = Router();

// GET /api/resenas?negocioId=X  (público)
resenasRoutes.get('/', getResenas);

// POST /api/resenas  (requiere auth de cliente)
resenasRoutes.post('/', requireAuth, createResena);

// PATCH /api/resenas/:id/respuesta  (solo dueño del negocio)
resenasRoutes.patch('/:id/respuesta', requireAuth, requireOwner, responderResena);

// DELETE /api/resenas/:id  (solo el autor de la reseña)
resenasRoutes.delete('/:id', requireAuth, deleteResena);
