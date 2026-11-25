package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasteSurveyTest {

    private static TasteSurvey survey() {
        return TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Rosół", "Sałatka grecka"),
                new TasteSurvey.Pair("Kotlet schabowy", "Curry z ciecierzycą"),
                new TasteSurvey.Pair("Rosół", "Pad thai")), "[]");
    }

    @Test
    public void postepIdzieParaPoParzeDoKonca() {
        TasteSurvey survey = survey();
        assertEquals(0, survey.position());
        assertEquals(3, survey.size());
        assertEquals("Rosół", survey.current().getDishA());

        survey = survey.answer(TasteSurvey.CHOICE_B)
                .answer(TasteSurvey.CHOICE_NEITHER)
                .answer(TasteSurvey.CHOICE_A);

        assertTrue(survey.isFinished());
        assertNull(survey.current());
        assertEquals(Arrays.asList("Sałatka grecka", "Rosół"), survey.pendingPicks());
    }

    @Test
    public void cofnijPodmieniaWyborZamiastGoDokladac() {
        TasteSurvey survey = survey().answer(TasteSurvey.CHOICE_A).back()
                .answer(TasteSurvey.CHOICE_B);
        assertEquals(Collections.singletonList("Sałatka grecka"), survey.pendingPicks());
        assertFalse(survey().canGoBack());
    }

    @Test
    public void zapisaneOdpowiedziSaOstateczne() {
        TasteSurvey survey = survey().answer(TasteSurvey.CHOICE_A).commit();
        assertTrue(survey.pendingPicks().isEmpty());
        assertFalse(survey.canGoBack());
        assertEquals(survey, survey.back());

        survey = survey.answer(TasteSurvey.CHOICE_B);
        assertEquals(Collections.singletonList("Curry z ciecierzycą"), survey.pendingPicks());
        assertTrue(survey.canGoBack());
    }

    @Test
    public void grupaLubianychDanMaKazdeDanieRaz() {
        TasteSurvey survey = survey().answer(TasteSurvey.CHOICE_A)
                .answer(TasteSurvey.CHOICE_B).commit().answer(TasteSurvey.CHOICE_A);
        assertEquals(Arrays.asList("Rosół", "Curry z ciecierzycą"), survey.likedDishes());
    }

    @Test
    public void zmianaDietyUsuwaTylkoNieprzejrzanePary() {
        DietConstraints veg = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.VEGETARIAN));
        Map<String, String> details = new HashMap<>();
        details.put("Kotlet schabowy", "Składniki: schab, jajko.");
        details.put("Rosół", "Składniki: kurczak, marchew.");
        TasteSurvey survey = survey().answer(TasteSurvey.CHOICE_A).commit()
                .withoutDisallowed(veg, details, "[VEGETARIAN]");

        // Para 1 (z rosołem) jest już przejrzana i zostaje; dwie dalsze mają mięso.
        assertEquals(1, survey.size());
        assertTrue(survey.isFinished());
        assertEquals("[VEGETARIAN]", survey.dietKey());
    }

    @Test
    public void serializacjaZachowujeStanDoWznowienia() {
        TasteSurvey survey = survey().answer(TasteSurvey.CHOICE_A).commit()
                .answer(TasteSurvey.CHOICE_NEITHER);
        TasteSurveySerializer serializer = new TasteSurveySerializer();

        TasteSurvey restored = serializer.fromJson(serializer.toJson(survey));

        assertEquals(3, restored.size());
        assertEquals(2, restored.position());
        assertEquals(1, restored.committed());
        assertEquals(survey.answers(), restored.answers());
        assertEquals("Pad thai", restored.current().getDishB());
        assertEquals("[]", restored.dietKey());
        assertNull(serializer.fromJson("{zepsute"));
        assertNull(serializer.fromJson(null));
    }

    @Test
    public void niespojneDaneSaPrzycinaneDoPoprawnegoStanu() {
        List<TasteSurvey.Pair> pairs = Collections.singletonList(new TasteSurvey.Pair("A", "B"));
        TasteSurvey survey = new TasteSurvey(pairs, Arrays.asList(7, 1), 5, 9, null);
        assertEquals(1, survey.position());
        assertEquals(1, survey.committed());
        assertEquals(Collections.singletonList(TasteSurvey.NOT_ANSWERED), survey.answers());
    }

    @Test
    public void paryZDaniemSprzedAktualizacjiOdpadajaPrzejrzaneZostaja() {
        // Ankieta zapisana przed zmianą nazw: druga, nieprzejrzana para ma starą nazwę.
        TasteSurvey saved = TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół"),
                new TasteSurvey.Pair("Kanapki z pieczarkami duszonymi", "Rosół"),
                new TasteSurvey.Pair("Rosół", "Pierogi ruskie")), "[]")
                .answer(TasteSurvey.CHOICE_A).commit();
        List<String> catalog = Arrays.asList("Rosół", "Pierogi ruskie",
                "Kanapki z duszonymi pieczarkami");

        TasteSurvey pruned = saved.withoutUnknownDishes(catalog);

        assertEquals(2, pruned.size());
        assertEquals("przejrzana para zostaje pod nazwą z odpowiedzi",
                "Pierogi z serem i ziemniakami", pruned.pairs().get(0).getDishA());
        assertEquals("Pierogi ruskie", pruned.current().getDishB());
        assertEquals(1, pruned.committed());
        assertTrue("bez zmian ta sama instancja",
                pruned.withoutUnknownDishes(catalog) == pruned);
    }

    @Test
    public void cofnijNaOdpowiedzianaPareZeStaraNazwaJejNieUsuwa() {
        // Odpowiedź sprzed aktualizacji, niezapisana; użytkownik cofa się na tę parę.
        TasteSurvey back = TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół"),
                new TasteSurvey.Pair("Rosół", "Pierogi ruskie")), "[]")
                .answer(TasteSurvey.CHOICE_A).back();

        TasteSurvey pruned = back.withoutUnknownDishes(Arrays.asList("Rosół", "Pierogi ruskie"));

        assertTrue("nic nie odpada", pruned == back);
        assertEquals(TasteSurvey.CHOICE_A, (int) pruned.answers().get(0));
    }

    @Test
    public void odpowiedzPrzedPozycjaPoCofnijLiczySieJakoOdpowiedz() {
        TasteSurvey back = survey().answer(TasteSurvey.CHOICE_B).back();
        assertEquals(0, back.position());
        assertTrue(back.hasAnswers());
        assertFalse(survey().hasAnswers());
    }

    @Test
    public void danieBezSkladnikowNiePrzechodziPrzezWykluczenieDiety() {
        DietConstraints noLactose = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.NO_LACTOSE));
        Map<String, String> details = new HashMap<>();
        details.put("Rosół", "Składniki: kurczak, marchew.");
        // Stara nazwa nie ma wpisu w katalogu — nie wiemy, że jest w niej masło.
        TasteSurvey survey = TasteSurvey.start(Collections.singletonList(
                new TasteSurvey.Pair("Kanapki z pieczarkami duszonymi", "Rosół")), "[]")
                .withoutDisallowed(noLactose, details, "[NO_LACTOSE]");

        assertEquals(0, survey.size());
        assertEquals("bez wykluczeń nieznane danie nie jest blokowane", 1,
                TasteSurvey.start(Collections.singletonList(
                        new TasteSurvey.Pair("Nieznane", "Rosół")), "[]")
                        .withoutDisallowed(DietConstraints.empty(), details, "[]").size());
    }
}
