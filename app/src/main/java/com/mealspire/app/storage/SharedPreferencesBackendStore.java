package com.mealspire.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import com.mealspire.app.backend.*;
import com.mealspire.app.domain.*;
import java.io.IOException;
import org.json.*;
/**
 * Last validated catalog survives outages. The endpoint is fixed (no user-entered address);
 * an empty or different address saved by an older version is replaced and its catalog dropped.
 * Also holds the ready (pre-rated) dish pool per meal, see {@link ReadyDishPool}.
 */
public final class SharedPreferencesBackendStore implements BackendClient.Configuration {
    private final SharedPreferences prefs;
    private final String endpoint;
    public SharedPreferencesBackendStore(Context context) { this(context, BackendClient.DEFAULT_BASE_URL); }
    public SharedPreferencesBackendStore(Context context, String endpoint) {
        prefs = context.getApplicationContext().getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE);
        this.endpoint = BackendClient.validateUrl(endpoint);
        if (!this.endpoint.equals(prefs.getString("url", null))) prefs.edit().clear().putString("url", this.endpoint).apply();
    }
    @Override public String baseUrl() { return endpoint; }
    /** One-time notice that AI goes through the Mealspire server (reset with the endpoint). */
    public boolean serverNoticeShown() { return prefs.getBoolean("server_notice", false); }
    public void markServerNoticeShown() { prefs.edit().putBoolean("server_notice", true).apply(); }
    public boolean needsCatalog() { return System.currentTimeMillis()-prefs.getLong("catalog_time", 0) > 86400000; }
    public synchronized void cache(String expectedUrl, JSONObject data) throws IOException {
        try { BackendCodec.catalog(data); } catch (JSONException e) { throw new IOException("Nieprawidłowy katalog.", e); }
        if (expectedUrl.equals(baseUrl())) prefs.edit().putString("catalog", data.toString()).putLong("catalog_time", System.currentTimeMillis()).apply();
    }
    /**
     * Gotowa pula ocenionych dań posiłku — obok katalogu, bo trzyma tylko nazwy
     * z jego dań (plus książki kucharskiej) i znika razem z nim przy zmianie adresu.
     */
    public synchronized ReadyDishPool readyPool(int meal) {
        return new ReadyDishPoolSerializer().fromJson(prefs.getString("ready_pool_" + meal, null));
    }
    public synchronized void saveReadyPool(int meal, ReadyDishPool pool) {
        prefs.edit().putString("ready_pool_" + meal, new ReadyDishPoolSerializer().toJson(pool)).apply();
    }
    public Recipe[] forMeal(int meal) {
        try { return BackendCodec.catalog(BackendCodec.response(prefs.getString("catalog", "")))[meal]; }
        catch (IOException | JSONException | IndexOutOfBoundsException e) { return BuiltInRecipes.forMeal(meal); }
    }
}
