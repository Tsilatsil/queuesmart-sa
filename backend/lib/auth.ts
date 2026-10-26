import { SignJWT, jwtVerify } from "jose";

function getSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET is not set in .env");
  }
  return new TextEncoder().encode(secret);
}

// Creates a signed token that lasts 7 days
export async function createToken(userId: string, role: string) {
  return new SignJWT({ role })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(userId)
    .setIssuedAt()
    .setExpirationTime("7d")
    .sign(getSecret());
}

// Checks a token and returns what's inside it
export async function verifyToken(token: string) {
  const { payload } = await jwtVerify(token, getSecret());
  return payload;
}

// Reads "Authorization: Bearer <token>" from a request.
// Returns the user's id, or null if the token is missing or invalid.
export async function getUserId(request: Request): Promise<string | null> {
  const header = request.headers.get("authorization") ?? "";
  const [scheme, token] = header.split(" ");
  if (scheme !== "Bearer" || !token) return null;

  try {
    const payload = await verifyToken(token);
    return typeof payload.sub === "string" ? payload.sub : null;
  } catch {
    return null;
  }
}