# Keep Android 16 Notification.ProgressStyle and Builder methods accessed via reflection
-keep class android.app.Notification$Builder { *; }
-keep class android.app.Notification$ProgressStyle { *; }
-keep class com.znarfm.bing.** { *; }

# Keep ViewModel constructors invoked via reflection by ViewModelProvider
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep Room/WorkManager classes instantiated via reflection under AGP 9 R8
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * implements androidx.startup.Initializer {
    <init>();
}
