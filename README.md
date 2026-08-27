# FireCash — Receipt Logging & EasySlip Verification Android App

FireCash is a modern, offline-first Android application built with **Jetpack Compose**, **Room Database**, and **Material Design 3**. It allows users to capture receipts and bank transfer slips, extract transaction data via intelligent OCR (including Tag 91 bank slip QR CRC parsing), verify slips with EasySlip through a secure proxy, auto-categorize spending with custom keyword rules, generate AI spending analytics, and perform unlimited bulk exports (CSV / PDF) and Google Drive backups.

## Key Features

1. **Receipt & Bank Slip Capture**
   - Live camera viewfinder simulation with alignment guides, flash controls, and laser scanning animations.
   - Gallery image picker, document/PDF upload, and sample bank slip quick testing.
2. **Intelligent OCR & Tag 91 CRC Parser**
   - Automatically extracts Merchant name, Total Amount, Date, Time, Currency, and Tag 91 PromptPay/EMVCo CRC payloads.
3. **EasySlip Slip Verification**
   - Communicates with `/api/verifySlip` backend proxy.
   - Verifies CRC, sending bank, and transaction reference with duplicate detection and amount validation.
4. **Offline-First Room Persistence**
   - Fully local storage with Room DB (`Expense`/`ReceiptEntity`, `KeywordRule`).
   - No forced user account required.
5. **Smart Auto-Categorization & Keyword Rules**
   - Instant categorization rules (e.g., Starbucks → Food & Dining, Uber → Travel).
   - Full user-editable keyword mapping UI.
6. **AI Analytics & Spending Insights**
   - On-device trend detection, week-over-week comparisons, category clustering, and recurring expense alerts.
7. **Unlimited Bulk Export**
   - Filter by date range (This Month, Last Month, YTD, Custom) and export clean CSV or formatted PDF reports.
8. **Google Drive Cloud Backup & Restore**
   - One-tap database snapshot export and encrypted cloud sync.
