package com.mealspire.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mealspire.app.domain.ChatGptAccount;
import com.mealspire.app.domain.ChatGptLlmClient;
import com.mealspire.app.domain.ChatGptOAuth;
import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.domain.BackNavigation;
import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.CookbookEntry;
import com.mealspire.app.domain.CookbookStore;
import com.mealspire.app.domain.DataManager;
import com.mealspire.app.domain.DietConstraints;
import com.mealspire.app.domain.DishProposal;
import com.mealspire.app.domain.DishRatingParser;
import com.mealspire.app.domain.DishRatingPromptBuilder;
import com.mealspire.app.domain.DishReaction;
import com.mealspire.app.domain.DishReactionLog;
import com.mealspire.app.domain.DishReactionStore;
import com.mealspire.app.domain.DishRecommender;
import com.mealspire.app.domain.MealPoolBuilder;
import com.mealspire.app.domain.HouseholdProfile;
import com.mealspire.app.domain.HouseholdProfileStore;
import com.mealspire.app.domain.KnownDishImporter;
import com.mealspire.app.domain.KnownDishPromptBuilder;
import com.mealspire.app.domain.LearningStats;
import com.mealspire.app.domain.LearningStatsStore;
import com.mealspire.app.domain.BuiltInRecipes;
import com.mealspire.app.domain.ContrastiveDishSampler;
import com.mealspire.app.domain.IngredientExtractor;
import com.mealspire.app.domain.OfflineProposalGenerator;
import com.mealspire.app.domain.PortionSize;
import com.mealspire.app.domain.ProposalValidator;
import com.mealspire.app.domain.MealHistory;
import com.mealspire.app.domain.MealHistoryStore;
import com.mealspire.app.domain.PreferenceStore;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.RecipePromptBuilder;
import com.mealspire.app.domain.RecipeRequest;
import com.mealspire.app.domain.RecipeService;
import com.mealspire.app.domain.RecipeTextParser;
import com.mealspire.app.domain.DishTagger;
import com.mealspire.app.domain.FrozenTasteAggregate;
import com.mealspire.app.domain.MonotonyDetector;
import com.mealspire.app.domain.TasteContextBuilder;
import com.mealspire.app.domain.TasteDimension;
import com.mealspire.app.domain.TasteEvent;
import com.mealspire.app.domain.TasteEventCompactor;
import com.mealspire.app.domain.TasteEventLog;
import com.mealspire.app.domain.TasteEventMigration;
import com.mealspire.app.domain.TasteEventStore;
import com.mealspire.app.domain.TasteModel;
import com.mealspire.app.domain.TasteProfile;
import com.mealspire.app.domain.TasteProfiler;
import com.mealspire.app.domain.UpdateChecker;
import com.mealspire.app.domain.UpdateStateStore;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.domain.VersionInfoParser;
import com.mealspire.app.domain.VersionJsonSource;
import com.mealspire.app.domain.GptModel;
import com.mealspire.app.domain.IdTokenVerifier;
import com.mealspire.app.domain.LlmClient;
import com.mealspire.app.net.HttpUrlTransport;
import com.mealspire.app.net.LoopbackCallbackServer;
import com.mealspire.app.net.HttpPageFetcher;
import com.mealspire.app.net.HttpVersionJsonFetcher;
import com.mealspire.app.notify.MealNotifications;
import com.mealspire.app.notify.MealReminderScheduler;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;
import com.mealspire.app.storage.SharedPreferencesDishReactionStore;
import com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore;
import com.mealspire.app.storage.SharedPreferencesMealHistoryStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;
import com.mealspire.app.storage.SharedPreferencesLearningStatsStore;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;
import com.mealspire.app.storage.SharedPreferencesTasteEventStore;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

/**
 * Thin UI layer. The user taps a meal — Śniadanie / Obiad / Kolacja — and the
 * app immediately offers a few simple proposals (name, short description, time,
 * key ingredients) at once. Tapping "Pokaż przepis" reveals the full recipe;
 * tapping "Lubię to" teaches the app the user's kitchen. The app only ever
 * remembers what the user <em>likes</em> — nothing negative — and leans on those
 * likes when suggesting the next, equally simple, everyday dishes.
 */
public class MainActivity extends Activity {
    // Warm palette shared by the whole UI (programmatic views, no XML layouts).
    // Keep in sync with values/styles.xml (dialog accent) and the launcher icon.
    private static final int COLOR_BACKGROUND = Color.rgb(255, 247, 237);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_OUTLINE = Color.rgb(243, 222, 195);
    private static final int COLOR_INK = Color.rgb(67, 56, 45);
    private static final int COLOR_INK_BODY = Color.rgb(80, 68, 54);
    private static final int COLOR_INK_SOFT = Color.rgb(120, 104, 86);
    private static final int COLOR_ACCENT = Color.rgb(234, 88, 12);
    private static final int COLOR_ACCENT_DEEP = Color.rgb(154, 52, 18);
    private static final int COLOR_ACCENT_SOFT = Color.rgb(255, 237, 213);
    private static final int RIPPLE_ON_ACCENT = Color.argb(64, 255, 255, 255);
    private static final int RIPPLE_ON_LIGHT = Color.argb(38, 234, 88, 12);
    private static final int BUTTON_CORNER_DP = 24;

    private static final String[] MEAL_TYPES = {"Śniadanie", "Obiad", "Kolacja"};
    private static final int PROPOSAL_COUNT = 3;
    private static final int MAX_SERVINGS = 12;
    // Odpowiedzi profilu domowników — wspólne dla quizu startowego i dialogów
    // „Profil domowników", żeby oba miejsca zawsze pokazywały to samo.
    private static final String[] CUISINE_OPTIONS =
            {"polska", "włoska", "azjatycka", "meksykańska", "bliskowschodnia"};
    private static final String[] AUDIENCE_LABELS = {"Tylko dorośli", "Dorośli i dzieci"};
    private static final HouseholdProfile.Audience[] AUDIENCE_VALUES = {
            HouseholdProfile.Audience.ADULTS_ONLY,
            HouseholdProfile.Audience.WITH_CHILDREN};
    private static final String[] SKILL_LABELS =
            {"Dopiero zaczynam", "Radzę sobie", "Gotuję dobrze i lubię wyzwania"};
    private static final HouseholdProfile.CookingSkill[] SKILL_VALUES = {
            HouseholdProfile.CookingSkill.BEGINNER,
            HouseholdProfile.CookingSkill.COMFORTABLE,
            HouseholdProfile.CookingSkill.CONFIDENT};
    private static final String[] TIME_LABELS =
            {"Do 20 minut", "Około pół godziny", "Godzina i więcej"};
    private static final HouseholdProfile.CookingTime[] TIME_VALUES = {
            HouseholdProfile.CookingTime.QUICK,
            HouseholdProfile.CookingTime.MEDIUM,
            HouseholdProfile.CookingTime.LONG};
    private static final int ONBOARDING_QUESTIONS = 4;
    private static final int ONBOARDING_DISH_ROUNDS = ContrastiveDishSampler.rounds();
    private static final int ONBOARDING_STEPS = ONBOARDING_QUESTIONS + ONBOARDING_DISH_ROUNDS;

    /** Intent extra: which meal to open (0=breakfast, 1=lunch, 2=dinner). */
    public static final String EXTRA_MEAL_INDEX = "meal_index";
    private static final int REQ_POST_NOTIFICATIONS = 1001;
    static final String SIGN_IN_DIALOG_TITLE = "Zaloguj się kontem ChatGPT";
    static final String SIGN_IN_MENU_LABEL = "Zaloguj się kontem ChatGPT";
    static final String MODEL_MENU_PREFIX = "Model AI: ";
    private static final int SIGN_IN_TIMEOUT_MS = 5 * 60 * 1000;

