package com.mealspire.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mealspire.app.domain.ApiKeyCipher;
import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.CookbookEntry;
import com.mealspire.app.domain.CookbookStore;
import com.mealspire.app.domain.DataManager;
import com.mealspire.app.domain.DeclineReason;
import com.mealspire.app.domain.DishProposal;
import com.mealspire.app.domain.KnownDishImporter;
import com.mealspire.app.domain.KnownDishPromptBuilder;
import com.mealspire.app.domain.IngredientExtractor;
import com.mealspire.app.domain.MealPoolBuilder;
import com.mealspire.app.domain.PortionSize;
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
import com.mealspire.app.net.HttpPageFetcher;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;
import com.mealspire.app.storage.SharedPreferencesMealHistoryStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Thin UI layer. The app proposes one dish at a time — name, short description,
 * time and key ingredients — without generating the whole recipe up front. The
 * user accepts ("Pokaż przepis") to get the full recipe, or skips ("Nie dla
 * mnie") and immediately gets a fresh proposal. A skip asks <em>why</em>, so a
 * real dislike is remembered while a "not today" reason only steers the next
 * idea. The number of people is asked once and then reused forever. Once a
 * recipe is shown it can be tweaked with a free-text request to the AI.
 */
public class MainActivity extends Activity {
    private static final String[] MEAL_TYPES = {"Śniadanie", "Obiad", "Kolacja"};
    private static final int MAX_SERVINGS = 12;

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
    private TextView servingsLabel;
    private TextView recipeTitle;
    private TextView recipeDetails;
    private Button generateButton;
    private Button acceptButton;
    private Button changeButton;
    private Button likeButton;
    private Button dislikeButton;

    private HttpClaudeClient claudeClient;
    private RecipeService recipeService;
    private PreferenceStore preferenceStore;
    private UserPreferences preferences;
    private MealHistoryStore historyStore;
    private MealHistory history;
    private CookbookStore cookbookStore;
    private Cookbook cookbook;
    private KnownDishImporter dishImporter;
    private DataManager dataManager;
    private AppSettings appSettings;
    private final StaleMealSelector staleSelector = new StaleMealSelector();
    private final MealPoolBuilder mealPoolBuilder = new MealPoolBuilder();
    private final IngredientExtractor ingredientExtractor = new IngredientExtractor();
    private final ApiKeyCipher apiKeyCipher = new ApiKeyCipher();

    // What is on screen right now. A proposal always has a name; the full recipe
    // is either resolved instantly (offline) or fetched on accept (AI).
    private DishProposal currentProposal;
    private Recipe currentRecipe;
    // Offline: the full recipe behind the current proposal, ready for instant reveal.
    private Recipe pendingRecipe;
    // Steers the next proposal after a "not today" skip (kept only for this session).
    private final Set<String> sessionHints = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The API key may be injected at build time (BuildConfig) or shipped
        // encrypted and unlocked at runtime with a password (see promptForApiKeyPassword).
        buildClaudeClients(BuildConfig.ANTHROPIC_API_KEY);
        preferenceStore = new SharedPreferencesPreferenceStore(this);
        preferences = preferenceStore.load();
        historyStore = new SharedPreferencesMealHistoryStore(this);
        history = historyStore.load();
        cookbookStore = new SharedPreferencesCookbookStore(this);
        cookbook = cookbookStore.load();
        dataManager = new DataManager(preferenceStore, historyStore, cookbookStore);
        appSettings = new SharedPreferencesAppSettings(this);

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
        subtitle.setText("Wybierz porę dnia — podsunę pomysł na danie.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(92, 78, 62));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle, marginTop(8));

        mealSpinner = new Spinner(this);
        mealSpinner.setId(R.id.meal_spinner);
        mealSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, MEAL_TYPES));
        root.addView(mealSpinner, marginTop(28));

        // The number of people is asked once (see maybeAskServings) and then only
        // shown here as a reminder; it can be changed from the "Więcej…" menu.
        servingsLabel = new TextView(this);
        servingsLabel.setId(R.id.servings_label);
        servingsLabel.setTextSize(16);
        servingsLabel.setTextColor(Color.rgb(92, 78, 62));
        root.addView(servingsLabel, marginTop(16));

        generateButton = new Button(this);
        generateButton.setId(R.id.generate_button);
        generateButton.setText("Zaproponuj danie");
        generateButton.setAllCaps(false);
        generateButton.setTextSize(20);
        generateButton.setOnClickListener(view -> generateProposal());
        root.addView(generateButton, marginTop(24));

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

        acceptButton = new Button(this);
        acceptButton.setId(R.id.accept_button);
        acceptButton.setText("Pokaż przepis");
        acceptButton.setAllCaps(false);
        acceptButton.setTextSize(18);
        acceptButton.setOnClickListener(view -> acceptProposal());
        acceptButton.setVisibility(View.GONE);
        root.addView(acceptButton, marginTop(18));

        changeButton = new Button(this);
        changeButton.setId(R.id.change_button);
        changeButton.setText("Zmień przepis");
        changeButton.setAllCaps(false);
        changeButton.setTextSize(18);
        changeButton.setOnClickListener(view -> showModifyRecipeDialog());
        changeButton.setVisibility(View.GONE);
        root.addView(changeButton, marginTop(12));

        LinearLayout feedbackRow = new LinearLayout(this);
        feedbackRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(feedbackRow, marginTop(14));

        likeButton = new Button(this);
        likeButton.setId(R.id.like_button);
        likeButton.setText("Lubię to");
        likeButton.setAllCaps(false);
        likeButton.setTextSize(16);
        likeButton.setOnClickListener(view -> likeCurrent());
        feedbackRow.addView(likeButton, equalWidthRowItem());

        dislikeButton = new Button(this);
        dislikeButton.setId(R.id.dislike_button);
        dislikeButton.setText("Nie dla mnie");
        dislikeButton.setAllCaps(false);
        dislikeButton.setTextSize(16);
        dislikeButton.setOnClickListener(view -> showDeclineReasons());
        feedbackRow.addView(dislikeButton, equalWidthRowItem());

        Button moreButton = new Button(this);
        moreButton.setId(R.id.more_button);
        moreButton.setText("Więcej…");
        moreButton.setAllCaps(false);
        moreButton.setTextSize(16);
        moreButton.setOnClickListener(view -> showMoreMenu());
        root.addView(moreButton, marginTop(20));

        setContentView(scrollView);
        updateServingsLabel();
        generateProposal();
        maybeAskServings();

        // If the key isn't already available (build-time), ask for the password
        // that unlocks the encrypted key shipped with the app.
        if (!claudeClient.hasApiKey() && !encryptedApiKey().isEmpty()) {
            promptForApiKeyPassword(false);
        }
    }

    /** (Re)creates the Claude-backed services with the given API key (may be empty). */
    private void buildClaudeClients(String apiKey) {
        claudeClient = new HttpClaudeClient(apiKey);
        recipeService = new RecipeService(claudeClient, new RecipePromptBuilder(),
                new RecipeTextParser());
        dishImporter = new KnownDishImporter(claudeClient, new HttpPageFetcher(),
                new KnownDishPromptBuilder(), new RecipeTextParser());
    }

    private String encryptedApiKey() {
        return getString(R.string.encrypted_api_key).trim();
    }

    private void promptForApiKeyPassword(boolean retry) {
        final EditText field = new EditText(this);
        field.setHint("Hasło");
        field.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle(retry ? "Niepoprawne hasło — spróbuj ponownie"
                        : "Podaj hasło, aby odblokować AI")
                .setMessage("Klucz API jest zaszyfrowany. Bez hasła aplikacja działa "
                        + "offline (losuje dania z bazy).")
                .setView(field)
                .setPositiveButton("Odblokuj", (dialog, which) ->
                        unlockApiKey(field.getText().toString()))
                .setNegativeButton("Pomiń", null)
                .show();
    }

    private void unlockApiKey(final String password) {
        if (TextUtils.isEmpty(password)) {
            promptForApiKeyPassword(true);
            return;
        }
        final String blob = encryptedApiKey();
        // PBKDF2 is intentionally slow, so derive/decrypt off the UI thread.
        new Thread(() -> {
            try {
                final String key = apiKeyCipher.decrypt(blob, password);
                runOnUiThread(() -> {
                    buildClaudeClients(key);
                    Toast.makeText(this, "Odblokowano AI — możesz generować dania.",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (GeneralSecurityException e) {
                runOnUiThread(() -> promptForApiKeyPassword(true));
            }
        }).start();
    }

    // ----- Proposals -------------------------------------------------------

    /** Main action: propose one dish (AI when possible, offline otherwise). */
    private void generateProposal() {
        if (claudeClient.hasApiKey()) {
            proposeAiDish();
        } else {
            proposeOfflineDish();
        }
    }

    private void proposeOfflineDish() {
        int mealIndex = mealSpinner.getSelectedItemPosition();
        // Pool = built-in recipes for this meal + the user's cookbook, with
        // disliked dishes excluded. Shuffle so ties vary, then prefer the dish
        // not seen for the longest time.
        List<Recipe> pool = mealPoolBuilder.build(RECIPES[mealIndex], cookbook, preferences);
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
        pendingRecipe = chosen;
        showProposal(proposalFromRecipe(chosen));
        recordChosen(chosen.getTitle());
    }

    /** Builds a lightweight proposal from a stored recipe for the offline flow. */
    private DishProposal proposalFromRecipe(Recipe recipe) {
        List<String> ingredients = ingredientExtractor.extract(recipe.getDetails());
        if (ingredients.size() > 5) {
            ingredients = new ArrayList<>(ingredients.subList(0, 5));
        }
        return new DishProposal(recipe.getTitle(),
                "Sprawdzone danie z Twojej puli.", "", ingredients);
    }

    private void proposeAiDish() {
        final RecipeRequest request = buildRequest();
        generateButton.setEnabled(false);
        recipeTitle.setText("Szukam pomysłu…");
        recipeDetails.setText("");
        setProposalActionsEnabled(false);
        new Thread(() -> {
            try {
                final DishProposal proposal = recipeService.proposeDish(request);
                runOnUiThread(() -> {
                    pendingRecipe = null;
                    showProposal(proposal);
                    recordChosen(proposal.getName());
                    generateButton.setEnabled(true);
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    recipeTitle.setText("Nie udało się wymyślić dania");
                    recipeDetails.setText(message != null ? message
                            : "Spróbuj ponownie za chwilę.");
                    generateButton.setEnabled(true);
                });
            }
        }).start();
    }

    /** Bundles the meal type, learned preferences, recent dishes and this-session hints. */
    private RecipeRequest buildRequest() {
        final String mealType = MEAL_TYPES[mealSpinner.getSelectedItemPosition()];
        List<String> fragments = new ArrayList<>();
        String portionFragment = PortionSize.promptFragment(appSettings.loadDefaultServings());
        if (!portionFragment.isEmpty()) {
            fragments.add(portionFragment);
        }
        fragments.addAll(sessionHints);

        List<String> knownDishes = cookbook.titles();
        if (knownDishes.size() > 10) {
            knownDishes = knownDishes.subList(0, 10);
        }
        return new RecipeRequest(mealType, preferences, history.recentTitles(5),
                fragments, knownDishes);
    }

    private void showProposal(DishProposal proposal) {
        currentProposal = proposal;
        currentRecipe = pendingRecipe; // null for AI proposals (no full recipe yet)
        recipeTitle.setText(proposal.getName());
        recipeDetails.setText(proposal.summary());
        boolean hasDish = !proposal.isEmpty();
        acceptButton.setVisibility(hasDish ? View.VISIBLE : View.GONE);
        acceptButton.setEnabled(hasDish);
        changeButton.setVisibility(View.GONE);
        setProposalActionsEnabled(hasDish);
    }

    /** Accept: reveal the full recipe (instant offline, fetched for AI proposals). */
    private void acceptProposal() {
        if (currentProposal == null || currentProposal.isEmpty()) {
            return;
        }
        if (pendingRecipe != null) {
            showFullRecipe(pendingRecipe);
            return;
        }
        final String dishName = currentProposal.getName();
        final RecipeRequest request = buildRequest();
        acceptButton.setEnabled(false);
        recipeDetails.setText("Przygotowuję przepis…");
        new Thread(() -> {
            try {
                final Recipe recipe = recipeService.generateRecipeFor(dishName, request);
                runOnUiThread(() -> showFullRecipe(recipe));
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    recipeDetails.setText(message != null ? message
                            : "Nie udało się pobrać przepisu. Spróbuj ponownie.");
                    acceptButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void showFullRecipe(Recipe recipe) {
        currentRecipe = recipe;
        // From now on the shown dish is the recipe itself (it may have been
        // revised), so feedback should track its title rather than the proposal.
        currentProposal = null;
        recipeTitle.setText(recipe.getTitle());
        recipeDetails.setText(recipe.getDetails());
        acceptButton.setVisibility(View.GONE);
        boolean canModify = claudeClient.hasApiKey();
        changeButton.setVisibility(canModify ? View.VISIBLE : View.GONE);
        changeButton.setEnabled(canModify);
        setProposalActionsEnabled(true);
        // Committing to a dish clears the temporary "not today" steering.
        sessionHints.clear();
    }

    // ----- Skip with a reason ---------------------------------------------

    private void showDeclineReasons() {
        if (!hasCurrentDish()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Dlaczego nie dziś?")
                .setItems(DeclineReason.labels(), (dialog, which) ->
                        applyDecline(DeclineReason.values()[which]))
                .show();
    }

    private void applyDecline(DeclineReason reason) {
        String dish = currentDishName();
        if (reason.isPermanentDislike()) {
            if (!dish.isEmpty()) {
                preferences = preferences.withDislike(dish);
                preferenceStore.save(preferences);
            }
            toast("Zapamiętano, że unikasz: " + dish);
        } else {
            sessionHints.add(reason.getPromptHint());
        }
        // Every click has an action: immediately offer a fresh proposal.
        generateProposal();
    }

    // ----- Modify the shown recipe ----------------------------------------

    private void showModifyRecipeDialog() {
        if (currentRecipe == null || currentRecipe.getTitle().trim().isEmpty()) {
            return;
        }
        if (!claudeClient.hasApiKey()) {
            Toast.makeText(this, "Zmiana przepisu wymaga klucza API.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final EditText field = new EditText(this);
        field.setHint("np. nie mam jogurtu — czym zastąpić?");
        field.setTextSize(16);
        new AlertDialog.Builder(this)
                .setTitle("Zmień przepis")
                .setMessage("Napisz, co zmienić — AI dopasuje przepis.")
                .setView(field)
                .setPositiveButton("Zmień", (dialog, which) ->
                        modifyRecipe(field.getText().toString().trim()))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void modifyRecipe(final String instruction) {
        if (TextUtils.isEmpty(instruction) || currentRecipe == null) {
            return;
        }
        final Recipe base = currentRecipe;
        changeButton.setEnabled(false);
        recipeDetails.setText("Zmieniam przepis…");
        new Thread(() -> {
            try {
                final Recipe revised = recipeService.modifyRecipe(base, instruction);
                runOnUiThread(() -> showFullRecipe(revised));
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    showFullRecipe(base);
                    Toast.makeText(MainActivity.this, message != null ? message
                            : "Nie udało się zmienić przepisu.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ----- Likes -----------------------------------------------------------

    private void likeCurrent() {
        if (!hasCurrentDish()) {
            return;
        }
        String dish = currentDishName();
        preferences = preferences.withLike(dish);
        preferenceStore.save(preferences);
        Toast.makeText(this, "Zapamiętano, że lubisz: " + dish, Toast.LENGTH_SHORT).show();
    }

    // ----- "Więcej…" menu --------------------------------------------------

    private void showMoreMenu() {
        boolean hasRecipe = currentRecipe != null && !currentRecipe.getTitle().trim().isEmpty();
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        if (hasRecipe) {
            labels.add("Zapisz danie do mojej bazy");
            actions.add(this::saveCurrentToCookbook);
            labels.add("Lista zakupów");
            actions.add(this::showShoppingList);
        }
        labels.add("Zmień liczbę osób");
        actions.add(() -> showServingsDialog(true));
        labels.add("Dodaj danie, które znasz i lubisz");
        actions.add(this::showAddKnownDishDialog);
        labels.add("Zarządzaj moimi danymi");
        actions.add(this::showManageDialog);

        new AlertDialog.Builder(this)
                .setTitle("Więcej")
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void showAddKnownDishDialog() {
        if (!claudeClient.hasApiKey()) {
            Toast.makeText(this, "Dodawanie z linku/opisu wymaga klucza API.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final EditText field = new EditText(this);
        field.setHint("Wklej link do przepisu albo opisz danie");
        field.setTextSize(16);
        new AlertDialog.Builder(this)
                .setTitle("Dodaj danie, które znasz")
                .setView(field)
                .setPositiveButton("Dodaj", (dialog, which) ->
                        importKnownDish(field.getText().toString().trim()))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void importKnownDish(final String input) {
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Wklej link albo opisz danie.", Toast.LENGTH_SHORT).show();
            return;
        }

        recipeTitle.setText("Dodaję do bazy…");
        recipeDetails.setText("");

        new Thread(() -> {
            try {
                final CookbookEntry entry = dishImporter.importDish(input);
                runOnUiThread(() -> {
                    cookbook = cookbook.add(entry);
                    cookbookStore.save(cookbook);
                    preferences = preferences.withLike(entry.getTitle());
                    preferenceStore.save(preferences);
                    pendingRecipe = entry.toRecipe();
                    showFullRecipe(entry.toRecipe());
                    Toast.makeText(MainActivity.this,
                            "Dodano do bazy: " + entry.getTitle(), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    recipeTitle.setText("Nie udało się dodać dania");
                    recipeDetails.setText(message != null ? message : "Spróbuj ponownie.");
                });
            }
        }).start();
    }

    private void showManageDialog() {
        final String[] options = {
                "Pokaż i usuń dania z bazy",
                "Wyczyść preferencje (lubię / nie lubię)",
                "Wyczyść historię podpowiedzi",
                "Wyczyść całą bazę dań"
        };
        new AlertDialog.Builder(this)
                .setTitle("Moje dane")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCookbookDialog();
                            break;
                        case 1:
                            dataManager.clearPreferences();
                            preferences = UserPreferences.empty();
                            toast("Wyczyszczono preferencje.");
                            break;
                        case 2:
                            dataManager.clearHistory();
                            history = MealHistory.empty();
                            toast("Wyczyszczono historię.");
                            break;
                        case 3:
                            dataManager.clearCookbook();
                            cookbook = Cookbook.empty();
                            toast("Wyczyszczono bazę dań.");
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void showCookbookDialog() {
        final String[] titles = cookbook.titles().toArray(new String[0]);
        if (titles.length == 0) {
            toast("Twoja baza jest pusta.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Dotknij danie, aby je usunąć")
                .setItems(titles, (dialog, which) -> {
                    cookbook = dataManager.removeDish(titles[which]);
                    toast("Usunięto z bazy: " + titles[which]);
                })
                .show();
    }

    private void showShoppingList() {
        if (currentRecipe == null) {
            return;
        }
        List<String> items = ingredientExtractor.extract(currentRecipe.getDetails());
        if (items.isEmpty()) {
            Toast.makeText(this, "Nie znalazłam listy składników w tym przepisie.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String[] itemsArray = items.toArray(new String[0]);
        boolean[] checked = new boolean[itemsArray.length];
        new AlertDialog.Builder(this)
                .setTitle("Lista zakupów")
                .setMultiChoiceItems(itemsArray, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Gotowe", null)
                .show();
    }

    private void saveCurrentToCookbook() {
        if (currentRecipe == null || currentRecipe.getTitle().trim().isEmpty()) {
            return;
        }
        cookbook = cookbook.add(new CookbookEntry(
                currentRecipe.getTitle(), currentRecipe.getDetails(), "zapisane"));
        cookbookStore.save(cookbook);
        Toast.makeText(this, "Zapisano w bazie: " + currentRecipe.getTitle(),
                Toast.LENGTH_SHORT).show();
    }

    // ----- Servings (asked once, then remembered forever) ------------------

    private void maybeAskServings() {
        if (!appSettings.hasChosenServings()) {
            showServingsDialog(false);
        }
    }

    /** @param regenerate whether to refresh the proposal after a change. */
    private void showServingsDialog(final boolean regenerate) {
        final String[] options = new String[MAX_SERVINGS];
        for (int i = 0; i < MAX_SERVINGS; i++) {
            options[i] = servingsLabelText(i + 1);
        }
        int current = appSettings.loadDefaultServings();
        new AlertDialog.Builder(this)
                .setTitle("Dla ilu osób gotujesz?")
                .setSingleChoiceItems(options, Math.max(0, Math.min(MAX_SERVINGS - 1, current - 1)),
                        (dialog, which) -> {
                            appSettings.saveDefaultServings(which + 1);
                            updateServingsLabel();
                            dialog.dismiss();
                            if (regenerate) {
                                generateProposal();
                            }
                        })
                .show();
    }

    private void updateServingsLabel() {
        servingsLabel.setText("Gotuję dla " + servingsLabelText(appSettings.loadDefaultServings()));
    }

    private static String servingsLabelText(int servings) {
        // Genitive ("dla …"): "1 osoby", but "2/3/…/12 osób".
        return servings == 1 ? "1 osoby" : servings + " osób";
    }

    // ----- Shared helpers --------------------------------------------------

    private boolean hasCurrentDish() {
        return !currentDishName().isEmpty();
    }

    private String currentDishName() {
        if (currentProposal != null && !currentProposal.isEmpty()) {
            return currentProposal.getName();
        }
        if (currentRecipe != null) {
            return currentRecipe.getTitle().trim();
        }
        return "";
    }

    private void setProposalActionsEnabled(boolean enabled) {
        likeButton.setEnabled(enabled);
        dislikeButton.setEnabled(enabled);
    }

    private void recordChosen(String dishTitle) {
        if (dishTitle == null || dishTitle.trim().isEmpty()) {
            return;
        }
        history = history.record(dishTitle, System.currentTimeMillis());
        historyStore.save(history);
    }

    private void toast(String message) {
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
