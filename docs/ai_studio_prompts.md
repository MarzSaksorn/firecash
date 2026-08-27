# FireCash – AI Studio Prompt Library

This document contains production-ready, modular system-spec prompts for all 10 core components of **FireCash**, designed for execution in Google AI Studio and Antigravity coding environments.

---

## 1. Room Database & Persistence Prompt

```markdown
You are an expert Android Kotlin systems engineer.
Produce the complete Room database architecture for FireCash in package `com.example.data.local`:
1. `Expense` entity with:
   - id: Long (auto-generated primary key)
   - merchant: String
   - amount: Double
   - date: String (YYYY-MM-DD)
   - time: String (HH:MM AM/PM)
   - category: String
   - tags: String
   - imageUrl: String?
   - receiptText: String?
   - isVerified: Boolean
   - verificationStatus: VerificationStatus enum (VERIFIED, UNVERIFIED, DUPLICATE_DETECTED, AMOUNT_MISMATCH, SLIP_NOT_FOUND, RATE_LIMITED)
   - crc: String? (Tag 91 CRC 4-hex checksum)
   - sendingBank: String?
   - transRef: String?
   - isDuplicate: Boolean
   - matchedAccount: String?
   - isAmountMatched: Boolean
   - sourceType: SourceType enum (CAMERA, GALLERY, PDF_UPLOAD, MANUAL)
   - currency: String
   - dateGroup: String
2. `KeywordRule` entity for auto-categorization mapping rules.
3. `ExpenseDao` and `KeywordRuleDao` interfaces with modern Flow-based reactive queries, suspend CRUD operations, search filters, and date range queries.
4. `FireCashDatabase` abstract RoomDatabase class with singleton instance provider, version 1 schema, and seed data initialization.
```

---

## 2. ML Kit OCR & Slip Parser Module Prompt

```markdown
You are an expert Android computer vision engineer.
Produce the ML Kit text recognition and slip parsing pipeline for FireCash in package `com.example.data.ocr`:
1. `OcrProcessor.kt`:
   - Wraps Google ML Kit TextRecognition (`TextRecognizer`) client.
   - Provides asynchronous image file / bitmap analysis using Kotlin Coroutines and SuspendCancellableCoroutine.
   - Fallback simulation parsing when camera simulator / emulator has no physical sensor.
2. `SlipDataParser.kt`:
   - Extracts merchant names by filtering headers (e.g., ignoring 'tax invoice', 'receipt', 'โอนเงิน').
   - Multi-currency amount regex extraction supporting THB (฿), USD ($), EUR (€), GBP (£), and JPY (¥).
   - PromptPay and EMVCo QR code payload extraction (`9104[A-Fa-f0-9]{4}` Tag 91 CRC regex, `000201010212` QR standards).
   - Thai Bank Code resolution (004 Kasikornbank, 014 Siam Commercial Bank, 002 Bangkok Bank, 006 Krungthai).
   - Auto-categorization inference engine for receipts.
```

---

## 3. EasySlip Proxy Client & Verification Module Prompt

```markdown
You are an expert Android network and security engineer.
Produce the EasySlip bank slip verification integration in package `com.example.data.easyslip`:
1. `VerificationModels.kt`:
   - `VerifySlipRequest`, `VerifySlipResponse`, `BankPayload`, `EasySlipData`, `AccountInfo`, `VerificationState`.
2. `EasySlipClient.kt`:
   - Secure proxy-compatible client communicating with Cloud Run / Express proxy endpoint.
   - Supports direct payload verification (CRC Tag 91 checksum, sendingBank code, matchAmount).
   - Mock verification mode when offline / demoing, simulating full 4-stage pipeline (QR Scan -> Bank Resolution -> EasySlip Proxy Check -> Duplicate Detection).
   - Rate limit and error state handling.
```

---

## 4. Google Drive Backup & Sync Manager Prompt

```markdown
You are an expert Android cloud synchronization engineer.
Produce the Google Drive backup and restore module for FireCash in package `com.example.data.backup`:
1. `DriveBackupManager.kt`:
   - Export SQLite / Room DB database and JSON snapshots.
   - Restore database from remote drive snapshots.
   - Snapshot data structures (`FireCashBackupSnapshot`) containing all receipts, keyword rules, and timestamps.
   - Reactive status flow monitoring (IDLE, IN_PROGRESS, SUCCESS, ERROR).
```