    // Test seams (package-private, null in production): replace the network
    // source and the background executor so Robolectric tests stay offline and
    // deterministic. Never assign these from production code.
    static VersionJsonSource versionJsonSourceOverride;
    static Executor updateCheckExecutorOverride;

    private final Random random = new Random();
    private TextView servingsLabel;
    private Button[] mealButtons;
    private LinearLayout contentContainer;
    private Button moreButton;

    private ChatGptAccount chatGptAccount;
    // Guards against a second browser sign-in while one is waiting for its redirect.
    private volatile boolean chatGptSignInRunning;
    private RecipeService recipeService;
    private DishRecommender dishRecommender;
    private PreferenceStore preferenceStore;
    private UserPreferences preferences;
    private MealHistoryStore historyStore;
    private MealHistory history;
    private CookbookStore cookbookStore;
    private Cookbook cookbook;
    private KnownDishImporter dishImporter;
    private DataManager dataManager;
    private HouseholdProfileStore householdProfileStore;
    private HouseholdProfile householdProfile;
    private AppSettings appSettings;
    private TasteEventStore tasteEventStore;
    private TasteEventLog tasteEvents;
    private FrozenTasteAggregate tasteAggregate;
    private LearningStatsStore learningStatsStore;
    private LearningStats learningStats;
    // Jawne reakcje lubię/nie lubię — jedyne wejście oceny gustu przez LLM.
    private DishReactionStore reactionStore;
    private DishReactionLog reactions;
    // Implicit signals fire once per trio: the first opened recipe decides the
    // SHOWN_NOT_CHOSEN losers, the first engagement bumps the stats, and each
    // dish counts as "viewed" at most once (re-opening must not inflate taste).
    private boolean trioChoiceRecorded;
    private boolean trioEngagementCounted;
    private final java.util.Set<String> viewedInTrio = new java.util.HashSet<>();
    private final DishTagger dishTagger = new DishTagger();
    private final TasteContextBuilder tasteContextBuilder = new TasteContextBuilder();
    private final TasteEventCompactor tasteEventCompactor = new TasteEventCompactor();
    private final MonotonyDetector monotonyDetector = new MonotonyDetector();
    private final OfflineProposalGenerator offlineProposalGenerator = new OfflineProposalGenerator();
    private final ProposalValidator proposalValidator = new ProposalValidator();
    private final IngredientExtractor ingredientExtractor = new IngredientExtractor();
    private final TasteProfiler tasteProfiler = new TasteProfiler();
    private final MealPoolBuilder mealPoolBuilder = new MealPoolBuilder();
    private final UpdateChecker updateChecker = new UpdateChecker();
    private final VersionInfoParser versionInfoParser = new VersionInfoParser();
    private UpdateStateStore updateStateStore;

    private int currentMealIndex = -1;
    // The proposals currently on offer and (for the offline flow) their recipes.
    private List<DishProposal> proposals = new ArrayList<>();
    private List<Recipe> proposalRecipes = new ArrayList<>();
    // Jednozdaniowe uzasadnienie modelu dla każdej propozycji ("" = offline).
    private List<String> proposalReasons = new ArrayList<>();
    // The full recipe currently open, or null while the proposal list is shown.
    private Recipe currentRecipe;
    // Whether the open recipe was reached from the proposal list; a recipe from
    // elsewhere (e.g. "Dodaj danie" import) has no proposals to go back to.
    private boolean recipeFromProposals;
    // Bumped whenever the user changes what the content area shows, so an async
    // answer that comes back late (recipe fetch, proposals, modification) cannot
    // stomp whatever the user is looking at now.
    private int contentEpoch;
    // Which screen the content area currently shows; drives the system back button.
    private BackNavigation.Screen currentScreen = BackNavigation.Screen.START;
    // First-launch taste quiz: current question (-1 = quiz not running), the
    // sampled dish rounds, and a meal tapped in a notification to open once the
    // quiz is finished or skipped. Held in fields (not saved state) because the
    // manifest's configChanges keeps the Activity alive across rotation.
    private final ContrastiveDishSampler onboardingDishSampler = new ContrastiveDishSampler();
    private int onboardingStep = -1;
    private List<List<Recipe>> onboardingRounds;
    // Diet the rounds were sampled with; a change (via back navigation) resamples.
    private String onboardingRoundsDietKey;
    // The dish picked in each quiz round. Persisted as likes only when the quiz
    // ends, so going back and re-picking replaces the choice instead of
    // accumulating extra likes.
    private final String[] onboardingPicks = new String[ONBOARDING_DISH_ROUNDS];
    private int pendingMealIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildLlmClients();
        preferenceStore = new SharedPreferencesPreferenceStore(this);
        preferences = preferenceStore.load();
        historyStore = new SharedPreferencesMealHistoryStore(this);
        history = historyStore.load();
        cookbookStore = new SharedPreferencesCookbookStore(this);
        cookbook = cookbookStore.load();
        reactionStore = new SharedPreferencesDishReactionStore(this);
        reactions = reactionStore.load();
        dataManager = new DataManager(preferenceStore, historyStore, cookbookStore,
                reactionStore);
        householdProfileStore = new SharedPreferencesHouseholdProfileStore(this);
        householdProfile = householdProfileStore.load();
        appSettings = new SharedPreferencesAppSettings(this);
        tasteEventStore = new SharedPreferencesTasteEventStore(this);
        tasteEvents = tasteEventStore.load();
        tasteAggregate = tasteEventStore.loadAggregate();
        learningStatsStore = new SharedPreferencesLearningStatsStore(this);
        learningStats = learningStatsStore.load();
        updateStateStore = new SharedPreferencesUpdateStateStore(this);
        // Polubienia sprzed ery dziennika stają się zdarzeniami LIKED (raz).
        TasteEventLog migrated = TasteEventMigration.migrate(
                tasteEvents, preferences, System.currentTimeMillis());
        if (migrated != tasteEvents) {
            tasteEvents = migrated;
            tasteEventStore.save(tasteEvents);
        }
        // Polubienia sprzed listy reakcji przechodzą do niej raz, przy aktualizacji.
        if (reactions.isEmpty() && !preferences.getLikes().isEmpty()) {
            reactions = DishReactionLog.fromLikes(preferences.getLikes(),
                    BuiltInRecipes.detailsByTitle(cookbook), System.currentTimeMillis());
            reactionStore.save(reactions);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Mealspire");
        title.setTextSize(34);
        title.setTextColor(COLOR_INK);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
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
        servingsLabel.setTextColor(COLOR_INK_SOFT);
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
            button.setTextSize(16);
            styleTonalButton(button);
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
        moreButton.setTextSize(16);
        styleGhostButton(moreButton);
        moreButton.setOnClickListener(view -> showMoreMenu());
        root.addView(moreButton, marginTop(24));

        setContentView(scrollView);
        updateServingsLabel();

        if (appSettings.isOnboardingDone()) {
            showStartScreen();
            showStartupPrompts();
            maybeCheckForUpdate();
        } else {
            // Fresh install: a short taste quiz first; every one-time dialog
            // (servings, ChatGPT sign-in, notification permission) waits until
            // it is finished or skipped.
            startOnboarding();
        }

        // Daily meal reminders (8/12/18). Scheduling is idempotent.
        MealNotifications.ensureChannel(this);
        new MealReminderScheduler().scheduleAll(this);
        handleMealIntent(getIntent());
    }

    /**
     * One-time prompts asked outside the quiz, so nothing covers the first
     * question: the servings dialog, the ChatGPT sign-in offer (only while
     * signed out) and the Android 13+ notification permission.
     */
    private void showStartupPrompts() {
        maybeAskServings();
        if (!chatGptAccount.isSignedIn()) {
            offerChatGptSignIn();
        }
        maybeRequestNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleMealIntent(intent);
    }

