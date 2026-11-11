package com.virhuman.testsdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;

import com.virhuman.testsdk.ui.SplashActivity;

public final class AppUtils {
    private AppUtils() {}

    /**
     * Restart the application using AlarmManager to schedule SplashActivity
     * after the current process is killed. This avoids starting an Activity
     * from the process that will be terminated, which can cause crashes.
     */
    public static void restartApp(Context context) {
        if (context == null) return;

        try {
            Intent restartIntent = new Intent(context, SplashActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context.getApplicationContext(),
                    12345,
                    restartIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                long triggerAt = SystemClock.elapsedRealtime() + 300;
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
            } else {
                // Fallback: start activity with a small delay on UI thread
                new Handler(context.getMainLooper()).postDelayed(() -> {
                    try {
                        context.startActivity(restartIntent);
                    } catch (Exception ignored) {}
                }, 300);
            }

            // Finish current activity if possible. Do NOT kill process here to avoid race conditions;
            // AlarmManager will launch the SplashActivity in a fresh process.
            if (context instanceof Activity) {
                ((Activity) context).finishAffinity();
            }
        } catch (Throwable ignored) {
        }
    }
}


