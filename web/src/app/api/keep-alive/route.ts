import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

export async function GET() {
  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
    const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1ODE0OTEsImV4cCI6MjEwNDE1NzQ5MX0.chEhUh4KKaTQGL4beE6ZSf6V65aiWBHvW3cLXMmmQhE";

    const supabase = createClient(supabaseUrl, anonKey);
    const { data, error } = await supabase.from("vault_codes").select("code").limit(1);

    return NextResponse.json({
      status: "alive",
      timestamp: new Date().toISOString(),
      databaseActive: !error
    });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || "Ping failed" }, { status: 500 });
  }
}
