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

# ProGuard rules — shrink & optimize without renaming (-dontobfuscate).

-dontobfuscate
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ViewModels are instantiated reflectively by ViewModelProvider.Factory.
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Fragments are recreated by name across config changes / process death.
-keep class * extends androidx.fragment.app.Fragment {
    <init>();
}

# Gson models.
-keep class com.aefyr.sai.model.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# flexfilter (local aar): Gson polymorphism + reflective factories.
-keep class com.aefyr.flexfilter.** { *; }

# Loaded via Class.forName.
-keep class * implements com.aefyr.sai.installerx.resolver.urimess.UriHostFactory {
    <init>();
}

# Shizuku / Sui.
-keep class moe.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn moe.shizuku.**
-dontwarn rikka.shizuku.**