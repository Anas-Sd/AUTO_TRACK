import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

// Credentials read from Environment Variables (Vercel / .env.local)
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || "";
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const DEFAULT_VAULT_CODE = process.env.DEFAULT_VAULT_CODE || "ANAS4455";

function getServiceSupabase() {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://kdiefrqgmoahpfcstbzc.supabase.co";
  const serviceRoleKey =
    process.env.SUPABASE_SERVICE_ROLE_KEY ||
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4ODU4MTQ5MSwiZXhwIjoyMTA0MTU3NDkxfQ.jKXBvbUv9MwHGetQ4TU1AfRTJzJACCiccJiATJZgVRI";
  return createClient(supabaseUrl, serviceRoleKey);
}

async function sendTelegramMessage(chatId: number | string, text: string) {
  try {
    const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
    await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: chatId,
        text: text,
        parse_mode: "Markdown",
      }),
    });
  } catch (err) {
    console.error("Failed to send Telegram message:", err);
  }
}

// Function tools for Gemini Function Calling
const GEMINI_TOOLS = [
  {
    functionDeclarations: [
      {
        name: "add_transaction",
        description: "Add a new transaction (expense or income) to the ledger.",
        parameters: {
          type: "OBJECT",
          properties: {
            amount: { type: "NUMBER", description: "Transaction amount in INR" },
            note: { type: "STRING", description: "Note, description, or vendor name (e.g. ice cream, coffee, rent)" },
            category_name: { type: "STRING", description: "Category name specified or inferred" },
            type: { type: "STRING", enum: ["expense", "income"], description: "Transaction type" },
            occurred_at: { type: "STRING", description: "ISO date string or relative date if specified" },
          },
          required: ["amount", "type"],
        },
      },
      {
        name: "delete_transaction",
        description: "Delete a transaction from the ledger by position, note/vendor, or amount.",
        parameters: {
          type: "OBJECT",
          properties: {
            transaction_id: { type: "STRING", description: "Direct transaction ID if known" },
            position_index: { type: "NUMBER", description: "1-based index in the recent ledger (e.g. 3 for 3rd log)" },
            note_query: { type: "STRING", description: "Search query matching transaction note/vendor (e.g. chocolates)" },
            amount: { type: "NUMBER", description: "Target transaction amount to match" },
          },
        },
      },
      {
        name: "manage_category",
        description: "Create, rename, or delete a category in AutoTrack.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", enum: ["create", "delete", "update"], description: "Action to perform" },
            category_name: { type: "STRING", description: "Target category name" },
            new_category_name: { type: "STRING", description: "New name if updating" },
          },
          required: ["action", "category_name"],
        },
      },
      {
        name: "query_summary",
        description: "Query expense and income breakdown or summary for a specific month, timeframe, or overall.",
        parameters: {
          type: "OBJECT",
          properties: {
            timeframe: { type: "STRING", description: "e.g. this_month, last_month, today, September 2026" },
            month_name: { type: "STRING", description: "e.g. September, October, January" },
            year: { type: "NUMBER", description: "e.g. 2026" },
          },
        },
      },
      {
        name: "ask_user_clarification",
        description: "Ask the user a clarifying question when critical parameters like category or amount are ambiguous or missing.",
        parameters: {
          type: "OBJECT",
          properties: {
            question: { type: "STRING", description: "Clarifying question to send to the user" },
          },
          required: ["question"],
        },
      },
    ],
  },
];

