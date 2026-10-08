package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DishReactionLogTest {

    @Test
    public void appendKeepsOrderAndIsImmutable() {
        DishReactionLog empty = DishReactionLog.empty();
        DishReactionLog one = empty.append(new DishReaction("Pierogi", "twaróg", true, 1L));
        DishReactionLog two = one.append(new DishReaction("Flaki", "flaki", false, 2L));

        assertTrue(empty.isEmpty());
        assertEquals(1, one.size());
        assertEquals("Flaki", two.all().get(1).getDish());
        assertFalse(two.all().get(1).isLiked());
        assertEquals(2L, two.all().get(1).getTimeMillis());
    }

    @Test
    public void latestPerDishIsNewestFirstAndLaterReactionWins() {
        DishReactionLog log = DishReactionLog.empty()
                .append(new DishReaction("Pierogi", "", true, 1L))
                .append(new DishReaction("Omlet", "", true, 2L))
                .append(new DishReaction("pierogi", "", false, 3L));

        List<DishReaction> latest = log.latestPerDish(10);

        assertEquals(2, latest.size());
        assertEquals("pierogi", latest.get(0).getDish());
        assertFalse(latest.get(0).isLiked());
        assertEquals("Omlet", latest.get(1).getDish());
    }

    @Test
    public void latestPerDishRespectsLimit() {
        DishReactionLog log = DishReactionLog.empty();
        for (int i = 0; i < 50; i++) {
            log = log.append(new DishReaction("Danie " + i, "", true, i));
        }
        List<DishReaction> latest = log.latestPerDish(DishRecommender.MAX_REACTIONS);

        assertEquals(DishRecommender.MAX_REACTIONS, latest.size());
        assertEquals("Danie 49", latest.get(0).getDish());
    }

    @Test
    public void dropsOldestAboveCap() {
        DishReactionLog log = DishReactionLog.empty();
        for (int i = 0; i < DishReactionLog.MAX_STORED + 5; i++) {
            log = log.append(new DishReaction("Danie " + i, "", true, i));
        }
        assertEquals(DishReactionLog.MAX_STORED, log.size());
        assertEquals("Danie 5", log.all().get(0).getDish());
    }

    @Test
    public void ignoresBlankDishAndShortensDescription() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            longText.append("ziemniaki ");
        }
        DishReactionLog log = DishReactionLog.empty()
                .append(new DishReaction("  ", "x", true, 1L))
                .append(new DishReaction("Placki", longText.toString(), true, 1L));

        assertEquals(1, log.size());
        assertTrue(log.all().get(0).getDescription().length() <= DishReaction.MAX_DESCRIPTION);
    }

    @Test
    public void seedsFromExistingLikesWithIngredients() {
        DishReactionLog log = DishReactionLog.fromLikes(Arrays.asList("Omlet"),
                Collections.singletonMap("Omlet", "Składniki: jajka, szczypiorek\n\n1. Ubij."),
                5L);

        assertEquals(1, log.size());
        assertTrue(log.all().get(0).isLiked());
        assertEquals("składniki: jajka, szczypiorek", log.all().get(0).getDescription());
    }

    @Test
    public void serializerRoundTripsAndToleratesGarbage() {
        DishReactionSerializer serializer = new DishReactionSerializer();
        DishReactionLog log = DishReactionLog.empty()
                .append(new DishReaction("Żurek", "kiełbasa, jajko", false, 42L));

        DishReactionLog back = serializer.fromJson(serializer.toJson(log));

        assertEquals(1, back.size());
        assertEquals("Żurek", back.all().get(0).getDish());
        assertEquals("kiełbasa, jajko", back.all().get(0).getDescription());
        assertFalse(back.all().get(0).isLiked());
        assertEquals(42L, back.all().get(0).getTimeMillis());
        assertTrue(serializer.fromJson("{zepsute").isEmpty());
        assertTrue(serializer.fromJson(null).isEmpty());
    }
}
