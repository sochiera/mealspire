package com.mealspire.app.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The bundled, offline recipe pool — three simple dishes per meal (breakfast,
 * lunch, dinner). Extracted from the UI so both the screen's offline flow and the
 * background reminder notifications draw from the same catalogue.
 */
public final class BuiltInRecipes {

    private static final Recipe[][] MEALS = {
            {
                    new Recipe("Owsianka z jabłkiem",
                            "Składniki: płatki owsiane, mleko, jabłko, cynamon, orzechy.\n\n" +
                                    "Gotuj płatki w mleku przez kilka minut. Dodaj starte jabłko, cynamon i orzechy."),
                    new Recipe("Jajecznica ze szczypiorkiem",
                            "Składniki: jajka, masło, szczypiorek, sól, pieprz.\n\n" +
                                    "Rozpuść masło, wlej roztrzepane jajka i mieszaj do ścięcia. Posyp szczypiorkiem."),
                    new Recipe("Tost z awokado",
                            "Składniki: pieczywo, awokado, sok z cytryny, sól, płatki chili.\n\n" +
                                    "Podpiecz pieczywo. Rozgnieć awokado z cytryną i przyprawami, rozsmaruj na toście.")
            },
            {
                    new Recipe("Makaron z pomidorami",
                            "Składniki: makaron, passata, czosnek, oliwa, bazylia.\n\n" +
                                    "Ugotuj makaron. Podsmaż czosnek na oliwie, dodaj passatę i bazylię, połącz z makaronem."),
                    new Recipe("Ryż z warzywami",
                            "Składniki: ryż, marchew, papryka, groszek, sos sojowy.\n\n" +
                                    "Ugotuj ryż. Podsmaż warzywa, dopraw sosem sojowym i wymieszaj z ryżem."),
                    new Recipe("Kurczak z kaszą",
                            "Składniki: pierś z kurczaka, kasza, ogórek, jogurt, koperek.\n\n" +
                                    "Usmaż lub upiecz kurczaka. Podaj z kaszą i szybkim sosem jogurtowo-koperkowym.")
            },
            {
                    new Recipe("Sałatka grecka",
                            "Składniki: pomidor, ogórek, feta, oliwki, oliwa, oregano.\n\n" +
                                    "Pokrój warzywa i fetę. Dodaj oliwki, oliwę oraz oregano, delikatnie wymieszaj."),
                    new Recipe("Kanapki z twarożkiem",
                            "Składniki: pieczywo, twaróg, jogurt, rzodkiewka, szczypiorek.\n\n" +
                                    "Wymieszaj twaróg z jogurtem i warzywami. Nałóż na pieczywo."),
                    new Recipe("Omlet warzywny",
                            "Składniki: jajka, papryka, cebula, szpinak, ser.\n\n" +
                                    "Podsmaż warzywa, zalej jajkami i smaż na małym ogniu. Na koniec dodaj ser.")
            }
    };

    private BuiltInRecipes() {
    }

    /** Number of meals (breakfast, lunch, dinner). */
    public static int mealCount() {
        return MEALS.length;
    }

    /** The bundled recipes for the given meal index (0=breakfast, 1=lunch, 2=dinner). */
    public static Recipe[] forMeal(int mealIndex) {
        return MEALS[mealIndex];
    }

    /**
     * Title → details for every bundled recipe, optionally merged with the user's
     * cookbook, for building the taste profile in one place.
     */
    public static Map<String, String> detailsByTitle(Cookbook cookbook) {
        Map<String, String> detailsByTitle = new LinkedHashMap<>();
        for (Recipe[] meal : MEALS) {
            for (Recipe recipe : meal) {
                detailsByTitle.put(recipe.getTitle(), recipe.getDetails());
            }
        }
        if (cookbook != null) {
            for (CookbookEntry entry : cookbook.getEntries()) {
                detailsByTitle.put(entry.getTitle(), entry.getRecipe());
            }
        }
        return detailsByTitle;
    }
}
