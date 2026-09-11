"use client";

import React, { useState } from "react";
import { useAuth } from "@/lib/authContext";
import { Shield, KeyRound, ArrowRight, PlusCircle, AlertCircle, Loader2 } from "lucide-react";

export default function VaultLoginModal() {
  const { loginVault, createVault } = useAuth();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [createdCode, setCreatedCode] = useState<string | null>(null);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [lockoutUntil, setLockoutUntil] = useState<number | null>(null);

  const isLockedOut = lockoutUntil !== null && Date.now() < lockoutUntil;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isLockedOut) return;

    const clean = code.trim().toUpperCase();
    if (clean.length < 6) {
      setError("Please enter a valid vault code (e.g. 8 characters)");
      return;
    }

    setLoading(true);
    setError(null);

    const res = await loginVault(clean);
    setLoading(false);

    if (!res.success) {
      const nextFail = failedAttempts + 1;
      setFailedAttempts(nextFail);
      if (nextFail >= 4) {
        setLockoutUntil(Date.now() + 30000); // 30 seconds soft lockout
        setError("Too many invalid attempts. Please wait 30 seconds.");
      } else {
        setError(res.error || "Invalid vault code. Check spelling and try again.");
      }
    }
  };

  const handleCreate = async () => {
    setLoading(true);
    setError(null);
    const newCode = await createVault();
    setLoading(false);
    if (newCode) {
      setCreatedCode(newCode);
    } else {
      setError("Failed to create vault. Check connection.");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B0F17]/90 backdrop-blur-md p-4">
      <div className="w-full max-w-md bg-[#131A26] border border-[#1E293B] rounded-2xl p-6 md:p-8 shadow-2xl relative overflow-hidden">
        {/* Glow effect */}
        <div className="absolute -top-24 -left-24 w-48 h-48 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-48 h-48 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
              Auto Track <span className="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-medium border border-emerald-500/30">Vault</span>
            </h1>
            <p className="text-xs text-slate-400">Zero-cost realtime expense tracker</p>
          </div>
        </div>

        {createdCode ? (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm">
              <p className="font-semibold text-emerald-400 mb-1">🎉 New Vault Created!</p>
              <p className="text-xs text-slate-300">
                Your vault code is shown below. This code will <strong>never be shown again on the web</strong>. Keep it secure to log in from other devices:
              </p>
              <div className="my-3 p-3 bg-[#0B0F17] rounded-lg border border-emerald-500/40 text-center font-mono text-xl tracking-widest text-emerald-400 select-all font-bold">
                {createdCode}
              </div>
            </div>
            <button
              onClick={() => setCreatedCode(null)}
              className="w-full py-3 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-medium text-sm transition shadow-lg shadow-emerald-500/25"
            >
              Enter Dashboard
            </button>
          </div>
        ) : (
          <div>
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Enter Vault Code
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <KeyRound className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    maxLength={12}
                    value={code}
                    disabled={isLockedOut || loading}
                    onChange={(e) => setCode(e.target.value.toUpperCase())}
                    placeholder="e.g. 8F3K9M2A"
                    className="w-full pl-10 pr-4 py-3 bg-[#0B0F17] border border-[#1E293B] rounded-xl text-white font-mono tracking-wider focus:outline-none focus:border-emerald-500 transition text-sm disabled:opacity-50"
                  />
                </div>
              </div>

              {error && (
                <div className="flex items-center gap-2 text-xs text-rose-400 bg-rose-500/10 border border-rose-500/20 p-3 rounded-lg">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={loading || isLockedOut || !code.trim()}
                className="w-full py-3 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium text-sm flex items-center justify-center gap-2 transition shadow-lg shadow-emerald-500/20 cursor-pointer"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" /> Unlocking...
                  </>
                ) : (
                  <>
                    Unlock Vault <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
