# FireCash — Development Log (Day-by-Day)

> Honest, evidence-backed journal of how FireCash was built. Separated by actual calendar days from git history, plus the off-plan detour through Google AI Studio.

**Project:** FireCash — Receipt Logging & PromptPay/EMVCo Slip Verification (Jetpack Compose + Room + EasySlip + ML Kit + NotificationListener)  
**Repo:** `C:\Users\admin\Project\FireCash` • `namespace = com.example` • `applicationId = com.aistudio.firecash.qxrtv`  
**Period:** 2026-08-25 (night) → 2026-08-28 (intensive) • Today is `Fri Aug 28 2026` (UTC)  
**Model:** `opencode/muse-spark-1.2-contributor-free` via opencode harness from **Day 3 (2026-08-27)** (project kicked off **2026-08-25 night**)

---

## Overview

FireCash started as research-driven. **Days 1-2 were pure research via a separate `opencode` convo** — that convo produced the Stitch UI plan (`docs/firecash_ui_stitch_plan.md` with MD3 dark tokens `#121316`/`#FF6B00`/`#10B981`/`#6366F1`). That plan was fed to **Google Stitch** on **2026-08-26 night**, then an **unplanned Google AI Studio** spike was tried the same night and was **not satisfying (not in the original plan)** and abandoned; **after that we came to this `opencode + Muse Spark` convo with the Google Stitch design as input** on **Day 3 (2026-08-27)** and rebuilt/iterated the entire app in one long intensive session — from `Account Settings` scaffolding through to notification whitelists, reverse chat-style list, search, copy-to-clipboard, logo, navigation and homepage changes. All commits below are from `git log --reverse --date=short` (local time).

---

## Day 1 — 2026-08-25 Night → 2026-08-26 — Research (via first opencode convo)

**Goal:** Understand the problem space, not code — done **through a separate `opencode` convo** (project kickoff **2026-08-25 night**, before any git commits).

- Read Thai banking slip specifics: PromptPay/EMVCo Tag 91 `9104[A-Fa-f0-9]{4}` CRC, Bangkok Bank `002`, KBank `004`, SCB `014`, KTB `006`, etc.
- Surveyed EasySlip `POST /verify/bank`, duplicate detection, 180-day `SLIP_NOT_FOUND`, `RATE_LIMIT_EXCEEDED`.
- Compared ML Kit Text Recognition vs Tag 91 QR parsing, Room `Expense`/`KeywordRule` persistence, CSV/PDF streaming, Drive snapshot vs full sync.
- That first opencode convo **produced the full research docs and the UI plan for Google Stitch**: `docs/firecash_full_plan.md` (11-feature table), `docs/firecash_ai_studio_systems_plan.md` (flow: Camera → OCR → EasySlip → Room → Analytics/Export), **`docs/firecash_ui_stitch_plan.md` (MD3 dark tokens `#121316` / `#FF6B00` / `#10B981` / `#6366F1` — specifically authored as input for Stitch)**, `docs/ai_studio_prompts.md`, `README.md` (8 key features).
- No code beyond `Initial commit` + `Add all remaining project files` scaffolding.

**Commits 2026-08-27 (first code push, research started 25th night):**
- `2026-08-27 Initial commit`
- `2026-08-27 feat: add Account Settings page with multiple tracked folders and sync options` — introduced `AccountSettingsScreen` (persisted `trackedFolderUris` via `JSONArray` in `SharedPreferences`), `DocumentFile` folder picker.
- `2026-08-27 Add all remaining project files` — baseline `data/ocr`, `data/easyslip`, `data/local`, `ui/`, theme.

---

## Day 2 — 2026-08-26 Night — Stitch + Google AI Studio (Off-Plan)

**Night — Google Stitch then Google AI Studio (both off-plan, not satisfying):**
- Fed the `firecash_ui_stitch_plan.md` from the first opencode convo into **Google Stitch** to generate the initial UI stitch. Stitch produced the dark-theme shell, card spacing, `14-20dp` radii, and test tags (`search_expenses_input` etc.) as a starting point.
- Then tried to continue the Stitch output in **Google AI Studio** in the same night window for faster iteration. Hit prompt limits, non-deterministic scaffolding, and no access to local `Room`/`CameraX`/`ML Kit` toolchain; generated code didn't compile against `FireCashDatabase`/`OcrProcessor` contracts and styling drifted from the Stitch design. Validated `docs/account-settings-summary.md` in parallel but AI Studio was not satisfying.
- Decision: **abandon AI Studio**.

> **Pivot after 26th night:** Came to **this `opencode + Muse Spark` convo (`muse-spark-1.2-contributor-free`) with the Google Stitch design as input** — allowed `scaffold → recon → hunt → validate → report` discipline and real `bash` builds (`./gradlew assembleDebug`).

---

## Day 3 — 2026-08-27 — This Opencode Convo Intensive (Stitch Design as Input)

This is **Day 3** (2026-08-27) — the entire intensive `opencode + Muse Spark` session rebuilding from the Google Stitch design. Presented in chronological order as they happened (git timestamps: `2026-08-27` + `2026-08-28`).

> Note: `Stitch + AI Studio` moved to **2026-08-26 night** per user; the `opencode` intensive below is **Day 3 = 2026-08-27**.

