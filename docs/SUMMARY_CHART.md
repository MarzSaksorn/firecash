# FireCash — Feature Summary

| Category | Feature | Status | Notes |
|----------|---------|--------|-------|
| **Capture** | Live CameraX QR scan (60% ROI) | ✅ | Center-frame only, laser guide removed |
| | Gallery image picker & import | ✅ | |
| | Auto-save scanned slips to Account | ✅ | `addSlip` resilient (`runCatching`) |
| **Verification** | EasySlip API verification (CRC, sender, ref) | ✅ | Fallback: unverified slips with Sync button |
| | Offline fallback for unverified slips | ✅ | `resyncUnverifiedSlips`/`fullResync` skip `manual:`/`notif:` |
| **Storage** | Local-only SharedPreferences persistence | ✅ | No Room DB; `savedSlips`, `seenPayloads`, `processedFiles`, `trackedFolders`, `knownNames`, `whitelists`, `settings` |
| | Slip links to actual photo on device | ✅ | `photoPath` in `SavedSlip`; `FileProvider` + `ACTION_VIEW` |
| **Smart Detection** | My Names auto-detection (sender/receiver/self) | ✅ | Retroactive via `effectiveIsMoneyIn` and `isSelfTransfer` |
| | Notification Income/Expense detection | ✅ | Per-app prefix whitelists, multiple prefixes per app |
| | Manual income/expense entry | ✅ | `+` button → dialog with amount/note |
| **Analytics** | Spending summary with AI insights | ✅ | Vertical stick chart (Day/Week/Month), trend detection |
| **Settings** | Unified Safe/Dangerous categories | ✅ | Dangerous section collapsible, red background |
| | Data Transfer (JSON export/import) | ✅ | Full state transfer between phones |
| **Background** | Persistent foreground service (non-dismissible) | ✅ | `START_STICKY`, music-player style notification |
| | Battery optimization exemption | ✅ | Button hides when granted |
| **Navigation** | Android back gesture | ✅ | Mirrors in-app navigation; Account is homepage |
| **Build** | Debug build (`assembleDebug`) | ✅ | Uses `debug.keystore`; release uses env vars |

> ✅ = Implemented and tested

---

*Generated from `git log --oneline` + `app/src/main/java` on `2026-08-29`. Run `git log --oneline` to replay.*