# FireCash — Development Log (Day-by-Day)

> Honest, evidence-backed journal of how FireCash was built. Separated by actual calendar days from git history, plus the off-plan detour through Google AI Studio.

**Project:** FireCash — Receipt Logging & PromptPay/EMVCo Slip Verification (Jetpack Compose + Room + EasySlip + ML Kit + NotificationListener)  
**Repo:** `C:\Users\admin\Project\FireCash` • `namespace = com.example` • `applicationId = com.aistudio.firecash.qxrtv`  
**Period:** 2026-08-27 → 2026-08-28 (intensive) • Today is `Fri Aug 28 2026` (UTC)  
**Model:** `opencode/muse-spark-1.2-contributor-free` via opencode harness after Day 2 night

---

## Overview

FireCash started as research-driven. **Days 1-2 were pure research via a separate `opencode` convo** — that convo produced the Stitch UI plan (`docs/firecash_ui_stitch_plan.md` with MD3 dark tokens `#121316`/`#FF6B00`/`#10B981`/`#6366F1`). That plan was fed to **Google Stitch** to generate the initial UI. **On Day 2 daylight** an **unplanned Google AI Studio** spike was tried and was **not satisfying (not in the original plan)** and abandoned; **at Day 2 night we came to this `opencode + Muse Spark` convo with the Google Stitch design as input** and rebuilt/iterated the entire app in one long `2026-08-28` night session — from `Account Settings` scaffolding through to notification whitelists, reverse chat-style list, search, copy-to-clipboard, logo, navigation and homepage changes. All commits below are from `git log --reverse --date=short` (local time).

---

## Day 1 — 2026-08-27 — Research (via first opencode convo)

**Goal:** Understand the problem space, not code — done **through a separate `opencode` convo**.

- Read Thai banking slip specifics: PromptPay/EMVCo Tag 91 `9104[A-Fa-f0-9]{4}` CRC, Bangkok Bank `002`, KBank `004`, SCB `014`, KTB `006`, etc.
- Surveyed EasySlip `POST /verify/bank`, duplicate detection, 180-day `SLIP_NOT_FOUND`, `RATE_LIMIT_EXCEEDED`.
- Compared ML Kit Text Recognition vs Tag 91 QR parsing, Room `Expense`/`KeywordRule` persistence, CSV/PDF streaming, Drive snapshot vs full sync.
- That first opencode convo **produced the full research docs and the UI plan for Google Stitch**: `docs/firecash_full_plan.md` (11-feature table), `docs/firecash_ai_studio_systems_plan.md` (flow: Camera → OCR → EasySlip → Room → Analytics/Export), **`docs/firecash_ui_stitch_plan.md` (MD3 dark tokens `#121316` / `#FF6B00` / `#10B981` / `#6366F1` — specifically authored as input for Stitch)**, `docs/ai_studio_prompts.md`, `README.md` (8 key features).
- No code beyond `Initial commit` + `Add all remaining project files` scaffolding.

**Commits 2026-08-27:**
- `2026-08-27 Initial commit`
- `2026-08-27 feat: add Account Settings page with multiple tracked folders and sync options` — introduced `AccountSettingsScreen` (persisted `trackedFolderUris` via `JSONArray` in `SharedPreferences`), `DocumentFile` folder picker.
- `2026-08-27 Add all remaining project files` — baseline `data/ocr`, `data/easyslip`, `data/local`, `ui/`, theme.

---

## Day 2 — 2026-08-27 → 2026-08-28 — Stitch + Google AI Studio (Daylight, Off-Plan)

**Daylight — Google Stitch then Google AI Studio (both off-plan, not satisfying):**
- Fed the `firecash_ui_stitch_plan.md` from the first opencode convo into **Google Stitch** to generate the initial UI stitch. Stitch produced the dark-theme shell, card spacing, `14-20dp` radii, and test tags (`search_expenses_input` etc.) as a starting point.
- Then tried to continue the Stitch output in **Google AI Studio** in the same daylight window for faster iteration. Hit prompt limits, non-deterministic scaffolding, and no access to local `Room`/`CameraX`/`ML Kit` toolchain; generated code didn't compile against `FireCashDatabase`/`OcrProcessor` contracts and styling drifted from the Stitch design. Validated `docs/account-settings-summary.md` in parallel but AI Studio was not satisfying.
- Decision: **abandon AI Studio**.

> **Pivot at Day 2 night:** Came to **this `opencode + Muse Spark` convo (`muse-spark-1.2-contributor-free`) with the Google Stitch design as input** — allowed `scaffold → recon → hunt → validate → report` discipline and real `bash` builds (`./gradlew assembleDebug`).

