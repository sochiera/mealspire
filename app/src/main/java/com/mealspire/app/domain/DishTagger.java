package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Słownikowa klasyfikacja dania w wymiarach gustu — pracuje na nazwie i (gdy
 * są) składnikach, świadomie bez wołania AI: offline, deterministycznie,
 * testowalnie. W obrębie wymiaru wygrywa pierwsza pasująca wartość, więc
 * kolejność słowników niesie priorytet (np. schabowy smażony na maśle to
 * wieprzowina, nie nabiał).
 *
 * <p>Słowniki nie zawsze trafią — nierozpoznany wymiar zostaje {@code null}
 * (brak obserwacji). Odsetek nieotagowanych dań jest wskaźnikiem starzenia
 * się słowników; to jedyny element systemu wymagający ręcznej pielęgnacji.
 */
public final class DishTagger {

    private static final Map<String, String[]> BASES = new LinkedHashMap<>();
    private static final Map<String, String[]> CUISINES = new LinkedHashMap<>();
    private static final Map<String, String[]> CHARACTERS = new LinkedHashMap<>();

    static {
        // Kolejność = priorytet: konkretne mięsa przed jajkami/nabiałem,
        // warzywa jako ostatnia deska ratunku.
        BASES.put("ryba", new String[]{"ryba", "ryby", "rybę", "rybą", "rybn", "rybi",
                "tuńczyk", "tunczyk", "łosos", "losos", "łosoś", "makrel", "śledz",
                "sledz", "dorsz", "krewetk", "pstrąg", "sardynk", "mintaj", "halibut"});
        BASES.put("wieprzowina", new String[]{"wieprzow", "schab", "szynk", "boczek",
                "kiełbas", "kielbas", "parówk", "parowk", "karkówk", "karkowk",
                "żeberk", "zeberk", "salami", "smalec"});
        BASES.put("wołowina", new String[]{"wołowin", "wolowin", "rostbef", "antrykot"});
        BASES.put("kurczak", new String[]{"kurczak", "kurczę", "kurcząt", "indyk",
                "drobiow", "drób"});
        BASES.put("mięso mielone", new String[]{"mielon", "klopsik", "pulpet"});
        BASES.put("jajka", new String[]{"jajk", "jajec", "jaja", "jajo", "omlet"});
        BASES.put("strączki", new String[]{"fasol", "soczewic", "ciecierzyc",
                "cieciork", "groch", "hummus", "tofu"});
        BASES.put("nabiał", new String[]{"twaróg", "twarog", "twaroż", "twaroz",
                "serek", "serk", "ser", "sera", "serem", "serow", "jogurt",
                "mozzarell", "feta", "fetą", "fety", "halloumi"});
        BASES.put("warzywa", new String[]{"warzyw", "pomidor", "papryk", "cukini",
                "szpinak", "pieczark", "marchew", "marchw", "ziemniak", "kapust",
                "ogórk", "ogork", "sałat", "salat", "brokuł", "brokul", "kalafior",
                "rzodkiew", "awokado", "dyni", "dynia", "burak"});

        CUISINES.put("azjatycka", new String[]{"curry", "sojow", "kokosow", "imbir",
                "sezam", "teriyaki", "sushi", "ramen", "woka", "tajsk", "chińsk",
                "chinsk"});
        CUISINES.put("włoska", new String[]{"spaghetti", "lasagn", "risotto", "pesto",
                "mozzarell", "pizza", "gnocchi", "carbonara", "bolognese", "włosk",
                "wlosk"});
        CUISINES.put("śródziemnomorska", new String[]{"oliwk", "feta", "fetą", "fety",
                "hummus", "greck", "bakłażan", "baklazan", "tzatziki", "falafel",
                "śródziemnomorsk", "srodziemnomorsk"});
        CUISINES.put("meksykańska", new String[]{"tortill", "taco", "burrito", "salsa",
                "nachos", "quesadill", "meksykańsk", "meksykansk"});
        CUISINES.put("polska", new String[]{"pierog", "schabow", "bigos", "rosół",
                "rosol", "żurek", "zurek", "kopytk", "gołąbk", "golabk", "kiszon",
                "oscypk", "polsk", "mielon", "bretońsku", "bretonsku"});

        CHARACTERS.put("zupa", new String[]{"zupa", "zupy", "zupk", "krem", "rosół",
                "rosol", "żurek", "zurek", "barszcz", "chłodnik", "chlodnik"});
        CHARACTERS.put("zapiekanka", new String[]{"zapiek"});
        CHARACTERS.put("sałatka", new String[]{"sałatk", "salatk"});
        CHARACTERS.put("kanapki", new String[]{"kanapk", "tost", "grzank", "pieczywo",
                "pieczywem"});
        CHARACTERS.put("z patelni", new String[]{"naleśnik", "nalesnik", "placki",
                "placuszk", "omlet", "jajecznic", "pancake", "racuch"});
        CHARACTERS.put("na słodko", new String[]{"owsiank", "kaszk", "musli",
                "granola", "budyń", "budyn"});
    }

    /** Taguje danie po nazwie i (opcjonalnie) tekście składników/przepisu. */
    public DishTags tag(String title, String details) {
        StringBuilder text = new StringBuilder();
        if (title != null) {
            text.append(title);
        }
        if (details != null) {
            text.append('\n').append(details);
        }
        String lower = text.toString().toLowerCase(Locale.ROOT);
        return new DishTags(firstMatch(lower, BASES), firstMatch(lower, CUISINES),
                firstMatch(lower, CHARACTERS));
    }

    /** Taguje propozycję AI: nazwa + opis + kluczowe składniki. */
    public DishTags tag(DishProposal proposal) {
        if (proposal == null) {
            return DishTags.none();
        }
        StringBuilder details = new StringBuilder(proposal.getDescription());
        for (String ingredient : proposal.getKeyIngredients()) {
            details.append('\n').append(ingredient);
        }
        return tag(proposal.getName(), details.toString());
    }

    /** Znane wartości wymiaru, w kolejności priorytetu słowników. */
    public List<String> knownValues(TasteDimension dimension) {
        return Collections.unmodifiableList(
                new ArrayList<>(dictionary(dimension).keySet()));
    }

    private static Map<String, String[]> dictionary(TasteDimension dimension) {
        switch (dimension) {
            case BASE:
                return BASES;
            case CUISINE:
                return CUISINES;
            case CHARACTER:
                return CHARACTERS;
            default:
                return Collections.emptyMap();
        }
    }

    private static String firstMatch(String lowerText, Map<String, String[]> dictionary) {
        for (Map.Entry<String, String[]> entry : dictionary.entrySet()) {
            if (WordStems.matchesAny(lowerText, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
