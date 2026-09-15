import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { verifyVaultToken, signVaultToken } from "@/lib/jwt";
import { createClient } from "@supabase/supabase-js";

async function getAuthenticatedVaultCode(): Promise<string | null> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("sb-vault-token")?.value;
    if (!token) return null;
    const payload = await verifyVaultToken(token);
    return payload?.vault_code || null;
  } catch (e) {
    return null;
  }
}

function getServiceSupabase() {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4ODU4MTQ5MSwiZXhwIjoyMTA0MTU3NDkxfQ.jKXBvbUv9MwHGetQ4TU1AfRTJzJACCiccJiATJZgVRI";
  return createClient(supabaseUrl, serviceRoleKey);
}

function generateRandomCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

export async function POST(req: Request) {
  try {
    const oldVaultCode = await getAuthenticatedVaultCode();
    if (!oldVaultCode) {
      return NextResponse.json({ error: "Unauthorized vault session" }, { status: 401 });
    }

    const body = await req.json().catch(() => ({}));
    const customCode = (body.customCode || "").toString().trim().toUpperCase();

    const supabase = getServiceSupabase();

    // 1. Verify old vault code exists in database
    const { data: oldVault, error: oldVaultErr } = await supabase
      .from("vault_codes")
      .select("code, label")
      .eq("code", oldVaultCode)
      .single();

    if (oldVaultErr || !oldVault) {
      return NextResponse.json({ error: "Current vault code is invalid or expired" }, { status: 404 });
    }

    // 2. Determine new Vault Code (custom vs generated)
    let newVaultCode = customCode;

    if (newVaultCode) {
      if (newVaultCode.length < 4 || newVaultCode.length > 20) {
        return NextResponse.json({ error: "Vault code must be between 4 and 20 characters long" }, { status: 400 });
      }
      const { data: existing } = await supabase
        .from("vault_codes")
        .select("code")
        .eq("code", newVaultCode)
        .maybeSingle();

      if (existing) {
        return NextResponse.json({ error: "Vault code already exists. Please choose a different code." }, { status: 400 });
      }
    } else {
      newVaultCode = generateRandomCode();
      let attempts = 0;
      while (attempts < 5) {
        const { data: existing } = await supabase
          .from("vault_codes")
          .select("code")
          .eq("code", newVaultCode)
          .maybeSingle();
        if (!existing) break;
        newVaultCode = generateRandomCode();
        attempts++;
      }
    }

    // 3. Step A: Insert new vault code entry with same label
    const { error: createErr } = await supabase
      .from("vault_codes")
      .insert({
        code: newVaultCode,
        label: oldVault.label || "My Vault",
      });

    if (createErr) {
      console.error("Error creating new vault code record:", createErr);
      return NextResponse.json({ error: "Failed to initialize new vault code" }, { status: 500 });
    }

    // Step B: Transfer categories to newVaultCode
    const { error: catUpdateErr } = await supabase
      .from("categories")
      .update({ vault_code: newVaultCode })
      .eq("vault_code", oldVaultCode);

    if (catUpdateErr) {
      console.error("Error transferring categories:", catUpdateErr);
    }

    // Step C: Transfer transactions to newVaultCode
    const { error: txnUpdateErr } = await supabase
      .from("transactions")
      .update({ vault_code: newVaultCode })
      .eq("vault_code", oldVaultCode);

    if (txnUpdateErr) {
      console.error("Error transferring transactions:", txnUpdateErr);
    }

    // Step D: Delete old vault code entry permanently
    const { error: deleteOldErr } = await supabase
      .from("vault_codes")
      .delete()
      .eq("code", oldVaultCode);

    if (deleteOldErr) {
      console.error("Error deleting old vault code:", deleteOldErr);
    }

    // 4. Issue new JWT token and update HTTP cookie
    const newToken = await signVaultToken(newVaultCode);
    const cookieStore = await cookies();
    cookieStore.set("sb-vault-token", newToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: 60 * 60 * 24 * 30, // 30 days
      path: "/",
    });

    return NextResponse.json({
      success: true,
      new_vault_code: newVaultCode,
      label: oldVault.label,
    });
  } catch (err: any) {
    console.error("Vault code rotation exception:", err);
    return NextResponse.json({ error: err.message || "Failed to rotate vault code" }, { status: 500 });
  }
}
