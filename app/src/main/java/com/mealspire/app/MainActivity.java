package com.mealspire.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.RecipePromptBuilder;
import com.mealspire.app.domain.RecipeService;
import com.mealspire.app.domain.RecipeTextParser;
import com.mealspire.app.net.HttpClaudeClient;

import java.io.IOException;
import java.util.Random;

/**
 * Thin UI layer: assembles the screen, reacts to clicks, and delegates recipe
 * logic to the domain layer. It keeps the original offline random picker and adds
 * AI-generated meal ideas via {@link RecipeService}.
 */
public class MainActivity extends Activity {
    private static final String[] MEAL_TYPES = {"Śniadanie", "Obiad", "Kolacja"};

    private static final Recipe[][] RECIPES = {
            {
                    new Recipe("Owsianka z jabłkiem",
                            "Składniki: płatki owsiane, mleko, jabłko, cynamon, orzechy.\n\n" +
                                    "Gotuj płatki w mleku przez kilka minut. Dodaj starte jabłko, cynamon i orzechy."),
                    new Recipe("Jajecznica ze szczypiorkiem",
                            "Składniki: jajka, masło, szczypiorek, sól, pieprz.\n\n" +
                                    "Rozpuść masło, wlej roztrzepane jajka i mieszaj do ścięcia. Posyp szczypiorkiem."),
                    new Recipe("Tost z awokado",
                            "Składniki: pieczywo, awokado, sok z cytryny, sól, płatki chili.\n\n" +
                                    "Podpiecz pieczywo. Rozgnieć awokado z cytryną i przyprawami, rozsmaruj na toście.")
            },
            {
                    new Recipe("Makaron z pomidorami",
                            "Składniki: makaron, passata, czosnek, oliwa, bazylia.\n\n" +
                                    "Ugotuj makaron. Podsmaż czosnek na oliwie, dodaj passatę i bazylię, połącz z makaronem."),
                    new Recipe("Ryż z warzywami",
                            "Składniki: ryż, marchew, papryka, groszek, sos sojowy.\n\n" +
                                    "Ugotuj ryż. Podsmaż warzywa, dopraw sosem sojowym i wymieszaj z ryżem."),
                    new Recipe("Kurczak z kaszą",
                            "Składniki: pierś z kurczaka, kasza, ogórek, jogurt, koperek.\n\n" +
                                    "Usmaż lub upiecz kurczaka. Podaj z kaszą i szybkim sosem jogurtowo-koperkowym.")
            },
            {
                    new Recipe("Sałatka grecka",
                            "Składniki: pomidor, ogórek, feta, oliwki, oliwa, oregano.\n\n" +
                                    "Pokrój warzywa i fetę. Dodaj oliwki, oliwę oraz oregano, delikatnie wymieszaj."),
                    new Recipe("Kanapki z twarożkiem",
                            "Składniki: pieczywo, twaróg, jogurt, rzodkiewka, szczypiorek.\n\n" +
                                    "Wymieszaj twaróg z jogurtem i warzywami. Nałóż na pieczywo."),
                    new Recipe("Omlet warzywny",
                            "Składniki: jajka, papryka, cebula, szpinak, ser.\n\n" +
                                    "Podsmaż warzywa, zalej jajkami i smaż na małym ogniu. Na koniec dodaj ser.")
            }
    };

    private final Random random = new Random();
    private Spinner mealSpinner;
    private TextView recipeTitle;
    private TextView recipeDetails;
    private Button aiButton;

    private HttpClaudeClient claudeClient;
    private RecipeService recipeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        claudeClient = new HttpClaudeClient(BuildConfig.ANTHROPIC_API_KEY);
        recipeService = new RecipeService(claudeClient, new RecipePromptBuilder(),
                new RecipeTextParser());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(255, 247, 237));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Mealspire");
        title.setTextSize(34);
        title.setTextColor(Color.rgb(67, 56, 45));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Wybierz porę dnia i znajdź pomysł na danie.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(92, 78, 62));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle, marginTop(8));

        mealSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MEAL_TYPES);
        mealSpinner.setAdapter(adapter);
        root.addView(mealSpinner, marginTop(28));

        aiButton = new Button(this);
        aiButton.setText("Wymyśl danie (AI)");
        aiButton.setAllCaps(false);
        aiButton.setTextSize(18);
        aiButton.setOnClickListener(view -> generateAiRecipe());
        root.addView(aiButton, marginTop(16));

        Button drawButton = new Button(this);
        drawButton.setText("Losuj gotowy przepis");
        drawButton.setAllCaps(false);
        drawButton.setTextSize(18);
        drawButton.setOnClickListener(view -> showRandomRecipe());
        root.addView(drawButton, marginTop(12));

        recipeTitle = new TextView(this);
        recipeTitle.setTextSize(24);
        recipeTitle.setTextColor(Color.rgb(67, 56, 45));
        root.addView(recipeTitle, marginTop(28));

        recipeDetails = new TextView(this);
        recipeDetails.setTextSize(17);
        recipeDetails.setLineSpacing(dp(4), 1.0f);
        recipeDetails.setTextColor(Color.rgb(80, 68, 54));
        root.addView(recipeDetails, marginTop(12));

        setContentView(scrollView);
        showRandomRecipe();
    }

    private void showRandomRecipe() {
        int mealIndex = mealSpinner.getSelectedItemPosition();
        Recipe[] recipes = RECIPES[mealIndex];
        showRecipe(recipes[random.nextInt(recipes.length)]);
    }

    private void generateAiRecipe() {
        if (!claudeClient.hasApiKey()) {
            recipeTitle.setText("Brak klucza API");
            recipeDetails.setText("Dodaj anthropic.api.key do pliku local.properties i zbuduj aplikację "
                    + "ponownie, aby korzystać z generowania dań przez AI. Możesz też użyć opcji "
                    + "„Losuj gotowy przepis”.");
            return;
        }

        final String mealType = MEAL_TYPES[mealSpinner.getSelectedItemPosition()];
        aiButton.setEnabled(false);
        recipeTitle.setText("Wymyślam danie…");
        recipeDetails.setText("");

        new Thread(() -> {
            try {
                final Recipe recipe = recipeService.generateRecipe(mealType);
                runOnUiThread(() -> {
                    showRecipe(recipe);
                    aiButton.setEnabled(true);
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    recipeTitle.setText("Nie udało się wymyślić dania");
                    recipeDetails.setText(message != null ? message
                            : "Spróbuj ponownie lub skorzystaj z opcji „Losuj gotowy przepis”.");
                    aiButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void showRecipe(Recipe recipe) {
        recipeTitle.setText(recipe.getTitle());
        recipeDetails.setText(recipe.getDetails());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
