package com.mealspire.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mealspire.app.domain.ChoiceLearning;
import com.mealspire.app.domain.MealChoiceOption;
import com.mealspire.app.domain.MealChoices;
import com.mealspire.app.domain.MealHistory;
import com.mealspire.app.domain.MealHistoryStore;
import com.mealspire.app.domain.PreferenceStore;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.RecipePromptBuilder;
import com.mealspire.app.domain.RecipeRequest;
import com.mealspire.app.domain.RecipeService;
import com.mealspire.app.domain.RecipeTextParser;
import com.mealspire.app.domain.StaleMealSelector;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.net.HttpClaudeClient;
import com.mealspire.app.storage.SharedPreferencesMealHistoryStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private Button likeButton;
    private Button dislikeButton;

    private HttpClaudeClient claudeClient;
    private RecipeService recipeService;
    private PreferenceStore preferenceStore;
    private UserPreferences preferences;
    private MealHistoryStore historyStore;
    private MealHistory history;
    private final StaleMealSelector staleSelector = new StaleMealSelector();
    private final List<MealChoiceOption> choiceOptions = MealChoices.defaults();
    private final List<CheckBox> choiceBoxes = new ArrayList<>();
    private Recipe currentRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        claudeClient = new HttpClaudeClient(BuildConfig.ANTHROPIC_API_KEY);
        recipeService = new RecipeService(claudeClient, new RecipePromptBuilder(),
                new RecipeTextParser());
        preferenceStore = new SharedPreferencesPreferenceStore(this);
        preferences = preferenceStore.load();
        historyStore = new SharedPreferencesMealHistoryStore(this);
        history = historyStore.load();

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
        mealSpinner.setId(R.id.meal_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MEAL_TYPES);
        mealSpinner.setAdapter(adapter);
        root.addView(mealSpinner, marginTop(28));

        TextView choicesLabel = new TextView(this);
        choicesLabel.setText("Czego dziś szukasz? (wybierz dowolne)");
        choicesLabel.setTextSize(16);
        choicesLabel.setTextColor(Color.rgb(92, 78, 62));
        root.addView(choicesLabel, marginTop(20));

        for (MealChoiceOption option : choiceOptions) {
            CheckBox box = new CheckBox(this);
            box.setText(option.getLabel());
            box.setTextSize(16);
            box.setTextColor(Color.rgb(67, 56, 45));
            choiceBoxes.add(box);
            root.addView(box, marginTop(4));
        }

        aiButton = new Button(this);
        aiButton.setId(R.id.ai_button);
        aiButton.setText("Wymyśl danie (AI)");
        aiButton.setAllCaps(false);
        aiButton.setTextSize(18);
        aiButton.setOnClickListener(view -> generateAiRecipe());
        root.addView(aiButton, marginTop(16));

        Button drawButton = new Button(this);
        drawButton.setId(R.id.draw_button);
        drawButton.setText("Losuj gotowy przepis");
        drawButton.setAllCaps(false);
        drawButton.setTextSize(18);
        drawButton.setOnClickListener(view -> showRandomRecipe());
        root.addView(drawButton, marginTop(12));

        recipeTitle = new TextView(this);
        recipeTitle.setId(R.id.recipe_title);
        recipeTitle.setTextSize(24);
        recipeTitle.setTextColor(Color.rgb(67, 56, 45));
        root.addView(recipeTitle, marginTop(28));

        recipeDetails = new TextView(this);
        recipeDetails.setId(R.id.recipe_details);
        recipeDetails.setTextSize(17);
        recipeDetails.setLineSpacing(dp(4), 1.0f);
        recipeDetails.setTextColor(Color.rgb(80, 68, 54));
        root.addView(recipeDetails, marginTop(12));

        LinearLayout feedbackRow = new LinearLayout(this);
        feedbackRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(feedbackRow, marginTop(20));

        likeButton = new Button(this);
        likeButton.setId(R.id.like_button);
        likeButton.setText("Lubię to");
        likeButton.setAllCaps(false);
        likeButton.setTextSize(16);
        likeButton.setOnClickListener(view -> rateCurrentRecipe(true));
        feedbackRow.addView(likeButton, equalWidthRowItem());

        dislikeButton = new Button(this);
        dislikeButton.setId(R.id.dislike_button);
        dislikeButton.setText("Nie dla mnie");
        dislikeButton.setAllCaps(false);
        dislikeButton.setTextSize(16);
        dislikeButton.setOnClickListener(view -> rateCurrentRecipe(false));
        feedbackRow.addView(dislikeButton, equalWidthRowItem());

        setContentView(scrollView);
        showRandomRecipe();
    }

    private void showRandomRecipe() {
        int mealIndex = mealSpinner.getSelectedItemPosition();
        // Shuffle so ties (e.g. never-shown dishes) vary, then prefer the dish
        // not seen for the longest time.
        List<Recipe> pool = new ArrayList<>();
        Collections.addAll(pool, RECIPES[mealIndex]);
        Collections.shuffle(pool, random);

        List<String> titles = new ArrayList<>();
        for (Recipe recipe : pool) {
            titles.add(recipe.getTitle());
        }
        String stalest = staleSelector.pickStalest(titles, history);

        Recipe chosen = pool.get(0);
        for (Recipe recipe : pool) {
            if (recipe.getTitle().equals(stalest)) {
                chosen = recipe;
                break;
            }
        }
        showRecipe(chosen);
        recordChosen(chosen);
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

        // Gather the user's choices: feed them into the prompt and learn from them.
        List<MealChoiceOption> selected = selectedChoices();
        List<String> fragments = new ArrayList<>();
        for (MealChoiceOption option : selected) {
            fragments.add(option.getPromptFragment());
        }
        if (!selected.isEmpty()) {
            preferences = ChoiceLearning.learnFrom(preferences, selected);
            preferenceStore.save(preferences);
        }

        final RecipeRequest request = new RecipeRequest(
                mealType, preferences, history.recentTitles(5), fragments);
        new Thread(() -> {
            try {
                final Recipe recipe = recipeService.generateRecipe(request);
                runOnUiThread(() -> {
                    showRecipe(recipe);
                    recordChosen(recipe);
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
        currentRecipe = recipe;
        recipeTitle.setText(recipe.getTitle());
        recipeDetails.setText(recipe.getDetails());
        boolean hasDish = !recipe.getTitle().trim().isEmpty();
        likeButton.setEnabled(hasDish);
        dislikeButton.setEnabled(hasDish);
    }

    private List<MealChoiceOption> selectedChoices() {
        List<MealChoiceOption> selected = new ArrayList<>();
        for (int i = 0; i < choiceOptions.size(); i++) {
            if (choiceBoxes.get(i).isChecked()) {
                selected.add(choiceOptions.get(i));
            }
        }
        return selected;
    }

    private void recordChosen(Recipe recipe) {
        if (recipe == null || recipe.getTitle().trim().isEmpty()) {
            return;
        }
        history = history.record(recipe.getTitle(), System.currentTimeMillis());
        historyStore.save(history);
    }

    private void rateCurrentRecipe(boolean liked) {
        if (currentRecipe == null || currentRecipe.getTitle().trim().isEmpty()) {
            return;
        }
        String dish = currentRecipe.getTitle();
        preferences = liked ? preferences.withLike(dish) : preferences.withDislike(dish);
        preferenceStore.save(preferences);
        String message = liked
                ? "Zapamiętano, że lubisz: " + dish
                : "Zapamiętano, że unikasz: " + dish;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout.LayoutParams equalWidthRowItem() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        return params;
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
