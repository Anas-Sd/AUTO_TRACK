import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";
import { signVaultToken } from "@/lib/jwt";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const code = (searchParams.get("code") || "").trim().toUpperCase();

  if (!code) {
    return NextResponse.redirect(new URL("/", req.url));
  }

  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";
    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const { data: vault, error } = await supabase
      .from("vault_codes")
      .select("code")
      .eq("code", code)
      .maybeSingle();

    if (error || !vault) {
      // Invalid code, redirect to home page with error parameter
      return NextResponse.redirect(new URL("/?error=invalid_code", req.url));
    }

    const token = await signVaultToken(vault.code);
    const redirectUrl = new URL("/", req.url);
    const response = NextResponse.redirect(redirectUrl);

    response.cookies.set("sb-vault-token", token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: 60 * 60 * 24 * 30, // 30 days
      path: "/",
    });

    return response;
  } catch (err) {
    console.error("Auto login error:", err);
    return NextResponse.redirect(new URL("/", req.url));
  }
}