---

## 5. AI Spending Analytics & Pattern Intelligence Prompt

```markdown
You are an expert machine learning & analytics engineer for Android Jetpack Compose.
Produce the on-device spending intelligence engine for FireCash in package `com.example.data.analytics`:
1. `AnalyticsEngine.kt`:
   - Computes total expenditures, daily averages, and percentage growth.
   - Generates category distributions and percentage weights.
   - Detects recurring vendor clusters (merchants with >= 2 transactions).
   - Identifies high-value spending anomalies (peak single expense thresholding).
   - Highlights top categorical spending dominance.
   - Outputs structured `AnalyticsSummary` and `SpendingInsight` for Bento-style Compose UI cards.
```

---

## 6. Export Module (CSV & PDF Streaming) Prompt

```markdown
You are an expert Android file I/O engineer.
Produce the file export module for FireCash in package `com.example.data.export`:
1. `ExportManager.kt`:
   - Date range filtering (`fromDate` to `toDate`).
   - `exportToCsv()`: Streaming CSV format with headers: ID, Date, Time, Merchant, Amount, Currency, Category, Tags, CRC, Status, Source.
   - `exportToPdf()`: Formatted PDF document using Android `android.graphics.pdf.PdfDocument` with title banner, summary stats, and itemized transaction rows.
   - Returns absolute file paths ready for Android `Intent.ACTION_SEND` sharing.
```

---

## 7. Settings & Preferences Repository Prompt

```markdown
You are an expert Android architecture engineer.
Produce the settings & preferences store in package `com.example.data.repository`:
1. `SettingsRepository.kt`:
   - Manages user currency selection (USD, THB, EUR, GBP, JPY).
   - Google Drive automated sync toggle and last sync timestamps.
   - EasySlip verification proxy endpoint URL and API Key management.
   - Duplicate detection toggles.
   - In-memory StateFlow persistence and SharedPreferences backing.
```

---

## 8. State Management & Architecture ViewModels Prompt

```markdown
You are an expert Android Jetpack Compose ViewModel engineer.
Produce the central state management layer in package `com.example.ui.viewmodel`:
1. `MainViewModel.kt`:
   - Screen routing hierarchy (`Screen.Onboarding`, `Screen.Capture`, `Screen.Verifying`, `Screen.Review`, `Screen.Expenses`, `Screen.Analytics`, `Screen.Export`, `Screen.Settings`, `Screen.BackupRestore`).
   - Reactive state flows combining Room queries, active search filters, and category selections.
   - Verification workflow coroutine orchestrating OCR extraction, EasySlip verification, and receipt confirmation.
   - Toast and snackbar event channels.
```

---

## 9. Test Suite (Robolectric & Unit Tests) Prompt

```markdown
You are an expert Android QA and unit testing engineer.
Produce unit and Robolectric tests in `app/src/test/java/com/example/`:
1. `SlipDataParserTest.kt`: Unit tests for Tag 91 CRC extraction, PromptPay strings, date/time regex, and multi-currency parsing.
2. `AnalyticsEngineTest.kt`: Unit tests for summary calculations, empty state fallback, category percentage distribution, and AI insights.
3. `EasySlipClientTest.kt`: Unit tests for verification models and proxy response handling.
4. `ExportManagerTest.kt`: Tests verifying CSV generation and filtering logic.
```

---

## 10. CI/CD Pipeline Workflows Prompt

```markdown
You are a DevOps and GitHub Actions engineer for Android.
Produce workflow configuration files in `.github/workflows/`:
1. `ci.yml`: Checks out repository, sets up Java 17 / 21, caches Gradle dependencies, runs `./gradlew lintDebug`, `./gradlew testDebugUnitTest`, and builds debug APK (`./gradlew assembleDebug`).
2. `proxy.yml`: Builds Docker container for EasySlip Node.js/Cloud Run proxy and deploys to Google Cloud Run with secret environment variables.
```
