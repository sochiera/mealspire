package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.DietConstraints;
import com.mealspire.app.domain.HouseholdProfile;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Świeża instalacja zaczyna od quizu gustu: pytania o domowników, poziom
 * gotowania i kuchnie, potem trzy rundy wyboru dań zapisywane jako zwykłe
 * polubienia. „Pomiń" i ukończenie ustawiają trwałą flagę — quiz pokazuje
 * się tylko raz. Ścieżka wyłącznie offline.
 */
@RunWith(RobolectricTestRunner.class)
public class OnboardingRobolectricTest {

    private MainActivity launch() {
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    private static String questionText(MainActivity activity) {
        TextView question = activity.findViewById(R.id.onboarding_question);
        return question == null ? null : question.getText().toString();
    }

    @Test
    public void swiezaInstalacjaZaczynaOdQuizu() {
        MainActivity activity = launch();

        assertEquals("Dla kogo gotujesz?", questionText(activity));
        assertNotNull(activity.findViewById(R.id.onboarding_skip_button));
        assertNull(activity.findViewById(R.id.accept_button));
        // Meal buttons wait until the quiz is finished or skipped.
        assertFalse(activity.<Button>findViewById(R.id.meal_lunch_button).isEnabled());
    }

    @Test
    public void pominKonczyQuizNaZawszeIPokazujeEkranStartowy() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        assertNull(activity.findViewById(R.id.onboarding_question));
        assertTrue(activity.<Button>findViewById(R.id.meal_lunch_button).isEnabled());
        assertTrue(new SharedPreferencesAppSettings(
                ApplicationProvider.getApplicationContext()).isOnboardingDone());

        // A second launch goes straight to the normal start screen.
        MainActivity second = launch();
        assertNull(second.findViewById(R.id.onboarding_question));
    }

    @Test
    public void odpowiedziZPytanTrafiajaDoProfiluDomownikow() {
        MainActivity activity = launch();

        // 1. „Dla kogo gotujesz?" → „Dorośli i dzieci"
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();
        // 2. „Czego nie jadacie?": bez zaznaczeń, „Dalej"
        assertEquals("Czego nie jadacie?", questionText(activity));
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();
        // 3. Czas na gotowanie → „Do 20 minut"
        assertEquals("Ile masz zwykle czasu na gotowanie w dzień powszedni?",
                questionText(activity));
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        // 4. „Jak Ci idzie gotowanie?" → „Dopiero zaczynam"
        assertEquals("Jak Ci idzie gotowanie?", questionText(activity));
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();

        HouseholdProfile profile = new SharedPreferencesHouseholdProfileStore(
                ApplicationProvider.getApplicationContext()).load();
        assertEquals(HouseholdProfile.Audience.WITH_CHILDREN, profile.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.BEGINNER, profile.getSkill());
        assertEquals(HouseholdProfile.CookingTime.QUICK, profile.getTime());
        assertTrue(profile.getCuisines().isEmpty());
        assertTrue(profile.getDiet().isEmpty());
    }

    @Test
    public void zaznaczoneWykluczeniaTrafiajaDoProfilu() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();

        assertEquals("Czego nie jadacie?", questionText(activity));
        // Pierwszy checkbox to „Wegetariańsko…" (kolejność enuma).
        firstCheckBox(activity).performClick();
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();

