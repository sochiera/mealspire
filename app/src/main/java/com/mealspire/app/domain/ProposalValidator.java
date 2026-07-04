package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Nieufność domyślna wobec odpowiedzi AI: propozycje naruszające wykluczenia
 * diety (albo zdublowane w obrębie trójki) są odrzucane i zastępowane daniami
 * z przefiltrowanej puli offline, żeby użytkownik zawsze dostał pełny zestaw.
 * Zero ponownych wołań AI — fallback offline jest tańszy i przewidywalny.
 */
public final class ProposalValidator {

    /** Opis dania dolosowanego z puli offline (ma od razu gotowy przepis). */
    static final String OFFLINE_DESCRIPTION = "Proste danie z Twojej puli.";

    private final IngredientExtractor ingredientExtractor = new IngredientExtractor();

    /**
     * Zweryfikowane propozycje i (równolegle) ich przepisy: {@code null} dla
     * propozycji od AI (pełny przepis dociągany na żądanie), gotowy
     * {@link Recipe} dla dolosowanych z puli offline.
     */
    public static final class Result {
        private final List<DishProposal> proposals;
        private final List<Recipe> recipes;

        Result(List<DishProposal> proposals, List<Recipe> recipes) {
            this.proposals = proposals;
            this.recipes = recipes;
        }

        public List<DishProposal> getProposals() {
            return proposals;
        }

        public List<Recipe> getRecipes() {
            return recipes;
        }
    }

    /**
     * @param aiProposals       propozycje zwrócone przez AI, w kolejności modelu
     * @param diet              twarde wykluczenia; naruszenie = odrzucenie
     * @param offlineFallback   dania z puli offline (już przefiltrowane dietą)
     *                          do uzupełnienia braków
     * @param count             docelowa liczba propozycji
     */
    public Result validate(List<DishProposal> aiProposals, DietConstraints diet,
                           List<Recipe> offlineFallback, int count) {
        List<DishProposal> proposals = new ArrayList<>();
        List<Recipe> recipes = new ArrayList<>();
        List<String> usedTitles = new ArrayList<>();

        if (aiProposals != null) {
            for (DishProposal proposal : aiProposals) {
                if (proposals.size() >= count || proposal == null || proposal.isEmpty()) {
                    continue;
                }
                if (usedTitles.contains(proposal.getName().toLowerCase())) {
                    continue;
                }
                if (!allowed(proposal, diet)) {
                    continue;
                }
                proposals.add(proposal);
                recipes.add(null);
                usedTitles.add(proposal.getName().toLowerCase());
            }
        }

        if (offlineFallback != null) {
            for (Recipe recipe : offlineFallback) {
                if (proposals.size() >= count) {
                    break;
                }
                if (usedTitles.contains(recipe.getTitle().toLowerCase())) {
                    continue;
                }
                proposals.add(proposalFromRecipe(recipe));
                recipes.add(recipe);
                usedTitles.add(recipe.getTitle().toLowerCase());
            }
        }
        return new Result(proposals, recipes);
    }

    private static boolean allowed(DishProposal proposal, DietConstraints diet) {
        if (diet == null || diet.isEmpty()) {
            return true;
        }
        StringBuilder text = new StringBuilder(proposal.getName());
        text.append('\n').append(proposal.getDescription());
        for (String ingredient : proposal.getKeyIngredients()) {
            text.append('\n').append(ingredient);
        }
        return diet.allows(text.toString());
    }

    /** Lekka propozycja z gotowego przepisu (wspólna dla ekranu i fallbacku). */
    public DishProposal proposalFromRecipe(Recipe recipe) {
        List<String> ingredients = ingredientExtractor.extract(recipe.getDetails());
        if (ingredients.size() > 5) {
            ingredients = new ArrayList<>(ingredients.subList(0, 5));
        }
        return new DishProposal(recipe.getTitle(), OFFLINE_DESCRIPTION, "", ingredients);
    }
}
