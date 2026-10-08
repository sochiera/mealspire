package com.mealspire.app;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.domain.*;
import com.mealspire.app.storage.BackendCatalogCache;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackendCatalogCacheTest {
    @After public void clearCatalog() { BuiltInRecipes.useCachedCatalog(null); }
    @Test public void storedServerCatalogSurvivesNewCacheInstanceAndInvalidDataFallsBack() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        JSONArray meals=new JSONArray();
        for(int i=0;i<3;i++) meals.put(new JSONArray().put(BackendWire.recipe(new Recipe("Zupa VPS","Składniki: woda"))));
        c.getSharedPreferences("backend_catalog",0).edit().putString("json",new JSONObject().put("meals",meals).toString()).commit();
        new BackendCatalogCache(c).load();
        assertEquals("Zupa VPS",BuiltInRecipes.forMeal(1)[0].getTitle());
        c.getSharedPreferences("backend_catalog",0).edit().putString("json","broken").commit();
        new BackendCatalogCache(c).load();
        assertTrue(BuiltInRecipes.forMeal(1).length>1);
    }
    @Test public void firstRefreshSeedsTimestampWithoutNetwork() {
        Context c=ApplicationProvider.getApplicationContext();
        c.getSharedPreferences("backend_catalog",0).edit().clear().commit();
        new BackendCatalogCache(c).refresh("invalid-url");
        assertTrue(c.getSharedPreferences("backend_catalog",0).getLong("lastAttempt",0)>0);
    }
}
