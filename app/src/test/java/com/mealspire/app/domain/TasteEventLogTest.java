package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * Dziennik zdarzeń gustu: chronologiczny, niemutowalny, z przycinaniem do
 * najnowszych zdarzeń (nadmiar idzie do kompakcji, nie do kosza) i widokiem
 * „ostatnio polubione" dla promptów.
 */
public class TasteEventLogTest {

    private static TasteEvent liked(String dish, long ts) {
        return new TasteEvent(TasteEvent.Type.LIKED, dish, 1, ts);
    }

    @Test
    public void appendZachowujeKolejnoscInieMutuje() {
        TasteEventLog empty = TasteEventLog.empty();
        TasteEventLog one = empty.append(liked("Omlet", 1));
        TasteEventLog two = one.append(liked("Zupa", 2));

        assertTrue(empty.isEmpty());
        assertEquals(1, one.size());
        assertEquals(2, two.size());
        assertEquals("Omlet", two.events().get(0).getDishTitle());
        assertEquals("Zupa", two.events().get(1).getDishTitle());
    }

    @Test
    public void niepoprawneZdarzeniaSaPomijane() {
        TasteEventLog log = TasteEventLog.empty()
                .append(null)
                .append(new TasteEvent(TasteEvent.Type.LIKED, "  ", 0, 1));
        assertTrue(log.isEmpty());
    }

    @Test
    public void overflowITrimDzielaDziennikNaStareINowe() {
        TasteEventLog log = TasteEventLog.empty();
        for (int i = 0; i < 10; i++) {
            log = log.append(liked("Danie " + i, i));
        }

        assertEquals(3, log.overflow(7).size());
        assertEquals("Danie 0", log.overflow(7).get(0).getDishTitle());
        TasteEventLog trimmed = log.trimToNewest(7);
        assertEquals(7, trimmed.size());
        assertEquals("Danie 3", trimmed.events().get(0).getDishTitle());
        // Poniżej limitu nic się nie dzieje.
        assertTrue(log.overflow(100).isEmpty());
        assertEquals(10, log.trimToNewest(100).size());
    }

    @Test
    public void recentLikedTitlesOdNajswiezszegoBezDuplikatow() {
        TasteEventLog log = TasteEventLog.empty()
                .append(liked("Omlet", 1))
                .append(new TasteEvent(TasteEvent.Type.IMPORTED, "Curry", 1, 2))
                .append(new TasteEvent(TasteEvent.Type.RECIPE_VIEWED, "Zupa", 1, 3))
                .append(new TasteEvent(TasteEvent.Type.REROLLED, "Bigos", 1, 4))
                .append(liked("omlet", 5));

        // RECIPE_VIEWED (dorozumiane) i REROLLED (negatywne) nie są „ulubione";
        // duplikat tytułu (inną wielkością liter) nie pojawia się dwa razy.
        assertEquals(Arrays.asList("omlet", "Curry"), log.recentLikedTitles(5));
        assertEquals(Arrays.asList("omlet"), log.recentLikedTitles(1));
    }

    @Test
    public void wagiTypowSaZgodneZDesignem() {
        assertTrue(TasteEvent.Type.LIKED.weight() > TasteEvent.Type.ONBOARDING_PICK.weight());
        assertTrue(TasteEvent.Type.ONBOARDING_PICK.weight()
                > TasteEvent.Type.RECIPE_VIEWED.weight());
        assertTrue(TasteEvent.Type.REROLLED.weight() < 0);
        assertTrue(TasteEvent.Type.SHOWN_NOT_CHOSEN.weight() < 0);
        // Negatywy są słabe: najmocniejszy z nich jest wielokrotnie lżejszy od lajka.
        assertTrue(Math.abs(TasteEvent.Type.REROLLED.weight())
                < TasteEvent.Type.LIKED.weight() / 5);
    }

    @Test
    public void zdarzenieEksploracyjneNieNiesieNegatywu() {
        TasteEvent negative = new TasteEvent(TasteEvent.Type.REROLLED, "Ryba", 1, 1, true);
        TasteEvent positive = new TasteEvent(TasteEvent.Type.LIKED, "Ryba", 1, 1, true);
        assertEquals(0.0, negative.effectiveWeight(), 0.0001);
        assertEquals(TasteEvent.Type.LIKED.weight(), positive.effectiveWeight(), 0.0001);
    }
}
