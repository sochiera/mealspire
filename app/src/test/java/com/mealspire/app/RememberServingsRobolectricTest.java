package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.storage.SharedPreferencesAppSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;

/**
 * The number of people is asked exactly once. After it has been chosen the app
 * never asks again — it just shows the remembered value as a label and reuses it.
 */
@RunWith(RobolectricTestRunner.class)
public class RememberServingsRobolectricTest {

    private static final String SERVINGS_DIALOG_TITLE = "Dla ilu osób gotujesz?";

    @Test
    public void labelShowsRememberedServings() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .saveDefaultServings(5);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        TextView label = activity.findViewById(R.id.servings_label);

        assertTrue(label.getText().toString().contains("5 osób"));
    }

    @Test
    public void asksForServingsOnFirstLaunchWhenNotYetChosen() {
        Robolectric.buildActivity(MainActivity.class).setup().get();
        assertTrue("expected the one-time servings dialog on first launch",
                dialogShownWithTitle(SERVINGS_DIALOG_TITLE));
    }

    @Test
    public void doesNotAskAgainOnceChosen() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .saveDefaultServings(3);

        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertFalse("should never ask for servings again once chosen",
                dialogShownWithTitle(SERVINGS_DIALOG_TITLE));
    }

    private boolean dialogShownWithTitle(String title) {
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            if (dialog instanceof AlertDialog) {
                CharSequence shown = Shadows.shadowOf((AlertDialog) dialog).getTitle();
                if (shown != null && title.contentEquals(shown)) {
                    return true;
                }
            }
        }
        return false;
    }
}
