# FireCash – AI Studio Systems Implementation Plan

## Overview
FireCash is an intelligent financial tracking and slip verification application for Android built with Jetpack Compose, Room Database, ML Kit OCR, and EasySlip verification proxy capabilities.

---

## Architecture Flow

```
[ Camera / File / Preset Slip ]
            │
            ▼
    [ ML Kit OCR Engine ]
     (SlipDataParser)
            │
    ┌───────┴───────┐
    ▼               ▼
[Standard Receipt] [PromptPay / Bank Slip]
    │               │
    │               ▼
    │       [EasySlip Proxy Client]
    │       - CRC Tag 91 Validation
    │       - Bank Code Resolution (004 KBank, 014 SCB, 002 BBL)
    │       - Duplicate Transaction Check
    │               │
    └───────┬───────┘
            ▼
   [ Review & Edit Screen ]
            │
            ▼
  [ Room Local Database ] ────► [ Google Drive Backup ]
   (FireCashDatabase)
            │
    ┌───────┴───────┐
    ▼               ▼
[AI Analytics]  [CSV / PDF Export]
(Bento Charts)  (Streaming Reports)
```

---

## Core System Modules

1. **Room Database & Persistence (`com.example.data.local`)**
   - Stores `Expense` items with CRC, status, source types, tags, and amounts.
   - Reactive `ExpenseDao` with search, category filtering, and date range queries.
   - `KeywordRule` and `KeywordRuleDao` for rule-based automatic category classification.

2. **OCR & Slip Parser (`com.example.data.ocr`)**
   - ML Kit text recognition for document images.
   - Regex-based Tag 91 CRC (`9104[A-Fa-f0-9]{4}`) and EMVCo QR code extraction.
   - Multi-currency parsing (THB ฿, USD $, EUR €, GBP £, JPY ¥).

3. **EasySlip Proxy Client (`com.example.data.easyslip`)**
   - Secure verification client communicating with proxy backend.
   - 4-stage verification: Tag 91 CRC check -> Bank Resolution -> EasySlip Proxy check -> Duplicate check.

4. **Google Drive Backup (`com.example.data.backup`)**
   - JSON snapshot and DB export/restore capabilities for secure cloud backups.

5. **AI Analytics (`com.example.data.analytics`)**
   - On-device pattern detection (recurring merchant discovery, peak anomaly expenses, category dominance).

6. **Export Manager (`com.example.data.export`)**
   - CSV and PDF document generation with date range filtering.

7. **Settings & Preferences (`com.example.data.repository`)**
   - Currency selection, auto-backup, proxy configuration, and API credentials.
