package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Lokalne liczniki uczenia: acceptance/reroll rate, jakość tagowania AI,
 * odporny tekst diagnostyczny i serializacja.
 */
public class LearningStatsTest {

    @Test
    public void licznikiRosnaNiemutujaco() {
        LearningStats stats = LearningStats.empty()
                .withTrioShown().withTrioShown().withTrioShown().withTrioShown()
                .withTrioEngaged()
                .withReroll()
                .withAiProposals(3, 1);

        assertEquals(4, stats.getTriosShown());
        assertEquals(1, stats.getTriosEngaged());
        assertEquals(1, stats.getRerolls());
        assertEquals(3, stats.getAiProposals());
        assertEquals(1, stats.getAiProposalsUntagged());
        assertEquals(0.25, stats.acceptanceRate(), 0.001);
        assertEquals(0.25, stats.rerollRate(), 0.001);
    }

    @Test
    public void pusteLicznikiNieDzielaPrzezZero() {
        assertEquals(0.0, LearningStats.empty().acceptanceRate(), 0.001);
        assertEquals(0.0, LearningStats.empty().rerollRate(), 0.001);
    }

    @Test
    public void tekstDiagnostycznyZawieraWszystkieLiczby() {
        String text = LearningStats.empty()
                .withTrioShown().withTrioShown()
                .withTrioEngaged()
                .withAiProposals(6, 2)
                .summaryText(42);

        assertTrue(text.contains("Pokazane zestawy propozycji: 2"));
        assertTrue(text.contains("Zestawy z wyborem: 1 (50%)"));
        assertTrue(text.contains("Propozycje od AI: 6"));
        assertTrue(text.contains("bez rozpoznanej bazy: 2 (33%)"));
        assertTrue(text.contains("Zdarzeń gustu w dzienniku: 42"));
    }

    @Test
    public void serializacjaPrzezywaRoundtripISmieci() {
        LearningStatsSerializer serializer = new LearningStatsSerializer();
        LearningStats original = LearningStats.empty()
                .withTrioShown().withTrioEngaged().withReroll().withAiProposals(2, 1);

        LearningStats restored = serializer.fromJson(serializer.toJson(original));

        assertEquals(1, restored.getTriosShown());
        assertEquals(1, restored.getTriosEngaged());
        assertEquals(1, restored.getRerolls());
        assertEquals(2, restored.getAiProposals());
        assertEquals(1, restored.getAiProposalsUntagged());
        assertEquals(0, serializer.fromJson(null).getTriosShown());
        assertEquals(0, serializer.fromJson("śmieci").getTriosShown());
    }
}
