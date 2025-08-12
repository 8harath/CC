# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 16 KB page size compatibility rules
-keep class androidx.camera.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Additional 16 KB page size compatibility rules
-keep class org.eclipse.paho.** { *; }
-keep class com.airbnb.lottie.** { *; }

# Preserve native libraries
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Preserve Room database classes
-keep class * extends androidx.room.RoomDatabase {
    public static <fields>;
}

# Preserve ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preserve LiveData
-keep class * extends androidx.lifecycle.LiveData {
    <init>(...);
}

# Preserve Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Preserve Material Design components
-keep class com.google.android.material.** { *; }

# Preserve native method implementations
-keepclasseswithmembers class * {
    native <methods>;
}

# Preserve JNI interface methods
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}