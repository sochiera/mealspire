package com.mealspire.app;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.storage.SharedPreferencesAppSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * The chosen number of people is remembered between launches so the user does
 * not have to re-pick it every time.
 */
@RunWith(RobolectricTestRunner.class)
public class AppSettingsRobolectricTest {

    private AppSettings newStore() {
        return new SharedPreferencesAppSettings(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void defaultsToTwoPeopleWhenNothingSaved() {
        assertEquals(2, newStore().loadDefaultServings());
    }

    @Test
    public void remembersSavedServingsAcrossInstances() {
        newStore().saveDefaultServings(4);
        // A fresh store instance reads the same persisted value.
        assertEquals(4, newStore().loadDefaultServings());
    }

    @Test
    public void ignoresOutOfRangeValuesAndKeepsDefault() {
        newStore().saveDefaultServings(0);
        assertEquals(2, newStore().loadDefaultServings());
    }
}
