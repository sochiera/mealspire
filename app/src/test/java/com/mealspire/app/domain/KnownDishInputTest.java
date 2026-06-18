package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KnownDishInputTest {

    @Test
    public void recognizesHttpAndHttpsUrls() {
        assertTrue(KnownDishInput.isUrl("https://example.com/przepis"));
        assertTrue(KnownDishInput.isUrl("http://example.com"));
        assertTrue(KnownDishInput.isUrl("  https://example.com/x  "));
    }

    @Test
    public void plainTextIsNotUrl() {
        assertFalse(KnownDishInput.isUrl("kremowy makaron z boczkiem"));
        assertFalse(KnownDishInput.isUrl(""));
        assertFalse(KnownDishInput.isUrl(null));
        assertFalse(KnownDishInput.isUrl("zobacz example.com"));
    }
}
