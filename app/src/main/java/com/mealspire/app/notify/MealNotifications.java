package com.mealspire.app.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.mealspire.app.MainActivity;
import com.mealspire.app.domain.MealNotificationContent;
import com.mealspire.app.domain.MealSlot;

/**
 * Builds and posts the daily meal-reminder notifications and owns the
 * notification channel. Tapping a notification opens {@link MainActivity} on the
 * matching meal. Kept free of androidx so the app stays dependency-light;
 * API-level differences are handled inline.
 */
public final class MealNotifications {

    static final String CHANNEL_ID = "meal_reminders";
    private static final String CHANNEL_NAME = "Przypomnienia o posiłkach";

    private MealNotifications() {
    }

    /** Creates the notification channel (no-op below API 26 / if already created). */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Codzienne propozycje na śniadanie, obiad i kolację.");
            manager.createNotificationChannel(channel);
        }
    }

    /** Posts a notification for {@code slot} with the given title/text. */
    public static void show(Context context, MealSlot slot, MealNotificationContent content) {
        ensureChannel(context);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        Notification notification = builder(context)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setContentTitle(content.getTitle())
                .setContentText(content.getText())
                .setStyle(new Notification.BigTextStyle().bigText(content.getText()))
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context, slot))
                .build();

        manager.notify(slot.mealIndex(), notification);
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(context, CHANNEL_ID);
        }
        return new Notification.Builder(context);
    }

    private static PendingIntent openAppIntent(Context context, MealSlot slot) {
        Intent open = new Intent(context, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_MEAL_INDEX, slot.mealIndex());
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, slot.mealIndex(), open, flags);
    }
}
