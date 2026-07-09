import { Router } from 'express';
import {
  getAmbientesByNegocio,
  updateAmbiente,
  toggleAmbiente,
} from '../controllers/ambientesController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const ambientesRoutes = Router();

// Todas las rutas requieren autenticación y ser dueño

// GET /api/ambientes/negocio/:idNegocio  (listar ambientes de un negocio)
ambientesRoutes.get('/negocio/:idNegocio', requireAuth, requireOwner, getAmbientesByNegocio);

// PUT /api/ambientes/:id  (editar ambiente)
ambientesRoutes.put('/:id', requireAuth, requireOwner, updateAmbiente);

// PATCH /api/ambientes/:id/toggle  (activar/desactivar ambiente)
ambientesRoutes.patch('/:id/toggle', requireAuth, requireOwner, toggleAmbiente);
