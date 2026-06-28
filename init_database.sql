-- =========================================================================================
-- UbiTop - Base de Datos PostgreSQL Inicial
-- Incluye la creación de tablas basadas en el esquema y algunos datos de prueba.
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
-- DATOS INICIALES DE PRUEBA (SEED)
-- Nota: La contraseña para ambos usuarios de prueba es "123456" (hasheada con bcrypt para Node.js)
-- =========================================================================================

-- Inserción de usuarios
INSERT INTO "usuarios" ("id", "nombreCompleto", "email", "contrasena", "telefono", "esDuenio", "emailVerificado", "ruc", "razonSocial") 
VALUES 
(1, 'Carlos Dueño', 'dueno@ubitop.com', '$2a$12$R.R1sI0l2d/Z8oV4K.q/HOfwXGg8x5Hq/K6lP/c5t2lXv7I8A8Xp6', '999888777', true, true, '20123456789', 'Servicios Deportivos SAC'),
(2, 'Ana Cliente', 'cliente@ubitop.com', '$2a$12$R.R1sI0l2d/Z8oV4K.q/HOfwXGg8x5Hq/K6lP/c5t2lXv7I8A8Xp6', '987654321', false, true, NULL, NULL);

-- Ajustar la secuencia de IDs de usuarios
SELECT setval(pg_get_serial_sequence('"usuarios"', 'id'), 2);

-- Inserción de un negocio
INSERT INTO "negocios" ("id", "nombreNegocio", "rubro", "direccion", "horario", "calificacionPromedio", "idDuenio", "descripcion", "fotoNegocio") 
VALUES 
(1, 'Cancha Sintética El Crack', 'Canchas Sintéticas', 'Av. Las Palmas 123', '08:00 - 22:00', 4.5, 1, 'Las mejores canchas sintéticas con grass artificial de última generación.', 'https://images.unsplash.com/photo-1518605368461-1ee125b29094?auto=format&fit=crop&q=80');

-- Ajustar la secuencia de IDs de negocios
SELECT setval(pg_get_serial_sequence('"negocios"', 'id'), 1);

-- Inserción de una reseña de prueba
INSERT INTO "resenas" ("id", "idUsuario", "idNegocio", "calificacion", "calidadAtencion", "calidadProductos", "costos", "comentario") 
VALUES 
(1, 2, 1, 5, 5, 4, 5, 'Excelente estado de las canchas, muy recomendable.');

-- Ajustar la secuencia de IDs de reseñas
SELECT setval(pg_get_serial_sequence('"resenas"', 'id'), 1);

-- Inserción de una reserva de prueba
INSERT INTO "reservas" ("id", "idNegocio", "idUsuario", "fecha", "horaInicio", "horaFin", "estado") 
VALUES 
(1, 1, 2, '2026-07-01', '18:00', '19:00', 'CONFIRMADA');

-- Ajustar la secuencia de IDs de reservas
SELECT setval(pg_get_serial_sequence('"reservas"', 'id'), 1);
