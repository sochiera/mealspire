package com.mealspire.app.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ReadyDishPoolTest {

    @Test
    public void roundTripsThroughJson() {
        ReadyDishPool pool = new ReadyDishPool("sig",
                Arrays.asList(new DishRating("Curry", 9, "Lubicie ostre."),
                        new DishRating("Pierogi", 7, "")),
                Collections.singletonList("Zupa"));
        ReadyDishPoolSerializer serializer = new ReadyDishPoolSerializer();

        ReadyDishPool restored = serializer.fromJson(serializer.toJson(pool));

        assertEquals("sig", restored.getSignature());
        assertEquals(2, restored.size());
        assertEquals("Curry", restored.getEntries().get(0).getDish());
        assertEquals(9, restored.getEntries().get(0).getScore());
        assertEquals("Lubicie ostre.", restored.getEntries().get(0).getReason());
        assertEquals(new java.util.HashSet<>(Arrays.asList("curry", "pierogi", "zupa")),
                restored.getRated());
    }

    @Test
    public void corruptJsonIsAnEmptyPool() {
        ReadyDishPoolSerializer serializer = new ReadyDishPoolSerializer();
        assertEquals(0, serializer.fromJson("{nie json").size());
        assertEquals(0, serializer.fromJson(null).size());
        assertEquals("", serializer.fromJson("").getSignature());
    }

    @Test
    public void signatureFollowsReactionsDietAndModel() {
        DishReactionLog none = DishReactionLog.empty();
        DishReactionLog liked = none.append(new DishReaction("Curry", "", true, 1L));
        DietConstraints vegan = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));
        String base = ReadyDishPool.signature(none, DietConstraints.empty(), "LUNA");

        assertEquals(base, ReadyDishPool.signature(none, DietConstraints.empty(), "LUNA"));
        assertNotEquals(base, ReadyDishPool.signature(liked, DietConstraints.empty(), "LUNA"));
        assertNotEquals(base, ReadyDishPool.signature(none, vegan, "LUNA"));
        assertNotEquals(base, ReadyDishPool.signature(none, DietConstraints.empty(), "SOL"));
        // Changing one's mind about a dish is a new taste.
        assertNotEquals(ReadyDishPool.signature(liked, DietConstraints.empty(), "LUNA"),
                ReadyDishPool.signature(liked.append(new DishReaction("Curry", "", false, 2L)),
                        DietConstraints.empty(), "LUNA"));
    }

    @Test
    public void availableIgnoresShownDishes() {
        ReadyDishPool pool = new ReadyDishPool("s",
                Arrays.asList(new DishRating("A", 8, ""), new DishRating("B", 8, "")),
                Collections.<String>emptyList());
        assertEquals(1, pool.available(Collections.singleton("a")));
        assertTrue(pool.isCurrent("s"));
    }
}
