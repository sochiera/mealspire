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
import org.robolectric.shadows.ShadowAlertDialog;
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
                        System.currentTimeMillis() + 3_600_000, "sub", "ola@example.com", "gpt-6-luna", "gpt-6.1-sol"));

        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertFalse(dialogShownWithTitle(MainActivity.SIGN_IN_DIALOG_TITLE));
    }

    @Test
    public void signedInUserGetsAiWithoutBeingAskedForServerAddress() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        new SharedPreferencesChatGptSessionStore(ApplicationProvider.getApplicationContext())
                .save(new ChatGptSession("oaiapp_x", "at", "rt", "it",
                        System.currentTimeMillis() + 3_600_000, "sub", "ola@example.com", "gpt-6-luna", "gpt-6.1-sol"));
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        activity.<android.widget.Button>findViewById(R.id.more_button).performClick();
        android.widget.ListAdapter items = ShadowAlertDialog.getLatestAlertDialog().getListView().getAdapter();
        boolean modelItem = false;
        for (int i = 0; i < items.getCount(); i++) {
            String item = String.valueOf(items.getItem(i));
            assertFalse(item, item.contains("Serwer"));
            modelItem |= item.startsWith(MainActivity.MODEL_MENU_PREFIX);
        }
        assertTrue(modelItem);
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            CharSequence title = dialog instanceof AlertDialog
                    ? Shadows.shadowOf((AlertDialog) dialog).getTitle() : null;
            assertFalse(title != null && title.toString().contains("Serwer"));
        }
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