- **feat: add save-to-account with money-in/out toggle on QR scan** — `MainApp` `savePayload(isMoneyIn)` + `QrPayloadScreen` toggle, `SavedSlip(isMoneyIn)` deduped by `transRef/payload`.
- **fix: auto-save scanned QR slip to account + make addSlip resilient** — `handlePayload` now `addSlip(payload)` immediately (`runCatching { verifyWithEasySlip }` so verification failure never blocks save). Scan → appears in Account instantly.
- **fix: Scaffold padding — last slip behind nav bar / top double push** — `Scaffold { paddingValues -> Box(padding(bottom=calculateBottomPadding())) }` (only bottom inset; top handled by `statusBarsPadding()` in screens). This fix re-appeared twice after navigation refactors (`9d8f837`, `9d8f837`).
- **feat: AnalyticsScreen with spending summary + AI insights** — new `ui/screens/AnalyticsScreen.kt` (`AnalyticsEngine.generateAnalytics` → `categorySpends/insights`), `AccountScreen` `View Spending Summary` button below balance card; `MainApp` `showAnalytics` branch.
- **Chart iterations:** `replace horizontal bar with vertical stick chart` → `style: thinner with circular caps + baseline` (`Canvas` `drawLine` 6dp round + dot) → `feat: time-based vertical stick chart with Day/Week/Month toggle` (`TimeBucket DAY/WEEK/MONTH`, `computeEntries` with `LocalDate`/`WeekFields`).


- **fix: background sync for Account page without blocking overlay** — split `syncTrackedFolder()` (foreground `isLoading` overlay) vs `syncTrackedFolderInBackground()` (`isBackgroundSyncing` inline `Syncing…` 14dp next to Transactions).
- **feat: exclude self-transfers from balance/analytics, show as Transfer** — `isSelfTransfer(sender==receiver)` → grey `SwapHoriz` `#9E9E9E`, `Transfer` label, `THB x` without `+/-`, excluded from `moneyIn/moneyOut`.
- **feat: known names in Settings for auto income/transfer detection** — `PREFS_KNOWN_NAMES` `JSONArray`, `SettingsScreen` **My Names** card, `MainApp.isKnownName()`; `addSlip` resolves `receiver known→Income, both known→Transfer`; `AccountScreen`/`AnalyticsScreen` `effectiveIsMoneyIn()` makes it retroactive.
- **feat: retroactive income detection using known names for existing slips** — `effectiveIsMoneyIn` used for `moneyIn/moneyOut` and row color, so adding a name instantly fixes old slips without migration.
- **refactor: remove manual transaction type toggle, use auto detection** + **refactor: remove Save to Account button** — `QrPayloadScreen` now read-only; type comes only from `knownNames`.
- **fix: unknown fallback for unverified slips + background resync when EasySlip enabled** — `EasySlipClient.simulateSlipVerification` no longer returns hard `Starbucks Thailand / Roasters 45.20`; now `UNVERIFIED` with `amount = extractAmount(payload)` or `null`; `MainApp.resyncUnverifiedSlips()` + Settings **Sync N unverified** button, auto-triggered on `onToggleEasySlip(true)`/`onUpdateApiKey`.
- **fix: limit QR scan to center frame (60% ROI)** — `PhotoCaptureScreen.kt:117` filters `barcodes.firstOrNull { bbox.center in [w*0.20..0.80, h*0.20..0.80] }` vs full `InputImage`.


- **feat: notification income detection scooping first number** — `service/IncomeNotificationService.kt` (`NotificationListenerService`, `AMOUNT_REGEX [-+]?\d{1,3}(,\d{3})*` → first number), `AndroidManifest.xml` `BIND_NOTIFICATION_LISTENER_SERVICE`, `firecash_settings` `PREFS_SLIPS/SEEN`, `MainApp.DisposableEffect` listener keeps `savedSlips` live.
- **feat: deletable only for unknown/invalid slips** + **feat: long-press multi-select to delete/manage** + **refactor: only multi-select delete** — `isDeletable = amount==null || UNVERIFIED` (`AccountScreen.kt:79`), `selectedKeys:Set<Long>`, `combinedClickable` `onLongClick`, `TopBar: ${n} selected + Delete + All`, per-row `Delete` removed, kept only multi; `DateHeader` still shows `• n`.
- **feat: whitelist for notification income (only whitelisted apps)** → **feat: whitelist with per-app prefix template `<Sender>/<Amount>`** → **refactor: prefix is simple string detection, amount is first number after it** → **feat: multiple prefixes per app in whitelist** (`IncomeNotificationService.loadWhitelist` migrates `String|{package,prefix}` → `WhitelistedApp`; `onNotificationPosted` tries each prefix for the package; `SettingsScreen` whitelist card with `whitelist_input`/`prefix_input`).
- **feat: add money-out notification whitelist mirroring money-in** — duplicated `PREFS_NOTIFICATION_EXPENSE/_EXPENSE_WHITELIST`, `saveExpenseFromNotification(isMoneyIn=false)`, `MainApp` `notificationExpenseEnabled/Whitelist`, `SettingsScreen` second **Notification Expense** card (red accent, `notification_expense_switch`).
- **fix: notification switch layout inside card (weight)** — inner `Row(weight=1f)` so `Switch` never overflows `RoundedCornerShape 16dp` card.
- **fix: slip details always visible offline** — `EasySlipClient` `extractAmount()` fallback + `MainApp.addSlip` fallback `VerifySlipResponse(UNVERIFIED, amount=extractAmount)` + `QrPayloadScreen` fallback card; `AccountScreen.onSlipClick` builds fallback `VerifySlipResponse` from `SavedSlip` so old slips show details.
- **fix: slip details card always visible even for unverified/old slips** — `QrPayloadScreen.kt:103` always renders card (uses `extractAmount(payload)` if `slipData==null`).


