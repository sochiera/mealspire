package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lightweight meal suggestion shown <em>before</em> a full recipe is
 * generated: just the dish name, a one-line description, an approximate
 * preparation time and the key ingredients. This lets the user quickly accept
 * or skip an idea without waiting for a whole recipe to be produced.
 */
public final class DishProposal {

    private final String name;
    private final String description;
    private final String time;
    private final List<String> keyIngredients;

    public DishProposal(String name, String description, String time,
                        List<String> keyIngredients) {
        this.name = name == null ? "" : name.trim();
        this.description = description == null ? "" : description.trim();
        this.time = time == null ? "" : time.trim();
        this.keyIngredients = keyIngredients == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(keyIngredients));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public List<String> getKeyIngredients() {
        return keyIngredients;
    }

    public boolean isEmpty() {
        return name.isEmpty();
    }

    /** Human-readable, multi-line body for the proposal screen (no title). */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (!description.isEmpty()) {
            sb.append(description);
        }
        if (!time.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Czas: ").append(time);
        }
        if (!keyIngredients.isEmpty()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("Kluczowe składniki: ").append(join(keyIngredients));
        }
        return sb.toString().trim();
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
