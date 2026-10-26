import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { getUserId } from "../../../../lib/auth";

// Next.js passes the [id] part of the address as a Promise, so we await it
export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const userId = await getUserId(request);
  if (!userId) {
    return NextResponse.json(
      { error: "Please log in to cancel an appointment." },
      { status: 401 }
    );
  }

  const { id } = await params;
  const appointment = await db.appointment.findUnique({ where: { id } });

  // Same answer for "doesn't exist" and "belongs to someone else",
  // so nobody can use this to discover other people's bookings.
  if (!appointment || appointment.userId !== userId) {
    return NextResponse.json(
      { error: "Appointment not found." },
      { status: 404 }
    );
  }

  if (appointment.status !== "BOOKED") {
    return NextResponse.json(
      { error: "This appointment is already cancelled or completed." },
      { status: 409 }
    );
  }

  if (appointment.startsAt.getTime() <= Date.now()) {
    return NextResponse.json(
      { error: "Past appointments cannot be cancelled." },
      { status: 400 }
    );
  }

  const updated = await db.appointment.update({
    where: { id },
    data: { status: "CANCELLED" },
  });

  return NextResponse.json({
    appointment: {
      id: updated.id,
      reference: updated.reference,
      status: updated.status,
    },
  });
}