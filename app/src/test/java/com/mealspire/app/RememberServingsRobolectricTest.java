package com.mealspire.app;

import static org.junit.Assert.assertEquals;

import android.widget.Spinner;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.storage.SharedPreferencesAppSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * The portion spinner starts on the remembered number of people, and changing
 * it persists the new default for next launch.
 */
@RunWith(RobolectricTestRunner.class)
public class RememberServingsRobolectricTest {

    @Test
    public void spinnerStartsOnRememberedServings() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .saveDefaultServings(5);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Spinner portions = activity.findViewById(R.id.portion_spinner);

        // position = servings - 1
        assertEquals(4, portions.getSelectedItemPosition());
    }

    @Test
    public void changingSpinnerPersistsDefault() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Spinner portions = activity.findViewById(R.id.portion_spinner);

        portions.setSelection(3); // 4 osoby

        int saved = new SharedPreferencesAppSettings(
                ApplicationProvider.getApplicationContext()).loadDefaultServings();
        assertEquals(4, saved);
    }
}
