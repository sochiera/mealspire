package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.LearningStats;
import com.mealspire.app.domain.LearningStatsSerializer;
import com.mealspire.app.domain.LearningStatsStore;

/** {@link LearningStatsStore} oparty na {@link SharedPreferences}. */
public final class SharedPreferencesLearningStatsStore implements LearningStatsStore {

    private static final String PREFS_NAME = "mealspire_learning_stats";
    private static final String KEY_STATS = "learning_stats_json";

    private final SharedPreferences sharedPreferences;
    private final LearningStatsSerializer serializer = new LearningStatsSerializer();

    public SharedPreferencesLearningStatsStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public LearningStats load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_STATS, null));
    }

    @Override
    public void save(LearningStats stats) {
        sharedPreferences.edit()
                .putString(KEY_STATS, serializer.toJson(stats))
                .apply();
    }
}
