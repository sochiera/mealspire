package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Profil domowników realnie wpływa na prompty: dzieci, poziom gotowania
 * i preferowane kuchnie trafiają zarówno do propozycji, jak i pełnego
 * przepisu. Pusty/pominięty profil nie dodaje nic (wzorem
 * {@link PreferenceAwarePromptTest}).
 */
public class HouseholdAwarePromptTest {

    private final ProposalPromptBuilder proposalBuilder = new ProposalPromptBuilder();
    private final RecipePromptBuilder recipeBuilder = new RecipePromptBuilder();
    private final ModifyRecipePromptBuilder modifyBuilder = new ModifyRecipePromptBuilder();

    private static RecipeRequest requestWith(HouseholdProfile profile) {
        return new RecipeRequest("Obiad", UserPreferences.empty(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),
                null, null, profile);
    }

    private static HouseholdProfile fullProfile() {
        return HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.WITH_CHILDREN)
                .withSkill(HouseholdProfile.CookingSkill.BEGINNER)
                .withCuisines(Arrays.asList("polska", "azjatycka"))
                .withDiet(DietConstraints.of(Arrays.asList(
                        DietConstraints.Exclusion.NO_PORK)));
    }

    @Test
    public void propozycjeUwzgledniajaCalyProfil() {
        String prompt = proposalBuilder.userPrompt(requestWith(fullProfile()), 3);

        assertTrue(prompt.contains("dzieci chętnie jedzą"));
        assertTrue(prompt.contains("uczę się gotować"));
        assertTrue(prompt.contains("Preferowane kuchnie: polska, azjatycka"));
        assertTrue(prompt.contains("Bezwzględny wymóg diety"));
        assertTrue(prompt.contains("wieprzowiny"));
    }

    @Test
    public void wymogDietyObowiazujeWKazdymPrompcie() {
        Recipe base = new Recipe("Kopytka", "Składniki: ziemniaki.\n\nUgotuj.");
        List<String> prompts = Arrays.asList(
                proposalBuilder.userPrompt(requestWith(fullProfile()), 3),
                proposalBuilder.userPrompt(requestWith(fullProfile())),
                recipeBuilder.fullRecipePrompt("Kopytka", requestWith(fullProfile())),
                recipeBuilder.userPrompt(requestWith(fullProfile())),
                modifyBuilder.userPrompt(base, "mniej soli", fullProfile()));
        for (String prompt : prompts) {
            assertTrue(prompt.contains("Bezwzględny wymóg diety"));
        }
    }

    @Test
    public void pojedynczaPropozycjaTezUwzgledniaProfil() {
        String prompt = proposalBuilder.userPrompt(requestWith(fullProfile()));
        assertTrue(prompt.contains("dzieci chętnie jedzą"));
    }

    @Test
    public void pelnyPrzepisUwzgledniaCalyProfil() {
        String prompt = recipeBuilder.fullRecipePrompt("Kopytka", requestWith(fullProfile()));

        assertTrue(prompt.contains("Kopytka"));
        assertTrue(prompt.contains("dzieci chętnie jedzą"));
        assertTrue(prompt.contains("uczę się gotować"));
        assertTrue(prompt.contains("Preferowane kuchnie: polska, azjatycka"));
    }

    @Test
    public void przepisJednymStrzalemTezUwzgledniaProfil() {
        String prompt = recipeBuilder.userPrompt(requestWith(fullProfile()));
        assertTrue(prompt.contains("dzieci chętnie jedzą"));
    }

    @Test
    public void wprawnyKucharzDostajeZdanieOWyzwaniach() {
        HouseholdProfile profile = HouseholdProfile.empty()
                .withSkill(HouseholdProfile.CookingSkill.CONFIDENT);
        String prompt = proposalBuilder.userPrompt(requestWith(profile), 3);
        assertTrue(prompt.contains("lubię wyzwania"));
    }

    @Test
    public void zmianaPrzepisuUwzgledniaCalyProfil() {
        Recipe base = new Recipe("Kopytka", "Składniki: ziemniaki.\n\nUgotuj.");
        String prompt = modifyBuilder.userPrompt(base, "bez glutenu", fullProfile());

        assertTrue(prompt.contains("bez glutenu"));
        assertTrue(prompt.contains("dzieci chętnie jedzą"));
        assertTrue(prompt.contains("uczę się gotować"));
        assertTrue(prompt.contains("Preferowane kuchnie: polska, azjatycka"));
    }

    @Test
    public void pustyProfilNieDodajeNic() {
        Recipe base = new Recipe("Kopytka", "Składniki: ziemniaki.\n\nUgotuj.");
        List<String> prompts = Arrays.asList(
                proposalBuilder.userPrompt(requestWith(HouseholdProfile.empty()), 3),
                recipeBuilder.fullRecipePrompt("Kopytka", requestWith(HouseholdProfile.empty())),
                modifyBuilder.userPrompt(base, "bez glutenu", HouseholdProfile.empty()),
                modifyBuilder.userPrompt(base, "bez glutenu", null),
                proposalBuilder.userPrompt(requestWith(null), 3));
        for (String prompt : prompts) {
            assertFalse(prompt.contains("dzieci"));
            assertFalse(prompt.contains("Preferowane kuchnie"));
            assertFalse(prompt.contains("uczę się gotować"));
            assertFalse(prompt.contains("wyzwania"));
            assertFalse(prompt.contains("Bezwzględny wymóg diety"));
        }
    }
}
