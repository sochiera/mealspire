package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skompresowany, budżetowany obraz gustu wstrzykiwany do promptów zamiast
 * surowej listy wszystkich polubień: kilka zdań profilu per wymiar plus
 * garść przykładowych ulubionych dań (ważonych świeżością). Przy 50+
 * polubieniach pełna lista szumi i rozmywa sygnał — kompresja go wyostrza.
 */
public final class TasteContext {

    private final List<String> profileSentences;
    private final List<String> exampleDishes;

    public TasteContext(List<String> profileSentences, List<String> exampleDishes) {
        this.profileSentences = copy(profileSentences);
        this.exampleDishes = copy(exampleDishes);
    }

    public static TasteContext empty() {
        return new TasteContext(null, null);
    }

    /** Zdania profilu („Najchętniej wybiera dania z bazą: …"). */
    public List<String> getProfileSentences() {
        return profileSentences;
    }

    /** Przykładowe ulubione dania, max kilka, najświeższe pierwsze. */
    public List<String> getExampleDishes() {
        return exampleDishes;
    }

    public boolean isEmpty() {
        return profileSentences.isEmpty() && exampleDishes.isEmpty();
    }

    private static List<String> copy(List<String> values) {
        return values == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