        HouseholdProfile profile = new SharedPreferencesHouseholdProfileStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue(profile.getDiet().getExclusions()
                .contains(DietConstraints.Exclusion.VEGETARIAN));
    }

    private static android.widget.CheckBox firstCheckBox(MainActivity activity) {
        android.view.ViewGroup content = activity.findViewById(android.R.id.content);
        android.widget.CheckBox box = findCheckBox(content);
        assertNotNull("na pytaniu o dietę musi być checkbox", box);
        return box;
    }

    private static android.widget.CheckBox findCheckBox(android.view.View view) {
        if (view instanceof android.widget.CheckBox) {
            return (android.widget.CheckBox) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.CheckBox found = findCheckBox(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Przechodzi cztery pytania profilu (dieta bez zaznaczeń), do rund dań. */
    private static void answerProfileQuestions(MainActivity activity) {
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();
    }

    @Test
    public void trzyRundyDanZapisujaTrzyPolubienia() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        for (int round = 0; round < 3; round++) {
            assertEquals("Które danie najbardziej Ci pasuje?", questionText(activity));
            activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        }

        assertNull(activity.findViewById(R.id.onboarding_question));
        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertEquals(3, saved.getLikes().size());
        assertTrue(new SharedPreferencesAppSettings(
                ApplicationProvider.getApplicationContext()).isOnboardingDone());
    }

    @Test
    public void cofnieciePozwalaZmienicWyborDaniaBezKumulowaniaPolubien() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        // Runda 1: wybierz danie A, cofnij się, wybierz jednak danie B.
        String dishA = activity.<Button>findViewById(R.id.onboarding_option_1)
                .getText().toString();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.onBackPressed();
        String dishB = activity.<Button>findViewById(R.id.onboarding_option_2)
                .getText().toString();
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();
        // Rundy 2 i 3 normalnie.
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();

        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertEquals(3, saved.getLikes().size());
        assertTrue(saved.getLikes().contains(dishB));
        assertFalse("wycofany wybór nie może zostać polubieniem",
                saved.getLikes().contains(dishA));
    }

    @Test
    public void zadneZTychNieZapisujePolubienia() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        for (int round = 0; round < 3; round++) {
            assertEquals("Które danie najbardziej Ci pasuje?", questionText(activity));
            activity.<Button>findViewById(R.id.onboarding_option_none).performClick();
        }

        assertNull(activity.findViewById(R.id.onboarding_question));
        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertEquals("wymuszony wybór to fałszywy sygnał — brak polubień",
                0, saved.getLikes().size());
    }

    @Test
    public void wykluczeniaFiltrujaRundyDan() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        // Dieta: „Wegetariańsko…" (pierwszy checkbox).
        firstCheckBox(activity).performClick();
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_2).performClick();

        DietConstraints veg = DietConstraints.of(java.util.Collections.singletonList(
                DietConstraints.Exclusion.VEGETARIAN));
        int[] optionIds = {R.id.onboarding_option_1, R.id.onboarding_option_2,
                R.id.onboarding_option_3};
        for (int round = 0; round < 3; round++) {
            assertEquals("Które danie najbardziej Ci pasuje?", questionText(activity));
            for (int optionId : optionIds) {
                String dish = activity.<Button>findViewById(optionId)
                        .getText().toString();
                assertTrue("mięso w rundzie mimo diety: " + dish, veg.allows(dish));
            }
            activity.<Button>findViewById(R.id.onboarding_option_none).performClick();
        }
    }

    @Test
    public void pominBezRundNieZapisujeZadnychPolubien() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertEquals(0, saved.getLikes().size());
    }

    @Test
    public void dialogHaslaNieZaslaniaQuizuIPojawiaSieDopieroPoNim() {
        MainActivity activity = launch();

        // Zasoby zawierają zaszyfrowany klucz, ale pytanie o hasło czeka na
        // koniec quizu — quiz ma być pierwszym, co widzi użytkownik.
        assertFalse("dialog hasła nie może zasłaniać quizu",
                dialogShownWithTitle("Podaj hasło, aby odblokować AI"));

        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        assertTrue("po quizie jednorazowe pytanie o hasło ma się pojawić",
                dialogShownWithTitle("Podaj hasło, aby odblokować AI"));
    }

    private static boolean dialogShownWithTitle(String title) {
        for (android.app.Dialog dialog : org.robolectric.shadows.ShadowDialog.getShownDialogs()) {
            if (dialog instanceof android.app.AlertDialog) {
                CharSequence shown = org.robolectric.Shadows
                        .shadowOf((android.app.AlertDialog) dialog).getTitle();
                if (shown != null && title.contentEquals(shown)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void wybraneDaniaZRundZapisujaSieTezPrzyPominieciu() {
        MainActivity activity = launch();
        answerProfileQuestions(activity);

        String dish = activity.<Button>findViewById(R.id.onboarding_option_1)
                .getText().toString();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        UserPreferences saved = new SharedPreferencesPreferenceStore(
                ApplicationProvider.getApplicationContext()).load();
        assertTrue("odpowiedź z ukończonej rundy ma zostać",
                saved.getLikes().contains(dish));
        assertEquals(1, saved.getLikes().size());
    }

    @Test
    public void cofnijWQuizieWracaDoPoprzedniegoPytania() {
        MainActivity activity = launch();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        assertEquals("Czego nie jadacie?", questionText(activity));

        activity.onBackPressed();

        assertFalse(activity.isFinishing());
        assertEquals("Dla kogo gotujesz?", questionText(activity));
    }

    @Test
    public void cofnijNaPierwszymPytaniuDzialaJakPomin() {
        MainActivity activity = launch();

        activity.onBackPressed();

        assertFalse(activity.isFinishing());
        assertNull(activity.findViewById(R.id.onboarding_question));
        assertTrue(new SharedPreferencesAppSettings(
                ApplicationProvider.getApplicationContext()).isOnboardingDone());
    }

    @Test
    public void startZPowiadomieniaCzekaZPosilkiemNaKoniecQuizu() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                MainActivity.class).putExtra(MainActivity.EXTRA_MEAL_INDEX, 1);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent)
                .setup().get();

        // The quiz comes first, the meal intent is not lost.
        assertEquals("Dla kogo gotujesz?", questionText(activity));
        activity.<Button>findViewById(R.id.onboarding_skip_button).performClick();

        // After skipping, the app continues straight to that meal's proposals.
        assertNotNull(activity.findViewById(R.id.accept_button));
    }
}
