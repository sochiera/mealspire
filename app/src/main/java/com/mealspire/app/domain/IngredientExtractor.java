package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pulls a shopping list out of a recipe body. Recognizes an "Składniki" section
 * (inline comma-separated or bulleted) and stops at the preparation steps.
 */
public final class IngredientExtractor {

    private static final Pattern NUMBERED_STEP = Pattern.compile("^\\d+[.)].*");
    private static final Pattern LEADING_MARKUP = Pattern.compile("^[\\*#\\s>]+");

    public List<String> extract(String details) {
        Set<String> items = new LinkedHashSet<>();
        if (details == null || details.trim().isEmpty()) {
            return new ArrayList<>(items);
        }

        boolean inIngredients = false;
        for (String rawLine : details.replace("\r\n", "\n").split("\n", -1)) {
            String line = stripMarkup(rawLine.trim());

            if (!inIngredients) {
                if (line.toLowerCase().startsWith("składnik")) {
                    inIngredients = true;
                    int colon = line.indexOf(':');
                    if (colon >= 0 && colon < line.length() - 1) {
                        addCommaSeparated(items, line.substring(colon + 1));
                    }
                }
                continue;
            }

            if (line.isEmpty()) {
                if (!items.isEmpty()) {
                    break; // blank line after items ends the ingredient block
                }
                continue;
            }
            if (isStepSection(line) || NUMBERED_STEP.matcher(line).matches()) {
                break;
            }
            addCommaSeparated(items, line);
        }

        return new ArrayList<>(items);
    }

    private static boolean isStepSection(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("sposób") || lower.startsWith("przygotowan")
                || lower.startsWith("wykonan") || lower.startsWith("kroki");
    }

    private static void addCommaSeparated(Set<String> items, String text) {
        for (String part : text.split(",")) {
            String item = clean(part);
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
    }

    private static String clean(String value) {
        String result = stripMarkup(value.trim());
        while (result.endsWith(".") || result.endsWith(",") || result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result.trim();
    }

    private static String stripMarkup(String value) {
        String result = LEADING_MARKUP.matcher(value).replaceAll("");
        if (result.startsWith("- ")) {
            result = result.substring(2);
        } else if (result.startsWith("-")) {
            result = result.substring(1);
        }
        if (result.startsWith("•")) {
            result = result.substring(1);
        }
        return result.replace("**", "").trim();
    }
}
