# FireCash — Slip & Income Tracker

FireCash is an offline-first Android app (Jetpack Compose, Material 3 dark theme) for logging Thai bank transfer slips and tracking income/expense. Scan a PromptPay/bank-slip QR, verify it with EasySlip when configured, and keep everything locally — no account required.

## Features

**Capture & OCR**
- Live CameraX preview with center-frame QR scanning (60% ROI — only QRs inside the frame are detected)
- Gallery picker and photo import; scanned slips auto-save to your Account
- EasySlip verification (`api.easyslip.com/v2/verify/bank`) with offline fallback: unverified slips show unknown data and a **Sync unverified** button once an API key is set

**Account (homepage)**
- Balance card: Money In / Money Out, camera shortcut at top-right, in-app logo + FireCash title
- Slip list scrolls bottom → top, grouped by day with **daily net total** in the date header; time shown above each amount
- Search slips by date, title, or amount — **exact match** for QR payload / transaction ref
- Long-press to multi-select and delete unknown/invalid slips only
- Manual income/expense entry (amount + note)
- Sync button: **tap** = only new photos, **hold 10s** = full resync (re-detect all photos + re-verify every slip on server)
- Each slip links to its actual photo on the device (persistent app storage)

**Smart detection**
- **My Names**: add your name variants (Thai/English); slips where the receiver is you → Income, sender is you → Expense, both you → Transfer (excluded from balance) — applied retroactively
- **Notification Income/Expense**: `NotificationListenerService` scoops the first number after a configurable prefix (e.g. `โอนเงินให้คุณ ฿`), per-app whitelist with multiple prefixes per app, separate whitelists for money-in and money-out
- Optional **Keep listening in background**: music-player-style non-dismissible status notification + battery-optimization exemption so notifications keep being caught

**Analytics**
- Spending summary with vertical stick chart (Day / Week / Month) and AI insights (recurring vendors, peaks, category dominance)

**Settings (Safe / Dangerous)**
- **Safe**: Base Currency, My Names, Tracked Folders (auto-scan folders for new slips), Keyword Mapping
- **Dangerous** (collapsible red section): EasySlip API key + duplicate check, Notification Income/Expense whitelists, Background & Battery, **Data Transfer**

**Data transfer**
- Export everything (slips, API key, all options, whitelists) to a single JSON file — import it on another phone to clone your app state

**Navigation**
- Android back gesture mirrors in-app navigation; Account is the homepage

## Build

```bash
./gradlew assembleDebug
```

Requires: Android SDK (compileSdk 36, minSdk 24). The debug build uses `debug.keystore`; release signing reads `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` env vars.

## Docs

- `docs/DEVELOPMENT_LOG.md` — day-by-day build journal
- `docs/firecash_full_plan.md`, `docs/firecash_ai_studio_systems_plan.md` — original planning docs
