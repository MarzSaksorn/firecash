# FireCash Project – Status & Next Steps

## Current State
| Item | Status | Details |
|------|--------|---------|
| Camera preview | Done | CameraX + runtime permission (Activity Result API) |
| Photo capture & save | Done | Saves to `cacheDir/capture_<timestamp>.jpg` |
| Pass image path to ViewModel | Done | `onCapture(SourceType.CAMERA, null, photoFile.absolutePath)` |
| OCR processing | Broken | `OcrProcessor` ignores `imageUri`; always returns Uber preset / hard-coded $45.20 |
| Persist image location | Not done | Image stays in cache, not moved to permanent storage |
| Tests | Not done | No tests cover OCR or capture flow |
| Build | OK | `gradlew assembleDebug` succeeds |

## Problem
Even after wiring the captured image path through `CaptureScreen` → `MainApp` → `MainViewModel.startVerification` → `OcrProcessor.processReceipt(imageUri, samplePreset)`, the extracted data is always the Uber placeholder:
- Merchant: UBER TECHNOLOGIES INC.
- Amount: $45.20

Root cause: `OcrProcessor.kt` never reads the image from `imageUri`. It returns preset text when `samplePreset != null`, otherwise returns a hard-coded default Uber receipt string. No real OCR is performed on the captured photo.

## Next Steps
1. Add an OCR library (e.g., Google ML Kit `com.google.mlkit:text-recognition`).
2. Update `OcrProcessor.processReceipt`:
   - If `imageUri` is provided and file exists → decode bitmap, run text recognition, feed text to `SlipDataParser.parse(text)`.
   - Else fall back to preset/default text.
3. Move captured image to permanent storage (e.g., `filesDir/receipts/<uuid>.jpg`) and store the path on the `Expense`.
4. Add unit tests:
   - `startVerification` forwards `imageUri` to `ocrProcessor`.
   - `OcrProcessor` returns parsed result from OCR text when image present.
   - Review screen displays the captured image.
5. UI: show progress while OCR runs; disable shutter during processing.
6. Rebuild and verify with a real receipt photo.

## Files Touched / Relevant
- `app/src/main/java/com/example/ui/screens/CaptureScreen.kt`
- `app/src/main/java/com/example/ui/MainApp.kt`
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/data/ocr/OcrProcessor.kt`
- `app/src/main/java/com/example/data/ocr/SlipDataParser.kt`
- `app/build.gradle`
