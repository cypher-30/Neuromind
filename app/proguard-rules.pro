# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
   public <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.Room
-keep class androidx.room.util.TableInfo
-keep class androidx.room.util.TableInfo$Column
-keep class androidx.room.util.TableInfo$ForeignKey
-keep class androidx.room.util.TableInfo$Index
-keep class androidx.room.util.ViewInfo
-keep class androidx.room.util.ViewInfo$ViewColumn

# DataStore
-keep class androidx.datastore.preferences.protobuf.** { *; }

# Neuromind Data Models
-keep class com.alvin.neuromind.data.** { *; }
-keep enum com.alvin.neuromind.data.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable
