package com.mealspire.app;

import static org.junit.Assume.assumeFalse;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;
import com.mealspire.app.update.UpdateActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Opt-in visual check of the redesigned UI and launcher icon: renders the main
 * screens offline to PNG files for a human to look at. Skipped in the normal
 * suite; run with {@code ./gradlew testDebugUnitTest -Pscreenshots
 * --tests '*UiScreenshotTest'} → {@code app/build/screenshots/}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi", sdk = 34)
public class UiScreenshotTest {
    private File outDir;

    @Before
    public void requireOptIn() {
        String dir = System.getProperty("mealspire.screenshots", "");
        assumeFalse("screenshots are opt-in (-Pscreenshots)", dir.isEmpty());
        outDir = new File(dir);
        outDir.mkdirs();
    }

    @Test
    public void onboarding() throws IOException {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        save(activity, "01-quiz-start");
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        save(activity, "02-quiz-diet");
        activity.<Button>findViewById(R.id.onboarding_next_button).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        activity.<Button>findViewById(R.id.onboarding_option_1).performClick();
        save(activity, "03-quiz-dishes");
    }

    @Test
    public void everydayFlow() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferencesAppSettings settings = new SharedPreferencesAppSettings(context);
        settings.markOnboardingDone();
        settings.saveDefaultServings(2);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        save(activity, "04-start");
        activity.<Button>findViewById(R.id.meal_lunch_button).performClick();
        save(activity, "05-proposals");
        activity.<Button>findViewById(R.id.like_button).performClick();
        save(activity, "06-proposals-liked");
        activity.<Button>findViewById(R.id.accept_button).performClick();
        save(activity, "07-recipe");
        activity.<View>findViewById(R.id.more_button).performClick();
        ShadowLooper.idleMainLooper();
        android.app.Dialog menu = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        saveView(menu.getWindow().getDecorView(), "14-dialog-more");
    }

    @Test
    public void updateBannerAndScreen() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferencesAppSettings settings = new SharedPreferencesAppSettings(context);
        settings.markOnboardingDone();
        settings.saveDefaultServings(4);
        new SharedPreferencesUpdateStateStore(context).saveLatestKnown(
                new VersionInfo(999, "2.0", "https://example.invalid/a.apk"));
        save(Robolectric.buildActivity(MainActivity.class).setup().get(), "08-update-banner");
        UpdateActivity update = Robolectric.buildActivity(UpdateActivity.class,
                UpdateActivity.intent(context, new VersionInfo(999, "2.0",
                        "https://example.invalid/a.apk"))).setup().get();
        save(update, "09-update-screen");
    }

    @Test
    public void launcherIcon() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        saveDrawable(context.getDrawable(R.mipmap.ic_launcher), 432, "10-icon-adaptive");
        saveDrawable(context.getDrawable(R.drawable.ic_brand_mark), 432, "11-icon-flat");
        saveDrawable(context.getDrawable(R.drawable.ic_launcher_monochrome), 432,
                "12-icon-monochrome");
        saveDrawable(context.getDrawable(R.drawable.ic_notification), 96, "13-icon-notification");
    }

    /** Renders the whole scrollable content (not just the visible window). */
    private void save(Activity activity, String name) throws IOException {
        ShadowLooper.idleMainLooper();
        ViewGroup content = activity.findViewById(android.R.id.content);
        View scroll = content.getChildAt(0);
        View page = scroll instanceof ViewGroup && ((ViewGroup) scroll).getChildCount() > 0
                ? ((ViewGroup) scroll).getChildAt(0) : scroll;
        saveView(page, name);
    }

    private void saveView(View page, String name) throws IOException {
        int width = page.getResources().getDisplayMetrics().widthPixels;
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        page.layout(0, 0, page.getMeasuredWidth(), page.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(page.getMeasuredWidth(),
                Math.max(1, page.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(com.mealspire.app.ui.Ui.BACKGROUND);
        page.draw(canvas);
        write(bitmap, name);
    }

    private void saveDrawable(Drawable drawable, int size, String name) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (name.contains("monochrome") || name.contains("notification")) {
            canvas.drawColor(0xFF3A3A3A); // white-only layers need a dark backdrop
        }
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        write(bitmap, name);
    }

    private void write(Bitmap bitmap, String name) throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(outDir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
