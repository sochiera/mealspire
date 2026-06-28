package com.mealspire.app.domain;

import java.util.List;

/**
 * Title and body text for a meal-reminder notification, built from the meal
 * label and the proposed dish names. Falls back to a gentle nudge when there are
 * no proposals to show.
 */
public final class MealNotificationContent {

    private static final String SEPARATOR = " · ";

    private final String title;
    private final String text;

    private MealNotificationContent(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public static MealNotificationContent forMeal(String mealLabel, List<String> proposalNames) {
        String title = mealLabel + " — pomysły na dziś";
        StringBuilder body = new StringBuilder();
        if (proposalNames != null) {
            for (String name : proposalNames) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                if (body.length() > 0) {
                    body.append(SEPARATOR);
                }
                body.append(name.trim());
            }
        }
        String text = body.length() > 0 ? body.toString() : "Otwórz, by zobaczyć pomysły.";
        return new MealNotificationContent(title, text);
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}
