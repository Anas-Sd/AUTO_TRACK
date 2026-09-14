"use client";

import React from "react";
import { Transaction } from "@/lib/types";
import ManualLogWidget from "./ManualLogWidget";

interface TransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTransaction?: Transaction | null;
}

export default function TransactionModal({
  isOpen,
  onClose,
  initialTransaction,
}: TransactionModalProps) {
  if (!isOpen) return null;

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B0F17]/85 backdrop-blur-sm p-4 overflow-y-auto animate-in fade-in duration-100 cursor-pointer"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md bg-[#131A26] border border-[#1E293B] rounded-2xl p-4 shadow-2xl relative my-8 cursor-default"
      >
        <ManualLogWidget onClose={onClose} initialTransaction={initialTransaction} />
      </div>
    </div>
  );
}