---

## Day 2 Night — 2026-08-28 — This Opencode Convo Intensive (Stitch Design as Input)

This is **Day 2 night** in real time but `2026-08-28` in git (`~60` commits) — the entire intensive `opencode + Muse Spark` session rebuilding from the Google Stitch design. Presented in chronological order as they happened.

### 00:00–02:00 — Core Scan & Persistence

- **feat: add save-to-account with money-in/out toggle on QR scan** — `MainApp` `savePayload(isMoneyIn)` + `QrPayloadScreen` toggle, `SavedSlip(isMoneyIn)` deduped by `transRef/payload`.
- **fix: auto-save scanned QR slip to account + make addSlip resilient** — `handlePayload` now `addSlip(payload)` immediately (`runCatching { verifyWithEasySlip }` so verification failure never blocks save). Scan → appears in Account instantly.
- **fix: Scaffold padding — last slip behind nav bar / top double push** — `Scaffold { paddingValues -> Box(padding(bottom=calculateBottomPadding())) }` (only bottom inset; top handled by `statusBarsPadding()` in screens). This fix re-appeared twice after navigation refactors (`9d8f837`, `9d8f837`).
- **feat: AnalyticsScreen with spending summary + AI insights** — new `ui/screens/AnalyticsScreen.kt` (`AnalyticsEngine.generateAnalytics` → `categorySpends/insights`), `AccountScreen` `View Spending Summary` button below balance card; `MainApp` `showAnalytics` branch.
- **Chart iterations:** `replace horizontal bar with vertical stick chart` → `style: thinner with circular caps + baseline` (`Canvas` `drawLine` 6dp round + dot) → `feat: time-based vertical stick chart with Day/Week/Month toggle` (`TimeBucket DAY/WEEK/MONTH`, `computeEntries` with `LocalDate`/`WeekFields`).

### 02:00–06:00 — Account & Sync UX

- **fix: background sync for Account page without blocking overlay** — split `syncTrackedFolder()` (foreground `isLoading` overlay) vs `syncTrackedFolderInBackground()` (`isBackgroundSyncing` inline `Syncing…` 14dp next to Transactions).
- **feat: exclude self-transfers from balance/analytics, show as Transfer** — `isSelfTransfer(sender==receiver)` → grey `SwapHoriz` `#9E9E9E`, `Transfer` label, `THB x` without `+/-`, excluded from `moneyIn/moneyOut`.
- **feat: known names in Settings for auto income/transfer detection** — `PREFS_KNOWN_NAMES` `JSONArray`, `SettingsScreen` **My Names** card, `MainApp.isKnownName()`; `addSlip` resolves `receiver known→Income, both known→Transfer`; `AccountScreen`/`AnalyticsScreen` `effectiveIsMoneyIn()` makes it retroactive.
- **feat: retroactive income detection using known names for existing slips** — `effectiveIsMoneyIn` used for `moneyIn/moneyOut` and row color, so adding a name instantly fixes old slips without migration.
- **refactor: remove manual transaction type toggle, use auto detection** + **refactor: remove Save to Account button** — `QrPayloadScreen` now read-only; type comes only from `knownNames`.
- **fix: unknown fallback for unverified slips + background resync when EasySlip enabled** — `EasySlipClient.simulateSlipVerification` no longer returns hard `Starbucks Thailand / Roasters 45.20`; now `UNVERIFIED` with `amount = extractAmount(payload)` or `null`; `MainApp.resyncUnverifiedSlips()` + Settings **Sync N unverified** button, auto-triggered on `onToggleEasySlip(true)`/`onUpdateApiKey`.
- **fix: limit QR scan to center frame (60% ROI)** — `PhotoCaptureScreen.kt:117` filters `barcodes.firstOrNull { bbox.center in [w*0.20..0.80, h*0.20..0.80] }` vs full `InputImage`.

### 06:00–10:00 — Notifications & Whitelist

