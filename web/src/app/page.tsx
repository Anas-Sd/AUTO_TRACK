"use client";

import React, { useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/authContext";
import Navbar from "@/components/Navbar";
import MobileMenu from "@/components/MobileMenu";
import VaultLoginModal from "@/components/VaultLoginModal";
import OverviewTab from "@/components/OverviewTab";
import LedgerTab from "@/components/LedgerTab";
import CategoriesTab from "@/components/CategoriesTab";
import SettingsTab from "@/components/SettingsTab";
import TransactionModal from "@/components/TransactionModal";
import ManualLogWidget from "@/components/ManualLogWidget";
import { Loader2 } from "lucide-react";

function MainDashboard() {
  const {
    vaultCode,
    token,
    loading,
    activeTab,
    setActiveTab,
    isLogModalOpen,
    setIsLogModalOpen,
    editingTransaction,
    setEditingTransaction,
  } = useAuth();

  const searchParams = useSearchParams();

  // Sync tab from query param if provided (e.g. ?tab=ledger)
  useEffect(() => {
    const tabParam = searchParams.get("tab");
    if (tabParam && ["overview", "ledger", "categories", "settings"].includes(tabParam)) {
      setActiveTab(tabParam as any);
    }
  }, [searchParams, setActiveTab]);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0B0F17] flex flex-col items-center justify-center text-slate-400 gap-3">
        <Loader2 className="w-8 h-8 text-emerald-400 animate-spin" />
        <p className="text-xs font-medium tracking-wide">Syncing Auto Track Vault...</p>
      </div>
    );
  }

  if (!vaultCode || !token) {
    return <VaultLoginModal />;
  }

  return (
    <div className="min-h-screen bg-[#0B0F17] flex flex-col text-slate-100 selection:bg-emerald-500 selection:text-white">
      {/* Desktop Top Navbar */}
      <Navbar />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-4 relative">
        {/* Top 1/3rd Manual Log Widget - Only available in Mobile / Android Application */}
        <div className="md:hidden sticky top-2 z-30 mb-5 shadow-2xl">
          <ManualLogWidget />
        </div>

        {activeTab === "overview" && <OverviewTab />}
        {activeTab === "ledger" && <LedgerTab />}
        {activeTab === "categories" && <CategoriesTab />}
        {activeTab === "settings" && <SettingsTab />}
      </main>

      {/* Mobile Expanding Floating Action Menu */}
      <MobileMenu />

      {/* Transaction Logging / Editing Modal */}
      <TransactionModal
        isOpen={isLogModalOpen || !!editingTransaction}
        initialTransaction={editingTransaction}
        onClose={() => {
          setIsLogModalOpen(false);
          setEditingTransaction(null);
        }}
      />
    </div>
  );
}

export default function Home() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#0B0F17] flex items-center justify-center">
          <Loader2 className="w-8 h-8 text-emerald-400 animate-spin" />
        </div>
      }
    >
      <MainDashboard />
    </Suspense>
  );
}
