package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class IngredientExtractorTest {

    private final IngredientExtractor extractor = new IngredientExtractor();

    @Test
    public void extractsInlineCommaSeparatedIngredients() {
        String details = "Składniki: jajka, mleko, mąka.\n\nWymieszaj i usmaż.";
        List<String> items = extractor.extract(details);
        assertEquals(3, items.size());
        assertTrue(items.contains("jajka"));
        assertTrue(items.contains("mleko"));
        assertTrue(items.contains("mąka"));
    }

    @Test
    public void extractsBulletedIngredientsAndStopsBeforeSteps() {
        String details = "Składniki:\n- pomidor\n- ogórek\n- feta\n\n"
                + "Sposób przygotowania:\n1. Pokrój warzywa.\n2. Wymieszaj.";
        List<String> items = extractor.extract(details);
        assertEquals(3, items.size());
        assertTrue(items.contains("pomidor"));
        assertTrue(items.contains("feta"));
        assertFalse(items.contains("Pokrój warzywa."));
    }

    @Test
    public void handlesMarkdownLabelAndBoldBullets() {
        String details = "**Składniki:**\n- 500 g mięsa mielonego\n- 1 cebula\n\n"
                + "**Przygotowanie:**\n1. Smaż.";
        List<String> items = extractor.extract(details);
        assertTrue(items.contains("500 g mięsa mielonego"));
        assertTrue(items.contains("1 cebula"));
    }

    @Test
    public void noIngredientsLabelYieldsEmptyList() {
        assertTrue(extractor.extract("Po prostu coś ugotuj.").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
    }

    @Test
    public void deduplicatesAndTrims() {
        String details = "Składniki: sól , sól, pieprz.\n\nGotuj.";
        List<String> items = extractor.extract(details);
        assertEquals(2, items.size());
    }
}
