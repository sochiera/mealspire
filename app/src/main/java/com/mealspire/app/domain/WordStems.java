package com.mealspire.app.domain;

/**
 * Wspólne, słownikowe dopasowanie tekstu do rdzeni polskich słów: tekst jest
 * tokenizowany po znakach niebędących literami, a token pasuje do rdzenia
 * dokładnie (rdzenie krótsze niż 4 znaki, żeby „ser" nie łapał „serwuj")
 * albo prefiksowo (dłuższe, żeby „śmietan" łapał odmiany). Jedna definicja
 * dla wykluczeń diety i tagowania dań — nie rozjeżdżać.
 */
final class WordStems {

    private WordStems() {
    }

    /** Czy jakikolwiek token tekstu (już lowercase) pasuje do któregoś rdzenia. */
    static boolean matchesAny(String lowerCaseText, String[] stems) {
        for (String token : lowerCaseText.split("[^\\p{L}]+")) {
            if (tokenMatches(token, stems)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tokenMatches(String token, String[] stems) {
        for (String stem : stems) {
            if (stem.length() < 4 ? token.equals(stem) : token.startsWith(stem)) {
                return true;
            }
        }
        return false;
    }
}
