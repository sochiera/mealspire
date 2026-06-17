package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class RecipeRequestTest {

    @Test
    public void exposesAllFields() {
        UserPreferences prefs = UserPreferences.empty().withLike("ryż");
        RecipeRequest request = new RecipeRequest("Obiad", prefs,
                Arrays.asList("Pizza"), Arrays.asList("danie wegetariańskie"));

        assertEquals("Obiad", request.getMealType());
        assertEquals(prefs, request.getPreferences());
        assertTrue(request.getRecentToAvoid().contains("Pizza"));
        assertTrue(request.getChoiceFragments().contains("danie wegetariańskie"));
    }

    @Test
    public void nullCollectionsBecomeEmpty() {
        RecipeRequest request = new RecipeRequest("Kolacja", null, null, null);
        assertTrue(request.getRecentToAvoid().isEmpty());
        assertTrue(request.getChoiceFragments().isEmpty());
        assertTrue(request.getPreferences().getLikes().isEmpty());
    }
}
