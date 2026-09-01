# AGENTS.md — FireCash

Offline-first Android app (Kotlin, Jetpack Compose, Material 3 dark theme) for **shop operators** logging customer PromptPay/bank-slip transfers and tracking income/expense. Scans QR from slips via camera/gallery, verifies with EasySlip/ThunderAPI/Slip2Go (when an API key is set), cross-checks the slip photo text against the QR/bank amount for fraud, captures income/expense from other apps' notifications, and stores everything locally. There is **no personal/shop mode switch** — the app is shop-operator only (single dataset).

## Commands

```bash
./gradlew assembleDebug      # build debug APK (app/build/outputs/apk/debug/)
./gradlew test               # JVM unit tests (incl. Robolectric, `testDebugUnitTest` equivalent)
./gradlew testDebugUnitTest --tests "com.example.SlipDataParserTest"   # single test class
```

- CI (`.github/workflows/ci.yml`): JDK 17, Android SDK via `android-actions/setup-android@v3`, generates a `debug.keystore` with `keytool` (passwords `android`), then `./gradlew assembleDebug --no-daemon --stacktrace`. On push to `main`/`master` it also creates a GitHub release with the APK.
- Release signing reads `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` env vars (`app/build.gradle.kts`). Debug builds use root `debug.keystore` (gitignored). `isMinifyEnabled = false`.
- `gradle.properties` gotchas: `org.gradle.configuration-cache=true`, `kotlin.compiler.execution.strategy=in-process` (avoids Kotlin daemon connection errors), `googleServices.missing.passthrough=true` (no `google-services.json` in repo despite Firebase deps), max 4 workers.

## Architecture — read this first

There are **two parallel architectures**. The live app is NOT the one using Room/ViewModel.

### Live app (what actually runs)
`MainActivity` → `ui/MainApp.kt` (a single ~1200-line composable holding ALL app state). No DI, no ViewModel, no Navigation Compose.

- State = `remember { mutableStateOf(...) }` + `SharedPreferences("firecash_settings", MODE_PRIVATE)`. Slips and whitelists are persisted as JSON strings inside prefs, not in Room.
- Navigation = four booleans (`showSavedSlips`/`showCapture`/`showPayload`/`showAnalytics`) + `BackHandler`s. Homepage is Account (slip list).
- Screens: `ui/screens/AccountScreen.kt` (home, grouped list + balance card), `PhotoCaptureScreen.kt` (CameraX), `QrPayloadScreen.kt` (slip detail), `AnalyticsScreen.kt`, `SettingsScreen.kt`. All state flows down from MainApp via callbacks.
- Live data pipeline: `OcrProcessor` (ML Kit barcode scan) → raw QR payload string → `addSlip()` in MainApp → `SlipVerificationManager.verifyPayload()` → `SavedSlip` JSON → prefs → AccountScreen.

### Legacy/dead code (do not extend; may be deleted)
`ui/viewmodel/MainViewModel.kt`, `data/local/*` (Room `FireCashDatabase`, `ExpenseDao`, `KeywordRuleDao`), `data/model/Expense.kt` + `KeywordRule.kt`, `data/repository/*` (ExpenseRepository seeds 6 fake USD expenses + rules on empty DB), `data/export/ExportManager.kt`, `data/backup/DriveBackupManager.kt`, `data/easyslip/EasySlipClient.kt`. None of these are referenced from `MainApp`; `MainViewModel` is never instantiated. `Screen` sealed class in MainViewModel is not the real navigation. Don't "fix" or wire these up — replicate their behavior in MainApp instead if a feature needs it.

## Persistence — SharedPreferences keys (`firecash_settings`)

| Key | Content |
|---|---|
| `saved_slips` | JSON array of `SavedSlip` (the whole account list, incl. notification-caught slips) |
| `seen_payloads` | JSON array of already-processed QR payloads (dedupe) |
| `processed_files` | content:// URIs of tracked-folder files already OCR'd (dedupe) |
| `tracked_folders` | JSON array of folder tree URIs (persisted read permission) |
| `known_names` | JSON array of your name variants (Thai/English) |
| `notification_whitelist` / `notification_whitelist_expense` | JSON array of `{"package","prefix"}` for money-in / money-out |
| `preset_disabled_income` / `preset_disabled_expense` | StringSet of disabled preset keys (`pkg\|prefix`) |
| `notification_presets_seeded` | one-shot seed flag for presets |
| `notification_income_enabled`, `notification_expense_enabled` | master toggles |
| `easy_slip_enabled`, `verification_provider` (easyslip/thunder/slip2go), `api_key_easyslip`, `api_key_thunder`, `api_key_slip2go`, `check_duplicates` | verification settings |
| `background_listening` | foreground keepalive toggle |
| `app_mode` | `"personal"` (default) or `"shop"` — controls the home card's primary action |

