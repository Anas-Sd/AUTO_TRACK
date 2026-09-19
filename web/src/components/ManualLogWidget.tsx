"use client";

import React, { useState } from "react";
import { useAuth } from "@/lib/authContext";
import CategoryModal from "./CategoryModal";
import CustomSelect from "./CustomSelect";
import {
  TrendingDown,
  TrendingUp,
  ArrowRight,
  ArrowLeft,
  Check,
  X,
  Plus,
  Tag,
  Wallet,
  Smartphone,
  FileText,
  Loader2,
  Calendar,
} from "lucide-react";
import { Transaction } from "@/lib/types";

interface ManualLogWidgetProps {
  onClose?: () => void;
  initialTransaction?: Transaction | null;
}

const toDatetimeLocal = (isoStr: string) => {
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return "";
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    const hours = String(d.getHours()).padStart(2, "0");
    const minutes = String(d.getMinutes()).padStart(2, "0");
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  } catch {
    return "";
  }
};

export default function ManualLogWidget({
  onClose,
  initialTransaction,
}: ManualLogWidgetProps) {
  const { categories, addTransaction, updateTransaction, addCategory } = useAuth();

  // Navigation level: 1 or 2
  const [level, setLevel] = useState<1 | 2>(1);

  // Level 1 State
  const [type, setType] = useState<"income" | "expense">("expense"); // "expense" = Outcome
  const [receiverVendor, setReceiverVendor] = useState("");
  const [amount, setAmount] = useState("");
  const [level1Error, setLevel1Error] = useState<string | null>(null);
  const [errorField, setErrorField] = useState<"toFrom" | "amount" | null>(null);

  // Level 2 State
  const [categoryId, setCategoryId] = useState<string>("");
  const [paymentMethod, setPaymentMethod] = useState<"UPI" | "Cash">("UPI");
  const [note, setNote] = useState("");
  const [occurredAt, setOccurredAt] = useState<string>("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState(false);

  // Inline Category Modal State
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  React.useEffect(() => {
    if (initialTransaction) {
      setAmount(initialTransaction.amount ? initialTransaction.amount.toString() : "");
      setType(initialTransaction.type || "expense");
      setReceiverVendor(initialTransaction.receiver_vendor || "");
      setCategoryId(initialTransaction.category_id || "");
      setPaymentMethod(initialTransaction.source_app === "Cash" ? "Cash" : "UPI");
      setNote(initialTransaction.note || "");
      if (initialTransaction.occurred_at) {
        setOccurredAt(toDatetimeLocal(initialTransaction.occurred_at));
      } else {
        setOccurredAt(toDatetimeLocal(new Date().toISOString()));
      }
      setLevel(2);
    } else {
      setOccurredAt(toDatetimeLocal(new Date().toISOString()));
    }
  }, [initialTransaction]);

  // Handle Level 1 -> Level 2 transition
  const handleContinue = () => {
    const num = parseFloat(amount);
    if (isNaN(num) || num <= 0) {
      setErrorField("amount");
      setLevel1Error("Please enter a valid amount");
      return;
    }
    setErrorField(null);
    setLevel1Error(null);
    setLevel(2);
  };

  // Handle Level 2 Save
  const handleSaveTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = parseFloat(amount);
    if (isNaN(num) || num <= 0) {
      setErrorField("amount");
      setLevel1Error("Please enter amount");
      setLevel(1);
      return;
    }

    setIsSaving(true);
    setSaveError(null);

    const finalOccurredAt = occurredAt
      ? new Date(occurredAt).toISOString()
      : initialTransaction?.occurred_at || new Date().toISOString();

    const payload = {
      amount: num,
      type,
      receiver_vendor: receiverVendor.trim() || null,
      category_id: categoryId || null,
      source_app: paymentMethod,
      note: note.trim() || null,
      occurred_at: finalOccurredAt,
    };

    let result = false;
    if (initialTransaction && initialTransaction.id) {
      result = await updateTransaction(initialTransaction.id, payload);
    } else {
      const created = await addTransaction(payload);
      result = !!created;
    }
    setIsSaving(false);

    if (result) {
      if (onClose) {
        onClose();
      } else {
        // Reset form to Level 1
        setAmount("");
        setType("expense");
        setCategoryId("");
        setPaymentMethod("UPI");
        setNote("");
        setLevel1Error(null);
        setLevel(1);
        setSuccessToast(true);
        setTimeout(() => setSuccessToast(false), 2500);
      }
    } else {
      setSaveError("Failed to save. Please try again.");
    }
  };

  // Inline Category Creation Callback (auto-selects newly created category)
  const handleCategorySave = async (catData: any) => {
    const created = await addCategory(catData);
    if (created && created.id) {
      setCategoryId(created.id);
      setIsCategoryModalOpen(false);
    }
    return created;
  };

  return (
    <div className="w-full bg-[#131A26] border border-[#1E293B] rounded-2xl p-3.5 sm:p-4 shadow-md transition-all duration-200 shrink-0">
      {/* Success Toast Banner */}
      {successToast && (
        <div className="mb-2 p-2 bg-emerald-500/15 border border-emerald-500/30 rounded-xl text-emerald-400 text-xs font-medium flex items-center justify-between animate-in fade-in duration-150">
          <span className="flex items-center gap-1.5">
            <Check className="w-4 h-4 text-emerald-400" /> Transaction saved successfully!
          </span>
          <span className="text-[10px] text-slate-400">Synced</span>
        </div>
      )}

      {level === 1 ? (
        /* ================= LEVEL 1 VIEW ================= */
        <div className="space-y-3">
          {/* Modal Header when rendered as Popcard Modal */}
          {onClose && (
            <div className="flex items-center justify-between pb-2 border-b border-[#1E293B] text-xs font-bold text-white">
              <span>{initialTransaction ? "Edit Transaction" : "Log Transaction"}</span>
              <button
                type="button"
                onClick={onClose}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Row 1: Income / Outcome Toggle (Outcome selected by default) */}
          <div className="grid grid-cols-2 gap-2 p-1 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
            <button
              type="button"
              onClick={() => {
                setType("expense");
                if (level1Error) setLevel1Error(null);
              }}
              className={`py-2 rounded-lg font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1.5 ${
                type === "expense"
                  ? "bg-rose-500 text-white shadow-xs"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <TrendingDown className="w-3.5 h-3.5" /> Outcome (-)
            </button>
            <button
              type="button"
              onClick={() => {
                setType("income");
                if (level1Error) setLevel1Error(null);
              }}
              className={`py-2 rounded-lg font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1.5 ${
                type === "income"
                  ? "bg-emerald-500 text-white shadow-xs"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <TrendingUp className="w-3.5 h-3.5" /> Income (+)
            </button>
          </div>

          {/* Row 2: Amount Field */}
          <div>
            <label className="block text-[11px] font-medium text-slate-300 mb-1">
              Amount
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm font-bold font-mono text-slate-400">
                ₹
              </span>
              <input
                type="number"
                step="any"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  if (errorField === "amount") {
                    setErrorField(null);
                    setLevel1Error(null);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleContinue();
                  }
                }}
                placeholder="0.00"
                className={`w-full pl-7 pr-3 py-2 bg-[#0B0F17] border rounded-xl font-mono text-sm font-bold focus:outline-none transition ${
                  errorField === "amount"
                    ? "border-rose-500 text-rose-400 focus:border-rose-500"
                    : type === "expense"
                    ? "border-[#1E293B] text-rose-400 focus:border-rose-500"
                    : "border-[#1E293B] text-emerald-400 focus:border-emerald-500"
                }`}
              />
            </div>
          </div>

          {/* Row 3: Notes / Description */}
          <div>
            <label className="block text-[11px] font-medium text-slate-300 mb-1 flex items-center gap-1">
              <FileText className="w-3 h-3 text-emerald-400" /> Notes / Description (Optional)
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. Tea, Grocery, Salary..."
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3 py-2 text-white text-xs placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
            />
          </div>

          {/* Continue Button */}
          <button
            type="button"
            onClick={handleContinue}
            className="w-full py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold text-xs rounded-xl flex items-center justify-center gap-1.5 transition cursor-pointer shadow-sm mt-2"
          >
            <span>Continue to Level 2</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>

          {/* Validation Error Message */}
          {level1Error && (
            <p className="text-rose-400 text-[11px] font-medium animate-in fade-in duration-100 flex items-center gap-1">
              • {level1Error}
            </p>
          )}
        </div>
      ) : (
        /* ================= LEVEL 2 VIEW ================= */
        <form onSubmit={handleSaveTransaction} className="space-y-3 animate-in fade-in slide-in-from-right-2 duration-150">
          {/* Summary Indicator Header */}
          <div className="flex items-center justify-between pb-2 border-b border-[#1E293B] text-xs">
            <div className="flex items-center gap-1.5 font-mono font-bold">
              <span className={type === "expense" ? "text-rose-400" : "text-emerald-400"}>
                {type === "expense" ? "- Outcome" : "+ Income"}: ₹{parseFloat(amount || "0").toLocaleString("en-IN")}
              </span>
            </div>
            <button
              type="button"
              onClick={() => setLevel(1)}
              className="px-2.5 py-1 rounded-lg bg-[#0B0F17] hover:bg-[#1E293B] border border-[#1E293B] text-slate-300 hover:text-white text-[11px] font-semibold flex items-center gap-1 transition cursor-pointer"
            >
              <ArrowLeft className="w-3 h-3 text-emerald-400" /> Back to Level 1
            </button>
          </div>

          {/* Category Dropdown + Inline + New Category */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-[11px] font-medium text-slate-300 flex items-center gap-1">
                <Tag className="w-3 h-3 text-emerald-400" /> Category
              </label>
              <button
                type="button"
                onClick={() => setIsCategoryModalOpen(true)}
                className="text-emerald-400 hover:text-emerald-300 text-[11px] font-semibold flex items-center gap-0.5 cursor-pointer"
              >
                <Plus className="w-3 h-3" /> New Category
              </button>
            </div>
            <CustomSelect
              value={categoryId}
              onChange={(val) => setCategoryId(val)}
              className="w-full text-xs"
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

          {/* Payment Method (Cash vs UPI - Default: UPI) */}
          <div>
            <label className="block text-[11px] font-medium text-slate-300 mb-1 flex items-center gap-1">
              Payment Method
            </label>
            <div className="grid grid-cols-2 gap-2 p-1 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
              <button
                type="button"
                onClick={() => setPaymentMethod("UPI")}
                className={`py-1.5 rounded-lg font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1.5 ${
                  paymentMethod === "UPI"
                    ? "bg-emerald-500 text-white shadow-xs"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <Smartphone className="w-3.5 h-3.5" /> UPI
              </button>
              <button
                type="button"
                onClick={() => setPaymentMethod("Cash")}
                className={`py-1.5 rounded-lg font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1.5 ${
                  paymentMethod === "Cash"
                    ? "bg-amber-500 text-white shadow-xs"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <Wallet className="w-3.5 h-3.5" /> Cash
              </button>
            </div>
          </div>

          {/* Notes (Optional) */}
          <div>
            <label className="block text-[11px] font-medium text-slate-300 mb-1 flex items-center gap-1">
              <FileText className="w-3 h-3 text-emerald-400" /> Notes (Optional)
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. Tea with friends, Grocery..."
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3 py-1.5 text-white text-xs placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
            />
          </div>

          {/* Date & Time (Editable) */}
          <div>
            <label className="block text-[11px] font-medium text-slate-300 mb-1 flex items-center gap-1">
              <Calendar className="w-3 h-3 text-emerald-400" /> Date & Time (IST)
            </label>
            <input
              type="datetime-local"
              value={occurredAt}
              onChange={(e) => setOccurredAt(e.target.value)}
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3 py-1.5 text-white text-xs focus:outline-none focus:border-emerald-500"
            />
          </div>

          {saveError && (
            <p className="text-rose-400 text-[11px] bg-rose-500/10 p-2 rounded-lg border border-rose-500/20">
              {saveError}
            </p>
          )}

          {/* Action Row: Back, Cancel, and Save Transaction */}
          <div className="flex items-center gap-2 pt-1 border-t border-[#1E293B]">
            <button
              type="button"
              onClick={() => setLevel(1)}
              className="px-3 py-2 rounded-xl bg-[#0B0F17] hover:bg-[#1E293B] border border-[#1E293B] text-slate-300 font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1 shrink-0"
            >
              <ArrowLeft className="w-3.5 h-3.5 text-emerald-400" /> Back
            </button>

            <button
              type="button"
              onClick={() => {
                setAmount("");
                setType("expense");
                setCategoryId("");
                setPaymentMethod("UPI");
                setNote("");
                setLevel1Error(null);
                setLevel(1);
              }}
              className="px-3 py-2 rounded-xl bg-[#0B0F17] hover:bg-[#1E293B] border border-[#1E293B] text-slate-400 hover:text-slate-200 font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1 shrink-0"
            >
              <X className="w-3.5 h-3.5" /> Cancel
            </button>

            <button
              type="submit"
              disabled={isSaving}
              className="flex-1 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-semibold text-xs transition cursor-pointer flex items-center justify-center gap-1 shadow-sm disabled:opacity-50"
            >
              {isSaving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
              <span>Save Transaction</span>
            </button>
          </div>
        </form>
      )}

      {/* Inline Category Adding Modal */}
      <CategoryModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        onSave={handleCategorySave}
      />
    </div>
  );
}
