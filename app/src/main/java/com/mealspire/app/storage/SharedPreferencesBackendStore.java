package com.mealspire.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import com.mealspire.app.backend.*;
import com.mealspire.app.domain.*;
import java.io.IOException;
import org.json.*;
/**
 * Last validated catalog survives outages. The endpoint is fixed (no user-entered address);
 * an empty or different address saved by an older version is replaced and its catalog dropped,
 * and so is a catalog cached under another {@link BuiltInRecipes#NAMES_REVISION}.
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
        // Catalog cached before a dish rename would keep old names (also with no session to refresh it):
        // drop only the catalog; forMeal falls back to the built-in pool until the next validated fetch.
        if (prefs.getInt("catalog_revision", 0) != BuiltInRecipes.NAMES_REVISION)
            prefs.edit().remove("catalog").remove("catalog_time").remove("catalog_names").putInt("catalog_revision", BuiltInRecipes.NAMES_REVISION).apply();
    }
    @Override public String baseUrl() { return endpoint; }
    /** One-time notice that AI goes through the Mealspire server (reset with the endpoint). */
    public boolean serverNoticeShown() { return prefs.getBoolean("server_notice", false); }
    public void markServerNoticeShown() { prefs.edit().putBoolean("server_notice", true).apply(); }
    public boolean needsCatalog() { return System.currentTimeMillis()-prefs.getLong("catalog_time", 0) > 86400000; }
    /** Shared by all instances (same preferences file): revision check and write are one step. */
    private static final Object CATALOG_LOCK = new Object();
    public void cache(String expectedUrl, JSONObject data) throws IOException {
        try { BackendCodec.catalog(data); } catch (JSONException e) { throw new IOException("Nieprawidłowy katalog.", e); }
        synchronized (CATALOG_LOCK) { cacheChecked(expectedUrl, data); }
    }
    private void cacheChecked(String expectedUrl, JSONObject data) throws IOException {
        // A server older than this build's dish names (e.g. a backend rollback) would bring old names back:
        // keep the current cache or built-in pool instead.
        // Nor older than the catalog already cached (a newer server catalog survives a later rollback).
        int names = data.optInt("namesRevision", 0);
        if (names < Math.max(BuiltInRecipes.NAMES_REVISION, prefs.getInt("catalog_names", 0))) throw new IOException("Nieaktualny katalog serwera.");
        if (expectedUrl.equals(baseUrl())) prefs.edit().putString("catalog", data.toString()).putLong("catalog_time", System.currentTimeMillis())
                .putInt("catalog_revision", BuiltInRecipes.NAMES_REVISION).putInt("catalog_names", names).apply();
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
    /** All meals from one decode of the cached catalog (built-in pool when there is none). */
    public Recipe[][] catalog(int meals) {
        Recipe[][] result = new Recipe[meals][];
        try {
            Recipe[][] cached = BackendCodec.catalog(BackendCodec.response(prefs.getString("catalog", "")));
            for (int i = 0; i < meals; i++) result[i] = cached[i];
        } catch (IOException | JSONException | IndexOutOfBoundsException e) {
            for (int i = 0; i < meals; i++) result[i] = BuiltInRecipes.forMeal(i);
        }
        return result;
    }
}
