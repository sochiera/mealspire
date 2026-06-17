package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable record of when each dish was last chosen, used to surface meals the
 * user hasn't had in a while. Titles are keyed case-insensitively (the most
 * recent spelling is kept for display).
 */
public final class MealHistory {

    private final Map<String, Long> lastEaten;

    public MealHistory(Map<String, Long> lastEaten) {
        Map<String, Long> copy = new LinkedHashMap<>();
        if (lastEaten != null) {
            for (Map.Entry<String, Long> entry : lastEaten.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().trim().isEmpty()
                        && entry.getValue() != null) {
                    copy.put(entry.getKey().trim(), entry.getValue());
                }
            }
        }
        this.lastEaten = Collections.unmodifiableMap(copy);
    }

    public static MealHistory empty() {
        return new MealHistory(Collections.<String, Long>emptyMap());
    }

    public MealHistory record(String title, long timestamp) {
        if (title == null || title.trim().isEmpty()) {
            return this;
        }
        String trimmed = title.trim();
        Map<String, Long> updated = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : lastEaten.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(trimmed)) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        updated.put(trimmed, timestamp);
        return new MealHistory(updated);
    }

    /** Returns the last timestamp this dish was chosen, or 0 if never. */
    public long lastEatenAt(String title) {
        if (title == null) {
            return 0L;
        }
        for (Map.Entry<String, Long> entry : lastEaten.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(title.trim())) {
                return entry.getValue();
            }
        }
        return 0L;
    }

    /** Dish titles ordered most-recent first, capped at {@code limit}. */
    public List<String> recentTitles(int limit) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(lastEaten.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return Long.compare(b.getValue(), a.getValue());
            }
        });
        List<String> titles = new ArrayList<>();
        for (Map.Entry<String, Long> entry : entries) {
            if (titles.size() >= limit) {
                break;
            }
            titles.add(entry.getKey());
        }
        return titles;
    }

    /** Exposes a snapshot for serialization. */
    public Map<String, Long> asMap() {
        return lastEaten;
    }
}
