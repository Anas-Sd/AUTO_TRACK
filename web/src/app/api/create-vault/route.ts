import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

export async function POST() {
  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || "";

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let code = "";
    for (let i = 0; i < 8; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }

    const { data, error } = await supabase
      .from("vault_codes")
      .insert({ code, label: "My Vault" })
      .select("code")
      .single();

    if (error) {
      console.error("Error creating vault in Supabase:", error);
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    // Seed default categories ONCE during vault creation
    const DEFAULT_CATS = [
      { vault_code: data.code, name: "Food & Dining", icon: "🍔", color: "#F59E0B", monthly_cap: 10000 },
      { vault_code: data.code, name: "Shopping", icon: "🛍️", color: "#EC4899", monthly_cap: 8000 },
      { vault_code: data.code, name: "Bills & Utilities", icon: "⚡", color: "#3B82F6", monthly_cap: 5000 },
      { vault_code: data.code, name: "Transportation", icon: "🚗", color: "#10B981", monthly_cap: 4000 },
      { vault_code: data.code, name: "Entertainment", icon: "🎬", color: "#8B5CF6", monthly_cap: 3000 },
      { vault_code: data.code, name: "Health & Care", icon: "💊", color: "#EF4444", monthly_cap: 5000 },
      { vault_code: data.code, name: "Salary & Income", icon: "💰", color: "#10B981", monthly_cap: null },
      { vault_code: data.code, name: "Investments", icon: "📈", color: "#06B6D4", monthly_cap: null },
    ];
    await supabase.from("categories").insert(DEFAULT_CATS);

    return NextResponse.json({ code: data.code });
  } catch (err: any) {
    console.error("Create vault route exception:", err);
    return NextResponse.json({ error: err.message || "Failed to create vault" }, { status: 500 });
  }
}
