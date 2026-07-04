package com.mealspire.app.domain;

/**
 * Jedno zdarzenie gustu — surowa prawda o tym, co użytkownik zrobił: polubił
 * danie, obejrzał przepis, dodał własne danie, wybrał coś w quizie. Zdarzenia
 * są niemutowalne i lądują w dzienniku ({@link TasteEventLog}); wnioski
 * (model gustu) są zawsze pochodną dziennika, nigdy odwrotnie.
 */
public final class TasteEvent {

    /**
     * Typ zdarzenia wraz z wagą w modelu gustu. Sygnały jawne ważą najwięcej;
     * dorozumiane (reroll, widziane-niewybrane) tylko delikatnie korygują
     * i nigdy nie stają się twardym zakazem.
     */
    public enum Type {
        /** „Lubię to" — najmocniejsza deklaracja gustu. */
        LIKED(3.0, true),
        /** Import własnego dania („Dodaj danie") — równie mocna deklaracja. */
        IMPORTED(3.0, true),
        /** Wybór dania w rundzie quizu startowego — prior, wygasa z czasem. */
        ONBOARDING_PICK(2.0, true),
        /** „Pokaż przepis" — zainteresowanie, sygnał dorozumiany. */
        RECIPE_VIEWED(1.0, false),
        /** „Inne propozycje" — słaby negatyw dla całej pokazanej trójki. */
        REROLLED(-0.3, false),
        /** Widziane, ale przegrało z innym wyborem w tej samej trójce. */
        SHOWN_NOT_CHOSEN(-0.1, false);

        private final double weight;
        private final boolean explicitSignal;

        Type(double weight, boolean explicitSignal) {
            this.weight = weight;
            this.explicitSignal = explicitSignal;
        }

        public double weight() {
            return weight;
        }

        /** Jawna deklaracja użytkownika (vs cicho obserwowane zachowanie). */
        public boolean isExplicit() {
            return explicitSignal;
        }
    }

    /** Slot posiłku nieznany (np. import dania spoza przepływu propozycji). */
    public static final int NO_MEAL = -1;

    private final Type type;
    private final String dishTitle;
    private final int mealIndex;
    private final long timestamp;
    private final boolean exploratory;

    public TasteEvent(Type type, String dishTitle, int mealIndex, long timestamp) {
        this(type, dishTitle, mealIndex, timestamp, false);
    }

    /**
     * @param exploratory czy dotyczy propozycji eksploracyjnej — takie zdarzenia
     *                    nie mogą nieść sygnału ujemnego (nie karzemy własnych
     *                    eksperymentów), liczy się wyłącznie ich sukces
     */
    public TasteEvent(Type type, String dishTitle, int mealIndex, long timestamp,
                      boolean exploratory) {
        this.type = type;
        this.dishTitle = dishTitle == null ? "" : dishTitle.trim();
        this.mealIndex = mealIndex;
        this.timestamp = timestamp;
        this.exploratory = exploratory;
    }

    public Type getType() {
        return type;
    }

    public String getDishTitle() {
        return dishTitle;
    }

    /** 0=śniadanie, 1=obiad, 2=kolacja, {@link #NO_MEAL} = nieznany. */
    public int getMealIndex() {
        return mealIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExploratory() {
        return exploratory;
    }

    /**
     * Waga zdarzenia w modelu. Zdarzenie eksploracyjne o ujemnej wadze liczy
     * się jako zero — system nie kara wymiarów, które sam postanowił zbadać.
     */
    public double effectiveWeight() {
        if (exploratory && type.weight() < 0) {
            return 0.0;
        }
        return type.weight();
    }

    public boolean isValid() {
        return type != null && !dishTitle.isEmpty();
    }
}
