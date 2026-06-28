-- =========================================================================================
-- UbiTop - Base de Datos PostgreSQL (solo esquema)
-- Crea las 4 tablas del sistema: usuarios, negocios, resenas, reservas
-- Sin datos de prueba — se crean desde la app al registrarse
-- =========================================================================================

-- Eliminar tablas si existen (útil para reiniciar la base de datos)
DROP TABLE IF EXISTS "reservas" CASCADE;
DROP TABLE IF EXISTS "resenas" CASCADE;
DROP TABLE IF EXISTS "negocios" CASCADE;
DROP TABLE IF EXISTS "usuarios" CASCADE;

-- =========================================================================================
-- 1. Tabla USUARIOS
-- =========================================================================================
CREATE TABLE "usuarios" (
    "id" SERIAL NOT NULL,
    "nombreCompleto" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "contrasena" TEXT NOT NULL,
    "telefono" TEXT,
    "fotoPerfil" TEXT,
    "esDuenio" BOOLEAN NOT NULL DEFAULT false,
    "ruc" TEXT,
    "razonSocial" TEXT,
    "emailVerificado" BOOLEAN NOT NULL DEFAULT false,
    "tokenVerificacion" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "usuarios_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "usuarios_email_key" ON "usuarios"("email");

-- =========================================================================================
-- 2. Tabla NEGOCIOS
-- =========================================================================================
CREATE TABLE "negocios" (
    "id" SERIAL NOT NULL,
    "nombreNegocio" TEXT NOT NULL,
    "rubro" TEXT NOT NULL,
    "direccion" TEXT NOT NULL,
    "horario" TEXT,
    "calificacionPromedio" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "latitud" DOUBLE PRECISION,
    "longitud" DOUBLE PRECISION,
    "fotoNegocio" TEXT,
    "descripcion" TEXT,
    "precios" TEXT,
    "idDuenio" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "negocios_pkey" PRIMARY KEY ("id")
);
ALTER TABLE "negocios" ADD CONSTRAINT "negocios_idDuenio_fkey" FOREIGN KEY ("idDuenio") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- =========================================================================================
-- 3. Tabla RESEÑAS
-- =========================================================================================
CREATE TABLE "resenas" (
    "id" SERIAL NOT NULL,
    "idUsuario" INTEGER NOT NULL,
    "idNegocio" INTEGER NOT NULL,
    "calificacion" INTEGER NOT NULL,
    "calidadAtencion" INTEGER NOT NULL,
    "calidadProductos" INTEGER NOT NULL,
    "costos" INTEGER NOT NULL,
    "comentario" TEXT,
    "respuestaDuenio" TEXT,
    "fecha" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "resenas_pkey" PRIMARY KEY ("id")
);
ALTER TABLE "resenas" ADD CONSTRAINT "resenas_idUsuario_fkey" FOREIGN KEY ("idUsuario") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "resenas" ADD CONSTRAINT "resenas_idNegocio_fkey" FOREIGN KEY ("idNegocio") REFERENCES "negocios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- =========================================================================================
-- 4. Tabla RESERVAS
-- =========================================================================================
CREATE TABLE "reservas" (
    "id" SERIAL NOT NULL,
    "idNegocio" INTEGER NOT NULL,
    "idUsuario" INTEGER NOT NULL,
    "fecha" TEXT NOT NULL,
    "horaInicio" TEXT NOT NULL,
    "horaFin" TEXT NOT NULL,
    "estado" TEXT NOT NULL DEFAULT 'PENDIENTE',
    "fechaCreacion" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "reservas_pkey" PRIMARY KEY ("id")
);
ALTER TABLE "reservas" ADD CONSTRAINT "reservas_idNegocio_fkey" FOREIGN KEY ("idNegocio") REFERENCES "negocios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "reservas" ADD CONSTRAINT "reservas_idUsuario_fkey" FOREIGN KEY ("idUsuario") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- =========================================================================================
-- Fin del esquema — Sin datos de prueba
-- Los usuarios, negocios, reseñas y reservas se crean desde la app
-- =========================================================================================
