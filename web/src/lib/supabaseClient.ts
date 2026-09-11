import { createClient } from "@supabase/supabase-js";

export const SUPABASE_URL =
  process.env.NEXT_PUBLIC_SUPABASE_URL ||
  process.env.VITE_SUPABASE_URL ||
  "https://kdiefrqgmoahpfcstbzc.supabase.co";

export const SUPABASE_ANON_KEY =
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
  process.env.VITE_SUPABASE_ANON_KEY ||
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1ODE0OTEsImV4cCI6MjEwNDE1NzQ5MX0.chEhUh4KKaTQGL4beE6ZSf6V65aiWBHvW3cLXMmmQhE";

export const supabaseAnon = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export function getSupabaseClient(token?: string | null) {
  if (!token) return supabaseAnon;
  return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    global: {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  });
}

export const DEFAULT_CATEGORIES = [
  { name: "Food & Dining", icon: "🍔", color: "#F59E0B", monthly_cap: 10000 },
  { name: "Shopping", icon: "🛍️", color: "#EC4899", monthly_cap: 8000 },
  { name: "Bills & Utilities", icon: "⚡", color: "#3B82F6", monthly_cap: 5000 },
  { name: "Transportation", icon: "🚗", color: "#10B981", monthly_cap: 4000 },
  { name: "Entertainment", icon: "🎬", color: "#8B5CF6", monthly_cap: 3000 },
  { name: "Health & Care", icon: "💊", color: "#EF4444", monthly_cap: 5000 },
  { name: "Salary & Income", icon: "💰", color: "#10B981", monthly_cap: null },
  { name: "Investments", icon: "📈", color: "#06B6D4", monthly_cap: null },
];
