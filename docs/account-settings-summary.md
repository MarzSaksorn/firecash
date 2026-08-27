# Account Settings — Summary of Changes

## Objective
- Add a dedicated **Account Settings** page that centralises all sync‑related options.
- Move sync handling out of `AccountScreen` into its own composable `AccountSettingsScreen`.
- Support **multiple tracked folders** instead of a single folder.
- Hide the bottom navigation bar while the settings page is visible.

## Key Changes
- **`AccountScreen.kt`**: Re‑added `CircularProgressIndicator` import; added `onOpenSettings` callback to navigate to the new settings page.
- **`MainApp.kt`**:
  - Introduced `showAccountSettings: Boolean` state.
  - Updated bottom‑bar conditional to `if (!showPayload && !showAccountSettings)`.
  - Added new branch `showAccountSettings` that instantiates `AccountSettingsScreen`.
  - Passes `trackedFolderUris` (list of folder URIs stored as JSON in SharedPreferences) to the settings screen.
  - Added helper functions `loadTrackedFolders` / `saveTrackedFolders` for persistent storage.
  - Extracted `scanFolder(uriStr)` suspend function to scan any number of folders.
  - Added `onFolderSelected(uri)` – adds a folder (with persisting permission) and triggers sync.
  - Added `onRemoveFolder(uriStr)` – removes a folder from the list.
- **New UI flow**:
  - Account page → gear icon → **Account Settings**.
  - Settings page shows:
    - List of tracked folders (name + remove ✕).
    - “Add Tracked Folder” button (opens system picker).
    - “Sync Tracked Folders Now” button (scans all folders).
    - “Import Slip Photos from Device” (existing import flow).
  - Back from settings returns to Account page and sets `showSavedSlips = true`.

## Files Modified
| Path | Change |
|------|--------|
| `app\src\main\java\com\example\ui\screens\AccountScreen.kt` | Re‑added `CircularProgressIndicator` import; added `onOpenSettings` lambda prop. |
| `app\src\main\java\com\example\ui\MainApp.kt` | Added `showAccountSettings` state, bottom‑bar logic, new branch, `AccountSettingsScreen` call, folder‑list persistence helpers, `onFolderSelected` / `onRemoveFolder`. |
| `app\src\main\java\com\example\ui\screens\AccountSettingsScreen.kt` | (Already existed – now receives `trackedFolders`, `onRemoveFolder`, computes folder names via `DocumentFile`). |
| `app\src\main\java\com\example\ui\MainApp.kt` (helpers) | `loadTrackedFolders`, `saveTrackedFolders` using JSON array in SharedPreferences. |

## Build Status
```
./gradlew assembleDebug
BUILD SUCCESSFUL (warnings only – pre‑existing deprecation notices)
```

## Next Steps / TODOs
- [ ] Verify that the **AccountSettingsScreen** UI correctly displays folder names and handles remove actions.
- [ ] Test navigation: opening settings from Account page, returning, and ensuring bottom bar reappears.
- [ ] (Optional) Add a toggle in **Account Settings** to enable **income‑tracking via NotificationListenerService** (reading bank notifications) – see `docs/notification-income-plan.md` for a draft plan.
- [ ] Run UI tests or manual walkthrough to confirm no regression in the Account → Slip flow.

---
*Generated on Thu Aug 27 2026*