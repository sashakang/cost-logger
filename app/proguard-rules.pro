# Add project specific ProGuard rules here.
# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Room entities
-keep class com.notificationlogger.data.** { *; }
