package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.mealspire.app.domain.DishReactionLog;
import com.mealspire.app.domain.HttpTransport;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.TasteEvent;
import com.mealspire.app.domain.TasteSurvey;
import com.mealspire.app.domain.TasteSurveySerializer;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesBackendStore;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;
import com.mealspire.app.storage.SharedPreferencesDishReactionStore;
import com.mealspire.app.storage.SharedPreferencesTasteEventStore;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Wcześniejsze zakończenie ankiety A/B: pierwsza seria to 20 porównań, potem
 * „Dalej" albo „Na razie wystarczy"; skrócona ankieta i tak uczy rekomendacje.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TasteSurveyEarlyFinishRobolectricTest {

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

    /** Answers {@code count} pairs (A, B alternately) and returns the picked dishes. */
    private static List<String> answerPairs(MainActivity activity, int from, int count,
                                            int seriesEnd) {
        List<String> picks = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            // Licznik jest lokalny dla serii: „Porównanie 1 z 20” na początku każdej.
            int seriesStart = (i - 1) / TasteSurvey.SERIES_PAIRS * TasteSurvey.SERIES_PAIRS;
            int seriesLength = Math.min(seriesEnd, seriesStart + TasteSurvey.SERIES_PAIRS)
                    - seriesStart;
            assertEquals("Porównanie " + (i - seriesStart) + " z " + seriesLength,
                    text(activity, R.id.survey_progress));
            int option = i % 2 == 0 ? R.id.onboarding_option_2 : R.id.onboarding_option_1;
            picks.add(text(activity, option));
            click(activity, option);
        }
        return picks;
    }

    private Set<String> likedReactions() {
        Set<String> dishes = new LinkedHashSet<>();
        for (DishReaction reaction : new SharedPreferencesDishReactionStore(context).load().all()) {
            assertTrue(reaction.isLiked());
            dishes.add(reaction.getDish());
        }
        return dishes;
    }

    @Test
    public void domyslnaAnkietaPokazujePierwszaSerie20Porownan() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        assertEquals("domyślnie pierwsza seria to 20 porównań, nie 40/80",
                "Porównanie 1 z 20", text(activity, R.id.survey_progress));
        answerPairs(activity, 1, 19, 20);
        assertEquals("Porównanie 20 z 20", text(activity, R.id.survey_progress));
    }

    @Test
    public void zakonczeniePrzed20PytaniemZapisujeWybory() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        List<String> picks = answerPairs(activity, 1, 7, 20);
        assertNull("przed końcem serii nie ma decyzji",
                activity.findViewById(R.id.survey_continue_button));
        click(activity, R.id.onboarding_skip_button);

        assertNull(activity.findViewById(R.id.onboarding_question));
        assertTrue(new SharedPreferencesAppSettings(context).isOnboardingDone());
        assertEquals(new LinkedHashSet<>(picks), likedReactions());
        assertTrue("posiłki dostępne bez pełnej ankiety",
                activity.<Button>findViewById(R.id.meal_lunch_button).isEnabled());
    }

    @Test
    public void po20OdpowiedziachObieOpcjeINaRazieWystarczyKonczyAnkiete() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        List<String> picks = answerPairs(activity, 1, 20, 20);

        assertNull("decyzja zamiast 21. pary", activity.findViewById(R.id.onboarding_option_1));
        assertEquals("Dalej", text(activity, R.id.survey_continue_button));
        assertEquals("Na razie wystarczy", text(activity, R.id.survey_enough_button));
        click(activity, R.id.survey_enough_button);

        assertNull(activity.findViewById(R.id.onboarding_question));
        assertEquals(new LinkedHashSet<>(picks), likedReactions());
        TasteSurvey stored = new SharedPreferencesTasteSurveyStore(context).load();
        assertTrue(stored.isFinished());
        assertEquals(20, stored.size());
        assertEquals(20, stored.committed());
        assertNull("skrócona ankieta nie wymusza dokończenia",
                activity.findViewById(R.id.survey_resume_button));

        click(activity, R.id.meal_lunch_button);
        assertNull("wybór posiłku nie wraca do ankiety",
                activity.findViewById(R.id.onboarding_question));
    }

    @Test
    public void dalejZachowujeDotychczasoweOdpowiedzi() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        List<String> picks = answerPairs(activity, 1, 20, 20);
        click(activity, R.id.survey_continue_button);

        assertEquals("postęp liczony w bieżącej serii", "Porównanie 1 z 20",
                text(activity, R.id.survey_progress));
        TasteSurvey stored = new SharedPreferencesTasteSurveyStore(context).load();
        assertEquals(20, stored.position());
        assertEquals(20, stored.pendingPicks().size());

        // Cofnij z 21. pary wraca do 20. — odpowiedzi nadal są.
        activity.onBackPressed();
        assertEquals("Porównanie 20 z 20", text(activity, R.id.survey_progress));
        picks.remove(picks.size() - 1);
        picks.addAll(answerPairs(activity, 20, 1, 20));
        assertNotNull("„Dalej” już wybrane — bez ponownej decyzji",
                activity.findViewById(R.id.survey_progress));

        picks.addAll(answerPairs(activity, 21, 3, 40));
        click(activity, R.id.onboarding_skip_button);
        assertEquals(new LinkedHashSet<>(picks), likedReactions());
    }

    @Test
    public void bardzoDlugaAnkieta80TezMaDecyzjePo20() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(80);
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        answerPairs(activity, 1, 20, 20);
        assertNotNull(activity.findViewById(R.id.survey_enough_button));
        click(activity, R.id.survey_continue_button);
        answerPairs(activity, 21, 20, 40);
        assertNotNull("kolejna seria też kończy się decyzją",
                activity.findViewById(R.id.survey_enough_button));
    }

    @Test
    public void skroconaAnkietaTrafiaDoOcenyPropozycji() throws Exception {
        new SharedPreferencesAppSettings(context).markOnboardingDone();
        new SharedPreferencesChatGptSessionStore(context).save(new ChatGptSession(
                "client", "access", "refresh", "id", System.currentTimeMillis() + 3_600_000,
                "subject", "email", "gpt-6-luna", "gpt-6.1-sol"));
        SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
        store.markServerNoticeShown();
        JSONArray meals = new JSONArray();
        for (int meal = 0; meal < 3; meal++) {
            JSONArray dishes = new JSONArray();
            for (int i = 1; i <= 8; i++) {
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
        click(activity, R.id.survey_resume_button);
        List<String> picks = answerPairs(activity, 1, 20, 20);
        click(activity, R.id.survey_enough_button);
        runAll(background);

        assertTrue("skrócona ankieta odświeża gotową pulę", transport.bodies.size() > before);
        String reactions = new JSONObject(transport.bodies.get(transport.bodies.size() - 1))
                .getJSONArray("reactions").toString();
        for (String pick : new LinkedHashSet<>(picks)) {
            assertTrue("wybór ze skróconej ankiety w ocenie: " + pick, reactions.contains(pick));
        }
    }

    // ----- Uwagi z review: odzyskanie po zabiciu procesu, pauza, postęp, „Dalej” -----

    /** Ankieta „Na razie wystarczy” po {@code answered} odpowiedziach, jeszcze bez zapisu wyborów. */
    private static TasteSurvey finishedEarlyNotCommitted(int answered) {
        List<TasteSurvey.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            pairs.add(new TasteSurvey.Pair("Danie A" + i, "Danie B" + i, i % 3));
        }
        TasteSurvey survey = TasteSurvey.start(pairs, "[]");
        for (int i = 0; i < answered; i++) {
            survey = survey.answer(i % 3 == 2 ? TasteSurvey.CHOICE_NEITHER
                    : i % 2 == 0 ? TasteSurvey.CHOICE_A : TasteSurvey.CHOICE_B);
        }
        return survey.finishEarly();
    }

    private List<String> allReactionDishes() {
        List<String> dishes = new ArrayList<>();
        for (DishReaction reaction : new SharedPreferencesDishReactionStore(context).load().all()) {
            dishes.add(reaction.getDish());
        }
        return dishes;
    }

    private List<String> onboardingPickEvents() {
        List<String> dishes = new ArrayList<>();
        for (TasteEvent event : new SharedPreferencesTasteEventStore(context).load().events()) {
            if (event.getType() == TasteEvent.Type.ONBOARDING_PICK) {
                dishes.add(event.getDishTitle());
            }
        }
        return dishes;
    }

    @Test
    public void zabicieProcesuPrzedZapisemWyborowNieGubiOdpowiedziINieDublujePrzyPonowieniu() {
        // „Na razie wystarczy” po 22 odpowiedziach zapisało ankietę, a proces zginął,
        // zanim wybory trafiły do reakcji (onboarding nie zdążył się zakończyć).
        TasteSurvey interrupted = finishedEarlyNotCommitted(22);
        List<String> picks = interrupted.pendingPicks();
        assertFalse(picks.isEmpty());
        new SharedPreferencesTasteSurveyStore(context).save(interrupted);

        MainActivity activity = launch();

        assertEquals("zapisane odpowiedzi odzyskane jako reakcje", picks, allReactionDishes());
        assertEquals(picks, onboardingPickEvents());
        TasteSurvey stored = new SharedPreferencesTasteSurveyStore(context).load();
        assertEquals(22, stored.committed());
        assertTrue(stored.isFinished());
        assertNull("odzyskana ankieta nie zaczyna się od nowa",
                activity.findViewById(R.id.onboarding_question));
        assertTrue(new SharedPreferencesAppSettings(context).isOnboardingDone());

        launch(); // ponowne uruchomienie: nic się nie dubluje
        assertEquals(picks, allReactionDishes());
        assertEquals(picks, onboardingPickEvents());
    }

    @Test
    public void ponowienieZapisuPrzerwanegoPoReakcjachDokanczaGoBezDuplikatow() throws Exception {
        // Proces zginął w trakcie zapisu: reakcje z tej partii już są (ten sam znacznik
        // czasu), zdarzeń gustu i zatwierdzenia ankiety jeszcze nie ma.
        long stamp = 1_700_000_000_000L;
        TasteSurvey interrupted = finishedEarlyNotCommitted(22);
        List<String> picks = interrupted.pendingPicks();
        String json = new JSONObject(new TasteSurveySerializer().toJson(interrupted))
                .put("commitStamp", stamp).toString();
        context.getSharedPreferences("mealspire_taste_survey", Context.MODE_PRIVATE).edit()
                .putString("taste_survey_json", json).commit();
        DishReactionLog written = DishReactionLog.empty();
        for (String pick : picks) {
            written = written.append(new DishReaction(pick, "", true, stamp));
        }
        new SharedPreferencesDishReactionStore(context).save(written);
        new SharedPreferencesAppSettings(context).markOnboardingDone();

        launch();

        assertEquals("bez duplikatów reakcji", picks, allReactionDishes());
        assertEquals("brakujące zdarzenia dopisane", picks, onboardingPickEvents());
        assertEquals(22, new SharedPreferencesTasteSurveyStore(context).load().committed());

        launch();
        assertEquals(picks, allReactionDishes());
        assertEquals(picks, onboardingPickEvents());
    }

    @Test
    public void naCheckpoinciePrzerwijZachowujeOdpowiedziIWracaDoDecyzji() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        List<String> picks = answerPairs(activity, 1, 20, 20);

        assertNotNull("checkpoint ma też pauzę", activity.findViewById(R.id.onboarding_skip_button));
        assertEquals("Przerwij — dokończę później", text(activity, R.id.onboarding_skip_button));
        click(activity, R.id.onboarding_skip_button);

        assertNull(activity.findViewById(R.id.onboarding_question));
        assertEquals(new LinkedHashSet<>(picks), likedReactions());
        TasteSurvey stored = new SharedPreferencesTasteSurveyStore(context).load();
        assertEquals(20, stored.position());
        assertEquals(20, stored.committed());
        assertFalse("pauza nie kończy ankiety", stored.isFinished());

        activity = launch();
        click(activity, R.id.survey_resume_button);
        assertNotNull("wznowienie wraca do decyzji", activity.findViewById(R.id.survey_continue_button));
        click(activity, R.id.survey_continue_button);
        assertEquals("Porównanie 1 z 20", text(activity, R.id.survey_progress));
    }

    @Test
    public void ostatniaKrotszaSeriaLiczyPostepOdSwojegoPoczatku() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(30);
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        answerPairs(activity, 1, 20, 30);
        click(activity, R.id.survey_continue_button);
        assertEquals("Porównanie 1 z 10", text(activity, R.id.survey_progress));
        answerPairs(activity, 21, 10, 30);
    }

    @Test
    public void cofniecieIPonownaAkceptacjaWczesniejszegoCheckpointuZachowujeNajdalszy() {
        new SharedPreferencesAppSettings(context).saveSurveyLength(60);
        MainActivity activity = launch();
        answerProfileQuestions(activity);
        answerPairs(activity, 1, 20, 60);
        click(activity, R.id.survey_continue_button);
        answerPairs(activity, 21, 20, 60);
        click(activity, R.id.survey_continue_button);
        answerPairs(activity, 41, 1, 60);

        // Cofnij aż do 20. pary (przez zaakceptowany checkpoint przy 40).
        for (int i = 0; i < 22; i++) {
            activity.onBackPressed();
        }
        assertEquals("Porównanie 20 z 20", text(activity, R.id.survey_progress));
        answerPairs(activity, 20, 1, 60);
        assertNull("checkpoint 20 objęty zaakceptowanym 40 — bez ponownej decyzji",
                activity.findViewById(R.id.survey_continue_button));
        assertEquals(40, new SharedPreferencesTasteSurveyStore(context).load().continuedAt());

        answerPairs(activity, 21, 20, 60);
        assertNull("przy 40 „Dalej” już zaakceptowane",
                activity.findViewById(R.id.survey_continue_button));
        assertEquals("Porównanie 1 z 20", text(activity, R.id.survey_progress));
    }

    private static void runAll(List<Runnable> tasks) {
        while (!tasks.isEmpty()) {
            tasks.remove(0).run();
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

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
