# FireCash Receipt‑Logging App – Full Implementation Plan

## 1. Vision & Scope
- **Goal**: An offline‑first Android app that captures bank slips or credit‑card receipts, extracts key data via OCR, validates bank slips through EasySlip, stores everything locally (Room DB), and optionally backs up to Google Drive.
- **Key Differentiators**: No user account required, premium‑free unlimited export, on‑device AI analytics, and secure EasySlip verification.

---

## 2. Consolidated Feature Set
| # | Feature | Details |
|---|---------|---------|
| 1 | Capture Receipt | Camera, gallery picker, PDF/image upload. |
| 2 | OCR Extraction | ML Kit Text Recognition → amount, date, merchant, currency, **bank‑slip CRC**. |
| 3 | EasySlip Verification | Build payload (CRC, sendingBank, transRef) → POST your proxy endpoint `/api/verifySlip` (which securely forwards to EasySlip) → duplicate detection, amount validation, error handling. |
| 4 | Local Storage | Room DB (`ReceiptEntity`: id, amount, date, merchant, category, sourceType, verificationStatus). |
| 5 | Categorization | Keyword‑based auto‑category with user‑editable rules (premium‑free). |
| 6 | Summary UI | Jetpack Compose list + bar chart (daily/weekly/monthly). |
| 7 | Unlimited Bulk Export | CSV/PDF for any date range; unlimited for free tier. |
| 8 | AI Analytics | On‑device trend detection, category suggestions, clustering (premium‑free). |
| 9 | Google Drive Backup | One‑tap export of Room DB file; restore via import. |
|10| Settings | Currency, keyword maps, backup toggle, API‑key entry (secure). |
|11| Onboarding/Tutorial | Intro screens walking through capture → store → view flow. |

---

## 3. Architecture & Components
- **`data/ocr/`**: `OcrProcessor.kt`, `SlipDataParser.kt` for Tag 91 and EMVCo/PromptPay QR & CRC extraction.
- **`data/easyslip/`**: `EasySlipClient.kt`, `VerificationModels.kt` for proxy integration, duplicate detection, and rate limiting.
- **`data/analytics/`**: `AnalyticsEngine.kt` for on-device AI trend analysis, spending clusters, and anomaly detection.
- **`data/export/`**: `ExportManager.kt` for high-throughput streaming CSV and PDF generation.
- **`data/backup/`**: `DriveBackupManager.kt` for secure DB snapshotting and restoration.
- **`data/local/`**: Room SQLite persistence (`FireCashDatabase`, `ExpenseDao`, `KeywordRuleDao`).
- **`ui/`**: Jetpack Compose screens with Material Design 3 and responsive Dark Palette.

---

## 4. EasySlip Proxy Integration
To ensure complete API key security, client requests are sent to a server-side proxy `/api/verifySlip` which attaches the secret EasySlip API key before forwarding to `https://api.easyslip.com/v2/verify/bank/payload`.
The client handles all responses:
- `VERIFIED`: Transaction valid, bank and account matched.
- `DUPLICATE_DETECTED`: Warning flagged to prevent double accounting.
- `AMOUNT_MISMATCH`: Highlighted for user confirmation.
- `SLIP_NOT_FOUND`: Older than 180 days or invalid CRC.
- `RATE_LIMIT_EXCEEDED`: Client-side back-off and retry.
