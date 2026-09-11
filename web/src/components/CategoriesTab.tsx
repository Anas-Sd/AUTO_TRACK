"use client";

import React, { useState, useMemo } from "react";
import { useAuth } from "@/lib/authContext";
import { Category } from "@/lib/types";
import CategoryModal from "./CategoryModal";
import ConfirmModal from "./ConfirmModal";
import {
  FolderTree,
  Plus,
  Edit2,
  Trash2,
  AlertCircle,
  TrendingDown,
  Sparkles,
  Layers,
} from "lucide-react";

export default function CategoriesTab() {
  const {
    categories,
    transactions,
    addCategory,
    updateCategory,
    deleteCategory,
    setLedgerCategoryFilter,
    setActiveTab,
  } = useAuth();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCat, setEditingCat] = useState<Category | null>(null);
  const [deleteConfirmCat, setDeleteConfirmCat] = useState<{ id: string; name: string } | null>(null);

  // Compute total spend per category across ALL time (does not reset monthly)
  const categoryTotalSpend = useMemo(() => {
    const map = new Map<string, number>();

    transactions.forEach((t) => {
      if (t.type === "expense" && t.category_id) {
        const current = map.get(t.category_id) || 0;
        map.set(t.category_id, current + (Number(t.amount) || 0));
      }
    });

    return map;
  }, [transactions]);

  const handleCreate = () => {
    setEditingCat(null);
    setIsModalOpen(true);
  };

  const handleEdit = (cat: Category) => {
    setEditingCat(cat);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string, name: string) => {
    setDeleteConfirmCat({ id, name });
  };

  const handleSave = async (catData: Partial<Category>) => {
    if (editingCat) {
      return await updateCategory(editingCat.id, catData);
    } else {
      return await addCategory(catData);
    }
  };

  return (
    <div className="h-[calc(100vh-6.5rem)] md:h-[calc(100vh-7.5rem)] flex flex-col space-y-4 pb-2 overflow-hidden">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 shrink-0">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <FolderTree className="w-5 h-5 text-emerald-400" />
            Categories & Opening Balances
          </h2>
          <p className="text-xs text-slate-400">
            Define classification tags and track opening balances & transaction totals
          </p>
        </div>

        <button
          onClick={handleCreate}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-semibold transition shadow-md shadow-emerald-500/20 self-start sm:self-auto cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Add Category</span>
        </button>
      </div>

      {/* Categories Grid Container - Fixed Box with Internal Scroll */}
      <div className="flex-1 min-h-0 overflow-y-auto custom-scrollbar pr-1 pb-16 md:pb-4">
        {categories.length === 0 ? (
        <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-12 text-center">
          <Layers className="w-12 h-12 mx-auto mb-3 opacity-30 text-slate-400" />
          <h3 className="text-sm font-semibold text-slate-300">
            No categories available. Click &apos;Add Category&apos; above to create one.
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Create custom categories to organize your transactions and track opening balances.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map((cat) => {
            const spent = categoryTotalSpend.get(cat.id) || 0;
            const openingBalance = cat.monthly_cap;
            const hasBalance = openingBalance !== null && openingBalance !== undefined && openingBalance > 0;
            const percent = hasBalance ? Math.min(Math.round((spent / openingBalance) * 100), 100) : 0;
            const isOverBalance = hasBalance && spent > openingBalance;

            return (
              <div
                key={cat.id}
                onClick={() => {
                  setLedgerCategoryFilter(cat.id);
                  setActiveTab("ledger");
                }}
                title={`Click to view all ${cat.name} transactions in Ledger`}
                className="bg-[#131A26] border border-[#1E293B] hover:border-emerald-500/50 rounded-2xl p-5 transition flex flex-col justify-between group shadow-sm cursor-pointer hover:bg-[#131A26]/80 active:scale-[0.99]"
              >
                <div>
                  {/* Top: Icon + Name + Actions */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className="w-10 h-10 rounded-xl flex items-center justify-center text-lg shadow-sm"
                        style={{
                          backgroundColor: `${cat.color || "#10B981"}20`,
                          borderColor: `${cat.color || "#10B981"}40`,
                        }}
                      >
                        {cat.icon || "🏷️"}
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-white flex items-center gap-1.5 group-hover:text-emerald-400 transition">
                          {cat.name}
                        </h4>
                        <span
                          className="inline-block w-2.5 h-1 rounded-full mt-0.5"
                          style={{ backgroundColor: cat.color || "#10B981" }}
                        />
                      </div>
                    </div>

                    <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleEdit(cat);
                        }}
                        title="Edit category"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(cat.id, cat.name);
                        }}
                        title="Delete category"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Total Spend & Opening Balance Status */}
                  <div className="mt-5 space-y-2">
                    <div className="flex items-baseline justify-between">
                      <span className="text-[11px] text-slate-400 flex items-center gap-1">
                        <TrendingDown className="w-3 h-3 text-slate-500" /> Total Spent
                      </span>
                      <span className="font-mono text-xs font-bold text-white">
                        ₹{spent.toLocaleString("en-IN")}
                        {hasBalance && (
                          <span className="text-slate-500 font-normal">
                            {" "}
                            / Opening: ₹{openingBalance.toLocaleString("en-IN")}
                          </span>
                        )}
                      </span>
                    </div>

                    {/* Progress Bar if Opening Balance is set */}
                    {hasBalance ? (
                      <div className="space-y-1">
                        <div className="w-full h-2 bg-[#0B0F17] rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all duration-300 ${
                              isOverBalance
                                ? "bg-rose-500"
                                : percent > 80
                                ? "bg-amber-500"
                                : "bg-emerald-500"
                            }`}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                        <div className="flex justify-between items-center text-[10px]">
                          <span
                            className={
                              isOverBalance
                                ? "text-rose-400 font-semibold"
                                : "text-slate-500"
                            }
                          >
                            {isOverBalance ? "⚠️ Over opening balance" : `${percent}% used`}
                          </span>
                          <span className="text-slate-500 font-mono">
                            {hasBalance ? `Rem: ₹${(openingBalance - spent).toLocaleString("en-IN")}` : ""}
                          </span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-[10px] text-slate-500 italic">No opening balance set</p>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
      </div>

      {/* Modal */}
      <CategoryModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSave}
        category={editingCat}
      />

      {/* Delete Confirmation Popup */}
      <ConfirmModal
        isOpen={!!deleteConfirmCat}
        title="Delete Category"
        message={
          deleteConfirmCat
            ? `Are you sure you want to delete "${deleteConfirmCat.name}"? Transactions assigned to this category will become Uncategorized.`
            : ""
        }
        confirmText="Delete Category"
        onConfirm={async () => {
          if (deleteConfirmCat) {
            await deleteCategory(deleteConfirmCat.id);
          }
        }}
        onClose={() => setDeleteConfirmCat(null)}
      />
    </div>
  );
}
