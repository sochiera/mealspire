package com.mealspire.app.domain;

import java.util.List;

/**
 * Jedna jawna reakcja użytkownika na danie: „Lubię to" albo „Nie lubię".
 * Trzyma tylko to, czego potrzebuje ocena przez LLM — nazwę, krótki opis
 * (skład) i czas. Otwarcie przepisu czy „Inne propozycje" reakcją nie są.
 */
public final class DishReaction {

    /** Opis idzie do promptu — krótko, żeby lista reakcji nie puchła. */
    static final int MAX_DESCRIPTION = 140;

    private static final IngredientExtractor INGREDIENTS = new IngredientExtractor();

    private final String dish;
    private final String description;
    private final boolean liked;
    private final long timeMillis;

    public DishReaction(String dish, String description, boolean liked, long timeMillis) {
        this.dish = dish == null ? "" : dish.trim();
        this.description = shorten(description);
        this.liked = liked;
        this.timeMillis = timeMillis;
    }

    public String getDish() {
        return dish;
    }

    public String getDescription() {
        return description;
    }

    public boolean isLiked() {
        return liked;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    /** Krótki opis dania z treści przepisu: jego kluczowe składniki. */
    public static String describe(String recipeDetails) {
        List<String> ingredients = INGREDIENTS.extract(recipeDetails);
        if (ingredients.size() > 6) {
            ingredients = ingredients.subList(0, 6);
        }
        return ingredients.isEmpty() ? "" : "składniki: " + PromptText.join(ingredients);
    }

    /** Krótki opis propozycji bez przepisu: opis i kluczowe składniki. */
    public static String describe(DishProposal proposal) {
        StringBuilder sb = new StringBuilder();
        if (!proposal.getDescription().isEmpty()
                && !ProposalValidator.OFFLINE_DESCRIPTION.equals(proposal.getDescription())) {
            sb.append(proposal.getDescription());
        }
        if (!proposal.getKeyIngredients().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append("składniki: ").append(PromptText.join(proposal.getKeyIngredients()));
        }
        return sb.toString();
    }

    private static String shorten(String text) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() <= MAX_DESCRIPTION
                ? flat : flat.substring(0, MAX_DESCRIPTION - 1).trim() + "…";
    }
}
