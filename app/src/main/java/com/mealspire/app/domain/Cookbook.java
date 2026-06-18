package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable collection of the user's known/liked dishes. Entries are keyed by
 * title case-insensitively; adding an existing title updates it in place.
 */
public final class Cookbook {

    private final Map<String, CookbookEntry> entries;

    public Cookbook(Collection<CookbookEntry> entries) {
        Map<String, CookbookEntry> map = new LinkedHashMap<>();
        if (entries != null) {
            for (CookbookEntry entry : entries) {
                if (entry != null && !entry.getTitle().trim().isEmpty()) {
                    map.put(entry.getTitle().toLowerCase(), entry);
                }
            }
        }
        this.entries = Collections.unmodifiableMap(map);
    }

    public static Cookbook empty() {
        return new Cookbook(Collections.<CookbookEntry>emptyList());
    }

    public Cookbook add(CookbookEntry entry) {
        if (entry == null || entry.getTitle().trim().isEmpty()) {
            return this;
        }
        Map<String, CookbookEntry> updated = new LinkedHashMap<>(entries);
        updated.put(entry.getTitle().toLowerCase(), entry);
        return new Cookbook(updated.values());
    }

    public Cookbook remove(String title) {
        if (title == null) {
            return this;
        }
        Map<String, CookbookEntry> updated = new LinkedHashMap<>(entries);
        updated.remove(title.trim().toLowerCase());
        return new Cookbook(updated.values());
    }

    public boolean contains(String title) {
        return title != null && entries.containsKey(title.trim().toLowerCase());
    }

    public List<CookbookEntry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public List<String> titles() {
        List<String> titles = new ArrayList<>();
        for (CookbookEntry entry : entries.values()) {
            titles.add(entry.getTitle());
        }
        return titles;
    }
}
