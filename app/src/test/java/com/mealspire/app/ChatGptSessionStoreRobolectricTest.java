package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.ChatGptSession;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** The ChatGPT session survives restarts; signing out keeps the install's host id. */
@RunWith(RobolectricTestRunner.class)
public class ChatGptSessionStoreRobolectricTest {

    private SharedPreferencesChatGptSessionStore newStore() {
        return new SharedPreferencesChatGptSessionStore(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void sessionSurvivesNewStoreInstance() {
        newStore().save(new ChatGptSession("oaiapp_x", "at", "rt", "it", 5L, "sub",
                "ola@example.com", "gpt-6-luna", "gpt-6.1-sol"));
        assertEquals("rt", newStore().load().refreshToken);
    }

    @Test
    public void clearSignsOutButKeepsHostId() {
        SharedPreferencesChatGptSessionStore store = newStore();
        String hostId = store.hostId();
        store.save(new ChatGptSession("oaiapp_x", "at", "rt", "it", 5L, "sub", "e", "gpt-6-luna", "gpt-6.1-sol"));
        store.clear();
        assertNull(newStore().load());
        assertEquals(hostId, newStore().hostId());
    }

    @Test
    public void modelChoiceDefaultsToLunaAndSurvivesSignOut() {
        SharedPreferencesChatGptSessionStore store = newStore();
        assertEquals(com.mealspire.app.domain.GptModel.LUNA, store.modelChoice());
        store.saveModelChoice(com.mealspire.app.domain.GptModel.SOL);
        store.clear();
        assertEquals(com.mealspire.app.domain.GptModel.SOL, newStore().modelChoice());
    }

    @Test
    public void hostIdIsUuidUrn() {
        assertTrue(newStore().hostId().matches(
                "urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
    }
}