Helper functions for slips/whitelists/seen/processed live as top-level `private fun`s at the bottom of `MainApp.kt` (e.g. `loadSlips`/`saveSlips`/`slipToJson`, `loadNotificationWhitelist`, `saveProcessedFiles`).

## Verification (SlipVerificationManager)

- Multi-provider: EasySlip (`https://api.easyslip.com/v2/verify/bank`, `Authorization: Bearer`), ThunderAPI (`api.thunder.in.th`, Bearer), Slip2Go (`https://connect.slip2go.com/api/verify-slip/qr-code/info` — **requires `Bearer` prefix on the secret**, raw secret returns 401001; note host is `connect.` not `api.`).
- No API key configured, or network failure, or unexpected HTTP code → falls back to `simulateSlipVerification()`: **payload ending in `9999` ⇒ `DUPLICATE_DETECTED`**, otherwise `UNVERIFIED` with a "not configured" message. This is also how the app behaves in demo mode, and tests rely on it.
- Rate limit info surfaced from `X-RateLimit-Remaining` header; 429 → `RATE_LIMITED`, 401 → `AUTH_FAILED`, 404 → `SLIP_NOT_FOUND`.
- `EasySlipClient.kt` is the older single-provider client used only by (dead) MainViewModel + its test. The live path is `SlipVerificationManager`.

## OCR / parsing

- `OcrProcessor.processReceipt(imageUri, samplePreset, scanCenterOnly=true)`: uses ML Kit **barcode scanning only** (QR). With `scanCenterOnly=true` it crops the image to the center 60% square to match the on-screen frame overlay. `samplePreset != null` bypasses the image entirely and parses a canned OCR string (used for demo/testing). `OcrProcessor.recognizeText` runs ML Kit Latin text recognition over the photo — used ONLY for the shop-mode fraud cross-check (extract the baht amount printed on the slip). Note: ML Kit on-device text recognition has **no Thai script support** (Latin/digits only), so Thai-only glyphs are not extracted.
- `SlipDataParser.parse(rawText)`: regex-based extraction — amounts (`TOTAL`, `ยอดรวม`, etc.), dates (`2023-10-24`, `dd/MM/yyyy`, `24 Oct 2023`), merchant (first plausible line, truncated to 32 chars), Tag 91 CRC (`9104XXXX`), EMVCo QR CRC (`6304XXXX`). Bank slip detection via CRC/Tag 91 or `PromptPay`/`โอนเงินสำเร็จ`. Falls back to hardcoded values (amount `45.20`, today's date) when nothing matches.
- Bank code mapping: KBank `004`, SCB `014`, Bangkok Bank `002`, KTB `006` (see `SlipDataParser.extractBankSlipPayload` and `SlipVerificationManager.getBankName`).

## Notification income/expense

- `IncomeNotificationService` (`NotificationListenerService`): reads title/text/bigText of every notification, skips own package, applies per-app whitelist prefix matching (`extractAfterPrefix`, e.g. prefix `โอนเงินให้คุณ ฿` → first number after it), saves a `SavedSlip` with payload `notif:<pkg>:<amount>:<hash>:<ts>`. Dedupe via `seen_payloads`-style hash set (`saved_slips` + hashes stored as `seen_payloads`-like set in prefs — see `PREFS_SEEN`).
- `NotificationPresets`: permanent preset whitelists (KBank/SCB packages) seeded ONCE into prefs (`seedIfNeeded`). Presets are **toggleable but not removable** — disabled state lives in `preset_disabled_*` StringSets; `mergeIncome`/`mergeExpense` rebuild the effective list. User-added entries are stored separately and are removable. `isIncomePermanent`/`isExpensePermanent` gate deletion in the UI. When adding a new bank app's notification prefix, add it to `incomePresets`/`expensePresets` (presets are seeded only if the user hasn't already customized the list).
- `BackgroundListenerService`: foreground `specialUse` service ("music-player style", `FLAG_NO_CLEAR|FLAG_ONGOING_EVENT`, `ACTION_STOP` action to stop) that keeps the notification listener alive; started/stopped from MainApp settings, auto-restarted on app launch when `background_listening` is true.

## Money in/out semantics

