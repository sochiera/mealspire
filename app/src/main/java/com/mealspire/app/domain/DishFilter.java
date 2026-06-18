package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Hard-excludes dishes the user has marked as disliked: any candidate whose
 * title contains a disliked term (case-insensitive) is dropped.
 */
public final class DishFilter {

    public List<Recipe> excludeDisliked(List<Recipe> candidates, UserPreferences preferences) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : candidates) {
            if (!isDisliked(recipe.getTitle(), preferences)) {
                result.add(recipe);
            }
        }
        return result;
    }

    private static boolean isDisliked(String title, UserPreferences preferences) {
        String lowerTitle = title.toLowerCase();
        for (String disliked : preferences.getDislikes()) {
            String term = disliked.toLowerCase();
            if (lowerTitle.contains(term)) {
                return true;
            }
            // Match simple Polish inflections too (e.g. "grzyby" -> "grzybami")
            // by also checking a short stem of the disliked term.
            String stem = stem(term);
            if (stem != null && lowerTitle.contains(stem)) {
                return true;
            }
        }
        return false;
    }

    private static String stem(String term) {
        if (term.length() < 5) {
            return null;
        }
        return term.substring(0, term.length() - 2);
    }
}
