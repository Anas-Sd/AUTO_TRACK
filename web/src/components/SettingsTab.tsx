"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/lib/authContext";
import {
  Settings,
  Shield,
  Check,
  LogOut,
  Save,
  Radio,
} from "lucide-react";

export default function SettingsTab() {
  const {
    vaultLabel,
    updateVaultLabel,
    logoutVault,
  } = useAuth();

  const [labelInput, setLabelInput] = useState(vaultLabel);
  const [isSavingLabel, setIsSavingLabel] = useState(false);
  const [labelSavedSuccess, setLabelSavedSuccess] = useState(false);

  useEffect(() => {
    setLabelInput(vaultLabel);
  }, [vaultLabel]);

  const handleSaveLabel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!labelInput.trim()) return;
    setIsSavingLabel(true);
    await updateVaultLabel(labelInput.trim());
    setIsSavingLabel(false);
    setLabelSavedSuccess(true);
    setTimeout(() => setLabelSavedSuccess(false), 2000);
  };

  return (
    <div className="space-y-6 max-w-3xl pb-32 md:pb-8">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Settings className="w-5 h-5 text-emerald-400" />
          Settings
        </h2>
        <p className="text-xs text-slate-400">
          Manage your vault profile and account session
        </p>
      </div>

      {/* 1. Vault Display Name / Label */}
      <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
        <div className="flex items-center gap-2.5 pb-3 border-b border-[#1E293B]">
          <Shield className="w-4 h-4 text-emerald-400" />
          <h3 className="text-sm font-semibold text-white">Vault Profile</h3>
        </div>

        <form onSubmit={handleSaveLabel} className="space-y-3">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Display Name
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={labelInput}
                onChange={(e) => setLabelInput(e.target.value)}
                placeholder="e.g. Anas's Personal Vault"
                className="flex-1 bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2.5 text-white text-xs focus:outline-none focus:border-emerald-500"
              />
              <button
                type="submit"
                disabled={isSavingLabel}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-semibold text-xs transition cursor-pointer"
              >
                {labelSavedSuccess ? (
                  <>
                    <Check className="w-3.5 h-3.5" /> Saved
                  </>
                ) : (
                  <>
                    <Save className="w-3.5 h-3.5" /> Save
                  </>
                )}
              </button>
            </div>
            <p className="text-[11px] text-slate-500 mt-1">
              This display name identifies your vault on your phone and web.
            </p>
          </div>
        </form>
      </div>

      {/* 2. Storage & Security Status */}
      <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-3 shadow-sm text-xs">
        <h3 className="text-sm font-semibold text-white">System Diagnostics</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          <div className="p-3 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
            <span className="text-[10px] text-slate-500 uppercase tracking-wider">
              Database Sync
            </span>
            <p className="font-semibold text-slate-200 mt-0.5">Supabase PostgreSQL</p>
            <p className="text-[10px] text-emerald-400 flex items-center gap-1 mt-0.5">
              <Radio className="w-2.5 h-2.5 animate-pulse" /> Live Sync Active
            </p>
          </div>
          <div className="p-3 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
            <span className="text-[10px] text-slate-500 uppercase tracking-wider">
              Offline Queue Engine
            </span>
            <p className="font-semibold text-slate-200 mt-0.5">SQLite Local Storage</p>
            <p className="text-[10px] text-slate-400 mt-0.5">Offline-first queue ready</p>
          </div>
        </div>
      </div>

      {/* 3. Account Session: Log Out Only */}
      <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h4 className="text-sm font-bold text-slate-300 flex items-center gap-2">
              <LogOut className="w-4 h-4 text-emerald-400" /> Account Session
            </h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Clears session. You will need your vault code to log back in.
            </p>
          </div>
          <button
            onClick={logoutVault}
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-white font-semibold text-xs flex items-center justify-center gap-2 transition cursor-pointer"
          >
            <LogOut className="w-4 h-4 text-emerald-400" />
            <span>Log Out</span>
          </button>
        </div>
      </div>
    </div>
  );
}
