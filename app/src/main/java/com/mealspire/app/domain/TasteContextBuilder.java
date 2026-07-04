package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Buduje {@link TasteContext} z modelu gustu: bierze najmocniejsze wartości
 * wymiarów dla bieżącego slotu posiłku (z fallbackiem na profil ogólny, gdy
 * slot ma za mało obserwacji) i maskuje wszystko, co koliduje z aktualnymi
 * wykluczeniami diety — kto przechodzi na wegetarianizm, nie dostanie
 * „najchętniej: wieprzowina" z własnej historii.
 */
public final class TasteContextBuilder {

    /** Poniżej tylu obserwacji slot jest „cienki" i dostaje profil ogólny. */
    public static final int MIN_SLOT_OBSERVATIONS = 8;

    /** Budżet przykładowych ulubionych dań w prompcie. */
    public static final int MAX_EXAMPLES = 8;

    private static final int MAX_BASES = 3;
    private static final int MAX_CUISINES = 2;
    private static final int MAX_CHARACTERS = 2;

    public TasteContext build(TasteModel model, TasteEventLog log, DietConstraints diet,
                              int mealIndex) {
        if (model == null) {
            return TasteContext.empty();
        }
        // Fallback cienkiego slotu: 5 lajków obiadowych włącza AI też dla
        // śniadań — sekcję buduje wtedy profil ogólny, nie pusta cisza.
        int slot = mealIndex;
        if (mealIndex < 0 || model.totalObservations(mealIndex) < MIN_SLOT_OBSERVATIONS) {
            slot = TasteModel.ALL_MEALS;
        }

        List<String> sentences = new ArrayList<>();
        addSentence(sentences, "Najchętniej wybiera dania z bazą: ",
                masked(model.topValues(TasteDimension.BASE, slot, MAX_BASES), diet));
        addSentence(sentences, "Lubiane style kuchni: ",
                masked(model.topValues(TasteDimension.CUISINE, slot, MAX_CUISINES), diet));
        addSentence(sentences, "Lubiany charakter dań: ",
                masked(model.topValues(TasteDimension.CHARACTER, slot, MAX_CHARACTERS),
                        diet));

        List<String> examples = new ArrayList<>();
        if (log != null) {
            for (String title : log.recentLikedTitles(MAX_EXAMPLES)) {
                if (diet == null || diet.allows(title)) {
                    examples.add(title);
                }
            }
        }
        return new TasteContext(sentences, examples);
    }

    private static void addSentence(List<String> sentences, String prefix,
                                    List<String> values) {
        if (!values.isEmpty()) {
            sentences.add(prefix + PromptText.join(values) + ".");
        }
    }

    /** Wartości kolidujące z dietą wypadają z profilu — bez czyszczenia historii. */
    private static List<String> masked(List<String> values, DietConstraints diet) {
        if (diet == null || diet.isEmpty()) {
            return values;
        }
        List<String> allowed = new ArrayList<>();
        for (String value : values) {
            if (diet.allows(value)) {
                allowed.add(value);
            }
        }
        return allowed;
    }
}
