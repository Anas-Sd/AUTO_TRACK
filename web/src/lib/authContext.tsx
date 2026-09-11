"use client";

import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from "react";
import { DEFAULT_CATEGORIES } from "./supabaseClient";
import { Category, Transaction } from "./types";

interface AuthContextType {
  vaultCode: string | null;
  vaultLabel: string;
  token: string | null;
  categories: Category[];
  transactions: Transaction[];
  loading: boolean;
  activeTab: "overview" | "ledger" | "categories" | "settings";
  setActiveTab: (tab: "overview" | "ledger" | "categories" | "settings") => void;
  ledgerCategoryFilter: string;
  setLedgerCategoryFilter: (catId: string) => void;
  isLogModalOpen: boolean;
  setIsLogModalOpen: (open: boolean) => void;
  editingTransaction: Transaction | null;
  setEditingTransaction: (txn: Transaction | null) => void;
  loginVault: (code: string) => Promise<{ success: boolean; error?: string }>;
  createVault: () => Promise<string | null>;
  logoutVault: () => Promise<void>;
  refreshData: () => Promise<void>;
  addTransaction: (txn: Partial<Transaction>) => Promise<Transaction | null>;
  updateTransaction: (id: string, txn: Partial<Transaction>) => Promise<boolean>;
  deleteTransaction: (id: string) => Promise<boolean>;
  addCategory: (cat: Partial<Category>) => Promise<Category | null>;
  updateCategory: (id: string, cat: Partial<Category>) => Promise<boolean>;
  deleteCategory: (id: string) => Promise<boolean>;
  updateVaultLabel: (label: string) => Promise<boolean>;
  rotateVaultCode: () => Promise<string | null>;
  wipeVaultData: () => Promise<boolean>;
  deleteVaultPermanently: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [vaultCode, setVaultCode] = useState<string | null>(null);
  const [vaultLabel, setVaultLabel] = useState<string>("My Vault");
  const [token, setToken] = useState<string | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<"overview" | "ledger" | "categories" | "settings">("overview");
  const [ledgerCategoryFilter, setLedgerCategoryFilter] = useState<string>("all");
  const [isLogModalOpen, setIsLogModalOpen] = useState<boolean>(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);

  // Cache refs to prevent re-renders when polling data is identical
  const prevCatsRef = useRef<string>("");
  const prevTxnsRef = useRef<string>("");

  // Initialize session from cookie, AndroidBridge, or local storage
  useEffect(() => {
    async function initSession() {
      try {
        // 0. Check native AndroidBridge vault code first if inside Android APK
        if (typeof window !== "undefined" && (window as any).AndroidBridge?.getVaultCode) {
          const nativeCode = (window as any).AndroidBridge.getVaultCode();
          if (nativeCode && nativeCode.length > 0) {
            const loginRes = await loginVault(nativeCode);
            if (loginRes.success) {
              setLoading(false);
              return;
            }
          }
        }

        const res = await fetch("/api/vault-session");
        const data = await res.json();
        if (data.authenticated && data.vault_code && data.token) {
          setVaultCode(data.vault_code);
          setToken(data.token);
          if (data.label) setVaultLabel(data.label);
          if (typeof window !== "undefined" && (window as any).AndroidBridge?.setVaultCode) {
            (window as any).AndroidBridge.setVaultCode(data.vault_code);
          }
          setLoading(false);
          return;
        }

        const savedCode = localStorage.getItem("autotrack_vault_code");
        if (savedCode) {
          await loginVault(savedCode);
        } else {
          setLoading(false);
        }
      } catch (err) {
        console.error("Init session error:", err);
        setLoading(false);
      }
    }
    initSession();

    if (typeof window !== "undefined") {
      (window as any).openManualLogModal = (source?: string) => {
        if (source && typeof source === "string") {
          setEditingTransaction({ source_app: source } as any);
        } else {
          setEditingTransaction(null);
        }
        setIsLogModalOpen(true);
      };
    }
  }, []);

