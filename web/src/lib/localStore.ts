import fs from "fs";
import path from "path";

interface LocalStoreData {
  vault_codes: Array<{ code: string; label: string; created_at: string; last_accessed?: string }>;
  categories: Array<{
    id: string;
    vault_code: string;
    name: string;
    icon?: string;
    color?: string;
    monthly_cap?: number | null;
    created_at: string;
  }>;
  transactions: Array<{
    id: string;
    vault_code: string;
    note?: string;
    receiver_vendor?: string;
    amount: number;
    type: "income" | "expense";
    category_id?: string | null;
    source_app?: string;
    raw_notification?: string;
    occurred_at: string;
    created_at: string;
    updated_at: string;
  }>;
}

const DB_DIR = path.join(process.cwd(), ".local-db");
const DB_FILE = path.join(DB_DIR, "store.json");

function getStore(): LocalStoreData {
  try {
    if (!fs.existsSync(DB_DIR)) {
      fs.mkdirSync(DB_DIR, { recursive: true });
    }
    if (!fs.existsSync(DB_FILE)) {
      const initial: LocalStoreData = { vault_codes: [], categories: [], transactions: [] };
      fs.writeFileSync(DB_FILE, JSON.stringify(initial, null, 2), "utf8");
      return initial;
    }
    const content = fs.readFileSync(DB_FILE, "utf8");
    return JSON.parse(content);
  } catch (e) {
    return { vault_codes: [], categories: [], transactions: [] };
  }
}

function saveStore(data: LocalStoreData) {
  try {
    if (!fs.existsSync(DB_DIR)) {
      fs.mkdirSync(DB_DIR, { recursive: true });
    }
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), "utf8");
  } catch (e) {
    console.error("Failed to persist local store:", e);
  }
}

export const LocalStore = {
  getVault(code: string) {
    const store = getStore();
    return store.vault_codes.find((v) => v.code === code.toUpperCase()) || null;
  },

  createVault(code: string, label = "My Vault") {
    const store = getStore();
    const cleanCode = code.toUpperCase();
    const existing = store.vault_codes.find((v) => v.code === cleanCode);
    if (!existing) {
      const entry = {
        code: cleanCode,
        label,
        created_at: new Date().toISOString(),
        last_accessed: new Date().toISOString(),
      };
      store.vault_codes.push(entry);
      saveStore(store);
      return entry;
    }
    return existing;
  },

  updateVaultLabel(code: string, label: string) {
    const store = getStore();
    const vault = store.vault_codes.find((v) => v.code === code.toUpperCase());
    if (vault) {
      vault.label = label;
      saveStore(store);
      return true;
    }
    return false;
  },

  getCategories(vaultCode: string) {
    const store = getStore();
    return store.categories.filter((c) => c.vault_code === vaultCode.toUpperCase());
  },

  saveCategory(cat: any) {
    const store = getStore();
    const id = cat.id || `cat_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    const newCat = {
      ...cat,
      id,
      vault_code: cat.vault_code.toUpperCase(),
      created_at: cat.created_at || new Date().toISOString(),
    };
    const existingIndex = store.categories.findIndex((c) => c.id === id);
    if (existingIndex >= 0) {
      store.categories[existingIndex] = newCat;
    } else {
      store.categories.push(newCat);
    }
    saveStore(store);
    return newCat;
  },

  deleteCategory(id: string, vaultCode: string) {
    const store = getStore();
    store.categories = store.categories.filter(
      (c) => !(c.id === id && c.vault_code === vaultCode.toUpperCase())
    );
    // Set category_id to null on affected transactions
    store.transactions.forEach((t) => {
      if (t.category_id === id) t.category_id = null;
    });
    saveStore(store);
    return true;
  },

  getTransactions(vaultCode: string) {
    const store = getStore();
    return store.transactions
      .filter((t) => t.vault_code === vaultCode.toUpperCase())
      .sort((a, b) => new Date(b.occurred_at).getTime() - new Date(a.occurred_at).getTime());
  },

  saveTransaction(txn: any) {
    const store = getStore();
    const id = txn.id || `txn_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    const newTxn = {
      ...txn,
      id,
      vault_code: txn.vault_code.toUpperCase(),
      occurred_at: txn.occurred_at || new Date().toISOString(),
      created_at: txn.created_at || new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    const existingIndex = store.transactions.findIndex((t) => t.id === id);
    if (existingIndex >= 0) {
      store.transactions[existingIndex] = newTxn;
    } else {
      store.transactions.unshift(newTxn);
    }
    saveStore(store);
    return newTxn;
  },

  deleteTransaction(id: string, vaultCode: string) {
    const store = getStore();
    store.transactions = store.transactions.filter(
      (t) => !(t.id === id && t.vault_code === vaultCode.toUpperCase())
    );
    saveStore(store);
    return true;
  },
};
