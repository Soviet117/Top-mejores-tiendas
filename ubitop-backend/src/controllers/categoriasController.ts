import { Response } from 'express';
import { z } from 'zod';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middleware/auth';

const prisma = new PrismaClient();

const CreateCategoriaSchema = z.object({
  nombre: z.string().min(1, 'El nombre es requerido'),
});

export const getCategorias = async (_req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const categorias = await prisma.categoria.findMany({
      orderBy: { nombre: 'asc' },
    });
    res.status(200).json({ categorias });
  } catch (error) {
    console.error('[CATEGORIAS] getCategorias error:', error);
    res.status(500).json({ error: 'Error al obtener categorías' });
  }
};

export const createCategoria = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { nombre } = CreateCategoriaSchema.parse(req.body);

    const existing = await prisma.categoria.findUnique({ where: { nombre } });
    if (existing) {
      res.status(409).json({ error: 'La categoría ya existe' });
      return;
    }

    const categoria = await prisma.categoria.create({
      data: { nombre, icono: 'Category', color: '#757575' },
    });

    res.status(201).json({ categoria });
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'Datos inválidos', details: error.errors });
      return;
    }
    console.error('[CATEGORIAS] createCategoria error:', error);
    res.status(500).json({ error: 'Error al crear la categoría' });
  }
};
