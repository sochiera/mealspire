package com.mealspire.app.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cechy jednego dania w wymiarach gustu. Wymiar nierozpoznany przez słowniki
 * to {@code null} — świadomie „brak obserwacji", a nie fałszywe „neutralne",
 * żeby nie rozwadniać modelu; takie danie nadal pracuje w modelu przez tytuł
 * i tokenowy {@link TasteProfile}.
 */
public final class DishTags {

    private final String base;
    private final String cuisine;
    private final String character;

    public DishTags(String base, String cuisine, String character) {
        this.base = base;
        this.cuisine = cuisine;
        this.character = character;
    }

    public static DishTags none() {
        return new DishTags(null, null, null);
    }

    /** Wartość w wymiarze albo {@code null}, gdy słowniki nie rozpoznały. */
    public String get(TasteDimension dimension) {
        switch (dimension) {
            case BASE:
                return base;
            case CUISINE:
                return cuisine;
            case CHARACTER:
                return character;
            default:
                return null;
        }
    }

    /** Rozpoznane wymiary jako mapa (bez {@code null}-i). */
    public Map<TasteDimension, String> asMap() {
        Map<TasteDimension, String> map = new EnumMap<>(TasteDimension.class);
        for (TasteDimension dimension : TasteDimension.values()) {
            String value = get(dimension);
            if (value != null) {
                map.put(dimension, value);
            }
        }
        return map;
    }

    public boolean isEmpty() {
        return base == null && cuisine == null && character == null;
    }
}
