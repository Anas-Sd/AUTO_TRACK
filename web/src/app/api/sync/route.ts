import { NextResponse } from "next/server";
import { LocalStore } from "@/lib/localStore";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const type = searchParams.get("type");
  const vaultCode = searchParams.get("vault_code") || "";

  if (!vaultCode) {
    return NextResponse.json({ error: "vault_code is required" }, { status: 400 });
  }

  if (type === "categories") {
    const categories = LocalStore.getCategories(vaultCode);
    return NextResponse.json({ data: categories });
  } else if (type === "transactions") {
    const transactions = LocalStore.getTransactions(vaultCode);
    return NextResponse.json({ data: transactions });
  }

  return NextResponse.json({ error: "Invalid type" }, { status: 400 });
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const { type, payload, vault_code } = body;

    if (!vault_code) {
      return NextResponse.json({ error: "vault_code is required" }, { status: 400 });
    }

    if (type === "category") {
      const saved = LocalStore.saveCategory({ ...payload, vault_code });
      return NextResponse.json({ data: saved });
    } else if (type === "transaction") {
      const saved = LocalStore.saveTransaction({ ...payload, vault_code });
      return NextResponse.json({ data: saved });
    } else if (type === "vault_label") {
      LocalStore.updateVaultLabel(vault_code, payload.label);
      return NextResponse.json({ success: true });
    }

    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  } catch (err: any) {
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}

export async function DELETE(req: Request) {
  try {
    const body = await req.json();
    const { type, id, vault_code } = body;

    if (!vault_code || !id) {
      return NextResponse.json({ error: "vault_code and id are required" }, { status: 400 });
    }

    if (type === "category") {
      LocalStore.deleteCategory(id, vault_code);
      return NextResponse.json({ success: true });
    } else if (type === "transaction") {
      LocalStore.deleteTransaction(id, vault_code);
      return NextResponse.json({ success: true });
    }

    return NextResponse.json({ error: "Invalid type" }, { status: 400 });
  } catch (err: any) {
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}
