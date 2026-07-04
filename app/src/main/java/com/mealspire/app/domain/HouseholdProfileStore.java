package com.mealspire.app.domain;

/**
 * Trwały magazyn profilu domowników — interfejs domenowy, implementacja
 * (SharedPreferences) mieszka w warstwie {@code storage/}.
 */
public interface HouseholdProfileStore {

    /** Zwraca zapisany profil albo pusty, jeśli niczego nie zapisano. */
    HouseholdProfile load();

    void save(HouseholdProfile profile);
}
