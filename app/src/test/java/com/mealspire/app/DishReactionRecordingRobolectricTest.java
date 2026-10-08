package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.DishReaction;
import com.mealspire.app.domain.DishReactionLog;
import com.mealspire.app.storage.SharedPreferencesDishReactionStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * „Lubię to" / „Nie lubię" dopisują jawną reakcję do listy; otwarcie przepisu
 * reakcją nie jest. Bez logowania — ścieżka offline, bez wołania AI.
 */
@RunWith(RobolectricTestRunner.class)
public class DishReactionRecordingRobolectricTest {

    private static DishReactionLog loadReactions() {
        return new SharedPreferencesDishReactionStore(
                ApplicationProvider.getApplicationContext()).load();
    }

    private static MainActivity launchOnLunchProposals() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        Button skip = activity.findViewById(R.id.onboarding_skip_button);
        if (skip != null) {
            skip.performClick();
        }
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        return activity;
    }

    private static String firstDish(MainActivity activity) {
        return activity.<TextView>findViewById(R.id.recipe_title).getText().toString();
    }

    @Test
    public void nieLubieDopisujeReakcjeZOpisemICzasem() {
        MainActivity activity = launchOnLunchProposals();
        String dish = firstDish(activity);

        activity.<Button>findViewById(R.id.dislike_button).performClick();

        DishReactionLog reactions = loadReactions();
        assertEquals(1, reactions.size());
        DishReaction reaction = reactions.all().get(0);
        assertEquals(dish, reaction.getDish());
        assertFalse(reaction.isLiked());
        assertTrue(reaction.getDescription().startsWith("składniki:"));
        assertTrue(reaction.getTimeMillis() > 0);
    }

    @Test
    public void lubieToDopisujeReakcjePozytywna() {
        MainActivity activity = launchOnLunchProposals();
        String dish = firstDish(activity);

        activity.<Button>findViewById(R.id.like_button).performClick();

        DishReactionLog reactions = loadReactions();
        assertEquals(1, reactions.size());
        assertEquals(dish, reactions.all().get(0).getDish());
        assertTrue(reactions.all().get(0).isLiked());
    }

    @Test
    public void otwarciePrzepisuIInnePropozycjeNieSaOcena() {
        MainActivity activity = launchOnLunchProposals();

        activity.<Button>findViewById(R.id.refresh_button).performClick();
        activity.<Button>findViewById(R.id.accept_button).performClick();

        assertTrue(loadReactions().isEmpty());
    }

    @Test
    public void nieLubieNaEkraniePrzepisuTezDopisujeReakcje() {
        MainActivity activity = launchOnLunchProposals();
        activity.<Button>findViewById(R.id.accept_button).performClick();
        String dish = firstDish(activity);

        activity.<Button>findViewById(R.id.dislike_button).performClick();

        DishReactionLog reactions = loadReactions();
        assertEquals(1, reactions.size());
        assertEquals(dish, reactions.all().get(0).getDish());
        assertFalse(reactions.all().get(0).isLiked());
    }
}
