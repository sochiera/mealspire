package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A compact picture of what the user tends to like, distilled into a handful of
 * recurring "affinity" terms (ingredients, keywords, style words) drawn from the
 * dishes they marked as liked. It is used in two ways: the terms are fed into the
 * AI prompt so it can suggest <em>new</em> dishes that share something with the
 * user's taste, and {@link #score(String)} ranks offline candidates by how much
 * they overlap with that taste — so suggestions generalise beyond the exact
 * dishes already liked.
 */
public final class TasteProfile {

    private final List<String> affinities;

    public TasteProfile(List<String> affinities) {
        List<String> cleaned = new ArrayList<>();
        if (affinities != null) {
            for (String term : affinities) {
                if (term == null) {
                    continue;
                }
                String normalized = term.trim().toLowerCase();
                if (!normalized.isEmpty() && !cleaned.contains(normalized)) {
                    cleaned.add(normalized);
                }
            }
        }
        this.affinities = Collections.unmodifiableList(cleaned);
    }

    /** The recurring terms, strongest first. */
    public List<String> getAffinities() {
        return affinities;
    }

    public boolean isEmpty() {
        return affinities.isEmpty();
    }

    /** How many affinity terms occur in the given text (case-insensitive). */
    public int score(String text) {
        if (text == null || affinities.isEmpty()) {
            return 0;
        }
        String lower = text.toLowerCase();
        int score = 0;
        for (String term : affinities) {
            if (lower.contains(term)) {
                score++;
            }
        }
        return score;
    }
}
