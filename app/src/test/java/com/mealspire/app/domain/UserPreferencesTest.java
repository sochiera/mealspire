package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class UserPreferencesTest {

    @Test
    public void emptyHasNoLikesOrDislikes() {
        UserPreferences prefs = UserPreferences.empty();
        assertTrue(prefs.getLikes().isEmpty());
        assertTrue(prefs.getDislikes().isEmpty());
    }

    @Test
    public void withLikeAddsItem() {
        UserPreferences prefs = UserPreferences.empty().withLike("Pierogi ruskie");
        assertTrue(prefs.getLikes().contains("Pierogi ruskie"));
    }

    @Test
    public void likingSameItemTwiceCaseInsensitiveDoesNotDuplicate() {
        UserPreferences prefs = UserPreferences.empty()
                .withLike("Pizza")
                .withLike("pizza");
        assertEquals(1, prefs.getLikes().size());
    }

    @Test
    public void likingAnItemRemovesItFromDislikes() {
        UserPreferences prefs = UserPreferences.empty()
                .withDislike("Brokuły")
                .withLike("brokuły");
        assertTrue(prefs.getLikes().contains("brokuły"));
        assertFalse(containsIgnoreCase(prefs.getDislikes(), "brokuły"));
    }

    @Test
    public void blankFeedbackIsIgnored() {
        UserPreferences prefs = UserPreferences.empty().withLike("   ").withDislike(null);
        assertTrue(prefs.getLikes().isEmpty());
        assertTrue(prefs.getDislikes().isEmpty());
    }

    @Test
    public void isImmutable_originalUnchangedAfterWith() {
        UserPreferences original = UserPreferences.empty();
        original.withLike("Zupa");
        assertTrue(original.getLikes().isEmpty());
    }

    @Test
    public void buildsFromExistingCollections() {
        UserPreferences prefs = new UserPreferences(
                Arrays.asList("Makaron", "makaron"), Arrays.asList("Kalafior"));
        assertEquals(1, prefs.getLikes().size());
        assertTrue(prefs.getDislikes().contains("Kalafior"));
    }

    private static boolean containsIgnoreCase(Iterable<String> items, String value) {
        for (String item : items) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
