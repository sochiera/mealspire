package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DataManagerTest {

    private static class FakePreferenceStore implements PreferenceStore {
        UserPreferences value = UserPreferences.empty();
        public UserPreferences load() { return value; }
        public void save(UserPreferences preferences) { value = preferences; }
    }

    private static class FakeHistoryStore implements MealHistoryStore {
        MealHistory value = MealHistory.empty();
        public MealHistory load() { return value; }
        public void save(MealHistory history) { value = history; }
    }

    private static class FakeCookbookStore implements CookbookStore {
        Cookbook value = Cookbook.empty();
        public Cookbook load() { return value; }
        public void save(Cookbook cookbook) { value = cookbook; }
    }

    private final FakePreferenceStore prefs = new FakePreferenceStore();
    private final FakeHistoryStore history = new FakeHistoryStore();
    private final FakeCookbookStore cookbook = new FakeCookbookStore();
    private final DataManager manager = new DataManager(prefs, history, cookbook);

    @Test
    public void clearPreferencesEmptiesThem() {
        prefs.value = UserPreferences.empty().withLike("ryż");
        manager.clearPreferences();
        assertTrue(prefs.load().getLikes().isEmpty());
    }

    @Test
    public void clearHistoryEmptiesIt() {
        history.value = MealHistory.empty().record("Pizza", 1L);
        manager.clearHistory();
        assertTrue(history.load().recentTitles(10).isEmpty());
    }

    @Test
    public void clearCookbookEmptiesIt() {
        cookbook.value = Cookbook.empty().add(new CookbookEntry("Bigos", "opis", "ręcznie"));
        manager.clearCookbook();
        assertTrue(cookbook.load().getEntries().isEmpty());
    }

    @Test
    public void removeDishRemovesOneEntryAndPersists() {
        cookbook.value = Cookbook.empty()
                .add(new CookbookEntry("Bigos", "o", ""))
                .add(new CookbookEntry("Żurek", "o", ""));
        Cookbook updated = manager.removeDish("Bigos");
        assertFalse(updated.contains("Bigos"));
        assertTrue(updated.contains("Żurek"));
        assertFalse(cookbook.load().contains("Bigos"));
    }

    @Test
    public void clearAllEmptiesEverything() {
        prefs.value = UserPreferences.empty().withLike("ryż");
        history.value = MealHistory.empty().record("Pizza", 1L);
        cookbook.value = Cookbook.empty().add(new CookbookEntry("Bigos", "o", ""));
        manager.clearAll();
        assertTrue(prefs.load().getLikes().isEmpty());
        assertTrue(history.load().recentTitles(10).isEmpty());
        assertTrue(cookbook.load().getEntries().isEmpty());
    }
}
