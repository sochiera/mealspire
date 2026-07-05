package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Kompakcja dziennika gustu: nadmiar zwijany do zamrożonego agregatu tak, że
 * model z (przycięty dziennik + agregat) wychodzi identyczny jak z pełnego
 * dziennika — wygaszanie wykładnicze składa się dokładnie.
 */
public class TasteEventCompactorTest {

    private static final long DAY = 24 * 60 * 60 * 1000L;
    private static final long NOW = 1000 * DAY;

    private final DishTagger tagger = new DishTagger();
    private final TasteEventCompactor compactor = new TasteEventCompactor();

    private static TasteEventLog logOf(int count) {
        List<TasteEvent> events = new ArrayList<>();
        String[] dishes = {"Kurczak z ryżem", "Łosoś pieczony", "Omlet z serem",
                "Zupa pomidorowa", "Pierogi z serem"};
        for (int i = 0; i < count; i++) {
            events.add(new TasteEvent(
                    i % 4 == 3 ? TasteEvent.Type.RECIPE_VIEWED : TasteEvent.Type.LIKED,
                    dishes[i % dishes.length], i % 3, NOW - (count - i) * DAY));
        }
        return new TasteEventLog(events);
    }

    @Test
    public void ponizejLimituNicSieNieDzieje() {
        TasteEventLog log = logOf(5);
        TasteEventCompactor.Result result = compactor.compact(
                log, FrozenTasteAggregate.empty(), tagger, null, NOW, 10);
        assertSame(log, result.getLog());
        assertTrue(result.getAggregate().isEmpty());
    }

    @Test
    public void modelPoKompakcjiJestRownowaznyModelowiZPelnegoDziennika() {
        TasteEventLog full = logOf(30);

        TasteEventCompactor.Result compacted = compactor.compact(
                full, FrozenTasteAggregate.empty(), tagger, null, NOW - 5 * DAY, 10);
        assertEquals(10, compacted.getLog().size());

        TasteModel fromFull = TasteModel.build(full,
                FrozenTasteAggregate.empty(), tagger, null, NOW);
        TasteModel fromCompacted = TasteModel.build(compacted.getLog(),
                compacted.getAggregate(), tagger, null, NOW);

        for (TasteDimension dimension : TasteDimension.values()) {
            for (String value : tagger.knownValues(dimension)) {
                for (int slot = -1; slot <= 2; slot++) {
                    assertEquals("wynik " + dimension + "/" + value + "/" + slot,
                            fromFull.score(dimension, value, slot),
                            fromCompacted.score(dimension, value, slot), 1e-9);
                }
            }
            assertEquals(fromFull.observationCount(dimension, TasteModel.ALL_MEALS),
                    fromCompacted.observationCount(dimension, TasteModel.ALL_MEALS));
        }
        assertEquals(fromFull.explicitPositiveMass(),
                fromCompacted.explicitPositiveMass(), 1e-9);
    }

    @Test
    public void kolejneKompakcjeSkladajaSiePoprawnie() {
        TasteEventLog full = logOf(30);

        // Dwie kompakcje w różnych momentach vs jedna — ten sam model.
        TasteEventCompactor.Result first = compactor.compact(
                full, FrozenTasteAggregate.empty(), tagger, null, NOW - 20 * DAY, 20);
        TasteEventCompactor.Result second = compactor.compact(
                first.getLog(), first.getAggregate(), tagger, null, NOW - 10 * DAY, 10);

        TasteModel fromFull = TasteModel.build(full,
                FrozenTasteAggregate.empty(), tagger, null, NOW);
        TasteModel fromTwice = TasteModel.build(second.getLog(),
                second.getAggregate(), tagger, null, NOW);

        assertEquals(fromFull.score(TasteDimension.BASE, "kurczak", 1),
                fromTwice.score(TasteDimension.BASE, "kurczak", 1), 1e-9);
        assertEquals(fromFull.score(TasteDimension.BASE, "ryba",
                        TasteModel.ALL_MEALS),
                fromTwice.score(TasteDimension.BASE, "ryba",
                        TasteModel.ALL_MEALS), 1e-9);
        assertEquals(fromFull.explicitPositiveMass(),
                fromTwice.explicitPositiveMass(), 1e-9);
    }
}
