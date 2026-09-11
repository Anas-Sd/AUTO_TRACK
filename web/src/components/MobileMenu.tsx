"use client";

import React, { useState } from "react";
import { useAuth } from "@/lib/authContext";
import {
  Plus,
  X,
  PieChart,
  Receipt,
  FolderTree,
  Settings,
  LogOut,
  Sparkles,
} from "lucide-react";

export default function MobileMenu() {
  const [open, setOpen] = useState(false);
  const { activeTab, setActiveTab, setIsLogModalOpen, logoutVault } = useAuth();

  const handleSelectTab = (tab: "overview" | "ledger" | "categories" | "settings") => {
    setActiveTab(tab);
    setOpen(false);
  };

  const handleOpenManualLog = () => {
    setOpen(false);
    setIsLogModalOpen(true);
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 md:hidden">
      {/* Backdrop */}
      {open && (
        <div
          onClick={() => setOpen(false)}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 transition-opacity"
        />
      )}

      {/* Expanded Menu */}
      {open && (
        <div className="absolute bottom-16 right-0 mb-2 w-64 bg-[#131A26] border border-[#1E293B] rounded-2xl shadow-2xl p-2.5 z-50 flex flex-col gap-1.5 animate-in fade-in slide-in-from-bottom-5 duration-150">
          {/* Navigation Items */}
          <div className="flex flex-col gap-1">
            <button
              onClick={handleOpenManualLog}
              className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium text-slate-300 hover:bg-[#1E293B]/50 hover:text-white transition cursor-pointer"
            >
              <Plus className="w-4 h-4 text-[#10B981]" />
              Manual Log Transaction
            </button>
            <button
              onClick={() => handleSelectTab("overview")}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                activeTab === "overview"
                  ? "bg-[#1E293B] text-emerald-400 font-semibold"
                  : "text-slate-300 hover:bg-[#1E293B]/50 hover:text-white"
              }`}
            >
              <PieChart className="w-4 h-4 text-slate-400" />
              Financial Overview
            </button>

            <button
              onClick={() => handleSelectTab("ledger")}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                activeTab === "ledger"
                  ? "bg-[#1E293B] text-emerald-400 font-semibold"
                  : "text-slate-300 hover:bg-[#1E293B]/50 hover:text-white"
              }`}
            >
              <Receipt className="w-4 h-4 text-slate-400" />
              Transactions Ledger
            </button>

            <button
              onClick={() => handleSelectTab("categories")}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                activeTab === "categories"
                  ? "bg-[#1E293B] text-emerald-400 font-semibold"
                  : "text-slate-300 hover:bg-[#1E293B]/50 hover:text-white"
              }`}
            >
              <FolderTree className="w-4 h-4 text-slate-400" />
              Categories & Budgets
            </button>

            <button
              onClick={() => handleSelectTab("settings")}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                activeTab === "settings"
                  ? "bg-[#1E293B] text-emerald-400 font-semibold"
                  : "text-slate-300 hover:bg-[#1E293B]/50 hover:text-white"
              }`}
            >
              <Settings className="w-4 h-4 text-slate-400" />
              System Settings
            </button>
          </div>

          <div className="my-1 border-t border-[#1E293B]" />

          {/* Destructive Action */}
          <button
            onClick={() => {
              setOpen(false);
              logoutVault();
            }}
            className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium text-rose-400 hover:bg-rose-500/10 transition"
          >
            <LogOut className="w-4 h-4" />
            Log Out & Lock Vault
          </button>
        </div>
      )}

      {/* Floating Circular Trigger Button */}
      <button
        onClick={() => setOpen(!open)}
        aria-label="Navigation Menu"
        className={`w-14 h-14 rounded-full shadow-2xl flex items-center justify-center transition-transform active:scale-95 ${
          open
            ? "bg-[#1E293B] text-white border border-[#334155]"
            : "bg-gradient-to-tr from-emerald-500 to-teal-400 text-[#0B0F17] shadow-emerald-500/30"
        }`}
      >
        {open ? (
          <X className="w-6 h-6 stroke-[2.5]" />
        ) : (
          <Plus className="w-7 h-7 stroke-[2.5]" />
        )}
      </button>
    </div>
  );
}
