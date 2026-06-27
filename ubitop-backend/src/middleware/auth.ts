import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

// ─── Tipos ─────────────────────────────────────────────────
export interface AuthenticatedRequest extends Request {
  user?: {
    id: number;
    email: string;
    esDuenio: boolean;
  };
}

// ─── Verificar JWT ─────────────────────────────────────────
export const requireAuth = (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): void => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Token de autenticación requerido' });
    return;
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET!) as {
      id: number;
      email: string;
      esDuenio: boolean;
    };
    req.user = decoded;
    next();
  } catch {
    res.status(401).json({ error: 'Token inválido o expirado' });
  }
};

// ─── Solo Dueños ────────────────────────────────────────────
export const requireOwner = (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): void => {
  if (!req.user?.esDuenio) {
    res.status(403).json({ error: 'Acceso restringido a dueños de negocios' });
    return;
  }
  next();
};
