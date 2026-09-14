-- AUTO TRACK SUPABASE SCHEMA & RLS SETUP

DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS vault_codes CASCADE;

CREATE TABLE vault_codes (
  code            TEXT PRIMARY KEY,
  label           TEXT DEFAULT 'My Vault',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_accessed   TIMESTAMPTZ
);

CREATE TABLE categories (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vault_code      TEXT NOT NULL REFERENCES vault_codes(code) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  icon            TEXT,
  color           TEXT,
  monthly_cap     NUMERIC,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (vault_code, name)
);

CREATE TABLE transactions (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vault_code        TEXT NOT NULL REFERENCES vault_codes(code) ON DELETE CASCADE,
  note              TEXT,
  receiver_vendor   TEXT,
  amount            NUMERIC NOT NULL,
  type              TEXT NOT NULL CHECK (type IN ('income','expense')),
  category_id       UUID REFERENCES categories(id) ON DELETE SET NULL,
  source_app        TEXT,
  occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_vault_occurred ON transactions (vault_code, occurred_at DESC);
CREATE INDEX idx_transactions_vault_category ON transactions (vault_code, category_id);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vault_codes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "vault can read own categories" ON categories;
CREATE POLICY "vault can read own categories" ON categories
  FOR SELECT USING (vault_code = (auth.jwt() ->> 'vault_code'));

DROP POLICY IF EXISTS "vault can write own categories" ON categories;
CREATE POLICY "vault can write own categories" ON categories
  FOR ALL USING (vault_code = (auth.jwt() ->> 'vault_code'));

DROP POLICY IF EXISTS "vault can read own transactions" ON transactions;
CREATE POLICY "vault can read own transactions" ON transactions
  FOR SELECT USING (vault_code = (auth.jwt() ->> 'vault_code'));

DROP POLICY IF EXISTS "vault can write own transactions" ON transactions;
CREATE POLICY "vault can write own transactions" ON transactions
  FOR ALL USING (vault_code = (auth.jwt() ->> 'vault_code'));
