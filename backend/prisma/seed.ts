import "dotenv/config";
import { PrismaClient } from "../app/generated/prisma/client";

const db = new PrismaClient();

async function main() {
  // Clear old data first so the script can be run again safely
  await db.appointment.deleteMany();
  await db.requiredDocument.deleteMany();
  await db.serviceType.deleteMany();
  await db.branch.deleteMany();

  await db.branch.createMany({
    data: [
      { name: "Demo Branch Pretoria Central", city: "Pretoria", province: "Gauteng", address: "1 Example Street, Pretoria" },
      { name: "Demo Branch Johannesburg", city: "Johannesburg", province: "Gauteng", address: "2 Sample Road, Johannesburg" },
      { name: "Demo Branch Cape Town", city: "Cape Town", province: "Western Cape", address: "3 Test Avenue, Cape Town" },
      { name: "Demo Branch Durban", city: "Durban", province: "KwaZulu-Natal", address: "4 Demo Lane, Durban" },
    ],
  });

  await db.serviceType.create({
    data: {
      name: "Smart ID card application",
      description: "Apply for a new or replacement smart ID card",
      durationMin: 20,
      requiredDocuments: {
        create: [
          { name: "Birth certificate", note: "Original or certified copy" },
          { name: "Proof of address", note: "Not older than 3 months" },
          { name: "Passport photo", note: "Only if your branch requires one" },
        ],
      },
    },
  });

  await db.serviceType.create({
    data: {
      name: "Passport application",
      description: "Apply for a new or renewed passport",
      durationMin: 30,
      requiredDocuments: {
        create: [
          { name: "Smart ID card or ID book", note: "Original" },
          { name: "Birth certificate", note: "Unabridged, if first-time applicant" },
          { name: "Passport photos", note: "Two recent photos" },
        ],
      },
    },
  });

  await db.serviceType.create({
    data: {
      name: "Birth certificate request",
      description: "Request a new or replacement birth certificate",
      durationMin: 15,
      requiredDocuments: {
        create: [
          { name: "Parent's ID", note: "Original" },
          { name: "Clinic or hospital birth record", note: "If available" },
        ],
      },
    },
  });
}

main()
  .then(() => console.log("Seed complete"))
  .finally(() => db.$disconnect());