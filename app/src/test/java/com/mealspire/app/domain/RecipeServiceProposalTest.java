package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RecipeServiceProposalTest {

    /** Fake client that records prompts and returns a canned response. */
    private static class FakeClaudeClient implements ClaudeClient {
        String lastSystem;
        String lastUser;
        final String response;

        FakeClaudeClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.lastSystem = systemPrompt;
            this.lastUser = userPrompt;
            return response;
        }
    }

    private RecipeService service(FakeClaudeClient client) {
        return new RecipeService(client, new RecipePromptBuilder(), new RecipeTextParser());
    }

    @Test
    public void proposeDishReturnsParsedProposal() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient(
                "Nazwa: Leczo\nOpis: Warzywne leczo.\nCzas: 40 min\nSkładniki: papryka, cebula, kiełbasa");
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(),
                Collections.<String>emptyList(), Collections.<String>emptyList());

        DishProposal proposal = service(client).proposeDish(request);

        assertEquals("Leczo", proposal.getName());
        assertEquals("40 min", proposal.getTime());
        // Uses the proposal-specific system prompt (not the full-recipe one).
        assertEquals(new ProposalPromptBuilder().systemPrompt(), client.lastSystem);
    }

    @Test
    public void proposeDishesReturnsSeveralProposalsFromOneCall() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient(
                "Nazwa: Jajecznica\nCzas: 10 min\nSkładniki: jajka\n"
                        + "---\nNazwa: Owsianka\nCzas: 8 min\nSkładniki: płatki\n"
                        + "---\nNazwa: Tost\nCzas: 5 min\nSkładniki: chleb");
        RecipeRequest request = new RecipeRequest("Śniadanie", UserPreferences.empty(),
                Collections.<String>emptyList(), Collections.<String>emptyList());

        java.util.List<DishProposal> proposals = service(client).proposeDishes(request, 3);

        assertEquals(3, proposals.size());
        assertEquals("Jajecznica", proposals.get(0).getName());
        assertTrue(client.lastUser.contains("3"));
    }

    @Test
    public void generateRecipeForPinsTheAcceptedDishName() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient(
                "Leczo\n\nSkładniki: papryka...\nDusić warzywa.");
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(),
                Collections.<String>emptyList(), Arrays.asList("dla 4 osób"));

        Recipe recipe = service(client).generateRecipeFor("Leczo", request);

        assertEquals("Leczo", recipe.getTitle());
        assertTrue(client.lastUser.contains("Leczo"));
        assertTrue(client.lastUser.contains("dla 4 osób"));
    }

    @Test
    public void modifyRecipeSendsCurrentRecipeAndInstruction() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient(
                "Kurczak z kaszą\n\nSkładniki: kurczak, kasza, śmietana.\nUsmaż.");
        Recipe current = new Recipe("Kurczak z kaszą",
                "Składniki: kurczak, kasza, jogurt.");

        Recipe modified = service(client).modifyRecipe(current, "nie mam jogurtu");

        assertEquals("Kurczak z kaszą", modified.getTitle());
        assertTrue(modified.getDetails().contains("śmietana"));
        assertTrue(client.lastUser.contains("nie mam jogurtu"));
        assertEquals(new ModifyRecipePromptBuilder().systemPrompt(), client.lastSystem);
    }
}
