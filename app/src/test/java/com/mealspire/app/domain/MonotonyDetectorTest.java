package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Anty-monotonia: dominująca baza w ostatnich propozycjach daje zdanie
 * „unikaj tej bazy"; różnorodna historia i stare propozycje poza oknem — nie.
 */
public class MonotonyDetectorTest {

    private final MonotonyDetector detector = new MonotonyDetector();

    @Test
    public void dominujacaBazaDajeZdanieUnikania() {
        List<String> recent = Arrays.asList("Kurczak z ryżem", "Kurczak pieczony",
                "Kurczak w curry", "Kurczak po tajsku", "Zupa pomidorowa");
        String sentence = detector.detect(recent, null);
        assertTrue(sentence.contains("kurczak"));
        assertTrue(sentence.contains("unikaj"));
    }

    @Test
    public void roznorodnaHistoriaNieDajeZdania() {
        List<String> recent = Arrays.asList("Kurczak z ryżem", "Zupa pomidorowa",
                "Omlet", "Łosoś z pieca", "Leczo z cieciorką");
        assertEquals("", detector.detect(recent, null));
    }

    @Test
    public void liczaSieTylkoOstatniePropozycjeZOkna() {
        // Trzy kurczaki w oknie, czwarty poza nim — poniżej progu.
        String[] titles = new String[MonotonyDetector.WINDOW + 1];
        titles[0] = "Kurczak raz";
        titles[1] = "Kurczak dwa";
        titles[2] = "Kurczak trzy";
        for (int i = 3; i < MonotonyDetector.WINDOW; i++) {
            titles[i] = "Danie bez tagu " + i;
        }
        titles[MonotonyDetector.WINDOW] = "Kurczak cztery (za stary)";
        assertEquals("", detector.detect(Arrays.asList(titles), null));
    }

    @Test
    public void pusteWejscieNieWywracaDetekcji() {
        assertEquals("", detector.detect(null, null));
        assertEquals("", detector.detect(Collections.<String>emptyList(), null));
    }
}
