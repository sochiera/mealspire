package com.mealspire.app.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.MealSlot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowNotificationManager;

/**
 * Firing the receiver posts a meal-reminder notification with a non-empty title
 * and some proposals in the body.
 */
@RunWith(RobolectricTestRunner.class)
public class MealReminderReceiverRobolectricTest {

    private ShadowNotificationManager shadowNotificationManager() {
        Context context = ApplicationProvider.getApplicationContext();
        return Shadows.shadowOf(
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Test
    public void postsNotificationForTheMeal() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(MealReminderReceiver.ACTION_FIRE)
                .putExtra(MealReminderReceiver.EXTRA_MEAL_INDEX, MealSlot.LUNCH.mealIndex());

        new MealReminderReceiver().onReceive(context, intent);

        assertEquals(1, shadowNotificationManager().size());
        Notification posted = shadowNotificationManager().getAllNotifications().get(0);
        CharSequence title = posted.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = posted.extras.getCharSequence(Notification.EXTRA_TEXT);
        assertTrue("title should name the meal",
                title != null && title.toString().contains("Obiad"));
        assertFalse("body should list proposals",
                text == null || text.toString().trim().isEmpty());
    }

    @Test
    public void ignoresUnknownMealIndex() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(MealReminderReceiver.ACTION_FIRE)
                .putExtra(MealReminderReceiver.EXTRA_MEAL_INDEX, 99);

        new MealReminderReceiver().onReceive(context, intent);

        assertEquals(0, shadowNotificationManager().size());
    }
}
