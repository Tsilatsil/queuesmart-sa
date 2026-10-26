import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { db } from "../../../../lib/db";
import { createToken } from "../../../../lib/auth";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  const name = typeof body?.name === "string" ? body.name.trim() : "";
  const email =
    typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
  const password = typeof body?.password === "string" ? body.password : "";

  if (!name || !email.includes("@") || password.length < 8) {
    return NextResponse.json(
      {
        error:
          "Name, a valid email and a password of at least 8 characters are required.",
      },
      { status: 400 }
    );
  }

  const existing = await db.user.findUnique({ where: { email } });
  if (existing) {
    return NextResponse.json(
      { error: "An account with this email already exists." },
      { status: 409 }
    );
  }

  // Scramble the password before saving it. We never store the real one.
  const passwordHash = await bcrypt.hash(password, 12);

  const user = await db.user.create({
    data: { name, email, passwordHash },
  });

  const token = await createToken(user.id, user.role);

  return NextResponse.json(
    {
      token,
      user: { id: user.id, name: user.name, email: user.email, role: user.role },
    },
    { status: 201 }
  );
}