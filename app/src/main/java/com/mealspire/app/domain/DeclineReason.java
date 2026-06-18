package com.mealspire.app.domain;

/**
 * Why the user skipped a proposed dish. The reason shapes what happens next:
 * a genuine dislike is remembered permanently (so it is avoided in the future),
 * while the "not today" reasons only steer the very next proposal via a prompt
 * hint without polluting the long-term preferences.
 */
public enum DeclineReason {

    DISLIKE("Nie lubię tego wcale", true,
            "Użytkownik w ogóle nie lubi tego dania — zaproponuj coś zupełnie innego."),
    TOO_HARD("Za trudne", false,
            "Poprzednia propozycja była za trudna — zaproponuj coś prostszego i łatwego do zrobienia."),
    NO_TIME("Dzisiaj nie mam czasu", false,
            "Użytkownik nie ma dziś czasu — zaproponuj coś szybkiego, do około 20 minut."),
    NO_DESIRE("Nie mam na to ochoty dzisiaj", false,
            "Użytkownik nie ma dziś ochoty na to danie — zaproponuj coś innego dla odmiany.");

    private final String label;
    private final boolean permanentDislike;
    private final String promptHint;

    DeclineReason(String label, boolean permanentDislike, String promptHint) {
        this.label = label;
        this.permanentDislike = permanentDislike;
        this.promptHint = promptHint;
    }

    /** Short, user-facing label shown in the "why not?" picker. */
    public String getLabel() {
        return label;
    }

    /** Whether this reason should be remembered as a lasting dislike. */
    public boolean isPermanentDislike() {
        return permanentDislike;
    }

    /** A hint fed into the next proposal so the new idea fits "not today" reasons. */
    public String getPromptHint() {
        return promptHint;
    }

    /** All labels, in declaration order, for building a selection dialog. */
    public static String[] labels() {
        DeclineReason[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].label;
        }
        return result;
    }
}
