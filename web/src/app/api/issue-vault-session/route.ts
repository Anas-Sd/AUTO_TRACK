import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";
import { signVaultToken } from "@/lib/jwt";

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => ({}));
    const code = (body.code || "").toString().trim().toUpperCase();

    if (!code) {
      return NextResponse.json({ error: "Vault code is required" }, { status: 400 });
    }

    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || "";

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const { data: vault, error } = await supabase
      .from("vault_codes")
      .select("code, label")
      .eq("code", code)
      .maybeSingle();

    if (error || !vault) {
      return NextResponse.json({ error: "Invalid Vault Code" }, { status: 401 });
    }

    // Update last_accessed timestamp in Supabase
    await supabase
      .from("vault_codes")
      .update({ last_accessed: new Date().toISOString() })
      .eq("code", vault.code);

    const token = await signVaultToken(vault.code);

    const response = NextResponse.json({
      token,
      vault_code: vault.code,
      label: vault.label || "My Vault",
    });

    response.cookies.set("sb-vault-token", token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: 60 * 60 * 24 * 30, // 30 days
      path: "/",
    });

    return response;
  } catch (err: any) {
    console.error("Issue vault session error:", err);
    return NextResponse.json({ error: err.message || "Failed to issue session" }, { status: 500 });
  }
}
