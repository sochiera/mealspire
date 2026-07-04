package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.storage.SharedPreferencesAppSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Systemowe „Cofnij" cofa o jeden widok (przepis → propozycje → ekran startowy)
 * zamiast od razu zamykać aplikację. Ścieżka wyłącznie offline — AI nie jest
 * wołane.
 */
@RunWith(RobolectricTestRunner.class)
public class BackNavigationRobolectricTest {

    private MainActivity launch() {
        // Back within the quiz is covered by OnboardingRobolectricTest.
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    @Test
    public void backZPrzepisuWracaDoPropozycji() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        activity.<Button>findViewById(R.id.accept_button).performClick();
        assertNotNull(activity.findViewById(R.id.recipe_details));

        activity.onBackPressed();

        assertFalse(activity.isFinishing());
        assertNull(activity.findViewById(R.id.recipe_details));
        assertNotNull(activity.findViewById(R.id.accept_button));
    }

    @Test
    public void backZPropozycjiWracaNaEkranStartowy() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        assertNotNull(activity.findViewById(R.id.accept_button));

        activity.onBackPressed();

        assertFalse(activity.isFinishing());
        assertNull(activity.findViewById(R.id.accept_button));
        assertNull(activity.findViewById(R.id.refresh_button));
    }

    @Test
    public void backZEkranuStartowegoKonczyActivity() {
        MainActivity activity = launch();

        activity.onBackPressed();

        assertTrue(activity.isFinishing());
    }

    @Test
    public void dwaRazyBackZPrzepisuKonczyNaEkranieStartowym() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_dinner_button).performClick();
        activity.<Button>findViewById(R.id.accept_button).performClick();

        activity.onBackPressed();
        activity.onBackPressed();

        assertFalse(activity.isFinishing());
        assertNull(activity.findViewById(R.id.accept_button));

        activity.onBackPressed();
        assertTrue(activity.isFinishing());
    }
}
