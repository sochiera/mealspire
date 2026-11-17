package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class FoodLogTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void dopisujeWpisyIPomijaPuste() {
        FoodLog log = FoodLog.empty().append("  owsianka,\n pomidorowa ", NOW);
        assertEquals(1, log.entries().size());
        assertEquals("owsianka, pomidorowa", log.entries().get(0).getText());
        assertSame(log, log.append("   ", NOW));
    }

    @Test
    public void przycinaDlugieWpisyIStareWpisy() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longText.append('a');
        }
        FoodLog log = FoodLog.empty().append(longText.toString(), NOW);
        assertEquals(FoodLog.MAX_TEXT_LENGTH, log.entries().get(0).getText().length());
        for (int i = 0; i < FoodLog.MAX_ENTRIES + 5; i++) {
            log = log.append("wpis " + i, NOW + i);
        }
        assertEquals(FoodLog.MAX_ENTRIES, log.entries().size());
        assertEquals("wpis " + (FoodLog.MAX_ENTRIES + 4),
                log.entries().get(FoodLog.MAX_ENTRIES - 1).getText());
    }

    @Test
    public void ostatnieWpisyOdNajnowszegoDoPromptu() {
        FoodLog log = FoodLog.empty()
                .append("bigos", NOW - 5 * DAY)
                .append("schabowy", NOW - DAY)
                .append("sushi", NOW - 1000);
        assertEquals(Arrays.asList("sushi", "schabowy"), log.recentTexts(NOW, 2));
        assertEquals(Arrays.asList("niedawno zjedzone: sushi", "niedawno zjedzone: schabowy"),
                log.promptAvoidItems(NOW));
        assertEquals(Collections.emptyList(), FoodLog.empty().promptAvoidItems(NOW));
    }

    @Test
    public void serializacjaPrzezywaZapisIZepsuteDane() {
        FoodLogSerializer serializer = new FoodLogSerializer();
        FoodLog log = FoodLog.empty().append("pierogi", NOW);
        FoodLog restored = serializer.fromJson(serializer.toJson(log));
        assertEquals("pierogi", restored.entries().get(0).getText());
        assertEquals(NOW, restored.entries().get(0).getTimeMillis());
        assertTrue(serializer.fromJson("nie json").isEmpty());
        assertTrue(serializer.fromJson(null).isEmpty());
    }

    @Test
    public void domyslnieJednoPytanieWieczoremPoKolacji() {
        assertTrue(FoodLogReminder.DEFAULT_HOUR > MealSlot.DINNER.hour());
        assertTrue(FoodLogReminder.isValidHour(FoodLogReminder.DISABLED));
        assertTrue(!FoodLogReminder.isValidHour(24));
        assertEquals("20:00", FoodLogReminder.hourLabel(20));
    }
}
