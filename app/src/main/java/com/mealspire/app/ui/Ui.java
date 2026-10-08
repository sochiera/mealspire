package com.mealspire.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The Mealspire look in one place: palette, type scale and the few view
 * recipes (buttons, cards, chips) every screen is assembled from. Views stay
 * programmatic (no XML layouts); this class only dresses them.
 *
 * <p>Keep the palette in sync with {@code values/colors.xml} (theme, dialogs,
 * launcher icon). Text/background pairs below meet WCAG AA (≥ 4.5:1).
 */
public final class Ui {
    // Warm paper background, white cards, terracotta accent, herb green for
    // "taste" signals (likes, the model's reason).
    public static final int BACKGROUND = Color.rgb(251, 246, 240);
    public static final int SURFACE = Color.WHITE;
    public static final int SURFACE_MUTED = Color.rgb(245, 237, 227);
    public static final int OUTLINE = Color.rgb(234, 220, 203);
    public static final int INK = Color.rgb(43, 33, 26);
    public static final int INK_BODY = Color.rgb(74, 62, 51);
    public static final int INK_SOFT = Color.rgb(110, 94, 80);
    public static final int ACCENT = Color.rgb(200, 75, 20);
    public static final int ACCENT_DEEP = Color.rgb(154, 52, 18);
    public static final int ACCENT_SOFT = Color.rgb(252, 230, 214);
    public static final int HERB = Color.rgb(47, 107, 79);
    public static final int HERB_SOFT = Color.rgb(229, 241, 232);
    public static final int RIPPLE_ON_ACCENT = Color.argb(64, 255, 255, 255);
    public static final int RIPPLE_ON_LIGHT = Color.argb(38, 200, 75, 20);

    public static final int PILL_CORNER_DP = 24;
    public static final int CARD_CORNER_DP = 20;

    /** Headlines use the platform serif — a quiet cookbook feel, no bundled fonts. */
    public static final Typeface DISPLAY = Typeface.create("serif", Typeface.BOLD);
    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ----- Shapes ------------------------------------------------------------

