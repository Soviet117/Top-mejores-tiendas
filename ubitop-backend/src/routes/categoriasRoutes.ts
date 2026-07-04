import { Router } from 'express';
import { getCategorias, createCategoria } from '../controllers/categoriasController';
import { requireAuth, requireOwner } from '../middleware/auth';

export const categoriasRoutes = Router();

categoriasRoutes.get('/', getCategorias);
categoriasRoutes.post('/', requireAuth, requireOwner, createCategoria);
