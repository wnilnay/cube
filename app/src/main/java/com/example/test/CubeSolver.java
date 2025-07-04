package com.example.test;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;

public class CubeSolver extends Application {
    private static WeakReference<Activity> topActivityRef;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                topActivityRef = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (topActivityRef != null && topActivityRef.get() == activity) {
                    topActivityRef = null;
                }
            }
        });
    }

    @Nullable
    public static Activity getTopActivity() {
        return topActivityRef == null ? null : topActivityRef.get();
    }
} 