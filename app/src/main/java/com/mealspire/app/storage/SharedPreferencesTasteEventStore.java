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
        // Zwykły zapis asynchronicznie (bez blokowania UI); ankieta używa saveDurably.
        sharedPreferences.edit()
                .putString(KEY_EVENTS, serializer.toJson(log))
                .apply();
    }

    @Override
    public boolean saveDurably(TasteEventLog log) {
        // commit(): zapis wyborów z ankiety zatwierdza ankietę dopiero po utrwaleniu
        // tego pliku — kolejka apply() mogłaby zginąć razem z procesem.
        return sharedPreferences.edit()
                .putString(KEY_EVENTS, serializer.toJson(log))
                .commit();
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

    @Override
    public void save(TasteEventLog log, FrozenTasteAggregate aggregate) {
        withAggregate(log, aggregate).apply();
    }

    @Override
    public boolean saveDurably(TasteEventLog log, FrozenTasteAggregate aggregate) {
        return withAggregate(log, aggregate).commit();
    }

    /** Jedna transakcja: po kompakcji dziennik i agregat nie mogą się rozjechać. */
    private SharedPreferences.Editor withAggregate(TasteEventLog log,
                                                   FrozenTasteAggregate aggregate) {
        return sharedPreferences.edit()
                .putString(KEY_EVENTS, serializer.toJson(log))
                .putString(KEY_AGGREGATE, aggregateSerializer.toJson(aggregate));
    }
}
