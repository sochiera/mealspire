package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * Słownikowe tagowanie dań w wymiarach gustu. Priorytet w obrębie wymiaru
 * (mięso przed nabiałem), kontrakt „nierozpoznane = null" i sensowne pokrycie
 * wbudowanej puli — bez wołania AI.
 */
public class DishTaggerTest {

    private final DishTagger tagger = new DishTagger();

    @Test
    public void rozpoznajeBazyTypowychDan() {
        assertEquals("wieprzowina", tagger.tag("Kotlet schabowy",
                "Składniki: schab, jajko, bułka tarta.").get(TasteDimension.BASE));
        assertEquals("ryba", tagger.tag("Makaron z tuńczykiem",
                "Składniki: makaron, tuńczyk w sosie własnym.").get(TasteDimension.BASE));
        assertEquals("kurczak", tagger.tag("Ryż z kurczakiem curry",
                "Składniki: pierś z kurczaka, ryż, curry.").get(TasteDimension.BASE));
        assertEquals("jajka", tagger.tag("Jajecznica",
                "Składniki: jajka, masło.").get(TasteDimension.BASE));
        assertEquals("strączki", tagger.tag("Leczo z cieciorką",
                "Składniki: ciecierzyca, papryka.").get(TasteDimension.BASE));
    }

    @Test
    public void miesoMaPriorytetNadNabialemIWarzywami() {
        // Schabowy smażony na maśle z kapustą to wieprzowina, nie nabiał.
        DishTags tags = tagger.tag("Kotlet schabowy",
                "Składniki: schab, masło, ser żółty, kapusta.");
        assertEquals("wieprzowina", tags.get(TasteDimension.BASE));
    }

    @Test
    public void rozpoznajeKuchnieTylkoGdyCosNaToWskazuje() {
        assertEquals("azjatycka", tagger.tag("Ryż z kurczakiem curry",
                "Składniki: curry, mleko kokosowe.").get(TasteDimension.CUISINE));
        assertEquals("polska", tagger.tag("Pierogi z serem",
                "Składniki: ciasto pierogowe, twaróg.").get(TasteDimension.CUISINE));
        assertEquals("śródziemnomorska", tagger.tag("Sałatka grecka",
                "Składniki: feta, oliwki.").get(TasteDimension.CUISINE));
        // Zwykła jajecznica nie ma stylu — null, nie zgadywanie.
        assertNull(tagger.tag("Jajecznica", "Składniki: jajka, masło.")
                .get(TasteDimension.CUISINE));
    }

    @Test
    public void rozpoznajeCharakterDania() {
        assertEquals("zupa", tagger.tag("Zupa krem z pomidorów", null)
                .get(TasteDimension.CHARACTER));
        assertEquals("zapiekanka", tagger.tag("Zapiekanka ziemniaczana", null)
                .get(TasteDimension.CHARACTER));
        assertEquals("kanapki", tagger.tag("Tost z awokado", null)
                .get(TasteDimension.CHARACTER));
        assertEquals("na słodko", tagger.tag("Owsianka z dodatkami", null)
                .get(TasteDimension.CHARACTER));
    }

    @Test
    public void nierozpoznaneWymiaryZostajaNullemAsMapJePomija() {
        DishTags tags = tagger.tag("Danie tajemnicze", "Składniki: niespodzianka.");
        assertTrue(tags.isEmpty());
        assertTrue(tags.asMap().isEmpty());
    }

    @Test
    public void tagujePropozycjeAiPoSkladnikachNieTylkoNazwie() {
        DishProposal proposal = new DishProposal("Miska mocy", "Szybka i sycąca.",
                "ok. 20 min", Arrays.asList("ciecierzyca", "papryka", "ryż"));
        assertEquals("strączki", tagger.tag(proposal).get(TasteDimension.BASE));
        assertTrue(tagger.tag((DishProposal) null).isEmpty());
    }

    @Test
    public void znaneWartosciWymiarowSaNiepuste() {
        for (TasteDimension dimension : TasteDimension.values()) {
            assertTrue(tagger.knownValues(dimension).size() >= 3);
        }
        assertTrue(tagger.knownValues(TasteDimension.BASE).contains("ryba"));
    }

    @Test
    public void wbudowanaPulaJestWWiekszosciOtagowanaPoBazie() {
        int total = 0;
        int tagged = 0;
        for (int meal = 0; meal < BuiltInRecipes.mealCount(); meal++) {
            for (Recipe recipe : BuiltInRecipes.forMeal(meal)) {
                total++;
                DishTags tags = tagger.tag(recipe.getTitle(), recipe.getDetails());
                if (tags.get(TasteDimension.BASE) != null) {
                    tagged++;
                }
                // Każda rozpoznana wartość musi pochodzić ze znanego słownika.
                for (TasteDimension dimension : TasteDimension.values()) {
                    String value = tags.get(dimension);
                    assertTrue(value == null
                            || tagger.knownValues(dimension).contains(value));
                }
            }
        }
        assertTrue("baza rozpoznana dla co najmniej 80% wbudowanych dań "
                        + "(jest: " + tagged + "/" + total + ")",
                tagged * 100 >= total * 80);
    }
}
