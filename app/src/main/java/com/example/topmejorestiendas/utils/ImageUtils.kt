package com.example.topmejorestiendas.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun uriToBase64(context: Context, uriString: String): String? {
        if (uriString.isBlank()) return null
        // Si ya es una URL web o un base64, devolvemos el string original
        if (uriString.startsWith("http") || uriString.startsWith("data:image")) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                
                // Redimensionar imagen para evitar Base64 gigantes
                val maxWidth = 1024f
                val maxHeight = 1024f
                val scale = Math.min(maxWidth / originalBitmap.width, maxHeight / originalBitmap.height)
                
                val scaledBitmap = if (scale < 1) {
                    Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width * scale).toInt(), (originalBitmap.height * scale).toInt(), true)
                } else {
                    originalBitmap
                }
                
                val outputStream = ByteArrayOutputStream()
                // Comprimir imagen para no exceder límites de payload (70% de calidad)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                // Cloudinary/PostgreSQL requiere este formato
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