- **feat: set custom launcher icon from provided flame mascot (adaptive + legacy mipmap)** — generated `drawable/firecash_icon.png` (1024) via `System.Drawing`, `drawable/ic_launcher_background.xml #121316`, `mipmap-*/ic_launcher*.png` 48/72/96/144/192 + `ic_launcher_foreground.png`; `mipmap-anydpi-v26` adaptive XML.
- **feat: move camera to FAB on Account with scroll-hide** → **refactor: persistent camera button inside balance card at right side** → **refactor: move camera button to top-right of balance card** — `AccountScreen` balance card `Row(SpaceBetween, Top)` with `Column(Current Balance + THB 32sp Bold)` left and `44dp` circular `PhotoCamera` `IconButton(white 12% bg)` right; FAB with `rememberLazyListState` + `snapshotFlow` + `AnimatedVisibility(fade+scale)` removed after user feedback.
- **refactor: merge AccountSettings and System Settings into unified Settings** — deleted `AccountSettingsScreen.kt` (273 lines), moved **Tracked Folders** card (folder picker `OpenDocumentTree`, `syncTrackedFolder`, `importSlips`, `isLoading` spinner) into `SettingsScreen` (now 1294 lines), `MainApp` removed `showAccountSettings`, `Scaffold bottomBar` → `if (!showPayload)`, `AccountScreen.onOpenSettings` → unified `Settings`.
- **User logo detour:** `chore: update launcher logo via Android Studio` (`88f49ac`, `ic_launcher-playstore.png` + `mipmap-*/ic_launcher*.webp`), `chore: update launcher assets after rebuild` (`8112e8b`), `chore: use App Logo (1).png` (`1537e8e` 44KB), then **Do not touch app logo** → `Revert "chore: use App Logo (1).png"` (`03adeb8`) + `fix: restore cute mascot for in-app title (drawable only, launcher untouched)` (`e286ac6` 44KB), then **make cat bigger than title** → `style: make cat logo 56dp larger than FireCash title` (`0ffff08`, `48dp→56dp`, `headlineSmall→headlineMedium 26sp ExtraBold`).
- **refactor: remove bottom nav, add top bar navigation on camera** (`7c364a9`) — deleted `CaptureBottomBar` from `Scaffold`, `PhotoCaptureScreen` top `Row(SpaceBetween)` with left `AccountBalanceWallet` (`onNavigateToAccount`) and right `Settings`.
- **feat: make Account page the homepage** (`c82270f`) — `showSavedSlips=true` by default, `QrPayload.onBack`/`savePayload`/`Settings.onBack` → Account.
- **feat: system back gesture mirrors in-app navigation** (`709cf38`) — `BackHandler` in `MainApp` (`showPayload→Account`, `showAnalytics→Account`, `showCapture→Account`, `Settings→Account`) + `AccountScreen` `BackHandler(isSelectionMode) → clear selection`.
- **fix: prevent top double padding after removing bottom bar** (`9d8f837`) — back to `padding(bottom=calculateBottomPadding())`.


- **feat: reverse layout for slips list (bottom to top)** (`4cc5050` `LazyColumn(reverseLayout=true)`) + **fix: keep reversed slips list anchored at top** (`6e2d897` `fillMaxWidth` wrap) — newest at bottom, scroll up for older.
- **fix: date header on top of day group with reverse layout** (`3dc91f7`) — emit `items` before `item(header)` so header visually sits above its day's slips; also **feat: show daily net total in date header** (`c4c0cfe` `DateHeader(date,count,total:Double)` `THB %.2f • n` green/red).
- **feat: show slip time above amount in transaction row** (`892f041`) — `Column(End)` with `11sp` `slip.time` + `2dp Spacer` + `15sp SemiBold` amount.
- **refactor: replace back+My Account with FireCash logo+title** (`f7b277b`) — `AccountScreen` top bar `Image(R.drawable.firecash_icon, 32dp Circle)` + `FireCash headlineSmall Bold` (later enlarged to 56dp/`26sp ExtraBold`).
- **Logo swap again:** user supplied `App Logo (1).png`/`(2).png` in `Downloads`; we copied `(1)` to `drawable/firecash_icon.png` and regenerated `mipmap-*/ic_launcher*.png` + `foreground.png` + removed `webp` duplicates, then reverted launcher to Studio version on request.
- **feat: search slips by date/title/amount/exact QR payload on Transactions header** (`b800aeb`) — `Row(SpaceBetween)` `Transactions` + `Search/Close` icon, `OutlinedTextField` (`RoundedCornerShape 10dp`), `filteredSlips = remember(slips, searchQuery)` with `date/title/amount contains` + `payload==q` exact.
- **fix: require exact match for QR payload and ref in slip search** (`ee40096` `transRef==q` exact, payload already exact).
- **feat: tap detail row to copy, shows green Copied feedback** (`6b02680`) + **feat: tap QR payload to copy** (`1ab7099`) — `DetailRow` `clickable` + `ClipboardManager` + `copied` 1200ms delay → `Copied` `0xFF66BB6A` green.
- **style: make back control a pill button with Back label** (`b423617`) — `QrPayloadScreen` `IconButton(ArrowRightAlt)` → `Button(RoundedCornerShape 12dp, BorderStroke 1dp Gray 30%, FireCashSurfaceContainerLow, 14×10dp)` with `ArrowBack 18dp + Back 14sp`.

---

## Day 3 (Continued) — 2026-08-27 → 2026-08-28 — CameraX, Permissions & OCR Wiring

> Same `opencode + Muse Spark` session (Day 3). Focused on adding a working CameraX preview, runtime permission handling, and wiring the captured image to the OCR pipeline.

### CameraX Integration & Runtime Permission

- **feat: add CameraX preview to CaptureScreen** — `CaptureScreen.kt` now uses `PreviewView` + `ImageCapture` use cases bound to `ProcessCameraProvider`. Replaced the old placeholder with a real live camera feed.
- **feat: runtime permission via Activity Result API** — Removed Accompanist `rememberPermissionState` dependency (version mismatch caused compile errors). Replaced with `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` + `mutableStateOf` flag. Permission is auto-requested on first composition; camera only starts after grant.
- **fix: add `android.permission.CAMERA` to AndroidManifest.xml** — Required for the permission prompt. Added between `RECEIVE_BOOT_COMPLETED` and `BIND_NOTIFICATION_LISTENER_SERVICE`.
- **fix: update CaptureScreen call site in MainApp.kt** — Lambda signature changed from `(SourceType, SampleSlipPreset?) -> Unit` to `(SourceType, SampleSlipPreset?, String?) -> Unit` to carry the captured image path.

### OCR Pipeline Wiring

- **feat: pass captured image path through to OCR** — Shutter button now calls `onCapture(SourceType.CAMERA, null, photoFile.absolutePath)` instead of `onCapture(SourceType.CAMERA, null)`. Image saved to `cacheDir/capture_<timestamp>.jpg`.
- **feat: update MainViewModel.startVerification signature** — Added optional `imageUri: String? = null` parameter. Passes it to `ocrProcessor.processReceipt(imageUri, samplePreset)`.
- **fix: all CaptureScreen UI triggers updated** — Preset buttons, gallery picker, PDF upload all pass `null` as the third argument. Shutter button passes `photoFile.absolutePath`.
- **Issue found: OcrProcessor ignores imageUri** — `OcrProcessor.processReceipt` still returns hard-coded Uber `$45.20` text when `samplePreset == null`. No real OCR (ML Kit) is integrated yet. This is the current blocker.

### Gradle & Build Fixes

- **fix: removed google-services plugin** — `google-services.json` was a placeholder; plugin removed from `app/build.gradle`.
- **fix: added gradle wrapper** — Copied `gradle-wrapper.jar` from another project, created `gradlew.bat` and `gradlew` scripts, cached `gradle-9.3.1-bin.zip` in `~/.gradle/wrapper/dists/`.
- **fix: local.properties set sdk.dir** — Points to `C:\Users\admin\AppData\Local\Android\Sdk`.
- **fix: added file_paths.xml** — Created `res/xml/file_paths.xml` for `FileProvider` configuration.
- **fix: updated AndroidManifest for FileProvider** — Added `tools:replace="android:authorities"` and `tools:node="replace"` to the provider declaration.
- **fix: added gradle.properties** — `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx2048m`.

### Project Status Documentation

- **docs: created PROJECT_STATUS.md** — Comprehensive status file with current state table, root cause analysis of OCR issue, and next steps checklist. Written via PowerShell `Set-Content` as direct tool calls were unavailable.

### Current Blocker

The OCR pipeline is wired end-to-end (camera → file → ViewModel → OcrProcessor) but `OcrProcessor.kt` never actually reads the image from disk. It returns placeholder Uber text regardless of what photo is captured. To fix:

1. Add ML Kit `text-recognition` dependency.
2. Update `OcrProcessor.processReceipt` to decode the bitmap from `imageUri` and run `TextRecognizer`.
3. Feed OCR output to `SlipDataParser.parse()`.

### Files Modified This Session

| File | Change |
|------|--------|
| `app/src/main/java/com/example/ui/screens/CaptureScreen.kt` | CameraX preview, permission handling, image path passing |
| `app/src/main/java/com/example/ui/MainApp.kt` | Updated `onCapture` lambda to pass `imagePath` |
| `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` | Added `imageUri` param to `startVerification` |
| `app/src/main/AndroidManifest.xml` | Added `CAMERA` permission |
| `app/src/main/res/xml/file_paths.xml` | Created for FileProvider |
| `gradle.properties` | Created with AndroidX + JVM args |
| `local.properties` | Set `sdk.dir` |
| `gradle/libs.versions.toml` | CameraX + Accompanist versions (unchanged) |
| `PROJECT_STATUS.md` | Created with status summary |
| `docs/DEVELOPMENT_LOG.md` | This entry |

---

### Current State (2026-08-31)

**Build:** `BUILD SUCCESSFUL`. CI (`.github/workflows/ci.yml`) builds `assembleDebug` on the runner (auto-generated `debug.keystore`, `setup-android@v3`), uploads the APK artifact and auto-creates a **GitHub release** on every `main` push.

**Homepage:** `AccountScreen` — balance card (56dp cat + `FireCash 26sp`, Money In/Out + camera shortcut, Personal/Shop mode dependent action), `View Spending Summary`, searchable Transactions (reversed daily groups with net totals), long-press multi-select delete (unverified/no-ref slips), time above amount, `Transfer` chips.

**Analytics:** stat cards + monthly **donut pie** (income/spending, Net in center, legend with %) + **Compare** mode splitting the pie into equal sectors per month (up to 3 months from actual slip data).

**Verification:** **SlipVerificationManager** with three providers — EasySlip / ThunderAPI / Slip2Go — selected via chips in Settings, per-provider API key, offline fallback, 401/404/429 mapped to statuses. Slip2Go uses `connect.slip2go.com` + `Bearer <secret>`.

**Settings:** Safe (My Names, Tracked Folders, Keyword Mapping, Personal/Shop mode) / **Dangerous** collapsible red section (Slip Verification, Notification Income/Expense whitelists with **permanent toggleable presets**, Background & Battery, Data Transfer).

**Not yet implemented (vs `firecash_full_plan.md`):** `Screen.Onboarding`, `Screen.Export`/`BackupRestore` routes, keyword `rules` persistence (`MainApp` `emptyList()`), `BottomNavBar` dead code after `7c364a9`. Google Drive removed by request (`19eb207`). Unit tests skipped in CI by request.

**Next steps you mentioned:** keep launcher untouched, potentially revisit `Onboarding`, `Export` unlimited, verification rate-limit UX.

---

## Day 4 — Sync Caching, Photos, Transfer & Settings Overhaul

> Post-"Current State" iteration in the same `opencode + Muse Spark` session (2026-08-28 late). All commits below.

### Performance & Sync

