package com.mealspire.app.domain;

/**
 * Central point for clearing the data the app has learned, so the user stays in
 * control of what's stored. Works against the store interfaces, so it's fully
 * unit-testable with in-memory fakes.
 */
public final class DataManager {

    private final PreferenceStore preferenceStore;
    private final MealHistoryStore historyStore;
    private final CookbookStore cookbookStore;

    public DataManager(PreferenceStore preferenceStore, MealHistoryStore historyStore,
                       CookbookStore cookbookStore) {
        this.preferenceStore = preferenceStore;
        this.historyStore = historyStore;
        this.cookbookStore = cookbookStore;
    }

    public void clearPreferences() {
        preferenceStore.save(UserPreferences.empty());
    }

    public void clearHistory() {
        historyStore.save(MealHistory.empty());
    }

    public void clearCookbook() {
        cookbookStore.save(Cookbook.empty());
    }

    public Cookbook removeDish(String title) {
        Cookbook updated = cookbookStore.load().remove(title);
        cookbookStore.save(updated);
        return updated;
    }

    public void clearAll() {
        clearPreferences();
        clearHistory();
        clearCookbook();
    }
}
