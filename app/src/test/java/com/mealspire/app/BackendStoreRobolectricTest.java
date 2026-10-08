package com.mealspire.app;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.storage.SharedPreferencesBackendStore;
import com.mealspire.app.backend.BackendCodec;
import com.mealspire.app.domain.Recipe;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackendStoreRobolectricTest {
    @Test public void validatedCatalogSurvivesRestartAndEndpointChangeClearsIt() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE).edit().clear().commit();
        SharedPreferencesBackendStore store = new SharedPreferencesBackendStore(context);
        store.configure("https://trusted.example");
        JSONArray meals = new JSONArray();
        for (int i=0; i<3; i++) meals.put(new JSONArray().put(BackendCodec.recipe(new Recipe("Nowe danie " + i, "Gotuj"))));
        JSONObject catalog = BackendCodec.envelope().put("meals", meals).put("newField", true);
        store.cache(store.baseUrl(), catalog);
        assertEquals("Nowe danie 0", new SharedPreferencesBackendStore(context).forMeal(0)[0].getTitle());
        assertFalse(store.needsCatalog());
        try { store.cache(store.baseUrl(), BackendCodec.envelope().put("meals", new JSONArray())); fail(); }
        catch (java.io.IOException expected) { assertEquals("Nowe danie 0", store.forMeal(0)[0].getTitle()); }
        store.configure("https://different.example");
        store.cache("https://trusted.example", catalog);
        assertNotEquals("Nowe danie 0", store.forMeal(0)[0].getTitle());
        assertTrue(store.needsCatalog());
    }
}
