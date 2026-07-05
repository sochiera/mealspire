package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Serializacja dziennika gustu do JSON-a. Odporna na null/śmieci i nieznane
 * typy zdarzeń (z nowszych wersji appki) — pomija wpis, nie wywraca dziennika.
 */
public class TasteEventSerializerTest {

    private final TasteEventSerializer serializer = new TasteEventSerializer();

    @Test
    public void dziennikPrzezywaRoundtrip() {
        TasteEventLog original = TasteEventLog.empty()
                .append(new TasteEvent(TasteEvent.Type.LIKED, "Omlet", 0, 123L))
                .append(new TasteEvent(TasteEvent.Type.REROLLED, "Bigos", 2, 456L, true));

        TasteEventLog restored = serializer.fromJson(serializer.toJson(original));

        assertEquals(2, restored.size());
        TasteEvent first = restored.events().get(0);
        assertEquals(TasteEvent.Type.LIKED, first.getType());
        assertEquals("Omlet", first.getDishTitle());
        assertEquals(0, first.getMealIndex());
        assertEquals(123L, first.getTimestamp());
        assertTrue(!first.isExploratory());
        TasteEvent second = restored.events().get(1);
        assertEquals(TasteEvent.Type.REROLLED, second.getType());
        assertTrue(second.isExploratory());
    }

    @Test
    public void nullPusteISmieciDajaPustyDziennik() {
        assertTrue(serializer.fromJson(null).isEmpty());
        assertTrue(serializer.fromJson("").isEmpty());
        assertTrue(serializer.fromJson("nie-json").isEmpty());
    }

    @Test
    public void nieznanyTypZdarzeniaJestPomijany() {
        String json = "{\"events\":[" +
                "{\"t\":\"TELEPORTED\",\"d\":\"Danie\",\"m\":0,\"ts\":1}," +
                "{\"t\":\"LIKED\",\"d\":\"Omlet\",\"m\":1,\"ts\":2}]}";
        TasteEventLog restored = serializer.fromJson(json);
        assertEquals(1, restored.size());
        assertEquals("Omlet", restored.events().get(0).getDishTitle());
    }
}
