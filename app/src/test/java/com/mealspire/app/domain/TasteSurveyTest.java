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

    private static TasteSurvey longSurvey(int size) {
        List<TasteSurvey.Pair> pairs = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            pairs.add(new TasteSurvey.Pair("A" + i, "B" + i, i % 3));
        }
        return TasteSurvey.start(pairs, "[]");
    }

    private static TasteSurvey answered(TasteSurvey survey, int count) {
        for (int i = 0; i < count; i++) {
            survey = survey.answer(i % 2 == 0 ? TasteSurvey.CHOICE_A : TasteSurvey.CHOICE_B);
        }
        return survey;
    }

    @Test
    public void pierwszaSeriaMa20ParIPostepLiczyDoKoncaSerii() {
        assertEquals(20, TasteSurvey.SERIES_PAIRS);
        TasteSurvey survey = longSurvey(80);
        assertEquals(20, survey.seriesEnd());
        assertEquals(20, answered(survey, 19).seriesEnd());
        assertEquals("krótsza ankieta kończy serię na swoim końcu", 5, longSurvey(5).seriesEnd());
    }

    @Test
    public void po20OdpowiedziachAnkietaCzekaNaDecyzjeDalejAlboWystarczy() {
        TasteSurvey survey = answered(longSurvey(80), 19);
        assertFalse(survey.awaitsDecision());
        survey = survey.answer(TasteSurvey.CHOICE_A);
        assertTrue(survey.awaitsDecision());
        assertEquals(20, survey.position());
        assertFalse(survey.isFinished());
    }

    @Test
    public void dalejZachowujeOdpowiedziIOtwieraKolejnaSerie() {
        TasteSurvey atCheckpoint = answered(longSurvey(80), 20);
        TasteSurvey next = atCheckpoint.continueSeries();

        assertFalse(next.awaitsDecision());
        assertEquals(20, next.position());
        assertEquals(atCheckpoint.answers(), next.answers());
        assertEquals(atCheckpoint.pendingPicks(), next.pendingPicks());
        assertEquals(40, next.seriesEnd());
        assertTrue("kolejna seria też kończy się decyzją",
                answered(next, 20).awaitsDecision());
    }

    @Test
    public void naRazieWystarczyKonczyAnkieteZZebranymiOdpowiedziami() {
        TasteSurvey atCheckpoint = answered(longSurvey(80), 20);
        TasteSurvey done = atCheckpoint.finishEarly();

        assertTrue(done.isFinished());
        assertFalse(done.awaitsDecision());
        assertEquals(20, done.size());
        assertEquals(atCheckpoint.pendingPicks(), done.pendingPicks());
        assertEquals(20, done.pendingPicks().size());
    }

    @Test
    public void zakonczeniePrzed20PytaniemZachowujeOdpowiedzi() {
        TasteSurvey done = answered(longSurvey(80), 7).finishEarly();
        assertTrue(done.isFinished());
        assertEquals(7, done.size());
        assertEquals(7, done.likedDishes().size());
    }

    @Test
    public void ostatniaParaNieCzekaNaDecyzje() {
        assertFalse(answered(longSurvey(20), 20).awaitsDecision());
        assertTrue(answered(longSurvey(20), 20).isFinished());
    }

    @Test
    public void decyzjaPoSeriiPrzezywaZapis() {
        TasteSurveySerializer serializer = new TasteSurveySerializer();
        TasteSurvey continued = answered(longSurvey(40), 20).continueSeries();
        TasteSurvey restored = serializer.fromJson(serializer.toJson(continued));
        assertFalse(restored.awaitsDecision());
        TasteSurvey waiting = serializer.fromJson(serializer.toJson(answered(longSurvey(40), 20)));
        assertTrue(waiting.awaitsDecision());
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
    public void paraNaKursorzePoCofnijZeStaraNazwaOdpadaBezUtratyWyboru() {
        // Przed aktualizacją: odpowiedź, „Cofnij", zamknięcie aplikacji. Kursor stoi
        // na odpowiedzianej parze ze starą nazwą — ta odpowiedź nie jest wyborem.
        TasteSurvey back = TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół"),
                new TasteSurvey.Pair("Rosół", "Pierogi ruskie")), "[]")
                .answer(TasteSurvey.CHOICE_A).back();
        assertTrue(back.pendingPicks().isEmpty());

        TasteSurvey pruned = back.withoutUnknownDishes(Arrays.asList("Rosół", "Pierogi ruskie"));

        assertEquals(1, pruned.size());
        assertEquals("Pierogi ruskie", pruned.current().getDishB());
        assertTrue(pruned.pendingPicks().isEmpty());
    }

    @Test
    public void odpowiedzPrzedPozycjaPoCofnijLiczySieJakoOdpowiedz() {
        TasteSurvey back = survey().answer(TasteSurvey.CHOICE_B).back();
        assertEquals(0, back.position());
        assertTrue(back.hasAnswers());
        assertFalse(survey().hasAnswers());
    }

    @Test
    public void cofnijNieWracaNaPareSpozaKataloguAniSprzecznaZDieta() {
        Map<String, String> catalog = new HashMap<>();
        catalog.put("Rosół", "Składniki: kurczak, marchew.");
        catalog.put("Pierogi ruskie", "Składniki: ziemniaki, twaróg, masło.");
        catalog.put("Sałatka grecka", "Składniki: pomidor, ogórek, oliwki.");
        DietConstraints none = DietConstraints.empty();
        DietConstraints noLactose = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.NO_LACTOSE));

        TasteSurvey stale = TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Pierogi z serem i ziemniakami", "Rosół"),
                new TasteSurvey.Pair("Rosół", "Sałatka grecka")), "[]").answer(TasteSurvey.CHOICE_A);
        assertFalse("stara nazwa nie wraca", stale.canGoBack(none, catalog));

        TasteSurvey dairy = TasteSurvey.start(Arrays.asList(
                new TasteSurvey.Pair("Pierogi ruskie", "Rosół"),
                new TasteSurvey.Pair("Rosół", "Sałatka grecka")), "[]").answer(TasteSurvey.CHOICE_A);
        assertTrue(dairy.canGoBack(none, catalog));
        assertFalse("para łamiąca dietę nie wraca", dairy.canGoBack(noLactose, catalog));
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

    @Test
    public void ponownaAkceptacjaWczesniejszegoCheckpointuNieCofaNajdalszego() {
        TasteSurvey survey = answered(answered(longSurvey(60), 20).continueSeries(), 20)
                .continueSeries();
        assertEquals(40, survey.continuedAt());
        survey = survey.answer(TasteSurvey.CHOICE_A);
        for (int i = 0; i < 22; i++) {
            survey = survey.back();
        }
        assertEquals(19, survey.position());
        survey = survey.answer(TasteSurvey.CHOICE_B);
        assertFalse("checkpoint przed najdalszym zaakceptowanym nie pyta ponownie",
                survey.awaitsDecision());
        survey = survey.continueSeries(); // ponowna akceptacja wcześniejszego
        assertEquals("najdalszy zaakceptowany checkpoint zostaje", 40, survey.continuedAt());
        survey = answered(survey, 20);
        assertEquals(40, survey.position());
        assertFalse("przy 40 „Dalej” już było", survey.awaitsDecision());
    }

    @Test
    public void usuniecieParSpozaKataloguZachowujeDecyzjeDalej() {
        List<TasteSurvey.Pair> pairs = new java.util.ArrayList<>();
        List<String> catalog = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            pairs.add(new TasteSurvey.Pair("A" + i, "B" + i, i % 3));
            if (i != 30) {
                catalog.add("A" + i);
                catalog.add("B" + i);
            }
        }
        TasteSurvey continued = answered(TasteSurvey.start(pairs, "[]"), 20).continueSeries();
        TasteSurvey pruned = continued.withoutUnknownDishes(catalog);
        assertEquals(39, pruned.size());
        assertEquals(20, pruned.continuedAt());
        assertFalse(pruned.awaitsDecision());
    }

    @Test
    public void postepJestLokalnyDlaBiezacejSerii() {
        TasteSurvey second = answered(longSurvey(30), 20).continueSeries();
        assertEquals(0, second.seriesPosition());
        assertEquals("ostatnia seria krótsza", 10, second.seriesLength());
        assertEquals(3, answered(second, 3).seriesPosition());
        assertEquals(19, answered(longSurvey(80), 19).seriesPosition());
        assertEquals(20, answered(longSurvey(80), 19).seriesLength());
    }

    @Test
    public void rozpoczetyZapisPrzetrwaSerializacjeIZnikaPoZatwierdzeniu() {
        TasteSurveySerializer serializer = new TasteSurveySerializer();
        TasteSurvey committing = answered(longSurvey(40), 22).finishEarly().beginCommit(123L);
        assertTrue(committing.needsCommitRecovery());
        assertEquals("ponowienie zachowuje znacznik", 123L,
                committing.beginCommit(999L).commitStamp());
        TasteSurvey restored = serializer.fromJson(serializer.toJson(committing));
        assertEquals(123L, restored.commitStamp());
        assertEquals(committing.pendingPicks(), restored.pendingPicks());
        TasteSurvey done = restored.commit();
        assertFalse(done.isCommitting());
        assertFalse(done.needsCommitRecovery());
        assertEquals(22, done.committed());
        assertFalse("niezakończona ankieta bez zapisu w toku nie wymaga odzyskania",
                answered(longSurvey(40), 5).needsCommitRecovery());
        assertTrue("zakończona z niezapisanymi wyborami wymaga",
                answered(longSurvey(40), 5).finishEarly().needsCommitRecovery());
    }

    @Test
    public void brakujaceWyboryLiczoneJakoMultizbior() {
        assertEquals(Arrays.asList("B", "A"), TasteSurvey.missingFrom(
                Arrays.asList("A", "B", "A", "C"), Arrays.asList("C", "A")).subList(0, 2));
        assertEquals(Collections.emptyList(), TasteSurvey.missingFrom(
                Arrays.asList("A", "B"), Arrays.asList("B", "A")));
    }

    @Test
    public void wTrakcieNiedokonczonegoZapisuOdpowiedziNieDaSieZmienic() {
        TasteSurvey committing = answered(longSurvey(40), 5).beginCommit(7L);
        assertFalse("partia w zapisie jest tylko do odczytu", committing.canGoBack());
        assertTrue(committing.back() == committing);
        assertTrue("po zatwierdzeniu i nowej odpowiedzi „Cofnij” znów działa",
                committing.commit().answer(TasteSurvey.CHOICE_A).canGoBack());
    }
}