  const refreshData = useCallback(async () => {
    if (!vaultCode) return;

    try {
      // 1. Fetch Categories
      const catRes = await fetch("/api/vault/data?type=categories");
      const catJson = await catRes.json();
      const catData: Category[] = catJson.data || [];
      const catsHash = JSON.stringify(catData);
      if (catsHash !== prevCatsRef.current) {
        prevCatsRef.current = catData.length ? catsHash : "";
        setCategories(catData);
      }

      // 2. Fetch Transactions
      const txnRes = await fetch("/api/vault/data?type=transactions");
      const txnJson = await txnRes.json();
      const txnData: Transaction[] = txnJson.data || [];

      const txnsHash = JSON.stringify(txnData);
      if (txnsHash !== prevTxnsRef.current) {
        prevTxnsRef.current = txnData.length ? txnsHash : "";
        setTransactions(txnData);
      }
    } catch (err) {
      console.error("Error refreshing vault data:", err);
    }
  }, [vaultCode]);

  useEffect(() => {
    if (!vaultCode) return;
    refreshData();

    // Auto refresh interval every 3 seconds for live sync
    const interval = setInterval(() => {
      refreshData();
    }, 3000);

    return () => clearInterval(interval);
  }, [vaultCode, refreshData]);

  const loginVault = async (code: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const formatted = code.trim().toUpperCase();
      const res = await fetch("/api/issue-vault-session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code: formatted }),
      });
      const data = await res.json();

