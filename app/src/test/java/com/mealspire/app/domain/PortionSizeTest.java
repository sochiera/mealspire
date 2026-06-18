package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PortionSizeTest {

    @Test
    public void singlePersonUsesSingularGenitive() {
        assertTrue(PortionSize.promptFragment(1).contains("1 osoby"));
    }

    @Test
    public void multiplePeopleUsePluralGenitive() {
        assertTrue(PortionSize.promptFragment(4).contains("4 osób"));
        assertTrue(PortionSize.promptFragment(2).contains("2 osób"));
    }

    @Test
    public void zeroOrNegativeYieldsEmptyFragment() {
        assertEquals("", PortionSize.promptFragment(0));
        assertEquals("", PortionSize.promptFragment(-3));
    }
}
