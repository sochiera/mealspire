package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.TasteEvent;
import com.mealspire.app.domain.TasteEventLog;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;
import com.mealspire.app.storage.SharedPreferencesTasteEventStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Akcje użytkownika zostawiają ślad w dzienniku zdarzeń gustu: polubienie,
 * „Pokaż przepis", wybory z quizu i migracja starych polubień. Ścieżka
 * wyłącznie offline.
 */
@RunWith(RobolectricTestRunner.class)
public class TasteEventRecordingRobolectricTest {

    private static TasteEventLog loadEvents() {
        return new SharedPreferencesTasteEventStore(
                ApplicationProvider.getApplicationContext()).load();
    }

    private static MainActivity launchPastOnboarding() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Button skip = activity.findViewById(R.id.onboarding_skip_button);
        if (skip != null) {
            skip.performClick();
        }
        return activity;
    }

    @Test
    public void polubieniePropozycjiZapisujeZdarzenieLiked() {
        MainActivity activity = launchPastOnboarding();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        String dish = activity.<android.widget.TextView>findViewById(R.id.recipe_title)
                .getText().toString();

        activity.<Button>findViewById(R.id.like_button).performClick();

        TasteEventLog events = loadEvents();
        boolean found = false;
        for (TasteEvent event : events.events()) {
            if (event.getType() == TasteEvent.Type.LIKED
                    && event.getDishTitle().equals(dish)
                    && event.getMealIndex() == 1) {
                found = true;
            }
        }
        assertTrue("polubienie ma zostawić zdarzenie LIKED ze slotem obiadu", found);
    }

    @Test
    public void pokazPrzepisZapisujeRecipeViewedIPrzegranychWTrojce() {
        MainActivity activity = launchPastOnboarding();
        activity.<Button>findViewById(R.id.meal_breakfast_button).performClick();

        activity.<Button>findViewById(R.id.accept_button).performClick();

        TasteEventLog events = loadEvents();
        int viewed = 0;
        int notChosen = 0;
        for (TasteEvent event : events.events()) {
            if (event.getType() == TasteEvent.Type.RECIPE_VIEWED) {
                viewed++;
                assertEquals(0, event.getMealIndex());
            } else if (event.getType() == TasteEvent.Type.SHOWN_NOT_CHOSEN) {
                notChosen++;
            }
        }
        assertEquals(1, viewed);
        assertEquals("dwie pozostałe propozycje przegrały porównanie", 2, notChosen);
    }

    @Test
    public void przegladanieTamIZPowrotemNiePompujeSygnalow() {
        MainActivity activity = launchPastOnboarding();
        activity.<Button>findViewById(R.id.meal_breakfast_button).performClick();

        // Otwórz przepis, wróć do propozycji, otwórz ten sam ponownie.
        activity.<Button>findViewById(R.id.accept_button).performClick();
        activity.onBackPressed();
        activity.<Button>findViewById(R.id.accept_button).performClick();

        int viewed = 0;
        int notChosen = 0;
        for (TasteEvent event : loadEvents().events()) {
            if (event.getType() == TasteEvent.Type.RECIPE_VIEWED) {
                viewed++;
            } else if (event.getType() == TasteEvent.Type.SHOWN_NOT_CHOSEN) {
                notChosen++;
            }
        }
        assertEquals("to samo danie liczy się raz na trójkę", 1, viewed);
        assertEquals("porównanie liczy się raz na trójkę", 2, notChosen);
    }

    @Test
    public void innePropozycjeZapisujaRerolledDlaCalejTrojki() {
        MainActivity activity = launchPastOnboarding();
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();

        activity.<Button>findViewById(R.id.refresh_button).performClick();

        int rerolled = 0;
        for (TasteEvent event : loadEvents().events()) {
            if (event.getType() == TasteEvent.Type.REROLLED) {
                rerolled++;
                assertEquals(1, event.getMealIndex());
            }
        }
        assertEquals(3, rerolled);
    }

    @Test
    public void wyborDaniaWQuizieZapisujeZdarzenieOnboardingPick() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        // Cztery pytania profilu (dieta bez zaznaczeń), potem jedna runda dania.
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        String dish = activity.<Button>findViewById(R.id.onboarding_option_1)
                .getText().toString();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        TasteEventLog events = loadEvents();
        assertEquals(1, events.size());
        assertEquals(TasteEvent.Type.ONBOARDING_PICK, events.events().get(0).getType());
        assertEquals(dish, events.events().get(0).getDishTitle());
    }

    @Test
    public void starePolubieniaMigrujaDoDziennikaPrzyStarcie() {
        SharedPreferencesPreferenceStore preferenceStore =
                new SharedPreferencesPreferenceStore(
                        ApplicationProvider.getApplicationContext());
        preferenceStore.save(UserPreferences.empty()
                .withLike("Omlet").withLike("Zupa krem"));

        launchPastOnboarding();

        TasteEventLog events = loadEvents();
        assertEquals(2, events.size());
        assertEquals(TasteEvent.Type.LIKED, events.events().get(0).getType());
    }
}
