import { randomBytes } from "crypto";
import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getUserId } from "../../../lib/auth";

const SLOT_MINUTES = 30;
// South Africa is UTC+2 all year (no daylight saving)
const SA_OFFSET_MINUTES = 120;
const MINUTE_MS = 60_000;

class SlotFullError extends Error {}
class AlreadyBookedError extends Error {}

// Makes a reference like QS-A1B2C3D4
function makeReference() {
  return "QS-" + randomBytes(4).toString("hex").toUpperCase();
}

export async function POST(request: Request) {
  // 1. Who is booking?
  const userId = await getUserId(request);
  if (!userId) {
    return NextResponse.json(
      { error: "Please log in to book an appointment." },
      { status: 401 }
    );
  }

  // 2. What are they booking?
  const body = await request.json().catch(() => null);
  const branchId = typeof body?.branchId === "string" ? body.branchId : "";
  const serviceTypeId =
    typeof body?.serviceTypeId === "string" ? body.serviceTypeId : "";
  const startsAt = new Date(typeof body?.startsAt === "string" ? body.startsAt : "");

  if (!branchId || !serviceTypeId || Number.isNaN(startsAt.getTime())) {
    return NextResponse.json(
      { error: "branchId, serviceTypeId and startsAt are required." },
      { status: 400 }
    );
  }

  const [user, branch, service] = await Promise.all([
    db.user.findUnique({ where: { id: userId } }),
    db.branch.findUnique({ where: { id: branchId } }),
    db.serviceType.findUnique({ where: { id: serviceTypeId } }),
  ]);

  if (!user) {
    return NextResponse.json(
      { error: "Please log in again." },
      { status: 401 }
    );
  }
  if (!branch) {
    return NextResponse.json({ error: "Branch not found." }, { status: 404 });
  }
  if (!service) {
    return NextResponse.json({ error: "Service not found." }, { status: 404 });
  }

  // 3. Is this a real slot? (weekday, inside opening hours, on the 30-minute grid)
  const startMs = startsAt.getTime();
  const saTime = new Date(startMs + SA_OFFSET_MINUTES * MINUTE_MS);
  const weekday = saTime.getUTCDay();
  const minuteOfDay = saTime.getUTCHours() * 60 + saTime.getUTCMinutes();

  const onTheGrid =
    saTime.getUTCSeconds() === 0 &&
    saTime.getUTCMilliseconds() === 0 &&
    (minuteOfDay - branch.opensAtMin) % SLOT_MINUTES === 0;
  const insideOpeningHours =
    minuteOfDay >= branch.opensAtMin &&
    minuteOfDay + SLOT_MINUTES <= branch.closesAtMin;

  if (weekday === 0 || weekday === 6 || !onTheGrid || !insideOpeningHours) {
    return NextResponse.json(
      { error: "That is not a valid time slot for this branch." },
      { status: 400 }
    );
  }
  if (startMs <= Date.now()) {
    return NextResponse.json(
      { error: "That time slot has already passed." },
      { status: 400 }
    );
  }

  // 4. Book it, safely
  try {
    const appointment = await db.$transaction(
      async (tx) => {
        // Lock this branch's row. If another booking at this branch is in
        // progress, we wait here until it finishes. This is what stops two
        // people from taking the last place at the same moment.
        await tx.$queryRaw`SELECT id FROM "Branch" WHERE id = ${branchId} FOR UPDATE`;

        const booked = await tx.appointment.count({
          where: { branchId, startsAt, status: "BOOKED" },
        });
        if (booked >= branch.slotCapacity) {
          throw new SlotFullError();
        }

        const mine = await tx.appointment.count({
          where: { userId, startsAt, status: "BOOKED" },
        });
        if (mine > 0) {
          throw new AlreadyBookedError();
        }

        return tx.appointment.create({
          data: {
            reference: makeReference(),
            userId,
            branchId,
            serviceTypeId,
            startsAt,
          },
          include: {
            branch: { select: { name: true } },
            serviceType: { select: { name: true } },
          },
        });
      },
      // The database is online, so give the transaction a little more time
      { maxWait: 10_000, timeout: 15_000 }
    );

    return NextResponse.json(
      {
        appointment: {
          id: appointment.id,
          reference: appointment.reference,
          startsAt: appointment.startsAt.toISOString(),
          status: appointment.status,
          branch: appointment.branch.name,
          service: appointment.serviceType.name,
        },
      },
      { status: 201 }
    );
  } catch (error) {
    if (error instanceof SlotFullError) {
      return NextResponse.json(
        { error: "That time slot is full. Please choose another." },
        { status: 409 }
      );
    }
    if (error instanceof AlreadyBookedError) {
      return NextResponse.json(
        { error: "You already have a booking at this time." },
        { status: 409 }
      );
    }
    throw error;
  }
}