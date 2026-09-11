import { SignJWT, jwtVerify } from "jose";

function getSecretKey(): Uint8Array {
  const secret = process.env.SUPABASE_JWT_SECRET || "super-secret-jwt-key-auto-track-2026-vault";
  return new TextEncoder().encode(secret);
}

export async function signVaultToken(vaultCode: string): Promise<string> {
  const secretKey = getSecretKey();
  return await new SignJWT({
    role: "authenticated",
    vault_code: vaultCode,
  })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuedAt()
    .setExpirationTime("30d")
    .sign(secretKey);
}

export async function verifyVaultToken(
  token: string
): Promise<{ role: string; vault_code: string } | null> {
  try {
    const secretKey = getSecretKey();
    const { payload } = await jwtVerify(token, secretKey);
    return payload as unknown as { role: string; vault_code: string };
  } catch (err) {
    return null;
  }
}
