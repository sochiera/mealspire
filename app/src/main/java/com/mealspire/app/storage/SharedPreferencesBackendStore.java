package com.mealspire.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import com.mealspire.app.backend.*;
import com.mealspire.app.domain.*;
import java.io.IOException;
import org.json.*;
/** Last validated catalog survives outages. Changing endpoint clears consent and old catalog. */
public final class SharedPreferencesBackendStore implements BackendClient.Configuration {
    private final SharedPreferences prefs;
    public SharedPreferencesBackendStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("mealspire_backend", Context.MODE_PRIVATE);
    }
    @Override public String baseUrl() { return prefs.getString("url", ""); }
    public synchronized void configure(String url) {
        String validated = BackendClient.validateUrl(url);
        if (!validated.equals(baseUrl())) prefs.edit().clear().putString("url", validated).apply();
    }
    public boolean needsCatalog() { return !baseUrl().isEmpty() && System.currentTimeMillis()-prefs.getLong("catalog_time", 0) > 86400000; }
    public synchronized void cache(String expectedUrl, JSONObject data) throws IOException {
        try { BackendCodec.catalog(data); } catch (JSONException e) { throw new IOException("Nieprawidłowy katalog.", e); }
        if (expectedUrl.equals(baseUrl())) prefs.edit().putString("catalog", data.toString()).putLong("catalog_time", System.currentTimeMillis()).apply();
    }
    public Recipe[] forMeal(int meal) {
        try { return BackendCodec.catalog(BackendCodec.response(prefs.getString("catalog", "")))[meal]; }
        catch (IOException | JSONException | IndexOutOfBoundsException e) { return BuiltInRecipes.forMeal(meal); }
    }
}
