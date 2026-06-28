const { z } = require('zod');

const CreateNegocioSchema = z.object({
  nombreNegocio: z.string().min(1, 'El nombre es requerido'),
  rubro: z.string().min(1, 'El rubro es requerido'),
  direccion: z.string().min(1, 'La dirección es requerida'),
  horario: z.string().optional(),
  latitud: z.number().optional(),
  longitud: z.number().optional(),
  descripcion: z.string().optional(),
  precios: z.string().optional(),
  fotoNegocioBase64: z.string().optional(),
});

const UpdateNegocioSchema = CreateNegocioSchema.partial();

const reqBody = {
  nombreNegocio: 'Test',
  fotoNegocioBase64: 'data:image/jpeg;base64,AAABBBCCC'
};

const data = UpdateNegocioSchema.parse(reqBody);

const prismaUpdateData = {
  nombreNegocio: data.nombreNegocio,
  rubro: data.rubro,
  direccion: data.direccion,
  horario: data.horario,
  latitud: data.latitud,
  longitud: data.longitud,
  descripcion: data.descripcion,
  precios: data.precios,
  ...(data.fotoNegocioBase64 ? { fotoNegocio: data.fotoNegocioBase64 } : {}),
};

console.log("Prisma data:", prismaUpdateData);
