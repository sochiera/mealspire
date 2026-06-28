package com.mealspire.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mealspire.app.domain.ApiKeyCipher;
import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.CookbookEntry;
import com.mealspire.app.domain.CookbookStore;
import com.mealspire.app.domain.DataManager;
import com.mealspire.app.domain.DishProposal;
import com.mealspire.app.domain.KnownDishImporter;
import com.mealspire.app.domain.KnownDishPromptBuilder;
import com.mealspire.app.domain.BuiltInRecipes;
import com.mealspire.app.domain.IngredientExtractor;
import com.mealspire.app.domain.OfflineProposalGenerator;
import com.mealspire.app.domain.PortionSize;
import com.mealspire.app.domain.MealHistory;
import com.mealspire.app.domain.MealHistoryStore;
import com.mealspire.app.domain.PreferenceStore;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.RecipePromptBuilder;
import com.mealspire.app.domain.RecipeRequest;
import com.mealspire.app.domain.RecipeService;
import com.mealspire.app.domain.RecipeTextParser;
import com.mealspire.app.domain.SecretStore;
import com.mealspire.app.domain.TasteProfile;
import com.mealspire.app.domain.TasteProfiler;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.net.HttpClaudeClient;
import com.mealspire.app.net.HttpPageFetcher;
import com.mealspire.app.notify.MealNotifications;
import com.mealspire.app.notify.MealReminderScheduler;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;
import com.mealspire.app.storage.SharedPreferencesMealHistoryStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;
import com.mealspire.app.storage.SharedPreferencesSecretStore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Thin UI layer. The user taps a meal — Śniadanie / Obiad / Kolacja — and the
 * app immediately offers a few simple proposals (name, short description, time,
 * key ingredients) at once. Tapping "Pokaż przepis" reveals the full recipe;
 * tapping "Lubię to" teaches the app the user's kitchen. The app only ever
 * remembers what the user <em>likes</em> — nothing negative — and leans on those
 * likes when suggesting the next, equally simple, everyday dishes.
 */
public class MainActivity extends Activity {
    private static final String[] MEAL_TYPES = {"Śniadanie", "Obiad", "Kolacja"};
    private static final int PROPOSAL_COUNT = 3;
    private static final int MAX_SERVINGS = 12;

    /** Intent extra: which meal to open (0=breakfast, 1=lunch, 2=dinner). */
    public static final String EXTRA_MEAL_INDEX = "meal_index";
    private static final int REQ_POST_NOTIFICATIONS = 1001;

    private final Random random = new Random();
    private TextView servingsLabel;
    private Button[] mealButtons;
    private LinearLayout contentContainer;
    private Button moreButton;

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
    private SecretStore secretStore;
    private final OfflineProposalGenerator offlineProposalGenerator = new OfflineProposalGenerator();
    private final IngredientExtractor ingredientExtractor = new IngredientExtractor();
    private final TasteProfiler tasteProfiler = new TasteProfiler();
    private final ApiKeyCipher apiKeyCipher = new ApiKeyCipher();

