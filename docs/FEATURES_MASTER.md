# FireCash Features — Latest Master (Redesign Branch)

> All features implemented from commit `1a14d7c` to the latest redesign branch.  
> Generated from `git log origin/redesign --not 1a14d7c --reverse`

---

## 1. Fraud Detection & Cross-Check

- **feat: flag slips whose photo text amount doesn't match the QR or bank** (`1a14d7c`) — compares the baht amount read from the slip photo text, the EMVCo QR tag-54 amount, and the bank-verified amount. Any disagreement >0.005 sets `SavedSlip.amountMismatch` and shows a red "possible tampered slip" banner on the detail screen.
- **feat: cross-check the slip photo date against the bank-verified date** (`a574d47`) — compares the date printed on the slip photo against the bank-verified `transDate`. A mismatch sets `SavedSlip.dateMismatch` and shows a red banner alongside the amount-mismatch banner.
- **fix: never let batch verification overwrite slip amounts** (`abd7f55`) — `verifyBatch` detects canned sandbox responses (same transRef across multiple slips) and discards the batch; `applyVerifiedUpdate` keeps the existing amount, only fills missing ones, and sets the fraud flag on disagreement.

## 2. Shop-Only Refactor

- **refactor: make FireCash shop-operator only** (`5e44c9f`) — removed the App Mode setting card, all personal-mode paths, the dataset split, and the `showVerification` parameter. The home card always opens the camera, slips always go through QR + verification with the photo-text fraud cross-check.
- **feat: keep personal and shop slips in fully separate datasets** (`db6c41f`, later removed) — mode-scoped prefs keys, dataset swap on mode switch. Removed when the app dropped the mode switch.

## 3. Slip Document Detection & Perspective Flattening

- **feat: detect and perspective-flatten the slip before OCR and QR decode** (`1207f2a`) — new `data/ocr/SlipDocumentDetector.kt` + OpenCV 4.10.0. Every captured/picked/imported/folder slip photo is checked for a flat 4-corner document region. When found, the region is perspective-warped into a flat copy, and QR + text recognition run on THAT.
- **feat: disable live QR auto-scan** (`3ed547b`) — the CameraX analyzer no longer runs ML Kit barcode scanning on every preview frame. The flow is now: frame the slip (green hint when a flat surface is detected), tap the shutter, and the QR is decoded from the captured (flattened) photo.
- **feat: outline the detected slip on the camera preview** (`0adbad8`) — draws a green 4-corner outline around the detected slip, mapped through the same FILL_CENTER crop the PreviewView uses. A white guide box + "Align the slip within the view" shows while nothing is detected.
- **feat: store the flattened slip crop as the slip photo** (`f5fceb4`) — the crop is persisted to `getExternalFilesDir(DIRECTORY_PICTURES)`, becomes the slip's `photoPath`, and the full frame is deleted after the slip is saved. Deterministic naming prevents duplicate files on Force Sync.
- **fix: make slip flattening reliable across photos** (`1707597`) — added EXIF rotation (`decodeRotated`) and robust detector with three edge passes (Canny 50/150, 20/70, contrast-equalized 20/70), convex-hull fallback, and rotated-rectangle fallback.

## 4. Verification & API Error Handling

- **fix: surface actual EasySlip API errors instead of "not configured" fallback** (`2e2dbc0`) — HTTP 400, 403, 500 errors now parse the error body and return the real error code instead of silently falling back to `simulateSlipVerification` with a misleading "not configured" message.
- **fix: remove 600ms artificial delay from processReceipt** (`1611648`) — removed the forced 600ms pause at the top of every `processReceipt` call, making the scanning pipeline ~600ms faster.

## 5. Manual Classification Override

- **feat: allow manual override of income/expense/transfer classification** (`31c8b5f`) — added `manualCategory` field to `SavedSlip` (null = auto, `"income"`, `"expense"`, `"transfer"`). The slip detail screen shows three toggle buttons; tapping the active button resets to auto-detection. `effectiveIsMoneyIn` in all three files (AccountScreen, AnalyticsScreen, MainApp) checks the override first.

## 6. Bank and Cash Wallet Categories

- **feat: add Bank and Cash wallet categories** (`0575cda`) — added `wallet` field to `SavedSlip` (null = Bank, `"cash"` = Cash). Wallet toggle on the slip detail screen alongside the classification toggle. Serialized in JSON.
- **feat: separate Bank and Cash wallet menus with tab bar** (`91cf462`) — added a tab bar below the header to switch between Bank and Cash views. The camera button becomes a plus button on the Cash tab (opens the manual add dialog).
- **fix: separate Bank and Cash transaction lists** (`672a74e`) — Bank tab shows only bank/uncategorized slips, Cash tab shows only cash slips. Manual entries from the Cash tab are assigned to the cash wallet automatically.
- **separate spending summary by wallet** (`3147c27`) — Analytics screen shows bank data on Bank tab and cash data on Cash tab.

## 7. UI Refinements

- **move spending summary to a graph icon beside the camera button** (`936335b`) — replaced the full-width "View Spending Summary" button with a TrendingUp graph icon beside the camera/plus button in the balance card header.
- **remove wallet breakdown from balance card** (`8d03464`) — the small Bank/Cash balance box inside the balance card was removed. The tab bar stayed.
- **move Bank/Cash switch to a toggle beside settings button** (`17c3f56`) — replaced the full-width tab bar with a small circular toggle button next to the settings gear.

## 8. Stitch Design Redesign (Redesign Branch)

- **apply Stitch design: bottom nav bar, filter chips, balance card** (`3fa6c73`) — added bottom navigation bar (Home, Analytics, Settings), filter chips (All, Income, Expense, Transfer), and updated balance card layout.
- **apply Stitch design: redesigned balance card with Total Balance, Income/Spent** (`4b173aa`) — balance card now shows "Total Balance" label, "Income" and "Spent" row labels, and pill-shaped action buttons.
- **apply Stitch design: transaction row, bottom nav, filter chips** (`91a238d`) — transaction rows show category + time inline, bottom nav bar uses Stitch styling.

## 9. Fixes & Maintenance

- **fix: periodic tracked folder polling + isLoading safety guards** (`9e1cb11`) — added safety reset for `isLoading` flag in folder sync functions.
- **fix: LazyColumn crash from duplicate savedAt keys** (`efe2426`) — changed LazyColumn key from `payload` to `payload + savedAt` to prevent crashes from duplicate keys.
- Various docs updates, AGENTS.md maintenance, and gitignore updates.

---

## Summary

| Feature Area | Commits |
|---|---|
| Fraud Detection | 4 |
| Shop-Only Refactor | 3 |
| Slip Flattening/Crop | 5 |
| API Error Handling | 2 |
| Manual Classification | 1 |
| Bank/Cash Wallets | 5 |
| UI Refinements | 3 |
| Stitch Redesign | 3 |
| Fixes & Maintenance | 4 |
| **Total** | **~30 commits** |