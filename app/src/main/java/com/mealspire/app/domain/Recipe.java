package com.mealspire.app.domain;

/**
 * A meal idea with a title and a free-text recipe body (ingredients + steps).
 */
public final class Recipe {
    private final String title;
    private final String details;

    public Recipe(String title, String details) {
        this.title = title == null ? "" : title;
        this.details = details == null ? "" : details;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }
}
