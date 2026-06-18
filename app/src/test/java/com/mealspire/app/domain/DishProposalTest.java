package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class DishProposalTest {

    @Test
    public void emptyNameMeansEmptyProposal() {
        assertTrue(new DishProposal("", "opis", "30 min",
                Collections.<String>emptyList()).isEmpty());
        assertFalse(new DishProposal("Naleśniki", "", "", null).isEmpty());
    }

    @Test
    public void summaryShowsDescriptionTimeAndIngredients() {
        DishProposal proposal = new DishProposal("Naleśniki",
                "Puszyste naleśniki na słodko.", "ok. 25 min",
                Arrays.asList("mąka", "mleko", "jajka"));

        String summary = proposal.summary();

        assertTrue(summary.contains("Puszyste naleśniki na słodko."));
        assertTrue(summary.contains("Czas: ok. 25 min"));
        assertTrue(summary.contains("Kluczowe składniki: mąka, mleko, jajka"));
    }

    @Test
    public void nullValuesAreNormalisedToEmpty() {
        DishProposal proposal = new DishProposal(null, null, null, null);
        assertEquals("", proposal.getName());
        assertEquals("", proposal.getDescription());
        assertEquals("", proposal.getTime());
        assertTrue(proposal.getKeyIngredients().isEmpty());
        assertEquals("", proposal.summary());
    }

    @Test
    public void keyIngredientsAreImmutable() {
        DishProposal proposal = new DishProposal("X", "", "",
                Arrays.asList("a", "b"));
        try {
            proposal.getKeyIngredients().add("c");
            throw new AssertionError("expected immutable list");
        } catch (UnsupportedOperationException expected) {
            // good
        }
    }
}
