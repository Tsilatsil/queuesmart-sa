import { NextResponse } from "next/server";
import { db } from "../../../lib/db";

export async function GET() {
  const services = await db.serviceType.findMany({
    include: { requiredDocuments: true },
    orderBy: { name: "asc" },
  });
  return NextResponse.json(services);
}