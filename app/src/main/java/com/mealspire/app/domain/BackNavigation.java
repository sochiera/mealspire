package com.mealspire.app.domain;

/**
 * Czysta decyzja „co się dzieje po systemowym Cofnij" na podstawie aktualnego
 * ekranu. {@code MainActivity} tylko śledzi ekran i wykonuje wskazane przejście
 * (pamiętając o podbiciu epoki treści, żeby spóźnione odpowiedzi nie nadpisały
 * widoku, do którego użytkownik wrócił). Lista zakupów jest dialogiem —
 * systemowe „Cofnij" zamyka ją samo, więc nie jest osobnym ekranem.
 */
public final class BackNavigation {

    /** Ekrany, między którymi porusza się użytkownik. */
    public enum Screen {
        START,
        PROPOSALS,
        RECIPE,
        ONBOARDING
    }

    /** Co Activity ma zrobić po wciśnięciu Cofnij. */
    public enum Action {
        EXIT,
        SHOW_START,
        SHOW_PROPOSALS,
        ONBOARDING_PREVIOUS,
        ONBOARDING_SKIP
    }

    private BackNavigation() {
    }

    /**
     * @param onboardingStep      indeks aktualnego pytania quizu (0 = pierwsze);
     *                            ignorowany poza ekranem {@link Screen#ONBOARDING}
     * @param recipeFromProposals czy pokazany przepis pochodzi z listy propozycji;
     *                            przepis spoza niej (np. import z „Dodaj danie")
     *                            wraca na ekran startowy, bo nie ma propozycji,
     *                            do których można by wrócić
     */
    public static Action onBack(Screen screen, int onboardingStep,
                                boolean recipeFromProposals) {
        switch (screen) {
            case RECIPE:
                return recipeFromProposals ? Action.SHOW_PROPOSALS : Action.SHOW_START;
            case PROPOSALS:
                return Action.SHOW_START;
            case ONBOARDING:
                return onboardingStep > 0 ? Action.ONBOARDING_PREVIOUS
                        : Action.ONBOARDING_SKIP;
            case START:
            default:
                return Action.EXIT;
        }
    }
}
