package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * The app ships its own launcher icon (not the Android default) and that icon
 * resolves to a loadable drawable.
 */
@RunWith(RobolectricTestRunner.class)
public class AppIconTest {

    @Test
    public void usesCustomLauncherIcon() {
        Context context = ApplicationProvider.getApplicationContext();
        int iconRes = context.getApplicationInfo().icon;

        assertEquals(R.mipmap.ic_launcher, iconRes);
        assertNotNull(context.getResources().getDrawable(iconRes, context.getTheme()));
    }

    /** Android 13+ themed icons need a one-colour layer of their own. */
    @Test
    @org.robolectric.annotation.Config(sdk = 33)
    public void adaptiveIconHasDedicatedMonochromeLayer() {
        Context context = ApplicationProvider.getApplicationContext();
        android.graphics.drawable.AdaptiveIconDrawable icon =
                (android.graphics.drawable.AdaptiveIconDrawable)
                        context.getDrawable(R.mipmap.ic_launcher);

        assertNotNull(icon.getMonochrome());
        assertNotNull(icon.getForeground());
    }
}
