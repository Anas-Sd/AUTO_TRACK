"use client";

import React, { useState } from "react";
import { AlertTriangle, X } from "lucide-react";

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
  onConfirm: () => void | Promise<void>;
  onClose: () => void;
}

export default function ConfirmModal({
  isOpen,
  title,
  message,
  confirmText = "Delete",
  cancelText = "Cancel",
  isDanger = true,
  onConfirm,
  onClose,
}: ConfirmModalProps) {
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleConfirm = async () => {
    try {
      setLoading(true);
      await onConfirm();
    } finally {
      setLoading(false);
      onClose();
    }
  };

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm bg-[#131A26] border border-[#1E293B] rounded-2xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-150"
      >
        {/* Modal Header */}
        <div className="p-5 flex items-start justify-between border-b border-[#1E293B]">
          <div className="flex items-center gap-3">
            <div
              className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                isDanger
                  ? "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                  : "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
              }`}
            >
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white tracking-tight">{title}</h3>
              <p className="text-xs text-slate-400 mt-0.5">Action confirmation</p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={loading}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-[#1E293B] transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5">
          <p className="text-xs leading-relaxed text-slate-300">{message}</p>
        </div>

        {/* Modal Actions */}
        <div className="p-4 bg-[#0B0F17] border-t border-[#1E293B] flex items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white hover:bg-[#1E293B] transition cursor-pointer border border-[#1E293B]"
          >
            {cancelText}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={loading}
            className={`px-4 py-2 rounded-xl text-xs font-semibold text-white transition cursor-pointer shadow-md flex items-center gap-1.5 ${
              isDanger
                ? "bg-rose-600 hover:bg-rose-500 active:scale-98"
                : "bg-emerald-600 hover:bg-emerald-500 active:scale-98"
            } ${loading ? "opacity-60 cursor-not-allowed" : ""}`}
          >
            {loading ? (
              <span className="inline-block w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : null}
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
