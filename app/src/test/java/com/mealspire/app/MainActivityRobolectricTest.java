package com.mealspire.app;

import static org.junit.Assert.assertFalse;
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
 * emulator). They cover offline interactions only — the AI button is never
 * clicked here, so no network call happens.
 */
@RunWith(RobolectricTestRunner.class)
public class MainActivityRobolectricTest {

    private MainActivity launch() {
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    @Test
    public void showsARecipeTitleOnLaunch() {
        MainActivity activity = launch();
        TextView title = activity.findViewById(R.id.recipe_title);
        assertFalse(title.getText().toString().trim().isEmpty());
    }

    @Test
    public void generateButtonKeepsARecipeVisible() {
        MainActivity activity = launch();
        // No API key in the test build, so generate falls back to an offline pick.
        Button generate = activity.findViewById(R.id.generate_button);
        generate.performClick();
        TextView title = activity.findViewById(R.id.recipe_title);
        assertFalse(title.getText().toString().trim().isEmpty());
    }

    @Test
    public void likeButtonPersistsPreference() {
        MainActivity activity = launch();
        TextView title = activity.findViewById(R.id.recipe_title);
        String shownDish = title.getText().toString();

        activity.<Button>findViewById(R.id.like_button).performClick();

        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue(saved.getLikes().contains(shownDish));
    }
}
