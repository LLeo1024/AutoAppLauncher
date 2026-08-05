# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
