package com.mealspire.app.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModifyRecipePromptBuilderTest {

    private final ModifyRecipePromptBuilder builder = new ModifyRecipePromptBuilder();

    @Test
    public void systemPromptKeepsTheTitleThenBodyFormat() {
        String system = builder.systemPrompt().toLowerCase();
        assertTrue(system.contains("polsk"));
        assertTrue(system.contains("zamiennik"));
        assertTrue(system.contains("nazwa dania"));
    }

    @Test
    public void userPromptCarriesCurrentRecipeAndTheChange() {
        Recipe current = new Recipe("Kurczak z kaszą",
                "Składniki: kurczak, kasza, jogurt.\nUsmaż kurczaka.");

        String user = builder.userPrompt(current, "nie mam jogurtu, czym zastąpić?");

        assertTrue(user.contains("Kurczak z kaszą"));
        assertTrue(user.contains("kasza"));
        assertTrue(user.contains("nie mam jogurtu"));
    }

    @Test
    public void handlesNullRecipeAndInstruction() {
        // Should not throw; produces a usable prompt skeleton.
        String user = builder.userPrompt(null, null);
        assertTrue(user.toLowerCase().contains("przepis"));
    }
}
