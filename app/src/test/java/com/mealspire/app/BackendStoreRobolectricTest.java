package com.mealspire.app;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.storage.SharedPreferencesBackendStore;
import com.mealspire.app.backend.BackendClient;
import com.mealspire.app.backend.BackendCodec;
import com.mealspire.app.domain.BuiltInRecipes;
import com.mealspire.app.domain.Recipe;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackendStoreRobolectricTest {
    private Context context;
    @Before public void clearPrefs() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear().commit();
    }
    private JSONObject catalog() throws JSONException {
        JSONArray meals = new JSONArray();
        for (int i=0; i<3; i++) meals.put(new JSONArray().put(BackendCodec.recipe(new Recipe("Nowe danie " + i, "Gotuj"))));
        return BackendCodec.envelope().put("meals", meals).put("newField", true);
    }
    @Test public void validatedCatalogSurvivesRestartAndEndpointChangeClearsIt() throws Exception {
        SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context, "https://trusted.example");
        JSONObject catalog = catalog();
        store.cache(store.baseUrl(), catalog);
        assertEquals("Nowe danie 0", new SharedPreferencesBackendStore(context, "https://trusted.example").forMeal(0)[0].getTitle());
        assertFalse(store.needsCatalog());
        try { store.cache(store.baseUrl(), BackendCodec.envelope().put("meals", new JSONArray())); fail(); }
        catch (java.io.IOException expected) { assertEquals("Nowe danie 0", store.forMeal(0)[0].getTitle()); }
        store = new SharedPreferencesBackendStore(context, "https://different.example");
        store.cache("https://trusted.example", catalog);
        assertNotEquals("Nowe danie 0", store.forMeal(0)[0].getTitle());
        assertTrue(store.needsCatalog());
    }
    @Test public void freshInstallUsesProductionServerWithoutAsking() {
        SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
        assertEquals(BackendClient.DEFAULT_BASE_URL, store.baseUrl());
        assertEquals("https://sochiera.pl/mealspire-api", BackendClient.validateUrl(store.baseUrl()));
        assertTrue(store.needsCatalog());
    }
    @Test public void savedEmptyOrOldAddressFromEarlierVersionIsReplacedByDefault() throws Exception {
        for (String saved : new String[]{"", "https://old-server.example"}) {
            context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear()
                    .putString("url", saved).putString("catalog", catalog().toString())
                    .putLong("catalog_time", System.currentTimeMillis()).commit();
            SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
            assertEquals(BackendClient.DEFAULT_BASE_URL, store.baseUrl());
            assertEquals(BuiltInRecipes.forMeal(0)[0].getTitle(), store.forMeal(0)[0].getTitle());
            assertTrue(store.needsCatalog());
        }
    }
    @Test public void catalogOfDefaultServerSurvivesUpgradeRestart() throws Exception {
        new SharedPreferencesBackendStore(context).cache(BackendClient.DEFAULT_BASE_URL, catalog());
        SharedPreferencesBackendStore restarted = new SharedPreferencesBackendStore(context);
        assertEquals("Nowe danie 0", restarted.forMeal(0)[0].getTitle());
        assertFalse(restarted.needsCatalog());
    }
}
