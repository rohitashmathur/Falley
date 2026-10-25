# Android's optimized defaults and generated AAPT rules handle framework callbacks.
# Preserve the two manifest entry points without retaining unrelated app members.
-keep,allowoptimization class com.perfgames.ropelab.MainActivity {
    public <init>();
}
-keep,allowoptimization class com.perfgames.ropelab.ShareCardProvider {
    public <init>();
}

# Keep API 33 callback references in their SDK-guarded helper on API 24-32.
# The helper and its methods may shrink or be renamed, but must not be inlined
# or merged into MainActivity, which is verified on older Android versions.
-keep,allowshrinking,allowobfuscation class com.perfgames.ropelab.MainActivity$Api33Back {
    static java.lang.Object register(android.app.Activity, java.lang.Runnable);
    static void unregister(android.app.Activity, java.lang.Object);
}

# Likewise retain the API 26 audio-focus boundary for the API 24/25 path.
# Object fields/signatures in FalleyMusic must not acquire newer platform types
# through helper inlining or class merging; helper names remain obfuscatable.
-keep,allowshrinking,allowobfuscation class com.perfgames.ropelab.FalleyMusic$Focus26 {
    static java.lang.Object create(android.media.AudioAttributes, android.media.AudioManager$OnAudioFocusChangeListener);
    static int request(android.media.AudioManager, java.lang.Object);
    static void abandon(android.media.AudioManager, java.lang.Object);
}
# Useful source positions accompany this build's separately retained mapping.txt.
-keepattributes SourceFile,LineNumberTable

# Audio uses direct R.raw references. No dynamic resource/class lookup, JNI,
# serialization, or production reflection needs additional app-wide keep rules.
# Reflective debug instrumentation is intentionally not a release keep contract.