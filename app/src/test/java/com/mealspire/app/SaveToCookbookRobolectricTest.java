package com.mealspire.app;

import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

/**
 * Saving lives behind the "Więcej…" menu now. This drives that menu and clicks
 * the "Zapisz…" item, then asserts the dish landed in the cookbook.
 */
@RunWith(RobolectricTestRunner.class)
public class SaveToCookbookRobolectricTest {

    @Test
    public void saveItemInMoreMenuAddsShownRecipeToCookbook() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        // Pick a meal, then open a proposal's full recipe so "Zapisz" is offered.
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        activity.<Button>findViewById(R.id.accept_button).performClick();
        String shownDish = ((TextView) activity.findViewById(R.id.recipe_title))
                .getText().toString();

        activity.<Button>findViewById(R.id.more_button).performClick();
        clickDialogItemStartingWith("Zapisz");

        Cookbook cookbook = new SharedPreferencesCookbookStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue(cookbook.contains(shownDish));
    }

    private void clickDialogItemStartingWith(String prefix) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        ListAdapter adapter = dialog.getListView().getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (String.valueOf(adapter.getItem(i)).startsWith(prefix)) {
                dialog.getListView().performItemClick(null, i, i);
                return;
            }
        }
        throw new AssertionError("No menu item starting with: " + prefix);
    }
}
