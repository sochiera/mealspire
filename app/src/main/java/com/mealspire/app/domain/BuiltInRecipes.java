package com.mealspire.app.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The bundled, offline recipe pool — a large catalogue of simple, everyday dishes
 * per meal (breakfast, lunch, dinner), shipped with the app so it has plenty to
 * suggest from the very first launch, without needing the network or any learned
 * data. Extracted from the UI so both the screen's offline flow and the
 * background reminder notifications draw from the same catalogue.
 *
 * <p>Dishes are kept deliberately simple and ordinary — the kind of thing someone
 * actually cooks on a Tuesday. Where a dish naturally has toppings or extras
 * (owsianka, płatki, tosty…), those are called out as an optional "Dodatki"
 * line rather than turned into a separate, more complex recipe.
 */
public final class BuiltInRecipes {

    private static final Recipe[][] MEALS = {
            {
                    new Recipe("Jajecznica",
                            "Składniki: jajka, masło, szczypiorek, sól, pieprz.\n\n"
                                    + "Rozpuść masło, wlej roztrzepane jajka i mieszaj do ścięcia. "
                                    + "Posyp szczypiorkiem."),
                    new Recipe("Jajka sadzone",
                            "Składniki: jajka, masło, sól, pieprz, pieczywo.\n\n"
                                    + "Rozgrzej masło na patelni, wbij jajka i smaż na małym ogniu, aż "
                                    + "białko się zetnie, a żółtko zostanie płynne. Podawaj z pieczywem."),
                    new Recipe("Jajka na twardo z pieczywem",
                            "Składniki: jajka, pieczywo, masło, szczypiorek, sól.\n\n"
                                    + "Gotuj jajka we wrzątku około 8-9 minut, ostudź i obierz. Podawaj "
                                    + "pokrojone na pieczywie z masłem i szczypiorkiem."),
                    new Recipe("Omlet",
                            "Składniki: jajka, mleko, masło, sól, pieprz.\n\n"
                                    + "Roztrzep jajka z mlekiem i przyprawami, wylej na rozgrzane masło i "
                                    + "smaż na małym ogniu, aż się zetnie. Złóż na pół."),
                    new Recipe("Owsianka z dodatkami",
                            "Składniki: płatki owsiane, mleko lub woda, szczypta soli.\n\n"
                                    + "Gotuj płatki w mleku lub wodzie kilka minut, aż zgęstnieją.\n"
                                    + "Dodatki (opcjonalnie, do wyboru): jabłko, banan, cynamon, miód, "
                                    + "orzechy, rodzynki — dorzuć to, co akurat masz pod ręką."),
                    new Recipe("Kaszka manna na mleku",
                            "Składniki: kasza manna, mleko, cukier lub miód.\n\n"
                                    + "Zagotuj mleko, wsyp kaszę manną cienkim strumieniem, mieszając, i "
                                    + "gotuj 2-3 minuty. Dodatki (opcjonalnie): cynamon, owoce, dżem."),
                    new Recipe("Płatki z mlekiem",
                            "Składniki: płatki śniadaniowe, mleko.\n\n"
                                    + "Zalej płatki zimnym mlekiem. Dodatki (opcjonalnie): owoce, miód, "
                                    + "orzechy."),
                    new Recipe("Musli z jogurtem i owocami",
                            "Składniki: musli, jogurt naturalny, owoce sezonowe.\n\n"
                                    + "Wymieszaj jogurt z musli, na wierzch dodaj pokrojone owoce. "
                                    + "Dodatki (opcjonalnie): miód, orzechy, nasiona."),
                    new Recipe("Tosty",
                            "Składniki: pieczywo tostowe, masło, szynka, ser żółty.\n\n"
                                    + "Nałóż szynkę i ser między kromki posmarowane masłem, opiecz na "
                                    + "patelni lub w tosterze z obu stron do zrumienienia."),
                    new Recipe("Tost z awokado",
                            "Składniki: pieczywo, awokado, sok z cytryny, sól, płatki chili.\n\n"
                                    + "Podpiecz pieczywo. Rozgnieć awokado z cytryną i przyprawami, "
                                    + "rozsmaruj na toście."),
                    new Recipe("Grzanki z serem",
                            "Składniki: pieczywo, ser żółty, masło, opcjonalnie czosnek.\n\n"
                                    + "Posmaruj pieczywo masłem, przykryj plastrem sera i zapiecz w "
                                    + "piekarniku lub na patelni pod przykryciem, aż ser się rozpuści."),
                    new Recipe("Pancakes",
                            "Składniki: mąka, mleko, jajko, cukier, proszek do pieczenia, masło.\n\n"
                                    + "Wymieszaj składniki na gładkie ciasto i smaż małe placuszki na "
                                    + "maśle z obu stron. Dodatki (opcjonalnie): syrop klonowy, owoce, "
                                    + "masło orzechowe."),
                    new Recipe("Naleśniki",
                            "Składniki: mąka, mleko, jajka, szczypta soli, olej do smażenia.\n\n"
                                    + "Zmiksuj składniki na ciasto i smaż cienkie placki na rozgrzanej "
                                    + "patelni z obu stron. Dodatki (opcjonalnie): dżem, twaróg na słodko, "
                                    + "ser i szynka na słono."),
                    new Recipe("Placki z twarożku",
                            "Składniki: twaróg, jajko, mąka, cukier, szczypta soli, olej do smażenia.\n\n"
                                    + "Wymieszaj twaróg z jajkiem, mąką i cukrem na gładką masę, formuj "
                                    + "placki i smaż na oleju z obu stron do zrumienienia."),
                    new Recipe("Kanapki klasyczne z wędliną i serem",
                            "Składniki: pieczywo, masło, wędlina, ser żółty, warzywa (pomidor, "
                                    + "ogórek).\n\n"
                                    + "Posmaruj pieczywo masłem, ułóż wędlinę, ser i warzywa."),
                    new Recipe("Kanapki z pastą jajeczną",
                            "Składniki: jajka na twardo, majonez, szczypiorek, sól, pieprz, pieczywo.\n\n"
                                    + "Rozgnieć ugotowane jajka z majonezem i szczypiorkiem, dopraw i "
                                    + "nałóż na pieczywo."),
                    new Recipe("Serek wiejski z rzodkiewką i szczypiorkiem",
                            "Składniki: serek wiejski, rzodkiewka, szczypiorek, pieczywo.\n\n"
                                    + "Wymieszaj serek z pokrojoną rzodkiewką i szczypiorkiem, podawaj "
                                    + "z pieczywem."),
                    new Recipe("Parówki z pieczywem",
                            "Składniki: parówki, pieczywo, musztarda lub ketchup.\n\n"
                                    + "Ugotuj lub podgrzej parówki i podaj z pieczywem oraz ulubionym "
                                    + "sosem."),
                    new Recipe("Ryż na mleko",
                            "Składniki: ryż, mleko, cukier, wanilia lub cynamon.\n\n"
                                    + "Gotuj ryż w mleku na małym ogniu, aż zmięknie i zgęstnieje. "
                                    + "Dodatki (opcjonalnie): cynamon, owoce, dżem."),
                    new Recipe("Kanapki z pastą z awokado i jajkiem",
                            "Składniki: awokado, jajko na twardo, pieczywo, sok z cytryny, sól.\n\n"
                                    + "Rozgnieć awokado z cytryną i solą, dodaj pokrojone jajko, "
                                    + "rozsmaruj na pieczywie.")
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
                                    "Usmaż lub upiecz kurczaka. Podaj z kaszą i szybkim sosem jogurtowo-koperkowym."),
                    new Recipe("Zupa pomidorowa z makaronem",
                            "Składniki: bulion, passata pomidorowa, makaron, śmietana, natka "
                                    + "pietruszki.\n\n"
                                    + "Zagotuj bulion z passatą, dodaj makaron i gotuj do miękkości. "
                                    + "Zabiel śmietaną, posyp natką."),
                    new Recipe("Rosół z makaronem",
                            "Składniki: kurczak, marchew, pietruszka, seler, por, makaron.\n\n"
                                    + "Gotuj mięso z warzywami na wolnym ogniu około godziny, wyjmij "
                                    + "warzywa i mięso, podawaj bulion z ugotowanym makaronem."),
                    new Recipe("Kotlet mielony z ziemniakami i surówką",
                            "Składniki: mięso mielone, bułka, jajko, cebula, ziemniaki, kapusta "
                                    + "kiszona.\n\n"
                                    + "Wymieszaj mięso z namoczoną bułką, jajkiem i cebulą, uformuj "
                                    + "kotlety i usmaż. Podaj z ziemniakami i surówką."),
                    new Recipe("Kotlet schabowy z ziemniakami i kapustą",
                            "Składniki: schab, jajko, bułka tarta, mąka, ziemniaki, kapusta zasmażana.\n\n"
                                    + "Rozbij schab, obtocz w mące, jajku i bułce tartej, usmaż na "
                                    + "złoty kolor. Podaj z ziemniakami i kapustą."),
                    new Recipe("Gulasz wieprzowy z kaszą",
                            "Składniki: mięso wieprzowe, cebula, papryka, koncentrat pomidorowy, "
                                    + "kasza.\n\n"
                                    + "Podsmaż mięso z cebulą, dodaj paprykę i koncentrat, duś pod "
                                    + "przykryciem do miękkości. Podaj z kaszą."),
                    new Recipe("Pierogi z serem i ziemniakami",
                            "Składniki: ciasto pierogowe, ziemniaki, twaróg, cebula, masło.\n\n"
                                    + "Ugniecione ziemniaki wymieszaj z twarogiem, zawiń w ciasto, "
                                    + "ugotuj we wrzątku. Podaj polane cebulką podsmażoną na maśle."),
                    new Recipe("Klopsiki w sosie pomidorowym z ryżem",
                            "Składniki: mięso mielone, cebula, bułka, jajko, passata pomidorowa, ryż.\n\n"
                                    + "Uformuj małe klopsiki, obsmaż i duś w sosie pomidorowym. Podaj z "
                                    + "ugotowanym ryżem."),
                    new Recipe("Leczo z kiełbasą",
                            "Składniki: papryka, cukinia, cebula, pomidory, kiełbasa.\n\n"
                                    + "Podsmaż kiełbasę z cebulą, dodaj paprykę i cukinię, duś z "
                                    + "pomidorami do miękkości warzyw."),
                    new Recipe("Makaron z serem i szynką",
                            "Składniki: makaron, ser żółty, szynka, śmietana, masło.\n\n"
                                    + "Ugotuj makaron, wymieszaj na gorąco z masłem, startym serem i "
                                    + "pokrojoną szynką, dopraw śmietaną."),
                    new Recipe("Ryż z kurczakiem curry",
                            "Składniki: pierś z kurczaka, ryż, curry w proszku, mleko kokosowe lub "
                                    + "śmietana, cebula.\n\n"
                                    + "Podsmaż kurczaka z cebulą, dopraw curry, dolej mleko kokosowe i "
                                    + "duś chwilę. Podaj z ryżem."),
                    new Recipe("Zupa ogórkowa z ziemniakami",
                            "Składniki: ogórki kiszone, ziemniaki, marchew, śmietana, koperek.\n\n"
                                    + "Ugotuj warzywa w bulionie, dodaj starte ogórki kiszone, zabiel "
                                    + "śmietaną i posyp koperkiem."),
                    new Recipe("Zapiekanka ziemniaczana z serem",
                            "Składniki: ziemniaki, ser żółty, śmietana, cebula, masło.\n\n"
                                    + "Ułóż plastry ziemniaków warstwami z cebulą i śmietaną, posyp "
                                    + "serem, zapiecz w piekarniku do miękkości."),
                    new Recipe("Kasza gryczana z boczkiem i cebulką",
                            "Składniki: kasza gryczana, boczek, cebula, sól, pieprz.\n\n"
                                    + "Ugotuj kaszę. Podsmaż pokrojony boczek z cebulą i wymieszaj z "
                                    + "kaszą."),
                    new Recipe("Fasolka po bretońsku",
                            "Składniki: fasola w puszce, kiełbasa lub boczek, koncentrat pomidorowy, "
                                    + "cebula, przyprawy.\n\n"
                                    + "Podsmaż kiełbasę z cebulą, dodaj fasolę i koncentrat, duś kilka "
                                    + "minut, dopraw do smaku."),
                    new Recipe("Placki ziemniaczane",
                            "Składniki: ziemniaki, jajko, mąka, cebula, sól, olej do smażenia.\n\n"
                                    + "Zetrzyj ziemniaki i cebulę, wymieszaj z jajkiem, mąką i solą, "
                                    + "smaż placki na rozgrzanym oleju z obu stron."),
                    new Recipe("Makaron z tuńczykiem",
                            "Składniki: makaron, tuńczyk w sosie własnym, śmietana, cebula, groszek "
                                    + "konserwowy.\n\n"
                                    + "Ugotuj makaron. Podsmaż cebulę, dodaj tuńczyka, groszek i "
                                    + "śmietanę, wymieszaj z makaronem."),
                    new Recipe("Kurczak w sosie musztardowym z ryżem",
                            "Składniki: pierś z kurczaka, musztarda, śmietana, cebula, ryż.\n\n"
                                    + "Podsmaż kurczaka z cebulą, dodaj musztardę i śmietanę, duś kilka "
                                    + "minut. Podaj z ryżem.")
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
                                    "Podsmaż warzywa, zalej jajkami i smaż na małym ogniu. Na koniec dodaj ser."),
                    new Recipe("Kanapki z pastą z tuńczyka",
                            "Składniki: tuńczyk w sosie własnym, jogurt lub majonez, cebula, pieczywo.\n\n"
                                    + "Rozgnieć tuńczyka z jogurtem i drobno posiekaną cebulą, nałóż na "
                                    + "pieczywo."),
                    new Recipe("Sałatka jarzynowa z jajkiem",
                            "Składniki: warzywa gotowane (marchew, ziemniak, groszek), jajko, majonez.\n\n"
                                    + "Pokrój ugotowane warzywa i jajko w kostkę, wymieszaj z majonezem, "
                                    + "dopraw do smaku."),
                    new Recipe("Twarożek ze szczypiorkiem i rzodkiewką na kanapki",
                            "Składniki: twaróg, jogurt, rzodkiewka, szczypiorek, pieczywo.\n\n"
                                    + "Rozgnieć twaróg z jogurtem, dodaj pokrojoną rzodkiewkę i "
                                    + "szczypiorek, podawaj z pieczywem."),
                    new Recipe("Jajecznica z pomidorem",
                            "Składniki: jajka, pomidor, masło, szczypiorek, sól, pieprz.\n\n"
                                    + "Podsmaż pokrojonego pomidora na maśle, wlej roztrzepane jajka i "
                                    + "mieszaj do ścięcia. Posyp szczypiorkiem."),
                    new Recipe("Kanapki z pastą jajeczno-szczypiorkową",
                            "Składniki: jajka na twardo, masło, szczypiorek, sól, pieczywo.\n\n"
                                    + "Rozgnieć jajka z miękkim masłem i szczypiorkiem, dopraw i nałóż "
                                    + "na pieczywo."),
                    new Recipe("Sałatka z kurczakiem i kukurydzą",
                            "Składniki: pierś z kurczaka, kukurydza konserwowa, sałata, ogórek, jogurt.\n\n"
                                    + "Usmaż lub upiecz kurczaka, pokrój i wymieszaj z sałatą, kukurydzą "
                                    + "i ogórkiem, polej sosem jogurtowym."),
                    new Recipe("Naleśniki z szynką i serem",
                            "Składniki: mąka, mleko, jajka, szynka, ser żółty, olej do smażenia.\n\n"
                                    + "Usmaż cienkie naleśniki, nałóż szynkę i ser, zwiń i podgrzej "
                                    + "chwilę na patelni, aż ser się rozpuści."),
                    new Recipe("Zupa krem z pomidorów",
                            "Składniki: pomidory lub passata, bulion, śmietana, bazylia.\n\n"
                                    + "Gotuj pomidory z bulionem kilka minut, zblenduj na gładki krem, "
                                    + "dopraw i zabiel śmietaną."),
                    new Recipe("Kanapki z hummusem i warzywami",
                            "Składniki: pieczywo, hummus, ogórek, papryka, rzodkiewka.\n\n"
                                    + "Posmaruj pieczywo hummusem, ułóż pokrojone warzywa."),
                    new Recipe("Jajka faszerowane",
                            "Składniki: jajka na twardo, majonez, musztarda, szczypiorek.\n\n"
                                    + "Przekrój ugotowane jajka na pół, wyjmij żółtka, wymieszaj z "
                                    + "majonezem i musztardą, nałóż z powrotem do połówek białek."),
                    new Recipe("Sałatka z tuńczykiem i fasolą",
                            "Składniki: tuńczyk w sosie własnym, fasola konserwowa, cebula czerwona, "
                                    + "oliwa, cytryna.\n\n"
                                    + "Wymieszaj odsączonego tuńczyka z fasolą i cebulą, skrop oliwą i "
                                    + "sokiem z cytryny."),
                    new Recipe("Kanapki z pastą z makreli",
                            "Składniki: makrela wędzona, twaróg lub serek, cebula, pieczywo.\n\n"
                                    + "Rozgnieć oczyszczoną makrelę z twarogiem i drobno posiekaną "
                                    + "cebulą, nałóż na pieczywo."),
                    new Recipe("Serek wiejski z warzywami na kanapki",
                            "Składniki: serek wiejski, pomidor, ogórek, szczypiorek, pieczywo.\n\n"
                                    + "Wymieszaj serek z pokrojonymi warzywami i szczypiorkiem, podawaj "
                                    + "z pieczywem."),
                    new Recipe("Omlet z szynką i serem",
                            "Składniki: jajka, mleko, szynka, ser żółty, masło.\n\n"
                                    + "Roztrzep jajka z mlekiem, wylej na rozgrzane masło, na wierzch "
                                    + "połóż szynkę i ser, smaż do ścięcia i złóż na pół."),
                    new Recipe("Kanapki z pieczarkami duszonymi",
                            "Składniki: pieczarki, cebula, masło, pieczywo, sól, pieprz.\n\n"
                                    + "Podduś pokrojone pieczarki z cebulą na maśle, dopraw i nałóż na "
                                    + "pieczywo."),
                    new Recipe("Sałatka śledziowa z jabłkiem",
                            "Składniki: śledzie w oleju, jabłko, cebula, jogurt lub śmietana.\n\n"
                                    + "Pokrój śledzie, jabłko i cebulę w kostkę, wymieszaj z jogurtem "
                                    + "lub śmietaną."),
                    new Recipe("Kanapki z pastą serowo-czosnkową",
                            "Składniki: serek topiony lub twarogowy, czosnek, szczypiorek, pieczywo.\n\n"
                                    + "Wymieszaj serek z przeciśniętym czosnkiem i szczypiorkiem, nałóż "
                                    + "na pieczywo.")
            }
    };

    private static volatile Recipe[][] cached;
    public static void useCachedCatalog(Recipe[][] catalog) { cached = catalog; }
    private static Recipe[][] active() { return cached == null ? MEALS : cached; }
    private BuiltInRecipes() {
    }

    /** Number of meals (breakfast, lunch, dinner). */
    public static int mealCount() {
        return MEALS.length;
    }

    /** The bundled recipes for the given meal index (0=breakfast, 1=lunch, 2=dinner). */
    public static Recipe[] forMeal(int mealIndex) {
        return active()[mealIndex].clone();
    }

    /**
     * Title → details for every bundled recipe, optionally merged with the user's
     * cookbook, for building the taste profile in one place.
     */
    public static Map<String, String> detailsByTitle(Cookbook cookbook) {
        Map<String, String> detailsByTitle = new LinkedHashMap<>();
        for (Recipe[] meal : active()) {
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
