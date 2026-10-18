# Macrobenchmark instrumentation shares the target application's classloader.
# Keep its tracing and Kotlin runtime dependencies in the isolated target APK.
-keep class androidx.tracing.** { *; }
-keep class kotlin.** { *; }
