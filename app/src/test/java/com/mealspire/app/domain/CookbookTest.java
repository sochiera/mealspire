package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CookbookTest {

    private CookbookEntry entry(String title, String recipe, String source) {
        return new CookbookEntry(title, recipe, source);
    }

    @Test
    public void emptyHasNoEntries() {
        assertTrue(Cookbook.empty().getEntries().isEmpty());
    }

    @Test
    public void addStoresEntry() {
        Cookbook cookbook = Cookbook.empty().add(entry("Bigos", "Gotuj kapustę z mięsem.", "ręcznie"));
        assertTrue(cookbook.contains("Bigos"));
        assertEquals(1, cookbook.getEntries().size());
    }

    @Test
    public void addDuplicateTitleCaseInsensitiveReplaces() {
        Cookbook cookbook = Cookbook.empty()
                .add(entry("Pierogi", "stary opis", "ręcznie"))
                .add(entry("pierogi", "nowy opis", "AI"));
        assertEquals(1, cookbook.getEntries().size());
        assertEquals("nowy opis", cookbook.getEntries().get(0).getRecipe());
    }

    @Test
    public void removeByTitleCaseInsensitive() {
        Cookbook cookbook = Cookbook.empty()
                .add(entry("Żurek", "opis", "ręcznie"))
                .remove("ŻUREK");
        assertFalse(cookbook.contains("Żurek"));
    }

    @Test
    public void titlesListsAllTitles() {
        Cookbook cookbook = Cookbook.empty()
                .add(entry("A", "a", ""))
                .add(entry("B", "b", ""));
        assertTrue(cookbook.titles().contains("A"));
        assertTrue(cookbook.titles().contains("B"));
    }

    @Test
    public void blankTitleIsIgnored() {
        Cookbook cookbook = Cookbook.empty().add(entry("   ", "opis", ""));
        assertTrue(cookbook.getEntries().isEmpty());
    }

    @Test
    public void addReturnsNewInstance() {
        Cookbook original = Cookbook.empty();
        original.add(entry("Zupa", "opis", ""));
        assertTrue(original.getEntries().isEmpty());
    }
}
