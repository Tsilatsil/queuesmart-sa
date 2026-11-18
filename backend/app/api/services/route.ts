import { NextResponse, connection } from "next/server";
import { db } from "../../../lib/db";

export async function GET() {
  // Run this when someone asks, not once while the site is being built
  await connection();

  const services = await db.serviceType.findMany({
    include: { requiredDocuments: true },
    orderBy: { name: "asc" },
  });
  return NextResponse.json(services);
}