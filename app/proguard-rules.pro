# ProGuard / R8 rules for Haze Go Consumer App
# Preserves stack traces, reflection, data models, coroutines, and serialization

# Preserve Line Numbers and Source File Attributes for accurate Play Console Crash Reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin Coroutines & Reflection
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Data Models, Remote DTOs & Repositories
-keep class com.haze.app.data.model.** { *; }
-keep class com.haze.app.domain.model.** { *; }
-keep class com.haze.app.data.remote.** { *; }
-keep class com.haze.app.ads.** { *; }

# Keep OpenStreetMap (OSMDroid)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep MultiDex
-keep class androidx.multidex.** { *; }

# Keep Jetpack Compose & Navigation
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# Google Play Services & Maps (for seamless SDK integration)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