- **feat: cache processed folder files, sync only new slips on open** (`0c99bd9`) — new `PREFS_PROCESSED_FILES` cache (`processedFiles: Set<String>` of `content://` URIs); `scanFolder()` skips already-processed files (no re-OCR/verify on every open), marks each processed even if OCR blank.
- **feat: sync button on slip list — tap = new photos, hold 10s = full resync** (`fce790a`) — `Sync` icon next to `+`/search in Transactions header; `awaitEachGesture` + `withTimeoutOrNull(10_000)` → tap = `syncTrackedFolderInBackground()`; 10s hold = `fullResync()` (clears `processedFiles` → re-OCRs every photo → re-verifies **all** real slips via EasySlip, skipping `manual:`/`notif:` payloads).
- **feat: show syncing spinner beside Transactions on manual sync tap** (`5095889`) — new `isUserSyncing` state; 14dp `CircularProgressIndicator` + `"Syncing…"` (vs `"Auto-sync…"` for background) next to the Transactions title.

### Slip Photos

- **feat: link each slip to its actual photo on device** (`6835ca7`) — `SavedSlip.photoPath: String?` (persisted in JSON); capture/gallery/import/sync thread the image path through `handlePayload → addSlip(photoPath)`; tracked folders store the original `content://` URI; dedupe keeps existing photo on re-add. `QrPayloadScreen` **PhotoSection**: Coil `AsyncImage` thumbnail + **Open photo on device** (`ACTION_VIEW` via `FileProvider` `${applicationId}.fileprovider`); `res/xml/file_paths.xml` + provider added to manifest.
- **fix: persist captured/picked slip photos** (`e83d547`) — camera shutter, gallery picker, and Settings import now save into `getExternalFilesDir(DIRECTORY_PICTURES)` (persistent) instead of `cacheDir` so the photo link survives.

### Manual Entry & Background Survival

- **feat: manual income/expense entry from Account page** (`aad249a`) — `+` button in Transactions header → **Add Transaction** dialog (Money In/Out pills, THB amount decimal field, optional note); `MainApp.addManualSlip` creates `manual:<ts>` slips (`MANUAL-<ts>` ref, `UNVERIFIED`, note as sender/receiver); `resyncUnverifiedSlips` now skips `manual:`/`notif:` payloads.
- **feat: request disabling battery optimization for background notification catching** (`f37cc38`) — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission; **Background & Battery** card with status + button; status refreshed via `LifecycleEventObserver` on `ON_RESUME`.
- **style: hide battery optimization button when already granted** (`7016397`).
- **feat: music-player style foreground service** (`9fc0098`) — `BackgroundListenerService` (`specialUse` FGS): silent `IMPORTANCE_LOW` ongoing notification *"FireCash listening"* with tap-to-open + **Stop** action, `START_STICKY`; `Keep listening in background` toggle in Settings (requests `POST_NOTIFICATIONS` on 13+), auto-restart on launch via `isListenerRunning`.
- **feat: make background listener notification explicitly non-dismissible** (`cf93877`) — `FLAG_NO_CLEAR | FLAG_ONGOING_EVENT` raw flags; cannot be swiped/cleared while running.

### Settings Overhaul

- **refactor: remove Google Drive backup option from settings** (`19eb207`) — deleted Drive card + dead `googleDriveSync`/`onToggleDriveSync`/`onNavigateToBackup` params (DriveBackupManager stays unreferenced in `data/`).
- **refactor: organize settings into Safe and Dangerous categories** (`7b04431`) — Safe (green title): Base Currency, My Names, Tracked Folders, Keyword Mapping; **Dangerous** (red title): EasySlip, Notification Income/Expense, Background & Battery.
- **feat: put all dangerous settings in a collapsible toggle menu** (`8baa784`) — Dangerous header is a tappable row (chevron + `tap to expand/collapse`), `AnimatedVisibility(expandVertically/shrinkVertically + fade)` wraps all dangerous cards.
- **style: red background behind dangerous settings section** (`226f3b9`) — 10% `#EF5350` tinted container behind the cards (cards keep their own surface).
- **feat: hide notification access button when access already granted** (`47e4906`) — `notificationAccessGranted` (via `IncomeNotificationService.hasPermission`) refreshed on `ON_RESUME`; both Income/Expense cards show green *"Notification access granted"* instead of the button.
- **feat: JSON export/import for phone-to-phone data transfer** (`f56ec1d`) — `exportAllData()` writes slips + seen/processed sets + folders + names + whitelists + settings (incl. API key) to `FireCash_Backup_<ts>.json`, shared via `ACTION_SEND`; `importAllData()` reads the JSON, replaces all state, restarts listener if enabled; **Data Transfer** card with Export/Import buttons (system `OpenDocument` picker).
- **refactor: move Data Transfer card into Dangerous section** (`3c56586`).
- **docs: rewrite README to match actual app features** (`2edbdd6`).

---

## Day 5 — Analytics Chart Wars, Multi-Provider Verification, Notification Presets & CI

> 2026-08-29 → 2026-08-31. Analytics debugging was done **live against the connected device** (`adb` + `uiautomator` + `logcat`); the Slip2Go fix was found by mining their dashboard JS bundle. All commits below, newest last.

### Analytics charts (bar → pie → compare)

