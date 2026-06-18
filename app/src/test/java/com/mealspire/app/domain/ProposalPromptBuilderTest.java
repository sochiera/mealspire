package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ProposalPromptBuilderTest {

    private final ProposalPromptBuilder builder = new ProposalPromptBuilder();

    @Test
    public void systemPromptAsksForProposalOnlyNotFullRecipe() {
        String system = builder.systemPrompt();
        assertTrue(system.toLowerCase().contains("polsk"));
        // Must ask for the four-field proposal format, explicitly not a full recipe.
        assertTrue(system.contains("Nazwa:"));
        assertTrue(system.contains("Opis:"));
        assertTrue(system.contains("Czas:"));
        assertTrue(system.contains("Składniki:"));
        assertTrue(system.toLowerCase().contains("nie podajesz pełnego przepisu"));
    }

    @Test
    public void userPromptIncludesMealTypeAndPreferences() {
        UserPreferences prefs = UserPreferences.empty()
                .withLike("naleśniki").withDislike("brokuły");
        RecipeRequest request = new RecipeRequest("Obiad", prefs,
                Collections.<String>emptyList(), Collections.<String>emptyList());

        String user = builder.userPrompt(request);

        assertTrue(user.contains("Obiad"));
        assertTrue(user.contains("naleśniki"));
        assertTrue(user.contains("brokuły"));
    }

    @Test
    public void userPromptIncludesChoiceFragmentsAndRecentDishes() {
        RecipeRequest request = new RecipeRequest("Kolacja", UserPreferences.empty(),
                Arrays.asList("Pierogi"), Arrays.asList("dla 4 osób, coś szybkiego"));

        String user = builder.userPrompt(request);

        assertTrue(user.contains("dla 4 osób, coś szybkiego"));
        assertTrue(user.contains("Pierogi"));
    }

    @Test
    public void userPromptDoesNotAskForFullPreparationSteps() {
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
        String user = builder.userPrompt(request).toLowerCase();
        assertFalse(user.contains("sposób przygotowania"));
    }
}
