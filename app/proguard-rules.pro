# =============================================================================
# BLUE PLAYER - PROGUARD / R8 RULES
# =============================================================================
# Organized by concern. Each section explains what it protects and why.
# =============================================================================


# -----------------------------------------------------------------------------
# 1. GENERAL ATTRIBUTES
# Keep metadata needed by reflection, Compose compiler and Kotlin coroutines.
# -----------------------------------------------------------------------------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions


# -----------------------------------------------------------------------------
# 2. COMPOSE RUNTIME (fixes the main issue from logcat)
# "Class androidx.compose.runtime.snapshots.SnapshotStateList failed lock
# verification and will run slower"
# Snapshot system uses reflection on internal fields; R8 must keep them.
# -----------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }
-keep class androidx.compose.runtime.collection.** { *; }
-dontwarn androidx.compose.runtime.**

# Compose UI: layout system, modifiers, platform adapters use reflection
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.ui.**

# Compose animation: keyframe and transition internals reflect on fields
-keep class androidx.compose.animation.** { *; }
-dontwarn androidx.compose.animation.**

# Compose foundation: lazy lists, gestures, scrolling use reflection
-keep class androidx.compose.foundation.** { *; }
-dontwarn androidx.compose.foundation.**

# Material 3 components: some ripple/surface effects use reflection
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# Material icons extended: keep icon metadata
-keep class androidx.compose.material.icons.** { *; }


# -----------------------------------------------------------------------------
# 3. KOTLIN COROUTINES
# Compose heavily uses coroutines; internal dispatchers use reflection.
# -----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.flow.**


# -----------------------------------------------------------------------------
# 4. KOTLIN STDLIB & METADATA
# Needed for reflection-based serialization and Compose generated code.
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlin.reflect.jvm.internal.**


# -----------------------------------------------------------------------------
# 5. JETPACK NAVIGATION
# Navigation arguments use reflection on route types.
# -----------------------------------------------------------------------------
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**


# -----------------------------------------------------------------------------
# 6. GLIDE
# Glide 4.16 ships its own consumer ProGuard rules inside the AAR, so the
# old manual enum/rewinder rules match nothing and only produce warnings.
# A warning silencer is enough here.
# -----------------------------------------------------------------------------
-dontwarn com.bumptech.glide.**


# -----------------------------------------------------------------------------
# 7. MEDIA3 / EXOPLAYER
# Media3 AARs include consumer rules for their internals. This safety net
# keeps only the reflective public API our session, notification and
# widget code touches, instead of disabling minification for the whole
# library (which the old blanket rule did).
# -----------------------------------------------------------------------------
-keep class androidx.media3.session.MediaSession { *; }
-keep class androidx.media3.session.MediaSession$Callback { *; }
-keep class androidx.media3.session.MediaSession$ConnectionResult { *; }
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaController { *; }
-keep class androidx.media3.session.DefaultMediaNotificationProvider { *; }
-keep class androidx.media3.session.MediaNotification { *; }
-keep class androidx.media3.session.MediaNotification$Provider { *; }
-keep class androidx.media3.session.CommandButton { *; }
-keep class androidx.media3.session.SessionCommand { *; }
-keep class androidx.media3.session.SessionCommands { *; }
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.MediaMetadata { *; }
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-dontwarn androidx.media3.**


# -----------------------------------------------------------------------------
# 8. PROJECT NATIVE CODE
# Generic JNI rule: keeps EVERY class that declares native methods, so it
# works no matter which class hosts the JNI bridge (NativeBassProcessor,
# BassDsp, future engines). The old rule pointed at a non-existent
# NativeAudioEngine class and produced "Unresolved class name".
# -----------------------------------------------------------------------------
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.blueplayer.app.audio.** { *; }
-keep class com.blueplayer.core.player.BassDsp { *; }


# -----------------------------------------------------------------------------
# 9. ANDROID PLATFORM GREYLIST SUPPRESSION
# These warnings are safe to ignore on Android 9+ where greylist access is
# permitted for apps targeting API 28+ and Compose uses them legitimately.
# -----------------------------------------------------------------------------
-dontwarn android.view.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**


# -----------------------------------------------------------------------------
# 10. DOCUMENT FILE (used for SAF-based track deletion fallback)
# -----------------------------------------------------------------------------
-keep class androidx.documentfile.** { *; }
-dontwarn androidx.documentfile.**


# -----------------------------------------------------------------------------
# 11. ACTIVITY / FRAGMENT RESULT API
# rememberLauncherForActivityResult and ActivityResultContracts use reflection
# on contract classes; they must survive minification.
# -----------------------------------------------------------------------------
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }
-dontwarn androidx.activity.**
-dontwarn androidx.fragment.**


# -----------------------------------------------------------------------------
# 12. LIFECYCLE & VIEWMODEL
# Compose ViewModel integration uses reflection on factory constructors.
# -----------------------------------------------------------------------------
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-dontwarn androidx.lifecycle.**


# -----------------------------------------------------------------------------
# 13. APPWIDGET (home-screen widget RemoteViews binding)
# AppWidgetProvider must keep its class name for the manifest receiver.
# -----------------------------------------------------------------------------
-keep class com.blueplayer.app.widget.** { *; }


# -----------------------------------------------------------------------------
# 14. CORE APP CLASSES WE MUST NEVER OBFUSCATE
# - MainActivity (launcher target in AndroidManifest)
# - MusicPlaybackService (bound via MediaSessionService)
# - BluePlayerApplication (android:name in manifest)
# - DI container / repositories accessed via reflection
# -----------------------------------------------------------------------------
-keep class com.blueplayer.app.MainActivity { *; }
-keep class com.blueplayer.app.MusicPlaybackService { *; }
-keep class com.blueplayer.app.BluePlayerApplication { *; }
-keep class com.blueplayer.app.di.** { *; }


# -----------------------------------------------------------------------------
# 15. ENUMS — keep values() and valueOf() for serialization
# Used by SharedPreferencesSettingsRepository and SettingsBackup.
# -----------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# -----------------------------------------------------------------------------
# 16. PARCELABLE (Track, Playlist, Bookmark cross-IPC)
# -----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}


# -----------------------------------------------------------------------------
# 17. SERIALIZABLE (Room / JSON round-trip)
# -----------------------------------------------------------------------------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# -----------------------------------------------------------------------------
# 18. SUPPRESS NOISY WARNINGS FROM MIUI / OEM LOGGING
# The "getFolderSize NullPointerException" warnings come from MIUI's Perf
# analyzer and are not part of our code — safe to silence.
# -----------------------------------------------------------------------------
-dontwarn com.miui.**
-dontwarn miui.**