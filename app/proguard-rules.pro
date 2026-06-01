# Dogear release rules. R8 full mode is on by default with AGP 8.

# Keep Room generated implementations referenced via reflection-free codegen (safe defaults).
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Hilt/Dagger generate code; keep generated component metadata.
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules** { *; }

# Kotlin metadata for reflection-light libraries.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

# Baseline profiles + Compose are handled by their consumer rules.
