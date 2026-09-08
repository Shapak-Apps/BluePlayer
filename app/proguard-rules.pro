-keepclasseswithmembers class com.blueplayer.app.NativeAudioEngine {
    native <methods>;
}

-keep class com.blueplayer.app.NativeAudioEngine { *; }

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**