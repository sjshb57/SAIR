# Shrink and optimize, but keep names readable in crash reports.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ViewModels are built reflectively by the factories in viewmodels/factory,
# which look up specific constructor signatures.
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Fragments are recreated by name across config changes and process death.
# PreferencesActivity additionally loads PreferenceFragmentCompat subclasses
# via Class.forName on an intent extra.
-keep class * extends androidx.fragment.app.Fragment {
    <init>();
}

# Parcelable CREATOR fields are looked up reflectively by the framework.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Gson models (SaiExportedAppMeta / SaiExportedAppMeta2 are deserialized by field name).
-keep class com.aefyr.sai.model.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# flexfilter: Gson polymorphism and reflective config factories.
-keep class com.aefyr.flexfilter.** { *; }

# Room resolves generated implementations via Class.forName(name + "_Impl").
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep @androidx.room.Entity class * { *; }

# Loaded via Class.forName in InstallerXDialogFragment.
-keep class * implements com.aefyr.sai.installerx.resolver.urimess.UriHostFactory {
    <init>();
}

# Shizuku / Sui. ShizukuShell calls Shizuku.newProcess reflectively.
-keep class moe.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn moe.shizuku.**
-dontwarn rikka.shizuku.**
