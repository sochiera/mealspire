package com.mealspire.app.domain;

/**
 * One dish in the user's personal cookbook: a title, an optional recipe body, and
 * an optional source note (e.g. "ręcznie", "AI", or a URL it came from).
 */
public final class CookbookEntry {
    private final String title;
    private final String recipe;
    private final String source;

    public CookbookEntry(String title, String recipe, String source) {
        this.title = title == null ? "" : title.trim();
        this.recipe = recipe == null ? "" : recipe;
        this.source = source == null ? "" : source;
    }

    public String getTitle() {
        return title;
    }

    public String getRecipe() {
        return recipe;
    }

    public String getSource() {
        return source;
    }

    public Recipe toRecipe() {
        return new Recipe(title, recipe);
    }
}
