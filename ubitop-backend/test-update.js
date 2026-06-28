async function test() {
  try {
    const { z } = require('zod');
    const CreateNegocioSchema = z.object({
      nombreNegocio: z.string().min(1, 'El nombre es requerido'),
      rubro: z.string().min(1, 'El rubro es requerido'),
      direccion: z.string().min(1, 'La dirección es requerida'),
      horario: z.string().optional(),
      latitud: z.number().optional(),
      longitud: z.number().optional(),
      descripcion: z.string().optional(),
      precios: z.string().optional(),       // JSON string
      fotoNegocioBase64: z.string().optional(), // Imagen en base64
    });

    const UpdateNegocioSchema = CreateNegocioSchema.partial();
    
    // Create a 1MB base64 string
    const largeBase64 = "data:image/jpeg;base64," + "A".repeat(1024 * 1024);
    
    const payload = {
      nombreNegocio: "Test",
      fotoNegocioBase64: largeBase64
    };
    
    const data = UpdateNegocioSchema.parse(payload);
    console.log("Zod parse success! Length:", data.fotoNegocioBase64.length);
  } catch (e) {
    console.error("Zod parse error:", e);
  }
}
test();
