"use client";

import React, { useState } from "react";
import { Download, FileSpreadsheet, FileText, X } from "lucide-react";

interface ExportModalProps {
  isOpen: boolean;
  onClose: () => void;
  transactionCount: number;
  onExport: (fileName: string, format: "csv" | "pdf") => void;
}

export default function ExportModal({
  isOpen,
  onClose,
  transactionCount,
  onExport,
}: ExportModalProps) {
  const [baseName, setBaseName] = useState("AutoTrack_Ledger");
  const [format, setFormat] = useState<"csv" | "pdf">("csv");

  if (!isOpen) return null;

  // Compute today's date in IST (YYYY-MM-DD)
  const nowIST = new Date(Date.now() + 5.5 * 60 * 60 * 1000);
  const todayDate = nowIST.toISOString().slice(0, 10);

  const cleanName = baseName.trim().replace(/[\\/:*?"<>|\s]+/g, "_") || "AutoTrack_Ledger";
  const finalPreviewName = `${cleanName}_${todayDate}.${format}`;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onExport(cleanName, format);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="relative w-full max-w-md bg-[#111827] border border-[#1E293B] rounded-2xl p-6 shadow-2xl space-y-5 text-white animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[#1E293B] pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
              <Download className="w-5 h-5 text-emerald-400" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Export Ledger</h2>
              <p className="text-xs text-slate-400">
                Exporting {transactionCount} filtered transaction{transactionCount === 1 ? "" : "s"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-[#1E293B] transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 1. File Name Input */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              1. File Name
            </label>
            <input
              type="text"
              value={baseName}
              onChange={(e) => setBaseName(e.target.value)}
              placeholder="e.g. AutoTrack_Ledger"
              className="w-full bg-[#0B0F17] border border-[#1E293B] rounded-xl px-3.5 py-2 text-sm text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
              autoFocus
            />
            {/* Live Preview */}
            <p className="text-[11px] text-slate-400 mt-1.5 flex items-center gap-1 font-mono">
              <span className="text-slate-500">Will be saved as:</span>
              <span className="text-emerald-400 font-semibold truncate">{finalPreviewName}</span>
            </p>
          </div>

          {/* 2. File Format / Type Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2">
              2. Select File Format
            </label>
            <div className="grid grid-cols-2 gap-3">
              {/* CSV Option */}
              <button
                type="button"
                onClick={() => setFormat("csv")}
                className={`flex flex-col items-start p-3 rounded-xl border text-left transition cursor-pointer ${
                  format === "csv"
                    ? "bg-emerald-500/10 border-emerald-500 text-white shadow-xs"
                    : "bg-[#0B0F17] border-[#1E293B] text-slate-400 hover:border-slate-700 hover:text-slate-300"
                }`}
              >
                <div className="flex items-center gap-2 mb-1 w-full justify-between">
                  <div className="flex items-center gap-1.5">
                    <FileSpreadsheet className={`w-4 h-4 ${format === "csv" ? "text-emerald-400" : "text-slate-400"}`} />
                    <span className="text-xs font-bold text-white">CSV</span>
                  </div>
                  <div className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${format === "csv" ? "border-emerald-400 bg-emerald-500" : "border-slate-600"}`}>
                    {format === "csv" && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                  </div>
                </div>
                <span className="text-[10px] text-slate-400 leading-tight">
                  Spreadsheet data (.csv) for Excel & Sheets
                </span>
              </button>

              {/* PDF (Searchable) Option */}
              <button
                type="button"
                onClick={() => setFormat("pdf")}
                className={`flex flex-col items-start p-3 rounded-xl border text-left transition cursor-pointer ${
                  format === "pdf"
                    ? "bg-emerald-500/10 border-emerald-500 text-white shadow-xs"
                    : "bg-[#0B0F17] border-[#1E293B] text-slate-400 hover:border-slate-700 hover:text-slate-300"
                }`}
              >
                <div className="flex items-center gap-2 mb-1 w-full justify-between">
                  <div className="flex items-center gap-1.5">
                    <FileText className={`w-4 h-4 ${format === "pdf" ? "text-emerald-400" : "text-slate-400"}`} />
                    <span className="text-xs font-bold text-white">PDF</span>
                  </div>
                  <div className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${format === "pdf" ? "border-emerald-400 bg-emerald-500" : "border-slate-600"}`}>
                    {format === "pdf" && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                  </div>
                </div>
                <span className="text-[10px] text-emerald-400/90 font-medium leading-tight">
                  Searchable PDF (.pdf) with ledger summary
                </span>
              </button>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-[#1E293B]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-600 transition flex items-center gap-1.5 shadow-sm cursor-pointer"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download {format.toUpperCase()}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
