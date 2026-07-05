package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Skompresowany kontekst gustu do promptów: top wartości wymiarów dla slotu
 * (z fallbackiem ogólnym dla cienkiego slotu), maskowanie wykluczeniami diety
 * i budżetowana lista przykładowych dań.
 */
public class TasteContextBuilderTest {

    private static final long NOW = 1000L * 24 * 60 * 60 * 1000;

    private final DishTagger tagger = new DishTagger();
    private final TasteContextBuilder builder = new TasteContextBuilder();

    private TasteEventLog likedLunches(String... dishes) {
        TasteEventLog log = TasteEventLog.empty();
        long ts = NOW;
        for (String dish : dishes) {
            log = log.append(new TasteEvent(TasteEvent.Type.LIKED, dish, 1, ts++));
        }
        return log;
    }

    private TasteModel modelOf(TasteEventLog log) {
        return TasteModel.build(log, FrozenTasteAggregate.empty(), tagger, null, NOW);
    }

    @Test
    public void profilZawieraTopWartosciWymiarowIPrzyklady() {
        TasteEventLog log = likedLunches("Kurczak z ryżem", "Kurczak pieczony",
                "Kurczak w curry", "Zupa pomidorowa", "Zupa ogórkowa",
                "Łosoś z pieca", "Pierogi z serem", "Rosół z makaronem");
        TasteContext context = builder.build(modelOf(log), log,
                DietConstraints.empty(), 1);

        String joined = String.join(" ", context.getProfileSentences());
        assertTrue(joined.contains("bazą"));
        assertTrue(joined.contains("kurczak"));
        assertTrue(joined.contains("charakter"));
        assertTrue(joined.contains("zupa"));
        assertFalse(context.getExampleDishes().isEmpty());
        assertEquals("Rosół z makaronem", context.getExampleDishes().get(0));
    }

    @Test
    public void cienkiSlotDostajeProfilOgolny() {
        // 8 obiadowych lajków, zero śniadaniowych: kontekst śniadania nie może
        // być pusty — bierze profil ogólny.
        TasteEventLog log = likedLunches("Kurczak z ryżem", "Kurczak pieczony",
                "Kurczak w curry", "Kurczak po tajsku", "Zupa pomidorowa",
                "Zupa ogórkowa", "Łosoś z pieca", "Pierogi ruskie");
        TasteContext breakfast = builder.build(modelOf(log), log,
                DietConstraints.empty(), 0);

        String joined = String.join(" ", breakfast.getProfileSentences());
        assertTrue("cienki slot ma dostać profil ogólny", joined.contains("kurczak"));
    }

    @Test
    public void wykluczeniaMaskujaWyuczonyProfilIPrzyklady() {
        TasteEventLog log = likedLunches("Gulasz wieprzowy", "Schabowy z kapustą",
                "Kiełbasa z grilla", "Zupa pomidorowa", "Ryż z warzywami",
                "Leczo z cukinią", "Placki ziemniaczane", "Krem z dyni");
        DietConstraints noPork = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.NO_PORK));

        TasteContext context = builder.build(modelOf(log), log, noPork, 1);

        String joined = String.join(" ", context.getProfileSentences());
        assertFalse("po zmianie diety wieprzowina nie może wracać z historii",
                joined.contains("wieprzowina"));
        for (String example : context.getExampleDishes()) {
            assertTrue("przykład łamie dietę: " + example, noPork.allows(example));
        }
    }

    @Test
    public void przykladySaBudzetowane() {
        List<String> dishes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            dishes.add("Danie numer " + i);
        }
        TasteEventLog log = likedLunches(dishes.toArray(new String[0]));
        TasteContext context = builder.build(modelOf(log), log,
                DietConstraints.empty(), 1);
        assertTrue(context.getExampleDishes().size()
                <= TasteContextBuilder.MAX_EXAMPLES);
    }

    @Test
    public void pustyModelDajePustyKontekst() {
        TasteContext context = builder.build(modelOf(TasteEventLog.empty()),
                TasteEventLog.empty(), DietConstraints.empty(), 1);
        assertTrue(context.isEmpty());
        assertTrue(builder.build(null, null, null, 1).isEmpty());
    }
}