      if (res.ok && data.token) {
        setVaultCode(formatted);
        setToken(data.token);
        if (data.label) setVaultLabel(data.label);
        localStorage.setItem("autotrack_vault_code", formatted);
        if (typeof window !== "undefined" && (window as any).AndroidBridge?.setVaultCode) {
          (window as any).AndroidBridge.setVaultCode(formatted);
        }
        setLoading(false);
        return { success: true };
      }
      return { success: false, error: data.error || "Invalid vault code" };
    } catch (err: any) {
      console.error("Login vault error:", err);
      return { success: false, error: err.message || "Network error" };
    }
  };

  const createVault = async (): Promise<string | null> => {
    try {
      const res = await fetch("/api/create-vault", { method: "POST" });
      const data = await res.json();

      if (res.ok && data.code) {
        await loginVault(data.code);
        return data.code;
      }
      return null;
    } catch (err) {
      console.error("Create vault error:", err);
      return null;
    }
  };

  const logoutVault = async () => {
    try {
      await fetch("/api/logout", { method: "POST" });
    } catch (e) {
      // ignore
    }
    setVaultCode(null);
    setToken(null);
    setCategories([]);
    setTransactions([]);
    localStorage.removeItem("autotrack_vault_code");
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.onLogout) {
      (window as any).AndroidBridge.onLogout();
    }
  };

  const addTransaction = async (txn: Partial<Transaction>): Promise<Transaction | null> => {
    if (!vaultCode) return null;
    try {
      const payload = {
        note: txn.note?.trim() || null,
        receiver_vendor: txn.receiver_vendor?.trim() || null,
        amount: Number(txn.amount) || 0,
        type: txn.type || "expense",
        category_id: txn.category_id || null,
        source_app: txn.source_app || "Manual",
        raw_notification: txn.raw_notification || null,
        occurred_at: txn.occurred_at || new Date().toISOString(),
      };

      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "insert", type: "transaction", payload }),
      });
      const json = await res.json();
      if (json.data) {
        await refreshData();
        return json.data;
      }
      return null;
    } catch (err) {
      console.error("Add transaction error:", err);
      return null;
    }
  };

  const updateTransaction = async (id: string, txn: Partial<Transaction>): Promise<boolean> => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "update", type: "transaction", id, payload: txn }),
      });
      const json = await res.json();
      if (json.success) {
        await refreshData();
        return true;
      }
      return false;
    } catch (err) {
      console.error("Update transaction error:", err);
      return false;
    }
  };

  const deleteTransaction = async (id: string): Promise<boolean> => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "delete", type: "transaction", id }),
      });
      const json = await res.json();
      if (json.success) {
        await refreshData();
        return true;
      }
      return false;
    } catch (err) {
      console.error("Delete transaction error:", err);
      return false;
    }
  };

  const addCategory = async (cat: Partial<Category>): Promise<Category | null> => {
    if (!vaultCode) return null;
    try {
      const payload = {
        name: cat.name?.trim() || "New Category",
        icon: cat.icon || "🏷️",
        color: cat.color || "#10B981",
        monthly_cap: cat.monthly_cap ? Number(cat.monthly_cap) : null,
      };

      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "insert", type: "category", payload }),
      });
      const json = await res.json();
      if (json.data) {
        await refreshData();
        return json.data;
      }
      return null;
    } catch (err) {
      console.error("Add category error:", err);
      return null;
    }
  };

  const updateCategory = async (id: string, cat: Partial<Category>): Promise<boolean> => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "update", type: "category", id, payload: cat }),
      });
      const json = await res.json();
      if (json.success) {
        await refreshData();
        return true;
      }
      return false;
    } catch (err) {
      console.error("Update category error:", err);
      return false;
    }
  };

  const deleteCategory = async (id: string): Promise<boolean> => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "delete", type: "category", id }),
      });
      const json = await res.json();
      if (json.success) {
        await refreshData();
        return true;
      }
      return false;
    } catch (err) {
      console.error("Delete category error:", err);
      return false;
    }
  };

  const updateVaultLabel = async (newLabel: string): Promise<boolean> => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "update", type: "vault_label", payload: { label: newLabel.trim() } }),
      });
      const json = await res.json();
      if (json.success) {
        setVaultLabel(newLabel.trim());
        return true;
      }
      return false;
    } catch (err) {
      console.error("Update vault label error:", err);
      return false;
    }
  };

  const rotateVaultCode = async (): Promise<string | null> => {
    if (!vaultCode) return null;
    try {
      const res = await fetch("/api/vault/rotate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });
      const json = await res.json();
      if (json.success && json.new_vault_code) {
        setVaultCode(json.new_vault_code);
        if (json.label) setVaultLabel(json.label);
        localStorage.setItem("autotrack_vault_code", json.new_vault_code);
        await refreshData();
        return json.new_vault_code;
      }
      return null;
    } catch (err) {
      console.error("Rotate vault code error:", err);
      return null;
    }
  };

  const wipeVaultData = useCallback(async () => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "wipe_vault_data" }),
      });
      const json = await res.json();
      if (json.success) {
        setTransactions([]);
        setCategories([]);
        return true;
      }
      return false;
    } catch (err) {
      console.error("Wipe vault data error:", err);
      return false;
    }
  }, [vaultCode]);

  const deleteVaultPermanently = useCallback(async () => {
    if (!vaultCode) return false;
    try {
      const res = await fetch("/api/vault/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "delete_vault_permanently" }),
      });
      const json = await res.json();
      if (json.success) {
        if (typeof window !== "undefined" && (window as any).AndroidBridge?.onLogout) {
          (window as any).AndroidBridge.onLogout();
        } else {
          await logoutVault();
        }
        return true;
      }
      return false;
    } catch (err) {
      console.error("Delete vault permanently error:", err);
      return false;
    }
  }, [vaultCode, logoutVault]);

  return (
    <AuthContext.Provider
      value={{
        vaultCode,
        vaultLabel,
        token,
        categories,
        transactions,
        loading,
        activeTab,
        setActiveTab,
        ledgerCategoryFilter,
        setLedgerCategoryFilter,
        isLogModalOpen,
        setIsLogModalOpen,
        editingTransaction,
        setEditingTransaction,
        loginVault,
        createVault,
        logoutVault,
        refreshData,
        addTransaction,
        updateTransaction,
        deleteTransaction,
        addCategory,
        updateCategory,
        deleteCategory,
        updateVaultLabel,
        rotateVaultCode,
        wipeVaultData,
        deleteVaultPermanently,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
