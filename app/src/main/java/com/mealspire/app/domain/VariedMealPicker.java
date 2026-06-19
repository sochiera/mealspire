package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses a handful of meals that lean towards the user's taste <em>without</em>
 * collapsing into monotony. It takes at most one "taste-led" pick — the single
 * strongest match for the {@link TasteProfile} — and fills the remaining slots
 * from the candidates in their given (typically shuffled) order. So liking three
 * chicken dishes nudges one chicken suggestion to the top, but the rest stay
 * varied instead of being all chicken.
 */
public final class VariedMealPicker {

    public List<Recipe> pick(List<Recipe> candidates, TasteProfile profile, int count) {
        List<Recipe> result = new ArrayList<>();
        if (candidates == null || count <= 0) {
            return result;
        }
        List<Recipe> remaining = new ArrayList<>(candidates);

        // One taste-led pick so the user's likes are represented...
        if (profile != null && !profile.isEmpty()) {
            Recipe best = null;
            int bestScore = 0;
            for (Recipe recipe : remaining) {
                int score = profile.score(recipe.getTitle() + "\n" + recipe.getDetails());
                if (score > bestScore) {
                    bestScore = score;
                    best = recipe;
                }
            }
            if (best != null) {
                result.add(best);
                remaining.remove(best);
            }
        }

        // ...then keep the rest in the given order to preserve variety.
        for (Recipe recipe : remaining) {
            if (result.size() >= count) {
                break;
            }
            result.add(recipe);
        }
        return result;
    }
}
