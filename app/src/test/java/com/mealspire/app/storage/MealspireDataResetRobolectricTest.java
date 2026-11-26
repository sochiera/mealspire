package com.mealspire.app.storage;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.domain.GptModel;
import java.lang.reflect.Proxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MealspireDataResetRobolectricTest {
    private final Context app = ApplicationProvider.getApplicationContext();
    private static final String[] DATA_STORES = {
            "mealspire_settings", "mealspire_cookbook", "mealspire_reactions",
            "mealspire_food_log", "mealspire_household", "mealspire_learning_stats",
            "mealspire_history", "mealspire_preferences", "mealspire_taste_events",
            "mealspire_taste_survey"
    };

    @Test public void clearsAllAppStoresOnlyInTheCurrentProfile() {
        Context other = new ContextWrapper(app) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("other_profile_" + name, mode);
            }
        };
        seed(app);
        seed(other);
        SharedPreferencesChatGptSessionStore account = new SharedPreferencesChatGptSessionStore(app);
        account.saveModelChoice(GptModel.LUNA);
        SharedPreferences backend = prefs(app, "mealspire_backend");
        new SharedPreferencesBackendStore(app).markServerNoticeShown();
        backend.edit().putString("catalog", "test catalog").putString("ready_pool_1", "test pool").commit();

        assertTrue(MealspireDataReset.reset(app));

        for (String name : DATA_STORES) {
            assertTrue(name, prefs(app, name).getAll().isEmpty());
            assertEquals(name, "test data", prefs(other, name).getString("test", null));
        }
        assertEquals(GptModel.DEFAULT, account.modelChoice());
        assertFalse(backend.contains("catalog"));
        assertFalse(backend.contains("ready_pool_1"));
        assertTrue(new SharedPreferencesBackendStore(app).serverNoticeShown());
        assertEquals(0, MealspireDataReset.generation(other));
        assertFalse(MealspireDataReset.isPending(app));
    }

    @Test public void failedCommitLeavesResetPendingAndRetryCompletesIt() {
        seed(app);
        Context failing = new ContextWrapper(app) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                SharedPreferences store = super.getSharedPreferences(name, mode);
                if (!name.equals("mealspire_cookbook")) return store;
                return (SharedPreferences) Proxy.newProxyInstance(
                        SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
                        (proxy, method, args) -> method.getName().equals("edit")
                                ? failingEditor() : method.invoke(store, args));
            }
        };

        assertFalse(MealspireDataReset.reset(failing));
        assertTrue(MealspireDataReset.isPending(app));
        assertEquals("test data", prefs(app, "mealspire_cookbook").getString("test", null));
        assertEquals(1, MealspireDataReset.generation(app));

        assertTrue(MealspireDataReset.reset(app));
        assertFalse(MealspireDataReset.isPending(app));
        assertEquals(1, MealspireDataReset.generation(app));
        for (String name : DATA_STORES) {
            assertTrue(name, prefs(app, name).getAll().isEmpty());
        }
    }

    private static SharedPreferences.Editor failingEditor() {
        return (SharedPreferences.Editor) Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class},
                (proxy, method, args) -> method.getName().equals("commit") ? false : proxy);
    }

    private static void seed(Context context) {
        for (String name : DATA_STORES) {
            assertTrue(prefs(context, name).edit().putString("test", "test data").commit());
        }
    }

    private static SharedPreferences prefs(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
}
