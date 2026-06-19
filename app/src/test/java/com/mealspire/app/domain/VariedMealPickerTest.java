package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class VariedMealPickerTest {

    private final VariedMealPicker picker = new VariedMealPicker();

    private final Recipe chicken1 = new Recipe("Kurczak z kaszą", "Składniki: kurczak, kasza");
    private final Recipe chicken2 = new Recipe("Kurczak po grecku", "Składniki: kurczak, feta");
    private final Recipe salad = new Recipe("Sałatka", "Składniki: pomidor, ogórek");
    private final Recipe omlet = new Recipe("Omlet", "Składniki: jajka, ser");

    @Test
    public void likingChickenDoesNotMakeEveryPickChicken() {
        TasteProfile chickenLover = new TasteProfile(Arrays.asList("kurczak"));

        List<Recipe> picked = picker.pick(
                Arrays.asList(salad, chicken1, omlet, chicken2), chickenLover, 3);

        assertEquals(3, picked.size());
        // Taste shows up (one chicken first), but the rest are varied — not all chicken.
        assertEquals(chicken1, picked.get(0));
        assertTrue(picked.contains(salad));
        assertTrue(picked.contains(omlet));
        assertFalse("second chicken dish should be crowded out by variety",
                picked.contains(chicken2));
    }

    @Test
    public void withoutAProfileKeepsTheGivenOrder() {
        List<Recipe> picked = picker.pick(
                Arrays.asList(salad, omlet, chicken1),
                new TasteProfile(Collections.<String>emptyList()), 2);

        assertEquals(Arrays.asList(salad, omlet), picked);
    }

    @Test
    public void returnsAllWhenFewerCandidatesThanRequested() {
        List<Recipe> picked = picker.pick(Arrays.asList(salad, omlet),
                new TasteProfile(Arrays.asList("kurczak")), 5);
        assertEquals(2, picked.size());
    }

    @Test
    public void emptyCandidatesGiveEmptyResult() {
        assertTrue(picker.pick(Collections.<Recipe>emptyList(),
                new TasteProfile(Arrays.asList("kurczak")), 3).isEmpty());
    }
}
