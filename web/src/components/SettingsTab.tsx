"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/lib/authContext";
import ConfirmModal from "./ConfirmModal";
import {
  Settings,
  Shield,
  Smartphone,
  Copy,
  Check,
  BellRing,
  Layers,
  BatteryCharging,
  LogOut,
  Save,
  Plus,
  Trash2,
  ExternalLink,
  Info,
  Radio,
  RefreshCw,
  KeyRound,
  Loader2,
  Power,
  ShieldOff,
  Banknote,
  Clock,
} from "lucide-react";

export default function SettingsTab() {
  const { vaultLabel, updateVaultLabel, logoutVault, rotateVaultCode, wipeVaultData, deleteVaultPermanently } = useAuth();
  const [labelInput, setLabelInput] = useState(vaultLabel);
  const [isSavingLabel, setIsSavingLabel] = useState(false);
  const [labelSavedSuccess, setLabelSavedSuccess] = useState(false);

  // Vault Code Rotation states
  const [isRotatingCode, setIsRotatingCode] = useState(false);
  const [rotateConfirmOpen, setRotateConfirmOpen] = useState(false);
  const [rotateSuccessMsg, setRotateSuccessMsg] = useState<string | null>(null);

  // Wipe Data states
  const [isWipingData, setIsWipingData] = useState(false);
  const [wipeConfirmOpen, setWipeConfirmOpen] = useState(false);
  const [wipeSuccessMsg, setWipeSuccessMsg] = useState<string | null>(null);

  // Mobile-Only Delete Vault states
  const [isDeletingVault, setIsDeletingVault] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

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

  // Android Native Integration states
  const [isAndroid, setIsAndroid] = useState(false);
  const [isServiceActive, setIsServiceActive] = useState(true);
  const [cashReminderEnabled, setCashReminderEnabledState] = useState(false);
  const [cashReminderTime, setCashReminderTimeState] = useState("21:00");
  const [nativeVaultCode, setNativeVaultCode] = useState<string | null>(null);
  const [hasCopiedCode, setHasCopiedCode] = useState(false);
  const [notifGranted, setNotifGranted] = useState(false);
  const [overlayGranted, setOverlayGranted] = useState(false);
  const [batteryIgnored, setBatteryIgnored] = useState(false);
  const [allowlist, setAllowlist] = useState<string[]>([
    "com.google.android.apps.nbu.paisa.user", // Google Pay
    "com.phonepe.app",                       // PhonePe
    "net.one97.paytm",                       // Paytm
    "in.org.npci.upiapp",                    // BHIM
    "com.hdfcbank.payzapp",                  // HDFC PayZapp
    "com.csam.icici.bank.imobile",          // ICICI iMobile
    "com.sbi.lotusintouch",                  // SBI Yono
  ]);
  const [newPkgInput, setNewPkgInput] = useState("");

  // Detect Android WebView Bridge
  useEffect(() => {
    setLabelInput(vaultLabel);

    if (typeof window !== "undefined") {
      const savedService = localStorage.getItem("autotrack_service_active");
      if (savedService !== null) {
        setIsServiceActive(savedService === "true");
      }

      const bridge = (window as any).AndroidBridge;
      if (bridge) {
        setIsAndroid(true);
        try {
          if (bridge.getServiceEnabled) {
            setIsServiceActive(bridge.getServiceEnabled());
          }
          if (bridge.getCashReminderEnabled) {
            setCashReminderEnabledState(bridge.getCashReminderEnabled());
          }
          if (bridge.getCashReminderTime) {
            setCashReminderTimeState(bridge.getCashReminderTime());
          }
          if (bridge.getVaultCode) {
            setNativeVaultCode(bridge.getVaultCode());
          }
          if (bridge.getNotificationPermission) {
            setNotifGranted(bridge.getNotificationPermission());
          }
          if (bridge.getOverlayPermission) {
            setOverlayGranted(bridge.getOverlayPermission());
          }
          if (bridge.getBatteryOptimizationStatus) {
            setBatteryIgnored(bridge.getBatteryOptimizationStatus());
          }
          if (bridge.getAllowedPackages) {
            const raw = bridge.getAllowedPackages();
            if (raw) {
              const parsed = JSON.parse(raw);
              if (Array.isArray(parsed) && parsed.length > 0) {
                setAllowlist(parsed);
              }
            }
          }
        } catch (e) {
          console.error("Android bridge error:", e);
        }
      }
    }
  }, [vaultLabel]);

  const handleToggleService = () => {
    const nextState = !isServiceActive;
    setIsServiceActive(nextState);
    localStorage.setItem("autotrack_service_active", String(nextState));
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.setServiceEnabled) {
      try {
        (window as any).AndroidBridge.setServiceEnabled(nextState);
      } catch (e) {
        console.error("Bridge setServiceEnabled error:", e);
      }
    }
  };

  const handleToggleCashReminder = () => {
    const next = !cashReminderEnabled;
    setCashReminderEnabledState(next);
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.setCashReminderEnabled) {
      try {
        (window as any).AndroidBridge.setCashReminderEnabled(next);
      } catch (e) {
        console.error("Bridge setCashReminderEnabled error:", e);
      }
    }
  };

  const handleChangeCashReminderTime = (newTime: string) => {
    setCashReminderTimeState(newTime);
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.setCashReminderTime) {
      try {
        (window as any).AndroidBridge.setCashReminderTime(newTime);
      } catch (e) {
        console.error("Bridge setCashReminderTime error:", e);
      }
    }
  };

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
      setNativeVaultCode(newCode);
      if (typeof window !== "undefined" && (window as any).AndroidBridge?.setVaultCode) {
        (window as any).AndroidBridge.setVaultCode(newCode);
      }
      setHasCopiedCode(true);
      setTimeout(() => setHasCopiedCode(false), 2500);
      setRotateSuccessMsg(`Vault Code successfully updated to ${newCode} & copied to clipboard!`);
      setTimeout(() => setRotateSuccessMsg(null), 5000);
    }
  };

  const copyVaultCode = () => {
    if (!nativeVaultCode) return;
    navigator.clipboard?.writeText(nativeVaultCode);
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.copyToClipboard) {
      (window as any).AndroidBridge.copyToClipboard(nativeVaultCode);
    }
    setHasCopiedCode(true);
    setTimeout(() => setHasCopiedCode(false), 2000);
  };

  const handleRequestNotif = () => {
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.requestNotificationPermission) {
      (window as any).AndroidBridge.requestNotificationPermission();
    }
  };

  const handleRequestOverlay = () => {
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.requestOverlayPermission) {
      (window as any).AndroidBridge.requestOverlayPermission();
    }
  };

  const handleRequestBattery = () => {
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.requestBatteryOptimization) {
      (window as any).AndroidBridge.requestBatteryOptimization();
    }
  };

  const handleAddPackage = () => {
    const pkg = newPkgInput.trim().toLowerCase();
    if (!pkg || allowlist.includes(pkg)) return;
    const updated = [...allowlist, pkg];
    setAllowlist(updated);
    setNewPkgInput("");
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.setAllowedPackages) {
      (window as any).AndroidBridge.setAllowedPackages(JSON.stringify(updated));
    }
  };

  const handleRemovePackage = (pkg: string) => {
    const updated = allowlist.filter((p) => p !== pkg);
    setAllowlist(updated);
    if (typeof window !== "undefined" && (window as any).AndroidBridge?.setAllowedPackages) {
      (window as any).AndroidBridge.setAllowedPackages(JSON.stringify(updated));
    }
  };

  return (
    <div className="space-y-6 max-w-3xl pb-20 md:pb-8">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Settings className="w-5 h-5 text-emerald-400" />
          System Settings
        </h2>
        <p className="text-xs text-slate-400">
          Manage vault configuration and system sync parameters
        </p>
      </div>

      {/* 1. Vault Details & Label */}
      <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
        <div className="flex items-center gap-2.5 pb-3 border-b border-[#1E293B]">
          <Shield className="w-4 h-4 text-emerald-400" />
          <h3 className="text-sm font-semibold text-white">Vault Profile</h3>
        </div>

        <form onSubmit={handleSaveLabel} className="space-y-3">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Vault Display Label
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
              This label helps you identify this vault on desktop and mobile.
            </p>
          </div>
        </form>

        {/* Security Notice */}
        <div className="p-3.5 rounded-xl bg-[#0B0F17] border border-[#1E293B] text-xs text-slate-400 flex items-start gap-2.5">
          <Info className="w-4 h-4 text-sky-400 shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-slate-300">Strict Vault Security</p>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Vault codes are permanently locked inside the Android app and never visible on the web dashboard to prevent accidental session leaks.
            </p>
          </div>
        </div>
      </div>

      {/* 2. Android App Native Controls (Shows when running inside Android App) */}
      {isAndroid && (
        <div className="bg-[#131A26] border border-emerald-500/30 rounded-2xl p-5 md:p-6 space-y-5 shadow-sm">
          <div className="flex items-center justify-between pb-3 border-b border-[#1E293B]">
            <div className="flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-white">
                Android Device Integration
              </h3>
            </div>
            <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              Native Active
            </span>
          </div>

          {/* Master Pause / Temporary Disable Switch */}
          <div className="p-4 rounded-xl bg-[#0B0F17] border border-[#1E293B] flex items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className={`p-2.5 rounded-xl border ${isServiceActive ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400" : "bg-rose-500/10 border-rose-500/30 text-rose-400"}`}>
                {isServiceActive ? <Power className="w-5 h-5" /> : <ShieldOff className="w-5 h-5" />}
              </div>
              <div>
                <h4 className="text-xs font-bold text-white flex items-center gap-2">
                  Auto-Tracking & Popup Service
                  <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase border ${isServiceActive ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30" : "bg-rose-500/20 text-rose-400 border-rose-500/30"}`}>
                    {isServiceActive ? "Active" : "Paused"}
                  </span>
                </h4>
                <p className="text-[10.5px] text-slate-400 mt-0.5">
                  {isServiceActive
                    ? "App is actively listening for payment alerts & displaying transaction popups."
                    : "App is temporarily PAUSED. No background auto-tracking or popups will occur."}
                </p>
              </div>
            </div>

            {/* Toggle Switch Button */}
            <button
              type="button"
              onClick={handleToggleService}
              className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer shrink-0 ${isServiceActive ? "bg-emerald-500 justify-end" : "bg-[#1E293B] border border-[#334155] justify-start"}`}
            >
              <span className="w-4 h-4 rounded-full bg-white shadow-md transition-transform" />
            </button>
          </div>

          {/* Daily Cash Expense Reminder */}
          <div className="p-4 rounded-xl bg-[#0B0F17] border border-[#1E293B] space-y-3">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-start gap-3">
                <div className={`p-2.5 rounded-xl border ${cashReminderEnabled ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400" : "bg-[#1E293B] border-[#334155] text-slate-400"}`}>
                  <Banknote className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white flex items-center gap-2">
                    Daily Cash Expense Reminder
                    <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase border ${cashReminderEnabled ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30" : "bg-slate-500/20 text-slate-400 border-slate-500/30"}`}>
                      {cashReminderEnabled ? "Scheduled" : "Off"}
                    </span>
                  </h4>
                  <p className="text-[10.5px] text-slate-400 mt-0.5">
                    Receives a daily notification at your chosen time asking if you spent any cash today.
                  </p>
                </div>
              </div>

              {/* Toggle Switch Button */}
              <button
                type="button"
                onClick={handleToggleCashReminder}
                className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer shrink-0 ${cashReminderEnabled ? "bg-emerald-500 justify-end" : "bg-[#1E293B] border border-[#334155] justify-start"}`}
              >
                <span className="w-4 h-4 rounded-full bg-white shadow-md transition-transform" />
              </button>
            </div>

            {cashReminderEnabled && (
              <div className="flex items-center justify-between pt-2 border-t border-[#1E293B] text-xs">
                <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                  <Clock className="w-3.5 h-3.5 text-emerald-400" /> Daily Reminder Time:
                </span>
                <input
                  type="time"
                  value={cashReminderTime}
                  onChange={(e) => handleChangeCashReminderTime(e.target.value)}
                  className="bg-[#131A26] border border-[#1E293B] rounded-xl px-3 py-1.5 text-white font-mono text-xs focus:outline-none focus:border-emerald-500"
                />
              </div>
            )}
          </div>

          {/* Vault Code (Native Only) */}
          {nativeVaultCode && (
            <div className="p-4 rounded-xl bg-[#0B0F17] border border-[#1E293B] space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
                  Vault Code (On-Device Secret)
                </span>
                <span className="text-[10px] text-slate-500 font-mono">
                  Keep secret
                </span>
              </div>
              <div className="flex items-center justify-between gap-3">
                <span className="font-mono text-lg font-bold text-emerald-400 tracking-widest">
                  {nativeVaultCode}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={copyVaultCode}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-white text-xs font-medium transition cursor-pointer"
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
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-emerald-400 text-xs font-semibold transition cursor-pointer"
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
            </div>
          )}

          {/* Special Permissions Status */}
          <div className="space-y-2.5">
            <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
              Background Permissions
            </h4>

            {/* Notification Listener */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-[#0B0F17] border border-[#1E293B]">
              <div className="flex items-center gap-2.5">
                <BellRing className="w-4 h-4 text-emerald-400" />
                <div>
                  <p className="text-xs font-medium text-white">
                    Notification Listener Access
                  </p>
                  <p className="text-[10px] text-slate-500">
                    Reads UPI payment alerts from allowlisted apps
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    notifGranted
                      ? "bg-emerald-500/20 text-emerald-400"
                      : "bg-rose-500/20 text-rose-400"
                  }`}
                >
                  {notifGranted ? "Granted" : "Not Granted"}
                </span>
                <button
                  onClick={handleRequestNotif}
                  className="px-2.5 py-1 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-xs text-white"
                >
                  {notifGranted ? "Settings" : "Grant"}
                </button>
              </div>
            </div>

            {/* Draw Over Other Apps */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-[#0B0F17] border border-[#1E293B]">
              <div className="flex items-center gap-2.5">
                <Layers className="w-4 h-4 text-emerald-400" />
                <div>
                  <p className="text-xs font-medium text-white">
                    Draw Over Other Apps (Overlay)
                  </p>
                  <p className="text-[10px] text-slate-500">
                    Pops instant confirm card upon transaction
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    overlayGranted
                      ? "bg-emerald-500/20 text-emerald-400"
                      : "bg-rose-500/20 text-rose-400"
                  }`}
                >
                  {overlayGranted ? "Granted" : "Not Granted"}
                </span>
                <button
                  onClick={handleRequestOverlay}
                  className="px-2.5 py-1 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-xs text-white"
                >
                  {overlayGranted ? "Settings" : "Grant"}
                </button>
              </div>
            </div>

            {/* Battery Optimization Guidance */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-[#0B0F17] border border-[#1E293B]">
              <div className="flex items-center gap-2.5">
                <BatteryCharging className="w-4 h-4 text-emerald-400" />
                <div>
                  <p className="text-xs font-medium text-white">
                    Battery Optimization Exemption
                  </p>
                  <p className="text-[10px] text-slate-500">
                    For reliable detection on Xiaomi / Oppo / Vivo OEM managers
                  </p>
                </div>
              </div>
              <button
                onClick={handleRequestBattery}
                className="px-2.5 py-1 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-xs text-white"
              >
                Disable Limits
              </button>
            </div>
          </div>

          {/* Source App Allowlist */}
          <div className="space-y-2">
            <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
              Source App Package Allowlist
            </h4>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={newPkgInput}
                onChange={(e) => setNewPkgInput(e.target.value)}
                placeholder="e.g. com.axisbank.axispay"
                className="flex-1 bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3 py-2 text-white text-xs font-mono focus:outline-none focus:border-emerald-500"
              />
              <button
                type="button"
                onClick={handleAddPackage}
                className="px-3 py-2 bg-[#1E293B] hover:bg-[#334155] text-white rounded-xl text-xs font-medium flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add
              </button>
            </div>

            <div className="flex flex-wrap gap-1.5 pt-1">
              {allowlist.map((pkg) => (
                <span
                  key={pkg}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#0B0F17] border border-[#1E293B] text-[11px] font-mono text-slate-300"
                >
                  <span>{pkg}</span>
                  <button
                    type="button"
                    onClick={() => handleRemovePackage(pkg)}
                    className="text-slate-500 hover:text-rose-400 transition"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 3. System Info & Architecture */}
      <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-5 md:p-6 space-y-3 shadow-sm text-xs">
        <h3 className="text-sm font-semibold text-white">System Diagnostics</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
          <div className="p-3 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
            <span className="text-[10px] text-slate-500 uppercase tracking-wider">
              Backend Architecture
            </span>
            <p className="font-semibold text-slate-200 mt-0.5">Supabase PostgreSQL</p>
            <p className="text-[10px] text-emerald-400 flex items-center gap-1 mt-0.5">
              <Radio className="w-2.5 h-2.5 animate-pulse" /> Realtime Channel Subscribed
            </p>
          </div>
          <div className="p-3 bg-[#0B0F17] rounded-xl border border-[#1E293B]">
            <span className="text-[10px] text-slate-500 uppercase tracking-wider">
              Security Model
            </span>
            <p className="font-semibold text-slate-200 mt-0.5">RLS Scoped JWT (30-day)</p>
            <p className="text-[10px] text-slate-400 mt-0.5">HttpOnly Secure Cookie Guard</p>
          </div>
        </div>
      </div>

      {/* 4. Danger Zone: Wipe Data & Logout */}
      <div className="bg-[#131A26] border border-rose-500/20 rounded-2xl p-5 md:p-6 space-y-4 shadow-sm">
        <div className="flex items-center justify-between pb-3 border-b border-rose-500/20">
          <div>
            <h4 className="text-sm font-bold text-rose-400 flex items-center gap-2">
              <Trash2 className="w-4 h-4 text-rose-400" /> Clear All Vault Data
            </h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Permanently delete all transactions and all categories in this vault.
            </p>
          </div>
          <button
            onClick={() => setWipeConfirmOpen(true)}
            disabled={isWipingData}
            className="px-4 py-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-400 font-semibold text-xs flex items-center gap-1.5 transition cursor-pointer disabled:opacity-50"
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

        <div className="flex items-center justify-between pt-1 border-t border-[#1E293B]">
          <div>
            <h4 className="text-sm font-bold text-slate-300">Lock & Log Out</h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Clears browser cookie session. You will need your vault code to re-enter.
            </p>
          </div>
          <button
            onClick={logoutVault}
            className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold text-xs flex items-center gap-1.5 transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Lock Vault</span>
          </button>
        </div>

        {/* Mobile App Only: Delete Vault Code & All Data Permanently */}
        {isAndroid && (
          <div className="flex items-center justify-between pt-3 border-t border-rose-500/30">
            <div>
              <h4 className="text-sm font-bold text-rose-500 flex items-center gap-2">
                <Trash2 className="w-4 h-4 text-rose-500" /> Delete Vault Code Permanently
              </h4>
              <p className="text-xs text-slate-400 mt-0.5">
                Permanently delete this Vault Code, transactions, and categories from database and reset app.
              </p>
            </div>
            <button
              onClick={() => setDeleteConfirmOpen(true)}
              disabled={isDeletingVault}
              className="px-4 py-2.5 rounded-xl bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/40 text-rose-300 font-semibold text-xs flex items-center gap-1.5 transition cursor-pointer disabled:opacity-50"
            >
              {isDeletingVault ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Trash2 className="w-4 h-4" />
              )}
              <span>Delete Vault Code</span>
            </button>
          </div>
        )}
      </div>

      {/* Vault Code Rotation Confirmation Modal */}
      <ConfirmModal
        isOpen={rotateConfirmOpen}
        title="Rotate Vault Code"
        message="Are you sure you want to generate a NEW Vault Code? All your categories and transactions will be transferred atomically to the new code, and your old Vault Code will be permanently deleted. Anyone currently logged into your vault on the web using the old code will be logged out immediately."
        confirmText="Generate New Code & Rotate"
        onConfirm={handlePerformRotation}
        onClose={() => setRotateConfirmOpen(false)}
      />

      {/* Wipe Data Confirmation Modal */}
      <ConfirmModal
        isOpen={wipeConfirmOpen}
        title="Wipe All Vault Data"
        message="Are you sure you want to permanently delete ALL transactions and ALL categories in this vault? This action cannot be undone."
        confirmText="Yes, Delete Everything"
        onConfirm={handlePerformWipe}
        onClose={() => setWipeConfirmOpen(false)}
      />

      {/* Mobile-Only Delete Vault Code Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        title="Delete Vault Code Permanently"
        message="Are you sure you want to PERMANENTLY DELETE this Vault Code along with ALL transactions, categories, and ledger records from the database? This action CANNOT be undone, and the mobile app will be reset to Onboarding."
        confirmText="Yes, Permanently Delete Vault"
        onConfirm={handlePerformDeleteVault}
        onClose={() => setDeleteConfirmOpen(false)}
      />
    </div>
  );
}
