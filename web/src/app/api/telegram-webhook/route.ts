import { NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

// Credentials read from Environment Variables
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

async function sendTypingAction(chatId: number | string) {
  try {
    const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendChatAction`;
    await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: chatId,
        action: "typing",
      }),
    });
  } catch (err) {
    // ignore typing action errors
  }
}

async function sendTelegramDocument(chatId: number | string, content: string, fileName: string, caption: string) {
  try {
    const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument`;
    const formData = new FormData();
    formData.append("chat_id", String(chatId));
    formData.append("caption", caption);
    formData.append("parse_mode", "Markdown");

    const fileBlob = new Blob([content], { type: "text/csv;charset=utf-8;" });
    formData.append("document", fileBlob, fileName);

    await fetch(url, {
      method: "POST",
      body: formData,
    });
  } catch (err) {
    console.error("Failed to send Telegram document:", err);
  }
}

interface ChatTurn {
  role: "user" | "model";
  parts: Array<{ text: string }>;
}

async function getChatMemory(supabase: any): Promise<ChatTurn[]> {
  try {
    const { data } = await supabase
      .from("categories")
      .select("icon")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .eq("name", "_BOT_MEMORY_")
      .maybeSingle();

    if (data && data.icon) {
      const parsed = typeof data.icon === "string" ? JSON.parse(data.icon) : data.icon;
      if (Array.isArray(parsed)) {
        const cleanTurns: ChatTurn[] = [];
        for (const item of parsed) {
          if (!item) continue;
          const role = item.role === "user" || item.role === "model" ? item.role : null;
          if (!role) continue;

          let text = "";
          if (Array.isArray(item.parts) && item.parts.length > 0) {
            text = item.parts
              .map((p: any) => (p && typeof p.text === "string" ? p.text : ""))
              .filter(Boolean)
              .join("\n")
              .trim();
          } else if (typeof item.text === "string") {
            text = item.text.trim();
          }

          if (!text) continue;

          if (cleanTurns.length === 0) {
            if (role === "user") {
              cleanTurns.push({ role: "user", parts: [{ text }] });
            }
          } else {
            const lastTurn = cleanTurns[cleanTurns.length - 1];
            if (lastTurn.role !== role) {
              cleanTurns.push({ role, parts: [{ text }] });
            } else {
              lastTurn.parts[0].text += `\n${text}`;
            }
          }
        }

        // Must end with model turn so appending incoming user turn alternates properly
        while (cleanTurns.length > 0 && cleanTurns[cleanTurns.length - 1].role === "user") {
          cleanTurns.pop();
        }

        return cleanTurns;
      }
    }
  } catch (e) {
    // ignore memory read error
  }
  return [];
}

async function saveChatMemory(supabase: any, memory: ChatTurn[]) {
  try {
    const sanitized = memory
      .filter(
        (t) =>
          t &&
          (t.role === "user" || t.role === "model") &&
          Array.isArray(t.parts) &&
          t.parts.length > 0 &&
          typeof t.parts[0]?.text === "string" &&
          t.parts[0].text.trim().length > 0
      )
      .slice(-10);

    await supabase.from("categories").upsert(
      {
        vault_code: DEFAULT_VAULT_CODE,
        name: "_BOT_MEMORY_",
        icon: JSON.stringify(sanitized),
      },
      { onConflict: "vault_code,name" }
    );
  } catch (e) {
    // ignore memory save error
  }
}

// Function tools for Gemini 2.5 Function Calling
const GEMINI_TOOLS = [
  {
    functionDeclarations: [
      {
        name: "add_transactions",
        description: "Add one or multiple transactions (expense or income) to the ledger at once.",
        parameters: {
          type: "OBJECT",
          properties: {
            items: {
              type: "ARRAY",
              description: "List of transactions to add",
              items: {
                type: "OBJECT",
                properties: {
                  amount: { type: "NUMBER", description: "Transaction amount in INR" },
                  note: { type: "STRING", description: "Note, description, or vendor (e.g. petrol, ice cream, salary)" },
                  category_name: { type: "STRING", description: "Category name. If category doesn't exist, it will be created." },
                  type: { type: "STRING", enum: ["expense", "income"], description: "Transaction type. Default is 'expense'. Choose 'income' only if explicitly stated as income, salary, or received money." },
                  payment_method: { type: "STRING", description: "UPI (default) or Cash" },
                  occurred_at: { type: "STRING", description: "ISO date string or relative date if specified" },
                },
                required: ["amount", "note", "type"],
              },
            },
          },
          required: ["items"],
        },
      },
      {
        name: "update_transaction",
        description: "Update an existing transaction's amount, note, category, or type.",
        parameters: {
          type: "OBJECT",
          properties: {
            transaction_id: { type: "STRING", description: "Direct transaction ID if known" },
            position_index: { type: "NUMBER", description: "1-based index in the recent ledger (e.g. 1 for latest transaction)" },
            note_query: { type: "STRING", description: "Search query matching transaction note/vendor (e.g. petrol)" },
            amount: { type: "NUMBER", description: "Amount of transaction to match" },
            new_amount: { type: "NUMBER", description: "New amount value if updating amount" },
            new_note: { type: "STRING", description: "New note or vendor description" },
            new_category_name: { type: "STRING", description: "New category name to move transaction into" },
            new_type: { type: "STRING", enum: ["expense", "income"], description: "New type" },
          },
        },
      },
      {
        name: "delete_transaction",
        description: "Delete a transaction from the ledger by position, note/vendor, or amount.",
        parameters: {
          type: "OBJECT",
          properties: {
            transaction_id: { type: "STRING", description: "Direct transaction ID if known" },
            position_index: { type: "NUMBER", description: "1-based index in the recent ledger (e.g. 1 for latest transaction)" },
            note_query: { type: "STRING", description: "Search query matching transaction note/vendor (e.g. chocolates)" },
            amount: { type: "NUMBER", description: "Target transaction amount to match" },
          },
        },
      },
      {
        name: "manage_categories",
        description: "Create, rename, update properties (monthly cap), or delete one or multiple categories.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", enum: ["create", "update", "delete"], description: "Action to perform" },
            items: {
              type: "ARRAY",
              description: "List of categories to manage",
              items: {
                type: "OBJECT",
                properties: {
                  name: { type: "STRING", description: "Target or new category name" },
                  new_name: { type: "STRING", description: "New name if renaming" },
                  monthly_cap: { type: "NUMBER", description: "Monthly budget cap amount" },
                },
                required: ["name"],
              },
            },
          },
          required: ["action", "items"],
        },
      },
      {
        name: "query_overview_analytics",
        description: "Query expense, income, latest transaction, daily/monthly breakdown, or custom date range analytics.",
        parameters: {
          type: "OBJECT",
          properties: {
            query_type: {
              type: "STRING",
              enum: [
                "latest_transaction",
                "summary",
                "category_breakdown",
                "top_spending",
                "filtered_range",
                "list_categories",
                "list_transactions",
                "transaction_count",
              ],
              description: "Type of data query",
            },
            timeframe: { type: "STRING", description: "e.g. today, yesterday, this_week, this_month, last_month, September 2026, all" },
            start_date: { type: "STRING", description: "Start date (YYYY-MM-DD)" },
            end_date: { type: "STRING", description: "End date (YYYY-MM-DD)" },
            category_name: { type: "STRING", description: "Filter by specific category if requested" },
          },
          required: ["query_type"],
        },
      },
      {
        name: "undo_last_action",
        description: "Undo or revert the most recent transaction log or action performed.",
        parameters: {
          type: "OBJECT",
          properties: {
            reason: { type: "STRING", description: "Context if specified" },
          },
        },
      },
      {
        name: "export_transactions_file",
        description: "Generate and send a downloadable CSV file attachment of transactions filtered by date, timeframe, or category.",
        parameters: {
          type: "OBJECT",
          properties: {
            timeframe: { type: "STRING", description: "e.g. today, yesterday, this_week, this_month, last_month, September 2026, all" },
            category_name: { type: "STRING", description: "Filter by specific category if requested" },
            sort_by: { type: "STRING", enum: ["date_desc", "date_asc", "amount_desc"], description: "Sorting order" },
          },
        },
      },
      {
        name: "ask_user_clarification",
        description: "Ask the user a clarifying question when crucial parameters like amount, note, or category are missing or ambiguous.",
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

    const message = body?.message;
    if (!message || !message.text) {
      return NextResponse.json({ status: "ignored" });
    }

    const chatId = message.chat.id;
    const userMessage = message.text.trim();

    // 1. Show typing status in Telegram immediately
    await sendTypingAction(chatId);

    // Handle /start or pure greeting commands directly
    const lowerUserMsg = userMessage.toLowerCase().trim();
    const cleanMsg = lowerUserMsg.replace(/[!.,?]+$/, "").trim();
    const hasCommandKeywords = /\b(add|log|record|spend|spent|paid|received|delete|remove|update|change|create|list|export|show|what|whats|how|much|rs|inr|₹)\b/i.test(cleanMsg);
    const isGreeting =
      !hasCommandKeywords &&
      (userMessage.startsWith("/start") ||
        userMessage.startsWith("/help") ||
        ["hi", "hii", "hiii", "hello", "hey", "heyy", "hlo", "help", "start"].includes(cleanMsg) ||
        /^(hi+|hello+|hey+|hlo+|namaste|assalam\s*o\s*alaikum|salam|good\s*(morning|afternoon|evening))\b/i.test(cleanMsg));

    if (isGreeting) {
      await sendTelegramMessage(
        chatId,
        `👋 *Hello! I'm your AutoTrack AI Assistant.*\n\nYou can talk to me naturally in plain English to manage your expense ledger. Here are some things you can try:\n\n` +
          `• *"50 rs for ice cream under regular expenses"*\n` +
          `• *"40 rs for friend under adjustment, 30rs for tea under clg works, 24 rs for beggar under donation"*\n` +
          `• *"Create 3 categories named x, y (with 10k opening balance), and z"*\n` +
          `• *"Update petrol transaction to clg category"*\n` +
          `• *"Whats the latest transaction?"*\n` +
          `• *"Delete 3rd log in ledger"*\n` +
          `• *"Give me monthly expenses of September"*`
      );
      return NextResponse.json({ status: "ok" });
    }

    const supabase = getServiceSupabase();

    // Fetch current categories for vault (excluding _BOT_MEMORY_)
    const { data: rawCategories } = await supabase
      .from("categories")
      .select("*")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("created_at", { ascending: true });

    const categories = (rawCategories || []).filter((c) => c.name !== "_BOT_MEMORY_");

    // Fetch recent 30 transactions for vault context
    const { data: recentTransactions } = await supabase
      .from("transactions")
      .select("*, categories(name)")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("occurred_at", { ascending: false })
      .limit(30);

    const categoryNamesList = categories.map((c) => c.name).join(", ");
    const recentLedgerFormatted = (recentTransactions || [])
      .map(
        (t, idx) =>
          `[${idx + 1}] ID:${t.id} | ${t.type.toUpperCase()} | ₹${t.amount} | Note: "${t.note || t.receiver_vendor || "N/A"}" | Category: ${
            t.categories?.name || "Uncategorized"
          } | Date: ${t.occurred_at}`
      )
      .join("\n");

    // Retrieve conversation history memory
    const chatHistory = await getChatMemory(supabase);

    const systemPrompt = `You are the AutoTrack AI Assistant for personal finance ledger management.
Your active Vault Code is: "${DEFAULT_VAULT_CODE}".
Current Date & Time: ${new Date().toISOString()}.

Available Categories in Vault:
[ ${categoryNamesList || "None"} ]

Recent Ledger Transactions (Ordered 1-based, newest first):
${recentLedgerFormatted || "No transactions recorded yet."}

CRITICAL MANDATORY RULES:
1. 3 MANDATORY FIELDS FOR EVERY TRANSACTION:
   Every transaction MUST have 3 mandatory fields:
   a) Amount (numeric value e.g. 149)
   b) Note / Description (e.g. "petrol", "ice cream")
   c) Category (e.g. "college", "food")
   If ANY of these 3 fields is missing when user asks to add a transaction (for example, "add petrol under college" which is missing the amount, or "add 100 under food" which is missing the note), YOU MUST NOT CALL 'add_transactions'! Instead, call 'ask_user_clarification' to ask for the specific missing field!

2. CONVERSATION HISTORY & FOLLOW-UP ANSWERS:
   Pay strict attention to the conversation history provided.
   If you previously asked the user for a missing field (e.g. "Could you please specify the amount for the petrol transaction?") and the user replies with a number or text (e.g. "149" or "149 rs"), treat "149" as the missing amount for that pending petrol transaction under college, and call 'add_transactions' with amount: 149, note: "petrol", category_name: "college"!

3. TRANSACTION TYPE DEFAULT (EXPENSE VS INCOME):
   Default transaction type is ALWAYS "expense"!
   Phrases like "100 for frnd 1", "50 rs for tea", "paid 200", "bought book", "100 for petrol" are ALL EXPENSES (type: "expense")!
   Choose type "income" ONLY if user explicitly says "income", "received", "got", "salary", "cashback", "credit", or "deposit"!

4. PAYMENT METHOD DEFAULT:
   Default payment method is "UPI" unless the user explicitly states "Cash".

5. ADDING TRANSACTIONS:
   - When all 3 fields are present, choose 'add_transactions'. Auto-create category if it does not exist yet.

6. UPDATING TRANSACTIONS:
   - When user asks to edit/update a transaction, choose 'update_transaction'. DO NOT choose delete_transaction!

7. DELETING TRANSACTIONS:
   - Choose 'delete_transaction' ONLY when explicitly asked to delete/remove a transaction log.

8. MANAGING CATEGORIES:
   - Choose 'manage_categories' to create, rename, or delete categories.

9. QUERYING ANALYTICS:
   - Choose 'query_overview_analytics' for totals, category lists, transaction counts, or transaction lists.`;

    // Build multi-turn contents payload
    const contentsPayload = [
      ...chatHistory,
      {
        role: "user",
        parts: [{ text: userMessage }],
      },
    ];

    // Helper to send message and persist chat memory
    async function recordAndSend(replyText: string) {
      await sendTelegramMessage(chatId, replyText);
      chatHistory.push({ role: "user", parts: [{ text: userMessage }] });
      chatHistory.push({ role: "model", parts: [{ text: replyText }] });
      await saveChatMemory(supabase, chatHistory);
    }

    // Call Gemini API via REST with candidate models (prioritize flash-lite for high availability and speed)
    const candidateModels = ["gemini-2.5-flash-lite", "gemini-2.5-flash"];
    let geminiRes: Response | null = null;
    let lastErrorText = "";

    for (const model of candidateModels) {
      for (let attempt = 0; attempt < 2; attempt++) {
        try {
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
          const res = await fetch(geminiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              contents: contentsPayload,
              systemInstruction: {
                parts: [{ text: systemPrompt }],
              },
              tools: GEMINI_TOOLS,
            }),
            signal: AbortSignal.timeout(8000),
          });

          if (res.ok) {
            geminiRes = res;
            break;
          } else {
            lastErrorText = await res.text();
            console.warn(`Gemini model ${model} (attempt ${attempt + 1}) returned error:`, lastErrorText);
            // If rate limited or service busy, wait 1.2s and retry
            if (res.status === 429 || res.status === 503) {
              if (attempt === 0) {
                await new Promise((r) => setTimeout(r, 1200));
                continue;
              }
            }
            break;
          }
        } catch (e: any) {
          lastErrorText = e.message || String(e);
          break;
        }
      }
      if (geminiRes?.ok) break;
    }

    if (!geminiRes || !geminiRes.ok) {
      console.error("All Gemini API candidate models failed:", lastErrorText);
      let errMsg = `⚠️ AI Service temporarily unavailable. Please try again.`;
      if (lastErrorText.includes("API key not valid")) {
        errMsg = `⚠️ Gemini API Key invalid. Please verify your GEMINI_API_KEY environment variable in Vercel.`;
      } else if (lastErrorText.includes("RESOURCE_EXHAUSTED") || lastErrorText.includes("quota") || lastErrorText.includes("429")) {
        errMsg = `⚠️ AI Assistant is momentarily busy. Please try again in a few seconds.`;
      }
      await sendTelegramMessage(chatId, errMsg);
      return NextResponse.json({ error: "Gemini call failed" }, { status: 500 });
    }

    const geminiData = await geminiRes.json();
    const candidate = geminiData.candidates?.[0];
    const functionCall = candidate?.content?.parts?.find((p: any) => p.functionCall)?.functionCall;

    if (!functionCall) {
      const textReply = candidate?.content?.parts?.[0]?.text || "I'm sorry, I couldn't understand that request.";
      await recordAndSend(textReply);
      return NextResponse.json({ status: "ok" });
    }

    const { name, args } = functionCall;

    function getCategoryEmoji(name: string): string {
      const q = name.toLowerCase().trim();
      if (/college|school|education|study|book|exam|fees/i.test(q)) return "🎓";
      if (/car|vehicle|petrol|diesel|fuel|travel|cab|auto|ride/i.test(q)) return "🚗";
      if (/food|restaurant|dining|snacks|hotel|pizza|burger|tea|coffee/i.test(q)) return "🍕";
      if (/checking|bank|salary|income|money|cash|deposit|savings/i.test(q)) return "💰";
      if (/shopping|clothes|fashion|mall|store|buy/i.test(q)) return "🛒";
      if (/home|rent|flat|house|electricity|utility|bill/i.test(q)) return "🏠";
      if (/health|medicine|doctor|hospital|pharma/i.test(q)) return "💊";
      if (/movie|entertainment|game|play|fun|sports/i.test(q)) return "🎬";
      if (/donation|charity|beggar|gift/i.test(q)) return "🎁";
      return "🏷️";
    }

    // --- HELPER FUNCTION: Find or Auto-Create Category ---
    async function getOrCreateCategory(categoryName?: string): Promise<{ id: string | null; name: string }> {
      if (!categoryName) return { id: null, name: "Uncategorized" };
      const q = categoryName.toLowerCase().trim();

      const existing = (categories || []).find(
        (c) => c.name.toLowerCase().trim() === q || c.name.toLowerCase().includes(q)
      );

      if (existing) {
        return { id: existing.id, name: existing.name };
      }

      // Auto-create missing category in Supabase with smart emoji icon
      const { data: newCat, error } = await supabase
        .from("categories")
        .insert({
          vault_code: DEFAULT_VAULT_CODE,
          name: categoryName.trim(),
          icon: getCategoryEmoji(categoryName),
          color: "#3B82F6",
        })
        .select()
        .single();

      if (!error && newCat) {
        categories.push(newCat);
        return { id: newCat.id, name: newCat.name };
      }

      // Fallback: If insert failed (e.g. category already exists or race condition), fetch it directly from DB
      const { data: fallbackCat } = await supabase
        .from("categories")
        .select("id, name")
        .eq("vault_code", DEFAULT_VAULT_CODE)
        .ilike("name", categoryName.trim())
        .maybeSingle();

      if (fallbackCat) {
        categories.push(fallbackCat);
        return { id: fallbackCat.id, name: fallbackCat.name };
      }

      return { id: null, name: categoryName };
    }

    // --- TOOL EXECUTION SWITCH ---

    if (name === "ask_user_clarification") {
      const question = args.question || "Could you please clarify your request?";
      await recordAndSend(question);
      return NextResponse.json({ status: "ok" });
    }

    // 1. ADD TRANSACTIONS (Single or Bulk)
    if (name === "add_transactions") {
      const items = args.items || [];
      if (items.length === 0) {
        await recordAndSend(`⚠️ No transactions were specified.`);
        return NextResponse.json({ status: "ok" });
      }

      const addedResults: string[] = [];

      for (const item of items) {
        const { id: catId, name: catName } = await getOrCreateCategory(item.category_name);
        const method = item.payment_method?.toLowerCase() === "cash" ? "Cash" : "UPI";

        const newTx = {
          vault_code: DEFAULT_VAULT_CODE,
          amount: item.amount,
          type: item.type || "expense",
          note: item.note || null,
          receiver_vendor: item.note || null,
          category_id: catId,
          source_app: method === "Cash" ? "Cash" : "UPI",
          occurred_at: item.occurred_at || new Date().toISOString(),
        };

        const { data: inserted, error: insertErr } = await supabase
          .from("transactions")
          .insert(newTx)
          .select()
          .single();

        if (!insertErr && inserted) {
          const symbol = item.type === "income" ? "📈" : "💸";
          const noteStr = item.note ? ` ("${item.note}")` : "";
          addedResults.push(`• ${symbol} *₹${item.amount}*${noteStr} → Category: *${catName}* [${method}]`);
        }
      }

      if (addedResults.length > 0) {
        await recordAndSend(
          `✅ *${addedResults.length} Transaction(s) Added Successfully!*\n\n` + addedResults.join("\n")
        );
      } else {
        await recordAndSend(`❌ Failed to insert transactions.`);
      }

      return NextResponse.json({ status: "ok" });
    }

    // 2. UPDATE TRANSACTION
    if (name === "update_transaction") {
      let targetTx: any = null;

      if (args.transaction_id) {
        targetTx = (recentTransactions || []).find((t) => t.id === args.transaction_id);
      } else if (args.position_index && recentTransactions) {
        targetTx = recentTransactions[args.position_index - 1];
      } else if (args.note_query && recentTransactions) {
        const q = args.note_query.toLowerCase();
        targetTx = recentTransactions.find(
          (t) => (t.note && t.note.toLowerCase().includes(q)) || (t.receiver_vendor && t.receiver_vendor.toLowerCase().includes(q))
        );
      } else if (args.amount && recentTransactions) {
        targetTx = recentTransactions.find((t) => t.amount === args.amount);
      }

      if (!targetTx) {
        await recordAndSend(`⚠️ Could not find a matching transaction to update.`);
        return NextResponse.json({ status: "ok" });
      }

      const updates: any = { updated_at: new Date().toISOString() };
      let updateMsgParts: string[] = [];

      if (args.new_amount) {
        updates.amount = args.new_amount;
        updateMsgParts.push(`Amount: ₹${args.new_amount}`);
      }
      if (args.new_note) {
        updates.note = args.new_note;
        updates.receiver_vendor = args.new_note;
        updateMsgParts.push(`Note: "${args.new_note}"`);
      }
      if (args.new_type) {
        updates.type = args.new_type;
        updateMsgParts.push(`Type: ${args.new_type}`);
      }
      if (args.new_category_name) {
        const { id: catId, name: catName } = await getOrCreateCategory(args.new_category_name);
        updates.category_id = catId;
        updateMsgParts.push(`Category: *${catName}*`);
      }

      const { error: upErr } = await supabase
        .from("transactions")
        .update(updates)
        .eq("id", targetTx.id)
        .eq("vault_code", DEFAULT_VAULT_CODE);

      if (upErr) {
        await recordAndSend(`❌ Failed to update transaction: ${upErr.message}`);
      } else {
        await recordAndSend(
          `✏️ *Transaction Updated Successfully!*\n\n` +
            `Target: ₹${targetTx.amount} ("${targetTx.note || targetTx.receiver_vendor || "N/A"}")\n` +
            `Updated Fields: ${updateMsgParts.join(", ")}`
        );
      }
      return NextResponse.json({ status: "ok" });
    }

    // 3. DELETE TRANSACTION
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
        await recordAndSend(`⚠️ Could not find a matching transaction to delete.`);
        return NextResponse.json({ status: "ok" });
      }

      const { error: delErr } = await supabase
        .from("transactions")
        .delete()
        .eq("id", targetTxId)
        .eq("vault_code", DEFAULT_VAULT_CODE);

      if (delErr) {
        await recordAndSend(`❌ Failed to delete transaction: ${delErr.message}`);
      } else {
        await recordAndSend(`🗑️ *Transaction deleted successfully!* ${deletedInfo}`);
      }
      return NextResponse.json({ status: "ok" });
    }

    // 4. MANAGE CATEGORIES (Single or Bulk)
    if (name === "manage_categories") {
      const { action, items } = args;
      const categoryList = items || [];
      const results: string[] = [];

      for (const item of categoryList) {
        const qName = item.name.toLowerCase().trim();
        const targetCat = (categories || []).find(
          (c) =>
            c.name.toLowerCase().trim() === qName ||
            c.name.toLowerCase().includes(qName) ||
            qName.includes(c.name.toLowerCase().trim())
        );

        if (action === "create") {
          const { data: created, error } = await supabase
            .from("categories")
            .insert({
              vault_code: DEFAULT_VAULT_CODE,
              name: item.name.trim(),
              icon: getCategoryEmoji(item.name),
              color: "#3B82F6",
              monthly_cap: item.monthly_cap || null,
            })
            .select()
            .single();

          if (!error && created) {
            categories.push(created);
            results.push(`• *"${created.name}"*`);
          } else {
            console.error("Failed to create category:", error?.message);
          }
        } else if (action === "update") {
          if (targetCat) {
            const updates: any = {};
            if (item.new_name) updates.name = item.new_name.trim();
            if (item.monthly_cap !== undefined) updates.monthly_cap = item.monthly_cap;

            const { error } = await supabase
              .from("categories")
              .update(updates)
              .eq("id", targetCat.id)
              .eq("vault_code", DEFAULT_VAULT_CODE);

            if (!error) {
              const catDisplayName = item.new_name ? `"${targetCat.name}" renamed to "${item.new_name}"` : `"${targetCat.name}"`;
              results.push(`• *${catDisplayName}* updated`);
            }
          } else {
            // Auto-create category if update target wasn't found
            const { data: created, error } = await supabase
              .from("categories")
              .insert({
                vault_code: DEFAULT_VAULT_CODE,
                name: (item.new_name || item.name).trim(),
                icon: getCategoryEmoji(item.name),
                color: "#3B82F6",
                monthly_cap: item.monthly_cap || null,
              })
              .select()
              .single();

            if (!error && created) {
              categories.push(created);
              results.push(`• *"${created.name}"* created`);
            }
          }
        } else if (action === "delete" && targetCat) {
          // Unlink transactions
          await supabase
            .from("transactions")
            .update({ category_id: null })
            .eq("category_id", targetCat.id)
            .eq("vault_code", DEFAULT_VAULT_CODE);

          // Delete category
          const { error } = await supabase
            .from("categories")
            .delete()
            .eq("id", targetCat.id)
            .eq("vault_code", DEFAULT_VAULT_CODE);

          if (!error) {
            results.push(`• *"${targetCat.name}"* deleted (Transactions moved to Uncategorized)`);
          }
        }
      }

      if (results.length > 0) {
        const symbol = action === "create" ? "✅" : action === "delete" ? "🗑️" : "✏️";
        await recordAndSend(
          `${symbol} *${results.length} Category(ies) ${action === "create" ? "Created" : action === "delete" ? "Deleted" : "Updated"} Successfully!*\n\n` +
            results.join("\n")
        );
      } else {
        await recordAndSend(`⚠️ No categories were modified.`);
      }
      return NextResponse.json({ status: "ok" });
    }

    // 5. QUERY OVERVIEW & ANALYTICS
    if (name === "query_overview_analytics") {
      const { query_type, timeframe, category_name } = args;

      // Handle "latest_transaction"
      if (query_type === "latest_transaction") {
        const latest = (recentTransactions || [])[0];
        if (!latest) {
          await recordAndSend(`ℹ️ No transactions recorded yet in your ledger.`);
        } else {
          const symbol = latest.type === "income" ? "📈" : "💸";
          await recordAndSend(
            `📌 *Latest Transaction Details*\n\n` +
              `${symbol} *Amount:* ₹${latest.amount}\n` +
              `📝 *Note:* ${latest.note || latest.receiver_vendor || "N/A"}\n` +
              `🏷️ *Category:* ${latest.categories?.name || "Uncategorized"}\n` +
              `📅 *Date:* ${new Date(latest.occurred_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}`
          );
        }
        return NextResponse.json({ status: "ok" });
      }

      // Handle "list_categories"
      if (query_type === "list_categories") {
        const catList = categories || [];
        if (catList.length === 0) {
          await recordAndSend(`🏷️ No categories created yet in Vault "${DEFAULT_VAULT_CODE}".`);
        } else {
          const rows = catList
            .map(
              (c) =>
                `• *${c.name}*` +
                (c.opening_balance ? ` (Opening Balance: ₹${c.opening_balance.toLocaleString("en-IN")})` : "") +
                (c.monthly_cap ? ` [Cap: ₹${c.monthly_cap.toLocaleString("en-IN")}]` : "")
            )
            .join("\n");
          await recordAndSend(
            `🏷️ *Categories in Vault "${DEFAULT_VAULT_CODE}" (${catList.length}):*\n\n` + rows
          );
        }
        return NextResponse.json({ status: "ok" });
      }

      // Fetch all transactions for general overview queries
      const { data: allTx, error: txErr } = await supabase
        .from("transactions")
        .select("*, categories(name)")
        .eq("vault_code", DEFAULT_VAULT_CODE)
        .order("occurred_at", { ascending: false });

      if (txErr || !allTx) {
        await recordAndSend(`❌ Failed to fetch expense analytics.`);
        return NextResponse.json({ status: "ok" });
      }

      let filtered = allTx;
      const now = new Date();

      if (category_name) {
        const qCat = category_name.toLowerCase().trim();
        filtered = filtered.filter(
          (t) =>
            t.categories?.name?.toLowerCase().trim() === qCat ||
            t.categories?.name?.toLowerCase().includes(qCat)
        );
      }

      if (timeframe === "today") {
        const todayStr = now.toISOString().substring(0, 10);
        filtered = filtered.filter((t) => t.occurred_at?.substring(0, 10) === todayStr);
      } else if (timeframe === "this_month") {
        filtered = filtered.filter((t) => {
          const d = new Date(t.occurred_at);
          return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
        });
      }

      // Handle "transaction_count"
      if (query_type === "transaction_count") {
        const catLabel = category_name ? ` under category "*${category_name}*"` : "";
        const tfLabel = timeframe ? ` (${timeframe.replace("_", " ")})` : "";
        let totalAmt = 0;
        filtered.forEach((t) => (totalAmt += Number(t.amount || 0)));
        await recordAndSend(
          `🔢 *Transaction Count Query*\n\n` +
            `Found *${filtered.length}* transaction(s)${catLabel}${tfLabel}.\n` +
            `💸 *Total Amount:* ₹${totalAmt.toLocaleString("en-IN")}`
        );
        return NextResponse.json({ status: "ok" });
      }

      // Handle "list_transactions"
      if (query_type === "list_transactions") {
        const catLabel = category_name ? ` under "*${category_name}*"` : "";
        const tfLabel = timeframe ? ` (${timeframe.replace("_", " ")})` : "";
        if (filtered.length === 0) {
          await recordAndSend(`ℹ️ No transactions found matching your request.`);
        } else {
          const rows = filtered
            .map((t, idx) => {
              const symbol = t.type === "income" ? "📈" : "💸";
              return `[${idx + 1}] ${symbol} *₹${t.amount}* ("${t.note || t.receiver_vendor || "N/A"}") → *${t.categories?.name || "Uncategorized"}* (${t.occurred_at?.substring(0, 10)})`;
            })
            .join("\n");
          await recordAndSend(
            `📋 *Transactions List (${filtered.length})${catLabel}${tfLabel}:*\n\n` + rows
          );
        }
        return NextResponse.json({ status: "ok" });
      }

      let totalIncome = 0;
      let totalExpense = 0;
      const catTotals: Record<string, number> = {};

      filtered.forEach((t) => {
        if (t.type === "income") {
          totalIncome += Number(t.amount || 0);
        } else {
          totalExpense += Number(t.amount || 0);
          const cName = t.categories?.name || "Uncategorized";
          catTotals[cName] = (catTotals[cName] || 0) + Number(t.amount || 0);
        }
      });

      let catBreakdown = "";
      Object.entries(catTotals).forEach(([c, a]) => {
        catBreakdown += `  ▫️ *${c}:* ₹${a.toLocaleString("en-IN")}\n`;
      });

      const label = timeframe ? timeframe.replace("_", " ").toUpperCase() : "OVERALL";

      await recordAndSend(
        `📊 *Expense Analytics (${label})*\n\n` +
          `💸 *Total Expenses:* ₹${totalExpense.toLocaleString("en-IN")}\n` +
          `📈 *Total Income:* ₹${totalIncome.toLocaleString("en-IN")}\n` +
          `💰 *Net Savings:* ₹${(totalIncome - totalExpense).toLocaleString("en-IN")}\n\n` +
          `🏷️ *Category Breakdown:*\n${catBreakdown || "  ▫️ No expenses recorded."}`
      );

      return NextResponse.json({ status: "ok" });
    }

    // 6. UNDO LAST ACTION
    if (name === "undo_last_action") {
      const { data: lastTx } = await supabase
        .from("transactions")
        .select("*, categories(name)")
        .eq("vault_code", DEFAULT_VAULT_CODE)
        .order("created_at", { ascending: false })
        .limit(1)
        .maybeSingle();

      if (!lastTx) {
        await recordAndSend(`⚠️ No recent transaction found to undo.`);
        return NextResponse.json({ status: "ok" });
      }

      const { error: delErr } = await supabase
        .from("transactions")
        .delete()
        .eq("id", lastTx.id)
        .eq("vault_code", DEFAULT_VAULT_CODE);

      if (delErr) {
        await recordAndSend(`❌ Failed to undo last transaction: ${delErr.message}`);
      } else {
        const catName = lastTx.categories?.name || "Uncategorized";
        await recordAndSend(
          `↩️ *Undo Successful!*\n\n` +
            `Deleted last logged transaction:\n` +
            `• Amount: ₹${lastTx.amount}\n` +
            `• Note: "${lastTx.note || lastTx.receiver_vendor || "N/A"}"\n` +
            `• Category: ${catName}`
        );
      }
      return NextResponse.json({ status: "ok" });
    }

    // 7. EXPORT TRANSACTIONS FILE (CSV Download)
    if (name === "export_transactions_file") {
      const { timeframe, category_name } = args;

      const { data: allTx, error: txErr } = await supabase
        .from("transactions")
        .select("*, categories(name)")
        .eq("vault_code", DEFAULT_VAULT_CODE)
        .order("occurred_at", { ascending: false });

      if (txErr || !allTx || allTx.length === 0) {
        await recordAndSend(`⚠️ No transactions found to export.`);
        return NextResponse.json({ status: "ok" });
      }

      let filtered = allTx;
      const now = new Date();

      if (timeframe === "today") {
        const todayStr = now.toISOString().substring(0, 10);
        filtered = allTx.filter((t) => t.occurred_at?.substring(0, 10) === todayStr);
      } else if (timeframe === "this_month") {
        filtered = allTx.filter((t) => {
          const d = new Date(t.occurred_at);
          return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
        });
      }

      if (category_name) {
        const qCat = category_name.toLowerCase();
        filtered = filtered.filter((t) => t.categories?.name?.toLowerCase().includes(qCat));
      }

      if (filtered.length === 0) {
        await recordAndSend(`⚠️ No transactions match your requested filter.`);
        return NextResponse.json({ status: "ok" });
      }

      // Generate clean CSV content
      const csvHeader = `"ID","Date","Time","Type","Amount (INR)","Note / Vendor","Category","Source App"\n`;
      const csvRows = filtered
        .map((t) => {
          const d = new Date(t.occurred_at);
          const dateStr = d.toISOString().substring(0, 10);
          const timeStr = d.toTimeString().substring(0, 8);
          const noteStr = (t.note || t.receiver_vendor || "").replace(/"/g, '""');
          const catStr = (t.categories?.name || "Uncategorized").replace(/"/g, '""');
          return `"${t.id}","${dateStr}","${timeStr}","${t.type}","${t.amount}","${noteStr}","${catStr}","${t.source_app || "AutoTrack"}"`;
        })
        .join("\n");

      const csvContent = csvHeader + csvRows;
      const tfLabel = timeframe ? timeframe.replace("_", " ") : "export";
      const fileName = `AutoTrack_Transactions_${tfLabel}_${now.toISOString().substring(0, 10)}.csv`;

      await sendTelegramDocument(
        chatId,
        csvContent,
        fileName,
        `📄 *Here is your requested transaction export file!* (${filtered.length} transactions)`
      );

      // Record memory for export file
      chatHistory.push({ role: "user", parts: [{ text: userMessage }] });
      chatHistory.push({ role: "model", parts: [{ text: `Sent CSV export document for ${filtered.length} transactions.` }] });
      await saveChatMemory(supabase, chatHistory);

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
