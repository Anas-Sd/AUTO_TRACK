import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { create, getNumericDate } from "https://deno.land/x/djwt@v2.8/mod.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const body = await req.json().catch(() => ({}));
    const code = (body.code || "").toString().trim().toUpperCase();
    if (!code) {
      return new Response(JSON.stringify({ error: "Vault code is required" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    const jwtSecret = Deno.env.get("SUPABASE_JWT_SECRET") || "";

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    // Verify vault code exists
    const { data: vault, error } = await supabase
      .from("vault_codes")
      .select("code, label")
      .eq("code", code)
      .single();

    if (error || !vault) {
      return new Response(JSON.stringify({ error: "Invalid Vault Code" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      });
    }

    // Update last_accessed timestamp
    await supabase
      .from("vault_codes")
      .update({ last_accessed: new Date().toISOString() })
      .eq("code", vault.code);

    // Generate JWT signed with SUPABASE_JWT_SECRET
    const keyBuf = new TextEncoder().encode(jwtSecret);
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyBuf,
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const payload = {
      role: "authenticated",
      vault_code: vault.code,
      exp: getNumericDate(60 * 60 * 24 * 30), // 30 days
    };

    const token = await create({ alg: "HS256", typ: "JWT" }, payload, cryptoKey);

    return new Response(JSON.stringify({ token, vault_code: vault.code, label: vault.label }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message || "Session issue failed" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 400,
    });
  }
});
