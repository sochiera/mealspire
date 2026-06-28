package com.mealspire.app.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mealspire.app.domain.BuiltInRecipes;
import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.MealNotificationContent;
import com.mealspire.app.domain.MealSlot;
import com.mealspire.app.domain.OfflineProposalGenerator;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.TasteProfile;
import com.mealspire.app.domain.TasteProfiler;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fired by the daily alarms (8/12/18). Picks a few offline proposals for the
 * meal — the same pipeline as the screen — and posts a notification. Work is
 * deliberately cheap and offline (no network, no API key) so it always succeeds
 * within the broadcast time limit.
 */
public final class MealReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.mealspire.app.action.MEAL_REMINDER";
    public static final String EXTRA_MEAL_INDEX = "meal_index";
    private static final int PROPOSAL_COUNT = 3;

    private final OfflineProposalGenerator proposalGenerator = new OfflineProposalGenerator();
    private final TasteProfiler tasteProfiler = new TasteProfiler();

    @Override
    public void onReceive(Context context, Intent intent) {
        int mealIndex = intent != null ? intent.getIntExtra(EXTRA_MEAL_INDEX, -1) : -1;
        MealSlot slot = MealSlot.byMealIndex(mealIndex);
        if (slot == null) {
            return;
        }

        UserPreferences preferences = new SharedPreferencesPreferenceStore(context).load();
        Cookbook cookbook = new SharedPreferencesCookbookStore(context).load();
        TasteProfile profile = tasteProfiler.build(
                preferences.getLikes(), BuiltInRecipes.detailsByTitle(cookbook));

        List<Recipe> chosen = proposalGenerator.generate(
                BuiltInRecipes.forMeal(mealIndex), cookbook, preferences,
                profile, PROPOSAL_COUNT, new Random());

        List<String> names = new ArrayList<>();
        for (Recipe recipe : chosen) {
            names.add(recipe.getTitle());
        }

        MealNotifications.show(context, slot,
                MealNotificationContent.forMeal(slot.label(), names));
    }
}
