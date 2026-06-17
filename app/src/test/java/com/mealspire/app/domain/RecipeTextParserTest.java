package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecipeTextParserTest {

    private final RecipeTextParser parser = new RecipeTextParser();

    @Test
    public void firstNonEmptyLineBecomesTitle() {
        String text = "Naleśniki z serem\n\nSkładniki: mąka, jajka, mleko, ser.\nUsmaż naleśniki i nadziej serem.";
        Recipe recipe = parser.parse(text);
        assertEquals("Naleśniki z serem", recipe.getTitle());
        assertTrue(recipe.getDetails().contains("Składniki"));
        assertTrue(recipe.getDetails().contains("Usmaż"));
    }

    @Test
    public void stripsLeadingMarkdownFromTitle() {
        String text = "# **Owsianka bananowa**\nGotuj płatki z mlekiem, dodaj banana.";
        Recipe recipe = parser.parse(text);
        assertEquals("Owsianka bananowa", recipe.getTitle());
    }

    @Test
    public void skipsLeadingBlankLines() {
        String text = "\n\n   \nZupa pomidorowa\nUgotuj na bulionie.";
        Recipe recipe = parser.parse(text);
        assertEquals("Zupa pomidorowa", recipe.getTitle());
    }

    @Test
    public void singleLineTextHasTitleAndEmptyDetails() {
        Recipe recipe = parser.parse("Kanapka z awokado");
        assertEquals("Kanapka z awokado", recipe.getTitle());
        assertEquals("", recipe.getDetails());
    }
}
