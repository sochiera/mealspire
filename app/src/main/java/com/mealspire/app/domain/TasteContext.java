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
    private final String explorationSentence;
    private final String antiMonotonySentence;

    public TasteContext(List<String> profileSentences, List<String> exampleDishes) {
        this(profileSentences, exampleDishes, "", "");
    }

    private TasteContext(List<String> profileSentences, List<String> exampleDishes,
                         String explorationSentence, String antiMonotonySentence) {
        this.profileSentences = copy(profileSentences);
        this.exampleDishes = copy(exampleDishes);
        this.explorationSentence = explorationSentence == null ? "" : explorationSentence;
        this.antiMonotonySentence = antiMonotonySentence == null
                ? "" : antiMonotonySentence;
    }

    public static TasteContext empty() {
        return new TasteContext(null, null);
    }

    /** Kopia z instrukcją eksploracji 2+1 (cel wybiera appka, nie model). */
    public TasteContext withExploration(String sentence) {
        return new TasteContext(profileSentences, exampleDishes, sentence,
                antiMonotonySentence);
    }

    /** Kopia ze zdaniem anty-monotonii („ostatnio dużo X — unikaj"). */
    public TasteContext withAntiMonotony(String sentence) {
        return new TasteContext(profileSentences, exampleDishes, explorationSentence,
                sentence);
    }

    public String getExplorationSentence() {
        return explorationSentence;
    }

    public String getAntiMonotonySentence() {
        return antiMonotonySentence;
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
