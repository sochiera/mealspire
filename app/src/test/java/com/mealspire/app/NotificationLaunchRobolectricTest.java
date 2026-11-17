package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.storage.SharedPreferencesAppSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;

/**
 * Launching the app sets up the daily reminders, and opening it from a reminder
 * notification jumps straight to that meal's proposals.
 */
@RunWith(RobolectricTestRunner.class)
public class NotificationLaunchRobolectricTest {

    @Test
    public void schedulesRemindersOnLaunch() {
        Robolectric.buildActivity(MainActivity.class).setup().get();

        Context context = ApplicationProvider.getApplicationContext();
        ShadowAlarmManager shadow = Shadows.shadowOf(
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        assertEquals("8/12/18 + evening food-log question", 4,
                shadow.getScheduledAlarms().size());
    }

    @Test
    public void notificationIntentOpensThatMeal() {
        // After onboarding a reminder tap goes straight to that meal; the fresh
        // install case is covered by OnboardingRobolectricTest.
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
                .putExtra(MainActivity.EXTRA_MEAL_INDEX, 1);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent)
                .setup().get();

        // Proposals for the requested meal are shown immediately.
        assertNotNull(activity.findViewById(R.id.accept_button));
        TextView title = activity.findViewById(R.id.recipe_title);
        assertFalse(title.getText().toString().trim().isEmpty());
    }

    @Test
    public void foodLogNotificationOpensTheAnswerDialogAndSavesTheAnswer() {
        Context context = ApplicationProvider.getApplicationContext();
        new SharedPreferencesAppSettings(context).markOnboardingDone();
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_FOOD_LOG, true);

        Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        android.app.AlertDialog dialog =
                org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        android.widget.EditText input = dialog.findViewById(R.id.food_log_input);
        assertNotNull(input);
        input.setText("jajecznica, rosół");
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        com.mealspire.app.domain.FoodLog log =
                new com.mealspire.app.storage.SharedPreferencesFoodLogStore(context).load();
        assertEquals(1, log.entries().size());
        assertEquals("jajecznica, rosół", log.entries().get(0).getText());
    }
}
