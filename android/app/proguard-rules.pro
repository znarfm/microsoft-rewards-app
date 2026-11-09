# Keep Android 16 Notification.ProgressStyle and Builder methods accessed via reflection
-keep class android.app.Notification$Builder { *; }
-keep class android.app.Notification$ProgressStyle { *; }
-keep class com.spin311.microsoft_automatic_rewards.** { *; }

# Keep ViewModel constructors invoked via reflection by ViewModelProvider
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
