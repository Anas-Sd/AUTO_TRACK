"use client";

import React from "react";
import { useAuth } from "@/lib/authContext";
import {
  PieChart,
  Receipt,
  FolderTree,
  Settings,
  Plus,
  Shield,
  Zap,
  Radio,
  LogOut,
} from "lucide-react";

interface NavItem {
  id: "overview" | "ledger" | "categories" | "settings";
  label: string;
  icon: any;
  badge?: number;
}

export default function Navbar() {
  const {
    activeTab,
    setActiveTab,
    setIsLogModalOpen,
    vaultLabel,
    logoutVault,
    transactions,
  } = useAuth();

  const navItems: NavItem[] = [
    { id: "overview", label: "Overview", icon: PieChart },
    {
      id: "ledger",
      label: "Ledger",
      icon: Receipt,
      badge: transactions.length > 0 ? transactions.length : undefined,
    },
    { id: "categories", label: "Categories", icon: FolderTree },
    { id: "settings", label: "Settings", icon: Settings },
  ];

  return (
    <header className="sticky top-0 z-40 w-full bg-[#0F172A]/80 backdrop-blur-md border-b border-[#1E293B]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Left Branding */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-md shadow-emerald-500/20">
            <Zap className="w-5 h-5 text-[#0B0F17] font-extrabold fill-current" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold tracking-tight text-white text-base">
                AUTO TRACK
              </span>
              <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <Radio className="w-2.5 h-2.5 animate-pulse text-emerald-400" />
                Live
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium truncate max-w-[120px] sm:max-w-[180px]">
              {vaultLabel}
            </p>
          </div>
        </div>

        {/* Center: Exactly 4 Tabs (Desktop) */}
        <nav className="hidden md:flex items-center space-x-1 bg-[#131A26] p-1.5 rounded-xl border border-[#1E293B]">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  isActive
                    ? "bg-emerald-500 text-white shadow-sm"
                    : "text-slate-400 hover:text-slate-200 hover:bg-[#1E293B]/50"
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
                {item.badge !== undefined && (
                  <span
                    className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                      isActive
                        ? "bg-white/20 text-white"
                        : "bg-[#1E293B] text-slate-400"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* Right Actions */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsLogModalOpen(true)}
            className="hidden sm:flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-semibold transition shadow-md shadow-emerald-500/20 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Manual Log</span>
          </button>

          <button
            onClick={logoutVault}
            title="Log Out & Lock Vault"
            className="hidden sm:flex items-center justify-center w-9 h-9 rounded-xl border border-[#1E293B] hover:bg-[#1E293B] text-slate-400 hover:text-rose-400 transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
}
