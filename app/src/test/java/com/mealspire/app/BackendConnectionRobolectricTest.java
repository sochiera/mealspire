package com.mealspire.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.backend.BackendClient;
import com.mealspire.app.backend.BackendCodec;
import com.mealspire.app.domain.*;
import com.mealspire.app.storage.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackendConnectionRobolectricTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final RecordingTransport transport = new RecordingTransport();

    @Before public void setUp() {
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear().commit();
        new SharedPreferencesChatGptSessionStore(context).clear();
        new SharedPreferencesAppSettings(context).markOnboardingDone();
        MainActivity.backendTransportOverride = transport;
    }
    @After public void tearDown() { MainActivity.backendTransportOverride = null; }

    private void signIn() {
        new SharedPreferencesChatGptSessionStore(context).save(new ChatGptSession(
                "client", "access", "refresh", "id", System.currentTimeMillis()+3_600_000,
                "subject", "email", "gpt-6-luna", "gpt-6.1-sol"));
    }

    @Test public void notificationDoesNotContactBackendBeforeServerNoticeAccepted() throws Exception {
        signIn();
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit()
                .putString("url", "https://old.example").commit();
        Intent intent = new Intent(context, MainActivity.class).putExtra(MainActivity.EXTRA_MEAL_INDEX, 1);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        assertNotNull(activity.findViewById(R.id.accept_button)); // offline proposals already usable
        assertFalse(transport.called.await(250, TimeUnit.MILLISECONDS));
        assertFalse(new SharedPreferencesBackendStore(context).serverNoticeShown());
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertTrue(transport.called.await(5, TimeUnit.SECONDS));
        assertEquals(BackendClient.DEFAULT_BASE_URL + "/v1/catalog", transport.urls.get(0));
    }

    @Test public void signedOutNotificationUsesOfflineWithoutBackendRequests() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class,
                new Intent(context, MainActivity.class).putExtra(MainActivity.EXTRA_MEAL_INDEX, 1)).setup().get();
        assertNotNull(activity.findViewById(R.id.accept_button));
        assertFalse(transport.called.await(250, TimeUnit.MILLISECONDS));
    }

    @Test public void migratedAddressesReachProductionAndNetworkFailureFallsBackOffline() {
        signIn();
        for (String old : new String[]{"", "https://old.example"}) {
            transport.urls.clear();
            context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit()
                    .clear().putString("url", old).commit();
            SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
            ChatGptAccount account = new ChatGptAccount(new SharedPreferencesChatGptSessionStore(context),
                    transport, new ChatGptOAuth(new java.security.SecureRandom()), new IdTokenVerifier(),
                    System::currentTimeMillis);
            List<Recipe> offline = Arrays.asList(store.forMeal(1));
            DishRecommender.Recommendation result = new DishRecommender(new BackendClient(store, account, transport))
                    .recommend(true, "Obiad", DishReactionLog.empty(), offline, DietConstraints.empty(),
                            offline, 3, System.currentTimeMillis());
            assertEquals(Collections.singletonList(BackendClient.DEFAULT_BASE_URL + "/v1/rate"), transport.urls);
            assertTrue(result.isFailed());
            assertFalse(result.getRecipes().isEmpty());
            assertTrue(offline.containsAll(result.getRecipes()));
        }
    }

    @Test public void freshAndUpgradedInstallsCanLoadCatalogAndUseSuccessfulRatings() throws Exception {
        signIn();
        for (String saved : new String[]{null, "", "https://old.example"}) {
            context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit()
                    .clear().putString("url", saved).commit();
            SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
            SuccessfulTransport successful = new SuccessfulTransport();
            ChatGptAccount account = new ChatGptAccount(new SharedPreferencesChatGptSessionStore(context),
                    successful, new ChatGptOAuth(new java.security.SecureRandom()), new IdTokenVerifier(),
                    System::currentTimeMillis);
            BackendClient client = new BackendClient(store, account, successful);
            store.cache(store.baseUrl(), client.catalog());
            List<Recipe> candidates = Arrays.asList(store.forMeal(1));
            DishRecommender.Recommendation result = new DishRecommender(client).recommend(
                    true, "Obiad", DishReactionLog.empty(), candidates, DietConstraints.empty(),
                    Collections.emptyList(), 1, System.currentTimeMillis());

            assertFalse(result.isFailed());
            assertEquals(1, result.getRecipes().size());
            assertEquals("Ryż", result.getRecipes().get(0).getTitle());
            assertEquals(Arrays.asList(BackendClient.DEFAULT_BASE_URL + "/v1/catalog",
                    BackendClient.DEFAULT_BASE_URL + "/v1/rate"), successful.urls);
            assertFalse(new SharedPreferencesBackendStore(context).needsCatalog());
        }
    }

    private static final class SuccessfulTransport implements HttpTransport {
        final List<String> urls = new ArrayList<>();
        public Response get(String url, String token) throws IOException {
            urls.add(url);
            assertNull(token);
            try {
                JSONArray meals = new JSONArray();
                for (int i = 0; i < 3; i++) meals.put(new JSONArray()
                        .put(BackendCodec.recipe(new Recipe("Ryż", "Ugotuj ryż."))));
                return new Response(200, BackendCodec.envelope().put("meals", meals).toString());
            } catch (JSONException e) { throw new IOException(e); }
        }
        public Response postJson(String url, String token, String body) throws IOException {
            urls.add(url);
            assertEquals("access", token);
            try {
                JSONObject request = new JSONObject(body);
                assertEquals("Ryż", request.getJSONArray("candidates").getJSONObject(0).getString("name"));
                return new Response(200, BackendCodec.envelope().put("ratings", new JSONArray()
                        .put(BackendCodec.rating(new DishRating("Ryż", 9, "Pasuje do gustu.")))).toString());
            } catch (JSONException e) { throw new IOException(e); }
        }
        public Response postForm(String url, Map<String,String> form) {
            throw new AssertionError("Valid stored session must not require OAuth requests");
        }
    }

    private static final class RecordingTransport implements HttpTransport {
        final List<String> urls = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch called = new CountDownLatch(1);
        private Response fail(String url) throws IOException {
            urls.add(url); called.countDown(); throw new IOException("Simulated network outage");
        }
        public Response get(String url, String token) throws IOException { return fail(url); }
        public Response postJson(String url, String token, String body) throws IOException { return fail(url); }
        public Response postForm(String url, Map<String,String> form) throws IOException { return fail(url); }
    }
}
