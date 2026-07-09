-- Add ambiente assignment fields to reservas table
ALTER TABLE "reservas" ADD COLUMN "idAmbiente" INTEGER;
ALTER TABLE "reservas" ADD COLUMN "unidadNumero" INTEGER;
ALTER TABLE "reservas" ADD COLUMN "nombreAmbiente" TEXT;

-- Add foreign key constraint
ALTER TABLE "reservas" ADD CONSTRAINT "reservas_idAmbiente_fkey" FOREIGN KEY ("idAmbiente") REFERENCES "ambientes"("id") ON DELETE SET NULL ON UPDATE CASCADE;
