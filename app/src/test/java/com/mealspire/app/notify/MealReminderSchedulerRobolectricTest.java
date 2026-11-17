package com.mealspire.app.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.FoodLogReminder;
import com.mealspire.app.storage.SharedPreferencesAppSettings;

import java.util.Calendar;

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
    public void schedulesThreeMealAlarmsAndTheEveningFoodLogQuestion() {
        new MealReminderScheduler().scheduleAll(ApplicationProvider.getApplicationContext());
        assertEquals(4, shadowAlarmManager().getScheduledAlarms().size());
    }

    @Test
    public void foodLogQuestionFiresAtTheChosenHour() {
        Context context = ApplicationProvider.getApplicationContext();
        new SharedPreferencesAppSettings(context).saveFoodLogHour(21);
        new MealReminderScheduler().scheduleAll(context);

        ShadowAlarmManager.ScheduledAlarm foodLog = null;
        for (ShadowAlarmManager.ScheduledAlarm alarm : shadowAlarmManager().getScheduledAlarms()) {
            if (alarm.operation.equals(MealReminderScheduler.foodLogIntent(context))) {
                foodLog = alarm;
            }
        }
        assertNotNull(foodLog);
        Calendar at = Calendar.getInstance();
        at.setTimeInMillis(foodLog.triggerAtTime);
        assertEquals(21, at.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, at.get(Calendar.MINUTE));
        assertEquals(AlarmManager.INTERVAL_DAY, foodLog.interval);
    }

    @Test
    public void disabledFoodLogQuestionIsCancelledButMealRemindersStay() {
        Context context = ApplicationProvider.getApplicationContext();
        new MealReminderScheduler().scheduleAll(context);
        new SharedPreferencesAppSettings(context).saveFoodLogHour(FoodLogReminder.DISABLED);
        new MealReminderScheduler().scheduleAll(context);
        assertEquals(3, shadowAlarmManager().getScheduledAlarms().size());
    }

    @Test
    public void reschedulingDoesNotDuplicate() {
        Context context = ApplicationProvider.getApplicationContext();
        new MealReminderScheduler().scheduleAll(context);
        new MealReminderScheduler().scheduleAll(context);
        assertEquals("stable request codes keep it to four alarms",
                4, shadowAlarmManager().getScheduledAlarms().size());
    }
}
