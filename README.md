# FireCash — Slip & Income Tracker

FireCash is an offline-first Android app (Jetpack Compose, Material 3 dark theme) for logging Thai bank transfer slips and tracking income/expense. Scan a PromptPay/bank-slip QR, verify it with EasySlip when configured, and keep everything locally — no account required.

> **Design**: FireCash uses the **Stitch design system** — a custom dark theme with `#121316` background, `#FF6B00` primary accent, `#10B981` (income) and `#6366F1` (expense) semantic colors. See [`docs/figma_design_spec.md`](docs/figma_design_spec.md) for the complete Figma specification.

## Features

**Capture & OCR**
- Live CameraX preview with center-frame QR scanning (60% ROI — only QRs inside the frame are detected)
- Gallery picker and photo import; scanned slips auto-save to your Account
- Slip verification with **multiple providers** — EasySlip, ThunderAPI, or Slip2Go (choose in Settings, per-provider API key, offline fallback): unverified slips show unknown data and a **Sync unverified** button once a key is set

**Account (homepage)**
- Balance card: **Total Balance** (large), Income / Spent breakdown, camera shortcut at top-right, in-app logo + FireCash title
- **Live drag card swipe**: drag the balance card left/right to switch between Bank/Cash wallets — card follows your finger in real time with a 120px threshold
- **Page indicator dots**: animated dots below the card show the active wallet (Bank/Cash)
- **Bank/Cash wallet toggle**: compact toggle beside the settings button switches between Bank and Cash wallet views; the slip list and balance card filter by the active wallet; camera button becomes a plus button on the Cash tab
- Slip list scrolls bottom → top, grouped by day with **daily net total** in the date header; time shown above each amount
- **Filter chips**: All / Income / Expense / Transfer — quickly filter the slip list by transaction type
- **Manual override**: tap the classification toggle on any slip detail screen to override auto-detection (Income/Expense/Transfer or reset to auto)
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
- `docs/figma_design_spec.md` — Stitch design system Figma specification
- `docs/firecash_full_plan.md`, `docs/firecash_ai_studio_systems_plan.md` — original planning docs
