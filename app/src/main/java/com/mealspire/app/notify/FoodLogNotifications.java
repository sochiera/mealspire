package com.mealspire.app.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import com.mealspire.app.MainActivity;
import com.mealspire.app.R;
import com.mealspire.app.domain.FoodLogReminder;
import com.mealspire.app.storage.MealspireDataReset;
import com.mealspire.app.ui.Ui;

/**
 * The evening "Co dzisiaj jedliście?" notification — its own channel, separate
 * from the 8/12/18 meal ideas. On Android 7+ the answer is typed straight into
 * the notification (direct reply → {@link FoodLogReceiver}); tapping it, or any
 * tap on Android 6, opens the app's answer dialog instead.
 */
public final class FoodLogNotifications {

    static final String CHANNEL_ID = "food_log";
    private static final String CHANNEL_NAME = "Co dzisiaj jedliście?";
    /** Far from the meal reminders' ids (0–2). */
    public static final int NOTIFICATION_ID = 100;
    static final String KEY_REPLY_TEXT = "food_log_text";
    private static final long SAVED_TIMEOUT_MS = 10_000L;

    private FoodLogNotifications() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Wieczorne pytanie, co było dziś jedzone.");
            manager.createNotificationChannel(channel);
        }
    }

    /** Posts the question with a reply field (Android 7+). */
    public static void showQuestion(Context context) {
        NotificationManager manager = manager(context);
        if (manager == null) {
            return;
        }
        Notification.Builder builder = base(context, FoodLogReminder.TEXT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            RemoteInput input = new RemoteInput.Builder(KEY_REPLY_TEXT)
                    .setLabel(FoodLogReminder.REPLY_HINT)
                    .build();
            Notification.Action reply = new Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification),
                    FoodLogReminder.REPLY_LABEL, replyIntent(context))
                    .addRemoteInput(input)
                    .build();
            builder.addAction(reply);
        }
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    /** Replaces the question with a short "saved" note (stops the reply spinner). */
    public static void showSaved(Context context) {
        NotificationManager manager = manager(context);
        if (manager == null) {
            return;
        }
        Notification.Builder builder = base(context, FoodLogReminder.SAVED_TEXT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(SAVED_TIMEOUT_MS);
        }
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancel(Context context) {
        NotificationManager manager = manager(context);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private static NotificationManager manager(Context context) {
        ensureChannel(context);
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static Notification.Builder base(Context context, String text) {
        return builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Ui.ACCENT)
                .setContentTitle(FoodLogReminder.TITLE)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context));
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(context, CHANNEL_ID);
        }
        return new Notification.Builder(context);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent open = new Intent(context, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_FOOD_LOG, true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, NOTIFICATION_ID, open, flags);
    }

    private static PendingIntent replyIntent(Context context) {
        Intent reply = new Intent(context, FoodLogReceiver.class);
        reply.setAction(FoodLogReceiver.ACTION_REPLY);
        long generation = MealspireDataReset.generation(context);
        // Different generations must have different PendingIntent identities.
        reply.setData(Uri.parse("mealspire:food-log-reply/" + generation));
        reply.putExtra(FoodLogReceiver.EXTRA_DATA_GENERATION, generation);
        // The system writes the typed text into the intent, so it must stay mutable.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, NOTIFICATION_ID, reply, flags);
    }
}
