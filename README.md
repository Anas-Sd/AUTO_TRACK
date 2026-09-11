# AUTO TRACK — Master Architecture & Deployment Guide

> **Zero-cost, real-time end-to-end expense tracking system.**  
> Automatically intercepts UPI / Bank payment notifications on Android, displays an instant system overlay confirmation card over whatever app is open, and synchronizes with Supabase to reflect live on both the mobile view and the desktop web dashboard without refreshing.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         SUPABASE                            │
│  - PostgreSQL Database: vault_codes, categories, txns       │
│  - Row Level Security (RLS) scoped to JWT vault_code        │
│  - Edge Functions: create-vault, issue-vault-session        │
│  - Realtime WebSocket Channels (Postgres Changes)           │
└──────────────┬──────────────────────────────▲───────────────┘
               │ (Websockets / REST)          │ (Direct REST / Bearer JWT)
               ▼                              │
┌──────────────────────────────┐   ┌──────────┴─────────────────────────┐
│     NEXT.JS WEB APP (Vercel) │   │        ANDROID NATIVE APP          │
│  - 4 Tabs: Overview, Ledger, │   │  - NotificationListenerService     │
│    Categories, Settings      │   │  - Regex Amount & Direction Parser │
│  - Desktop + Mobile layout   │   │  - Floating System Overlay Window  │
│  - Bottom-right expanding FAB│   │  - Foreground Watcher Service      │
│  - Recharts Donut & CSV Exp. │   │  - EncryptedSharedPreferences      │
│  - HttpOnly Cookie Auth Guard│   │  - Offline SQLite Replay Queue     │
│  - Auto-login Route Handler  │   │  - Embedded WebView (Mobile Web)   │
└──────────────────────────────┘   └────────────────────────────────────┘
```

---

## 1. Non-Negotiable UI Specification

- **Exactly 4 Data Tabs Everywhere:**
  1. **Financial Overview**: Income/Expense cards, interactive category donut chart with hover/tap breakdown, timeframe pills (`Today | Monthly | Yearly | Custom Range`).
  2. **Transactions Ledger**: Search bar, collapsible multi-filter panel (Category, Type, Source, Timeframe), sort control, item counts, running totals (`+₹X | -₹Y`), editable/deletable table rows, and **CSV export button**.
  3. **Categories & Budgets**: Per-category monthly spend tracking, progress bar against monthly cap, empty state, Add/Edit/Delete modals.
  4. **System Settings**:
     - *Web*: Vault label editor, app diagnostics, session lock. (The vault code is **never** shown on the web).
     - *Android App*: Embedded WebView communicates via `AndroidBridge` JavascriptInterface to display native vault code (with copy button), live Notification Listener permission status, Draw Over Other Apps status, Battery Optimization guidance, and editable package allowlist.
- **Mobile Navigation Menu:**
  A single circular floating action button in the bottom-right that expands into:
  - `+ Manual Log Transaction` (Highlighted primary green pill)
  - `Financial Overview`
  - `Transactions Ledger`
  - `Categories & Budgets`
  - `System Settings`
  - `—`
  - `Log Out & Lock Vault` (Destructive red)

---

## 2. Supabase Backend Setup

1. Create a free project at [supabase.com](https://supabase.com).
2. Go to **SQL Editor** and run the contents of [`supabase/schema.sql`](supabase/schema.sql):
   - Creates `vault_codes`, `categories`, and `transactions` tables.
   - Configures indexes on `(vault_code, occurred_at desc)` and `(vault_code, category_id)`.
   - Enables Row Level Security (RLS) on all tables.
   - Restricts `vault_codes` strictly to service-role access.
   - Restricts `categories` and `transactions` reads and writes to callers matching `auth.jwt() ->> 'vault_code'`.
3. Deploy Edge Functions (or run via Next.js server-side API routes):
   - `supabase/functions/create-vault/index.ts`
   - `supabase/functions/issue-vault-session/index.ts`

---

## 3. Web App Deployment (Vercel — Free)

### Environment Variables

Configure these in your Vercel Project Settings or `.env.local`:

```env
NEXT_PUBLIC_SUPABASE_URL=https://<your-project>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOi...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOi...
SUPABASE_JWT_SECRET=<your-supabase-jwt-secret>
```

### Local Development

```bash
cd web
npm install
npm run dev
```

Visit `http://localhost:3000`.

### Production Build

```bash
npm run build
```

---

## 4. Android App (Kotlin & Gradle)

The Android app resides in [`android/`](android/) and builds a standalone sideloaded APK.

### Key Components

- `com.autotrack.app.TransactionListenerService`: Intercepts notifications from allowlisted packages (Google Pay, PhonePe, Paytm, BHIM, banks), dedupes duplicate alerts within a 60-second window, and extracts amounts and vendors using regex heuristics.
- `com.autotrack.app.ForegroundWatchService`: Persistent low-priority notification ensuring Auto Track is not killed by Android OEM battery managers.
- `com.autotrack.app.OverlayManager`: Displays a floating `TYPE_APPLICATION_OVERLAY` confirmation card over whatever app is open with amount, category selector, vendor, and quick Confirm/Snooze/Dismiss actions.
- `com.autotrack.app.OfflineQueueDbHelper`: Local SQLite queue that buffers confirmed transactions when offline and flushes them automatically once internet connectivity returns.
- `com.autotrack.app.MainActivity`: Hosts a hardware-accelerated `WebView` loading the Next.js mobile view with auto-login (`/auto-login?code=VAULT_CODE`), injecting the `AndroidBridge` interface for seamless native device integration.

### Build APK

Open the `android/` folder in **Android Studio**, or build from the command line:

```bash
cd android
./gradlew assembleDebug
```

The APK will be generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

### Sideloading & Testing

1. Transfer `app-debug.apk` to your phone and install.
2. Complete first-time onboarding:
   - Copy and save your generated **Vault Code**.
   - Grant **Notification Access**.
   - Grant **Draw Over Other Apps (Overlay)**.
   - Disable **Battery Optimization**.
3. To test transaction detection via ADB:
```bash
adb shell cmd notification post -S bigtext -t "Google Pay" "Tag" "Paid Rs. 450 to Starbucks on UPI ref 123456"
```
The floating confirmation card will instantly appear over your screen! Tap **Confirm**, and observe it sync immediately to both your phone's ledger and any open desktop browser session.
