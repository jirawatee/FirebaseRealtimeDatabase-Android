# Add this global rule
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.example.fdatabase.viewholder.** {*;}
-keepclassmembers class com.example.fdatabase.models.** {*;}