    /**
     * The system back button walks one view back — recipe → proposals → start
     * screen — instead of closing the app. The decision itself lives in the
     * domain ({@link BackNavigation}); here we only track the screen and apply
     * the transition, bumping the epoch so an in-flight async answer cannot
     * stomp the view the user went back to.
     */
    @Override
    public void onBackPressed() {
        // The quiz-active state has a single source of truth (onboardingStep);
        // the ONBOARDING screen is derived from it, never stored.
        BackNavigation.Screen screen = isOnboardingActive()
                ? BackNavigation.Screen.ONBOARDING : currentScreen;
        switch (BackNavigation.onBack(screen, onboardingStep,
                recipeFromProposals && !proposals.isEmpty())) {
            case SHOW_PROPOSALS:
                currentRecipe = null;
                contentEpoch++;
                setMealButtonsEnabled(true);
                renderProposals();
                break;
            case SHOW_START:
                showStartScreen();
                break;
            case ONBOARDING_PREVIOUS:
                onboardingStep--;
                renderOnboardingStep();
                break;
            case ONBOARDING_SKIP:
                endOnboarding();
                break;
            case EXIT:
            default:
                super.onBackPressed();
                break;
        }
    }

    /** Resets the content area to the initial "pick a meal" hint. */
    private void showStartScreen() {
        contentEpoch++;
        currentRecipe = null;
        currentMealIndex = -1;
        highlightSelectedMeal();
        setMealButtonsEnabled(true);
        currentScreen = BackNavigation.Screen.START;
        showHint("Wybierz porę dnia, a podsunę kilka prostych pomysłów.");
        maybeShowUpdateBanner();
    }

    /**
     * Checks the repo for a newer release at most once a day. The decision and
     * comparison are pure ({@link UpdateChecker}); here we run the fetch off the
     * main thread and, on success, refresh the start screen so the banner
     * appears. Any failure is swallowed — updates are not a critical feature —
     * but the attempt is still recorded so we don't hammer the network offline.
     */
    private void maybeCheckForUpdate() {
        final long now = System.currentTimeMillis();
        if (!updateChecker.shouldCheck(updateStateStore.loadLastCheckMillis(now), now)) {
            return;
        }
        final int epoch = contentEpoch;
        final VersionJsonSource source = versionJsonSourceOverride != null
                ? versionJsonSourceOverride : new HttpVersionJsonFetcher();
        updateCheckExecutor().execute(() -> {
            VersionInfo fetched = null;
            try {
                fetched = versionInfoParser.parse(source.fetchJson());
            } catch (IOException ignored) {
                // Brak sieci / błąd HTTP — próbę i tak odnotowujemy niżej.
            }
            final VersionInfo result = fetched;
            runOnUiThread(() -> {
                updateStateStore.saveLastCheckMillis(now);
                if (result != null) {
                    updateStateStore.saveLatestKnown(result);
                }
                // Odśwież baner tylko jeśli użytkownik wciąż jest na ekranie startowym.
                if (currentScreen == BackNavigation.Screen.START && epoch == contentEpoch) {
                    showStartScreen();
                }
            });
        });
    }

    private Executor updateCheckExecutor() {
        return updateCheckExecutorOverride != null
                ? updateCheckExecutorOverride
                : runnable -> new Thread(runnable).start();
    }

    /** Adds a tappable "new version available" banner atop the start screen. */
    private void maybeShowUpdateBanner() {
        VersionInfo latest = updateStateStore.loadLatestKnown();
        if (!updateChecker.isUpdateAvailable(BuildConfig.VERSION_CODE, latest)) {
            return;
        }
        Button banner = new Button(this);
        banner.setId(R.id.update_banner);
        banner.setText("Dostępna nowa wersja " + latest.getVersionName()
                + " — dotknij, aby pobrać");
        banner.setTextSize(15);
        styleTonalButton(banner);
        banner.setOnClickListener(v -> startActivity(
                com.mealspire.app.update.UpdateActivity.intent(this, latest)));
        contentContainer.addView(banner, 0, matchWrap());
    }

