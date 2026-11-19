import "dotenv/config";
import bcrypt from "bcryptjs";
import { PrismaClient } from "@prisma/client";

const db = new PrismaClient();

async function main() {
  const confirmed = process.argv.includes("confirm");

  const users = await db.user.count();
  const appointments = await db.appointment.count();
  console.log(`Found ${users} accounts and ${appointments} bookings.`);

  if (!confirmed) {
    console.log(
      "Nothing was changed. Run again with the word 'confirm' to delete them and create the demo account."
    );
    return;
  }

  // Bookings first, because they point at users
  await db.appointment.deleteMany();
  await db.user.deleteMany();

  const passwordHash = await bcrypt.hash("DemoPass123", 12);
  await db.user.create({
    data: { name: "Demo User", email: "demo@example.com", passwordHash },
  });

  console.log("Done. Deleted all accounts and bookings, then created demo@example.com");
}

main().finally(() => db.$disconnect());