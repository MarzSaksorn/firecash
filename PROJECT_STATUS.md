# FireCash Project – Status & Next Steps

> Last updated 2026-08-31. See `AGENTS.md` for the authoritative architecture notes; this file is the human-readable snapshot. Previous versions of this file were stale (referenced `CaptureScreen.kt` and claimed OCR was broken) — the OCR pipeline has since been reworked.

## Current State

| Item | Status | Details |
|------|--------|---------|
| Build | OK | `gradlew assembleDebug` succeeds (`BUILD SUCCESSFUL`, warnings only, no errors) |
| Tests | 10/11 pass | 1 pre-existing failure: `EasySlipClientTest.testMockVerificationPipeline` — the legacy client's simulation changed to return `UNVERIFIED`; test is dead code, left alone |
| Camera & QR scan | Done | CameraX live preview in `PhotoCaptureScreen.kt`; center-frame 60% ROI filtering — only QRs inside the frame overlay are detected |
| OCR pipeline | Done (barcode-only) | `OcrProcessor.processReceipt(imageUri, samplePreset)` reads the photo and runs ML Kit **barcode scanning** (QR payloads); `scanCenterOnly` crops center 60%. Text recognition is intentionally unused. `samplePreset` bypasses the image for demo/testing |
| Slip photos | Done | Capture/gallery/import save to persistent external storage; each slip stores its `photoPath`; viewable via FileProvider |
| Verification | Done | Multi-provider: EasySlip, ThunderAPI, Slip2Go (`SlipVerificationManager`); no key / network failure / unknown code → `simulateSlipVerification()` fallback (payload ending `9999` ⇒ DUPLICATE, else UNVERIFIED) |
| Account (homepage) | Done | Balance card (money in/out), reverse bottom→top slip list grouped by day with daily-net header, search (exact for QR payload/ref), long-press multi-select delete of unverifiable slips, manual income/expense entry, sync button (tap = new photos, hold 10s = full resync) |
| App Mode | Done | Settings → App Mode: **Personal** (manual `+` entry button on home card) or **Shop** (camera button on home card); persisted and carried through JSON export/import |
| Notification income/expense | Done | `IncomeNotificationService` prefix-based amount scooping, per-app whitelists (money-in and money-out), permanent presets (KBank/SCB, toggleable but not removable), optional foreground "keep listening" service |
| Analytics | Done | Spending summary with Day/Week/Month vertical stick chart + AI insights |
| Settings | Done | Safe (Base Currency, My Names, Tracked Folders, Keyword Mapping) / collapsible red **Dangerous** (EasySlip, Notification whitelists, Background & Battery, Data Transfer) |
| Data transfer | Done | Full JSON export/import incl. API keys, whitelists, app mode; import preserves permanent presets and won't wipe device lists on missing keys |
| Persistence | SharedPreferences | `firecash_settings` prefs hold slips/whitelists/settings as JSON strings; Room is NOT used by the live app |

## Architecture (read this first)

Two parallel architectures exist; **the live app is NOT the Room/ViewModel one**.

- **Live**: `MainActivity` → `ui/MainApp.kt` — a single ~1200-line composable holding all state (`remember { mutableStateOf(...) }` + `SharedPreferences`). Navigation = four booleans + `BackHandler`s. Data flow: QR photo → `OcrProcessor` (ML Kit barcode) → `addSlip()` → `SlipVerificationManager.verifyPayload()` → `SavedSlip` JSON → prefs → `AccountScreen`.
- **Dead code (do not extend, may be deleted)**: `ui/viewmodel/MainViewModel.kt`, `data/local/*` (Room), `data/model/Expense.kt`/`KeywordRule.kt`, `data/repository/*`, `data/export/ExportManager.kt`, `data/backup/DriveBackupManager.kt`, `data/easyslip/EasySlipClient.kt`. Replicate behavior in MainApp if a feature needs it.

## Known Gaps / Not Implemented

- **Keyword Mapping** in Settings is a stub — `MainApp` passes `rules = emptyList()`; no keyword rules are persisted or applied.
- **Onboarding screen** — planned but never built.
- **`BottomNavBar` / `CaptureBottomBar`** — dead components after top-bar navigation refactor; `BottomNavBar.kt` is unreferenced.
- **Google Drive backup** — removed by request; `DriveBackupManager.kt` sits unreferenced in `data/`.
- **`proxy.yml` CI workflow** (EasySlip proxy Cloud Run deploy) is a stub — the `proxy/` directory doesn't exist; gcloud command commented out.
- **Text recognition** (`mlkit:text-recognition` dependency) present but unused by design — slips are identified via QR barcodes only.

## Next Steps (candidates)

1. Wire Keyword Mapping persistence (replace `rules = emptyList()`).
2. Fix or delete `EasySlipClientTest` / legacy `EasySlipClient` to green the test suite.
3. Build the planned Onboarding screen.
4. Prune dead code (Room, ViewModel, repositories, `BottomNavBar`) to reduce confusion — AGENTS.md documents what's safe to remove.
5. CI: make `assembleDebug` artifact release (tag `debug-build-18` exists) — add lint/test to the workflow.

## Key Files

| File | Role |
|------|------|
| `app/src/main/java/com/example/ui/MainApp.kt` | All app state, nav, persistence, export/import, sync |
| `app/src/main/java/com/example/ui/screens/AccountScreen.kt` | Homepage: balance card, reversed slip list, search, selection mode, app-mode action button |
| `app/src/main/java/com/example/ui/screens/PhotoCaptureScreen.kt` | CameraX preview + QR scan (center 60% ROI) |
| `app/src/main/java/com/example/ui/screens/QrPayloadScreen.kt` | Slip detail: verification card, photo link, copy-to-clipboard |
| `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` | Safe/Dangerous settings incl. App Mode picker, whitelists, Data Transfer |
| `app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt` | Spending chart + AI insights |
| `app/src/main/java/com/example/data/ocr/OcrProcessor.kt` + `SlipDataParser.kt` | QR payload extraction + regex parsing |
| `app/src/main/java/com/example/data/verification/SlipVerificationManager.kt` | Multi-provider verification + fallback simulation |
| `app/src/main/java/com/example/service/IncomeNotificationService.kt` + `NotificationPresets.kt` | Notification income/expense capture + presets |
