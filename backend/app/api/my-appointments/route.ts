import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getUserId } from "../../../lib/auth";

export async function GET(request: Request) {
  const userId = await getUserId(request);
  if (!userId) {
    return NextResponse.json(
      { error: "Please log in to see your appointments." },
      { status: 401 }
    );
  }

  const appointments = await db.appointment.findMany({
    where: { userId },
    orderBy: { startsAt: "asc" },
    include: {
      branch: { select: { name: true, address: true } },
      serviceType: {
        select: {
          name: true,
          requiredDocuments: { select: { name: true, note: true } },
        },
      },
    },
  });

  return NextResponse.json(
    appointments.map((a) => ({
      id: a.id,
      reference: a.reference,
      startsAt: a.startsAt.toISOString(),
      status: a.status,
      branch: a.branch.name,
      address: a.branch.address,
      service: a.serviceType.name,
      requiredDocuments: a.serviceType.requiredDocuments,
    }))
  );
}