# ── kotlinx.serialization ────────────────────────────────────────────────
# Keep generated serializers and the @Serializable companion accessors.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.taleson2wheels.app.**$$serializer { *; }
-keepclassmembers class com.taleson2wheels.app.** {
    *** Companion;
}

# ── Retrofit / OkHttp ────────────────────────────────────────────────────
# Retrofit reads generic signatures and annotations via reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Suspend service interfaces keep their Kotlin metadata.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowobfuscation interface <1>

# OkHttp pulls in optional platform classes that are safe to ignore.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