    /** Opens the meal carried by a tapped reminder notification, if any. */
    private void handleMealIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        int mealIndex = intent.getIntExtra(EXTRA_MEAL_INDEX, -1);
        if (mealIndex >= 0 && mealIndex < MEAL_TYPES.length) {
            // Consume the extra so a later re-delivery of the same intent (e.g.
            // activity recreation) doesn't force the meal selection again.
            intent.removeExtra(EXTRA_MEAL_INDEX);
            if (isOnboardingActive()) {
                // The quiz comes first; the tapped meal opens right after it.
                pendingMealIndex = mealIndex;
            } else {
                selectMeal(mealIndex);
            }
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

    private void buildLlmClients() {
        HttpUrlTransport transport = new HttpUrlTransport();
        chatGptAccount = new ChatGptAccount(new SharedPreferencesChatGptSessionStore(this),
                transport, new ChatGptOAuth(new SecureRandom()), new IdTokenVerifier(),
                System::currentTimeMillis);
        LlmClient llmClient = new ChatGptLlmClient(chatGptAccount, transport);
        recipeService = new RecipeService(llmClient, new RecipePromptBuilder(),
                new RecipeTextParser());
        dishRecommender = new DishRecommender(llmClient, new DishRatingPromptBuilder(),
                new DishRatingParser());
        dishImporter = new KnownDishImporter(llmClient, new HttpPageFetcher(),
                new KnownDishPromptBuilder(), new RecipeTextParser());
    }

    private boolean isAiAvailable() {
        return chatGptAccount.isSignedIn();
    }

    private void offerChatGptSignIn() {
        new AlertDialog.Builder(this)
                .setTitle(SIGN_IN_DIALOG_TITLE)
                .setMessage("AI korzysta z Twojego konta ChatGPT (plan Plus lub Pro) — "
                        + "logujesz się w przeglądarce, aplikacja nie ma własnego klucza. "
                        + "Bez logowania aplikacja działa offline (losuje dania z bazy).")
                .setPositiveButton("Zaloguj się", (dialog, which) -> startChatGptSignIn())
                .setNegativeButton("Pomiń", null)
                .show();
    }

    /**
     * Sign in with ChatGPT: a loopback listener catches the browser redirect,
     * then the code is exchanged (PKCE) for the user's own tokens.
     */
    private void startChatGptSignIn() {
        if (chatGptSignInRunning) {
            toast("Logowanie do ChatGPT już trwa — dokończ je w przeglądarce.");
            return;
        }
        chatGptSignInRunning = true;
        new Thread(() -> {
            try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
                final ChatGptOAuth.PendingSignIn pending =
                        chatGptAccount.beginSignIn(server.redirectUri());
                runOnUiThread(() -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(pending.authorizeUrl)));
                    } catch (android.content.ActivityNotFoundException e) {
                        toast("Brak przeglądarki do zalogowania w ChatGPT.");
                    }
                });
                String query = server.awaitCallbackQuery(SIGN_IN_TIMEOUT_MS);
                final String email = chatGptAccount.completeSignIn(pending, query).email;
                runOnUiThread(() -> {
                    // Refresh the open recipe so the AI-only "Zmień przepis" button appears.
                    if (currentRecipe != null) {
                        showFullRecipe(currentRecipe);
                    }
                    toast(email.isEmpty() ? "Zalogowano do ChatGPT — możesz generować dania."
                            : "Zalogowano do ChatGPT jako " + email + ".");
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> toast(message != null ? message
                        : "Logowanie do ChatGPT nie powiodło się."));
            } finally {
                chatGptSignInRunning = false;
            }
        }).start();
    }

    /** GPT Luna (default) or GPT Sol; a model missing on the account is marked, not swapped. */
    private void showModelChoiceDialog() {
        final GptModel[] models = GptModel.values();
        String[] labels = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            labels[i] = models[i].label + (models[i] == GptModel.DEFAULT ? " (domyślny)" : "")
                    + (chatGptAccount.isAvailable(models[i]) ? "" : " — niedostępny na Twoim koncie");
        }
        new AlertDialog.Builder(this)
                .setTitle("Model AI")
                .setSingleChoiceItems(labels, chatGptAccount.modelChoice().ordinal(),
                        (dialog, which) -> {
                            chatGptAccount.setModelChoice(models[which]);
                            dialog.dismiss();
                            toast("AI używa teraz modelu " + models[which].label + ".");
                        })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void signOutOfChatGpt() {
        new Thread(chatGptAccount::signOut).start();
        // Refresh the open recipe so the AI-only "Zmień przepis" button disappears.
        if (currentRecipe != null) {
            showFullRecipe(currentRecipe);
        }
        toast("Wylogowano z ChatGPT — aplikacja działa offline.");
    }

    // ----- First-launch onboarding quiz ------------------------------------

    private boolean isOnboardingActive() {
        return onboardingStep >= 0;
    }

    /**
     * A short taste quiz on a fresh install: who the user cooks for, diet
     * exclusions, weekday cooking time, cooking skill, then three contrastive
     * "which dish appeals most?" rounds saved as ordinary likes. Every step can
     * be skipped; answers given so far are kept either way.
     */
    private void startOnboarding() {
        onboardingRounds = null;
        onboardingRoundsDietKey = null;
        onboardingStep = 0;
        setMealButtonsEnabled(false);
        setEnabledWithFade(moreButton, false);
        renderOnboardingStep();
    }

    /**
     * Samples the contrastive dish rounds lazily — after the diet question, so
     * exclusions ticked moments earlier already filter the rounds. Changing the
     * diet (back navigation) resamples and drops picks that may now be invalid.
     */
    private void ensureOnboardingRounds() {
        String dietKey = householdProfile.getDiet().getExclusions().toString();
        if (onboardingRounds != null && dietKey.equals(onboardingRoundsDietKey)) {
            return;
        }
        Recipe[][] pools = new Recipe[BuiltInRecipes.mealCount()][];
        for (int i = 0; i < pools.length; i++) {
            pools[i] = BuiltInRecipes.forMeal(i);
        }
        onboardingRounds = onboardingDishSampler.sample(
                pools, householdProfile.getDiet(), random);
        onboardingRoundsDietKey = dietKey;
        java.util.Arrays.fill(onboardingPicks, null);
    }

    private void renderOnboardingStep() {
        contentContainer.removeAllViews();

        TextView progress = new TextView(this);
        progress.setText("Pytanie " + (onboardingStep + 1) + " z " + ONBOARDING_STEPS);
        progress.setTextSize(15);
        progress.setTextColor(COLOR_INK_SOFT);
        contentContainer.addView(progress, matchWrap());

        TextView question = new TextView(this);
        question.setId(R.id.onboarding_question);
        question.setTextSize(22);
        question.setTextColor(COLOR_INK);
        question.setTypeface(null, Typeface.BOLD);
        contentContainer.addView(question, marginTop(8));

        switch (onboardingStep) {
            case 0:
                question.setText("Dla kogo gotujesz?");
                for (int i = 0; i < AUDIENCE_LABELS.length; i++) {
                    final HouseholdProfile.Audience value = AUDIENCE_VALUES[i];
                    addOnboardingOption(i + 1, AUDIENCE_LABELS[i], () -> {
                        saveProfile(householdProfile.withAudience(value));
                        advanceOnboarding();
                    });
                }
                break;
            case 1:
                question.setText("Czego nie jadacie?");
                TextView dietNote = new TextView(this);
                dietNote.setText("Tego nigdy nie zaproponuję. Możesz zaznaczyć kilka "
                        + "odpowiedzi albo nic.");
                dietNote.setTextSize(15);
                dietNote.setTextColor(COLOR_INK_SOFT);
                contentContainer.addView(dietNote, marginTop(6));
                for (final DietConstraints.Exclusion exclusion
                        : DietConstraints.Exclusion.values()) {
                    CheckBox dietBox = new CheckBox(this);
                    dietBox.setText(exclusion.label());
                    dietBox.setTextSize(18);
                    dietBox.setTextColor(COLOR_INK);
                    dietBox.setChecked(householdProfile.getDiet().getExclusions()
                            .contains(exclusion));
                    dietBox.setOnCheckedChangeListener((view, checked) ->
                            toggleExclusion(exclusion, checked));
                    contentContainer.addView(dietBox, marginTop(8));
                }
                Button dietNext = new Button(this);
                dietNext.setId(R.id.onboarding_next_button);
                dietNext.setText("Dalej");
                dietNext.setTextSize(18);
                stylePrimaryButton(dietNext);
                dietNext.setOnClickListener(v -> advanceOnboarding());
                contentContainer.addView(dietNext, marginTop(16));
                break;
            case 2:
                question.setText("Ile masz zwykle czasu na gotowanie w dzień powszedni?");
                for (int i = 0; i < TIME_LABELS.length; i++) {
                    final HouseholdProfile.CookingTime value = TIME_VALUES[i];
                    addOnboardingOption(i + 1, TIME_LABELS[i], () -> {
                        saveProfile(householdProfile.withTime(value));
                        advanceOnboarding();
                    });
                }
                break;
            case 3:
                question.setText("Jak Ci idzie gotowanie?");
                for (int i = 0; i < SKILL_LABELS.length; i++) {
                    final HouseholdProfile.CookingSkill value = SKILL_VALUES[i];
                    addOnboardingOption(i + 1, SKILL_LABELS[i], () -> {
                        saveProfile(householdProfile.withSkill(value));
                        advanceOnboarding();
                    });
                }
                break;
            default:
                question.setText("Które danie najbardziej Ci pasuje?");
                ensureOnboardingRounds();
                final int roundIndex = onboardingStep - ONBOARDING_QUESTIONS;
                List<Recipe> round = onboardingRounds.get(roundIndex);
                for (int i = 0; i < round.size(); i++) {
                    final String dish = round.get(i).getTitle();
                    addOnboardingOption(i + 1, dish, () -> {
                        // Remembered per round and persisted at the end, so
                        // going back and re-picking replaces the choice.
                        onboardingPicks[roundIndex] = dish;
                        toast("Zapamiętane — lubisz: " + dish);
                        advanceOnboarding();
                    });
                }
                // Wymuszony wybór to fałszywy sygnał — „Żadne z tych" po prostu
                // nie zapisuje polubienia (uczenie pozostaje tylko pozytywne).
                Button none = new Button(this);
                none.setId(R.id.onboarding_option_none);
                none.setText("Żadne z tych");
                none.setTextSize(16);
                styleGhostButton(none);
                none.setOnClickListener(v -> {
                    onboardingPicks[roundIndex] = null;
                    advanceOnboarding();
                });
                contentContainer.addView(none, marginTop(12));
                break;
        }

        Button skip = new Button(this);
        skip.setId(R.id.onboarding_skip_button);
        skip.setText("Pomiń");
        skip.setTextSize(16);
        styleGhostButton(skip);
        skip.setOnClickListener(v -> endOnboarding());
        contentContainer.addView(skip, marginTop(24));
    }

    /** One big, readable answer button; index picks the stable test id. */
    private void addOnboardingOption(int index, String label, final Runnable action) {
        Button option = new Button(this);
        int[] optionIds = {R.id.onboarding_option_1, R.id.onboarding_option_2,
                R.id.onboarding_option_3};
        if (index >= 1 && index <= optionIds.length) {
            option.setId(optionIds[index - 1]);
        }
        option.setText(label);
        option.setTextSize(18);
        styleChoiceButton(option);
        option.setPadding(dp(16), dp(14), dp(16), dp(14));
        option.setOnClickListener(v -> action.run());
        contentContainer.addView(option, marginTop(12));
    }

    /** The single place a profile change is kept and persisted. */
    private void saveProfile(HouseholdProfile updated) {
        householdProfile = updated;
        householdProfileStore.save(updated);
    }

    /** Saves each (un)ticked exclusion immediately, so skipping keeps the answers. */
    private void toggleExclusion(DietConstraints.Exclusion exclusion, boolean excluded) {
        List<DietConstraints.Exclusion> exclusions =
                new ArrayList<>(householdProfile.getDiet().getExclusions());
        if (excluded) {
            if (!exclusions.contains(exclusion)) {
                exclusions.add(exclusion);
            }
        } else {
            exclusions.remove(exclusion);
        }
        saveProfile(householdProfile.withDiet(DietConstraints.of(exclusions)));
    }

    private void advanceOnboarding() {
        onboardingStep++;
        if (onboardingStep >= ONBOARDING_STEPS) {
            endOnboarding();
        } else {
            renderOnboardingStep();
        }
    }

    /**
     * Finishes or skips the quiz for good: the flag is permanent, answers given
     * so far stay saved, and a meal tapped in a notification opens now.
     */
    private void endOnboarding() {
        appSettings.markOnboardingDone();
        onboardingStep = -1;
        saveOnboardingPicks();
        setEnabledWithFade(moreButton, true);
        showStartupPrompts();
        if (pendingMealIndex >= 0) {
            int meal = pendingMealIndex;
            pendingMealIndex = -1;
            setMealButtonsEnabled(true);
            selectMeal(meal);
        } else {
            showStartScreen();
        }
    }

    /** Persists the quiz dish picks as ordinary likes, one per answered round. */
    private void saveOnboardingPicks() {
        java.util.Map<String, String> detailsByTitle = BuiltInRecipes.detailsByTitle(cookbook);
        for (String pick : onboardingPicks) {
            if (pick != null) {
                reactions = reactions.append(new DishReaction(pick,
                        DishReaction.describe(detailsByTitle.get(pick)), true,
                        System.currentTimeMillis()));
                preferences = preferences.withLike(pick);
                recordTasteEvent(TasteEvent.Type.ONBOARDING_PICK, pick,
                        TasteEvent.NO_MEAL);
            }
        }
        preferenceStore.save(preferences);
        reactionStore.save(reactions);
    }

    // ----- Meal selection + proposals -------------------------------------

    private void selectMeal(int index) {
        currentMealIndex = index;
        highlightSelectedMeal();
        generateProposals();
    }

    private void highlightSelectedMeal() {
        for (int i = 0; i < mealButtons.length; i++) {
            // The picked meal flips to the filled primary look; the rest stay tonal.
            if (i == currentMealIndex) {
                stylePrimaryButton(mealButtons[i]);
            } else {
                styleTonalButton(mealButtons[i]);
            }
        }
    }

    /**
     * Offer a fresh set of proposals for the current meal. Signed in, one LLM call
     * rates catalogue candidates against the explicit like/dislike list (empty
     * list = popularity); otherwise the offline pool, never pretending to be AI.
     */
    private void generateProposals() {
        if (currentMealIndex < 0) {
            return;
        }
        contentEpoch++;
        // From now on the content area belongs to the proposal flow, so back
        // (even while the AI is still thinking) returns to the start screen.
        currentScreen = BackNavigation.Screen.PROPOSALS;
        if (isAiAvailable()) {
            generateRatedProposals();
        } else {
            generateOfflineProposals();
        }
    }

    private void generateOfflineProposals() {
        // Shared offline pipeline: pool -> shuffle -> at most one taste-led pick,
        // the rest kept varied (so liking three chicken dishes does not turn every
        // suggestion into chicken).
        List<Recipe> chosen = generateOfflineRecipes();

        List<DishProposal> newProposals = new ArrayList<>();
        List<Recipe> newRecipes = new ArrayList<>();
        for (Recipe recipe : chosen) {
            newProposals.add(proposalFromRecipe(recipe));
            newRecipes.add(recipe);
        }
        showProposals(newProposals, newRecipes, null);
    }

    /** The shared offline pipeline for the current meal, diet-filtered. */
    private List<Recipe> generateOfflineRecipes() {
        return offlineProposalGenerator.generate(
                BuiltInRecipes.forMeal(currentMealIndex), cookbook, preferences,
                buildTasteProfile(), PROPOSAL_COUNT, random, history,
                System.currentTimeMillis(), householdProfile.getDiet());
    }

    /** Distils the user's likes into recurring "taste" terms (with known recipe details). */
    private TasteProfile buildTasteProfile() {
        return tasteProfiler.build(preferences.getLikes(),
                BuiltInRecipes.detailsByTitle(cookbook));
    }

    private DishProposal proposalFromRecipe(Recipe recipe) {
        return proposalValidator.proposalFromRecipe(recipe);
    }

    /**
     * Jedno wywołanie LLM: kandydaci z katalogu (już po diecie) oceniani
     * względem listy reakcji; pokazujemy 3 najwyżej ocenione z powodem.
     * Sieć, zły JSON czy wylogowanie po drodze kończą się pulą offline.
     */
    private void generateRatedProposals() {
        final String mealType = MEAL_TYPES[currentMealIndex];
        final DishReactionLog reactionSnapshot = reactions;
        final DietConstraints diet = householdProfile.getDiet();
        final long now = System.currentTimeMillis();
        final List<Recipe> candidates = DishRecommender.candidates(
                mealPoolBuilder.build(BuiltInRecipes.forMeal(currentMealIndex), cookbook,
                        preferences, diet),
                history, now, random);
        final List<Recipe> offline = generateOfflineRecipes();
        final int epoch = contentEpoch;
        setMealButtonsEnabled(false);
        showHint("Dobieram dania do Twojego gustu…");
        new Thread(() -> {
            final DishRecommender.Recommendation recommendation = dishRecommender.recommend(
                    isAiAvailable(), mealType, reactionSnapshot, candidates, diet, offline,
                    PROPOSAL_COUNT, now);
            runOnUiThread(() -> {
                setMealButtonsEnabled(true);
                if (epoch != contentEpoch) {
                    return; // the user moved on; don't stomp the new content
                }
                if (recommendation.getRecipes().isEmpty()) {
                    showHint("Brak dań pasujących do diety. Dodaj własne danie.");
                    return;
                }
                List<DishProposal> newProposals = new ArrayList<>();
                for (Recipe recipe : recommendation.getRecipes()) {
                    newProposals.add(proposalFromRecipe(recipe));
                }
                showProposals(newProposals, new ArrayList<>(recommendation.getRecipes()),
                        recommendation.getReasons());
                if (recommendation.isFailed()) {
                    toast("AI nie oceniło dań — pokazuję propozycje offline.");
                }
            });
        }).start();
    }

    /** Aktualny model gustu (pochodna dziennika + agregatu, tanio liczona). */
    private TasteModel currentTasteModel(java.util.Map<String, String> detailsByTitle) {
        return TasteModel.build(tasteEvents, tasteAggregate, dishTagger,
                detailsByTitle, System.currentTimeMillis());
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
        java.util.Map<String, String> detailsByTitle =
                BuiltInRecipes.detailsByTitle(cookbook);
        return new RecipeRequest(mealType, preferences, history.recentTitles(8),
                fragments, knownDishes, buildTasteProfile().getAffinities(),
                householdProfile)
                .withTasteContext(tasteContextBuilder.build(
                        currentTasteModel(detailsByTitle), tasteEvents,
                        householdProfile.getDiet(), currentMealIndex)
                        .withAntiMonotony(monotonyDetector.detect(
                                history.recentTitles(MonotonyDetector.WINDOW),
                                detailsByTitle)));
    }

    private void showProposals(List<DishProposal> newProposals, List<Recipe> newRecipes,
                               List<String> reasons) {
        proposals = newProposals;
        proposalRecipes = newRecipes;
        proposalReasons = reasons == null ? new ArrayList<String>() : reasons;
        currentRecipe = null;
        trioChoiceRecorded = false;
        trioEngagementCounted = false;
        viewedInTrio.clear();
        for (DishProposal proposal : newProposals) {
            recordChosen(proposal.getName());
        }
        countShownTrio(newProposals, newRecipes);
        renderProposals();
    }

    /** Liczniki diagnostyczne: pokazany zestaw + jakość tagowania propozycji AI. */
    private void countShownTrio(List<DishProposal> newProposals, List<Recipe> newRecipes) {
        int aiCount = 0;
        int aiUntagged = 0;
        for (int i = 0; i < newProposals.size(); i++) {
            boolean fromAi = i >= newRecipes.size() || newRecipes.get(i) == null;
            if (fromAi) {
                aiCount++;
                if (dishTagger.tag(newProposals.get(i)).get(TasteDimension.BASE) == null) {
                    aiUntagged++;
                }
            }
        }
        learningStats = learningStats.withTrioShown()
                .withAiProposals(aiCount, aiUntagged);
        learningStatsStore.save(learningStats);
    }

    /** Pierwsze zaangażowanie w zestaw (przepis/lajk) liczy się do acceptance. */
    private void markTrioEngaged() {
        if (trioEngagementCounted) {
            return;
        }
        trioEngagementCounted = true;
        learningStats = learningStats.withTrioEngaged();
        learningStatsStore.save(learningStats);
    }

    private void renderProposals() {
        currentScreen = BackNavigation.Screen.PROPOSALS;
        contentContainer.removeAllViews();
        for (int i = 0; i < proposals.size(); i++) {
            contentContainer.addView(buildProposalCard(i), marginTop(i == 0 ? 0 : 12));
        }

        Button refresh = new Button(this);
        refresh.setId(R.id.refresh_button);
        refresh.setText("Inne propozycje");
        refresh.setTextSize(16);
        styleTonalButton(refresh);
        refresh.setOnClickListener(v -> {
            recordTrioRerolled();
            generateProposals();
        });
        contentContainer.addView(refresh, marginTop(16));
    }

    private View buildProposalCard(int index) {
        DishProposal proposal = proposals.get(index);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(outlined(COLOR_SURFACE, 20));
        card.setElevation(dp(1));
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView name = new TextView(this);
        if (index == 0) {
            name.setId(R.id.recipe_title); // first card title is the testable anchor
        }
        name.setText(proposal.getName());
        name.setTextSize(20);
        name.setTextColor(COLOR_INK);
        name.setTypeface(null, Typeface.BOLD);
        card.addView(name, matchWrap());

        String summary = proposal.summary();
        if (!summary.isEmpty()) {
            TextView body = new TextView(this);
            body.setText(summary);
            body.setTextSize(15);
            body.setTextColor(COLOR_INK_BODY);
            card.addView(body, marginTop(6));
        }

        String reason = index < proposalReasons.size() ? proposalReasons.get(index) : "";
        if (!reason.isEmpty()) {
            TextView why = new TextView(this);
            why.setText("Dlaczego: " + reason);
            why.setTextSize(14);
            why.setTextColor(COLOR_INK_BODY);
            why.setTypeface(null, Typeface.ITALIC);
            card.addView(why, marginTop(6));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, marginTop(10));

        Button like = new Button(this);
        if (index == 0) {
            like.setId(R.id.like_button);
        }
        like.setText("Lubię to");
        like.setTextSize(15);
        styleTonalButton(like);
        like.setOnClickListener(v -> reactToProposal(index, true));
        actions.addView(like, equalWidthRowItem());

        Button dislike = new Button(this);
        if (index == 0) {
            dislike.setId(R.id.dislike_button);
        }
        dislike.setText("Nie lubię");
        dislike.setTextSize(15);
        styleGhostButton(dislike);
        dislike.setOnClickListener(v -> reactToProposal(index, false));
        actions.addView(dislike, equalWidthRowItem());

        Button show = new Button(this);
        if (index == 0) {
            show.setId(R.id.accept_button);
        }
        show.setText("Pokaż przepis");
        show.setTextSize(15);
        stylePrimaryButton(show);
        show.setOnClickListener(v -> openRecipe(index));
        actions.addView(show, equalWidthRowItem());

        return card;
    }

    /** „Inne propozycje" = słaby negatyw dla trójki w offline'owym modelu gustu (nie reakcja). */
    private void recordTrioRerolled() {
        for (DishProposal proposal : proposals) {
            recordTasteEvent(TasteEvent.Type.REROLLED, proposal.getName(),
                    currentMealIndex, false);
        }
        learningStats = learningStats.withReroll();
        learningStatsStore.save(learningStats);
    }

    private void reactToProposal(int index, boolean liked) {
        if (index < 0 || index >= proposals.size()) {
            return;
        }
        markTrioEngaged();
        DishProposal proposal = proposals.get(index);
        Recipe recipe = index < proposalRecipes.size() ? proposalRecipes.get(index) : null;
        String description = recipe != null
                ? DishReaction.describe(recipe.getDetails()) : DishReaction.describe(proposal);
        rememberReaction(proposal.getName(), description, liked);
    }

    /** Reveal the full recipe for a proposal: instant offline, fetched for AI. */
    private void openRecipe(int index) {
        if (index < 0 || index >= proposals.size()) {
            return;
        }
        recipeFromProposals = true;
        markTrioEngaged();
        String chosenDish = proposals.get(index).getName();
        // Opening a recipe is an implicit "this one interests me" signal — once
        // per dish per trio, so browsing back and forth does not inflate taste...
        if (viewedInTrio.add(chosenDish.toLowerCase())) {
            recordTasteEvent(TasteEvent.Type.RECIPE_VIEWED, chosenDish,
                    currentMealIndex, false);
        }
        // ...and the first choice in a trio marks the other dishes as "lost the
        // comparison" — relative learning for the offline taste model.
        if (!trioChoiceRecorded) {
            trioChoiceRecorded = true;
            for (int i = 0; i < proposals.size(); i++) {
                String other = proposals.get(i).getName();
                if (i == index) {
                    continue;
                }
                recordTasteEvent(TasteEvent.Type.SHOWN_NOT_CHOSEN, other,
                        currentMealIndex, false);
            }
        }
        Recipe cached = index < proposalRecipes.size() ? proposalRecipes.get(index) : null;
        if (cached != null) {
            showFullRecipe(cached);
            return;
        }
        final String dishName = proposals.get(index).getName();
        final RecipeRequest request = buildRequest();
        final int requestedIndex = index;
        contentEpoch++;
        // Already "on" the recipe while it is being fetched, so back during the
        // fetch returns to the proposals (and the bumped epoch drops the answer).
        currentScreen = BackNavigation.Screen.RECIPE;
        final int epoch = contentEpoch;
        showHint("Przygotowuję przepis…");
        new Thread(() -> {
            try {
                final Recipe recipe = recipeService.generateRecipeFor(dishName, request);
                runOnUiThread(() -> {
                    // Cache the recipe so going back and reopening is instant.
                    if (requestedIndex < proposalRecipes.size()) {
                        proposalRecipes.set(requestedIndex, recipe);
                    }
                    if (epoch != contentEpoch) {
                        return;
                    }
                    showFullRecipe(recipe);
                    warnIfRecipeBreaksDiet(recipe);
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    if (epoch != contentEpoch) {
                        return;
                    }
                    // Bring the proposals back so the user isn't stranded on an
                    // error message with no way to retry.
                    renderProposals();
                    toast(message != null ? message
                            : "Nie udało się pobrać przepisu. Spróbuj ponownie.");
                });
            }
        }).start();
    }

    /**
     * Propozycje są twardo walidowane, ale pełny przepis od AI może mimo
     * wymogu w prompcie przemycić wykluczony składnik — wtedy przynajmniej
     * głośno ostrzegamy (przepisu nie podmieniamy, użytkownik na niego czeka).
     */
    private void warnIfRecipeBreaksDiet(Recipe recipe) {
        String warning = householdProfile.getDiet().warningFor(
                recipe.getTitle() + "\n" + recipe.getDetails());
        if (!warning.isEmpty()) {
            Toast.makeText(this, warning, Toast.LENGTH_LONG).show();
        }
    }

    private void showFullRecipe(Recipe recipe) {
        currentRecipe = recipe;
        currentScreen = BackNavigation.Screen.RECIPE;
        contentContainer.removeAllViews();

        TextView name = new TextView(this);
        name.setId(R.id.recipe_title);
        name.setText(recipe.getTitle());
        name.setTextSize(24);
        name.setTextColor(COLOR_INK);
        name.setTypeface(null, Typeface.BOLD);
        contentContainer.addView(name, matchWrap());

        TextView details = new TextView(this);
        details.setId(R.id.recipe_details);
        details.setText(recipe.getDetails());
        details.setTextSize(17);
        details.setLineSpacing(dp(4), 1.0f);
        details.setTextColor(COLOR_INK_BODY);
        contentContainer.addView(details, marginTop(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        contentContainer.addView(actions, marginTop(16));

        Button like = new Button(this);
        like.setId(R.id.like_button);
        like.setText("Lubię to");
        like.setTextSize(16);
        stylePrimaryButton(like);
        like.setOnClickListener(v -> rememberReaction(recipe.getTitle(),
                DishReaction.describe(recipe.getDetails()), true));
        actions.addView(like, equalWidthRowItem());

        Button dislike = new Button(this);
        dislike.setId(R.id.dislike_button);
        dislike.setText("Nie lubię");
        dislike.setTextSize(16);
        styleGhostButton(dislike);
        dislike.setOnClickListener(v -> rememberReaction(recipe.getTitle(),
                DishReaction.describe(recipe.getDetails()), false));
        actions.addView(dislike, equalWidthRowItem());

        if (isAiAvailable()) {
            Button change = new Button(this);
            change.setId(R.id.change_button);
            change.setText("Zmień przepis");
            change.setTextSize(16);
            styleTonalButton(change);
            change.setOnClickListener(v -> showModifyRecipeDialog());
            actions.addView(change, equalWidthRowItem());
        }

        // The button mirrors the system back button exactly (one shared path),
        // so a recipe that has no proposals to return to goes to the start screen.
        Button back = new Button(this);
        back.setId(R.id.back_button);
        back.setText(recipeFromProposals && !proposals.isEmpty()
                ? "Wróć do propozycji" : "Wróć");
        back.setTextSize(16);
        styleGhostButton(back);
        back.setOnClickListener(v -> onBackPressed());
        contentContainer.addView(back, marginTop(12));
    }

    // ----- Explicit reactions ---------------------------------------------

    /**
     * Dopisuje jawną reakcję do listy (wejście oceny przez LLM). Polubienie
     * zasila też offline'owy fallback; „Nie lubię" nie jest zakazem — danie
     * nie znika z puli, model po prostu oceni je i podobne niżej.
     */
    private void rememberReaction(String dish, String description, boolean liked) {
        if (dish == null || dish.trim().isEmpty()) {
            return;
        }
        reactions = reactions.append(new DishReaction(dish, description, liked,
                System.currentTimeMillis()));
        reactionStore.save(reactions);
        if (liked) {
            preferences = preferences.withLike(dish);
            preferenceStore.save(preferences);
            recordTasteEvent(TasteEvent.Type.LIKED, dish, currentMealIndex, false);
        }
        Toast.makeText(this, (liked ? "Zapamiętane — lubisz: " : "Zapamiętane — nie lubisz: ")
                + dish, Toast.LENGTH_SHORT).show();
    }

    private void recordTasteEvent(TasteEvent.Type type, String dish, int mealIndex) {
        recordTasteEvent(type, dish, mealIndex, false);
    }

    /** The single place taste events are appended, compacted and persisted. */
    private void recordTasteEvent(TasteEvent.Type type, String dish, int mealIndex,
                                  boolean exploratory) {
        tasteEvents = tasteEvents.append(new TasteEvent(
                type, dish, mealIndex, System.currentTimeMillis(), exploratory));
        if (tasteEvents.size() > TasteEventLog.MAX_EVENTS) {
            // Najstarsze zdarzenia zwijają się do zamrożonego agregatu —
            // model liczony dalej wychodzi ten sam, dziennik nie puchnie.
            TasteEventCompactor.Result compacted = tasteEventCompactor.compact(
                    tasteEvents, tasteAggregate, dishTagger,
                    BuiltInRecipes.detailsByTitle(cookbook),
                    System.currentTimeMillis(), TasteEventLog.MAX_EVENTS);
            tasteEvents = compacted.getLog();
            tasteAggregate = compacted.getAggregate();
            tasteEventStore.saveAggregate(tasteAggregate);
        }
        tasteEventStore.save(tasteEvents);
    }

    // ----- Modify the shown recipe ----------------------------------------

    private void showModifyRecipeDialog() {
        if (currentRecipe == null || currentRecipe.getTitle().trim().isEmpty()) {
            return;
        }
        if (!isAiAvailable()) {
            Toast.makeText(this, "Zmiana przepisu wymaga zalogowania kontem ChatGPT.",
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
        contentEpoch++;
        final int epoch = contentEpoch;
        showHint("Zmieniam przepis…");
        new Thread(() -> {
            try {
                final Recipe revised = recipeService.modifyRecipe(base, instruction,
                        householdProfile);
                runOnUiThread(() -> {
                    if (epoch != contentEpoch) {
                        return;
                    }
                    showFullRecipe(revised);
                    warnIfRecipeBreaksDiet(revised);
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    if (epoch != contentEpoch) {
                        return;
                    }
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
        labels.add("Profil domowników");
        actions.add(this::showHouseholdProfileDialog);
        labels.add("Dodaj danie, które znasz i lubisz");
        actions.add(this::showAddKnownDishDialog);
        labels.add("Zarządzaj moimi danymi");
        actions.add(this::showManageDialog);
        if (chatGptAccount.isSignedIn()) {
            labels.add(MODEL_MENU_PREFIX + chatGptAccount.modelChoice().label);
            actions.add(this::showModelChoiceDialog);
            String email = chatGptAccount.email();
            labels.add(email.isEmpty() ? "Wyloguj z ChatGPT" : "Wyloguj z ChatGPT (" + email + ")");
            actions.add(this::signOutOfChatGpt);
        } else {
            labels.add(SIGN_IN_MENU_LABEL);
            actions.add(this::startChatGptSignIn);
        }

        new AlertDialog.Builder(this)
                .setTitle("Więcej")
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    // ----- Household profile (answers from the onboarding quiz) ------------

    /** Lets the user change the quiz answers later, one simple dialog each. */
    private void showHouseholdProfileDialog() {
        final String[] items = {"Dla kogo gotujesz?", "Czego nie jadacie?",
                "Ile masz czasu na gotowanie?", "Jak Ci idzie gotowanie?",
                "Ulubione kuchnie"};
        new AlertDialog.Builder(this)
                .setTitle("Profil domowników")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showAudienceDialog();
                    } else if (which == 1) {
                        showDietDialog();
                    } else if (which == 2) {
                        showTimeDialog();
                    } else if (which == 3) {
                        showSkillDialog();
                    } else {
                        showCuisinesDialog();
                    }
                })
                .show();
    }

    private void showDietDialog() {
        final DietConstraints.Exclusion[] values = DietConstraints.Exclusion.values();
        final String[] labels = new String[values.length];
        final boolean[] checked = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label();
            checked[i] = householdProfile.getDiet().getExclusions().contains(values[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Czego nie jadacie?")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Gotowe", (dialog, which) -> {
                    List<DietConstraints.Exclusion> exclusions = new ArrayList<>();
                    for (int i = 0; i < values.length; i++) {
                        if (checked[i]) {
                            exclusions.add(values[i]);
                        }
                    }
                    saveProfile(householdProfile.withDiet(DietConstraints.of(exclusions)));
                })
                .show();
    }

    private void showAudienceDialog() {
        showProfileChoiceDialog("Dla kogo gotujesz?", AUDIENCE_LABELS, AUDIENCE_VALUES,
                householdProfile.getAudience(), HouseholdProfile::withAudience);
    }

    private void showSkillDialog() {
        showProfileChoiceDialog("Jak Ci idzie gotowanie?", SKILL_LABELS, SKILL_VALUES,
                householdProfile.getSkill(), HouseholdProfile::withSkill);
    }

    private void showTimeDialog() {
        showProfileChoiceDialog("Ile masz zwykle czasu na gotowanie?", TIME_LABELS,
                TIME_VALUES, householdProfile.getTime(), HouseholdProfile::withTime);
    }

    /** Applies one picked value to the profile ({@code profile.withX(value)}). */
    private interface ProfileUpdate<T> {
        HouseholdProfile apply(HouseholdProfile profile, T value);
    }

    /** One single-choice dialog shape for every profile question. */
    private <T> void showProfileChoiceDialog(String title, String[] labels,
                                             final T[] values, T current,
                                             final ProfileUpdate<T> update) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, indexOf(values, current), (dialog, which) -> {
                    saveProfile(update.apply(householdProfile, values[which]));
                    dialog.dismiss();
                })
                .show();
    }

    private void showCuisinesDialog() {
        final boolean[] checked = new boolean[CUISINE_OPTIONS.length];
        for (int i = 0; i < CUISINE_OPTIONS.length; i++) {
            checked[i] = householdProfile.getCuisines().contains(CUISINE_OPTIONS[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Ulubione kuchnie")
                .setMultiChoiceItems(CUISINE_OPTIONS, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Gotowe", (dialog, which) -> {
                    List<String> cuisines = new ArrayList<>();
                    for (int i = 0; i < CUISINE_OPTIONS.length; i++) {
                        if (checked[i]) {
                            cuisines.add(CUISINE_OPTIONS[i]);
                        }
                    }
                    saveProfile(householdProfile.withCuisines(cuisines));
                })
                .show();
    }

    private static <T> int indexOf(T[] values, T value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private void showAddKnownDishDialog() {
        if (!isAiAvailable()) {
            Toast.makeText(this, "Dodawanie z linku/opisu wymaga zalogowania kontem ChatGPT.",
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
        contentEpoch++;
        final int epoch = contentEpoch;
        showHint("Dodaję do bazy…");
        new Thread(() -> {
            try {
                final CookbookEntry entry = dishImporter.importDish(input);
                runOnUiThread(() -> {
                    // Always persist the imported dish, even if the view moved on.
                    cookbook = cookbook.add(entry);
                    cookbookStore.save(cookbook);
                    preferences = preferences.withLike(entry.getTitle());
                    preferenceStore.save(preferences);
                    recordTasteEvent(TasteEvent.Type.IMPORTED, entry.getTitle(),
                            TasteEvent.NO_MEAL);
                    Toast.makeText(MainActivity.this,
                            "Dodano do bazy: " + entry.getTitle(), Toast.LENGTH_SHORT).show();
                    if (epoch != contentEpoch) {
                        return;
                    }
                    // Not part of the proposal flow — back goes to the start screen.
                    recipeFromProposals = false;
                    showFullRecipe(entry.toRecipe());
                });
            } catch (IOException e) {
                final String message = e.getMessage();
                runOnUiThread(() -> {
                    if (epoch != contentEpoch) {
                        return;
                    }
                    showHint(message != null ? message : "Nie udało się dodać dania.");
                });
            }
        }).start();
    }

    private void showManageDialog() {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        labels.add("Pokaż i usuń dania z bazy");
        actions.add(this::showCookbookDialog);
        labels.add("Wyczyść reakcje (lubię / nie lubię)");
        actions.add(() -> {
            dataManager.clearPreferences();
            preferences = UserPreferences.empty();
            reactions = DishReactionLog.empty();
            // Dziennik gustu też — inaczej „zapomniane" polubienia wróciłyby
            // do propozycji bocznymi drzwiami przez model gustu.
            tasteEvents = TasteEventLog.empty();
            tasteEventStore.save(tasteEvents);
            tasteAggregate = FrozenTasteAggregate.empty();
            tasteEventStore.saveAggregate(tasteAggregate);
            toast("Wyczyszczono reakcje na dania.");
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
        labels.add("Statystyki uczenia (diagnostyka)");
        actions.add(this::showLearningStatsDialog);

        new AlertDialog.Builder(this)
                .setTitle("Moje dane")
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    /** Lokalne liczniki jakości uczenia — nic nie wychodzi z telefonu. */
    private void showLearningStatsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Statystyki uczenia")
                .setMessage(learningStats.summaryText(tasteEvents.size()))
                .setPositiveButton("OK", null)
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

    /** A rounded rectangle in {@code fill}, the building block of the theme. */
    private GradientDrawable rounded(int fill, int cornerDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(cornerDp));
        return shape;
    }

    /** Rounded rectangle with a hairline outline (cards, choice buttons). */
    private GradientDrawable outlined(int fill, int cornerDp) {
        GradientDrawable shape = rounded(fill, cornerDp);
        shape.setStroke(Math.max(1, dp(1)), COLOR_OUTLINE);
        return shape;
    }

    /** Shared button chrome: pill background with a ripple, no platform skin. */
    private void styleButton(Button button, int fill, int textColor, int ripple) {
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),
                rounded(fill, BUTTON_CORNER_DP), rounded(Color.WHITE, BUTTON_CORNER_DP)));
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setMinHeight(dp(48));
        button.setPadding(dp(18), dp(12), dp(18), dp(12));
    }

    /** The one main action on a screen: filled terracotta, white bold label. */
    private void stylePrimaryButton(Button button) {
        styleButton(button, COLOR_ACCENT, Color.WHITE, RIPPLE_ON_ACCENT);
        button.setTypeface(null, Typeface.BOLD);
    }

    /** Secondary actions: soft peach fill with deep-terracotta label. */
    private void styleTonalButton(Button button) {
        styleButton(button, COLOR_ACCENT_SOFT, COLOR_ACCENT_DEEP, RIPPLE_ON_LIGHT);
        button.setTypeface(null, Typeface.NORMAL);
    }

    /** Quiet actions ("Pomiń", "Wróć", "Więcej…"): label only, bounded ripple. */
    private void styleGhostButton(Button button) {
        styleButton(button, Color.TRANSPARENT, COLOR_ACCENT_DEEP, RIPPLE_ON_LIGHT);
        button.setTypeface(null, Typeface.NORMAL);
    }

    /** Quiz answers: white card-like buttons with dark text, easy to scan. */
    private void styleChoiceButton(Button button) {
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE_ON_LIGHT),
                outlined(COLOR_SURFACE, 16), rounded(Color.WHITE, 16)));
        button.setTextColor(COLOR_INK);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setElevation(dp(1));
    }

    /** Shows a single informational line in the content area (no recipe yet). */
    private void showHint(String text) {
        contentContainer.removeAllViews();
        TextView hint = new TextView(this);
        hint.setText(text);
        hint.setTextSize(16);
        hint.setTextColor(COLOR_INK_SOFT);
        contentContainer.addView(hint, matchWrap());
    }

    private void setMealButtonsEnabled(boolean enabled) {
        for (Button button : mealButtons) {
            setEnabledWithFade(button, enabled);
        }
    }

    /** Custom-drawn buttons have no platform disabled state, so fade them. */
    private static void setEnabledWithFade(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
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
