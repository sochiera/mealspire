package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class DishRatingParserTest {

    private final DishRatingParser parser = new DishRatingParser();

    @Test
    public void parsesRatingsObject() throws Exception {
        List<DishRating> ratings = parser.parse("{\"oceny\":["
                + "{\"danie\":\"Pierogi ruskie\",\"ocena\":9,\"powod\":\"Lubi ziemniaki i twaróg.\"},"
                + "{\"danie\":\"Sałatka grecka\",\"ocena\":3,\"powod\":\"Nie lubi oliwek.\"}]}");

        assertEquals(2, ratings.size());
        assertEquals("Pierogi ruskie", ratings.get(0).getDish());
        assertEquals(9, ratings.get(0).getScore());
        assertEquals("Lubi ziemniaki i twaróg.", ratings.get(0).getReason());
        assertEquals(3, ratings.get(1).getScore());
    }

    @Test
    public void toleratesMarkdownFenceBareArrayAndStringScore() throws Exception {
        List<DishRating> ratings = parser.parse("Oto oceny:\n```json\n"
                + "[{\"danie\":\"Omlet\",\"ocena\":\"7.6\",\"powód\":\"Szybki.\"}]\n```");

        assertEquals(1, ratings.size());
        assertEquals(8, ratings.get(0).getScore());
        assertEquals("Szybki.", ratings.get(0).getReason());
    }

    @Test
    public void clampsScoreToZeroTen() throws Exception {
        List<DishRating> ratings = parser.parse("{\"oceny\":[{\"danie\":\"A\",\"ocena\":14},"
                + "{\"danie\":\"B\",\"ocena\":-2}]}");

        assertEquals(10, ratings.get(0).getScore());
        assertEquals(0, ratings.get(1).getScore());
    }

    @Test
    public void skipsBrokenItemsButKeepsValidOnes() throws Exception {
        List<DishRating> ratings = parser.parse("{\"oceny\":[{\"danie\":\"\",\"ocena\":5},"
                + "{\"danie\":\"Bez oceny\"},\"tekst\",{\"danie\":\"Zupa\",\"ocena\":6}]}");

        assertEquals(1, ratings.size());
        assertEquals("Zupa", ratings.get(0).getDish());
    }

    @Test
    public void malformedJsonThrows() {
        assertUnreadable("{\"oceny\":[{\"danie\":\"Zupa\",\"ocena\":");
        assertUnreadable("Nie potrafię ocenić tych dań.");
        assertUnreadable("{\"oceny\":[]}");
        assertUnreadable("");
        assertUnreadable(null);
    }

    private void assertUnreadable(String text) {
        try {
            parser.parse(text);
            fail("expected IOException for: " + text);
        } catch (IOException expected) {
            // fallback offline
        }
    }
}
