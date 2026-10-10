package com.mealspire.app;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.backend.BackendCodec;
import com.mealspire.app.domain.*;
import com.mealspire.app.storage.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Executor;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * Gotowa pula dań: propozycje pokazują się od razu (bez wywołania LLM na
 * kliknięcie), LLM generuje nowe dania w tle — przy pustej puli i zanim się
 * wyczerpie — najwyżej jedno uzupełnienie naraz, a w serii nie ma powtórek.
 * Sieć zastępuje stub — AI nie jest wołane naprawdę.
 */
@RunWith(RobolectricTestRunner.class) @Config(sdk = 28)
public class ReadyDishPoolRobolectricTest {
    private static final int LUNCH = 1;
    private final Context context = ApplicationProvider.getApplicationContext();
    private final StubTransport transport = new StubTransport();
    private final QueuedExecutor background = new QueuedExecutor();

    @Before public void setUp() throws Exception {
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear().commit();
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
        MainActivity.backendTransportOverride = transport;
        MainActivity.readyPoolExecutorOverride = background;
    }

    @After public void tearDown() {
        MainActivity.backendTransportOverride = null;
        MainActivity.readyPoolExecutorOverride = null;
    }

    @Test public void emptyPoolShowsOfflineAtOnceAndLlmGeneratesNewDishesInBackground() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertEquals("start warms every meal in the background", 3, background.size());
        assertEquals(0, transport.calls());

        click(activity, R.id.meal_lunch_button);
        assertEquals("no LLM call while the user waits", 0, transport.calls());
        assertEquals(3, proposals(activity).size());
        assertEquals(MainActivity.proposalSourceNote(ReadyProposals.Source.OFFLINE), statusText(activity));
        assertTrue(activity.findViewById(R.id.meal_lunch_button).isEnabled());

        background.runAll();
        assertEquals("one generation per meal", 3, transport.proposeCalls);
        ReadyDishPool pool = readyPool(LUNCH);
        assertNotNull("pool holds dishes the LLM generated", generatedIn(pool));

