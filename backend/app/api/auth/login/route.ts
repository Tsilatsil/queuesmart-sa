import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { db } from "../../../../lib/db";
import { createToken } from "../../../../lib/auth";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  const email =
    typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
  const password = typeof body?.password === "string" ? body.password : "";

  if (!email || !password) {
    return NextResponse.json(
      { error: "Email and password are required." },
      { status: 400 }
    );
  }

  const user = await db.user.findUnique({ where: { email } });

  // Compare the typed password with the scrambled one we stored
  const passwordMatches = user
    ? await bcrypt.compare(password, user.passwordHash)
    : false;

  // Same message for "no such user" and "wrong password", so nobody
  // can use this endpoint to find out which emails have accounts.
  if (!user || !passwordMatches) {
    return NextResponse.json(
      { error: "Incorrect email or password." },
      { status: 401 }
    );
  }

  const token = await createToken(user.id, user.role);

  return NextResponse.json({
    token,
    user: { id: user.id, name: user.name, email: user.email, role: user.role },
  });
}