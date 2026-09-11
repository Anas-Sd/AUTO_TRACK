"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/lib/authContext";
import { Transaction } from "@/lib/types";
import CategoryModal from "./CategoryModal";
import CustomSelect, { CustomSelectOption } from "./CustomSelect";
import {
  X,
  Plus,
  Building,
  Calendar,
  FileText,
  Smartphone,
  Tag,
  Loader2,
  TrendingDown,
  TrendingUp,
} from "lucide-react";

interface TransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTransaction?: Transaction | null;
}

export default function TransactionModal({
  isOpen,
  onClose,
  initialTransaction,
}: TransactionModalProps) {
  const { vaultLabel, categories, addTransaction, updateTransaction, addCategory } = useAuth();

  const [amount, setAmount] = useState("");
  const [type, setType] = useState<"income" | "expense">("expense");
  const [categoryId, setCategoryId] = useState<string>("");
  const [receiverVendor, setReceiverVendor] = useState("");
  const [sourceApp, setSourceApp] = useState("Manual");
  const [note, setNote] = useState("");
  const [occurredAt, setOccurredAt] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Inline new category modal state
  const [isNewCatModalOpen, setIsNewCatModalOpen] = useState(false);

  // Form fields reset ONLY when modal opens or initialTransaction changes
  useEffect(() => {
    if (!isOpen) return;

    if (initialTransaction) {
      setAmount(initialTransaction.amount ? initialTransaction.amount.toString() : "");
      setType(initialTransaction.type || "expense");
      setCategoryId(initialTransaction.category_id || "");
      setReceiverVendor(initialTransaction.receiver_vendor || "");
      setSourceApp(initialTransaction.source_app || "Manual");
      setNote(initialTransaction.note || "");
      setOccurredAt(
        initialTransaction.occurred_at
          ? new Date(initialTransaction.occurred_at).toISOString().slice(0, 16)
          : new Date().toISOString().slice(0, 16)
      );
    } else {
      setAmount("");
      setType("expense");
      setCategoryId(categories.length > 0 ? categories[0].id : "");
      setReceiverVendor("");
      setSourceApp("Manual");
      setNote("");
      // Local datetime formatted for datetime-local input
      const localNow = new Date();
      localNow.setMinutes(localNow.getMinutes() - localNow.getTimezoneOffset());
      setOccurredAt(localNow.toISOString().slice(0, 16));
    }
    setError(null);
  }, [isOpen, initialTransaction]); // Intentionally exclude `categories` to prevent form resets on background poll

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = parseFloat(amount);
    if (isNaN(num) || num <= 0) {
      setError("Please enter a valid amount greater than 0");
      return;
    }

    setLoading(true);
    setError(null);

    const payload = {
      amount: num,
      type,
      category_id: categoryId || null,
      receiver_vendor: receiverVendor.trim() || null,
      source_app: sourceApp.trim() || "Manual",
      note: note.trim() || null,
      occurred_at: occurredAt ? new Date(occurredAt).toISOString() : new Date().toISOString(),
    };

    let success = false;
    if (initialTransaction && initialTransaction.id) {
      success = await updateTransaction(initialTransaction.id, payload);
    } else {
      const created = await addTransaction(payload);
      success = !!created;
    }

    setLoading(false);
    if (success) {
      onClose();
    } else {
      setError("Failed to save transaction. Please try again.");
    }
  };

  const handleInlineAddCategory = async (catData: any) => {
    const created = await addCategory(catData);
    if (created) {
      setCategoryId(created.id);
      setIsNewCatModalOpen(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B0F17]/85 backdrop-blur-sm p-4 overflow-y-auto animate-in fade-in duration-100">
        <div className="w-full max-w-md bg-[#131A26] border border-[#1E293B] rounded-2xl p-6 shadow-2xl relative my-8">
          {/* Top Bar */}
          <div className="flex items-center justify-between pb-4 border-b border-[#1E293B]">
            <div className="flex items-center gap-2">
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center ${
                  type === "expense"
                    ? "bg-rose-500/10 text-rose-400"
                    : "bg-emerald-500/10 text-emerald-400"
                }`}
              >
                {type === "expense" ? (
                  <TrendingDown className="w-4 h-4" />
                ) : (
                  <TrendingUp className="w-4 h-4" />
                )}
              </div>
              <div>
                <span className="text-[10px] text-emerald-400 font-bold uppercase tracking-wider block leading-none mb-0.5">
                  {vaultLabel}
                </span>
                <h3 className="text-sm font-bold text-white">
                  {initialTransaction ? "Edit Transaction" : "Log Transaction"}
                </h3>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="mt-4 space-y-3.5 text-xs">
            {/* Income / Expense Toggle */}
            <div className="grid grid-cols-2 gap-2 p-1 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
              <button
                type="button"
                onClick={() => setType("expense")}
                className={`py-2 rounded-lg font-semibold transition cursor-pointer flex items-center justify-center gap-1.5 ${
                  type === "expense"
                    ? "bg-rose-500 text-white shadow-sm"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <TrendingDown className="w-3.5 h-3.5" />
                Expense (-)
              </button>
              <button
                type="button"
                onClick={() => setType("income")}
                className={`py-2 rounded-lg font-semibold transition cursor-pointer flex items-center justify-center gap-1.5 ${
                  type === "income"
                    ? "bg-emerald-500 text-white shadow-sm"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <TrendingUp className="w-3.5 h-3.5" />
                Income (+)
              </button>
            </div>

            {/* Amount (₹) */}
            <div>
              <label className="block font-medium text-slate-300 mb-1">
                Amount (₹) *
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-lg font-bold font-mono text-slate-400">
                  ₹
                </span>
                <input
                  type="number"
                  step="any"
                  required
                  autoFocus={!initialTransaction}
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                  className={`w-full pl-8 pr-4 py-2.5 bg-[#0B0F17] border border-[#1E293B] rounded-xl text-lg font-mono font-bold focus:outline-none transition ${
                    type === "expense"
                      ? "text-rose-400 focus:border-rose-500"
                      : "text-emerald-400 focus:border-emerald-500"
                  }`}
                />
              </div>
            </div>

            {/* Receiver / Vendor */}
            <div>
              <label className="block font-medium text-slate-300 mb-1 flex items-center gap-1">
                <Building className="w-3 h-3 text-emerald-400" /> Receiver / Vendor
              </label>
              <input
                type="text"
                value={receiverVendor}
                onChange={(e) => setReceiverVendor(e.target.value)}
                placeholder="e.g. Starbucks, Amazon, Zomato"
                className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2.5 text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Category Picker with Inline + Add */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="font-medium text-slate-300 flex items-center gap-1">
                  <Tag className="w-3 h-3 text-emerald-400" /> Category
                </label>
                <button
                  type="button"
                  onClick={() => setIsNewCatModalOpen(true)}
                  className="text-emerald-400 hover:text-emerald-300 text-[11px] font-semibold flex items-center gap-0.5 cursor-pointer"
                >
                  <Plus className="w-3 h-3" /> New Category
                </button>
              </div>
              <CustomSelect
                value={categoryId}
                onChange={(val) => setCategoryId(val)}
                className="w-full"
                options={[
                  { value: "", label: "Uncategorized", icon: "📦" },
                  ...categories.map((c) => ({
                    value: c.id,
                    label: c.name,
                    icon: c.icon || "🏷️",
                  })),
                ]}
              />
            </div>

            {/* Source App & Date in 2 columns */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Source App */}
              <div>
                <label className="block font-medium text-slate-300 mb-1 flex items-center gap-1">
                  <Smartphone className="w-3 h-3 text-emerald-400" /> Source
                </label>
                <CustomSelect
                  value={sourceApp}
                  onChange={(val) => setSourceApp(val)}
                  className="w-full"
                  options={[
                    { value: "Cash", label: "Cash" },
                    { value: "Manual", label: "Manual" },
                    { value: "Google Pay", label: "Google Pay" },
                    { value: "PhonePe", label: "PhonePe" },
                    { value: "Paytm", label: "Paytm" },
                    { value: "HDFC Bank", label: "HDFC Bank" },
                    { value: "ICICI Bank", label: "ICICI Bank" },
                    { value: "SBI", label: "SBI" },
                    { value: "Axis Bank", label: "Axis Bank" },
                    { value: "Other Bank", label: "Other Bank" },
                  ]}
                />
              </div>

              {/* Date & Time */}
              <div>
                <label className="block font-medium text-slate-300 mb-1 flex items-center gap-1">
                  <Calendar className="w-3 h-3 text-emerald-400" /> Date & Time
                </label>
                <input
                  type="datetime-local"
                  value={occurredAt}
                  onChange={(e) => setOccurredAt(e.target.value)}
                  className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-2.5 py-2 text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>

            {/* Note */}
            <div>
              <label className="block font-medium text-slate-300 mb-1 flex items-center gap-1">
                <FileText className="w-3 h-3 text-emerald-400" /> Note (Optional)
              </label>
              <textarea
                rows={2}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Add any extra notes or memo..."
                className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 resize-none"
              />
            </div>

            {error && (
              <p className="text-rose-400 text-xs bg-rose-500/10 p-2.5 rounded-lg border border-rose-500/20">
                {error}
              </p>
            )}

            {/* Footer Actions */}
            <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#1E293B]">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-slate-300 hover:bg-[#1E293B] transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-semibold flex items-center gap-2 transition cursor-pointer"
              >
                {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                {initialTransaction ? "Update Transaction" : "Save Transaction"}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Inline category modal */}
      <CategoryModal
        isOpen={isNewCatModalOpen}
        onClose={() => setIsNewCatModalOpen(false)}
        onSave={handleInlineAddCategory}
      />
    </>
  );
}
