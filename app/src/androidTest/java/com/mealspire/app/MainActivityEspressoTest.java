package com.mealspire.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * On-device instrumentation tests (Espresso). Requires a connected device or
 * emulator: {@code ./gradlew connectedDebugAndroidTest}. Covers the offline UI
 * flow so the suite never depends on a network/API key.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityEspressoTest {

    @Test
    public void mainScreenShowsControls() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.meal_spinner)).check(matches(isDisplayed()));
            onView(withId(R.id.ai_button)).check(matches(isDisplayed()));
            onView(withId(R.id.draw_button)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void drawButtonShowsARecipe() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.draw_button)).perform(click());
            onView(withId(R.id.recipe_title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void choiceOptionIsVisible() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Wegetariańskie")).check(matches(isDisplayed()));
        }
    }
}
