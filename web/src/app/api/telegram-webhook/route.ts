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
                  type: { type: "STRING", enum: ["expense", "income"], description: "Transaction type" },
                  occurred_at: { type: "STRING", description: "ISO date string or relative date if specified" },
                },
                required: ["amount", "type"],
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
        description: "Create, rename, update properties (opening balance, monthly cap), or delete one or multiple categories.",
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
                  opening_balance: { type: "NUMBER", description: "Opening balance amount" },
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
              enum: ["latest_transaction", "summary", "category_breakdown", "top_spending", "filtered_range"],
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
        description: "Ask the user a clarifying question when crucial parameters are missing or ambiguous.",
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

    // 1. Immediately show "typing..." status in Telegram
    await sendTypingAction(chatId);

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

    // 2. Fetch current categories for vault
    const { data: categories } = await supabase
      .from("categories")
      .select("*")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("created_at", { ascending: true });

    // 3. Fetch recent 30 transactions for vault context
    const { data: recentTransactions } = await supabase
      .from("transactions")
      .select("*, categories(name)")
      .eq("vault_code", DEFAULT_VAULT_CODE)
      .order("occurred_at", { ascending: false })
      .limit(30);

    const categoryNamesList = (categories || []).map((c) => c.name).join(", ");
    const recentLedgerFormatted = (recentTransactions || [])
      .map(
        (t, idx) =>
          `[${idx + 1}] ID:${t.id} | ${t.type.toUpperCase()} | ₹${t.amount} | Note: "${t.note || t.receiver_vendor || "N/A"}" | Category: ${
            t.categories?.name || "Uncategorized"
          } | Date: ${t.occurred_at}`
      )
      .join("\n");

    const systemPrompt = `You are the AutoTrack AI Assistant for personal finance ledger management.
Your active Vault Code is: "${DEFAULT_VAULT_CODE}".
Current Date & Time: ${new Date().toISOString()}.

Available Categories in Vault:
[ ${categoryNamesList || "None"} ]

Recent Ledger Transactions (Ordered 1-based, newest first):
${recentLedgerFormatted || "No transactions recorded yet."}

CRITICAL RULES:
1. ADDING TRANSACTIONS:
   - When user wants to log one or multiple transactions (e.g. "40 rs for friend under adjustment, 30rs for tea under clg works"), choose 'add_transactions' with an array of items.
   - If user specifies a category name, provide category_name in the item. The system will auto-create the category if it does not exist yet. NEVER delete a transaction when asked to update it!
2. UPDATING TRANSACTIONS:
   - When user asks to update/edit a transaction (e.g. "update petrol transaction into clg category"), choose 'update_transaction'. DO NOT choose delete_transaction!
3. DELETING TRANSACTIONS:
   - Choose 'delete_transaction' ONLY when explicitly asked to delete/remove a transaction log (e.g. "delete 3rd log", "delete chocolates log").
4. MANAGING CATEGORIES:
   - Choose 'manage_categories' to create, update, or delete single or multiple categories (e.g. "create 3 categories named x, y (with 10k opening balance) and z").
5. QUERYING ANALYTICS & LATEST TRANSACTIONS:
   - Choose 'query_overview_analytics' when user asks about latest transaction, total expenses, category usage, daily/monthly summaries, or date ranges.
6. Always choose a tool call matching user intent.`;

    // Call Gemini API via REST with candidate models
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
      const textReply = candidate?.content?.parts?.[0]?.text || "I'm sorry, I couldn't understand that request.";
      await sendTelegramMessage(chatId, textReply);
      return NextResponse.json({ status: "ok" });
    }

    const { name, args } = functionCall;

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

      // Auto-create missing category in Supabase
      const { data: newCat, error } = await supabase
        .from("categories")
        .insert({
          vault_code: DEFAULT_VAULT_CODE,
          name: categoryName.trim(),
          icon: "Category",
          color: "#3B82F6",
        })
        .select()
        .single();

      if (!error && newCat) {
        return { id: newCat.id, name: newCat.name };
      }

      return { id: null, name: categoryName };
    }

    // --- TOOL EXECUTION SWITCH ---

    if (name === "ask_user_clarification") {
      await sendTelegramMessage(chatId, args.question || "Could you please clarify your request?");
      return NextResponse.json({ status: "ok" });
    }

    // 1. ADD TRANSACTIONS (Single or Bulk)
    if (name === "add_transactions") {
      const items = args.items || [];
      if (items.length === 0) {
        await sendTelegramMessage(chatId, `⚠️ No transactions were specified.`);
        return NextResponse.json({ status: "ok" });
      }

      const addedResults: string[] = [];

      for (const item of items) {
        const { id: catId, name: catName } = await getOrCreateCategory(item.category_name);

        const newTx = {
          vault_code: DEFAULT_VAULT_CODE,
          amount: item.amount,
          type: item.type || "expense",
          note: item.note || null,
          receiver_vendor: item.note || null,
          category_id: catId,
          source_app: "Telegram Bot",
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
          addedResults.push(`• ${symbol} *₹${item.amount}*${noteStr} → Category: *${catName}*`);
        }
      }

      if (addedResults.length > 0) {
        await sendTelegramMessage(
          chatId,
          `✅ *${addedResults.length} Transaction(s) Added Successfully!*\n\n` + addedResults.join("\n")
        );
      } else {
        await sendTelegramMessage(chatId, `❌ Failed to insert transactions.`);
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
        await sendTelegramMessage(chatId, `⚠️ Could not find a matching transaction to update.`);
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
        await sendTelegramMessage(chatId, `❌ Failed to update transaction: ${upErr.message}`);
      } else {
        await sendTelegramMessage(
          chatId,
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

    // 4. MANAGE CATEGORIES (Single or Bulk)
    if (name === "manage_categories") {
      const { action, items } = args;
      const categoryList = items || [];
      const results: string[] = [];

      for (const item of categoryList) {
        const targetCat = (categories || []).find((c) => c.name.toLowerCase().trim() === item.name.toLowerCase().trim());

        if (action === "create") {
          const { data: created, error } = await supabase
            .from("categories")
            .insert({
              vault_code: DEFAULT_VAULT_CODE,
              name: item.name.trim(),
              icon: "Category",
              color: "#3B82F6",
              opening_balance: item.opening_balance || 0,
              monthly_cap: item.monthly_cap || null,
            })
            .select()
            .single();

          if (!error && created) {
            const obStr = item.opening_balance ? ` (Opening Balance: ₹${item.opening_balance.toLocaleString("en-IN")})` : "";
            results.push(`• *"${created.name}"*${obStr}`);
          }
        } else if (action === "update" && targetCat) {
          const updates: any = {};
          if (item.new_name) updates.name = item.new_name;
          if (item.opening_balance !== undefined) updates.opening_balance = item.opening_balance;
          if (item.monthly_cap !== undefined) updates.monthly_cap = item.monthly_cap;

          const { error } = await supabase
            .from("categories")
            .update(updates)
            .eq("id", targetCat.id)
            .eq("vault_code", DEFAULT_VAULT_CODE);

          if (!error) {
            results.push(`• *"${targetCat.name}"* updated`);
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
        await sendTelegramMessage(
          chatId,
          `${symbol} *${results.length} Category(ies) ${action === "create" ? "Created" : action === "delete" ? "Deleted" : "Updated"} Successfully!*\n\n` +
            results.join("\n")
        );
      } else {
        await sendTelegramMessage(chatId, `⚠️ No categories were modified.`);
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
          await sendTelegramMessage(chatId, `ℹ️ No transactions recorded yet in your ledger.`);
        } else {
          const symbol = latest.type === "income" ? "📈" : "💸";
          await sendTelegramMessage(
            chatId,
            `📌 *Latest Transaction Details*\n\n` +
              `${symbol} *Amount:* ₹${latest.amount}\n` +
              `📝 *Note:* ${latest.note || latest.receiver_vendor || "N/A"}\n` +
              `🏷️ *Category:* ${latest.categories?.name || "Uncategorized"}\n` +
              `📅 *Date:* ${new Date(latest.occurred_at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}`
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
        await sendTelegramMessage(chatId, `❌ Failed to fetch expense analytics.`);
        return NextResponse.json({ status: "ok" });
      }

      let filtered = allTx;
      const now = new Date();

      if (category_name) {
        const qCat = category_name.toLowerCase();
        filtered = filtered.filter((t) => t.categories?.name?.toLowerCase().includes(qCat));
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

      await sendTelegramMessage(
        chatId,
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
        await sendTelegramMessage(chatId, `⚠️ No recent transaction found to undo.`);
        return NextResponse.json({ status: "ok" });
      }

      const { error: delErr } = await supabase
        .from("transactions")
        .delete()
        .eq("id", lastTx.id)
        .eq("vault_code", DEFAULT_VAULT_CODE);

      if (delErr) {
        await sendTelegramMessage(chatId, `❌ Failed to undo last transaction: ${delErr.message}`);
      } else {
        const catName = lastTx.categories?.name || "Uncategorized";
        await sendTelegramMessage(
          chatId,
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
        await sendTelegramMessage(chatId, `⚠️ No transactions found to export.`);
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
        await sendTelegramMessage(chatId, `⚠️ No transactions match your requested filter.`);
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
