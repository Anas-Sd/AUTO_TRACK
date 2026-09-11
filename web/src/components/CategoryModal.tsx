"use client";

import React, { useState, useEffect } from "react";
import { Category } from "@/lib/types";
import { X, FolderPlus, Tag, Palette, DollarSign, Loader2 } from "lucide-react";

interface CategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (cat: Partial<Category>) => Promise<any>;
  category?: Category | null;
}

const PRESET_ICONS = [
  "🍔", "☕", "🛒", "🛍️", "⚡", "🚗", "🎬", "💊",
  "✈️", "🏠", "📚", "🏋️", "🎁", "📱", "🎮", "💰"
];

const PRESET_COLORS = [
  "#10B981", "#3B82F6", "#8B5CF6", "#EC4899",
  "#F59E0B", "#EF4444", "#06B6D4", "#64748B"
];

export default function CategoryModal({
  isOpen,
  onClose,
  onSave,
  category,
}: CategoryModalProps) {
  const [name, setName] = useState("");
  const [icon, setIcon] = useState("🏷️");
  const [color, setColor] = useState("#10B981");
  const [monthlyCap, setMonthlyCap] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (category) {
      setName(category.name || "");
      setIcon(category.icon || "🏷️");
      setColor(category.color || "#10B981");
      setMonthlyCap(category.monthly_cap ? category.monthly_cap.toString() : "");
    } else {
      setName("");
      setIcon("🏷️");
      setColor("#10B981");
      setMonthlyCap("");
    }
    setError(null);
  }, [category, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError("Category name is required");
      return;
    }

    setLoading(true);
    setError(null);

    const capNum = monthlyCap.trim() ? parseFloat(monthlyCap) : null;
    const res = await onSave({
      name: name.trim(),
      icon: icon || "🏷️",
      color: color || "#10B981",
      monthly_cap: capNum && !isNaN(capNum) ? capNum : null,
    });

    setLoading(false);
    if (res) {
      onClose();
    } else {
      setError("Failed to save category. A category with this name might already exist.");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B0F17]/80 backdrop-blur-sm p-4 animate-in fade-in duration-100">
      <div className="w-full max-w-md bg-[#131A26] border border-[#1E293B] rounded-2xl p-6 shadow-2xl relative">
        <div className="flex items-center justify-between pb-4 border-b border-[#1E293B]">
          <div className="flex items-center gap-2">
            <FolderPlus className="w-5 h-5 text-emerald-400" />
            <h3 className="text-sm font-bold text-white">
              {category ? "Edit Category" : "New Category"}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4 text-xs">
          {/* Category Name */}
          <div>
            <label className="block font-medium text-slate-300 mb-1.5 flex items-center gap-1">
              <Tag className="w-3.5 h-3.5 text-emerald-400" /> Name *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Groceries, Entertainment, Fuel"
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2.5 text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
            />
          </div>

          {/* Icon Selector */}
          <div>
            <label className="block font-medium text-slate-300 mb-1.5">
              Select Emoji / Icon
            </label>
            <div className="flex flex-wrap gap-2 p-2 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
              {PRESET_ICONS.map((emoji) => (
                <button
                  type="button"
                  key={emoji}
                  onClick={() => setIcon(emoji)}
                  className={`w-8 h-8 rounded-lg text-sm flex items-center justify-center transition cursor-pointer ${
                    icon === emoji
                      ? "bg-emerald-500/20 border border-emerald-500 text-base"
                      : "hover:bg-[#1E293B]"
                  }`}
                >
                  {emoji}
                </button>
              ))}
            </div>
          </div>

          {/* Color Selector */}
          <div>
            <label className="block font-medium text-slate-300 mb-1.5 flex items-center gap-1">
              <Palette className="w-3.5 h-3.5 text-emerald-400" /> Color Accent
            </label>
            <div className="flex items-center gap-2 p-2 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
              {PRESET_COLORS.map((c) => (
                <button
                  type="button"
                  key={c}
                  onClick={() => setColor(c)}
                  style={{ backgroundColor: c }}
                  className={`w-6 h-6 rounded-full transition cursor-pointer ${
                    color === c ? "ring-2 ring-white ring-offset-2 ring-offset-[#0B0F17]" : "opacity-80 hover:opacity-100"
                  }`}
                />
              ))}
              <input
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                className="w-6 h-6 rounded-full bg-transparent border-none cursor-pointer ml-auto"
                title="Custom color"
              />
            </div>
          </div>

          {/* Opening Balance (Optional) */}
          <div>
            <label className="block font-medium text-slate-300 mb-1.5 flex items-center gap-1">
              <DollarSign className="w-3.5 h-3.5 text-emerald-400" /> Opening Balance (Optional ₹)
            </label>
            <input
              type="number"
              step="any"
              value={monthlyCap}
              onChange={(e) => setMonthlyCap(e.target.value)}
              placeholder="e.g. 10000 (leave blank if no opening balance)"
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2.5 text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 font-mono"
            />
          </div>

          {error && (
            <p className="text-rose-400 text-xs bg-rose-500/10 p-2.5 rounded-lg border border-rose-500/20">
              {error}
            </p>
          )}

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#1E293B]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-slate-300 hover:bg-[#1E293B] transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-semibold flex items-center gap-2 transition cursor-pointer"
            >
              {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              {category ? "Save Changes" : "Create Category"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
