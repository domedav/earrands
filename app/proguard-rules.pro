# R8 rules for Ballanceometer

# Prevent class renaming (breaks Glance resource lookups by launcher)
-dontobfuscate

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Glance / Widget (ActionCallback invoked by class name from widget host)
-keep class com.domedav.ballanceometer.widget.** { *; }
-keep class androidx.glance.** { *; }

# Preserve R classes (resource lookups)
-keep class **.R { *; }
-keep class **.R$* { *; }

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
