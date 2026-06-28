package com.mealspire.app.notify;

import static org.junit.Assert.assertEquals;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;

/**
 * The scheduler registers exactly three daily alarms (breakfast, lunch, dinner)
 * and re-running it does not pile up duplicates.
 */
@RunWith(RobolectricTestRunner.class)
public class MealReminderSchedulerRobolectricTest {

    private ShadowAlarmManager shadowAlarmManager() {
        Context context = ApplicationProvider.getApplicationContext();
        return Shadows.shadowOf((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }

    @Test
    public void schedulesThreeDailyAlarms() {
        new MealReminderScheduler().scheduleAll(ApplicationProvider.getApplicationContext());
        assertEquals(3, shadowAlarmManager().getScheduledAlarms().size());
    }

    @Test
    public void reschedulingDoesNotDuplicate() {
        Context context = ApplicationProvider.getApplicationContext();
        new MealReminderScheduler().scheduleAll(context);
        new MealReminderScheduler().scheduleAll(context);
        assertEquals("stable request codes keep it to three alarms",
                3, shadowAlarmManager().getScheduledAlarms().size());
    }
}
