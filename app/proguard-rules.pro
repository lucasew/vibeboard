# Project ProGuard / R8 rules for release builds.
# Default Android optimize rules come from proguard-android-optimize.txt.

# Keep line numbers so reportError / Log stack traces stay useful after minify.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
