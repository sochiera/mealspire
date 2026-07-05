package com.mealspire.app.domain;

import java.util.List;
import java.util.Map;

/**
 * Kompakcja dziennika gustu: zdarzenia ponad limit (najstarsze) są zwijane do
 * zamrożonego agregatu zdyskontowanego na moment kompakcji, a dziennik
 * przycinany do najnowszych. Model policzony z (przycięty dziennik + agregat)
 * jest równoważny modelowi z pełnego dziennika — wygaszanie wykładnicze
 * składa się dokładnie.
 */
public final class TasteEventCompactor {

    /** Wynik kompakcji: nowy dziennik i nowy agregat — zapisz oba. */
    public static final class Result {
        private final TasteEventLog log;
        private final FrozenTasteAggregate aggregate;

        Result(TasteEventLog log, FrozenTasteAggregate aggregate) {
            this.log = log;
            this.aggregate = aggregate;
        }

        public TasteEventLog getLog() {
            return log;
        }

        public FrozenTasteAggregate getAggregate() {
            return aggregate;
        }
    }

    public Result compact(TasteEventLog log, FrozenTasteAggregate aggregate,
                          DishTagger tagger, Map<String, String> detailsByTitle,
                          long now, int maxEvents) {
        if (aggregate == null) {
            aggregate = FrozenTasteAggregate.empty();
        }
        List<TasteEvent> overflow = log.overflow(maxEvents);
        if (overflow.isEmpty()) {
            return new Result(log, aggregate);
        }

        // Stary agregat zdyskontowany do „teraz" + wkład zwijanych zdarzeń —
        // ta sama arytmetyka co przy liczeniu modelu (TasteAccumulator).
        TasteAccumulator accumulator = new TasteAccumulator();
        accumulator.absorb(aggregate, now);
        for (TasteEvent event : overflow) {
            accumulator.absorb(event, tagger, detailsByTitle, now);
        }

        return new Result(log.trimToNewest(maxEvents),
                new FrozenTasteAggregate(now, accumulator.scores,
                        accumulator.observations, accumulator.explicitPositiveMass));
    }
}
