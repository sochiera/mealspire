package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.HouseholdProfile;
import com.mealspire.app.domain.HouseholdProfileSerializer;
import com.mealspire.app.domain.HouseholdProfileStore;

/**
 * {@link HouseholdProfileStore} oparty na {@link SharedPreferences}, żeby
 * odpowiedzi z onboardingu przeżywały restarty aplikacji i obrót ekranu.
 */
public final class SharedPreferencesHouseholdProfileStore implements HouseholdProfileStore {

    private static final String PREFS_NAME = "mealspire_household";
    private static final String KEY_PROFILE = "household_profile_json";

    private final SharedPreferences sharedPreferences;
    private final HouseholdProfileSerializer serializer = new HouseholdProfileSerializer();

    public SharedPreferencesHouseholdProfileStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public HouseholdProfile load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_PROFILE, null));
    }

    @Override
    public void save(HouseholdProfile profile) {
        sharedPreferences.edit()
                .putString(KEY_PROFILE, serializer.toJson(profile))
                .apply();
    }
}
