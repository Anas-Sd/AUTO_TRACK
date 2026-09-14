"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/lib/authContext";
import ConfirmModal from "./ConfirmModal";
import {
  Settings,
  Shield,
  Copy,
  Check,
  LogOut,
  Save,
  Trash2,
  Info,
  Radio,
  RefreshCw,
  Loader2,
  Eye,
  EyeOff,
  KeyRound,
} from "lucide-react";

export default function SettingsTab() {
  const {
    vaultCode,
    vaultLabel,
    updateVaultLabel,
    logoutVault,
    rotateVaultCode,
    wipeVaultData,
    deleteVaultPermanently,
  } = useAuth();

  const [labelInput, setLabelInput] = useState(vaultLabel);
  const [isSavingLabel, setIsSavingLabel] = useState(false);
  const [labelSavedSuccess, setLabelSavedSuccess] = useState(false);

  // Vault Code Reveal & Rotation states
  const [showVaultCode, setShowVaultCode] = useState(false);
  const [isRotatingCode, setIsRotatingCode] = useState(false);
  const [rotateConfirmOpen, setRotateConfirmOpen] = useState(false);
  const [rotateSuccessMsg, setRotateSuccessMsg] = useState<string | null>(null);
  const [hasCopiedCode, setHasCopiedCode] = useState(false);

  // Wipe Data states
  const [isWipingData, setIsWipingData] = useState(false);
  const [wipeConfirmOpen, setWipeConfirmOpen] = useState(false);
  const [wipeSuccessMsg, setWipeSuccessMsg] = useState<string | null>(null);

  // Delete Vault Account states
  const [isDeletingVault, setIsDeletingVault] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  // Check if running inside Android APK Mobile App
  const [isAndroidApp, setIsAndroidApp] = useState(false);

  const activeCode = vaultCode || "";

  useEffect(() => {
    setLabelInput(vaultLabel);
    if (typeof window !== "undefined" && (window as any).AndroidBridge != null) {
      setIsAndroidApp(true);
    }
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

  const handlePerformRotation = async () => {
    setRotateConfirmOpen(false);
    setIsRotatingCode(true);
    const newCode = await rotateVaultCode();
    setIsRotatingCode(false);
    if (newCode) {
      if (typeof window !== "undefined" && (window as any).AndroidBridge?.setVaultCode) {
        (window as any).AndroidBridge.setVaultCode(newCode);
      }
      setRotateSuccessMsg(`Vault Code successfully rotated to ${newCode}!`);
      setTimeout(() => setRotateSuccessMsg(null), 5000);
    }
  };

  const copyVaultCode = () => {
    if (!activeCode) return;
    navigator.clipboard?.writeText(activeCode);
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.copyToClipboard) {
      (window as any).AndroidBridge.copyToClipboard(activeCode);
    }
    setHasCopiedCode(true);
    setTimeout(() => setHasCopiedCode(false), 2000);
  };

  const handlePerformWipe = async () => {
    setIsWipingData(true);
    const ok = await wipeVaultData();
    setIsWipingData(false);
    setWipeConfirmOpen(false);
    if (ok) {
      setWipeSuccessMsg("All transactions and categories have been permanently wiped.");
      setTimeout(() => setWipeSuccessMsg(null), 5000);
    }
  };

  const handlePerformDeleteVault = async () => {
    setIsDeletingVault(true);
    await deleteVaultPermanently();
    setIsDeletingVault(false);
    setDeleteConfirmOpen(false);
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
          Manage your vault profile, vault code security, and data storage
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

      {/* 2. Vault Code (Visible ONLY inside Android App) */}
      {isAndroidApp && (
        <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
          <div className="flex items-center justify-between pb-3 border-b border-[#1E293B]">
            <div className="flex items-center gap-2.5">
              <KeyRound className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-white">Vault Access Code</h3>
            </div>
            <span className="text-[10px] text-slate-500 font-mono">Secret Key</span>
          </div>

          <div className="p-4 rounded-xl bg-[#0B0F17] border border-[#1E293B] space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              {/* Masked / Visible Code */}
              <div className="flex items-center gap-2">
                <span className="font-mono text-lg font-bold text-emerald-400 tracking-widest">
                  {showVaultCode ? activeCode : "••••••••"}
                </span>
                <button
                  type="button"
                  onClick={() => setShowVaultCode(!showVaultCode)}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
                  title={showVaultCode ? "Hide Vault Code" : "Show Vault Code"}
                >
                  {showVaultCode ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              {/* Action Buttons: Copy & Rotate */}
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={copyVaultCode}
                  className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-white text-xs font-medium transition cursor-pointer flex-1 sm:flex-initial"
                >
                  {hasCopiedCode ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" /> Copied
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" /> Copy Code
                    </>
                  )}
                </button>

                <button
                  onClick={() => setRotateConfirmOpen(true)}
                  disabled={isRotatingCode}
                  className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-emerald-400 text-xs font-semibold transition cursor-pointer disabled:opacity-50 flex-1 sm:flex-initial"
                >
                  {isRotatingCode ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <RefreshCw className="w-3.5 h-3.5" />
                  )}
                  <span>Rotate Code</span>
                </button>
              </div>
            </div>

            {rotateSuccessMsg && (
              <p className="text-[11px] text-emerald-400 font-medium bg-emerald-500/10 p-2.5 rounded-lg border border-emerald-500/20 animate-in fade-in duration-150">
                {rotateSuccessMsg}
              </p>
            )}

            <p className="text-[11px] text-slate-500">
              Use this Vault Code to log into your account across devices. Keep it secret.
            </p>
          </div>
        </div>
      )}

      {/* 3. Storage & Security Status */}
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

      {/* 4. Danger Zone: Wipe Data, Logout, & Delete Account */}
      <div className="bg-[#131A26] border border-rose-500/20 rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
        {/* Wipe Data */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-rose-500/20">
          <div>
            <h4 className="text-sm font-bold text-rose-400 flex items-center gap-2">
              <Trash2 className="w-4 h-4 text-rose-400" /> Clear All Data
            </h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Permanently delete all transactions and categories in this vault.
            </p>
          </div>
          <button
            onClick={() => setWipeConfirmOpen(true)}
            disabled={isWipingData}
            className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-400 font-semibold text-xs flex items-center justify-center gap-1.5 transition cursor-pointer disabled:opacity-50"
          >
            {isWipingData ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Trash2 className="w-4 h-4" />
            )}
            <span>Wipe All Data</span>
          </button>
        </div>

        {wipeSuccessMsg && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400 text-xs flex items-center gap-2">
            <Check className="w-4 h-4" />
            <span>{wipeSuccessMsg}</span>
          </div>
        )}

        {/* Lock & Logout */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-[#1E293B]">
          <div>
            <h4 className="text-sm font-bold text-slate-300">Lock & Log Out</h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Clears session. You will need your vault code to re-enter.
            </p>
          </div>
          <button
            onClick={logoutVault}
            className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold text-xs flex items-center justify-center gap-1.5 transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Log Out</span>
          </button>
        </div>

        {/* Delete Vault Code Permanently */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-rose-500/30">
          <div>
            <h4 className="text-sm font-bold text-rose-500 flex items-center gap-2">
              <Trash2 className="w-4 h-4 text-rose-500" /> Delete Vault Account
            </h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Permanently delete this Vault Code and all data from database.
            </p>
          </div>
          <button
            onClick={() => setDeleteConfirmOpen(true)}
            disabled={isDeletingVault}
            className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/40 text-rose-300 font-semibold text-xs flex items-center justify-center gap-1.5 transition cursor-pointer disabled:opacity-50"
          >
            {isDeletingVault ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Trash2 className="w-4 h-4" />
            )}
            <span>Delete Account</span>
          </button>
        </div>
      </div>

      {/* Confirmation Modals */}
      <ConfirmModal
        isOpen={rotateConfirmOpen}
        title="Rotate Vault Code"
        message="Are you sure you want to generate a NEW Vault Code? All your transactions will be transferred to the new code."
        confirmText="Generate New Code & Rotate"
        onConfirm={handlePerformRotation}
        onClose={() => setRotateConfirmOpen(false)}
      />

      <ConfirmModal
        isOpen={wipeConfirmOpen}
        title="Wipe All Vault Data"
        message="Are you sure you want to permanently delete ALL transactions and categories in this vault? This cannot be undone."
        confirmText="Yes, Delete Everything"
        onConfirm={handlePerformWipe}
        onClose={() => setWipeConfirmOpen(false)}
      />

      <ConfirmModal
        isOpen={deleteConfirmOpen}
        title="Delete Vault Account Permanently"
        message="Are you sure you want to PERMANENTLY DELETE this Vault Code along with ALL transactions and records from the database? This action CANNOT be undone."
        confirmText="Yes, Permanently Delete Account"
        onConfirm={handlePerformDeleteVault}
        onClose={() => setDeleteConfirmOpen(false)}
      />
    </div>
  );
}
