-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Signature,InnerClasses,EnclosingMethod

# Models are serialized across modules and are intentionally kept readable for
# early crash triage while the persistence schema is still forming.
-keep class com.evomrdm.iptvbox.core.model.** { *; }

# Media3 uses reflective access in a few integration points.
-keep class androidx.media3.** { *; }
