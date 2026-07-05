package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Twarde wykluczenia diety: filtrują dania po słownikach (nazwa + składniki),
 * budują bezwzględne zdanie-wymóg do promptów i nigdy nie są traktowane jako
 * miękka preferencja. Puste wykluczenia przepuszczają wszystko.
 */
public class DietConstraintsTest {

    private static DietConstraints of(DietConstraints.Exclusion... exclusions) {
        return DietConstraints.of(Arrays.asList(exclusions));
    }

    @Test
    public void pusteWykluczeniaPrzepuszczajaWszystko() {
        DietConstraints diet = DietConstraints.empty();
        assertTrue(diet.isEmpty());
        assertTrue(diet.allows("Kotlet schabowy z ziemniakami"));
        assertTrue(diet.allows(null));
        assertEquals("", diet.promptSentence());
    }

    @Test
    public void wegetarianskoBlokujeMiesoIRyby() {
        DietConstraints diet = of(DietConstraints.Exclusion.VEGETARIAN);
        assertFalse(diet.allows("Kurczak z kaszą\nSkładniki: pierś z kurczaka, kasza"));
        assertFalse(diet.allows("Kotlet schabowy"));
        assertFalse(diet.allows("Makaron z tuńczykiem"));
        assertFalse(diet.allows("Zapiekanka\nSkładniki: mięso mielone, ziemniaki"));
        assertFalse(diet.allows("Tosty\nSkładniki: pieczywo, szynka, ser"));
        assertTrue(diet.allows("Makaron z pomidorami\nSkładniki: makaron, passata, czosnek"));
        assertTrue(diet.allows("Owsianka z dodatkami"));
    }

    @Test
    public void bezWieprzowinyBlokujeSchabAleNieKurczaka() {
        DietConstraints diet = of(DietConstraints.Exclusion.NO_PORK);
        assertFalse(diet.allows("Kotlet schabowy z ziemniakami i kapustą"));
        assertFalse(diet.allows("Fasolka\nSkładniki: fasola, kiełbasa, cebula"));
        assertTrue(diet.allows("Kurczak z kaszą\nSkładniki: pierś z kurczaka"));
    }

    @Test
    public void bezLaktozyBlokujeNabialAleNieSlowaPodobne() {
        DietConstraints diet = of(DietConstraints.Exclusion.NO_LACTOSE);
        assertFalse(diet.allows("Owsianka\nSkładniki: płatki owsiane, mleko"));
        assertFalse(diet.allows("Makaron z serem i szynką"));
        assertFalse(diet.allows("Naleśniki ze śmietaną"));
        // „ser" ma łapać tylko nabiał, nie przypadkowe słowa z tym samym początkiem.
        assertTrue(diet.allows("Ryż z warzywami — na koniec serwuj z kolendrą"));
    }

    @Test
    public void bezGlutenuBlokujeMakeIPieczywo() {
        DietConstraints diet = of(DietConstraints.Exclusion.NO_GLUTEN);
        assertFalse(diet.allows("Naleśniki\nSkładniki: mąka, mleko, jajka"));
        assertFalse(diet.allows("Tosty\nSkładniki: pieczywo tostowe"));
        // Kanapki i grzanki to pieczywo z definicji — sama nazwa wystarcza
        // (maskowanie profilu widzi tylko wartość „kanapki", bez składników).
        assertFalse(diet.allows("Kanapki z pastą jajeczną"));
        assertFalse(diet.allows("Grzanki z serem"));
        assertFalse(diet.allows("kanapki"));
        assertTrue(diet.allows("Ryż z warzywami\nSkładniki: ryż, marchew, papryka"));
    }

    @Test
    public void ostrzezenieDlaPrzepisuLamiacegoDiete() {
        DietConstraints diet = of(DietConstraints.Exclusion.NO_LACTOSE);
        String warning = diet.warningFor("Placki\nSkładniki: mąka, mleko, jajka.");
        assertTrue(warning.contains("Uwaga"));
        assertTrue(warning.contains("Bez laktozy"));
        assertEquals("", diet.warningFor("Ryż z warzywami\nSkładniki: ryż, papryka."));
        assertEquals("", DietConstraints.empty().warningFor("Kotlet schabowy"));
        assertEquals("", diet.warningFor(null));
    }

    @Test
    public void bezOrzechowIBezRybDzialaLacznie() {
        DietConstraints diet = of(DietConstraints.Exclusion.NO_NUTS,
                DietConstraints.Exclusion.NO_FISH);
        assertFalse(diet.allows("Owsianka\nDodatki: miód, orzechy"));
        assertFalse(diet.allows("Sałatka z łososiem"));
        assertTrue(diet.allows("Jajecznica\nSkładniki: jajka, masło"));
    }

    @Test
    public void zdanieWymoguJestBezwzgledneIWymieniaWykluczenia() {
        String sentence = of(DietConstraints.Exclusion.NO_PORK,
                DietConstraints.Exclusion.NO_NUTS).promptSentence();
        assertTrue(sentence.contains("Bezwzględny wymóg diety"));
        assertTrue(sentence.contains("nigdy"));
        assertTrue(sentence.contains("wieprzowiny"));
        assertTrue(sentence.contains("orzechów"));
    }

    @Test
    public void ofNormalizujeNulleIDuplikaty() {
        DietConstraints diet = DietConstraints.of(Arrays.asList(
                DietConstraints.Exclusion.NO_PORK, null, DietConstraints.Exclusion.NO_PORK));
        assertEquals(1, diet.getExclusions().size());
        assertTrue(DietConstraints.of(null).isEmpty());
        assertTrue(DietConstraints.of(Collections.emptyList()).isEmpty());
    }

    @Test
    public void kazdeWykluczenieMaEtykieteIFraze() {
        for (DietConstraints.Exclusion exclusion : DietConstraints.Exclusion.values()) {
            assertFalse(exclusion.label().trim().isEmpty());
            assertFalse(exclusion.promptPhrase().trim().isEmpty());
        }
    }
}
