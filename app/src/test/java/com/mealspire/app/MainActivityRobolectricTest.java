package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Robolectric tests run the real Activity + SharedPreferences on the JVM (no
 * emulator). They cover the offline flow only — the AI is never called here.
 */
@RunWith(RobolectricTestRunner.class)
public class MainActivityRobolectricTest {

    private MainActivity launch() {
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    @Test
    public void showsMealButtonsAndNoProposalsOnLaunch() {
        MainActivity activity = launch();
        assertNotNull(activity.findViewById(R.id.meal_breakfast_button));
        assertNotNull(activity.findViewById(R.id.meal_lunch_button));
        assertNotNull(activity.findViewById(R.id.meal_dinner_button));
        // Nothing is proposed until the user picks a meal.
        assertNull(activity.findViewById(R.id.accept_button));
    }

    @Test
    public void pickingAMealShowsProposals() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();

        // Proposal cards are built: the first card exposes title + actions.
        assertNotNull(activity.findViewById(R.id.accept_button));
        TextView title = activity.findViewById(R.id.recipe_title);
        assertFalse(title.getText().toString().trim().isEmpty());
    }

    @Test
    public void likeButtonPersistsPreference() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();

        String shownDish = ((TextView) activity.findViewById(R.id.recipe_title))
                .getText().toString();
        activity.<Button>findViewById(R.id.like_button).performClick();

        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue(saved.getLikes().contains(shownDish));
    }

    @Test
    public void showRecipeRevealsFullRecipeOffline() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.meal_dinner_button).performClick();
        activity.<Button>findViewById(R.id.accept_button).performClick();

        TextView details = activity.findViewById(R.id.recipe_details);
        assertFalse(details.getText().toString().trim().isEmpty());
        assertNotNull(activity.findViewById(R.id.back_button));
    }
}