- Balance = moneyIn − moneyOut. Each slip resolves to in/out/transfer via `effectiveIsMoneyIn` (duplicated in `MainApp.isKnownName`-based logic and `AccountScreen`): receiver is a known name ⇒ income, sender is known ⇒ expense, both known (or sender==receiver) ⇒ transfer (excluded from balance), else the stored `isMoneyIn` flag. Known names are matched case-insensitively, trimmed.
- AccountScreen list: `LazyColumn(reverseLayout = true)`, slips appended in arrival order, grouped by the `date` string with a daily-net-total header. Newest is at the visual bottom.
- **App mode** — removed: the app is **shop-operator only**. The balance card's top-right 44dp action button always opens the camera.
- Deletion safety: only slips with `UNVERIFIED` status, blank `transRef`, or `null` amount can be deleted (long-press multi-select in AccountScreen, `onDeleteSlip` in MainApp).
- `fullResync()` (hold Sync 10s): clears `processed_files`, re-OCR's every tracked folder, then re-verifies all slips **except** `manual:`/`notif:` payloads. Settings → Tracked Folders also has **Force Sync All (re-scan every photo)** (`forceSyncTrackedFolders()`): same cache clear + re-scan but WITHOUT the re-verify loop; re-reading a photo updates the existing slip (dedupe by `photoPath`) instead of duplicating.
- Import/export (`exportAllData`/`importAllData`): full JSON clone including API keys, whitelists, and app mode. Import preserves permanent presets (merges with `mergeIncome`/`mergeExpense`) and a JSON missing a whitelist key does NOT wipe the device list.

## Gotchas

- **Package split**: `namespace = "com.example"` but `applicationId = "com.aistudio.firecash.qxrtv"` — use `com.example.R`, not the applicationId, in code. `MainActivity` etc. live in `com.example`.
- **Build tooling is bleeding-edge**: AGP `9.1.1`, Kotlin `2.2.10`, Gradle `9.3.1` wrapper, and `compileSdk { version = release(36) { minorApiLevel = 1 } }` — an unusual syntax that only works on this AGP. Don't "modernize" these casually.
- **Secrets plugin**: `secrets.propertiesFileName = ".env"` (`.env` is gitignored; `.env.example` has only a commented `GEMINI_API_KEY` placeholder). `FIREBASE_APPCHECK_DEBUG_TOKEN` is in the ignore list. If you add an API key for verification providers, it goes into prefs (settings), NOT `.env`.
- **Roborazzi/Robolectric are configured** (`libs.versions.toml`, `testOptions.isIncludeAndroidResources = true`) but no test uses Roborazzi (`captureRoboImage`); `app/src/test/screenshots/greeting.png` is a leftover. Robolectric tests need `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [36])`.
- **Tests**: JUnit4 + `runBlocking` (coroutines), plain JVM tests for pure logic (`SlipDataParserTest`, `EasySlipClientTest` mock pipeline, `AnalyticsEngineTest`, `ExportManagerTest`). `EasySlipClientTest.testMockVerificationPipeline` relies on the no-key simulation behavior.
- **Docs drift**: `PROJECT_STATUS.md` is now maintained (snapshot of current state; last updated 2026-08-31). `docs/DEVELOPMENT_LOG.md` has detailed history but also goes stale. `docs/firecash_ui_stitch_plan.md` defines the design tokens (`#121316` bg, `#FF6B00` primary, `#10B981`/`#6366F1` accents) mirrored in `ui/theme/Color.kt`.
- `package.json` contains only `opencode-ai` (harness used to build the app; `node_modules/` untracked). `snapui.zip` is a tracked artifact at repo root. `.github/workflows/proxy.yml` (EasySlip proxy Cloud Run deploy) is a stub — the `proxy/` directory does not exist and the gcloud command is commented out.
- Gradle: `RepositoriesMode.FAIL_ON_PROJECT_REPOS` (declare repos only in `settings.gradle.kts`); `android.nonTransitiveRClass=true`.
- Manual slips use payload prefix `manual:` and `MANUAL-` transRefs; the UI treats them as always-deletable. Notification slips (`notif:`) are also deletable only while unverified.
- **Shop-mode fraud cross-check** (`addSlip` + `SlipDataParser.extractQrAmount`): each scanned slip compares the baht amount read from the photo text (`extractSlipAmount`), the EMVCo QR tag-54 amount, and the bank-verified amount. Any disagreement >0.005 sets `SavedSlip.amountMismatch` (persisted in JSON) and shows a red "possible tampered slip" banner on the detail screen (`QrPayloadScreen.amountMismatch`).
- **Verification safety** (`verifyBatch` + `applyVerifiedUpdate`): batch verification (`resyncUnverifiedSlips`, `fullResync`) NEVER overwrites a slip's existing photo-extracted amount — a verified amount only fills a missing one, and any disagreement sets the `amountMismatch` fraud flag. `verifyBatch` also discards a whole batch when ≥2 different slips come back with one shared transRef (canned sandbox/test responses — EasySlip test keys return fixed data like 178.00 or 0.00 with identical transRefs). Note: a test key marks slips VERIFIED (making them undeletable per the deletion-safety rule); use a production key for real verification. Mode switching does NOT auto-verify (each mode's dataset is self-contained).
