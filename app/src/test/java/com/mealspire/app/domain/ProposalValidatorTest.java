package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Walidacja odpowiedzi AI po stronie appki: propozycje łamiące wykluczenia
 * diety (albo zdublowane) wypadają, a braki uzupełnia pula offline — użytkownik
 * zawsze dostaje pełny zestaw bez ponownego wołania AI.
 */
public class ProposalValidatorTest {

    private final ProposalValidator validator = new ProposalValidator();

    private static DishProposal proposal(String name, String... ingredients) {
        return new DishProposal(name, "opis", "ok. 20 min", Arrays.asList(ingredients));
    }

    private static final List<Recipe> FALLBACK = Arrays.asList(
            new Recipe("Ryż z warzywami", "Składniki: ryż, marchew, papryka.\n\nUgotuj."),
            new Recipe("Leczo warzywne", "Składniki: papryka, cukinia, pomidory.\n\nDuś."));

    @Test
    public void bezWykluczenPrzepuszczaWszystkoBezZmian() {
        List<DishProposal> ai = Arrays.asList(
                proposal("Kurczak z ryżem", "kurczak", "ryż"),
                proposal("Omlet", "jajka"));
        ProposalValidator.Result result =
                validator.validate(ai, DietConstraints.empty(), FALLBACK, 2);

        assertEquals(2, result.getProposals().size());
        assertEquals("Kurczak z ryżem", result.getProposals().get(0).getName());
        // AI proposals have no ready recipe — it is fetched on demand.
        assertNull(result.getRecipes().get(0));
        assertNull(result.getRecipes().get(1));
    }

    @Test
    public void propozycjaLamiacaDieteJestZastapionaZPuliOffline() {
        DietConstraints veg = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));
        List<DishProposal> ai = Arrays.asList(
                proposal("Kotlet schabowy", "schab", "bułka tarta"),
                proposal("Sałatka warzywna", "pomidor", "ogórek"));

        ProposalValidator.Result result = validator.validate(ai, veg, FALLBACK, 2);

        assertEquals(2, result.getProposals().size());
        assertEquals("Sałatka warzywna", result.getProposals().get(0).getName());
        assertEquals("Ryż z warzywami", result.getProposals().get(1).getName());
        // The offline substitute carries its ready recipe.
        assertNull(result.getRecipes().get(0));
        assertNotNull(result.getRecipes().get(1));
        assertEquals("Ryż z warzywami", result.getRecipes().get(1).getTitle());
    }

    @Test
    public void wykluczenieWykrywaneJestTezWSkladnikachNieTylkoWNazwie() {
        DietConstraints noLactose = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.NO_LACTOSE));
        List<DishProposal> ai = Collections.singletonList(
                proposal("Placki neutralne", "mąka", "mleko", "jajka"));

        ProposalValidator.Result result = validator.validate(ai, noLactose, FALLBACK, 1);

        assertEquals("Ryż z warzywami", result.getProposals().get(0).getName());
    }

    @Test
    public void duplikatyWewnatrzTrojkiSaOdrzucane() {
        List<DishProposal> ai = Arrays.asList(
                proposal("Omlet", "jajka"),
                proposal("omlet", "jajka, mleko"),
                proposal("Sałatka", "pomidor"));

        ProposalValidator.Result result =
                validator.validate(ai, DietConstraints.empty(), FALLBACK, 3);

        assertEquals(3, result.getProposals().size());
        assertEquals("Omlet", result.getProposals().get(0).getName());
        assertEquals("Sałatka", result.getProposals().get(1).getName());
        assertEquals("Ryż z warzywami", result.getProposals().get(2).getName());
    }

    @Test
    public void fallbackNieDublujePropozycjiAi() {
        List<DishProposal> ai = Collections.singletonList(
                proposal("Ryż z warzywami", "ryż", "marchew"));

        ProposalValidator.Result result =
                validator.validate(ai, DietConstraints.empty(), FALLBACK, 2);

        assertEquals(2, result.getProposals().size());
        assertEquals("Ryż z warzywami", result.getProposals().get(0).getName());
        assertEquals("Leczo warzywne", result.getProposals().get(1).getName());
    }

    @Test
    public void gdyBrakujeKandydatowZwracaTyleIleMa() {
        DietConstraints veg = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));
        List<DishProposal> ai = Collections.singletonList(
                proposal("Gulasz wieprzowy", "wieprzowina"));

        ProposalValidator.Result result = validator.validate(
                ai, veg, Collections.<Recipe>emptyList(), 3);

        assertTrue(result.getProposals().isEmpty());
    }

    @Test
    public void pusteWejsciaNieWywracajaWalidacji() {
        ProposalValidator.Result result =
                validator.validate(null, null, null, 3);
        assertTrue(result.getProposals().isEmpty());
        assertTrue(result.getRecipes().isEmpty());
    }

    @Test
    public void propozycjaZPuliOfflineMaOpisICzasIliSkladniki() {
        DishProposal proposal = validator.proposalFromRecipe(FALLBACK.get(0));
        assertEquals("Ryż z warzywami", proposal.getName());
        assertEquals("Proste danie z Twojej puli.", proposal.getDescription());
        assertTrue(proposal.getKeyIngredients().contains("ryż"));
    }
}
