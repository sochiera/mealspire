package com.mealspire.app.domain;

/**
 * The three daily reminder slots, each tying together the hour the notification
 * fires, the meal label shown to the user, and the meal index into
 * {@link BuiltInRecipes} / the screen's meal buttons.
 */
public enum MealSlot {
    BREAKFAST(8, "Śniadanie", 0),
    LUNCH(12, "Obiad", 1),
    DINNER(18, "Kolacja", 2);

    private final int hour;
    private final String label;
    private final int mealIndex;

    MealSlot(int hour, String label, int mealIndex) {
        this.hour = hour;
        this.label = label;
        this.mealIndex = mealIndex;
    }

    /** Hour of day (24h) the reminder fires: 8, 12 or 18. */
    public int hour() {
        return hour;
    }

    /** Human label for the meal: "Śniadanie", "Obiad", "Kolacja". */
    public String label() {
        return label;
    }

    /** Index into the built-in recipe catalogue and the screen's meal buttons. */
    public int mealIndex() {
        return mealIndex;
    }

    /** The slot for a meal index, or {@code null} if out of range. */
    public static MealSlot byMealIndex(int mealIndex) {
        for (MealSlot slot : values()) {
            if (slot.mealIndex == mealIndex) {
                return slot;
            }
        }
        return null;
    }
}
