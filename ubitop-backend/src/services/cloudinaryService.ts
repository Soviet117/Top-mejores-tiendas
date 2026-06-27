import { v2 as cloudinary } from 'cloudinary';
import dotenv from 'dotenv';

dotenv.config();

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
  secure: true,
});

/**
 * Sube una imagen en base64 a Cloudinary y retorna la URL segura.
 * @param base64Data - Cadena base64 de la imagen (puede incluir el prefijo data:image/...)
 * @param folder - Carpeta destino en Cloudinary (ej. "negocios", "perfiles")
 */
export const uploadImage = async (
  base64Data: string,
  folder: string = 'general'
): Promise<string> => {
  const result = await cloudinary.uploader.upload(base64Data, {
    folder: `ubitop/${folder}`,
    resource_type: 'image',
    transformation: [{ width: 800, height: 600, crop: 'limit', quality: 'auto' }],
  });
  return result.secure_url;
};

/**
 * Elimina una imagen de Cloudinary dado su public_id.
 */
export const deleteImage = async (publicId: string): Promise<void> => {
  await cloudinary.uploader.destroy(publicId);
};