- **feat: dual income/expense stick chart with gridlines, value labels, legend** (`fad3bb1`) — every bucket now plots Money In (green `#66BB6A`) + Money Out (orange `#FF6B00`) side by side; 4 gridlines with compact amounts (`1.2k`), max-bar value labels, legend dots; fixed Month sorting bug (was alphabetical).
- **feat: limit week chart to 6 weeks per window** (`7144a32`); **style: make chart bars thick** (`c2c5932`) — sticks fill half the column; **style: cap bar width at 10dp** (`0c7318c`).
- **feat: replace bar chart with monthly income/expense donut pie chart** (`79e4a73`) — single month donut (green in / orange out), Net in the center, legend with amounts + %, month label in header; deleted all windowed bar-chart code (~240 lines).
- **fix: render pie chart as a true circle** (`15b1afd`) — arc sized to `min(w,h)` and centered.
- **feat: monthly expense comparison stick chart** (`31caac9`) then **reverted** (`f9c941f`) — user didn't want it.
- **feat: pie chart month comparison via concentric rings** (`e43e2c8`) — Compare button + dialog listing last 12 calendar months; rings dimmed by depth.
- **feat: split pie into equal sectors per month, limit comparison to 3 months** (`fb0e449`) — 2 months = halves, 3 = thirds; each sector = that month's in/out ratio; dialog limited to 3 months.
- **fix: unify pie ring size, offer only months present in slip data** (`eef2a43`) — compare mode now uses the same 36dp ring geometry as default; dialog months now derived from actual slip dates (max 3).
- **fix: crash when slips have blank dates** (`22c7a5e`) — filter + `runCatching` around month-key parsing.
- **fix: default pie to newest month with data, empty-state fallbacks** (`ca1e17c`).
- **fix: stamp scan date on unverified slips, treat undated slips as today in analytics** (`e7beabf`) — `addSlip` falls back to now when `transDate` null; analytics treats blank dates as today.
- **fix: parse slips in any date format** (`9ac5f5e`) — `normalizeDate()` tries `yyyy-MM-dd`/`dd/MM/yyyy`/etc.; `splitDate` hardened for space-separated datetimes.
- **fix: analytics month parsing used LocalDate with yyyy-MM formatter, silently dropping every month** (`27242b5`) — **the real root cause** of "no chart": `LocalDate.parse("2026-08-01", "yyyy-MM")` throws on the trailing `-01`; every month key was dropped by `mapNotNull`. Fixed with `YearMonth.parse(key, "yyyy-MM")`. Verified live on device (pie: `140.0/754.0` matching the balance card).
- **docs: create feature summary chart** (`88f3927`).

### Settings & deletion

- **refactor: remove Base Currency option from settings** (`8b2f33c`) — card, dropdown, `currentCurrency`/`onCurrencyChange` params deleted.
- **feat: allow deleting slips that are unverified or have no transaction reference** (`03b4139`) — `isDeletable` = `UNVERIFIED || transRef.isBlank() || amount == null`; dialog copy updated.

### CI / GitHub Actions

- **ci: generate debug keystore on CI, build debug APK with Android SDK setup** (`009f668`) — `debug.keystore` is gitignored but `assembleDebug` requires it → `keytool` step creates it on the runner; added `android-actions/setup-android@v3`.
- **test: remove stale GreetingScreenshotTest referencing removed ExpenseItemCard** (`8d85ae4`) — this was breaking `testDebugUnitTest` compilation on CI.
- **ci: skip unit tests, build and upload debug APK only** (`9fb1101`) — user request.
- **ci: auto-create GitHub release with debug APK on main pushes** (`1f8ff1e`) → **ci: publish releases as full releases, not prereleases** (`19319f3`) — `softprops/action-gh-release@v2`, tag `debug-build-<run_number>`, `contents: write`.

### Multi-provider slip verification (NEW FILES)

