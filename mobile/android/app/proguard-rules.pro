# Keep Mestxa native JNI methods and bridge models
-keep class com.mestxa.app.engine.** { *; }
-keepclassmembers class com.mestxa.app.engine.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep SQLCipher encrypted database classes
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# Keep Stream WebRTC Native Audio classes
-keep class io.getstream.webrtc.android.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep OkHttp & Okio WebSocket classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
