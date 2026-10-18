# ProGuard / R8 rules for Glip Go Consumer App
# Keeps reflection, data models, coroutines, and serialization intact

# Keep Kotlin Coroutines & Serialization
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Data Models & Network DTOs
-keep class com.glip.app.data.model.** { *; }
-keep class com.glip.app.domain.model.** { *; }
-keep class com.glip.app.data.remote.** { *; }
-keep class com.glip.app.ads.** { *; }

# Keep OpenStreetMap (OSMDroid)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep MultiDex
-keep class androidx.multidex.** { *; }

# Keep Jetpack Compose & Navigation
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