    private int currentMealIndex = -1;
    // The proposals currently on offer and (for the offline flow) their recipes.
    private List<DishProposal> proposals = new ArrayList<>();
    private List<Recipe> proposalRecipes = new ArrayList<>();
    // The full recipe currently open, or null while the proposal list is shown.
    private Recipe currentRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildClaudeClients(BuildConfig.ANTHROPIC_API_KEY);
        preferenceStore = new SharedPreferencesPreferenceStore(this);
        preferences = preferenceStore.load();
        historyStore = new SharedPreferencesMealHistoryStore(this);
        history = historyStore.load();
        cookbookStore = new SharedPreferencesCookbookStore(this);
        cookbook = cookbookStore.load();
        dataManager = new DataManager(preferenceStore, historyStore, cookbookStore);
        appSettings = new SharedPreferencesAppSettings(this);
        secretStore = new SharedPreferencesSecretStore(this);

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
        subtitle.setText("Na co masz dziś ochotę?");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(92, 78, 62));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle, marginTop(8));

        servingsLabel = new TextView(this);
        servingsLabel.setId(R.id.servings_label);
        servingsLabel.setTextSize(15);
        servingsLabel.setTextColor(Color.rgb(120, 104, 86));
        servingsLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(servingsLabel, marginTop(6));

        // One tap to pick the meal — replaces the old picker + generate button.
        LinearLayout mealRow = new LinearLayout(this);
        mealRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(mealRow, marginTop(20));

        int[] mealIds = {R.id.meal_breakfast_button, R.id.meal_lunch_button, R.id.meal_dinner_button};
        mealButtons = new Button[MEAL_TYPES.length];
        for (int i = 0; i < MEAL_TYPES.length; i++) {
            final int index = i;
            Button button = new Button(this);
            button.setId(mealIds[i]);
            button.setText(MEAL_TYPES[i]);
            button.setAllCaps(false);
            button.setTextSize(16);
            button.setOnClickListener(v -> selectMeal(index));
            mealButtons[i] = button;
            mealRow.addView(button, equalWidthRowItem());
        }

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentContainer, marginTop(20));

        moreButton = new Button(this);
        moreButton.setId(R.id.more_button);
        moreButton.setText("Więcej…");
        moreButton.setAllCaps(false);
        moreButton.setTextSize(16);
        moreButton.setOnClickListener(view -> showMoreMenu());
        root.addView(moreButton, marginTop(24));

        setContentView(scrollView);
        updateServingsLabel();
        showHint("Wybierz porę dnia, a podsunę kilka prostych pomysłów.");
        maybeAskServings();

        // Key sources, in order: build-time key, a key remembered from a previous
        // unlock (so the password is asked only once), then the encrypted-in-repo
        // key which still needs the password.
        if (!claudeClient.hasApiKey()) {
            if (secretStore.hasApiKey()) {
                buildClaudeClients(secretStore.loadApiKey());
            } else if (!encryptedApiKey().isEmpty()) {
                promptForApiKeyPassword(false);
            }
        }

        // Daily meal reminders (8/12/18). Scheduling is idempotent.
        MealNotifications.ensureChannel(this);
        new MealReminderScheduler().scheduleAll(this);
        maybeRequestNotificationPermission();
        handleMealIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleMealIntent(intent);
    }

    /** Opens the meal carried by a tapped reminder notification, if any. */
    private void handleMealIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        int mealIndex = intent.getIntExtra(EXTRA_MEAL_INDEX, -1);
        if (mealIndex >= 0 && mealIndex < MEAL_TYPES.length) {
            selectMeal(mealIndex);
        }
    }

    /** On Android 13+ notifications need a runtime permission; ask once. */
    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
        }
    }

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
        new Thread(() -> {
            try {
                final String key = apiKeyCipher.decrypt(blob, password);
                runOnUiThread(() -> {
                    buildClaudeClients(key);
                    // Remember the unlocked key so the password is asked only once.
                    secretStore.saveApiKey(key);
                    Toast.makeText(this, "Odblokowano AI — możesz generować dania.",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (GeneralSecurityException e) {
                runOnUiThread(() -> promptForApiKeyPassword(true));
            }
        }).start();
    }

    // ----- Meal selection + proposals -------------------------------------

    private void selectMeal(int index) {
        currentMealIndex = index;
        highlightSelectedMeal();
        generateProposals();
    }

    private void highlightSelectedMeal() {
        for (int i = 0; i < mealButtons.length; i++) {
            mealButtons[i].setTypeface(null, i == currentMealIndex ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    /** Offer a fresh set of proposals for the current meal (AI or offline). */
    private void generateProposals() {
        if (currentMealIndex < 0) {
            return;
        }
        if (claudeClient.hasApiKey()) {
            generateAiProposals();
        } else {
            generateOfflineProposals();
        }
    }

    private void generateOfflineProposals() {
        // Shared offline pipeline: pool -> shuffle -> at most one taste-led pick,
        // the rest kept varied (so liking three chicken dishes does not turn every
        // suggestion into chicken).
        List<Recipe> chosen = offlineProposalGenerator.generate(
                BuiltInRecipes.forMeal(currentMealIndex), cookbook, preferences,
                buildTasteProfile(), PROPOSAL_COUNT, random);

        List<DishProposal> newProposals = new ArrayList<>();
        List<Recipe> newRecipes = new ArrayList<>();
        for (Recipe recipe : chosen) {
            newProposals.add(proposalFromRecipe(recipe));
            newRecipes.add(recipe);
        }
        showProposals(newProposals, newRecipes);
    }

    /** Distils the user's likes into recurring "taste" terms (with known recipe details). */
    private TasteProfile buildTasteProfile() {
        return tasteProfiler.build(preferences.getLikes(),
                BuiltInRecipes.detailsByTitle(cookbook));
    }

    private DishProposal proposalFromRecipe(Recipe recipe) {
        List<String> ingredients = ingredientExtractor.extract(recipe.getDetails());
        if (ingredients.size() > 5) {
            ingredients = new ArrayList<>(ingredients.subList(0, 5));
        }
        return new DishProposal(recipe.getTitle(),
                "Proste danie z Twojej puli.", "", ingredients);
    }

    private void generateAiProposals() {
        final RecipeRequest request = buildRequest();
        setMealButtonsEnabled(false);
        showHint("Szukam pomysłów…");
        new Thread(() -> {
            try {
                final List<DishProposal> result =
                        recipeService.proposeDishes(request, PROPOSAL_COUNT);
                runOnUiThread(() -> {
                    setMealButtonsEnabled(true);
                    if (result.isEmpty()) {
                        showHint("Nie udało się wymyślić dań. Spróbuj ponownie.");
                        return;
                    }
                    List<Recipe> noRecipes = new ArrayList<>();
                    for (int i = 0; i < result.size(); i++) {
                        noRecipes.add(null); // full recipe is fetched on demand
                    }
                    showProposals(result, noRecipes);
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    setMealButtonsEnabled(true);
                    showHint(message != null ? message : "Spróbuj ponownie za chwilę.");
                });
            }
        }).start();
    }

    private RecipeRequest buildRequest() {
        final String mealType = MEAL_TYPES[currentMealIndex];
        List<String> fragments = new ArrayList<>();
        String portionFragment = PortionSize.promptFragment(appSettings.loadDefaultServings());
        if (!portionFragment.isEmpty()) {
            fragments.add(portionFragment);
        }
        List<String> knownDishes = cookbook.titles();
        if (knownDishes.size() > 10) {
            knownDishes = knownDishes.subList(0, 10);
        }
        return new RecipeRequest(mealType, preferences, history.recentTitles(8),
                fragments, knownDishes, buildTasteProfile().getAffinities());
    }

    private void showProposals(List<DishProposal> newProposals, List<Recipe> newRecipes) {
        proposals = newProposals;
        proposalRecipes = newRecipes;
        currentRecipe = null;
        for (DishProposal proposal : newProposals) {
            recordChosen(proposal.getName());
        }
        renderProposals();
    }

    private void renderProposals() {
        contentContainer.removeAllViews();
        for (int i = 0; i < proposals.size(); i++) {
            contentContainer.addView(buildProposalCard(i), marginTop(i == 0 ? 0 : 12));
        }

        Button refresh = new Button(this);
        refresh.setId(R.id.refresh_button);
        refresh.setText("Inne propozycje");
        refresh.setAllCaps(false);
        refresh.setTextSize(16);
        refresh.setOnClickListener(v -> generateProposals());
        contentContainer.addView(refresh, marginTop(16));
    }

    private View buildProposalCard(int index) {
        DishProposal proposal = proposals.get(index);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.rgb(255, 237, 213));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView name = new TextView(this);
        if (index == 0) {
            name.setId(R.id.recipe_title); // first card title is the testable anchor
        }
        name.setText(proposal.getName());
        name.setTextSize(20);
        name.setTextColor(Color.rgb(67, 56, 45));
        name.setTypeface(null, Typeface.BOLD);
        card.addView(name, matchWrap());

        String summary = proposal.summary();
        if (!summary.isEmpty()) {
            TextView body = new TextView(this);
            body.setText(summary);
            body.setTextSize(15);
            body.setTextColor(Color.rgb(80, 68, 54));
            card.addView(body, marginTop(6));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, marginTop(10));

        Button like = new Button(this);
        if (index == 0) {
            like.setId(R.id.like_button);
        }
        like.setText("Lubię to");
        like.setAllCaps(false);
        like.setTextSize(15);
        like.setOnClickListener(v -> likeProposal(index));
        actions.addView(like, equalWidthRowItem());

        Button show = new Button(this);
        if (index == 0) {
            show.setId(R.id.accept_button);
        }
        show.setText("Pokaż przepis");
        show.setAllCaps(false);
        show.setTextSize(15);
        show.setOnClickListener(v -> openRecipe(index));
        actions.addView(show, equalWidthRowItem());

        return card;
    }

    private void likeProposal(int index) {
        if (index < 0 || index >= proposals.size()) {
            return;
        }
        rememberLike(proposals.get(index).getName());
    }

    /** Reveal the full recipe for a proposal: instant offline, fetched for AI. */
    private void openRecipe(int index) {
        if (index < 0 || index >= proposals.size()) {
            return;
        }
        Recipe cached = index < proposalRecipes.size() ? proposalRecipes.get(index) : null;
        if (cached != null) {
            showFullRecipe(cached);
            return;
        }
        final String dishName = proposals.get(index).getName();
        final RecipeRequest request = buildRequest();
        showHint("Przygotowuję przepis…");
        new Thread(() -> {
            try {
                final Recipe recipe = recipeService.generateRecipeFor(dishName, request);
                runOnUiThread(() -> showFullRecipe(recipe));
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    showHint(message != null ? message
                            : "Nie udało się pobrać przepisu. Spróbuj ponownie.");
                });
            }
        }).start();
    }

    private void showFullRecipe(Recipe recipe) {
        currentRecipe = recipe;
        contentContainer.removeAllViews();

        TextView name = new TextView(this);
        name.setId(R.id.recipe_title);
        name.setText(recipe.getTitle());
        name.setTextSize(24);
        name.setTextColor(Color.rgb(67, 56, 45));
        name.setTypeface(null, Typeface.BOLD);
        contentContainer.addView(name, matchWrap());

        TextView details = new TextView(this);
        details.setId(R.id.recipe_details);
        details.setText(recipe.getDetails());
        details.setTextSize(17);
        details.setLineSpacing(dp(4), 1.0f);
        details.setTextColor(Color.rgb(80, 68, 54));
        contentContainer.addView(details, marginTop(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        contentContainer.addView(actions, marginTop(16));

        Button like = new Button(this);
        like.setId(R.id.like_button);
        like.setText("Lubię to");
        like.setAllCaps(false);
        like.setTextSize(16);
        like.setOnClickListener(v -> rememberLike(recipe.getTitle()));
        actions.addView(like, equalWidthRowItem());

        if (claudeClient.hasApiKey()) {
            Button change = new Button(this);
            change.setId(R.id.change_button);
            change.setText("Zmień przepis");
            change.setAllCaps(false);
            change.setTextSize(16);
            change.setOnClickListener(v -> showModifyRecipeDialog());
            actions.addView(change, equalWidthRowItem());
        }

        Button back = new Button(this);
        back.setId(R.id.back_button);
        back.setText("Wróć do propozycji");
        back.setAllCaps(false);
        back.setTextSize(16);
        back.setOnClickListener(v -> {
            currentRecipe = null;
            renderProposals();
        });
        contentContainer.addView(back, marginTop(12));
    }

    // ----- Likes-only learning --------------------------------------------

    private void rememberLike(String dish) {
        if (dish == null || dish.trim().isEmpty()) {
            return;
        }
        preferences = preferences.withLike(dish);
        preferenceStore.save(preferences);
        Toast.makeText(this, "Zapamiętane — lubisz: " + dish, Toast.LENGTH_SHORT).show();
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
        showHint("Zmieniam przepis…");
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
        showHint("Dodaję do bazy…");
        new Thread(() -> {
            try {
                final CookbookEntry entry = dishImporter.importDish(input);
                runOnUiThread(() -> {
                    cookbook = cookbook.add(entry);
                    cookbookStore.save(cookbook);
                    preferences = preferences.withLike(entry.getTitle());
                    preferenceStore.save(preferences);
                    showFullRecipe(entry.toRecipe());
                    Toast.makeText(MainActivity.this,
                            "Dodano do bazy: " + entry.getTitle(), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() ->
                        showHint(message != null ? message : "Nie udało się dodać dania."));
            }
        }).start();
    }

    private void showManageDialog() {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        labels.add("Pokaż i usuń dania z bazy");
        actions.add(this::showCookbookDialog);
        labels.add("Wyczyść polubione dania");
        actions.add(() -> {
            dataManager.clearPreferences();
            preferences = UserPreferences.empty();
            toast("Wyczyszczono polubione dania.");
        });
        labels.add("Wyczyść historię podpowiedzi");
        actions.add(() -> {
            dataManager.clearHistory();
            history = MealHistory.empty();
            toast("Wyczyszczono historię.");
        });
        labels.add("Wyczyść całą bazę dań");
        actions.add(() -> {
            dataManager.clearCookbook();
            cookbook = Cookbook.empty();
            toast("Wyczyszczono bazę dań.");
        });
        // Only offer "forget" when AI was unlocked with the password (not when the
        // key is baked in at build time, which clearing here would not undo).
        if (secretStore.hasApiKey() && BuildConfig.ANTHROPIC_API_KEY.isEmpty()) {
            labels.add("Zablokuj AI (zapomnij hasło)");
            actions.add(this::forgetApiKey);
        }

        new AlertDialog.Builder(this)
                .setTitle("Moje dane")
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void forgetApiKey() {
        secretStore.clear();
        buildClaudeClients(BuildConfig.ANTHROPIC_API_KEY);
        // Refresh the open recipe so the AI-only "Zmień przepis" button disappears.
        if (currentRecipe != null) {
            showFullRecipe(currentRecipe);
        }
        toast("Zablokowano AI — aplikacja działa offline. Hasło przy następnym starcie.");
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
                            if (regenerate && currentMealIndex >= 0) {
                                generateProposals();
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

    /** Shows a single informational line in the content area (no recipe yet). */
    private void showHint(String text) {
        contentContainer.removeAllViews();
        TextView hint = new TextView(this);
        hint.setText(text);
        hint.setTextSize(16);
        hint.setTextColor(Color.rgb(120, 104, 86));
        contentContainer.addView(hint, matchWrap());
    }

    private void setMealButtonsEnabled(boolean enabled) {
        for (Button button : mealButtons) {
            button.setEnabled(enabled);
        }
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
