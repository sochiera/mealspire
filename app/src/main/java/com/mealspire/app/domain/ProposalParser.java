package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the model's short answer into a {@link DishProposal}. The expected
 * format is four labelled lines ("Nazwa:", "Opis:", "Czas:", "Składniki:"), but
 * the parser is forgiving: it strips markdown, tolerates missing fields and
 * falls back to the first non-empty line as the dish name.
 */
public final class ProposalParser {

    public DishProposal parse(String text) {
        if (text == null) {
            return new DishProposal("", "", "", null);
        }
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);

        String name = "";
        String description = "";
        String time = "";
        List<String> ingredients = new ArrayList<>();
        String firstFreeLine = "";

        for (String raw : lines) {
            String line = stripBullet(raw.trim());
            if (line.isEmpty()) {
                continue;
            }
            String value;
            if ((value = valueFor(line, "nazwa")) != null) {
                if (name.isEmpty()) {
                    name = value;
                }
            } else if ((value = valueFor(line, "opis")) != null) {
                if (description.isEmpty()) {
                    description = value;
                }
            } else if ((value = valueFor(line, "czas")) != null) {
                if (time.isEmpty()) {
                    time = value;
                }
            } else if ((value = valueForAny(line, "składniki", "skladniki")) != null) {
                if (ingredients.isEmpty()) {
                    ingredients = splitIngredients(value);
                }
            } else if (firstFreeLine.isEmpty()) {
                firstFreeLine = stripMarkdown(line);
            }
        }

        if (name.isEmpty()) {
            name = firstFreeLine;
        }
        return new DishProposal(name, description, time, ingredients);
    }

    private static String valueForAny(String line, String... labels) {
        for (String label : labels) {
            String value = valueFor(line, label);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** Returns the value after "label:" (case-insensitive), or null if the line isn't that field. */
    private static String valueFor(String line, String label) {
        String cleaned = stripMarkdown(line);
        int colon = cleaned.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String key = stripMarkdown(cleaned.substring(0, colon).trim()).toLowerCase();
        if (!key.equals(label)) {
            return null;
        }
        return stripMarkdown(cleaned.substring(colon + 1).trim());
    }

    private static List<String> splitIngredients(String value) {
        List<String> result = new ArrayList<>();
        for (String part : value.split("[,;]")) {
            String item = stripMarkdown(part.trim());
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    private static String stripBullet(String line) {
        String result = line;
        while (result.startsWith("-") || result.startsWith("*") || result.startsWith("•")) {
            result = result.substring(1).trim();
        }
        return result;
    }

    private static String stripMarkdown(String text) {
        String result = text;
        while (result.startsWith("#")) {
            result = result.substring(1);
        }
        result = result.replace("**", "").replace("__", "").trim();
        if (result.startsWith("*") && result.endsWith("*") && result.length() > 1) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }
}
