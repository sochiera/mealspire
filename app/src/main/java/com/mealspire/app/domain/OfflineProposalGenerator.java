package com.mealspire.app.domain;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * The offline meal-picking pipeline in one place: build the candidate pool
 * (built-ins + cookbook, disliked excluded), shuffle for variety, set aside
 * dishes shown within the last {@link #RECENCY_WINDOW_MILLIS} so each round
 * favours something new, then let the {@link VariedMealPicker} take at most
 * one taste-led pick and keep the rest varied. Shared by the on-screen
 * "Inne propozycje" flow and the background reminder notifications so both
 * behave the same.
 */
public final class OfflineProposalGenerator {

    /**
     * How long a dish stays "recently shown" before it's eligible again. Long
     * enough that offline users genuinely see new sets day to day, short enough
     * that liked dishes reliably come back within a couple of weeks.
     */
    public static final long RECENCY_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(3);

    private final MealPoolBuilder mealPoolBuilder = new MealPoolBuilder();
    private final VariedMealPicker variedMealPicker = new VariedMealPicker();
    private final RecentlyShownFilter recentlyShownFilter = new RecentlyShownFilter();

    public List<Recipe> generate(Recipe[] builtIns, Cookbook cookbook,
                                 UserPreferences preferences, TasteProfile profile,
                                 int count, Random random) {
        return generate(builtIns, cookbook, preferences, profile, count, random,
                MealHistory.empty());
    }

    public List<Recipe> generate(Recipe[] builtIns, Cookbook cookbook,
                                 UserPreferences preferences, TasteProfile profile,
                                 int count, Random random, MealHistory history) {
        return generate(builtIns, cookbook, preferences, profile, count, random, history,
                System.currentTimeMillis());
    }

    public List<Recipe> generate(Recipe[] builtIns, Cookbook cookbook,
                                 UserPreferences preferences, TasteProfile profile,
                                 int count, Random random, MealHistory history, long now) {
        List<Recipe> pool = mealPoolBuilder.build(builtIns, cookbook, preferences);
        Collections.shuffle(pool, random);
        List<Recipe> fresh = recentlyShownFilter.apply(pool, history, RECENCY_WINDOW_MILLIS,
                now, count);
        return variedMealPicker.pick(fresh, profile, count);
    }
}
