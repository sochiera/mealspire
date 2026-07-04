package com.mealspire.app.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Twarde wykluczenia diety — osobna kategoria niż gust: to wymóg, nie
 * preferencja. Wykluczenia filtrują pulę offline, walidują odpowiedzi AI
 * i trafiają do promptów jako bezwzględne zdanie, a nie luźna wskazówka.
 *
 * <p>Wykrywanie naruszeń jest słownikowe (tokeny tekstu vs rdzenie słów) i
 * celowo ostrożne: lepiej odrzucić danie niepotrzebnie (fallback poda inne),
 * niż zaproponować wieprzowinę wegetarianinowi. Słowniki są siatką
 * bezpieczeństwa — pierwszą linią obrony pozostaje wymóg w prompcie.
 */
public final class DietConstraints {

    /** Jedno możliwe wykluczenie; etykiety wspólne dla quizu i dialogu profilu. */
    public enum Exclusion {
        VEGETARIAN("Wegetariańsko (bez mięsa i ryb)", "mięsa ani ryb",
                new String[]{"mięs", "kurczak", "kurczę", "indyk", "wołowin",
                        "cielęcin", "wieprzow", "schab", "szynk", "boczek",
                        "kiełbas", "parówk", "karkówk", "żeberk", "salami",
                        "smalec", "mielon", "drobiow", "wędlin", "kaczk", "gęsi",
                        "ryba", "ryby", "rybę", "rybą", "rybn", "rybi", "tuńczyk",
                        "tunczyk", "łosos", "losos", "łosoś", "makrel", "śledz",
                        "sledz", "dorsz", "krewetk", "pstrąg", "sardynk",
                        "mintaj", "halibut"}),
        NO_PORK("Bez wieprzowiny", "wieprzowiny",
                new String[]{"wieprzow", "schab", "szynk", "boczek", "kiełbas",
                        "parówk", "karkówk", "żeberk", "salami", "smalec"}),
        NO_GLUTEN("Bez glutenu", "glutenu (mąki, pieczywa, makaronu, kaszy manny)",
                new String[]{"mąk", "maka", "makaron", "pieczyw", "chleb", "bułk",
                        "tost", "naleśnik", "nalesnik", "pierog", "manna", "manną",
                        "musli", "kuskus", "spaghetti", "lasagn", "tortill",
                        "gluten", "pszen", "jęczmien", "żytni"}),
        NO_LACTOSE("Bez laktozy", "laktozy (mleka, śmietany, sera, masła)",
                new String[]{"mlek", "śmietan", "smietan", "jogurt", "twaróg",
                        "twarog", "twaroż", "twaroz", "maślan", "maslan", "masł",
                        "masl", "nabiał", "nabial", "mozzarell", "parmezan",
                        "laktoz", "kefir", "ser", "sera", "serem", "serek",
                        "serk", "serow", "feta", "fetą", "fety"}),
        NO_NUTS("Bez orzechów", "orzechów",
                new String[]{"orzech", "orzeszk", "migdał", "migdal", "nerkowc",
                        "pistacj", "arachid"}),
        NO_FISH("Bez ryb i owoców morza", "ryb i owoców morza",
                new String[]{"ryba", "ryby", "rybę", "rybą", "rybn", "rybi",
                        "tuńczyk", "tunczyk", "łosos", "losos", "łosoś", "makrel",
                        "śledz", "sledz", "dorsz", "krewetk", "pstrąg", "sardynk",
                        "mintaj", "halibut", "małż", "kalmar", "ośmiornic"});

        private final String label;
        private final String promptPhrase;
        private final String[] stems;

        Exclusion(String label, String promptPhrase, String[] stems) {
            this.label = label;
            this.promptPhrase = promptPhrase;
            this.stems = stems;
        }

        /** Etykieta pokazywana w quizie i w dialogu „Profil domowników". */
        public String label() {
            return label;
        }

        /** Fraza w dopełniaczu do zdania „nie proponuj dań zawierających …". */
        public String promptPhrase() {
            return promptPhrase;
        }

        /** Czy tekst (nazwa/składniki/przepis) narusza to wykluczenie. */
        boolean isViolatedBy(String lowerCaseText) {
            for (String token : lowerCaseText.split("[^\\p{L}]+")) {
                for (String stem : stems) {
                    // Krótkie rdzenie tylko dokładnie (żeby „ser" nie łapał
                    // „serwuj"); dłuższe jako prefiks odmian („śmietaną").
                    if (stem.length() < 4 ? token.equals(stem) : token.startsWith(stem)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private final Set<Exclusion> exclusions;

    private DietConstraints(Set<Exclusion> exclusions) {
        this.exclusions = Collections.unmodifiableSet(exclusions);
    }

    public static DietConstraints empty() {
        return new DietConstraints(EnumSet.noneOf(Exclusion.class));
    }

    public static DietConstraints of(Collection<Exclusion> exclusions) {
        EnumSet<Exclusion> set = EnumSet.noneOf(Exclusion.class);
        if (exclusions != null) {
            for (Exclusion exclusion : exclusions) {
                if (exclusion != null) {
                    set.add(exclusion);
                }
            }
        }
        return new DietConstraints(set);
    }

    public Set<Exclusion> getExclusions() {
        return exclusions;
    }

    public boolean isEmpty() {
        return exclusions.isEmpty();
    }

    /** Czy danie (dowolny jego tekst: nazwa, składniki, przepis) jest dozwolone. */
    public boolean allows(String dishText) {
        if (dishText == null || exclusions.isEmpty()) {
            return true;
        }
        String lower = dishText.toLowerCase(Locale.ROOT);
        for (Exclusion exclusion : exclusions) {
            if (exclusion.isViolatedBy(lower)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Zdanie-wymóg do promptów AI. W odróżnieniu od zdań gustu jest sformułowane
     * bezwzględnie — „nigdy", nie „raczej".
     */
    public String promptSentence() {
        if (exclusions.isEmpty()) {
            return "";
        }
        StringBuilder phrases = new StringBuilder();
        for (Exclusion exclusion : exclusions) {
            if (phrases.length() > 0) {
                phrases.append(", ");
            }
            phrases.append(exclusion.promptPhrase());
        }
        return "Bezwzględny wymóg diety: nigdy nie proponuj dań zawierających "
                + phrases + " — bez wyjątków.";
    }
}
