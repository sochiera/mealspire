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
}
