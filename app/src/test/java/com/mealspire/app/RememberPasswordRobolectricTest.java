package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.storage.SharedPreferencesSecretStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDialog;

/**
 * The password is asked only once. Once the unlocked key is remembered, later
 * launches skip the password prompt; without a remembered key (but with an
 * encrypted key in resources) the prompt still appears.
 */
@RunWith(RobolectricTestRunner.class)
public class RememberPasswordRobolectricTest {

    private static final String PASSWORD_DIALOG_TITLE = "Podaj hasło, aby odblokować AI";

    @Test
    public void rememberedKeySkipsPasswordPrompt() {
        new SharedPreferencesSecretStore(ApplicationProvider.getApplicationContext())
                .saveApiKey("sk-ant-remembered");

        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertFalse("a remembered key must not trigger the password prompt",
                dialogShownWithTitle(PASSWORD_DIALOG_TITLE));
    }

    @Test
    public void noRememberedKeyShowsPasswordPrompt() {
        // No remembered key, but resources ship an encrypted key -> ask once.
        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertTrue("expected the one-time password prompt on first launch",
                dialogShownWithTitle(PASSWORD_DIALOG_TITLE));
    }

    private boolean dialogShownWithTitle(String title) {
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
