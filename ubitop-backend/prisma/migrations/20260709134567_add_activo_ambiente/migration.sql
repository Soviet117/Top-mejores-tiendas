-- Add activo column to ambientes table
ALTER TABLE "ambientes" ADD COLUMN "activo" BOOLEAN NOT NULL DEFAULT true;
