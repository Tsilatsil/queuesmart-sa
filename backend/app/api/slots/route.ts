import { NextResponse } from "next/server";
import { db } from "../../../lib/db";

const SLOT_MINUTES = 30;
// South Africa is UTC+2 all year (no daylight saving), so a fixed offset works
const SA_OFFSET_MINUTES = 120;
const MINUTE_MS = 60_000;

// 480 -> "08:00"
function formatMinutes(total: number) {
  const h = String(Math.floor(total / 60)).padStart(2, "0");
  const m = String(total % 60).padStart(2, "0");
  return `${h}:${m}`;
}

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const branchId = searchParams.get("branchId");
  const date = searchParams.get("date"); // looks like 2026-10-28

  const match = date ? /^(\d{4})-(\d{2})-(\d{2})$/.exec(date) : null;
  if (!branchId || !match) {
    return NextResponse.json(
      { error: "branchId and date (YYYY-MM-DD) are required." },
      { status: 400 }
    );
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);

  // Reject dates that don't exist, like 2026-02-31
  const calendarDay = new Date(Date.UTC(year, month - 1, day));
  if (
    calendarDay.getUTCFullYear() !== year ||
    calendarDay.getUTCMonth() !== month - 1 ||
    calendarDay.getUTCDate() !== day
  ) {
    return NextResponse.json(
      { error: "That date does not exist." },
      { status: 400 }
    );
  }

  const branch = await db.branch.findUnique({ where: { id: branchId } });
  if (!branch) {
    return NextResponse.json({ error: "Branch not found." }, { status: 404 });
  }

  // 0 = Sunday, 6 = Saturday
  const weekday = calendarDay.getUTCDay();
  if (weekday === 0 || weekday === 6) {
    return NextResponse.json({
      branchId,
      date,
      closed: true,
      message: "This branch is closed on weekends.",
      slots: [],
    });
  }

  // Midnight at the start of this day in South Africa, as a UTC time
  const dayStart = calendarDay.getTime() - SA_OFFSET_MINUTES * MINUTE_MS;
  const dayEnd = dayStart + 24 * 60 * MINUTE_MS;

  // Every active booking at this branch on this day
  const bookings = await db.appointment.findMany({
    where: {
      branchId,
      status: "BOOKED",
      startsAt: { gte: new Date(dayStart), lt: new Date(dayEnd) },
    },
    select: { startsAt: true },
  });

  // Count how many bookings each slot already has
  const bookedPerSlot = new Map<number, number>();
  for (const booking of bookings) {
    const key = booking.startsAt.getTime();
    bookedPerSlot.set(key, (bookedPerSlot.get(key) ?? 0) + 1);
  }

  const now = Date.now();
  const slots = [];

  for (
    let minute = branch.opensAtMin;
    minute + SLOT_MINUTES <= branch.closesAtMin;
    minute += SLOT_MINUTES
  ) {
    const startsAt = new Date(dayStart + minute * MINUTE_MS);
    const booked = bookedPerSlot.get(startsAt.getTime()) ?? 0;
    const remaining = Math.max(branch.slotCapacity - booked, 0);

    slots.push({
      startsAt: startsAt.toISOString(),
      time: formatMinutes(minute),
      capacity: branch.slotCapacity,
      remaining,
      available: remaining > 0 && startsAt.getTime() > now,
    });
  }

  return NextResponse.json({ branchId, date, closed: false, slots });
}