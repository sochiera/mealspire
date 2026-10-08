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
    private final DishReactionStore reactionStore;

    public DataManager(PreferenceStore preferenceStore, MealHistoryStore historyStore,
                       CookbookStore cookbookStore) {
        this(preferenceStore, historyStore, cookbookStore, null);
    }

    public DataManager(PreferenceStore preferenceStore, MealHistoryStore historyStore,
                       CookbookStore cookbookStore, DishReactionStore reactionStore) {
        this.preferenceStore = preferenceStore;
        this.historyStore = historyStore;
        this.cookbookStore = cookbookStore;
        this.reactionStore = reactionStore;
    }

    /** Czyści polubienia i listę reakcji lubię/nie lubię — jedno „zapomnij gust". */
    public void clearPreferences() {
        preferenceStore.save(UserPreferences.empty());
        if (reactionStore != null) {
            reactionStore.save(DishReactionLog.empty());
        }
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
