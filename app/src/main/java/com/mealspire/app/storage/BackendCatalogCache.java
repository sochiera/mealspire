package com.mealspire.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import com.mealspire.app.domain.*;
import com.mealspire.app.net.HttpUrlTransport;
import org.json.*;
/** Last successful catalogue survives process death. Offline reminders never use the network. */
public final class BackendCatalogCache {
    private final SharedPreferences prefs;
    public BackendCatalogCache(Context context) { prefs=context.getSharedPreferences("backend_catalog",Context.MODE_PRIVATE); }
    public void load() {
        try { BuiltInRecipes.useCachedCatalog(BackendWire.catalog(new JSONObject(prefs.getString("json","")))); }
        catch(JSONException e) { BuiltInRecipes.useCachedCatalog(null); }
    }
    public void refresh(String base) {
        long now=System.currentTimeMillis();
        long last=prefs.getLong("lastAttempt",0);
        // First launch seeds the clock, just like APK update checks; avoids startup network bursts.
        if(last==0) { prefs.edit().putLong("lastAttempt",now).apply(); return; }
        if(now-last<86400000L) return;
        prefs.edit().putLong("lastAttempt",now).apply();
        try {
            HttpTransport.Response response=new HttpUrlTransport().get(base+"/v1/catalog",null);
            if(!response.isSuccess()) return;
            Recipe[][] catalog=BackendWire.catalog(new JSONObject(response.body));
            prefs.edit().putString("json",response.body).apply();
            BuiltInRecipes.useCachedCatalog(catalog);
        } catch(Exception ignored) { /* retain last successful cache */ }
    }
}
