package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Systemowe „Cofnij" ma cofać o jeden widok, a nie zamykać aplikację.
 * Decyzja „co się dzieje po cofnięciu" jest czysto domenowa. Z przepisu
 * wracamy do propozycji tylko wtedy, gdy przepis faktycznie z nich pochodzi —
 * przepis z importu („Dodaj danie, które znasz") wraca na ekran startowy.
 */
public class BackNavigationTest {

    @Test
    public void zPrzepisuZPropozycjiWracaDoPropozycji() {
        assertEquals(BackNavigation.Action.SHOW_PROPOSALS,
                BackNavigation.onBack(BackNavigation.Screen.RECIPE, 0, true));
    }

    @Test
    public void zPrzepisuSpozaPropozycjiWracaNaEkranStartowy() {
        assertEquals(BackNavigation.Action.SHOW_START,
                BackNavigation.onBack(BackNavigation.Screen.RECIPE, 0, false));
    }

    @Test
    public void zPropozycjiWracaNaEkranStartowy() {
        assertEquals(BackNavigation.Action.SHOW_START,
                BackNavigation.onBack(BackNavigation.Screen.PROPOSALS, 0, false));
    }

    @Test
    public void zEkranuStartowegoZachowujeDomyslneWyjscie() {
        assertEquals(BackNavigation.Action.EXIT,
                BackNavigation.onBack(BackNavigation.Screen.START, 0, false));
    }

    @Test
    public void wOnboardinguCofaDoPoprzedniegoPytania() {
        assertEquals(BackNavigation.Action.ONBOARDING_PREVIOUS,
                BackNavigation.onBack(BackNavigation.Screen.ONBOARDING, 3, false));
    }

    @Test
    public void zPierwszegoPytaniaOnboardinguDzialaJakPomin() {
        assertEquals(BackNavigation.Action.ONBOARDING_SKIP,
                BackNavigation.onBack(BackNavigation.Screen.ONBOARDING, 0, false));
    }
}
