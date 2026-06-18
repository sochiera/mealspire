package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CookbookSerializerTest {

    private final CookbookSerializer serializer = new CookbookSerializer();

    @Test
    public void roundTripPreservesEntries() {
        Cookbook cookbook = Cookbook.empty()
                .add(new CookbookEntry("Pierogi", "Ulep i ugotuj.", "ręcznie"))
                .add(new CookbookEntry("Pad Thai", "Smaż makaron ryżowy.", "https://x.example/pad-thai"));

        Cookbook restored = serializer.fromJson(serializer.toJson(cookbook));

        assertEquals(2, restored.getEntries().size());
        CookbookEntry pierogi = restored.getEntries().get(0);
        assertEquals("Pierogi", pierogi.getTitle());
        assertEquals("Ulep i ugotuj.", pierogi.getRecipe());
        assertEquals("ręcznie", pierogi.getSource());
        assertEquals("https://x.example/pad-thai", restored.getEntries().get(1).getSource());
    }

    @Test
    public void nullOrBlankYieldsEmptyCookbook() {
        assertTrue(serializer.fromJson(null).getEntries().isEmpty());
        assertTrue(serializer.fromJson("").getEntries().isEmpty());
    }

    @Test
    public void malformedJsonYieldsEmptyCookbook() {
        assertTrue(serializer.fromJson("<<<").getEntries().isEmpty());
    }
}
