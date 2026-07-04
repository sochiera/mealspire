package com.mealspire.app.domain;

/**
 * Wspólne sklejanie list w tekst promptu — jedna definicja separatora dla
 * wszystkich builderów, żeby listy (polubienia, składniki, kuchnie…)
 * wyglądały w promptach tak samo.
 */
final class PromptText {

    private PromptText() {
    }

    static String join(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
