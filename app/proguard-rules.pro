# FireCash ProGuard/R8 rules
# Keep Moshi/Retrofit/JSON-serialized models
-keep class com.example.data.model.** { *; }
-keep class com.example.data.easyslip.** { *; }
-keep class com.example.data.verification.** { *; }
-keep class com.example.data.ocr.** { *; }

# Keep ML Kit (barcode + text) — uses native libraries via JNI
-keep class com.google.mlkit.** { *; }

# Keep OpenCV Java wrappers
-keep class org.opencv.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeException, Exception

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# General Android rules
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }