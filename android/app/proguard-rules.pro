# Flutter Wrapper
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.**  { *; }
-keep class io.flutter.util.**  { *; }
-keep class io.flutter.view.**  { *; }
-keep class io.flutter.**  { *; }
-keep class io.flutter.plugins.**  { *; }

# Aliyun Player SDK
-keep class com.aliyun.** { *; }
-keep class com.alivc.** { *; }
-dontwarn com.aliyun.**
-dontwarn com.alivc.**

# Google Play Core (for Flutter deferred components)
-dontwarn com.google.android.play.core.**
-keep class com.google.android.play.core.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep source file and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
