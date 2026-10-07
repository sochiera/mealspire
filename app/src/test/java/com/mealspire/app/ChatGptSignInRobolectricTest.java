package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.ChatGptSession;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;

/**
 * AI is unlocked by signing in with the user's own ChatGPT account — there is
 * no built-in key and no password. Signed-out users are offered the sign-in;
 * signed-in users are not nagged.
 */
@RunWith(RobolectricTestRunner.class)
public class ChatGptSignInRobolectricTest {

    @Test
    public void signedOutUserIsOfferedChatGptSignIn() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertTrue(dialogShownWithTitle(MainActivity.SIGN_IN_DIALOG_TITLE));
    }

    @Test
    public void signedInUserIsNotAskedAgain() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        new SharedPreferencesChatGptSessionStore(ApplicationProvider.getApplicationContext())
                .save(new ChatGptSession("oaiapp_x", "at", "rt", "it",
                        System.currentTimeMillis() + 3_600_000, "sub", "ola@example.com", "gpt-x"));

        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertFalse(dialogShownWithTitle(MainActivity.SIGN_IN_DIALOG_TITLE));
    }

    private static boolean dialogShownWithTitle(String title) {
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            if (dialog instanceof AlertDialog) {
                CharSequence shown = Shadows.shadowOf((AlertDialog) dialog).getTitle();
                if (shown != null && title.contentEquals(shown)) {
                    return true;
                }
            }
        }
        return false;
    }
}
