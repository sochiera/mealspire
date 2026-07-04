package com.mealspire.app.domain;

/**
 * Czysta decyzja „co się dzieje po systemowym Cofnij" na podstawie aktualnego
 * ekranu. {@code MainActivity} tylko śledzi ekran i wykonuje wskazane przejście
 * (pamiętając o podbiciu epoki treści, żeby spóźnione odpowiedzi nie nadpisały
 * widoku, do którego użytkownik wrócił).
 */
public final class BackNavigation {

    /** Ekrany, między którymi porusza się użytkownik. */
    public enum Screen {
        START,
        PROPOSALS,
        RECIPE,
        SHOPPING_LIST,
        ONBOARDING
    }

    /** Co Activity ma zrobić po wciśnięciu Cofnij. */
    public enum Action {
        EXIT,
        SHOW_START,
        SHOW_PROPOSALS,
        SHOW_RECIPE,
        ONBOARDING_PREVIOUS,
        ONBOARDING_SKIP
    }

    private BackNavigation() {
    }

    /**
     * @param onboardingStep indeks aktualnego pytania quizu (0 = pierwsze);
     *                       ignorowany poza ekranem {@link Screen#ONBOARDING}.
     */
    public static Action onBack(Screen screen, int onboardingStep) {
        switch (screen) {
            case RECIPE:
                return Action.SHOW_PROPOSALS;
            case SHOPPING_LIST:
                return Action.SHOW_RECIPE;
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
