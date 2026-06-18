package com.mealspire.app;

import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SaveToCookbookRobolectricTest {

    @Test
    public void saveButtonAddsShownRecipeToCookbook() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        String shownDish = ((TextView) activity.findViewById(R.id.recipe_title))
                .getText().toString();

        activity.<Button>findViewById(R.id.save_button).performClick();

        Cookbook cookbook = new SharedPreferencesCookbookStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue(cookbook.contains(shownDish));
    }
}