- **feat: notification income detection scooping first number** — `service/IncomeNotificationService.kt` (`NotificationListenerService`, `AMOUNT_REGEX [-+]?\d{1,3}(,\d{3})*` → first number), `AndroidManifest.xml` `BIND_NOTIFICATION_LISTENER_SERVICE`, `firecash_settings` `PREFS_SLIPS/SEEN`, `MainApp.DisposableEffect` listener keeps `savedSlips` live.
- **feat: deletable only for unknown/invalid slips** + **feat: long-press multi-select to delete/manage** + **refactor: only multi-select delete** — `isDeletable = amount==null || UNVERIFIED` (`AccountScreen.kt:79`), `selectedKeys:Set<Long>`, `combinedClickable` `onLongClick`, `TopBar: ${n} selected + Delete + All`, per-row `Delete` removed, kept only multi; `DateHeader` still shows `• n`.
- **feat: whitelist for notification income (only whitelisted apps)** → **feat: whitelist with per-app prefix template `<Sender>/<Amount>`** → **refactor: prefix is simple string detection, amount is first number after it** → **feat: multiple prefixes per app in whitelist** (`IncomeNotificationService.loadWhitelist` migrates `String|{package,prefix}` → `WhitelistedApp`; `onNotificationPosted` tries each prefix for the package; `SettingsScreen` whitelist card with `whitelist_input`/`prefix_input`).
- **feat: add money-out notification whitelist mirroring money-in** — duplicated `PREFS_NOTIFICATION_EXPENSE/_EXPENSE_WHITELIST`, `saveExpenseFromNotification(isMoneyIn=false)`, `MainApp` `notificationExpenseEnabled/Whitelist`, `SettingsScreen` second **Notification Expense** card (red accent, `notification_expense_switch`).
- **fix: notification switch layout inside card (weight)** — inner `Row(weight=1f)` so `Switch` never overflows `RoundedCornerShape 16dp` card.
- **fix: slip details always visible offline** — `EasySlipClient` `extractAmount()` fallback + `MainApp.addSlip` fallback `VerifySlipResponse(UNVERIFIED, amount=extractAmount)` + `QrPayloadScreen` fallback card; `AccountScreen.onSlipClick` builds fallback `VerifySlipResponse` from `SavedSlip` so old slips show details.
- **fix: slip details card always visible even for unverified/old slips** — `QrPayloadScreen.kt:103` always renders card (uses `extractAmount(payload)` if `slipData==null`).

### 10:00–14:00 — Logo & Navigation

- **feat: set custom launcher icon from provided flame mascot (adaptive + legacy mipmap)** — generated `drawable/firecash_icon.png` (1024) via `System.Drawing`, `drawable/ic_launcher_background.xml #121316`, `mipmap-*/ic_launcher*.png` 48/72/96/144/192 + `ic_launcher_foreground.png`; `mipmap-anydpi-v26` adaptive XML.
- **feat: move camera to FAB on Account with scroll-hide** → **refactor: persistent camera button inside balance card at right side** → **refactor: move camera button to top-right of balance card** — `AccountScreen` balance card `Row(SpaceBetween, Top)` with `Column(Current Balance + THB 32sp Bold)` left and `44dp` circular `PhotoCamera` `IconButton(white 12% bg)` right; FAB with `rememberLazyListState` + `snapshotFlow` + `AnimatedVisibility(fade+scale)` removed after user feedback.
- **refactor: merge AccountSettings and System Settings into unified Settings** — deleted `AccountSettingsScreen.kt` (273 lines), moved **Tracked Folders** card (folder picker `OpenDocumentTree`, `syncTrackedFolder`, `importSlips`, `isLoading` spinner) into `SettingsScreen` (now 1294 lines), `MainApp` removed `showAccountSettings`, `Scaffold bottomBar` → `if (!showPayload)`, `AccountScreen.onOpenSettings` → unified `Settings`.
- **User logo detour:** `chore: update launcher logo via Android Studio` (`88f49ac`, `ic_launcher-playstore.png` + `mipmap-*/ic_launcher*.webp`), `chore: update launcher assets after rebuild` (`8112e8b`), `chore: use App Logo (1).png` (`1537e8e` 44KB), then **Do not touch app logo** → `Revert "chore: use App Logo (1).png"` (`03adeb8`) + `fix: restore cute mascot for in-app title (drawable only, launcher untouched)` (`e286ac6` 44KB), then **make cat bigger than title** → `style: make cat logo 56dp larger than FireCash title` (`0ffff08`, `48dp→56dp`, `headlineSmall→headlineMedium 26sp ExtraBold`).
- **refactor: remove bottom nav, add top bar navigation on camera** (`7c364a9`) — deleted `CaptureBottomBar` from `Scaffold`, `PhotoCaptureScreen` top `Row(SpaceBetween)` with left `AccountBalanceWallet` (`onNavigateToAccount`) and right `Settings`.
- **feat: make Account page the homepage** (`c82270f`) — `showSavedSlips=true` by default, `QrPayload.onBack`/`savePayload`/`Settings.onBack` → Account.
- **feat: system back gesture mirrors in-app navigation** (`709cf38`) — `BackHandler` in `MainApp` (`showPayload→Account`, `showAnalytics→Account`, `showCapture→Account`, `Settings→Account`) + `AccountScreen` `BackHandler(isSelectionMode) → clear selection`.
- **fix: prevent top double padding after removing bottom bar** (`9d8f837`) — back to `padding(bottom=calculateBottomPadding())`.

