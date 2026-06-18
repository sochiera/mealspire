package com.mealspire.app.domain;

/**
 * Builds the "for N people" portion hint added to a recipe prompt, with correct
 * Polish wording. Returns an empty string when no size is chosen.
 */
public final class PortionSize {

    private PortionSize() {
    }

    public static String promptFragment(int servings) {
        if (servings <= 0) {
            return "";
        }
        String noun = servings == 1 ? "osoby" : "osób";
        return "przygotuj porcję dla " + servings + " " + noun;
    }
}
