package com.mealspire.app.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Reschedules the daily meal reminders after a reboot, since AlarmManager alarms
 * do not survive a restart.
 */
public final class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            new MealReminderScheduler().scheduleAll(context);
        }
    }
}
