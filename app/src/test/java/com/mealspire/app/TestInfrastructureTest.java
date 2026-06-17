package com.mealspire.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Smoke test that proves the JVM unit-test infrastructure is wired up.
 */
public class TestInfrastructureTest {

    @Test
    public void junitIsAvailable() {
        assertEquals(4, 2 + 2);
    }
}
