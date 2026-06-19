package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class TasteProfileTest {

    @Test
    public void emptyWhenNoAffinities() {
        assertTrue(new TasteProfile(null).isEmpty());
        assertTrue(new TasteProfile(Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void normalisesAndDeduplicatesTerms() {
        TasteProfile profile = new TasteProfile(Arrays.asList("Kurczak", "kurczak", " Ryż "));
        assertEquals(Arrays.asList("kurczak", "ryż"), profile.getAffinities());
    }

    @Test
    public void scoreCountsMatchingTerms() {
        TasteProfile profile = new TasteProfile(Arrays.asList("kurczak", "ryż"));
        assertEquals(2, profile.score("Kurczak z ryżem i warzywami"));
        assertEquals(1, profile.score("Kurczak po grecku"));
        assertEquals(0, profile.score("Naleśniki z dżemem"));
    }

    @Test
    public void scoreIsZeroForNullTextOrEmptyProfile() {
        assertEquals(0, new TasteProfile(Arrays.asList("kurczak")).score(null));
        assertEquals(0, new TasteProfile(Collections.<String>emptyList()).score("Kurczak"));
    }

    @Test
    public void notEmptyWhenTermsPresent() {
        assertFalse(new TasteProfile(Arrays.asList("kasza")).isEmpty());
    }
}
