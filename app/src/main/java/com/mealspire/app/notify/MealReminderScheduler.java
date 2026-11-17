package com.mealspire.app.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mealspire.app.domain.FoodLogReminder;
import com.mealspire.app.domain.MealSlot;
import com.mealspire.app.domain.NextAlarmTime;
import com.mealspire.app.storage.SharedPreferencesAppSettings;

import java.util.TimeZone;

/**
 * Schedules the three daily meal reminders (8:00, 12:00, 18:00) and the
 * evening "Co dzisiaj jedliście?" question (hour from settings) with
 * {@link AlarmManager}. Uses inexact repeating daily alarms anchored at the next
 * occurrence of each hour, which needs no exact-alarm permission. Scheduling is
 * idempotent: a stable request code per slot plus {@code FLAG_UPDATE_CURRENT}
 * means re-running just refreshes the existing alarms instead of duplicating them.
 */
public final class MealReminderScheduler {

    private final NextAlarmTime nextAlarmTime = new NextAlarmTime();

    public void scheduleAll(Context context) {
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (MealSlot slot : MealSlot.values()) {
            long triggerAt = nextAlarmTime.nextOccurrence(now, slot.hour(), TimeZone.getDefault());
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt,
                    AlarmManager.INTERVAL_DAY, pendingIntentFor(context, slot));
        }
        scheduleFoodLog(context, alarmManager, now);
    }

    /** (Re)schedules the evening food-log question, or cancels it when switched off. */
    private void scheduleFoodLog(Context context, AlarmManager alarmManager, long now) {
        int hour = new SharedPreferencesAppSettings(context).loadFoodLogHour();
        PendingIntent ask = foodLogIntent(context);
        if (hour == FoodLogReminder.DISABLED) {
            alarmManager.cancel(ask);
            return;
        }
        long triggerAt = nextAlarmTime.nextOccurrence(now, hour, TimeZone.getDefault());
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt,
                AlarmManager.INTERVAL_DAY, ask);
    }

    static PendingIntent foodLogIntent(Context context) {
        Intent intent = new Intent(context, FoodLogReceiver.class);
        intent.setAction(FoodLogReceiver.ACTION_ASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, FoodLogNotifications.NOTIFICATION_ID,
                intent, flags);
    }

    static PendingIntent pendingIntentFor(Context context, MealSlot slot) {
        Intent intent = new Intent(context, MealReminderReceiver.class);
        intent.setAction(MealReminderReceiver.ACTION_FIRE);
        intent.putExtra(MealReminderReceiver.EXTRA_MEAL_INDEX, slot.mealIndex());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, slot.mealIndex(), intent, flags);
    }
}
