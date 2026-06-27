import { Router } from 'express';
import { register, login, getMe } from '../controllers/authController';
import { requireAuth } from '../middleware/auth';

export const authRoutes = Router();

// POST /api/auth/register
authRoutes.post('/register', register);

// POST /api/auth/login
authRoutes.post('/login', login);

// GET /api/auth/me  (requiere token)
authRoutes.get('/me', requireAuth, getMe);
