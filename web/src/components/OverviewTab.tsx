"use client";

import React, { useState, useMemo } from "react";
import { useAuth } from "@/lib/authContext";
import ManualLogWidget from "./ManualLogWidget";
import {
  TrendingUp,
  TrendingDown,
  Calendar,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  PieChart as PieIcon,
} from "lucide-react";
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
} from "recharts";

type TimeRange = "today" | "monthly" | "yearly" | "custom";

export default function OverviewTab() {
  const { transactions, categories } = useAuth();
  // Requirement: By default timeframe filter should be "today"
  const [timeRange, setTimeRange] = useState<TimeRange>("today");
  const [customStart, setCustomStart] = useState<string>("");
  const [customEnd, setCustomEnd] = useState<string>("");
  const [activeCategoryIndex, setActiveCategoryIndex] = useState<number | null>(null);

  // Filter transactions according to selected range
  const filteredTransactions = useMemo(() => {
    const now = new Date();

    return transactions.filter((txn) => {
      const date = new Date(txn.occurred_at);

      if (timeRange === "today") {
        return (
          date.getFullYear() === now.getFullYear() &&
          date.getMonth() === now.getMonth() &&
          date.getDate() === now.getDate()
        );
      } else if (timeRange === "monthly") {
        return (
          date.getFullYear() === now.getFullYear() &&
          date.getMonth() === now.getMonth()
        );
      } else if (timeRange === "yearly") {
        return date.getFullYear() === now.getFullYear();
      } else if (timeRange === "custom") {
        if (!customStart && !customEnd) return true;
        const start = customStart ? new Date(customStart).getTime() : 0;
        const end = customEnd ? new Date(customEnd + "T23:59:59").getTime() : Infinity;
        const tTime = date.getTime();
        return tTime >= start && tTime <= end;
      }
      return true;
    });
  }, [transactions, timeRange, customStart, customEnd]);

  // Aggregate overall income & expense totals
  const { totalIncome, totalExpense } = useMemo(() => {
    let income = 0;
    let expense = 0;
    filteredTransactions.forEach((t) => {
      const amt = Number(t.amount) || 0;
      if (t.type === "income") {
        income += amt;
      } else {
        expense += amt;
      }
    });
    return { totalIncome: income, totalExpense: expense };
  }, [filteredTransactions]);

  // Donut data: include both Expense & Income per category
  const donutData = useMemo(() => {
    const categoryMap: {
      [id: string]: {
        id: string;
        name: string;
        icon: string;
        color: string;
        monthly_cap?: number | null;
        expense: number;
        income: number;
        total: number;
      };
    } = {};

    // Populate categories
    categories.forEach((c) => {
      categoryMap[c.id] = {
        id: c.id,
        name: c.name,
        icon: c.icon || "🏷️",
        color: c.color || "#10B981",
        monthly_cap: c.monthly_cap || null,
        expense: 0,
        income: 0,
        total: 0,
      };
    });

    // Uncategorized bucket
    const uncategorizedKey = "uncategorized";
    categoryMap[uncategorizedKey] = {
      id: uncategorizedKey,
      name: "Uncategorized",
      icon: "📦",
      color: "#64748B",
      expense: 0,
      income: 0,
      total: 0,
    };

    filteredTransactions.forEach((t) => {
      const catKey = t.category_id && categoryMap[t.category_id] ? t.category_id : uncategorizedKey;
      const amt = Number(t.amount) || 0;
      if (t.type === "expense") {
        categoryMap[catKey].expense += amt;
      } else {
        categoryMap[catKey].income += amt;
      }
      categoryMap[catKey].total += amt;
    });

    // Include categories with either expense or income in timeframe
    const list = Object.values(categoryMap).filter((c) => c.total > 0);
    list.sort((a, b) => b.total - a.total);
    return list;
  }, [filteredTransactions, categories]);

  const totalVolume = useMemo(() => {
    return donutData.reduce((sum, item) => sum + item.total, 0);
  }, [donutData]);

  const activeCategory =
    activeCategoryIndex !== null && donutData[activeCategoryIndex]
      ? donutData[activeCategoryIndex]
      : null;

  // Background tap handler for mobile tap-outside
  const handleContainerClick = () => {
    setActiveCategoryIndex(null);
  };

  return (
    <div
      onClick={handleContainerClick}
      className="space-y-5 pb-20 md:pb-8 flex flex-col"
    >
      {/* 2-Level Manual Transaction Logging Widget (Top 1/3rd Area) */}
      <ManualLogWidget />

      {/* Header & Filter Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 order-1">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Wallet className="w-5 h-5 text-emerald-400" />
            Financial Overview
          </h2>
          <p className="text-xs text-slate-400">
            Real-time cashflow analytics and category breakdown
          </p>
        </div>

        {/* Timeframe Pills (Default: Today) */}
        <div className="flex items-center gap-1 bg-[#131A26] p-1 rounded-xl border border-[#1E293B] self-start sm:self-auto overflow-x-auto max-w-full">
          {(["today", "monthly", "yearly", "custom"] as TimeRange[]).map((r) => (
            <button
              key={r}
              onClick={(e) => {
                e.stopPropagation();
                setTimeRange(r);
              }}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold capitalize whitespace-nowrap transition cursor-pointer ${
                timeRange === r
                  ? "bg-emerald-500 text-white shadow-sm"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              {r === "custom" ? "Custom Range" : r}
            </button>
          ))}
        </div>
      </div>

      {/* Custom Date Range Picker */}
      {timeRange === "custom" && (
        <div
          onClick={(e) => e.stopPropagation()}
          className="flex flex-wrap items-center gap-3 p-3 bg-[#131A26] border border-[#1E293B] rounded-xl text-xs order-2"
        >
          <div className="flex items-center gap-2">
            <Calendar className="w-4 h-4 text-emerald-400" />
            <span className="text-slate-400">From:</span>
            <input
              type="date"
              value={customStart}
              onChange={(e) => setCustomStart(e.target.value)}
              className="bg-[#0B0F17] border border-[#1E293B] rounded-lg px-2.5 py-1 text-white text-xs focus:outline-none focus:border-emerald-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-slate-400">To:</span>
            <input
              type="date"
              value={customEnd}
              onChange={(e) => setCustomEnd(e.target.value)}
              className="bg-[#0B0F17] border border-[#1E293B] rounded-lg px-2.5 py-1 text-white text-xs focus:outline-none focus:border-emerald-500"
            />
          </div>
        </div>
      )}

      {/* Responsive Section Ordering:
          Mobile (< lg): Order 1=Donut, Order 2=Category Breakdown, Order 3=Metric Cards
          Desktop (>= lg): Metric Cards on top, Donut (left 7 cols) & Breakdown (right 5 cols)
      */}
      <div className="flex flex-col lg:grid lg:grid-cols-12 gap-6 items-start order-3">
        {/* Donut Card (Mobile Order 1, Desktop Left 7 cols) */}
        <div
          onClick={(e) => e.stopPropagation()}
          className="w-full lg:col-span-7 bg-[#131A26] border border-[#1E293B] rounded-2xl p-4 sm:p-6 order-1 lg:order-2"
        >
          <div className="flex items-center justify-between mb-2 sm:mb-4">
            <div className="flex items-center gap-2">
              <PieIcon className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-white">Income & Expense Breakdown</h3>
            </div>
            <span className="text-xs text-slate-400">
              {donutData.length} {donutData.length === 1 ? "Category" : "Categories"}
            </span>
          </div>

          {donutData.length === 0 ? (
            <div className="py-16 text-center text-slate-500 text-xs">
              <PieIcon className="w-10 h-10 mx-auto mb-2 opacity-30 text-slate-400" />
              No activity recorded for {timeRange}.
            </div>
          ) : (
            <div className="relative">
              {/* Donut container - slightly reduced radius on mobile for perfect dimensions */}
              <div className="h-56 sm:h-72 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={donutData}
                      dataKey="total"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius="62%"
                      outerRadius="88%"
                      paddingAngle={3}
                      onMouseEnter={(_, index) => setActiveCategoryIndex(index)}
                      onMouseLeave={() => setActiveCategoryIndex(null)}
                      onClick={(_, index) => {
                        setActiveCategoryIndex(
                          activeCategoryIndex === index ? null : index
                        );
                      }}
                    >
                      {donutData.map((entry, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={entry.color || "#10B981"}
                          stroke="#131A26"
                          strokeWidth={2}
                          className="cursor-pointer transition-opacity hover:opacity-80"
                        />
                      ))}
                    </Pie>
                    <Tooltip content={() => null} />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              {/* CENTER OF CIRCLE DETAILS (Laptop hover / Mobile tap) */}
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center p-2">
                {activeCategory ? (
                  <div className="animate-in fade-in zoom-in-95 duration-100 flex flex-col items-center justify-center max-w-[130px] sm:max-w-[160px]">
                    <div className="flex items-center justify-center gap-1 text-xs font-bold text-white mb-0.5 truncate w-full">
                      <span className="text-sm">{activeCategory.icon}</span>
                      <span className="truncate">{activeCategory.name}</span>
                    </div>

                    {activeCategory.income > 0 && (
                      <div className="flex items-center justify-center gap-0.5 text-[11px] font-mono font-bold text-emerald-400">
                        <ArrowUpRight className="w-3 h-3 shrink-0" />
                        <span>+₹{activeCategory.income.toLocaleString("en-IN")}</span>
                      </div>
                    )}

                    {activeCategory.expense > 0 && (
                      <div className="flex items-center justify-center gap-0.5 text-[11px] font-mono font-bold text-rose-400">
                        <ArrowDownRight className="w-3 h-3 shrink-0" />
                        <span>-₹{activeCategory.expense.toLocaleString("en-IN")}</span>
                      </div>
                    )}

                    {/* Category Net Balance */}
                    <div className="mt-1 pt-1 border-t border-[#1E293B] w-full flex flex-col items-center justify-center text-[10px] font-mono leading-tight">
                      <div className="flex items-center gap-1">
                        <span className="text-slate-400">Bal:</span>
                        <span
                          className={`font-bold ${
                            activeCategory.income - activeCategory.expense >= 0
                              ? "text-emerald-400"
                              : "text-rose-400"
                          }`}
                        >
                          {activeCategory.income - activeCategory.expense >= 0 ? "+" : "-"}₹
                          {Math.abs(activeCategory.income - activeCategory.expense).toLocaleString("en-IN")}
                        </span>
                      </div>

                      {/* Remaining Opening Balance if set */}
                      {activeCategory.monthly_cap != null && activeCategory.monthly_cap > 0 && (
                        <div className="text-[9px] text-slate-400 mt-0.5">
                          {activeCategory.monthly_cap - activeCategory.expense >= 0 ? (
                            <span className="text-slate-400">
                              Rem Bal: <span className="text-emerald-400 font-semibold">₹{(activeCategory.monthly_cap - activeCategory.expense).toLocaleString("en-IN")}</span>
                            </span>
                          ) : (
                            <span className="text-rose-400 font-semibold">
                              Over opening bal!
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="animate-in fade-in duration-100 flex flex-col items-center justify-center">
                    <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-0.5">
                      Net Cashflow
                    </span>
                    <div
                      className={`text-sm sm:text-base font-bold font-mono tracking-tight ${
                        totalIncome - totalExpense >= 0 ? "text-emerald-400" : "text-rose-400"
                      }`}
                    >
                      {totalIncome - totalExpense >= 0 ? "+" : "-"}₹
                      {Math.abs(totalIncome - totalExpense).toLocaleString("en-IN")}
                    </div>
                    <div className="text-[9px] text-slate-500 font-mono mt-0.5">
                      +₹{totalIncome.toLocaleString("en-IN")} | -₹{totalExpense.toLocaleString("en-IN")}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Category Breakdown List (Mobile Order 2, Desktop Right 5 cols) */}
        <div
          onClick={(e) => e.stopPropagation()}
          className="w-full lg:col-span-5 bg-[#131A26] border border-[#1E293B] rounded-2xl p-4 sm:p-6 space-y-3 order-2 lg:order-3"
        >
          <div className="flex items-center justify-between pb-2 border-b border-[#1E293B]">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Category Breakdown
            </span>
            <span className="text-xs font-mono text-slate-400">Volume</span>
          </div>

          <div className="max-h-72 overflow-y-auto space-y-2 pr-1">
            {donutData.length === 0 ? (
              <p className="text-xs text-slate-500 py-4 text-center">No categories to display</p>
            ) : (
              donutData.map((item, idx) => {
                const isSelected = activeCategoryIndex === idx;

                return (
                  <div
                    key={item.id}
                    onMouseEnter={() => setActiveCategoryIndex(idx)}
                    onMouseLeave={() => setActiveCategoryIndex(null)}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveCategoryIndex(isSelected ? null : idx);
                    }}
                    className={`p-2.5 rounded-xl border transition cursor-pointer flex items-center justify-between ${
                      isSelected
                        ? "bg-[#1E293B] border-emerald-500/50"
                        : "bg-[#0B0F17] border-[#1E293B] hover:bg-[#1E293B]/40"
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span className="text-base">{item.icon}</span>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <span
                            className="w-2 h-2 rounded-full shrink-0"
                            style={{ backgroundColor: item.color }}
                          />
                          <p className="text-xs font-medium text-white truncate">{item.name}</p>
                        </div>
                      </div>
                    </div>

                    <div className="text-right font-mono shrink-0 space-y-0.5">
                      {item.expense > 0 && (
                        <div className="text-xs font-bold text-rose-400">
                          -₹{item.expense.toLocaleString("en-IN")}
                        </div>
                      )}
                      {item.income > 0 && (
                        <div className="text-xs font-bold text-emerald-400">
                          +₹{item.income.toLocaleString("en-IN")}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* 3 Summary Metric Cards (Mobile Order 3, Desktop Top) */}
        <div
          onClick={(e) => e.stopPropagation()}
          className="w-full lg:col-span-12 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 order-3 lg:order-1"
        >
          {/* Total Income */}
          <div className="p-4 sm:p-5 rounded-2xl bg-[#131A26] border border-[#1E293B] relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Total Income
              </span>
              <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <div className="text-2xl sm:text-3xl font-bold text-emerald-400 font-mono tracking-tight">
                +₹{totalIncome.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
              </div>
              <p className="text-[11px] text-slate-500 mt-1">For selected timeframe ({timeRange})</p>
            </div>
          </div>

          {/* Total Expense */}
          <div className="p-4 sm:p-5 rounded-2xl bg-[#131A26] border border-[#1E293B] relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Total Expense
              </span>
              <div className="w-8 h-8 rounded-lg bg-rose-500/10 flex items-center justify-center text-rose-400">
                <TrendingDown className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <div className="text-2xl sm:text-3xl font-bold text-rose-400 font-mono tracking-tight">
                -₹{totalExpense.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
              </div>
              <p className="text-[11px] text-slate-500 mt-1">For selected timeframe ({timeRange})</p>
            </div>
          </div>

          {/* Net Balance */}
          <div className="p-4 sm:p-5 rounded-2xl bg-[#131A26] border border-[#1E293B] relative overflow-hidden sm:col-span-2 lg:col-span-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Net Balance
              </span>
              <div className="w-8 h-8 rounded-lg bg-sky-500/10 flex items-center justify-center text-sky-400">
                <Wallet className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <div
                className={`text-2xl sm:text-3xl font-bold font-mono tracking-tight ${
                  totalIncome - totalExpense >= 0 ? "text-emerald-400" : "text-rose-400"
                }`}
              >
                {totalIncome - totalExpense >= 0 ? "+" : "-"}₹
                {Math.abs(totalIncome - totalExpense).toLocaleString("en-IN", {
                  minimumFractionDigits: 2,
                })}
              </div>
              <p className="text-[11px] text-slate-500 mt-1">Income minus Expenses</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
