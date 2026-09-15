import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { verifyVaultToken } from "@/lib/jwt";
import { createClient } from "@supabase/supabase-js";

export async function GET() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("sb-vault-token")?.value;

    if (!token) {
      return NextResponse.json({ authenticated: false, vault_code: null, token: null });
    }

    const payload = await verifyVaultToken(token);
    if (!payload || !payload.vault_code) {
      return NextResponse.json({ authenticated: false, vault_code: null, token: null });
    }

    // Verify vault code actually exists in DB
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";
    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const { data: vault } = await supabase
      .from("vault_codes")
      .select("code, label")
      .eq("code", payload.vault_code)
      .maybeSingle();

    if (!vault) {
      const response = NextResponse.json({ authenticated: false, vault_code: null, token: null });
      response.cookies.set("sb-vault-token", "", {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        maxAge: 0,
        path: "/",
      });
      return response;
    }

    return NextResponse.json({
      authenticated: true,
      vault_code: vault.code,
      token,
      label: vault.label || "My Vault",
    });
  } catch (err: any) {
    return NextResponse.json({ authenticated: false, vault_code: null, token: null });
  }
}
