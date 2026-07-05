package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Migracja starych polubień do dziennika zdarzeń: tylko przy pustym dzienniku,
 * jako zdarzenia LIKED z bieżącym czasem. Idempotentna.
 */
public class TasteEventMigrationTest {

    @Test
    public void polubieniaStajaSieZdarzeniamiLiked() {
        UserPreferences prefs = new UserPreferences(
                Arrays.asList("Omlet", "Zupa"), Collections.<String>emptyList());

        TasteEventLog migrated = TasteEventMigration.migrate(
                TasteEventLog.empty(), prefs, 1000L);

        assertEquals(2, migrated.size());
        assertEquals(TasteEvent.Type.LIKED, migrated.events().get(0).getType());
        assertEquals("Omlet", migrated.events().get(0).getDishTitle());
        assertEquals(1000L, migrated.events().get(0).getTimestamp());
        assertEquals(TasteEvent.NO_MEAL, migrated.events().get(0).getMealIndex());
    }

    @Test
    public void niepustyDziennikNieJestRuszany() {
        TasteEventLog existing = TasteEventLog.empty()
                .append(new TasteEvent(TasteEvent.Type.LIKED, "Curry", 1, 5L));
        UserPreferences prefs = new UserPreferences(
                Arrays.asList("Omlet"), Collections.<String>emptyList());

        TasteEventLog result = TasteEventMigration.migrate(existing, prefs, 1000L);

        assertEquals(1, result.size());
        assertEquals("Curry", result.events().get(0).getDishTitle());
    }

    @Test
    public void pusteWejsciaNieWywracajaMigracji() {
        assertTrue(TasteEventMigration.migrate(null, null, 1L).isEmpty());
        assertTrue(TasteEventMigration.migrate(
                TasteEventLog.empty(), UserPreferences.empty(), 1L).isEmpty());
    }
}
