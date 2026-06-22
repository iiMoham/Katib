# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.katib.app.**$$serializer { *; }
-keepclassmembers class com.katib.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.katib.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
