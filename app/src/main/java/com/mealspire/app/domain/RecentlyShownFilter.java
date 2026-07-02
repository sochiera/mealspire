package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Keeps offline suggestions from feeling repetitive: dishes shown within the
 * last {@code windowMillis} are set aside so each round favours something new,
 * and a dish quietly becomes eligible again once that window has passed — it
 * simply "comes back" rather than needing any special handling.
 *
 * <p>If too few dishes are outside the window to fill the request, the
 * least-recently-shown of the excluded ones are brought back first, so a
 * small catalogue (or a very active user) still gets a full set of proposals
 * instead of an abrupt drop to one or two, with occasional repeats rather than
 * an empty result.
 */
public final class RecentlyShownFilter {

    public List<Recipe> apply(List<Recipe> candidates, MealHistory history,
                              long windowMillis, long now, int minResults) {
        List<Recipe> fresh = new ArrayList<>();
        List<Recipe> recentlyShown = new ArrayList<>();
        for (Recipe candidate : candidates) {
            long lastShown = history.lastEatenAt(candidate.getTitle());
            if (lastShown == 0L || now - lastShown >= windowMillis) {
                fresh.add(candidate);
            } else {
                recentlyShown.add(candidate);
            }
        }

        if (fresh.size() >= minResults) {
            return fresh;
        }

        recentlyShown.sort(Comparator.comparingLong(
                recipe -> history.lastEatenAt(recipe.getTitle())));
        List<Recipe> padded = new ArrayList<>(fresh);
        for (Recipe candidate : recentlyShown) {
            if (padded.size() >= minResults) {
                break;
            }
            padded.add(candidate);
        }
        return padded;
    }
}
