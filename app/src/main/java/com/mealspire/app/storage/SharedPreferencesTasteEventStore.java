package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.FrozenTasteAggregate;
import com.mealspire.app.domain.FrozenTasteAggregateSerializer;
import com.mealspire.app.domain.TasteEventLog;
import com.mealspire.app.domain.TasteEventSerializer;
import com.mealspire.app.domain.TasteEventStore;

/**
 * {@link TasteEventStore} oparty na {@link SharedPreferences} — dziennik gustu
 * przeżywa restarty aplikacji i obrót ekranu, a nie opuszcza telefonu.
 */
public final class SharedPreferencesTasteEventStore implements TasteEventStore {

    private static final String PREFS_NAME = "mealspire_taste_events";
    private static final String KEY_EVENTS = "taste_events_json";
    private static final String KEY_AGGREGATE = "taste_aggregate_json";

    private final SharedPreferences sharedPreferences;
    private final TasteEventSerializer serializer = new TasteEventSerializer();
    private final FrozenTasteAggregateSerializer aggregateSerializer =
            new FrozenTasteAggregateSerializer();

    public SharedPreferencesTasteEventStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public TasteEventLog load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_EVENTS, null));
    }

    @Override
    public void save(TasteEventLog log) {
        sharedPreferences.edit()
                .putString(KEY_EVENTS, serializer.toJson(log))
                .apply();
    }

    @Override
    public FrozenTasteAggregate loadAggregate() {
        return aggregateSerializer.fromJson(
                sharedPreferences.getString(KEY_AGGREGATE, null));
    }

    @Override
    public void saveAggregate(FrozenTasteAggregate aggregate) {
        sharedPreferences.edit()
                .putString(KEY_AGGREGATE, aggregateSerializer.toJson(aggregate))
                .apply();
    }
}