### 14:00–18:00 — List & Search Polish

- **feat: reverse layout for slips list (bottom to top)** (`4cc5050` `LazyColumn(reverseLayout=true)`) + **fix: keep reversed slips list anchored at top** (`6e2d897` `fillMaxWidth` wrap) — newest at bottom, scroll up for older.
- **fix: date header on top of day group with reverse layout** (`3dc91f7`) — emit `items` before `item(header)` so header visually sits above its day's slips; also **feat: show daily net total in date header** (`c4c0cfe` `DateHeader(date,count,total:Double)` `THB %.2f • n` green/red).
- **feat: show slip time above amount in transaction row** (`892f041`) — `Column(End)` with `11sp` `slip.time` + `2dp Spacer` + `15sp SemiBold` amount.
- **refactor: replace back+My Account with FireCash logo+title** (`f7b277b`) — `AccountScreen` top bar `Image(R.drawable.firecash_icon, 32dp Circle)` + `FireCash headlineSmall Bold` (later enlarged to 56dp/`26sp ExtraBold`).
- **Logo swap again:** user supplied `App Logo (1).png`/`(2).png` in `Downloads`; we copied `(1)` to `drawable/firecash_icon.png` and regenerated `mipmap-*/ic_launcher*.png` + `foreground.png` + removed `webp` duplicates, then reverted launcher to Studio version on request.
- **feat: search slips by date/title/amount/exact QR payload on Transactions header** (`b800aeb`) — `Row(SpaceBetween)` `Transactions` + `Search/Close` icon, `OutlinedTextField` (`RoundedCornerShape 10dp`), `filteredSlips = remember(slips, searchQuery)` with `date/title/amount contains` + `payload==q` exact.
- **fix: require exact match for QR payload and ref in slip search** (`ee40096` `transRef==q` exact, payload already exact).
- **feat: tap detail row to copy, shows green Copied feedback** (`6b02680`) + **feat: tap QR payload to copy** (`1ab7099`) — `DetailRow` `clickable` + `ClipboardManager` + `copied` 1200ms delay → `Copied` `0xFF66BB6A` green.
- **style: make back control a pill button with Back label** (`b423617`) — `QrPayloadScreen` `IconButton(ArrowRightAlt)` → `Button(RoundedCornerShape 12dp, BorderStroke 1dp Gray 30%, FireCashSurfaceContainerLow, 14×10dp)` with `ArrowBack 18dp + Back 14sp`.

### Current State (End of 2026-08-28)

**Build:** `BUILD SUCCESSFUL` (last two `assembleDebug` 17-18s, warnings only: `EasySlipClient.kt:254 String?`, `FireCashDatabase.kt:27 fallbackToDestructiveMigration`, `BottomNavBar` deprecated `ReceiptLong`, `AnalyticsScreen` `TrendingUp`, `PhotoCaptureScreen` `LocalLifecycleOwner/setTargetResolution`, `SettingsScreen` `Label`).

**Homepage:** `AccountScreen` (balance card with 56dp cat + `FireCash 26sp`, `Money In/Out` + camera at top-right, `View Spending Summary`, searchable `Transactions` (`reverseLayout` bottom→top, `DateHeader` with net total), long-press multi-select, time above amount, `SwapHoriz` grey `Transfer`).

**Settings (unified):** Base Currency, EasySlip (API key + duplicate toggle + Sync unverified), My Names, Notification Income/Expense (whitelist per-app with multiple prefixes, string `prefix → first number after`), Tracked Folders (add/sync/import), Keyword Mapping, Drive Backup.

**Not yet implemented (vs `firecash_full_plan.md`):** `Screen.Onboarding`, `Screen.Export`/`BackupRestore` routes (`MainViewModel` defines them, `MainApp` else is `Settings`), Drive sync execution (`MainApp` `googleDriveSync=false`), keyword `rules` persistence (`MainApp` `emptyList()`), `BottomNavBar` component now dead code after `7c364a9`.

**Next steps you mentioned:** keep launcher untouched, potentially revisit `Onboarding`, `Export` unlimited, Drive restore, and verification rate-limit UX.

---

*Generated from `git log --reverse --date=short` + `docs/*.md` + `app/src/main/java` on `2026-08-28`. Run `git log --oneline` to replay.*
