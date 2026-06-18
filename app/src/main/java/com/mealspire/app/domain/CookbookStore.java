package com.mealspire.app.domain;

/**
 * Persists the user's {@link Cookbook} between launches and across rotation.
 */
public interface CookbookStore {
    Cookbook load();

    void save(Cookbook cookbook);
}
