package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeclineReasonTest {

    @Test
    public void offersTheFourReasonsTheUserAskedFor() {
        String[] labels = DeclineReason.labels();
        assertEquals(4, labels.length);
        assertEquals("Nie lubię tego wcale", labels[0]);
        assertEquals("Za trudne", labels[1]);
        assertEquals("Dzisiaj nie mam czasu", labels[2]);
        assertEquals("Nie mam na to ochoty dzisiaj", labels[3]);
    }

    @Test
    public void onlyAGenuineDislikeIsRememberedPermanently() {
        assertTrue(DeclineReason.DISLIKE.isPermanentDislike());
        assertFalse(DeclineReason.TOO_HARD.isPermanentDislike());
        assertFalse(DeclineReason.NO_TIME.isPermanentDislike());
        assertFalse(DeclineReason.NO_DESIRE.isPermanentDislike());
    }

    @Test
    public void everyReasonCarriesAHintForTheNextProposal() {
        for (DeclineReason reason : DeclineReason.values()) {
            assertFalse("hint missing for " + reason, reason.getPromptHint().trim().isEmpty());
        }
        assertTrue(DeclineReason.NO_TIME.getPromptHint().toLowerCase().contains("szybk"));
        assertTrue(DeclineReason.TOO_HARD.getPromptHint().toLowerCase().contains("prost"));
    }
}
