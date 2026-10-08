package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * Tekst przepisu (z katalogu albo od AI) to luźny tekst „Etykieta: treść".
 * Ekran przepisu dzieli go na nagłówki, listę składników i akapity, żeby dało
 * się go czytać przy garach — bez gubienia ani jednego słowa treści.
 */
public class RecipeLayoutTest {

    @Test
    public void składnikiStająSięNagłówkiemILista() {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(
                "Składniki: jajka, masło, sól.\n\nRozpuść masło i wbij jajka.");

        assertBlock(blocks.get(0), RecipeLayout.Type.HEADING, "Składniki");
        assertBlock(blocks.get(1), RecipeLayout.Type.ITEM, "jajka");
        assertBlock(blocks.get(2), RecipeLayout.Type.ITEM, "masło");
        assertBlock(blocks.get(3), RecipeLayout.Type.ITEM, "sól");
        assertBlock(blocks.get(4), RecipeLayout.Type.PARAGRAPH, "Rozpuść masło i wbij jajka.");
        assertEquals(5, blocks.size());
    }

    @Test
    public void przecinkiWNawiasieNieDzieląSkładnika() {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(
                "Składniki: owoce (jabłko, banan), miód");

        assertBlock(blocks.get(1), RecipeLayout.Type.ITEM, "owoce (jabłko, banan)");
        assertBlock(blocks.get(2), RecipeLayout.Type.ITEM, "miód");
        assertEquals(3, blocks.size());
    }

    @Test
    public void innaKrótkaEtykietaToNagłówekZAkapitem() {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(
                "Dodatki (opcjonalnie, do wyboru): jabłko, banan, cynamon");

        assertBlock(blocks.get(0), RecipeLayout.Type.HEADING, "Dodatki (opcjonalnie, do wyboru)");
        assertBlock(blocks.get(1), RecipeLayout.Type.PARAGRAPH, "jabłko, banan, cynamon");
    }

    @Test
    public void zdanieZDwukropkiemDalekoNieJestNagłówkiem() {
        String sentence = "Gotuj makaron w osolonej wodzie zgodnie z opakowaniem, potem dodaj: sos.";
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(sentence);

        assertBlock(blocks.get(0), RecipeLayout.Type.PARAGRAPH, sentence);
        assertEquals(1, blocks.size());
    }

    @Test
    public void punktoryIKrokiNumerowane() {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(
                "Składniki:\n- kapusta\n• grzyby\n\n1. Duś kapustę.\n2) Dodaj grzyby.");

        assertBlock(blocks.get(0), RecipeLayout.Type.HEADING, "Składniki");
        assertBlock(blocks.get(1), RecipeLayout.Type.ITEM, "kapusta");
        assertBlock(blocks.get(2), RecipeLayout.Type.ITEM, "grzyby");
        assertBlock(blocks.get(3), RecipeLayout.Type.STEP, "Duś kapustę.");
        assertEquals("1", blocks.get(3).getNumber());
        assertBlock(blocks.get(4), RecipeLayout.Type.STEP, "Dodaj grzyby.");
        assertEquals("2", blocks.get(4).getNumber());
        assertEquals(5, blocks.size());
    }

    @Test
    public void godzinaNieJestEtykietą() {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse("Piecz 12:30 minut.");

        assertBlock(blocks.get(0), RecipeLayout.Type.PARAGRAPH, "Piecz 12:30 minut.");
    }

    @Test
    public void pustyTekstNieMaBloków() {
        assertEquals(0, RecipeLayout.parse("").size());
        assertEquals(0, RecipeLayout.parse(null).size());
        assertEquals(0, RecipeLayout.parse(" \n\n ").size());
    }

    private static void assertBlock(RecipeLayout.Block block, RecipeLayout.Type type,
                                    String text) {
        assertEquals(type, block.getType());
        assertEquals(text, block.getText());
    }
}
