package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.HouseholdProfile;
import com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

/**
 * Profil domowników jest trwały: zapis w jednej instancji store'a jest
 * widoczny w świeżej instancji (SharedPreferences).
 */
@RunWith(RobolectricTestRunner.class)
public class HouseholdProfileStoreRobolectricTest {

    private SharedPreferencesHouseholdProfileStore newStore() {
        return new SharedPreferencesHouseholdProfileStore(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void pustyStoreZwracaPustyProfil() {
        assertTrue(newStore().load().isEmpty());
    }

    @Test
    public void zapisanyProfilPrzezywaNowaInstancjeStorea() {
        newStore().save(HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.WITH_CHILDREN)
                .withSkill(HouseholdProfile.CookingSkill.CONFIDENT)
                .withCuisines(Arrays.asList("włoska", "meksykańska")));

        HouseholdProfile restored = newStore().load();
        assertEquals(HouseholdProfile.Audience.WITH_CHILDREN, restored.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.CONFIDENT, restored.getSkill());
        assertEquals(Arrays.asList("włoska", "meksykańska"), restored.getCuisines());
    }
}