        int before = transport.calls();
        click(activity, R.id.refresh_button);
        assertEquals("ready pool is served without a new LLM call", before, transport.calls());
        assertNull("full set from the pool needs no status note", activity.findViewById(R.id.ready_pool_status));
    }

    @Test public void refillStartsBeforeThePoolRunsOutAndOnlyOnceAtATime() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        background.runAll();
        click(activity, R.id.meal_lunch_button);
        background.runAll(); // pool for lunch is full now
        int generations = transport.proposeCalls;

        // Tap on without letting the background finish: the refill is queued
        // while the pool still has dishes, and taps keep being served from it.
        int queuedAtSize = -1;
        List<String> shownWhenQueued = null;
        for (int tap = 0; tap < 6 && queuedAtSize < 0; tap++) {
            click(activity, R.id.refresh_button);
            assertEquals(3, proposals(activity).size());
            if (background.size() > 0) {
                queuedAtSize = readyPool(LUNCH).size();
                shownWhenQueued = names(activity);
            }
        }
        // The next tap, with the refill still running, is still served at once
        // from what is left — and does not start a second refill.
        click(activity, R.id.refresh_button);
        assertEquals(3, proposals(activity).size());
        assertEquals(ReadyProposals.Source.READY, field(activity, "proposalSource"));
        assertTrue("refill queued before the pool was empty", queuedAtSize > 0);
        assertTrue("refill queued once the pool dropped below two sets",
                queuedAtSize < ReadyProposals.LOW_WATER_SETS * 3);
        assertEquals("no parallel duplicate refills for one meal", 1, background.size());
        assertEquals("nothing ran on the tap itself", generations, transport.proposeCalls);

        background.runAll();
        assertEquals("exactly one generation for the queued refill", generations + 1,
                transport.proposeCalls);
        assertTrue("the generation was told what to avoid",
                lower(transport.lastAvoid).containsAll(lower(shownWhenQueued)));
    }

    @Test public void seriesNeverRepeatsWhileLlmKeepsAddingNewDishes() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        background.runAll();
        click(activity, R.id.meal_lunch_button);
        List<String> seen = new ArrayList<>(names(activity));
        for (int tap = 0; tap < 8; tap++) {
            background.runAll(); // refills happen in the background between taps
            click(activity, R.id.refresh_button);
            seen.addAll(names(activity));
        }
        assertEquals("beyond the 8 catalogue dishes thanks to generation", 27, seen.size());
        assertEquals("no repeats in one series", seen.size(), new HashSet<>(seen).size());
    }

    @Test public void seriesEndsWithExplicitStateWhenNothingNewIsLeft() throws Exception {
        transport.generateNothing = true;
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        background.runAll();

        click(activity, R.id.meal_lunch_button);
        List<String> seen = new ArrayList<>(names(activity));
        Button refresh = activity.findViewById(R.id.refresh_button);
        for (int guard = 0; guard < 10 && "Inne propozycje".contentEquals(refresh.getText()); guard++) {
            background.runAll();
            refresh.performClick();
            seen.addAll(names(activity));
            refresh = activity.findViewById(R.id.refresh_button);
        }

        assertEquals("every catalogue dish shown once", 8, seen.size());
        assertEquals("no repeats in one series", 8, new HashSet<>(seen).size());
        assertEquals("Zacznij od nowa", refresh.getText().toString());
        assertEquals(MainActivity.proposalSourceNote(ReadyProposals.Source.EXHAUSTED), statusText(activity));

        refresh.performClick(); // a new series starts over
        assertEquals(3, proposals(activity).size());
    }

    @Test public void failingRefillKeepsInterfaceUsable() throws Exception {
        transport.fail = true;
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        background.runAll();
        click(activity, R.id.meal_lunch_button);
        background.runAll();
        assertEquals(3, proposals(activity).size());
        assertEquals(MainActivity.proposalSourceNote(ReadyProposals.Source.OFFLINE), statusText(activity));
        assertEquals(0, readyPool(LUNCH).size());
    }

    private static Set<String> lower(List<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) result.add(name.toLowerCase());
        return result;
    }

    private ReadyDishPool readyPool(int meal) {
        return new SharedPreferencesBackendStore(context).readyPool(meal);
    }

    private static DishProposal generatedIn(ReadyDishPool pool) {
        for (DishRating entry : pool.getEntries()) {
            if (pool.generated(entry.getDish()) != null) return pool.generated(entry.getDish());
        }
        return null;
    }

    private static void click(MainActivity activity, int id) {
        activity.findViewById(id).performClick();
    }

    private static String statusText(MainActivity activity) {
        TextView status = activity.findViewById(R.id.ready_pool_status);
        return status == null ? null : status.getText().toString();
    }

    private static Object field(MainActivity activity, String name) throws Exception {
        Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(activity);
    }

    @SuppressWarnings("unchecked")
    private static List<DishProposal> proposals(MainActivity activity) throws Exception {
        return (List<DishProposal>) field(activity, "proposals");
    }

    private static List<String> names(MainActivity activity) throws Exception {
        List<String> names = new ArrayList<>();
        for (DishProposal proposal : proposals(activity)) names.add(proposal.getName());
        return names;
    }

    /** Collects background work; tests decide when it "finishes". */
    private static final class QueuedExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        int size() { return tasks.size(); }
        void runAll() {
            while (!tasks.isEmpty()) tasks.remove(0).run();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        }
    }

    /**
     * /v1/rate rates every candidate 8/10; /v1/proposals invents brand-new
     * dishes (unique names). The catalogue is already cached.
     */
    private static final class StubTransport implements HttpTransport {
        int rateCalls;
        int proposeCalls;
        int invented;
        boolean fail;
        boolean generateNothing;
        List<String> lastAvoid = new ArrayList<>();
        int calls() { return rateCalls + proposeCalls; }
        public Response get(String url, String token) throws IOException {
            throw new AssertionError("Catalogue is cached; no GET expected: " + url);
        }
        public Response postJson(String url, String token, String body) throws IOException {
            try {
                JSONObject request = new JSONObject(body);
                if (url.endsWith("/v1/proposals")) {
                    proposeCalls++;
                    if (fail) throw new IOException("Simulated network outage");
                    lastAvoid = BackendCodec.strings(request.getJSONObject("request").optJSONArray("recent"));
                    JSONArray proposals = new JSONArray();
                    for (int i = 0; !generateNothing && i < request.getInt("count"); i++) {
                        invented++;
                        proposals.put(BackendCodec.proposal(new DishProposal("Nowe danie " + invented,
                                "Pomysł AI", "20 min", Arrays.asList("warzywa"))));
                    }
                    return new Response(200, BackendCodec.envelope().put("proposals", proposals).toString());
                }
                assertTrue(url.endsWith("/v1/rate"));
                rateCalls++;
                if (fail) throw new IOException("Simulated network outage");
                JSONArray candidates = request.getJSONArray("candidates");
                JSONArray ratings = new JSONArray();
                for (int i = 0; i < candidates.length(); i++) {
                    String name = candidates.getJSONObject(i).getString("name");
                    ratings.put(BackendCodec.rating(new DishRating(name, 8, "Powód: " + name)));
                }
                return new Response(200, BackendCodec.envelope().put("ratings", ratings).toString());
            } catch (JSONException e) { throw new IOException(e); }
        }
        public Response postForm(String url, Map<String, String> form) {
            throw new AssertionError("Valid stored session must not require OAuth requests");
        }
    }
}
