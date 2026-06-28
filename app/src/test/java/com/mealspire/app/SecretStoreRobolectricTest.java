package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.SecretStore;
import com.mealspire.app.storage.SharedPreferencesSecretStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * The unlocked API key is remembered between launches so the password is asked
 * only once.
 */
@RunWith(RobolectricTestRunner.class)
public class SecretStoreRobolectricTest {

    private SecretStore newStore() {
        return new SharedPreferencesSecretStore(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void emptyByDefault() {
        SecretStore store = newStore();
        assertFalse(store.hasApiKey());
        assertEquals("", store.loadApiKey());
    }

    @Test
    public void remembersKeyAcrossInstances() {
        newStore().saveApiKey("sk-ant-test-123");
        SecretStore fresh = newStore();
        assertTrue(fresh.hasApiKey());
        assertEquals("sk-ant-test-123", fresh.loadApiKey());
    }

    @Test
    public void trimsStoredKey() {
        newStore().saveApiKey("  sk-ant-spaced  ");
        assertEquals("sk-ant-spaced", newStore().loadApiKey());
    }

    @Test
    public void blankKeyClearsInsteadOfStoring() {
        newStore().saveApiKey("sk-ant-test-123");
        newStore().saveApiKey("   ");
        assertFalse(newStore().hasApiKey());
    }

    @Test
    public void clearForgetsKey() {
        newStore().saveApiKey("sk-ant-test-123");
        newStore().clear();
        assertFalse(newStore().hasApiKey());
    }
}
