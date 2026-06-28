require('dotenv').config();
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function test() {
  try {
    // get a user
    const user = await prisma.usuario.findFirst();
    if (!user) {
      console.log("No user found");
      return;
    }
    
    // get a negocio
    const negocio = await prisma.negocio.findFirst({ where: { idDuenio: user.id } });
    if (!negocio) {
      console.log("No negocio found");
      return;
    }
    
    console.log("Updating negocio ID:", negocio.id);
    const largeBase64 = "data:image/jpeg;base64," + "A".repeat(1024 * 1024);
    
    const updated = await prisma.negocio.update({
      where: { id: negocio.id },
      data: {
        fotoNegocio: largeBase64
      }
    });
    
    console.log("Update success! Length:", updated.fotoNegocio.length);
  } catch (e) {
    console.error("Update error:", e);
  } finally {
    await prisma.$disconnect();
  }
}
test();
