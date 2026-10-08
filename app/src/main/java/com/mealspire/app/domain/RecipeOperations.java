package com.mealspire.app.domain;
import java.io.IOException;
import java.util.List;
/** Operations implemented locally in tests and remotely in the Android shell. */
public interface RecipeOperations {
    List<DishProposal> proposeDishes(RecipeRequest request, int count) throws IOException;
    Recipe generateRecipeFor(String dishName, RecipeRequest request) throws IOException;
    Recipe modifyRecipe(Recipe current, String instruction, HouseholdProfile profile) throws IOException;
}
