import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/lib/authContext";

export const metadata: Metadata = {
  title: "Auto Track — Financial Overview & Ledger",
  description: "Real-time intelligent expense tracking with Supabase sync and instant overlay confirmation.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark h-full">
      <body className="min-h-full bg-[#0B0F17] text-slate-100 antialiased flex flex-col selection:bg-emerald-500 selection:text-white">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
