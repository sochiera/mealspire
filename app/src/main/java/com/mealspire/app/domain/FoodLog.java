package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dziennik „co dzisiaj jedliście?": krótkie wpisy użytkownika z wieczornego
 * powiadomienia (albo z „Więcej…"). Tekst jest swobodny — nie próbujemy go
 * dopasować do katalogu; trafia do promptów jako to, czego nie powtarzać.
 * Niemutowalny, najnowsze wpisy na końcu, przycinany do {@link #MAX_ENTRIES}.
 */
public final class FoodLog {

    public static final int MAX_ENTRIES = 60;
    /** Dłuższe wpisy są ucinane — to notatka, nie przepis. */
    public static final int MAX_TEXT_LENGTH = 200;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    /** Jeden wpis: co zjedzono i kiedy go zapisano. */
    public static final class Entry {
        private final String text;
        private final long timeMillis;

        public Entry(String text, long timeMillis) {
            this.text = clean(text);
            this.timeMillis = timeMillis;
        }

        public String getText() {
            return text;
        }

        public long getTimeMillis() {
            return timeMillis;
        }
    }

    private final List<Entry> entries;

    public FoodLog(List<Entry> entries) {
        List<Entry> kept = new ArrayList<>();
        if (entries != null) {
            for (Entry entry : entries) {
                if (entry != null && !entry.getText().isEmpty()) {
                    kept.add(entry);
                }
            }
        }
        if (kept.size() > MAX_ENTRIES) {
            kept = kept.subList(kept.size() - MAX_ENTRIES, kept.size());
        }
        this.entries = Collections.unmodifiableList(new ArrayList<>(kept));
    }

    public static FoodLog empty() {
        return new FoodLog(null);
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Dopisuje wpis; pusty tekst niczego nie zmienia. */
    public FoodLog append(String text, long nowMillis) {
        Entry entry = new Entry(text, nowMillis);
        if (entry.getText().isEmpty()) {
            return this;
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(entry);
        return new FoodLog(next);
    }

    /** Teksty z ostatnich {@code days} dni, od najnowszego. */
    public List<String> recentTexts(long nowMillis, int days) {
        List<String> result = new ArrayList<>();
        long since = nowMillis - days * DAY_MS;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.getTimeMillis() >= since && entry.getTimeMillis() <= nowMillis) {
                result.add(entry.getText());
            }
        }
        return result;
    }

    /**
     * Pozycje do listy „ostatnio proponowane — zaproponuj coś innego" w
     * promptach: zjedzone w ostatnich dwóch dniach, z dopiskiem, że to już było.
     */
    public List<String> promptAvoidItems(long nowMillis) {
        List<String> items = new ArrayList<>();
        for (String text : recentTexts(nowMillis, 2)) {
            items.add("niedawno zjedzone: " + text);
        }
        return items;
    }

    private static String clean(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_TEXT_LENGTH
                ? cleaned.substring(0, MAX_TEXT_LENGTH).trim() : cleaned;
    }
}
