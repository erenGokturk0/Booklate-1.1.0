# PdfBox / BouncyCastle
-keep class org.apache.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.logging.**
-dontwarn javax.**
-dontwarn java.awt.**
# PDFBox'un opsiyonel JPEG2000 (JPX) kod çözücüsü; bu projede kullanılmıyor.
# Sayfalar PdfRenderer ile çiziliyor, PDFBox yalnızca metin/koordinat çıkarıyor.
-dontwarn com.gemalto.jp2.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.erengokturk.booklate.**$$serializer { *; }
-keepclassmembers class com.erengokturk.booklate.** {
    *** Companion;
}
-keepclasseswithmembers class com.erengokturk.booklate.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
