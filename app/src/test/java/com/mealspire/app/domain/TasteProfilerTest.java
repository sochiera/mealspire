package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TasteProfilerTest {

    private final TasteProfiler profiler = new TasteProfiler();

    @Test
    public void emptyLikesGiveEmptyProfile() {
        assertTrue(profiler.build(Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void recurringWordInTitlesBecomesTopAffinity() {
        TasteProfile profile = profiler.build(
                Arrays.asList("Kurczak z kaszą", "Kurczak po grecku", "Sałatka grecka"));

        // "kurczak" appears in two liked dishes, so it should lead the profile.
        assertEquals("kurczak", profile.getAffinities().get(0));
    }

    @Test
    public void dropsStopwordsAndShortTokens() {
        TasteProfile profile = profiler.build(Arrays.asList("Ryż na mleku"));
        List<String> terms = profile.getAffinities();
        assertTrue(terms.contains("ryż"));
        assertTrue(terms.contains("mleku"));
        assertFalse(terms.contains("na"));
    }

    @Test
    public void usesIngredientDetailsWhenAvailable() {
        Map<String, String> details = new HashMap<>();
        details.put("Sałatka", "Składniki: feta, oliwki, pomidor.\n\nWymieszaj.");

        TasteProfile profile = profiler.build(Arrays.asList("Sałatka"), details);
        List<String> terms = profile.getAffinities();

        assertTrue(terms.contains("feta"));
        assertTrue(terms.contains("oliwki"));
        assertTrue(terms.contains("pomidor"));
    }

    @Test
    public void dropsGenericStaplesLikeSalt() {
        Map<String, String> details = new HashMap<>();
        details.put("Jajecznica", "Składniki: jajka, sól, pieprz.");

        TasteProfile profile = profiler.build(Arrays.asList("Jajecznica"), details);
        List<String> terms = profile.getAffinities();

        assertTrue(terms.contains("jajka"));
        assertFalse(terms.contains("sól"));
        assertFalse(terms.contains("pieprz"));
    }
}
