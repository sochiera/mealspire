package com.mealspire.app.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link TasteProfile} from the dishes the user liked. It tokenises the
 * liked dish names — and, when available, the ingredient lists of those dishes —
 * and keeps the most frequently recurring meaningful words. Grammatical filler
 * and ubiquitous staples (sól, woda…) are dropped so the surviving terms actually
 * characterise the user's taste.
 */
public final class TasteProfiler {

    private static final int MAX_TERMS = 8;

    // Grammatical filler plus a few staples too generic to be a useful "affinity".
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "z", "ze", "i", "oraz", "na", "w", "we", "do", "po", "a", "o", "u",
            "że", "ale", "lub", "dla", "bez", "pod", "nad", "przy", "się", "to",
            "ten", "ta", "te", "od", "out", "the",
            "sól", "sol", "pieprz", "woda", "wody", "cukier"));

    private final IngredientExtractor ingredientExtractor = new IngredientExtractor();

    /** Builds the profile from liked titles only. */
    public TasteProfile build(Iterable<String> likedTitles) {
        return build(likedTitles, null);
    }

    /**
     * Builds the profile from liked titles, enriched with ingredient lists where
     * a liked dish's recipe text is known (built-in dishes, cookbook entries).
     */
    public TasteProfile build(Iterable<String> likedTitles, Map<String, String> detailsByTitle) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (likedTitles != null) {
            for (String title : likedTitles) {
                addTokens(counts, title);
                if (detailsByTitle != null) {
                    String details = detailsByTitle.get(title);
                    if (details != null) {
                        for (String ingredient : ingredientExtractor.extract(details)) {
                            addTokens(counts, ingredient);
                        }
                    }
                }
            }
        }
        return new TasteProfile(topTerms(counts));
    }

    private void addTokens(Map<String, Integer> counts, String text) {
        if (text == null) {
            return;
        }
        for (String token : text.toLowerCase().split("[^\\p{L}]+")) {
            if (token.length() < 3 || STOPWORDS.contains(token)) {
                continue;
            }
            counts.merge(token, 1, Integer::sum);
        }
    }

    /** Returns the most frequent terms, ties broken by first-seen order. */
    private List<String> topTerms(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> entries =
                new java.util.ArrayList<>(counts.entrySet());
        // Stable sort by descending count keeps insertion order among equal counts.
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> result = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (result.size() >= MAX_TERMS) {
                break;
            }
            result.add(entry.getKey());
        }
        return result;
    }
}
