import express, { Application, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { authRoutes } from './routes/authRoutes';
import { negociosRoutes } from './routes/negociosRoutes';
import { resenasRoutes } from './routes/resenasRoutes';
import { reservasRoutes } from './routes/reservasRoutes';

dotenv.config();

const app: Application = express();
const PORT = process.env.PORT || 3000;

// ─── Middleware Global ───────────────────────────────────────
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.use(express.json({ limit: '10mb' })); // Aumentado para soportar imágenes en base64 si fuera necesario
app.use(express.urlencoded({ extended: true }));

// ─── Health Check ────────────────────────────────────────────
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'UP',
    service: 'UbiTop API',
    timestamp: new Date().toISOString(),
  });
});

// ─── Rutas de la API ─────────────────────────────────────────
app.use('/api/auth', authRoutes);
app.use('/api/negocios', negociosRoutes);
app.use('/api/resenas', resenasRoutes);
app.use('/api/reservas', reservasRoutes);

// ─── 404 Handler ─────────────────────────────────────────────
app.use((_req: Request, res: Response) => {
  res.status(404).json({ error: 'Ruta no encontrada' });
});

// ─── Global Error Handler ─────────────────────────────────────
// eslint-disable-next-line @typescript-eslint/no-unused-vars
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error('[ERROR]', err.message);
  res.status(500).json({ error: 'Error interno del servidor', detail: err.message });
});

// ─── Inicio del Servidor ─────────────────────────────────────
app.listen(PORT, () => {
  console.log(`UbiTop API corriendo en el puerto ${PORT}`);
});

export default app;
