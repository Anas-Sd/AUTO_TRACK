"use client";

import React, { useState, useRef, useEffect } from "react";
import { ChevronDown, Check } from "lucide-react";

export interface CustomSelectOption {
  value: string;
  label: string;
  icon?: React.ReactNode | string;
}

interface CustomSelectProps {
  value: string;
  onChange: (value: string) => void;
  options: CustomSelectOption[];
  placeholder?: string;
  className?: string;
  icon?: React.ReactNode;
}

export default function CustomSelect({
  value,
  onChange,
  options,
  placeholder = "Select an option",
  className = "",
  icon,
}: CustomSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === value);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setIsOpen(false);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <div ref={containerRef} className={`relative inline-block ${className} ${isOpen ? "z-50" : "z-10"}`}>
      {/* Trigger Button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`w-full flex items-center justify-between gap-2 bg-[#0B0F17] hover:bg-[#131A26] border border-[#1E293B] hover:border-[#334155] rounded-xl px-3.5 py-2 text-white text-xs font-medium focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500/30 transition cursor-pointer ${
          isOpen ? "border-emerald-500 ring-1 ring-emerald-500/30 bg-[#131A26]" : ""
        }`}
      >
        <div className="flex items-center gap-2 min-w-0 truncate">
          {icon && <span className="shrink-0">{icon}</span>}
          {selectedOption ? (
            <span className="flex items-center gap-1.5 truncate">
              {selectedOption.icon && <span className="shrink-0">{selectedOption.icon}</span>}
              <span className="truncate">{selectedOption.label}</span>
            </span>
          ) : (
            <span className="text-slate-500 truncate">{placeholder}</span>
          )}
        </div>
        <ChevronDown
          className={`w-3.5 h-3.5 text-emerald-400 shrink-0 transition-transform duration-200 ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>

      {/* Floating Options Menu */}
      {isOpen && (
        <div className="absolute left-0 min-w-full w-max max-w-[280px] top-full mt-1.5 bg-[#131A26] border border-[#1E293B] rounded-xl shadow-2xl z-[100] max-h-60 overflow-y-auto py-1 animate-in fade-in zoom-in-95 duration-100 divide-y divide-[#1E293B]/40">
          {options.length === 0 ? (
            <div className="px-3 py-2 text-xs text-slate-500 text-center">No options</div>
          ) : (
            options.map((opt) => {
              const isSelected = opt.value === value;
              return (
                <div
                  key={opt.value}
                  onClick={() => {
                    onChange(opt.value);
                    setIsOpen(false);
                  }}
                  className={`px-3 py-2 text-xs flex items-center justify-between cursor-pointer transition select-none ${
                    isSelected
                      ? "bg-[#1E293B] text-emerald-400 font-semibold"
                      : "text-slate-200 hover:bg-[#1E293B]/70 hover:text-white"
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    {opt.icon && <span className="shrink-0">{opt.icon}</span>}
                    <span className="truncate">{opt.label}</span>
                  </div>
                  {isSelected && <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 ml-1.5" />}
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
