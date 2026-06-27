import { Router } from 'express';
import {
  getNegocios,
  getNegocioById,
  createNegocio,
  updateNegocio,
  deleteNegocio,
  getMisNegocios,
} from '../controllers/negociosController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const negociosRoutes = Router();

// GET /api/negocios?rubro=X&limit=N  (público)
negociosRoutes.get('/', getNegocios);

// GET /api/negocios/mios  (solo dueños, ANTES de /:id para evitar conflicto de rutas)
negociosRoutes.get('/mios', requireAuth, requireOwner, getMisNegocios);

// GET /api/negocios/:id  (público)
negociosRoutes.get('/:id', getNegocioById);

// POST /api/negocios  (solo dueños)
negociosRoutes.post('/', requireAuth, requireOwner, createNegocio);

// PUT /api/negocios/:id  (solo dueño del negocio)
negociosRoutes.put('/:id', requireAuth, requireOwner, updateNegocio);

// DELETE /api/negocios/:id  (solo dueño del negocio)
negociosRoutes.delete('/:id', requireAuth, requireOwner, deleteNegocio);
