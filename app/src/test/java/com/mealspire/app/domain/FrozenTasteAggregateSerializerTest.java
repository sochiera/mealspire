package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializacja zamrożonego agregatu gustu: roundtrip, odporność na śmieci
 * i porzucanie agregatu z inną wersją schematu.
 */
public class FrozenTasteAggregateSerializerTest {

    private final FrozenTasteAggregateSerializer serializer =
            new FrozenTasteAggregateSerializer();

    @Test
    public void agregatPrzezywaRoundtrip() {
        Map<String, Double> scores = new HashMap<>();
        scores.put(FrozenTasteAggregate.scoreKey(TasteDimension.BASE, -1, "ryba"), 4.5);
        Map<String, Integer> observations = new HashMap<>();
        observations.put(FrozenTasteAggregate.observationKey(TasteDimension.BASE, -1), 3);
        FrozenTasteAggregate original = new FrozenTasteAggregate(
                123456L, scores, observations, 9.0);

        FrozenTasteAggregate restored = serializer.fromJson(serializer.toJson(original));

        assertEquals(123456L, restored.getAsOf());
        assertEquals(4.5, restored.getScores().get(
                FrozenTasteAggregate.scoreKey(TasteDimension.BASE, -1, "ryba")), 0.001);
        assertEquals(Integer.valueOf(3), restored.getObservations().get(
                FrozenTasteAggregate.observationKey(TasteDimension.BASE, -1)));
        assertEquals(9.0, restored.getExplicitPositiveMass(), 0.001);
    }

    @Test
    public void nullSmieciIPustyDajaPustyAgregat() {
        assertTrue(serializer.fromJson(null).isEmpty());
        assertTrue(serializer.fromJson("").isEmpty());
        assertTrue(serializer.fromJson("nie-json").isEmpty());
        assertTrue(serializer.fromJson(
                serializer.toJson(FrozenTasteAggregate.empty())).isEmpty());
    }

    @Test
    public void innaWersjaSchematuJestPorzucana() {
        String json = "{\"v\":999,\"asOf\":5,\"scores\":{\"BASE|-1|ryba\":4.0},"
                + "\"obs\":{},\"explicitMass\":3.0}";
        assertTrue(serializer.fromJson(json).isEmpty());
    }
}
