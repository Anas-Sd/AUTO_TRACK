import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { verifyVaultToken } from "@/lib/jwt";
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
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || "";
  return createClient(supabaseUrl, serviceRoleKey);
}

export async function GET(req: Request) {
  try {
    const vaultCode = await getAuthenticatedVaultCode();
    if (!vaultCode) {
      return NextResponse.json({ error: "Unauthorized vault session" }, { status: 401 });
    }

    const { searchParams } = new URL(req.url);
    const type = searchParams.get("type");
    const supabase = getServiceSupabase();

    if (type === "categories") {
      let { data: categories, error } = await supabase
        .from("categories")
        .select("*")
        .eq("vault_code", vaultCode)
        .order("created_at", { ascending: true });

      if (error) throw error;
      return NextResponse.json({ data: categories || [] });
    } else if (type === "transactions") {
      let { data: transactions, error } = await supabase
        .from("transactions")
        .select("*")
        .eq("vault_code", vaultCode)
        .order("occurred_at", { ascending: false });

      if (error) throw error;
      return NextResponse.json({ data: transactions || [] });
    }

    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  } catch (err: any) {
    console.error("API vault data GET error:", err);
    return NextResponse.json({ error: err.message || "Fetch failed" }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const vaultCode = await getAuthenticatedVaultCode();
    if (!vaultCode) {
      return NextResponse.json({ error: "Unauthorized vault session" }, { status: 401 });
    }

    const body = await req.json();
    const { action, type, payload, id } = body;
    const supabase = getServiceSupabase();

    if (action === "wipe_vault_data") {
      const { error: txErr } = await supabase
        .from("transactions")
        .delete()
        .eq("vault_code", vaultCode);
      if (txErr) throw txErr;

      const { error: catErr } = await supabase
        .from("categories")
        .delete()
        .eq("vault_code", vaultCode);
      if (catErr) throw catErr;

      return NextResponse.json({ success: true });
    }

    if (action === "delete_vault_permanently") {
      const { error: txErr } = await supabase
        .from("transactions")
        .delete()
        .eq("vault_code", vaultCode);
      if (txErr) throw txErr;

      const { error: catErr } = await supabase
        .from("categories")
        .delete()
        .eq("vault_code", vaultCode);
      if (catErr) throw catErr;

      const { error: codeErr } = await supabase
        .from("vault_codes")
        .delete()
        .eq("code", vaultCode);
      if (codeErr) throw codeErr;

      const cookieStore = await cookies();
      cookieStore.delete("sb-vault-token");

      return NextResponse.json({ success: true });
    }

    if (type === "transaction") {
      if (action === "insert") {
        const item = {
          ...payload,
          vault_code: vaultCode,
          occurred_at: payload.occurred_at || new Date().toISOString(),
        };
        const { data, error } = await supabase.from("transactions").insert(item).select().single();
        if (error) throw error;
        return NextResponse.json({ data });
      } else if (action === "update") {
        const { error } = await supabase
          .from("transactions")
          .update({ ...payload, updated_at: new Date().toISOString() })
          .eq("id", id)
          .eq("vault_code", vaultCode);
        if (error) throw error;
        return NextResponse.json({ success: true });
      } else if (action === "delete") {
        const { error } = await supabase
          .from("transactions")
          .delete()
          .eq("id", id)
          .eq("vault_code", vaultCode);
        if (error) throw error;
        return NextResponse.json({ success: true });
      }
    } else if (type === "category") {
      if (action === "insert") {
        const item = {
          ...payload,
          vault_code: vaultCode,
        };
        const { data, error } = await supabase.from("categories").insert(item).select().single();
        if (error) throw error;
        return NextResponse.json({ data });
      } else if (action === "update") {
        const { error } = await supabase
          .from("categories")
          .update(payload)
          .eq("id", id)
          .eq("vault_code", vaultCode);
        if (error) throw error;
        return NextResponse.json({ success: true });
      } else if (action === "delete") {
        const { error } = await supabase
          .from("categories")
          .delete()
          .eq("id", id)
          .eq("vault_code", vaultCode);
        if (error) throw error;
        return NextResponse.json({ success: true });
      }
    } else if (type === "vault_label") {
      const { error } = await supabase
        .from("vault_codes")
        .update({ label: payload.label })
        .eq("code", vaultCode);
      if (error) throw error;
      return NextResponse.json({ success: true });
    }

    return NextResponse.json({ error: "Invalid action/type" }, { status: 400 });
  } catch (err: any) {
    console.error("API vault data POST error:", err);
    return NextResponse.json({ error: err.message || "Operation failed" }, { status: 500 });
  }
}