export async function POST(req: Request) {
  try {
    const body = await req.json();

    // Verify valid Telegram message
    const message = body?.message;
    if (!message || !message.text) {
      return NextResponse.json({ status: "ignored" });
    }

    const chatId = message.chat.id;
    const userMessage = message.text.trim();

    // Handle /start, greetings, or help commands directly
    const lowerUserMsg = userMessage.toLowerCase().trim();
    const isGreeting =
      userMessage.startsWith("/") ||
      /^(hi+|hello+|hey+|hlo+|help|good\s*morning|good\s*evening|good\s*afternoon)/i.test(lowerUserMsg);

    if (isGreeting) {
      await sendTelegramMessage(
        chatId,
        `👋 *Hello! I'm your AutoTrack AI Assistant.*\n\nYou can talk to me naturally in plain English to manage your expense ledger. Here are some things you can try:\n\n` +
          `• *"50 rs for ice cream under regular expenses"*\n` +
          `• *"Add 50000 as salary credited"*\n` +
          `• *"Add 50 rs as donation"* (I'll ask for category if missing!)\n` +
          `• *"Delete 3rd log in ledger"* or *"Delete sports category"*\n` +
          `• *"Give me monthly expenses of September"*`
      );
      return NextResponse.json({ status: "ok" });
    }

    const supabase = getServiceSupabase();

    // 1. Fetch current categories for vault
    const { data: categories } = await supabase
      .from("categories")
      .select("*")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("created_at", { ascending: true });

    // 2. Fetch recent transactions for vault context
    const { data: recentTransactions } = await supabase
      .from("transactions")
      .select("*, categories(name)")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("occurred_at", { ascending: false })
      .limit(20);

    const categoryNamesList = (categories || []).map((c) => c.name).join(", ");
    const recentLedgerFormatted = (recentTransactions || [])
      .map(
        (t, idx) =>
          `[${idx + 1}] ID:${t.id} | ${t.type.toUpperCase()} | ₹${t.amount} | Note: "${t.note || t.receiver_vendor || "N/A"}" | Category: ${
            t.categories?.name || "Uncategorized"
          } | Date: ${t.occurred_at?.substring(0, 10)}`
      )
      .join("\n");

    const systemPrompt = `You are the AutoTrack AI Assistant for personal finance ledger management.
Your active Vault Code is: "${DEFAULT_VAULT_CODE}".
Current Date: ${new Date().toISOString().substring(0, 10)}.

Available Categories in Vault:
[ ${categoryNamesList || "None"} ]

Recent Ledger Transactions (Ordered 1-based, newest first):
${recentLedgerFormatted || "No transactions recorded yet."}

INSTRUCTIONS:
1. When user wants to log a transaction (e.g. "50 rs for ice cream under regular expenses"):
   - Choose 'add_transaction'.
   - If category is mentioned or clear, map to an existing category if possible, or provide category_name.
   - If category is missing or ambiguous (e.g. "add 50 rs as donation" where donation is not in categories), choose 'ask_user_clarification' to ask which category to log under or if a new category should be created.
2. When user wants to delete a transaction (e.g. "delete 3rd log in ledger", "delete chocolates log"):
   - Choose 'delete_transaction' with position_index (e.g. 3) or note_query (e.g. "chocolates") or transaction_id.
3. When user wants to manage categories (e.g. "delete sports category"):
   - Choose 'manage_category' with action "delete", "create", or "update".
4. When user asks for expense breakdown/summary (e.g. "monthly expenses of September"):
   - Choose 'query_summary'.
5. Always select a function call tool matching user intent.`;

    // Call Gemini API via REST with fallback models (gemini-2.5-flash, gemini-flash-latest, etc.)
    const candidateModels = ["gemini-2.5-flash", "gemini-flash-latest", "gemini-2.5-flash-lite"];
    let geminiRes: Response | null = null;
    let lastErrorText = "";

    for (const model of candidateModels) {
      try {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
        const res = await fetch(geminiUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [
              {
                role: "user",
                parts: [{ text: userMessage }],
              },
            ],
            systemInstruction: {
              parts: [{ text: systemPrompt }],
            },
            tools: GEMINI_TOOLS,
          }),
        });

        if (res.ok) {
          geminiRes = res;
          break;
        } else {
          lastErrorText = await res.text();
          console.warn(`Gemini model ${model} returned error:`, lastErrorText);
        }
      } catch (e: any) {
        lastErrorText = e.message || String(e);
      }
    }

    if (!geminiRes || !geminiRes.ok) {
      console.error("All Gemini API candidate models failed:", lastErrorText);
      let errMsg = `⚠️ AI Service temporarily unavailable. Please try again.`;
      if (lastErrorText.includes("API key not valid")) {
        errMsg = `⚠️ Gemini API Key invalid. Please verify your GEMINI_API_KEY environment variable in Vercel (make sure it starts with 'AQ.').`;
      }
      await sendTelegramMessage(chatId, errMsg);
      return NextResponse.json({ error: "Gemini call failed" }, { status: 500 });
    }

    const geminiData = await geminiRes.json();
    const candidate = geminiData.candidates?.[0];
    const functionCall = candidate?.content?.parts?.find((p: any) => p.functionCall)?.functionCall;

    if (!functionCall) {
      // Direct text response fallback
      const textReply = candidate?.content?.parts?.[0]?.text || "I'm sorry, I couldn't understand that request.";
      await sendTelegramMessage(chatId, textReply);
      return NextResponse.json({ status: "ok" });
    }

    const { name, args } = functionCall;

    // --- TOOL EXECUTION SWITCH ---

    if (name === "ask_user_clarification") {
      await sendTelegramMessage(chatId, args.question || "Could you please clarify your request?");
      return NextResponse.json({ status: "ok" });
    }

    if (name === "add_transaction") {
      let targetCategoryId: string | null = null;
      let matchedCategoryName = "Uncategorized";

      if (args.category_name && categories && categories.length > 0) {
        const queryCatLower = args.category_name.toLowerCase().trim();
        const found = categories.find(
          (c) => c.name.toLowerCase().trim() === queryCatLower || c.name.toLowerCase().includes(queryCatLower)
        );

        if (found) {
          targetCategoryId = found.id;
          matchedCategoryName = found.name;
        } else {
          // Create new category if explicitly requested
          const { data: newCat, error: catErr } = await supabase
            .from("categories")
            .insert({
              vault_code: DEFAULT_VAULT_CODE,
              name: args.category_name.trim(),
              icon: "Category",
              color: "#3B82F6",
            })
            .select()
            .single();

          if (!catErr && newCat) {
            targetCategoryId = newCat.id;
            matchedCategoryName = newCat.name;
          }
        }
      }

      const newTx = {
        vault_code: DEFAULT_VAULT_CODE,
        amount: args.amount,
        type: args.type || "expense",
        note: args.note || null,
        receiver_vendor: args.note || null,
        category_id: targetCategoryId,
        source_app: "Telegram Bot",
        occurred_at: args.occurred_at || new Date().toISOString(),
      };

      const { data: inserted, error: insertErr } = await supabase
        .from("transactions")
        .insert(newTx)
        .select()
        .single();

      if (insertErr) {
        await sendTelegramMessage(chatId, `❌ Failed to add transaction: ${insertErr.message}`);
      } else {
        const symbol = args.type === "income" ? "📈" : "💸";
        const noteStr = args.note ? ` as "${args.note}"` : "";
        await sendTelegramMessage(
          chatId,
          `✅ *Transaction Added Successfully!*\n\n` +
            `${symbol} *Amount:* ₹${args.amount}\n` +
            `📝 *Note:* ${args.note || "N/A"}\n` +
            `🏷️ *Category:* ${matchedCategoryName}\n` +
            `📅 *Date:* ${new Date(inserted.occurred_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}`
        );
      }
      return NextResponse.json({ status: "ok" });
    }

    if (name === "delete_transaction") {
      let targetTxId: string | null = args.transaction_id || null;
      let deletedInfo = "";

      if (!targetTxId && args.position_index && recentTransactions) {
        const target = recentTransactions[args.position_index - 1];
        if (target) {
          targetTxId = target.id;
          deletedInfo = `₹${target.amount} ("${target.note || target.receiver_vendor || "N/A"}")`;
        }
      }

      if (!targetTxId && args.note_query && recentTransactions) {
        const q = args.note_query.toLowerCase();
        const target = recentTransactions.find(
          (t) => (t.note && t.note.toLowerCase().includes(q)) || (t.receiver_vendor && t.receiver_vendor.toLowerCase().includes(q))
        );
        if (target) {
          targetTxId = target.id;
          deletedInfo = `₹${target.amount} ("${target.note || target.receiver_vendor || "N/A"}")`;
        }
      }

      if (!targetTxId && args.amount && recentTransactions) {
        const target = recentTransactions.find((t) => t.amount === args.amount);
        if (target) {
          targetTxId = target.id;
          deletedInfo = `₹${target.amount} ("${target.note || target.receiver_vendor || "N/A"}")`;
        }
      }

      if (!targetTxId) {
        await sendTelegramMessage(chatId, `⚠️ Could not find a matching transaction to delete.`);
        return NextResponse.json({ status: "ok" });
      }

      const { error: delErr } = await supabase
        .from("transactions")
        .delete()
        .eq("id", targetTxId)
        .eq("vault_code", DEFAULT_VAULT_CODE);

      if (delErr) {
        await sendTelegramMessage(chatId, `❌ Failed to delete transaction: ${delErr.message}`);
      } else {
        await sendTelegramMessage(chatId, `🗑️ *Transaction deleted successfully!* ${deletedInfo}`);
      }
      return NextResponse.json({ status: "ok" });
    }

    if (name === "manage_category") {
      const { action, category_name, new_category_name } = args;
      const targetCat = (categories || []).find((c) => c.name.toLowerCase().trim() === category_name.toLowerCase().trim());

      if (action === "create") {
        const { data: created, error } = await supabase
          .from("categories")
          .insert({
            vault_code: DEFAULT_VAULT_CODE,
            name: category_name,
            icon: "Category",
            color: "#3B82F6",
          })
          .select()
          .single();

        if (error) {
          await sendTelegramMessage(chatId, `❌ Failed to create category: ${error.message}`);
        } else {
          await sendTelegramMessage(chatId, `✅ Category *"${created.name}"* created successfully!`);
        }
      } else if (action === "delete") {
        if (!targetCat) {
          await sendTelegramMessage(chatId, `⚠️ Category *"${category_name}"* not found.`);
          return NextResponse.json({ status: "ok" });
        }

        // 1. Unlink transactions
        await supabase
          .from("transactions")
          .update({ category_id: null })
          .eq("category_id", targetCat.id)
          .eq("vault_code", DEFAULT_VAULT_CODE);

        // 2. Delete category
        const { error } = await supabase
          .from("categories")
          .delete()
          .eq("id", targetCat.id)
          .eq("vault_code", DEFAULT_VAULT_CODE);

        if (error) {
          await sendTelegramMessage(chatId, `❌ Failed to delete category: ${error.message}`);
        } else {
          await sendTelegramMessage(chatId, `🗑️ Category *"${targetCat.name}"* deleted. All associated transactions are now Uncategorized.`);
        }
      } else if (action === "update" && new_category_name) {
        if (!targetCat) {
          await sendTelegramMessage(chatId, `⚠️ Category *"${category_name}"* not found.`);
          return NextResponse.json({ status: "ok" });
        }

        const { error } = await supabase
          .from("categories")
          .update({ name: new_category_name })
          .eq("id", targetCat.id)
          .eq("vault_code", DEFAULT_VAULT_CODE);

        if (error) {
          await sendTelegramMessage(chatId, `❌ Failed to rename category: ${error.message}`);
        } else {
          await sendTelegramMessage(chatId, `✏️ Category renamed from *"${targetCat.name}"* to *"${new_category_name}"*.`);
        }
      }
      return NextResponse.json({ status: "ok" });
    }

    if (name === "query_summary") {
      // Fetch all transactions for analysis
      const { data: allTx, error: txErr } = await supabase
        .from("transactions")
        .select("*, categories(name)")
        .eq("vault_code", DEFAULT_VAULT_CODE)
        .order("occurred_at", { ascending: false });

      if (txErr || !allTx) {
        await sendTelegramMessage(chatId, `❌ Failed to fetch expenses summary.`);
        return NextResponse.json({ status: "ok" });
      }

      // Filter by specified timeframe/month if present
      let filtered = allTx;
      const now = new Date();

      if (args.month_name || args.timeframe === "this_month") {
        const targetMonth = args.month_name ? args.month_name.toLowerCase() : null;
        filtered = allTx.filter((t) => {
          const d = new Date(t.occurred_at);
          if (targetMonth) {
            const monthStr = d.toLocaleString("default", { month: "long" }).toLowerCase();
            return monthStr === targetMonth;
          }
          return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
        });
      }

      let totalIncome = 0;
      let totalExpense = 0;
      const categoryBreakdown: Record<string, number> = {};

      filtered.forEach((t) => {
        if (t.type === "income") {
          totalIncome += Number(t.amount || 0);
        } else {
          totalExpense += Number(t.amount || 0);
          const catName = t.categories?.name || "Uncategorized";
          categoryBreakdown[catName] = (categoryBreakdown[catName] || 0) + Number(t.amount || 0);
        }
      });

      const netBalance = totalIncome - totalExpense;
      const periodLabel = args.month_name ? args.month_name : "This Month";

      let catBreakdownStr = "";
      Object.entries(categoryBreakdown).forEach(([cat, amt]) => {
        catBreakdownStr += `  ▫️ *${cat}:* ₹${amt.toLocaleString("en-IN")}\n`;
      });

      await sendTelegramMessage(
        chatId,
        `📊 *Expense Summary Report (${periodLabel})*\n\n` +
          `💸 *Total Expenses:* ₹${totalExpense.toLocaleString("en-IN")}\n` +
          `📈 *Total Income:* ₹${totalIncome.toLocaleString("en-IN")}\n` +
          `💰 *Net Savings:* ₹${netBalance.toLocaleString("en-IN")}\n\n` +
          `🏷️ *Category Breakdown:*\n${catBreakdownStr || "  ▫️ No expenses recorded."}`
      );
      return NextResponse.json({ status: "ok" });
    }

    return NextResponse.json({ status: "ok" });
  } catch (err: any) {
    console.error("Telegram webhook error:", err);
    return NextResponse.json({ error: err.message || "Internal error" }, { status: 500 });
  }
}

export async function GET() {
  return NextResponse.json({ status: "Telegram Webhook Endpoint Ready" });
}
