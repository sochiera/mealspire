package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the candidate pool for the offline "Losuj" picker: the built-in recipes
 * for a meal plus the user's cookbook dishes, de-duplicated by title and with
 * disliked dishes hard-excluded. Falls back to the built-ins if filtering would
 * leave nothing.
 */
public final class MealPoolBuilder {

    private final DishFilter dishFilter = new DishFilter();

    public List<Recipe> build(Recipe[] builtIns, Cookbook cookbook, UserPreferences preferences) {
        return build(builtIns, cookbook, preferences, DietConstraints.empty());
    }

    /**
     * Jak {@link #build(Recipe[], Cookbook, UserPreferences)}, ale dodatkowo
     * bezwzględnie odfiltrowuje dania naruszające wykluczenia diety. W
     * odróżnieniu od niechcianych dań ten filtr nie ma fallbacku — lepsza
     * krótsza lista niż wieprzowina u wegetarianina.
     */
    public List<Recipe> build(Recipe[] builtIns, Cookbook cookbook, UserPreferences preferences,
                              DietConstraints diet) {
        List<Recipe> pool = buildWithoutDiet(builtIns, cookbook, preferences);
        if (diet == null || diet.isEmpty()) {
            return pool;
        }
        List<Recipe> allowed = new ArrayList<>();
        for (Recipe recipe : pool) {
            if (diet.allows(recipe.getTitle() + "\n" + recipe.getDetails())) {
                allowed.add(recipe);
            }
        }
        return allowed;
    }

    private List<Recipe> buildWithoutDiet(Recipe[] builtIns, Cookbook cookbook,
                                          UserPreferences preferences) {
        Map<String, Recipe> byTitle = new LinkedHashMap<>();
        if (builtIns != null) {
            for (Recipe recipe : builtIns) {
                byTitle.put(recipe.getTitle().toLowerCase(), recipe);
            }
        }
        if (cookbook != null) {
            for (CookbookEntry entry : cookbook.getEntries()) {
                String key = entry.getTitle().toLowerCase();
                if (!byTitle.containsKey(key)) {
                    byTitle.put(key, entry.toRecipe());
                }
            }
        }

        List<Recipe> combined = new ArrayList<>(byTitle.values());
        List<Recipe> filtered = dishFilter.excludeDisliked(combined, preferences);
        if (filtered.isEmpty()) {
            // Don't leave the user with nothing to draw from.
            return combined.isEmpty() ? filtered : combined;
        }
        return filtered;
    }
}
