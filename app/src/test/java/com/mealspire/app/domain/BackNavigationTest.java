package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Systemowe „Cofnij" ma cofać o jeden widok, a nie zamykać aplikację.
 * Decyzja „co się dzieje po cofnięciu" jest czysto domenowa.
 */
public class BackNavigationTest {

    @Test
    public void zPelnegoPrzepisuWracaDoPropozycji() {
        assertEquals(BackNavigation.Action.SHOW_PROPOSALS,
                BackNavigation.onBack(BackNavigation.Screen.RECIPE, 0));
    }

    @Test
    public void zListyZakupowWracaDoPrzepisu() {
        assertEquals(BackNavigation.Action.SHOW_RECIPE,
                BackNavigation.onBack(BackNavigation.Screen.SHOPPING_LIST, 0));
    }

    @Test
    public void zPropozycjiWracaNaEkranStartowy() {
        assertEquals(BackNavigation.Action.SHOW_START,
                BackNavigation.onBack(BackNavigation.Screen.PROPOSALS, 0));
    }

    @Test
    public void zEkranuStartowegoZachowujeDomyslneWyjscie() {
        assertEquals(BackNavigation.Action.EXIT,
                BackNavigation.onBack(BackNavigation.Screen.START, 0));
    }

    @Test
    public void wOnboardinguCofaDoPoprzedniegoPytania() {
        assertEquals(BackNavigation.Action.ONBOARDING_PREVIOUS,
                BackNavigation.onBack(BackNavigation.Screen.ONBOARDING, 3));
    }

    @Test
    public void zPierwszegoPytaniaOnboardinguDzialaJakPomin() {
        assertEquals(BackNavigation.Action.ONBOARDING_SKIP,
                BackNavigation.onBack(BackNavigation.Screen.ONBOARDING, 0));
    }
}
