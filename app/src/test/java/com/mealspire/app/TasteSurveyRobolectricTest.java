package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.backend.BackendCodec;
import com.mealspire.app.domain.ChatGptSession;
import com.mealspire.app.domain.DishRating;
import com.mealspire.app.domain.DishReaction;
import com.mealspire.app.domain.HttpTransport;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.TasteSurvey;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesBackendStore;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;
import com.mealspire.app.storage.SharedPreferencesDishReactionStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;
import com.mealspire.app.storage.SharedPreferencesTasteSurveyStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Długa ankieta gustu A/B: seria różnych par z widocznym postępem, zapis
 * każdej odpowiedzi i wznowienie po przerwaniu, wynik jako zwykłe reakcje
 * „lubię", które trafiają do kolejnej oceny propozycji. Sieć zastępuje stub.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TasteSurveyRobolectricTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        MainActivity.backendTransportOverride = null;
        MainActivity.readyPoolExecutorOverride = null;
    }

    private static MainActivity launch() {
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    private static void click(MainActivity activity, int id) {
        activity.<Button>findViewById(id).performClick();
    }

    private static String text(MainActivity activity, int id) {
        TextView view = activity.findViewById(id);
        return view == null ? null : view.getText().toString();
    }

    /** Dziewięć pytań profilu (dieta i kuchnie bez zaznaczeń), do pierwszej pary ankiety. */
    private static void answerProfileQuestions(MainActivity activity) {
        click(activity, R.id.onboarding_option_1);
        click(activity, R.id.onboarding_next_button);
        click(activity, R.id.onboarding_option_2);
        click(activity, R.id.onboarding_option_2);
        click(activity, R.id.onboarding_next_button);
        for (int i = 0; i < 4; i++) {
            click(activity, R.id.onboarding_option_2);
        }
    }

    private Set<String> likes() {
        return new HashSet<>(new SharedPreferencesPreferenceStore(context).load().getLikes());
    }

    private List<String> likedReactions() {
        List<String> dishes = new ArrayList<>();
        for (DishReaction reaction : new SharedPreferencesDishReactionStore(context).load().all()) {
            assertTrue(reaction.isLiked());
            dishes.add(reaction.getDish());
        }
        return dishes;
    }

    @Test
    public void seriaRoznychParZWidocznymPostepemIKonfigurowalnaDlugoscia() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(12);
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        Set<String> pairs = new HashSet<>();
        for (int i = 1; i <= 12; i++) {
            assertTrue(text(activity, R.id.onboarding_question).startsWith("Co wolisz na "));
            assertEquals("Porównanie " + i + " z 12", text(activity, R.id.survey_progress));
            String a = text(activity, R.id.onboarding_option_1);
            String b = text(activity, R.id.onboarding_option_2);
            assertNotEquals(a, b);
            assertTrue("para się powtórzyła", pairs.add(a + "|" + b));
            click(activity, i % 3 == 0 ? R.id.onboarding_option_none
                    : i % 2 == 0 ? R.id.onboarding_option_2 : R.id.onboarding_option_1);
        }

        assertNull("koniec ankiety kończy quiz", activity.findViewById(R.id.onboarding_question));
        // 12 par, co trzecia „Żadne z tych" → 8 wyborów; dania mogą się powtórzyć.
        assertEquals(8, likedReactions().size());
        assertEquals(new HashSet<>(likedReactions()), likes());
        assertNull("ukończona ankieta nie zaprasza ponownie na ekranie startowym",
                activity.findViewById(R.id.survey_resume_button));
    }

    @Test
    public void ankietaWyswietlaNaturalnePolskieNazwyDan() {
        // 30 par po 10 na porę posiłku przegląda całą wbudowaną pulę (3 × 20 dań).
        new SharedPreferencesAppSettings(context).saveSurveyLength(30);
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        Set<String> catalog = com.mealspire.app.domain.BuiltInRecipes.detailsByTitle(null).keySet();
        Set<String> shown = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            shown.add(text(activity, R.id.onboarding_option_1));
            shown.add(text(activity, R.id.onboarding_option_2));
            click(activity, R.id.onboarding_option_none);
        }

        assertEquals("ankieta pokazuje całą pulę dań", catalog, shown);
        assertTrue(shown.contains("Pierogi ruskie"));
        assertTrue(shown.contains("Kanapki z serkiem wiejskim i warzywami"));
        for (String dish : shown) {
            assertTrue("kalka w ankiecie: " + dish, !dish.endsWith("na kanapki"));
            assertTrue("opisowa nazwa w ankiecie: " + dish,
                    !dish.startsWith("Pierogi z serem i ziemniakami"));
        }
    }

    /** Dania posiłku dozwolone przy diecie, z aktualnej wbudowanej puli. */
    private static List<String> allowedDishes(int meal,
                                              com.mealspire.app.domain.DietConstraints diet) {
        List<String> result = new ArrayList<>();
        for (Recipe recipe : com.mealspire.app.domain.BuiltInRecipes.forMeal(meal)) {
            if (diet.allows(recipe.getTitle() + "\n" + recipe.getDetails())) {
                result.add(recipe.getTitle());
            }
        }
        return result;
    }

    @Test
    public void poAktualizacjiZapisanaParaZeStaraNazwaNieOmijaWykluczeniaLaktozy() {
        // Stan z 1.8: ankieta przerwana po jednej parze, dieta bez wykluczeń. Druga para
        // ma starą nazwę dania z masłem; po aktualizacji użytkownik wyklucza laktozę.
        com.mealspire.app.domain.DietConstraints noLactose =
                com.mealspire.app.domain.DietConstraints.of(java.util.Collections.singletonList(
                        com.mealspire.app.domain.DietConstraints.Exclusion.NO_LACTOSE));
        new com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore(context).save(
                com.mealspire.app.domain.HouseholdProfile.empty().withDiet(noLactose));
        List<String> supper = allowedDishes(2, noLactose);
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        pairs.add(new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół", 1));
        pairs.add(new TasteSurvey.Pair("Kanapki z pieczarkami duszonymi", supper.get(0), 2));
        pairs.add(new TasteSurvey.Pair(supper.get(1), supper.get(2), 2));
        new SharedPreferencesTasteSurveyStore(context).save(TasteSurvey.start(pairs, "[]")
                .answer(TasteSurvey.CHOICE_A).commit());

        MainActivity activity = launch();

        assertEquals("para ze starą nazwą odpada, zostaje dozwolona", supper.get(1),
                text(activity, R.id.onboarding_option_1));
        assertEquals(supper.get(2), text(activity, R.id.onboarding_option_2));
        assertEquals("Porównanie 2 z 2", text(activity, R.id.survey_progress));
        TasteSurvey saved = new SharedPreferencesTasteSurveyStore(context).load();
        for (TasteSurvey.Pair pair : saved.pairs().subList(saved.position(), saved.size())) {
            assertNotEquals("Kanapki z pieczarkami duszonymi", pair.getDishA());
            assertNotEquals("Kanapki z pieczarkami duszonymi", pair.getDishB());
        }
    }

    @Test
    public void staraNazwaZapisanaWKsiazceKucharskiejNieZostawiaParyZeStaraNazwa() {
        // Danie zapisane do książki przed aktualizacją zachowuje tam starą nazwę;
        // pary ankiety pochodzą z katalogu, więc para ze starą nazwą i tak odpada.
        new com.mealspire.app.storage.SharedPreferencesCookbookStore(context).save(
                com.mealspire.app.domain.Cookbook.empty().add(new com.mealspire.app.domain.CookbookEntry(
                        "Pierogi z serem i ziemniakami", "Składniki: ziemniaki, twaróg.", "")));
        List<String> dinner = allowedDishes(1, com.mealspire.app.domain.DietConstraints.empty());
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        pairs.add(new TasteSurvey.Pair(dinner.get(0), dinner.get(1), 1));
        pairs.add(new TasteSurvey.Pair("Pierogi z serem i ziemniakami", dinner.get(2), 1));
        pairs.add(new TasteSurvey.Pair(dinner.get(3), dinner.get(4), 1));
        new SharedPreferencesTasteSurveyStore(context).save(TasteSurvey.start(pairs, "[]")
                .answer(TasteSurvey.CHOICE_A).commit());

        MainActivity activity = launch();

        assertEquals(dinner.get(3), text(activity, R.id.onboarding_option_1));
        assertEquals("Porównanie 2 z 2", text(activity, R.id.survey_progress));
    }

    @Test
    public void dietaParySprawdzanaNaSkladnikachZKataloguNieZKsiazkiKucharskiej() {
        // W książce „Pierogi ruskie" w wersji bez nabiału; w ankiecie są pierogi z katalogu
        // (twaróg, masło), więc po wykluczeniu laktozy para z nimi musi odpaść.
        com.mealspire.app.domain.DietConstraints noLactose =
                com.mealspire.app.domain.DietConstraints.of(java.util.Collections.singletonList(
                        com.mealspire.app.domain.DietConstraints.Exclusion.NO_LACTOSE));
        new com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore(context).save(
                com.mealspire.app.domain.HouseholdProfile.empty().withDiet(noLactose));
        new com.mealspire.app.storage.SharedPreferencesCookbookStore(context).save(
                com.mealspire.app.domain.Cookbook.empty().add(new com.mealspire.app.domain.CookbookEntry(
                        "Pierogi ruskie", "Składniki: ziemniaki, tofu, cebula, olej.", "")));
        List<String> dinner = allowedDishes(1, noLactose);
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        pairs.add(new TasteSurvey.Pair(dinner.get(0), dinner.get(1), 1));
        pairs.add(new TasteSurvey.Pair("Pierogi ruskie", dinner.get(2), 1));
        pairs.add(new TasteSurvey.Pair(dinner.get(3), dinner.get(4), 1));
        new SharedPreferencesTasteSurveyStore(context).save(TasteSurvey.start(pairs, "[]")
                .answer(TasteSurvey.CHOICE_A).commit());

        MainActivity activity = launch();

        assertEquals(dinner.get(3), text(activity, R.id.onboarding_option_1));
        assertEquals("Porównanie 2 z 2", text(activity, R.id.survey_progress));
    }

    @Test
    public void paryPrzedNamiSprawdzaneZDietaTakzeBezZmianyWykluczen() {
        // Dieta bez zmian, ale składniki dania w katalogu się zmieniły (tu: ankieta zapisana
        // z parą, która przy obecnym katalogu łamie wykluczenie laktozy) — para odpada.
        com.mealspire.app.domain.DietConstraints noLactose =
                com.mealspire.app.domain.DietConstraints.of(java.util.Collections.singletonList(
                        com.mealspire.app.domain.DietConstraints.Exclusion.NO_LACTOSE));
        new com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore(context).save(
                com.mealspire.app.domain.HouseholdProfile.empty().withDiet(noLactose));
        List<String> dinner = allowedDishes(1, noLactose);
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        pairs.add(new TasteSurvey.Pair(dinner.get(0), dinner.get(1), 1));
        pairs.add(new TasteSurvey.Pair("Pierogi ruskie", dinner.get(2), 1));
        pairs.add(new TasteSurvey.Pair(dinner.get(3), dinner.get(4), 1));
        new SharedPreferencesTasteSurveyStore(context).save(TasteSurvey.start(pairs,
                noLactose.getExclusions().toString()).answer(TasteSurvey.CHOICE_A).commit());

        MainActivity activity = launch();

        assertEquals(dinner.get(3), text(activity, R.id.onboarding_option_1));
        assertEquals("Porównanie 2 z 2", text(activity, R.id.survey_progress));
    }

    @Test
    public void poAktualizacjiBezZapisanychOdpowiedziAnkietaPlanujeSieOdNowaZNowymiNazwami() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(30);
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        pairs.add(new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół", 1));
        pairs.add(new TasteSurvey.Pair("Serek wiejski z warzywami na kanapki", "Omlet", 2));
        new SharedPreferencesTasteSurveyStore(context).save(TasteSurvey.start(pairs, "[]"));

        MainActivity activity = launch();

        assertEquals("Porównanie 1 z 30", text(activity, R.id.survey_progress));
        Set<String> catalog = com.mealspire.app.domain.BuiltInRecipes.detailsByTitle(null).keySet();
        assertTrue(catalog.contains(text(activity, R.id.onboarding_option_1)));
        assertTrue(catalog.contains(text(activity, R.id.onboarding_option_2)));
    }

    @Test
    public void katalogZCacheSprzedAktualizacjiNiePokazujeStarychNazwTakzeBezLogowania()
            throws JSONException {
        // Katalog pobrany przez 1.8 (bez wersji nazw), świeży — bez sesji ChatGPT nie
        // zostałby odświeżony. Po aktualizacji ankieta ma pokazać aktualne nazwy.
        JSONArray meals = new JSONArray();
        for (int meal = 0; meal < 3; meal++) {
            JSONArray recipes = new JSONArray();
            for (Recipe recipe : com.mealspire.app.domain.BuiltInRecipes.forMeal(meal)) {
                String title = recipe.getTitle().equals("Pierogi ruskie")
                        ? "Pierogi z serem i ziemniakami"
                        : recipe.getTitle().equals("Kanapki z serkiem wiejskim i warzywami")
                        ? "Serek wiejski z warzywami na kanapki" : recipe.getTitle();
                recipes.put(BackendCodec.recipe(new Recipe(title, recipe.getDetails())));
            }
            meals.put(recipes);
        }
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear()
                .putString("url", com.mealspire.app.backend.BackendClient.DEFAULT_BASE_URL)
                .putString("catalog", BackendCodec.envelope().put("meals", meals).toString())
                .putLong("catalog_time", System.currentTimeMillis()).commit();
        // Brak sesji ChatGPT: refreshCatalog nie pyta serwera, liczy się tylko cache.
        new SharedPreferencesAppSettings(context).saveSurveyLength(30);
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        Set<String> shown = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            shown.add(text(activity, R.id.onboarding_option_1));
            shown.add(text(activity, R.id.onboarding_option_2));
            click(activity, R.id.onboarding_option_none);
        }

        assertEquals(com.mealspire.app.domain.BuiltInRecipes.detailsByTitle(null).keySet(), shown);
        assertTrue(new SharedPreferencesBackendStore(context).needsCatalog());
    }

    @Test
    public void przerwanaAnkietaWznawiaSiePoPonownymUruchomieniu() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(5);
        MainActivity first = launch();
        answerProfileQuestions(first);
        String pick1 = text(first, R.id.onboarding_option_1);
        click(first, R.id.onboarding_option_1);
        String pick2 = text(first, R.id.onboarding_option_2);
        click(first, R.id.onboarding_option_2);
        String third = text(first, R.id.onboarding_option_1);
        // Proces ginie tu — bez „Przerwij" i bez zakończenia quizu.

        MainActivity second = launch();
        assertEquals("wznowienie od trzeciej pary", "Porównanie 3 z 5",
                text(second, R.id.survey_progress));
        assertEquals(third, text(second, R.id.onboarding_option_1));

        // Cofnij działa także na odpowiedziach sprzed przerwy.
        second.onBackPressed();
        assertEquals("Porównanie 2 z 5", text(second, R.id.survey_progress));
        click(second, R.id.onboarding_option_2);
        for (int i = 3; i <= 5; i++) {
            click(second, R.id.onboarding_option_none);
        }

        assertNull(second.findViewById(R.id.onboarding_question));
        Set<String> expected = new HashSet<>();
        expected.add(pick1);
        expected.add(pick2);
        assertEquals(expected, likes());
        assertEquals(2, likedReactions().size());
    }

    @Test
    public void przerwijZapisujeWyboryIEkranStartowyPozwalaDokonczyc() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(4);
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        String pick = text(activity, R.id.onboarding_option_1);
        click(activity, R.id.onboarding_option_1);
        assertEquals("Przerwij — dokończę później", text(activity, R.id.onboarding_skip_button));
        click(activity, R.id.onboarding_skip_button);

        assertTrue(new SharedPreferencesAppSettings(context).isOnboardingDone());
        assertEquals("wybór sprzed przerwania już uczy", java.util.Collections.singletonList(pick),
                likedReactions());
        assertEquals("Dokończ ankietę gustu (1 z 4)", text(activity, R.id.survey_resume_button));

        click(activity, R.id.survey_resume_button);
        assertEquals("Porównanie 2 z 4", text(activity, R.id.survey_progress));
        // Zapisanej odpowiedzi nie da się cofnąć — Cofnij wychodzi na ekran startowy.
        activity.onBackPressed();
        assertNull(activity.findViewById(R.id.onboarding_question));
        assertNotNull(activity.findViewById(R.id.survey_resume_button));

        click(activity, R.id.survey_resume_button);
        for (int i = 2; i <= 4; i++) {
            click(activity, R.id.onboarding_option_2);
        }
        assertEquals(4, likedReactions().size());
        TasteSurvey stored = new SharedPreferencesTasteSurveyStore(context).load();
        assertTrue(stored.isFinished());
        assertEquals(stored.size(), stored.committed());
        assertNull(activity.findViewById(R.id.survey_resume_button));
    }

    @Test
    public void wynikAnkietyTrafiaDoKolejnejOcenyPropozycji() throws Exception {
        new SharedPreferencesAppSettings(context).markOnboardingDone();
        new SharedPreferencesAppSettings(context).saveSurveyLength(4);
        new SharedPreferencesChatGptSessionStore(context).save(new ChatGptSession(
                "client", "access", "refresh", "id", System.currentTimeMillis() + 3_600_000,
                "subject", "email", "gpt-6-luna", "gpt-6.1-sol"));
        SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
        store.markServerNoticeShown();
        JSONArray meals = new JSONArray();
        for (int meal = 0; meal < 3; meal++) {
            JSONArray dishes = new JSONArray();
            for (int i = 1; i <= 6; i++) {
                dishes.put(BackendCodec.recipe(new Recipe("Danie " + meal + "-" + i,
                        "Składniki: warzywa " + i + ".\nPrzygotowanie: ugotuj.")));
            }
            meals.put(dishes);
        }
        store.cache(store.baseUrl(), BackendCodec.envelope().put("revision", "t").put("meals", meals));
        RecordingTransport transport = new RecordingTransport();
        List<Runnable> background = new ArrayList<>();
        Executor queued = background::add;
        MainActivity.backendTransportOverride = transport;
        MainActivity.readyPoolExecutorOverride = queued;

        MainActivity activity = launch();
        runAll(background);
        int before = transport.bodies.size();
        assertEquals("Ankieta gustu: A czy B?", text(activity, R.id.survey_resume_button));

        click(activity, R.id.survey_resume_button);
        List<String> picks = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            assertEquals("Porównanie " + i + " z 4", text(activity, R.id.survey_progress));
            String dish = text(activity, R.id.onboarding_option_1);
            assertTrue("ankieta używa wspólnego katalogu dań: " + dish, dish.startsWith("Danie "));
            picks.add(dish);
            click(activity, R.id.onboarding_option_1);
        }
        runAll(background);

        assertTrue("nowe polubienia odświeżają gotową pulę",
                transport.bodies.size() > before);
        JSONArray reactions = new JSONObject(transport.bodies.get(transport.bodies.size() - 1))
                .getJSONArray("reactions");
        Set<String> sent = new HashSet<>();
        for (int i = 0; i < reactions.length(); i++) {
            sent.add(reactions.getJSONObject(i).toString());
        }
        for (String pick : picks) {
            boolean found = false;
            for (String reaction : sent) {
                found |= reaction.contains(pick);
            }
            assertTrue("wybór z ankiety w ocenie propozycji: " + pick, found);
        }
    }

    private static void runAll(List<Runnable> tasks) {
        while (!tasks.isEmpty()) {
            tasks.remove(0).run();
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    /** /v1/rate records taste; the background /v1/proposals call returns no extra dishes. */
    private static final class RecordingTransport implements HttpTransport {
        final List<String> bodies = new ArrayList<>();

        public Response get(String url, String token) throws IOException {
            throw new AssertionError("Catalogue is cached; no GET expected: " + url);
        }

        public Response postJson(String url, String token, String body) throws IOException {
            try {
                if (url.endsWith("/v1/proposals")) {
                    return new Response(200,
                            BackendCodec.envelope().put("proposals", new JSONArray()).toString());
                }
                assertTrue(url.endsWith("/v1/rate"));
                bodies.add(body);
                JSONArray candidates = new JSONObject(body).getJSONArray("candidates");
                JSONArray ratings = new JSONArray();
                for (int i = 0; i < candidates.length(); i++) {
                    String name = candidates.getJSONObject(i).getString("name");
                    ratings.put(BackendCodec.rating(new DishRating(name, 8, "Powód")));
                }
                return new Response(200, BackendCodec.envelope().put("ratings", ratings).toString());
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }

        public Response postForm(String url, Map<String, String> form) {
            throw new AssertionError("Valid stored session must not require OAuth requests");
        }
    }
}
