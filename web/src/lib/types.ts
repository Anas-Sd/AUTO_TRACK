export interface VaultCode {
  code: string;
  label: string;
  created_at: string;
  last_accessed?: string | null;
}

export interface Category {
  id: string;
  vault_code: string;
  name: string;
  icon?: string | null;
  color?: string | null;
  monthly_cap?: number | null;
  opening_balance?: number | null;
  created_at: string;
}

export interface Transaction {
  id: string;
  vault_code: string;
  note?: string | null;
  receiver_vendor?: string | null;
  amount: number;
  type: "income" | "expense";
  category_id?: string | null;
  source_app?: string | null;
  raw_notification?: string | null;
  occurred_at: string;
  created_at: string;
  updated_at: string;
}

export type TimeframeFilter = "all" | "today" | "month" | "year" | "custom";