package com.mealspire.app.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.FoodLog;
import com.mealspire.app.domain.FoodLogReminder;
import com.mealspire.app.storage.MealspireDataReset;
import com.mealspire.app.storage.SharedPreferencesFoodLogStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowNotificationManager;

/**
 * Wieczorne „Co dzisiaj jedliście?": alarm wysyła pytanie z polem odpowiedzi,
 * a odpowiedź wpisana w powiadomieniu trafia do dziennika.
 */
@RunWith(RobolectricTestRunner.class)
public class FoodLogReceiverRobolectricTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private ShadowNotificationManager notifications() {
        return Shadows.shadowOf(
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Test
    public void alarmPostsTheQuestionWithAReplyField() {
        new FoodLogReceiver().onReceive(context, new Intent(FoodLogReceiver.ACTION_ASK));

        Notification posted = notifications().getNotification(
                FoodLogNotifications.NOTIFICATION_ID);
        assertNotNull(posted);
        assertEquals(FoodLogReminder.TITLE,
                posted.extras.getCharSequence(Notification.EXTRA_TITLE).toString());
        assertEquals(FoodLogNotifications.CHANNEL_ID, posted.getChannelId());
        assertEquals(1, posted.actions.length);
        assertEquals(FoodLogNotifications.KEY_REPLY_TEXT,
                posted.actions[0].getRemoteInputs()[0].getResultKey());
    }

    @Test
    public void replyIsSavedToTheFoodLog() {
        new FoodLogReceiver().onReceive(context, reply("  pierogi ruskie  "));

        FoodLog log = new SharedPreferencesFoodLogStore(context).load();
        assertEquals(1, log.entries().size());
        assertEquals("pierogi ruskie", log.entries().get(0).getText());
        Notification saved = notifications().getNotification(
                FoodLogNotifications.NOTIFICATION_ID);
        assertTrue(saved.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                .startsWith("Zapisane"));
    }

    @Test
    public void replyQueuedBeforeResetCannotRestoreFoodLog() {
        Intent stale = reply("stary wpis");
        assertTrue(MealspireDataReset.reset(context));

        FoodLogReceiver receiver = new FoodLogReceiver();
        receiver.onReceive(context, stale);
        assertTrue(new SharedPreferencesFoodLogStore(context).load().isEmpty());

        receiver.onReceive(context, reply("nowy wpis"));
        assertEquals(1, new SharedPreferencesFoodLogStore(context).load().entries().size());
    }

    private Intent reply(String text) {
        Intent reply = new Intent(FoodLogReceiver.ACTION_REPLY);
        reply.putExtra(FoodLogReceiver.EXTRA_DATA_GENERATION, MealspireDataReset.generation(context));
        Bundle results = new Bundle();
        results.putCharSequence(FoodLogNotifications.KEY_REPLY_TEXT, text);
        RemoteInput.addResultsToIntent(new RemoteInput[]{
                new RemoteInput.Builder(FoodLogNotifications.KEY_REPLY_TEXT).build()},
                reply, results);
        return reply;
    }

    @Test
    public void emptyReplySavesNothing() {
        new FoodLogReceiver().onReceive(context, new Intent(FoodLogReceiver.ACTION_REPLY));
        assertTrue(new SharedPreferencesFoodLogStore(context).load().isEmpty());
    }
}
