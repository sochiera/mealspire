package com.mealspire.app.domain;

/**
 * Chooses the "most overdue" dish from a set of candidates: the one not eaten for
 * the longest time (never-eaten dishes win, since their last timestamp is 0).
 * Pure and deterministic — callers can pre-shuffle candidates for tie variety.
 */
public final class StaleMealSelector {

    public String pickStalest(Iterable<String> candidates, MealHistory history) {
        String best = null;
        long bestTimestamp = Long.MAX_VALUE;
        for (String candidate : candidates) {
            long lastEaten = history.lastEatenAt(candidate);
            if (best == null || lastEaten < bestTimestamp) {
                best = candidate;
                bestTimestamp = lastEaten;
            }
        }
        return best;
    }
}
