package com.mealspire.app.domain;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The offline meal-picking pipeline in one place: build the candidate pool
 * (built-ins + cookbook, disliked excluded), shuffle for variety, then let the
 * {@link VariedMealPicker} take at most one taste-led pick and keep the rest
 * varied. Shared by the on-screen "Inne propozycje" flow and the background
 * reminder notifications so both behave the same.
 */
public final class OfflineProposalGenerator {

    private final MealPoolBuilder mealPoolBuilder = new MealPoolBuilder();
    private final VariedMealPicker variedMealPicker = new VariedMealPicker();

    public List<Recipe> generate(Recipe[] builtIns, Cookbook cookbook,
                                 UserPreferences preferences, TasteProfile profile,
                                 int count, Random random) {
        List<Recipe> pool = mealPoolBuilder.build(builtIns, cookbook, preferences);
        Collections.shuffle(pool, random);
        return variedMealPicker.pick(pool, profile, count);
    }
}