    public static GradientDrawable rounded(Context context, int fill, int cornerDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, cornerDp));
        return shape;
    }

    public static GradientDrawable outlined(Context context, int fill, int stroke, int cornerDp) {
        GradientDrawable shape = rounded(context, fill, cornerDp);
        shape.setStroke(Math.max(1, dp(context, 1)), stroke);
        return shape;
    }

    public static GradientDrawable circle(int fill) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(fill);
        return shape;
    }

    private static Drawable ripple(Context context, Drawable content, int ripple, int cornerDp) {
        return new RippleDrawable(ColorStateList.valueOf(ripple), content,
                rounded(context, Color.WHITE, cornerDp));
    }

    // ----- Text ----------------------------------------------------------------

    public static TextView text(Context context, CharSequence value, float sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    /** Serif headline (screen titles, dish names). */
    public static TextView headline(Context context, CharSequence value, float sizeSp) {
        TextView view = text(context, value, sizeSp, INK);
        view.setTypeface(DISPLAY);
        view.setLineSpacing(0, 1.05f);
        return view;
    }

    /** Small spaced caps label above a section ("SKŁADNIKI", "POMYSŁY NA OBIAD"). */
    public static TextView overline(Context context, CharSequence value) {
        TextView view = text(context, value.toString().toUpperCase(), 12, ACCENT_DEEP);
        view.setTypeface(MEDIUM);
        view.setLetterSpacing(0.12f);
        return view;
    }

    public static TextView body(Context context, CharSequence value) {
        TextView view = text(context, value, 16, INK_BODY);
        view.setLineSpacing(dp(context, 3), 1.0f);
        return view;
    }

    // ----- Buttons -------------------------------------------------------------

    private static void styleButton(Button button, int fill, int stroke, int textColor,
                                    int ripple) {
        Context context = button.getContext();
        GradientDrawable shape = stroke == 0
                ? rounded(context, fill, PILL_CORNER_DP)
                : outlined(context, fill, stroke, PILL_CORNER_DP);
        button.setBackground(ripple(context, shape, ripple, PILL_CORNER_DP));
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 18), dp(context, 12), dp(context, 18), dp(context, 12));
        button.setTypeface(MEDIUM);
        button.setLetterSpacing(0.01f);
    }

    /** The one main action on a screen: filled terracotta. */
    public static void primary(Button button) {
        styleButton(button, ACCENT, 0, Color.WHITE, RIPPLE_ON_ACCENT);
    }

    /** Secondary actions: soft peach fill. */
    public static void tonal(Button button) {
        styleButton(button, ACCENT_SOFT, 0, ACCENT_DEEP, RIPPLE_ON_LIGHT);
    }

    /** Alternatives next to a primary action: hairline outline on white. */
    public static void outlinedButton(Button button) {
        styleButton(button, SURFACE, OUTLINE, INK_BODY, RIPPLE_ON_LIGHT);
    }

    /** Quiet actions ("Pomiń", "Wróć"): label only, bounded ripple. */
    public static void ghost(Button button) {
        styleButton(button, Color.TRANSPARENT, 0, ACCENT_DEEP, RIPPLE_ON_LIGHT);
    }

    /** A reaction the user just gave ("Lubię to" ✓): herb green fill. */
    public static void confirmed(Button button) {
        styleButton(button, HERB_SOFT, 0, HERB, RIPPLE_ON_LIGHT);
    }

    /** Large left-aligned answer card (quiz options). */
    public static void choice(Button button) {
        Context context = button.getContext();
        button.setBackground(ripple(context,
                outlined(context, SURFACE, OUTLINE, 16), RIPPLE_ON_LIGHT, 16));
        button.setTextColor(INK);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTypeface(Typeface.DEFAULT);
        button.setMinHeight(dp(context, 56));
        button.setMinimumHeight(dp(context, 56));
        button.setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 14));
    }

    /** Compact rounded chip (servings, AI mode). */
    public static void chip(Button button, boolean highlighted) {
        Context context = button.getContext();
        int fill = highlighted ? HERB_SOFT : SURFACE;
        int stroke = highlighted ? HERB_SOFT : OUTLINE;
        button.setBackground(ripple(context, outlined(context, fill, stroke, 18),
                RIPPLE_ON_LIGHT, 18));
        button.setTextColor(highlighted ? HERB : INK_BODY);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setTypeface(MEDIUM);
        button.setMinHeight(dp(context, 36));
        button.setMinimumHeight(dp(context, 36));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 6));
    }

    /** Custom-drawn buttons have no platform disabled state, so fade them. */
    public static void setEnabledWithFade(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
    }

    // ----- Containers ----------------------------------------------------------

    /** White card with a hairline outline and a soft shadow. */
    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(outlined(context, SURFACE, OUTLINE, CARD_CORNER_DP));
        card.setElevation(dp(context, 1));
        int padding = dp(context, 20);
        card.setPadding(padding, dp(context, 18), padding, dp(context, 18));
        return card;
    }

    /** Tinted callout inside a card (the model's "why", tips). */
    public static TextView callout(Context context, CharSequence value, int fill, int ink) {
        TextView view = text(context, value, 14, ink);
        view.setBackground(rounded(context, fill, 12));
        view.setLineSpacing(dp(context, 2), 1.0f);
        view.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        return view;
    }

    public static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    // ----- Layout params -------------------------------------------------------

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams marginTop(Context context, int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(context, topDp);
        return params;
    }

    /** Equal-width cell in a horizontal row, with a gutter between cells. */
    public static LinearLayout.LayoutParams weighted(Context context, int gutterDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(context, gutterDp / 2f);
        params.rightMargin = dp(context, gutterDp / 2f);
        return params;
    }
}
