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

    // Optional: fetch vault label
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";
    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const { data: vault } = await supabase
      .from("vault_codes")
      .select("label")
      .eq("code", payload.vault_code)
      .maybeSingle();

    return NextResponse.json({
      authenticated: true,
      vault_code: payload.vault_code,
      token,
      label: vault?.label || "My Vault",
    });
  } catch (err: any) {
    return NextResponse.json({ authenticated: false, vault_code: null, token: null });
  }
}
