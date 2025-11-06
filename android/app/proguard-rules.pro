# Fleet GPS Tracker ProGuard Rules

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# JavaMail
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-dontwarn javax.mail.**
-dontwarn javax.activation.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.fleet.gpstracker.models.** { *; }

# Bluetooth
-keep class android.bluetooth.** { *; }
