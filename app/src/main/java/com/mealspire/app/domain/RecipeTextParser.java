package com.mealspire.app.domain;

/**
 * Turns the model's free-text answer into a {@link Recipe}: the first non-empty
 * line is the title, the remaining lines are the recipe body.
 */
public final class RecipeTextParser {

    public Recipe parse(String text) {
        if (text == null) {
            return new Recipe("", "");
        }
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        int titleIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                titleIndex = i;
                break;
            }
        }
        if (titleIndex < 0) {
            return new Recipe("", "");
        }

        String title = stripMarkdown(lines[titleIndex].trim());

        StringBuilder details = new StringBuilder();
        for (int i = titleIndex + 1; i < lines.length; i++) {
            details.append(lines[i]);
            if (i < lines.length - 1) {
                details.append('\n');
            }
        }

        return new Recipe(title, details.toString().trim());
    }

    private static String stripMarkdown(String line) {
        // Drop leading heading markers ("# ") and surrounding bold/italic markers.
        String result = line;
        while (result.startsWith("#")) {
            result = result.substring(1);
        }
        result = result.trim();
        result = result.replace("**", "").replace("__", "");
        if (result.startsWith("*") && result.endsWith("*") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
        }
        return result.trim();
    }
}
