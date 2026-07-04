import { Router } from 'express';
import { register, login, getMe, updateProfile, updatePassword, deleteAccount, sendVerificationCodeHandler } from '../controllers/authController';
import { requireAuth } from '../middleware/auth';

export const authRoutes = Router();

// POST /api/auth/register
authRoutes.post('/register', register);

// POST /api/auth/login
authRoutes.post('/login', login);

// GET /api/auth/me  (requiere token)
authRoutes.get('/me', requireAuth, getMe);

// PUT /api/auth/profile  (actualizar perfil)
authRoutes.put('/profile', requireAuth, updateProfile);

// PUT /api/auth/password  (cambiar contraseña)
authRoutes.put('/password', requireAuth, updatePassword);

// DELETE /api/auth/account  (eliminar cuenta)
authRoutes.delete('/account', requireAuth, deleteAccount);

// POST /api/auth/send-verification-code
authRoutes.post('/send-verification-code', sendVerificationCodeHandler);
