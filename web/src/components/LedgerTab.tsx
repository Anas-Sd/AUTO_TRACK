"use client";

import React, { useState, useMemo } from "react";
import { useAuth } from "@/lib/authContext";
import { Transaction } from "@/lib/types";
import ConfirmModal from "./ConfirmModal";
import CustomSelect from "./CustomSelect";
import {
  Search,
  Filter,
  Download,
  Trash2,
  Edit2,
  Calendar,
  Layers,
  Smartphone,
  ChevronDown,
  ChevronUp,
  Receipt,
  ArrowUpDown,
  Building,
  RotateCcw,
} from "lucide-react";

type LedgerTimeframe = "all" | "today" | "month" | "bymonth" | "custom";
type SortOption = "newest" | "oldest" | "highest" | "lowest";

export default function LedgerTab() {
  const {
    transactions,
    categories,
    deleteTransaction,
    setEditingTransaction,
    setIsLogModalOpen,
    ledgerCategoryFilter,
    setLedgerCategoryFilter,
  } = useAuth();

  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  // Search & Filter State
  const [searchQuery, setSearchQuery] = useState("");
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const selectedCategory = ledgerCategoryFilter;
  const setSelectedCategory = setLedgerCategoryFilter;
  const [selectedType, setSelectedType] = useState<"all" | "income" | "expense">("all");
  const [selectedSource, setSelectedSource] = useState("all");
  const [selectedReceiver, setSelectedReceiver] = useState("all");
  const [selectedSender, setSelectedSender] = useState("all");
  const [timeframe, setTimeframe] = useState<LedgerTimeframe>("all");
  const [byMonthValue, setByMonthValue] = useState<string>(
    new Date().toISOString().slice(0, 7) // e.g. "2026-09"
  );
  const [customStart, setCustomStart] = useState("");
  const [customEnd, setCustomEnd] = useState("");
  const [sortBy, setSortBy] = useState<SortOption>("newest");

  // Distinct sources list
  const availableSources = useMemo(() => {
    const s = new Set<string>();
    transactions.forEach((t) => {
      if (t.source_app) s.add(t.source_app);
    });
    return Array.from(s);
  }, [transactions]);

  // Distinct Receivers list (Outgoing / Expense payments)
  const availableReceivers = useMemo(() => {
    const set = new Set<string>();
    transactions.forEach((t) => {
      if (t.type === "expense" && t.receiver_vendor && t.receiver_vendor.trim()) {
        set.add(t.receiver_vendor.trim());
      }
    });
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [transactions]);

  // Distinct Senders list (Incoming / Income payments)
  const availableSenders = useMemo(() => {
    const set = new Set<string>();
    transactions.forEach((t) => {
      if (t.type === "income" && t.receiver_vendor && t.receiver_vendor.trim()) {
        set.add(t.receiver_vendor.trim());
      }
    });
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [transactions]);

  // Filtered & Sorted Transactions
  const filteredTransactions = useMemo(() => {
    const now = new Date();
    const query = searchQuery.trim().toLowerCase();

    return transactions
      .filter((t) => {
        // Search query match
        if (query) {
          const matchNote = (t.note || "").toLowerCase().includes(query);
          const matchVendor = (t.receiver_vendor || "").toLowerCase().includes(query);
          if (!matchNote && !matchVendor) return false;
        }

        // Category filter
        if (selectedCategory !== "all") {
          if (selectedCategory === "uncategorized") {
            if (t.category_id) return false;
          } else if (t.category_id !== selectedCategory) {
            return false;
          }
        }

        // Type filter
        if (selectedType !== "all" && t.type !== selectedType) {
          return false;
        }

        // Source filter
        if (selectedSource !== "all" && t.source_app !== selectedSource) {
          return false;
        }

        // Receiver (Outgoing / Expense) filter
        if (selectedReceiver !== "all") {
          if (
            t.type !== "expense" ||
            (t.receiver_vendor || "").trim().toLowerCase() !== selectedReceiver.trim().toLowerCase()
          ) {
            return false;
          }
        }

        // Sender (Incoming / Income) filter
        if (selectedSender !== "all") {
          if (
            t.type !== "income" ||
            (t.receiver_vendor || "").trim().toLowerCase() !== selectedSender.trim().toLowerCase()
          ) {
            return false;
          }
        }

        // Timeframe filter
        const d = new Date(t.occurred_at);
        if (timeframe === "today") {
          if (
            d.getFullYear() !== now.getFullYear() ||
            d.getMonth() !== now.getMonth() ||
            d.getDate() !== now.getDate()
          ) {
            return false;
          }
        } else if (timeframe === "month") {
          if (
            d.getFullYear() !== now.getFullYear() ||
            d.getMonth() !== now.getMonth()
          ) {
            return false;
          }
        } else if (timeframe === "bymonth") {
          if (!byMonthValue) return true;
          const ym = t.occurred_at.slice(0, 7);
          if (ym !== byMonthValue) return false;
        } else if (timeframe === "custom") {
          const tTime = d.getTime();
          const start = customStart ? new Date(customStart).getTime() : 0;
          const end = customEnd ? new Date(customEnd + "T23:59:59").getTime() : Infinity;
          if (tTime < start || tTime > end) return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === "newest") {
          return new Date(b.occurred_at).getTime() - new Date(a.occurred_at).getTime();
        } else if (sortBy === "oldest") {
          return new Date(a.occurred_at).getTime() - new Date(b.occurred_at).getTime();
        } else if (sortBy === "highest") {
          return Number(b.amount) - Number(a.amount);
        } else if (sortBy === "lowest") {
          return Number(a.amount) - Number(b.amount);
        }
        return 0;
      });
  }, [
    transactions,
    searchQuery,
    selectedCategory,
    selectedType,
    selectedSource,
    selectedReceiver,
    selectedSender,
    timeframe,
    byMonthValue,
    customStart,
    customEnd,
    sortBy,
  ]);

  // Running totals for currently filtered result set
  const { filteredIncome, filteredExpense } = useMemo(() => {
    let inc = 0;
    let exp = 0;
    filteredTransactions.forEach((t) => {
      const amt = Number(t.amount) || 0;
      if (t.type === "income") inc += amt;
      else exp += amt;
    });
    return { filteredIncome: inc, filteredExpense: exp };
  }, [filteredTransactions]);

  // CSV Export Handler
  const handleExportCSV = () => {
    const headers = [
      "ID",
      "Date",
      "Type",
      "Amount (INR)",
      "Vendor / Receiver",
      "Category",
      "Source App",
      "Note",
    ];

    const categoryMap = new Map(categories.map((c) => [c.id, c.name]));

    const rows = filteredTransactions.map((t) => [
      `"${t.id}"`,
      `"${new Date(t.occurred_at).toLocaleString("en-IN")}"`,
      `"${t.type.toUpperCase()}"`,
      t.amount,
      `"${(t.receiver_vendor || "").replace(/"/g, '""')}"`,
      `"${(t.category_id ? categoryMap.get(t.category_id) || "Uncategorized" : "Uncategorized").replace(/"/g, '""')}"`,
      `"${(t.source_app || "").replace(/"/g, '""')}"`,
      `"${(t.note || "").replace(/"/g, '""')}"`,
    ]);

    const csvContent =
      "data:text/csv;charset=utf-8," +
      [headers.join(","), ...rows.map((e) => e.join(","))].join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute(
      "download",
      `auto_track_ledger_${new Date().toISOString().slice(0, 10)}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleEdit = (txn: Transaction) => {
    setEditingTransaction(txn);
    setIsLogModalOpen(true);
  };

  const handleDelete = (id: string) => {
    setDeleteConfirmId(id);
  };

  // Helper mapping for category
  const catLookup = useMemo(() => {
    const map = new Map();
    categories.forEach((c) => map.set(c.id, c));
    return map;
  }, [categories]);

  return (
    <div className="h-[calc(100vh-6.5rem)] md:h-[calc(100vh-7.5rem)] flex flex-col space-y-3 pb-2 overflow-hidden">
      {/* Search Bar & Primary Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 shrink-0">
        {/* Search input */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by vendor, note..."
            className="w-full pl-10 pr-4 py-2 bg-[#131A26] border border-[#1E293B] rounded-xl text-white text-xs placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
          />
        </div>

        {/* Buttons: Filter toggle, Sort, Export CSV */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsFilterOpen(!isFilterOpen)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl border text-xs font-medium transition cursor-pointer ${isFilterOpen ||
              selectedCategory !== "all" ||
              selectedType !== "all" ||
              selectedSource !== "all" ||
              selectedReceiver !== "all" ||
              selectedSender !== "all" ||
              timeframe !== "all"
              ? "bg-[#1E293B] border-emerald-500/50 text-emerald-400"
              : "bg-[#131A26] border-[#1E293B] text-slate-300 hover:text-white"
              }`}
          >
            <Filter className="w-3.5 h-3.5" />
            <span>Filters</span>
            {selectedCategory !== "all" ||
              selectedType !== "all" ||
              selectedSource !== "all" ||
              selectedReceiver !== "all" ||
              selectedSender !== "all" ||
              timeframe !== "all" ? (
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            ) : null}
            {isFilterOpen ? (
              <ChevronUp className="w-3.5 h-3.5" />
            ) : (
              <ChevronDown className="w-3.5 h-3.5" />
            )}
          </button>

          {/* Custom Sort dropdown */}
          <CustomSelect
            value={sortBy}
            onChange={(val) => setSortBy(val as SortOption)}
            options={[
              { value: "newest", label: "Sort: Newest First" },
              { value: "oldest", label: "Sort: Oldest First" },
              { value: "highest", label: "Sort: Amount High-Low" },
              { value: "lowest", label: "Sort: Amount Low-High" },
            ]}
          />

          {/* CSV Export Button */}
          <button
            onClick={handleExportCSV}
            title="Export filtered transactions as CSV"
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-[#131A26] border border-[#1E293B] hover:bg-[#1E293B] text-slate-300 hover:text-white text-xs font-medium transition cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span className="hidden sm:inline">Export</span>
          </button>
        </div>
      </div>

      {/* Collapsible Filter Panel */}
      {isFilterOpen && (
        <div className="bg-[#131A26] border border-[#1E293B] rounded-2xl p-3.5 space-y-3 shrink-0 animate-in fade-in slide-in-from-top-2 duration-150 relative z-40 overflow-visible">
          <div className="flex items-center justify-between pb-1.5 border-b border-[#1E293B]">
            <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Filter className="w-3.5 h-3.5 text-emerald-400" /> Filter Criteria
            </span>
            {(selectedCategory !== "all" ||
              selectedType !== "all" ||
              selectedSource !== "all" ||
              timeframe !== "all") && (
                <button
                  onClick={() => {
                    setSelectedCategory("all");
                    setSelectedType("all");
                    setSelectedSource("all");
                    setTimeframe("all");
                    setIsFilterOpen(false);
                  }}
                  className="text-[11px] text-emerald-400 hover:text-emerald-300 flex items-center gap-1 cursor-pointer font-medium"
                >
                  <RotateCcw className="w-3 h-3" /> Reset Filters
                </button>
              )}
          </div>

          {/* Timeframe Pills (AT THE TOP - Horizontally Scrollable on Mobile) */}
          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1.5 flex items-center gap-1">
              <Calendar className="w-3 h-3 text-emerald-400" /> Timeframe
            </label>
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full no-scrollbar">
              {[
                { id: "all", label: "All Time" },
                { id: "today", label: "Today" },
                { id: "month", label: "This Month" },
                { id: "bymonth", label: "By Month" },
                { id: "custom", label: "Custom Range" },
              ].map((pill) => (
                <button
                  key={pill.id}
                  onClick={() => setTimeframe(pill.id as LedgerTimeframe)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-medium whitespace-nowrap transition cursor-pointer shrink-0 ${timeframe === pill.id
                    ? "bg-emerald-500 text-white shadow-xs"
                    : "bg-[#0B0F17] text-slate-400 hover:text-white border border-[#1E293B]"
                    }`}
                >
                  {pill.label}
                </button>
              ))}
            </div>

            {timeframe === "bymonth" && (
              <div className="mt-2">
                <input
                  type="month"
                  value={byMonthValue}
                  onChange={(e) => setByMonthValue(e.target.value)}
                  className="bg-[#0B0F17] border border-[#1E293B] rounded-lg px-2.5 py-1 text-white text-xs focus:outline-none focus:border-emerald-500 w-full sm:w-auto"
                />
              </div>
            )}

            {timeframe === "custom" && (
              <div className="flex items-center gap-2 mt-2">
                <input
                  type="date"
                  value={customStart}
                  onChange={(e) => setCustomStart(e.target.value)}
                  className="bg-[#0B0F17] border border-[#1E293B] rounded-lg px-2.5 py-1 text-white text-xs focus:outline-none focus:border-emerald-500 flex-1 min-w-0"
                />
                <span className="text-slate-500 text-xs shrink-0">to</span>
                <input
                  type="date"
                  value={customEnd}
                  onChange={(e) => setCustomEnd(e.target.value)}
                  className="bg-[#0B0F17] border border-[#1E293B] rounded-lg px-2.5 py-1 text-white text-xs focus:outline-none focus:border-emerald-500 flex-1 min-w-0"
                />
              </div>
            )}
          </div>

          {/* 3 Dropdowns Grid (Category, Type, Source) */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Category Dropdown */}
            <div>
              <label className="block text-[10px] sm:text-[11px] font-medium text-slate-400 mb-1 flex items-center gap-1">
                <Layers className="w-3 h-3 text-emerald-400" /> Category
              </label>
              <CustomSelect
                value={selectedCategory}
                onChange={(val) => setSelectedCategory(val)}
                className="w-full"
                options={[
                  { value: "all", label: "All Categories" },
                  ...categories.map((c) => ({
                    value: c.id,
                    label: c.name,
                    icon: c.icon || "🏷️",
                  })),
                  { value: "uncategorized", label: "Uncategorized", icon: "📦" },
                ]}
              />
            </div>

            {/* Type Dropdown */}
            <div>
              <label className="block text-[10px] sm:text-[11px] font-medium text-slate-400 mb-1 flex items-center gap-1">
                <Receipt className="w-3 h-3 text-emerald-400" /> Type
              </label>
              <CustomSelect
                value={selectedType}
                onChange={(val) => setSelectedType(val as any)}
                className="w-full"
                options={[
                  { value: "all", label: "All Types" },
                  { value: "income", label: "Income (+)" },
                  { value: "expense", label: "Expense (-)" },
                ]}
              />
            </div>

            {/* Source App Dropdown */}
            <div>
              <label className="block text-[10px] sm:text-[11px] font-medium text-slate-400 mb-1 flex items-center gap-1">
                <Smartphone className="w-3 h-3 text-emerald-400" /> Source
              </label>
              <CustomSelect
                value={selectedSource}
                onChange={(val) => setSelectedSource(val)}
                className="w-full"
                options={[
                  { value: "all", label: "All Sources" },
                  ...availableSources.map((s) => ({ value: s, label: s })),
                ]}
              />
            </div>
          </div>
        </div>
      )}

      {/* Stats Header Bar: Item count pill + Running totals */}
      <div className="flex flex-wrap items-center justify-between gap-3 p-2.5 bg-[#131A26]/80 rounded-xl border border-[#1E293B] shrink-0">
        <div className="flex items-center gap-2">
          <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#1E293B] text-slate-300">
            {filteredTransactions.length} {filteredTransactions.length === 1 ? "Item" : "Items"}
          </span>
          {searchQuery && (
            <span className="text-xs text-slate-400">matching &quot;{searchQuery}&quot;</span>
          )}
        </div>

        <div className="flex items-center gap-3 font-mono text-xs">
          <span className="text-emerald-400 font-semibold">
            +₹{filteredIncome.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </span>
          <span className="text-slate-600">|</span>
          <span className="text-rose-400 font-semibold">
            -₹{filteredExpense.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </span>
        </div>
      </div>

      {/* Transactions Table / List View - Fixed Box with Internal Scroll */}
      <div className="flex-1 min-h-0 overflow-hidden flex flex-col bg-[#131A26] border border-[#1E293B] rounded-2xl shadow-sm">
        {filteredTransactions.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center p-12 text-center">
            <Receipt className="w-10 h-10 mx-auto mb-3 opacity-30 text-slate-400" />
            <h3 className="text-sm font-semibold text-slate-300">No transactions found</h3>
            <p className="text-xs text-slate-500 mt-1">
              Try adjusting your search terms or filters above.
            </p>
          </div>
        ) : (
          <div className="overflow-y-auto flex-1 h-full min-h-0 custom-scrollbar">
            {/* Mobile View: 3 Columns, NO horizontal scrolling */}
            <div className="sm:hidden">
              <div className="grid grid-cols-[1fr_1.4fr_auto] gap-2 py-2 px-3 bg-[#0F172A]/70 border-b border-[#1E293B] text-[9px] font-semibold text-slate-400 uppercase tracking-wider sticky top-0 z-10 backdrop-blur-md">
                <div>TO / FROM</div>
                <div>CATEGORY & DATE</div>
                <div className="text-right">AMOUNT</div>
              </div>
              <div className="divide-y divide-[#1E293B] text-xs">
                {filteredTransactions.map((t) => {
                  const cat = t.category_id ? catLookup.get(t.category_id) : null;
                  const isExpense = t.type === "expense";
                  const dateStr = new Date(t.occurred_at).toLocaleDateString("en-IN", {
                    month: "short",
                    day: "numeric",
                    year: "numeric",
                  });
                  const timeStr = new Date(t.occurred_at).toLocaleTimeString("en-IN", {
                    hour: "2-digit",
                    minute: "2-digit",
                  });

                  return (
                    <div
                      key={`mob-${t.id}`}
                      className="grid grid-cols-[1fr_1.4fr_auto] gap-2 items-center py-2.5 px-3 hover:bg-[#1E293B]/40 transition"
                    >
                      {/* Col 1: TO (Receiver/Vendor), Note, Via (Source App) */}
                      <div className="min-w-0 pr-0.5">
                        <div className="font-semibold text-white text-[11px] leading-tight truncate">
                          {t.receiver_vendor || (t.note ? t.note : "Transaction")}
                        </div>
                        {t.receiver_vendor && t.note && (
                          <p className="text-[9.5px] text-slate-400 truncate mt-0.5 leading-tight">
                            {t.note}
                          </p>
                        )}
                        {t.source_app && (
                          <div className="mt-0.5">
                            <span className="inline-block text-[8.5px] px-1.5 py-[1px] rounded bg-[#0B0F17] text-slate-400 border border-[#1E293B] leading-none -ml-1">
                              {t.source_app}
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Col 2: Category & Date */}
                      <div className="min-w-0 flex flex-col items-start gap-1">
                        {cat ? (
                          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[9px] font-medium bg-[#0B0F17] border border-[#1E293B] max-w-full">
                            <span
                              className="w-1.5 h-1.5 rounded-full shrink-0"
                              style={{ backgroundColor: cat.color || "#10B981" }}
                            />
                            <span className="text-slate-200 truncate">
                              {cat.icon} {cat.name}
                            </span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[9px] font-medium bg-[#0B0F17] border border-[#1E293B] text-slate-400">
                            📦 Uncategorized
                          </span>
                        )}
                        <div className="text-[9.5px] text-slate-400 ml-5 text-center">
                          <div>{dateStr}</div>
                          <div className="text-[8.5px] text-slate-500">{timeStr}</div>
                        </div>
                      </div>

                      {/* Col 3: Amount & Edit/Delete actions */}
                      <div className="flex flex-col items-end justify-center shrink-0 min-w-0">
                        <div className="font-mono font-bold text-[11px] text-right whitespace-nowrap">
                          <span className={isExpense ? "text-rose-400" : "text-emerald-400"}>
                            {isExpense ? "-" : "+"}₹
                            {Number(t.amount).toLocaleString("en-IN", {
                              minimumFractionDigits: 2,
                            })}
                          </span>
                        </div>
                        <div className="flex items-center justify-end gap-1 mt-1">
                          <button
                            onClick={() => handleEdit(t)}
                            title="Edit transaction"
                            className="p-1 rounded-md text-slate-400 hover:text-white bg-[#0B0F17]/60 border border-[#1E293B]/60 transition cursor-pointer"
                          >
                            <Edit2 className="w-3 h-3" />
                          </button>
                          <button
                            onClick={() => handleDelete(t.id)}
                            title="Delete transaction"
                            className="p-1 rounded-md text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 bg-[#0B0F17]/60 border border-[#1E293B]/60 transition cursor-pointer"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Desktop / Tablet View: Full Table */}
            <table className="hidden sm:table w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#1E293B] bg-[#0F172A]/50 text-[11px] font-semibold text-slate-400 uppercase tracking-wider sticky top-0 z-10 backdrop-blur-md">
                  <th className="py-3 px-4">TO / FROM</th>
                  <th className="py-3 px-3">Category</th>
                  <th className="py-3 px-3">Source</th>
                  <th className="py-3 px-3">Date</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-3 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E293B] text-xs">
                {filteredTransactions.map((t) => {
                  const cat = t.category_id ? catLookup.get(t.category_id) : null;
                  const isExpense = t.type === "expense";
                  const dateStr = new Date(t.occurred_at).toLocaleDateString("en-IN", {
                    month: "short",
                    day: "numeric",
                    year: "numeric",
                  });
                  const timeStr = new Date(t.occurred_at).toLocaleTimeString("en-IN", {
                    hour: "2-digit",
                    minute: "2-digit",
                  });

                  return (
                    <tr
                      key={t.id}
                      className="hover:bg-[#1E293B]/40 transition group"
                    >
                      {/* Receiver & Note */}
                      <td className="py-3 px-4 max-w-[180px] sm:max-w-xs">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="font-semibold text-white">
                            {t.receiver_vendor || (t.note ? t.note : "Transaction")}
                          </span>
                          {t.source_app && (
                            <span className="sm:hidden text-[9px] px-1.5 py-0.2 rounded bg-[#1E293B] text-slate-400">
                              {t.source_app}
                            </span>
                          )}
                        </div>
                        {t.receiver_vendor && t.note && (
                          <p className="text-[11px] text-slate-400 truncate mt-0.5">
                            {t.note}
                          </p>
                        )}
                      </td>

                      {/* Category */}
                      <td className="py-3 px-3 whitespace-nowrap">
                        {cat ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium bg-[#0B0F17] border border-[#1E293B]">
                            <span
                              className="w-2 h-2 rounded-full shrink-0"
                              style={{ backgroundColor: cat.color || "#10B981" }}
                            />
                            <span className="text-slate-200">
                              {cat.icon} {cat.name}
                            </span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-medium bg-[#0B0F17] border border-[#1E293B] text-slate-400">
                            📦 Uncategorized
                          </span>
                        )}
                      </td>

                      {/* Source */}
                      <td className="py-3 px-3 whitespace-nowrap">
                        {t.source_app === "UPI" ? (
                          <span className="px-2.5 py-0.5 rounded-md bg-blue-500/15 text-blue-400 border border-blue-500/30 text-[11px] font-bold">
                            UPI
                          </span>
                        ) : t.source_app === "Cash" ? (
                          <span className="px-2.5 py-0.5 rounded-md bg-amber-500/15 text-amber-400 border border-amber-500/30 text-[11px] font-bold">
                            Cash
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded-md bg-[#0B0F17] text-slate-400 border border-[#1E293B] text-[11px]">
                            {t.source_app || "Manual"}
                          </span>
                        )}
                      </td>

                      {/* Date */}
                      <td className="py-3 px-3 whitespace-nowrap">
                        <div className="text-slate-300 font-medium">{dateStr}</div>
                        <div className="text-[10px] text-slate-500">{timeStr}</div>
                      </td>

                      {/* Amount */}
                      <td className="py-3 px-4 text-right font-mono font-bold whitespace-nowrap">
                        <span
                          className={isExpense ? "text-rose-400" : "text-emerald-400"}
                        >
                          {isExpense ? "-" : "+"}₹
                          {Number(t.amount).toLocaleString("en-IN", {
                            minimumFractionDigits: 2,
                          })}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-3 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1">
                          <button
                            onClick={() => handleEdit(t)}
                            title="Edit transaction"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDelete(t.id)}
                            title="Delete transaction"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition cursor-pointer"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete Confirmation Popup */}
      <ConfirmModal
        isOpen={!!deleteConfirmId}
        title="Delete Transaction"
        message="Are you sure you want to delete this transaction record? This action cannot be undone."
        confirmText="Delete Transaction"
        onConfirm={async () => {
          if (deleteConfirmId) {
            await deleteTransaction(deleteConfirmId);
          }
        }}
        onClose={() => setDeleteConfirmId(null)}
      />
    </div>
  );
}
