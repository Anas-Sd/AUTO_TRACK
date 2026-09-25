import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

export const dynamic = "force-dynamic";
export const maxDuration = 60;

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || "";
const CRON_SECRET = process.env.CRON_SECRET || "";

function getServiceSupabase() {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
  const serviceRoleKey =
    process.env.SUPABASE_SERVICE_ROLE_KEY ||
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4ODU4MTQ5MSwiZXhwIjoyMTA0MTU3NDkxfQ.jKXBvbUv9MwHGetQ4TU1AfRTJzJACCiccJiATJZgVRI";
  return createClient(supabaseUrl, serviceRoleKey);
}

async function sendTelegramMessage(chatId: string | number, text: string) {
  const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      chat_id: chatId,
      text: text,
      parse_mode: "Markdown",
    }),
  });
  return res.json();
}

export async function GET(req: Request) {
  try {
    const url = new URL(req.url);
    const authHeader = req.headers.get("authorization");
    const secretParam = url.searchParams.get("secret");

    // If CRON_SECRET is configured, enforce authorization (via Bearer token or ?secret=...)
    if (CRON_SECRET) {
      const isAuthorized =
        authHeader === `Bearer ${CRON_SECRET}` || secretParam === CRON_SECRET;
      if (!isAuthorized) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
    }

    if (!TELEGRAM_BOT_TOKEN) {
      return NextResponse.json({ error: "TELEGRAM_BOT_TOKEN is not configured" }, { status: 500 });
    }

    const supabase = getServiceSupabase();

    // Fetch all connected Telegram users from the categories table
    const { data: connectedUsers, error } = await supabase
      .from("categories")
      .select("name, color, vault_code, icon")
      .ilike("name", "_TG_%");

    if (error) {
      console.error("Failed to query connected Telegram users:", error);
      return NextResponse.json({ error: error.message }, { status: 500 });
    }

    if (!connectedUsers || connectedUsers.length === 0) {
      return NextResponse.json({
        message: "No connected Telegram users found.",
        remindersSent: 0,
      });
    }

    const results: Array<{ chatId: string; userName: string; status: string }> = [];
    const seenChatIds = new Set<string>();

    for (const record of connectedUsers) {
      let chatId = record.name.replace(/^_TG_/i, "").trim();
      let userName = record.color || "Friend";

      if (record.icon) {
        try {
          const meta = typeof record.icon === "string" ? JSON.parse(record.icon) : record.icon;
          if (meta.chatId) chatId = String(meta.chatId);
          if (meta.userName) userName = meta.userName;
        } catch {
          // ignore parsing error
        }
      }

      if (!chatId || seenChatIds.has(chatId)) continue;
      seenChatIds.add(chatId);

      const reminderMessage =
        `👤 *${userName}*\n\n` +
        `🌙 *Daily Cash Expense Reminder* (10:00 PM)\n\n` +
        `Hey *${userName}*! Did you make any cash payments today that you haven't logged yet? 💵\n\n` +
        `*Common daily cash spends:*\n` +
        `• ☕ *Chai / Coffee / Snacks*\n` +
        `• 🛺 *Auto / Cab / Travel fare*\n` +
        `• 🛒 *Local groceries / Vegetables*\n` +
        `• 🪙 *Tips / Donations / Small buys*\n\n` +
        `Reply directly to this message to log it instantly:\n` +
        `👉 *"50 cash for tea"*\n` +
        `👉 *"120 cash for auto ride"*\n` +
        `👉 *"40 cash for juice under snacks"*\n\n` +
        `_(If you had no cash expenses today, you're all set! ✨)_`;

      try {
        await sendTelegramMessage(chatId, reminderMessage);
        results.push({ chatId, userName, status: "sent" });
      } catch (sendErr: any) {
        console.error(`Failed to send reminder to chatId ${chatId}:`, sendErr);
        results.push({ chatId, userName, status: `failed: ${sendErr?.message || sendErr}` });
      }
    }

    return NextResponse.json({
      success: true,
      timestamp: new Date().toISOString(),
      remindersSent: results.filter((r) => r.status === "sent").length,
      details: results,
    });
  } catch (err: any) {
    console.error("Daily reminder cron error:", err);
    return NextResponse.json({ error: err.message || "Internal error" }, { status: 500 });
  }
}