- **feat: multi-provider slip verification (EasySlip, ThunderAPI, Slip2Go)** (`23164f7`) — new `data/verification/VerificationProvider.kt` (enum: `easyslip`/`thunder`/`slip2go`) and `data/verification/SlipVerificationManager.kt` (one `verifyPayload(payload, checkDuplicate, matchAmount)` dispatching per provider; 429/401/404 → `RATE_LIMITED`/`AUTH_FAILED`/`SLIP_NOT_FOUND`, offline → simulate fallback). Thunder: `api.thunder.in.th/v2/verify/bank`, `Bearer` key, `bank.short` codes mapped to names. Slip2Go: `connect.slip2go.com/api/verify-slip/qr-code/info` (initially wrong host, see below), `Authorization: Bearer <secret>`, nested `payload.qrCode` body, `code 200000` = success, `200501` dup / `200404` not found / `429000` rate-limit. Settings now has **provider chips** (EasySlip | ThunderAPI | Slip2Go) + per-provider API key; storage `verification_provider` + `api_key_easyslip/thunder/slip2go` (legacy `api_key` migrated); export/import carries provider + keys.
- **fix: Slip2Go API host is connect.slip2go.com** (`8ce7a68`) — docs only show relative paths; `api.slip2go.com` 404s every documented route. Found by extracting endpoint strings from `app.slip2go.com/assets/index-*.js` (`https://connect.slip2go.com${endpoint}`). Confirmed live: correct host returns `401 401001` for a bad key.
- **fix: Slip2Go requires Bearer prefix on secret** (`ce635a4`) — docs say raw `Authorization: <secret>`, but the live API rejects raw with `401001` and accepts `Bearer <secret>` (verified by replaying the user's key from the dev machine). Debugged end-to-end through the phone (temp logcat diagnostics: request URL/authLen/response), after which "Sync 33 unverified" succeeded live: `code 200000 Slip found` with real bank data, slips flipping UNVERIFIED → VERIFIED.

### Notification whitelist presets (NEW FILE)

- **feat: notification whitelist presets system** (`dd86a10`) — new `service/NotificationPresets.kt`: `incomePresets`/`expensePresets` lists of `WhitelistedApp(package, prefix)`; `seedIfNeeded(prefs)` seeds once on first launch and only when the whitelist pref doesn't exist (never overwrites user data); wired into MainApp startup.
- **feat: preset notification whitelists from existing device config** (`7cd99a4`) — the user's real device lists pulled via adb UI dump and added as the example presets: MAKE by KBank income `โอนเงินให้คุณ ฿`, expense `โอนเงินสำเร็จ ฿`; later expanded by the user with KPlus/wap/SCB entries.
- **feat: permanent notification whitelist presets (locked, cannot be removed)** (`a46ea80`) — `mergeIncome/mergeExpense` always re-inject presets; remove handlers guard permanent entries; Settings shows a lock icon instead of ✕; the background `IncomeNotificationService` merges presets at match time.
- **fix: JSON import preserves permanent presets; missing whitelist in JSON no longer wipes device list** (`ee1a1ad`) — import only touches whitelist keys when present in the JSON, and merges presets into imported lists.
- **feat: preset whitelist entries are toggleable (persisted), still not removable** (`5fb1bfe`) — per-entry enabled state in `preset_disabled_income`/`preset_disabled_expense` StringSets; Settings shows lock + Switch for presets; `merge*` filters disabled ones everywhere (UI state, service matching, import).
- **fix: disabled preset toggles stay visible in the whitelist list** (`9b18984`) — Settings now displays ALL presets (enabled + disabled) via `displayIncomeWhitelist`/`displayExpenseWhitelist` (`(effective + permanent).distinctBy { pkg to prefix }`); toggling off no longer makes the row disappear.

### App modes (other session)

- **feat: add Personal and Shop app modes with different home card actions** (`0294bbf`, generated with Crush) — `app_mode` pref (`personal`/`shop`); Personal shows manual-entry button on the home balance card, Shop keeps the camera button; chosen in Settings, carried through JSON export/import; also added root `AGENTS.md` + `.gitignore` entries.

### New files this session

- `app/src/main/java/com/example/data/verification/VerificationProvider.kt`
- `app/src/main/java/com/example/data/verification/SlipVerificationManager.kt`
- `app/src/main/java/com/example/service/NotificationPresets.kt`
- `docs/SUMMARY_CHART.md` (feature summary table)

---

*Generated from `git log --reverse --date=short` + `docs/*.md` + `app/src/main/java` on `2026-08-31`. Run `git log --oneline` to replay.*

---

## Day 6 - 2026-08-31 - Personal Mode, OCR Extraction, Fraud Detection & Shop-Only Refactor

> Long session focusing on the OCR text path, fraud detection, and ultimately narrowing the app to shop-operator only. All verified live on the connected device via adb + uiautomator + logcat + FireCashOCR tag.

### Personal Mode (added, then removed)

- **feat: personal mode text extraction from slip photos** (2577f27 -> 9e4acb5 -> c996663) - camera/gallery/import/sync flow in Personal mode reads the photo text with ML Kit Latin recognizer (recognizeText), parses amount/date/merchant, and saves the slip directly without any API call. SlipDataParser.extractParties (from/to lines) determined counterparty. Later removed when the app became shop-only.
- **fix: show scanning indicator during personal-mode OCR** (2577f27)
- **feat: brand detection for slip titles** (c996663) - recognized text scanned for truemoney, kbank, scb, bangkok bank to set a readable title instead of garbled Thai glyphs.

### Thai Date and Amount Fixes

- **feat: parse Thai month abbreviations and Buddhist Era years** (7d4a703) - all 12 Thai month abbreviations and BE-CE conversion (subtract 543). 31 X. 2569 -> 2026-08-31.
- **fix: fall back to photo file timestamp when OCR date is unreadable** (6466396) - ML Kit garbles Thai dates. When the parsed date falls back to today, the slip photo's lastModified time is used instead. Verified on-device: KBank slips from different days now group under the correct date.
- **fix: only trust parsed amount when text has a decimal** (c996663) - prevents the parser's 45.20 fallback from inventing fake amounts.

### Force Sync All

- **feat: Force Sync All button in Settings** (3dd5273) - clears the processed_files cache and re-scans every tracked-folder photo. Re-scanning updates the existing slip (dedupe by photoPath) instead of duplicating.

### Shop-Mode Fraud Detection

- **feat: cross-check slip photo text amount against QR/bank amount** (1a14d7c) - compares three amount sources: photo text baht amount, EMVCo QR tag-54, and bank-verified amount. Disagreement sets SavedSlip.amountMismatch and shows a red possible tampered slip banner.
- **fix: never let batch verification overwrite photo-extracted amounts** (abd7f55) - verifyBatch detects canned sandbox responses (same transRef across multiple slips) and discards the batch; applyVerifiedUpdate keeps the existing amount, only fills a missing one, and sets the fraud flag on disagreement.

### Dataset Separation (added, then removed)

- **feat: personal and shop keep separate datasets** (db6c41f) - mode-scoped prefs keys, dataset swap on mode switch, both modes exported/imported, notification listener writes to the active mode's key. Removed when the app dropped the mode switch.

### Shop-Only Refactor

- **refactor: make FireCash shop-operator only** (5e44c9f) - removed the App Mode setting card, all personal-mode paths, the dataset split, and the showVerification parameter. The home card always opens the camera, slips always go through QR + verification with the photo-text fraud cross-check.

### Shop-Mode Fraud Detection (continued)

- **feat: cross-check the slip photo date against the bank-verified date** (a574d47) - the camera now OCRs the full photo (scanCenterOnly=false) for the fraud cross-check. A new public extractSlipDate() returns null when no date is readable instead of silently falling back to today, so the date comparison only fires when the slip actually has a parseable date. The dateMismatch flag is persisted through export/import and shown as a red banner on the detail screen alongside the existing amount-mismatch banner.


### Slip Document Detection & Perspective Flattening

- **feat: flatten the slip photo before OCR/QR decode** (OpenCV) - new data/ocr/SlipDocumentDetector.kt + org.opencv:opencv:4.10.0. Every captured/picked/imported/folder slip photo is checked for a flat 4-corner document region (grayscale -> GaussianBlur -> Canny -> dilate -> RETR_EXTERNAL contours -> approxPolyDP epsilon sweep). When found, the region is perspective-warped (getPerspectiveTransform + warpPerspective) into a flat copy (OcrProcessor.flattenedCopy), and QR + text recognition run on THAT. Verified on-device with a rotated real KBank slip: raw OCR merged lines (Promp | l...), flattened OCR isolated them (Prompt | Pay, 60.00 un).
- **feat: live slip-detection hint in the camera preview** - the CameraX analyzer samples the Y plane every 4th frame (step 3, straight into OpenCV via SlipDocumentDetector.detectYPlane, no RGB bitmap allocation). When a flat slip is in view the center frame turns green with a "Slip detected - tap shutter" hint.

- **feat: disable live QR auto-scan** - the CameraX analyzer no longer runs ML Kit barcode scanning on every preview frame. The flow is now intentional: frame the slip (green hint when a flat surface is detected), tap the shutter, and the QR is decoded from the captured (flattened) photo. Removed the onQrDetected callback wiring.

- **feat: draw the live slip frame around the detected slip on the preview** - the camera preview now draws a green 4-corner outline right around the slip, not just a fixed center box. The analyzer reports the detected quad rotated into display pixels; the overlay maps it through the same FILL_CENTER crop the PreviewView uses (scale = max(view/rotW, view/rotH), centered) so the frame hugs the slip as the user moves the phone. A faint white box + "Align the slip within the view" shows while nothing is detected.

### On-Device Tools Developed

- **OcrProcessorTextTest (androidTest)** - instrumented test running the real ML Kit pipeline against slip photos pulled from the device via adb pull.
- **adb-driven UI verification** - PowerShell scripts for dumping uiautomator hierarchy, finding element bounds, and tapping through the full camera to import to verify flow. FireCashOCR log tag diagnostics traced every step. powershell.exe + System.Drawing generated doctored slip images for fraud-detection E2E tests.

---

## Day 7 - 2026-09-03 - The Crop Is the Slip Photo

> The flattened slip region now REPLACES the full camera frame as the stored slip photograph. Before, flattening only fed OCR/QR; the slip kept pointing at the whole frame. Now the crop is persisted, becomes `photoPath`, and the full frame is deleted after the slip is saved.

- **feat: persist the flattened crop under the app Pictures dir instead of cache** - `OcrProcessor.flattenedCopy()` now writes `getExternalFilesDir(DIRECTORY_PICTURES)/<base>_flat.jpg` (falling back to `filesDir`), not `cacheDir/flat_<ts>.jpg`. The file name is deterministic: derived from the source photo's name (sanitized), so re-processing the same photo overwrites one crop file instead of piling up timestamped copies.
- **feat: store the crop as the slip's photo on every ingest path** - camera shutter, gallery pick, Import Slip Photos, and tracked-folder Force Sync all pass the flattened path to `addSlip(photoPath = …)` (falling back to the original when no slip region is detected: full-frame file for camera/picker/import, content:// URI for tracked-folder photos). Force Sync of a folder photo whose payload is already known *upgrades* the existing slip's photoPath to the crop.
- **feat: delete the full frame once the crop is saved** - camera/gallery/import delete their full-frame JPEG after `addSlip` persists (slip survives via the crop). When flattening found nothing or OCR/QR found no payload, the crop is deleted and the full frame kept — a failed scan never destroys the photo. Tracked-folder originals are never touched (they live in the user's photo folder); the temp cache copy is cleaned up after processing.
- **test: crop persistence assertions in OcrProcessorTextTest** - instrumented test now asserts the crop file exists under `Pictures/` (not cache) and that a second `flattenedCopy()` returns the same deterministic path. Verified on device: `ReceiveMoney_QR_…_flat.jpg` created during a Force Sync All, its slip's photoPath pointing at the crop, no duplicate slips created.

- **on-device verification** - adb Export Data → JSON pull: 43 slips, one new crop-referencing slip, no new duplicates (the single dup pair pre-dates this build). No FATAL exceptions.

---

## Day 7b - 2026-09-03 - Making Slip Flattening Reliable

> "The flattening sometimes works and sometimes doesn't." Two root causes fixed: (1) JPEG EXIF rotation was ignored, so portrait captures were handed to OpenCV/ML Kit sideways; (2) the detector only tried one fixed Canny threshold and demanded the outline simplify to exactly 4 corners, with no fallback. Both made detection depend on luck (phone orientation, lighting, outline noise).

- **fix: decode photos with their EXIF rotation applied** - `OcrProcessor` gained `decodeRotated()`, used by `flattenedCopy`, `recognizeText`, and `processReceipt`. Camera JPEGs store orientation in EXIF metadata (`BitmapFactory.decodeFile` ignores it), so portrait captures were analyzed rotated 90°, which broke text-OCR and made flatten results depend on how the phone was held. Verified on device: force-sync now flattens 6 folder photos that previously produced "best=none" (TrueMoney invoices + a KBank slip among them) — before the fix only 1 of those photos flattened.
- **fix: robust slip-quad detection** - `SlipDocumentDetector.detectQuadInGray` now tries three edge passes (Canny 50/150, then 20/70, then contrast-equalized 20/70), accepts an exactly-4-corner polygon from the raw outline OR its convex hull (epsilon sweep now includes 0.09), and as a last resort uses the tightest rotated rectangle around the largest slip-like contour (must fill ≥50% of its bounding box and stay under 97% of the frame, so frame edges and text blobs are rejected). Each pass logs under `SlipFlattener`; failed passes are visible as `best=none canny=…` then `no quad found`.
- **on-device measurement** - Force Sync All over the two tracked folders: previous build flattened 1 photo; this build flattened 6 (`Image_2db109de`, `ReceiveMoney_QR`, 4 TrueMoney `invoice_*` crops), each persisted under Pictures with a deterministic `_flat.jpg` name. Instrumented `OcrProcessorTextTest` (crop persists under Pictures + deterministic name) passes. No FATAL exceptions.

> Note: the device's slip list shrank across adb exports during this session (43 → 36) because blind UI taps while driving the delete-capable Account list (long-press multi-select) removed test slips — not a code path in this change; the only `savedSlips.removeAt` lives in the UI-only `onDeleteSlip`, which this diff does not touch.

