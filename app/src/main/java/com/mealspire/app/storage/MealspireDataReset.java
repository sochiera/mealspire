package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

/** Resets the app's data in the current Android profile. All access is on the main thread. */
public final class MealspireDataReset {
    private static final String RESET_STATE = "mealspire_reset_state";
    private static final String PENDING = "pending";
    private static final String GENERATION = "generation";
    private static final String[] USER_STORES = {
            SharedPreferencesAppSettings.PREFS_NAME,
            SharedPreferencesCookbookStore.PREFS_NAME,
            SharedPreferencesDishReactionStore.PREFS_NAME,
            SharedPreferencesFoodLogStore.PREFS_NAME,
            SharedPreferencesHouseholdProfileStore.PREFS_NAME,
            SharedPreferencesLearningStatsStore.PREFS_NAME,
            SharedPreferencesMealHistoryStore.PREFS_NAME,
            SharedPreferencesPreferenceStore.PREFS_NAME,
            SharedPreferencesTasteEventStore.PREFS_NAME,
            SharedPreferencesTasteSurveyStore.PREFS_NAME
    };

    private MealspireDataReset() {}

    public static boolean isPending(Context context) {
        return prefs(context, RESET_STATE).getBoolean(PENDING, false);
    }

    public static long generation(Context context) {
        return prefs(context, RESET_STATE).getLong(GENERATION, 0);
    }

    public static boolean reset(Context context) {
        SharedPreferences state = prefs(context, RESET_STATE);
        long generation = generation(context) + (isPending(context) ? 0 : 1);
        // Keep a durable marker until every store has made its changes durable.
        if (!state.edit().putLong(GENERATION, generation).putBoolean(PENDING, true).commit()) {
            return false;
        }
        for (String store : USER_STORES) {
            if (!prefs(context, store).edit().clear().commit()) {
                return false;
            }
        }
        if (!SharedPreferencesBackendStore.reset(context)
                || !prefs(context, SharedPreferencesChatGptSessionStore.PREFS_NAME).edit()
                        .remove(SharedPreferencesChatGptSessionStore.KEY_MODEL_CHOICE).commit()) {
            return false;
        }
        if (state.edit().remove(PENDING).commit()) {
            return true;
        }
        // A failed commit still changes the in-memory SharedPreferences view.
        state.edit().putBoolean(PENDING, true).commit();
        return false;
    }

    private static SharedPreferences prefs(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
}
