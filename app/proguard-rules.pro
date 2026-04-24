# =============================================
# DropDate — ProGuard / R8 rules for release
# =============================================

# ---- Kotlin Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kmno.dropdate.**$$serializer { *; }
-keepclassmembers class com.kmno.dropdate.** {
    *** Companion;
}
-keepclasseswithmembers class com.kmno.dropdate.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Retrofit ----
-dontwarn retrofit2.**
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep generic signature of Call, Response (R8 full mode strips it)
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# ---- OkHttp ----
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ---- Hilt / Dagger ----
-dontwarn dagger.hilt.android.internal.**

# ---- Coil ----
-dontwarn coil3.**

# ---- Keep line numbers for crash reports ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
