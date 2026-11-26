package com.mealspire.app;

import com.mealspire.app.domain.RecipeOperations;
import com.mealspire.app.domain.DishImporter;
import com.mealspire.app.storage.SharedPreferencesBackendStore;
import com.mealspire.app.backend.BackendClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mealspire.app.domain.ChatGptAccount;
import com.mealspire.app.domain.ChatGptOAuth;
import com.mealspire.app.domain.ChatGptSignInFlow;
import com.mealspire.app.domain.ChatGptSession;
import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.domain.BackNavigation;
import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.CookbookEntry;
import com.mealspire.app.domain.CookbookStore;
import com.mealspire.app.domain.DataManager;
import com.mealspire.app.domain.DayGreeting;
import com.mealspire.app.domain.DietConstraints;
import com.mealspire.app.domain.DishProposal;
import com.mealspire.app.domain.DishReaction;
import com.mealspire.app.domain.DishReactionLog;
import com.mealspire.app.domain.DishReactionStore;
import com.mealspire.app.domain.DishRater;
import com.mealspire.app.domain.MealPoolBuilder;
import com.mealspire.app.domain.HouseholdProfile;
import com.mealspire.app.domain.HouseholdProfileStore;
import com.mealspire.app.domain.LearningStats;
import com.mealspire.app.domain.LearningStatsStore;
import com.mealspire.app.domain.BuiltInRecipes;
import com.mealspire.app.domain.IngredientExtractor;
import com.mealspire.app.domain.OfflineProposalGenerator;
import com.mealspire.app.domain.PortionSize;
import com.mealspire.app.domain.ProposalValidator;
import com.mealspire.app.domain.MealHistory;
import com.mealspire.app.domain.MealHistoryStore;
import com.mealspire.app.domain.PreferenceStore;
import com.mealspire.app.domain.ReadyDishPool;
import com.mealspire.app.domain.ReadyProposals;
import com.mealspire.app.domain.Recipe;
import com.mealspire.app.domain.RecipeLayout;
import com.mealspire.app.domain.RecipeRequest;
import com.mealspire.app.domain.DishTagger;
import com.mealspire.app.domain.FoodLog;
import com.mealspire.app.domain.FoodLogReminder;
import com.mealspire.app.domain.FoodLogStore;
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
import com.mealspire.app.domain.TasteSurvey;
import com.mealspire.app.domain.TasteSurveyPlanner;
import com.mealspire.app.domain.TasteSurveyStore;
import com.mealspire.app.domain.UpdateChecker;
import com.mealspire.app.domain.UpdateStateStore;
import com.mealspire.app.domain.UserPreferences;
import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.domain.VersionInfoParser;
import com.mealspire.app.domain.VersionJsonSource;
import com.mealspire.app.domain.GptModel;
import com.mealspire.app.domain.IdTokenVerifier;
import com.mealspire.app.net.HttpUrlTransport;
import com.mealspire.app.net.LoopbackCallbackServer;
import com.mealspire.app.net.HttpVersionJsonFetcher;
import com.mealspire.app.notify.FoodLogNotifications;
import com.mealspire.app.notify.MealNotifications;
import com.mealspire.app.notify.MealReminderScheduler;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesCookbookStore;
import com.mealspire.app.storage.SharedPreferencesFoodLogStore;
import com.mealspire.app.storage.SharedPreferencesDishReactionStore;
import com.mealspire.app.storage.SharedPreferencesHouseholdProfileStore;
import com.mealspire.app.storage.SharedPreferencesMealHistoryStore;
import com.mealspire.app.storage.SharedPreferencesPreferenceStore;
import com.mealspire.app.storage.SharedPreferencesLearningStatsStore;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;
import com.mealspire.app.storage.SharedPreferencesTasteEventStore;
import com.mealspire.app.storage.SharedPreferencesTasteSurveyStore;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;
import com.mealspire.app.ui.Ui;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    // Look and feel (palette, buttons, cards) lives in ui/Ui; this class only
    // decides what goes on screen.

    private static final String[] MEAL_TYPES = {"Śniadanie", "Obiad", "Kolacja"};
    // "Pomysły na …" — the meal in the accusative, for section headers.
    private static final String[] MEAL_ACCUSATIVE = {"śniadanie", "obiad", "kolację"};
    private static final int[] MEAL_ICONS = {R.drawable.ic_meal_breakfast,
            R.drawable.ic_meal_lunch, R.drawable.ic_meal_dinner};
    private static final int PROPOSAL_COUNT = 3;
    private static final String NO_DISHES_FOR_DIET =
            "Brak dań pasujących do diety. Dodaj własne danie.";
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
    private static final String[] SPICE_LABELS =
            {"Łagodnie, bez ostrego", "Średnio", "Im ostrzej, tym lepiej"};
    private static final HouseholdProfile.SpiceLevel[] SPICE_VALUES = {
            HouseholdProfile.SpiceLevel.MILD,
            HouseholdProfile.SpiceLevel.MEDIUM,
            HouseholdProfile.SpiceLevel.HOT};
    private static final String[] STYLE_LABELS =
            {"Lekko i zdrowo", "Różnie, zależnie od dnia", "Sycąco, po domowemu"};
    private static final HouseholdProfile.MealStyle[] STYLE_VALUES = {
            HouseholdProfile.MealStyle.LIGHT,
            HouseholdProfile.MealStyle.BALANCED,
            HouseholdProfile.MealStyle.HEARTY};
    private static final String[] BUDGET_LABELS =
            {"Oszczędnie", "Bez szaleństw", "Nie liczę — liczy się smak"};
    private static final HouseholdProfile.Budget[] BUDGET_VALUES = {
            HouseholdProfile.Budget.THRIFTY,
            HouseholdProfile.Budget.MODERATE,
            HouseholdProfile.Budget.GENEROUS};
    private static final String[] NOVELTY_LABELS =
            {"Sprawdzone klasyki", "Pół na pół", "Ciągle coś nowego"};
    private static final HouseholdProfile.Novelty[] NOVELTY_VALUES = {
            HouseholdProfile.Novelty.CLASSICS,
            HouseholdProfile.Novelty.MIXED,
            HouseholdProfile.Novelty.ADVENTUROUS};
    // Pytania profilu w quizie, w tej kolejności (indeks = krok quizu).
    private static final String Q_AUDIENCE = "Dla kogo gotujesz?";
    private static final String Q_DIET = "Czego nie jadacie?";
    private static final String Q_TIME = "Ile masz zwykle czasu na gotowanie w dzień powszedni?";
    private static final String Q_SKILL = "Jak Ci idzie gotowanie?";
    private static final String Q_CUISINES = "Jakie kuchnie lubicie?";
    private static final String Q_SPICE = "Jak ostro jadacie?";
    private static final String Q_STYLE = "Na co macie zwykle ochotę?";
    private static final String Q_BUDGET = "Ile chcecie wydawać na zakupy?";
    private static final String Q_NOVELTY = "Klasyki czy nowości?";
    private static final int ONBOARDING_QUESTIONS = 9;
    // The A/B taste survey is the one quiz step after the questions; its pairs
    // advance inside that step (TasteSurvey.position).
    private static final int SURVEY_STEP = ONBOARDING_QUESTIONS;
    private static final int ONBOARDING_STEPS = ONBOARDING_QUESTIONS + 1;

    /** Intent extra: which meal to open (0=breakfast, 1=lunch, 2=dinner). */
    public static final String EXTRA_MEAL_INDEX = "meal_index";
    /** Intent extra: open the "Co dzisiaj jedliście?" answer dialog. */
    public static final String EXTRA_FOOD_LOG = "food_log";
    private static final int REQ_POST_NOTIFICATIONS = 1001;
    static final String FOOD_LOG_HOUR_MENU_PREFIX = "Pytanie wieczorne: ";
    static final String SIGN_IN_DIALOG_TITLE = "Zaloguj się kontem ChatGPT";
    static final String SIGN_IN_MENU_LABEL = "Zaloguj się kontem ChatGPT";
    static final String MODEL_MENU_PREFIX = "Model AI: ";
    static final String SERVER_NOTICE_TITLE = "AI przez serwer Mealspire";
    static final String SIGN_IN_FAILED_TITLE = "Logowanie do ChatGPT nie powiodło się";
    static final String SIGN_IN_PENDING_TITLE = "Logowanie do ChatGPT trwa";
    static final String SIGN_IN_RETRY_LABEL = "Spróbuj ponownie";
    static final String SIGN_IN_RESTART_LABEL = "Zacznij od nowa";
    static final String SIGN_IN_CHIP_LABEL = "Logowanie do ChatGPT…";
    private static final int SIGN_IN_TIMEOUT_MS = 5 * 60 * 1000;
    private static final String SIGN_IN_TIMEOUT_MESSAGE =
            "Logowanie do ChatGPT trwało zbyt długo — spróbuj ponownie.";

    // Test seams (package-private, null in production): replace the network
    // source and the background executor so Robolectric tests stay offline and
    // deterministic. Never assign these from production code.
    static VersionJsonSource versionJsonSourceOverride;
    static Executor updateCheckExecutorOverride;
    static com.mealspire.app.domain.HttpTransport backendTransportOverride;
    static Executor readyPoolExecutorOverride;
    static Executor signInExecutorOverride;
    static DishReactionStore reactionStoreOverride;
    static TasteEventStore tasteEventStoreOverride;

    private final Random random = new Random();
    private Button servingsLabel;
    private Button aiChip;
    private TextView greeting;
    // Greeting, chips and meal tiles: hidden while the first-launch quiz runs.
    private LinearLayout homeSection;
    private Button[] mealButtons;
    private LinearLayout contentContainer;
    private Button moreButton;

    private ChatGptAccount chatGptAccount;
    // Attempt changes, browser launch and session persistence run on the main thread.
    private final ChatGptSignInFlow signInFlow = new ChatGptSignInFlow();
    private final Handler signInHandler = new Handler(Looper.getMainLooper());
    private LoopbackCallbackServer signInServer;
    private Runnable signInTimeout;
    private long signInDeadline;
    private AlertDialog signInFailureDialog;
    private RecipeOperations recipeService;
    private SharedPreferencesBackendStore backendStore;
    private BackendClient backendClient;
    private DishRater dishRater;
    // Ready pool refills run one at a time, in the background (one LLM call each).
    private Executor readyPoolExecutor;
    private final Set<Integer> refillingMeals = new HashSet<>();
    // Dishes (lower-case) already shown in this series: since the meal tile was tapped.
    private final Set<String> seriesShown = new HashSet<>();
    private ReadyProposals.Source proposalSource = ReadyProposals.Source.READY;
    private PreferenceStore preferenceStore;
    private UserPreferences preferences;
    private MealHistoryStore historyStore;
    private MealHistory history;
    private CookbookStore cookbookStore;
    private Cookbook cookbook;
    private DishImporter dishImporter;
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
    // First-launch taste quiz: current question (-1 = quiz not running) and a
    // meal tapped in a notification to open once the quiz is finished or
    // skipped. Held in fields (not saved state) because the manifest's
    // configChanges keeps the Activity alive across rotation.
    private int onboardingStep = -1;
    private int pendingMealIndex = -1;
    // „Co dzisiaj jedliście?" z powiadomienia w trakcie quizu — pytamy po nim.
    private boolean pendingFoodLogPrompt;
    private FoodLogStore foodLogStore;
    // Ankieta A/B: zapisywana po każdej odpowiedzi (wznowienie po przerwie);
    // wybrane dania trafiają do reakcji „lubię" przy zakończeniu albo przerwaniu,
    // więc „Cofnij" podmienia wybór zamiast dokładać polubienia.
    private final TasteSurveyPlanner surveyPlanner = new TasteSurveyPlanner();
    private TasteSurveyStore surveyStore;
    /** Catalog read by the last {@link #ensureSurvey()} (one decode per survey screen). */
    private Recipe[][] surveySnapshot;
    /** Set by Back so the next {@link #ensureSurvey()} keeps the snapshot that approved the pair. */
    private boolean surveySnapshotPinned;
    private TasteSurvey survey;
    // Ankieta otwarta z ekranu startowego/menu, poza quizem: bez pytań profilu
    // i jednorazowych dialogów startowych.
    private boolean surveyOnly;

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
        reactionStore = reactionStoreOverride != null ? reactionStoreOverride
                : new SharedPreferencesDishReactionStore(this);
        reactions = reactionStore.load();
        dataManager = new DataManager(preferenceStore, historyStore, cookbookStore,
                reactionStore);
        householdProfileStore = new SharedPreferencesHouseholdProfileStore(this);
        householdProfile = householdProfileStore.load();
        appSettings = new SharedPreferencesAppSettings(this);
        tasteEventStore = tasteEventStoreOverride != null ? tasteEventStoreOverride
                : new SharedPreferencesTasteEventStore(this);
        tasteEvents = tasteEventStore.load();
        tasteAggregate = tasteEventStore.loadAggregate();
        learningStatsStore = new SharedPreferencesLearningStatsStore(this);
        learningStats = learningStatsStore.load();
        updateStateStore = new SharedPreferencesUpdateStateStore(this);
        surveyStore = new SharedPreferencesTasteSurveyStore(this);
        survey = surveyStore.load();
        foodLogStore = new SharedPreferencesFoodLogStore(this);
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
        scrollView.setBackgroundColor(Ui.BACKGROUND);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(32));
        scrollView.addView(root);

        root.addView(buildHeader(), matchWrap());

        homeSection = new LinearLayout(this);
        homeSection.setOrientation(LinearLayout.VERTICAL);
        root.addView(homeSection, marginTop(20));

        greeting = Ui.text(this, "", 15, Ui.INK_SOFT);
        homeSection.addView(greeting, matchWrap());
        homeSection.addView(Ui.headline(this, "Na co masz dziś ochotę?", 28), marginTop(2));

        // Household context at a glance; each chip opens its setting.
        LinearLayout chips = Ui.row(this);
        homeSection.addView(chips, marginTop(14));
        servingsLabel = new Button(this);
        servingsLabel.setId(R.id.servings_label);
        Ui.chip(servingsLabel, false);
        servingsLabel.setOnClickListener(v -> showServingsDialog(true));
        chips.addView(servingsLabel, Ui.wrap());
        aiChip = new Button(this);
        aiChip.setId(R.id.ai_chip);
        aiChip.setOnClickListener(v -> onAiChipTapped());
        LinearLayout.LayoutParams aiChipParams = Ui.wrap();
        aiChipParams.leftMargin = dp(8);
        chips.addView(aiChip, aiChipParams);

        // One tap to pick the meal — three tiles, each with its time-of-day glyph.
        LinearLayout mealRow = Ui.row(this);
        LinearLayout.LayoutParams mealRowParams = marginTop(20);
        mealRowParams.leftMargin = -dp(5);
        mealRowParams.rightMargin = -dp(5);
        homeSection.addView(mealRow, mealRowParams);

        int[] mealIds = {R.id.meal_breakfast_button, R.id.meal_lunch_button, R.id.meal_dinner_button};
        mealButtons = new Button[MEAL_TYPES.length];
        for (int i = 0; i < MEAL_TYPES.length; i++) {
            final int index = i;
            Button button = new Button(this);
            button.setId(mealIds[i]);
            button.setText(MEAL_TYPES[i]);
            button.setTextSize(15);
            button.setOnClickListener(v -> selectMeal(index));
            mealButtons[i] = button;
            styleMealTile(i, false);
            mealRow.addView(button, Ui.weighted(this, 10));
        }

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentContainer, marginTop(24));

        setContentView(scrollView);
        updateServingsLabel();
        updateAiChip();

        boolean recoverSurvey = survey != null && survey.needsCommitRecovery();
        boolean surveyAnswered = survey != null && survey.isFinished() && survey.position() > 0;
        boolean surveyPaused = survey != null && !survey.isFinished() && survey.isPaused();
        if (!appSettings.isOnboardingDone() && (surveyAnswered || surveyPaused)) {
            // Proces zginął między zakończeniem ankiety („Na razie wystarczy” albo
            // ostatnia para) a zapisem wyborów lub flagi: ankieta jest ostatnim krokiem quizu,
            // więc kończymy go tak, jak zrobiłby to przerwany ekran — wybory trafiają
            // do reakcji raz, a quiz nie zaczyna się od nowa.
            endOnboarding();
            maybeCheckForUpdate();
        } else if (appSettings.isOnboardingDone()) {
            if (recoverSurvey) {
                commitSurveyPicks(); // dokończ przerwany zapis wyborów (bez duplikatów)
            }
            showStartScreen();
            showStartupPrompts();
            maybeCheckForUpdate();
            warmReadyPools();
        } else {
            if (recoverSurvey) {
                commitSurveyPicks(); // przerwany zapis przy „Przerwij” w trakcie quizu
            }
            // Fresh install: a short taste quiz first; every one-time dialog
            // (servings, ChatGPT sign-in, notification permission) waits until
            // it is finished or skipped. An interrupted survey resumes where it stopped.
            startOnboarding(isSurveyUnfinished() ? SURVEY_STEP : 0);
        }

        // Daily meal reminders (8/12/18) and the evening "Co dzisiaj jedliście?"
        // question. Scheduling is idempotent.
        MealNotifications.ensureChannel(this);
        FoodLogNotifications.ensureChannel(this);
        new MealReminderScheduler().scheduleAll(this);
        handleMealIntent(getIntent());
    }

    /** App bar: logo, wordmark and the overflow ("Więcej") button. */
    private View buildHeader() {
        LinearLayout header = Ui.row(this);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_brand_mark);
        logo.setScaleType(ImageView.ScaleType.FIT_XY);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(11));
            }
        });
        logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView wordmark = Ui.headline(this, "Mealspire", 22);
        LinearLayout.LayoutParams wordmarkParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wordmarkParams.leftMargin = dp(12);
        header.addView(wordmark, wordmarkParams);

        moreButton = new Button(this);
        moreButton.setId(R.id.more_button);
        moreButton.setContentDescription("Więcej");
        Drawable dots = tinted(R.drawable.ic_more, Ui.INK_BODY);
        dots.setBounds(0, 0, dp(24), dp(24));
        moreButton.setCompoundDrawables(dots, null, null, null);
        moreButton.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Ui.RIPPLE_ON_LIGHT), null, Ui.circle(Color.WHITE)));
        moreButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        moreButton.setMinWidth(0);
        moreButton.setMinimumWidth(0);
        moreButton.setMinHeight(0);
        moreButton.setMinimumHeight(0);
        moreButton.setStateListAnimator(null);
        moreButton.setOnClickListener(view -> showMoreMenu());
        header.addView(moreButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return header;
    }

    /** A vector icon in {@code color}, mutated so shared drawables stay untouched. */
    private Drawable tinted(int drawableRes, int color) {
        Drawable drawable = getDrawable(drawableRes).mutate();
        drawable.setTint(color);
        return drawable;
    }

    /** Meal tile: glyph over label; the picked meal flips to filled terracotta. */
    private void styleMealTile(int index, boolean selected) {
        Button tile = mealButtons[index];
        int fill = selected ? Ui.ACCENT : Ui.SURFACE;
        int ink = selected ? Color.WHITE : Ui.INK;
        tile.setBackground(new RippleDrawable(
                ColorStateList.valueOf(selected ? Ui.RIPPLE_ON_ACCENT : Ui.RIPPLE_ON_LIGHT),
                Ui.outlined(this, fill, selected ? Ui.ACCENT : Ui.OUTLINE, 20),
                Ui.rounded(this, Color.WHITE, 20)));
        tile.setTextColor(ink);
        tile.setAllCaps(false);
        tile.setStateListAnimator(null);
        tile.setElevation(selected ? dp(2) : 0f);
        tile.setTypeface(Ui.MEDIUM);
        Drawable icon = tinted(MEAL_ICONS[index], selected ? Color.WHITE : Ui.ACCENT);
        tile.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        tile.setCompoundDrawablePadding(dp(8));
        tile.setPadding(dp(4), dp(18), dp(4), dp(16));
    }

    /** Chip under the greeting: AI on the user's ChatGPT account, or offline. */
    private void updateAiChip() {
        boolean ai = isAiAvailable();
        String label;
        if (ai) {
            label = "AI · " + chatGptAccount.modelChoice().label;
        } else if (signInFlow.isRunning()) {
            label = SIGN_IN_CHIP_LABEL;
        } else {
            label = "Tryb offline";
        }
        aiChip.setText(label);
        Ui.chip(aiChip, ai);
    }

    private void onAiChipTapped() {
        if (signInFlow.isRunning()) {
            startChatGptSignIn();
        } else if (!chatGptAccount.isSignedIn()) {
            offerChatGptSignIn();
        } else if (!backendStore.serverNoticeShown()) {
            showServerNotice();
        } else {
            showModelChoiceDialog();
        }
    }

    /**
     * One-time prompts asked outside the quiz, so nothing covers the first
     * question: the servings dialog, the ChatGPT sign-in offer (only while
     * signed out) or, once, the server notice for sessions from older versions,
     * and the Android 13+ notification permission.
     */
    private void showStartupPrompts() {
        maybeAskServings();
        if (!chatGptAccount.isSignedIn()) {
            offerChatGptSignIn();
        } else if (!backendStore.serverNoticeShown()) {
            showServerNotice();
        }
        maybeRequestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from the browser: the stored session is the truth, whatever was on screen.
        if (aiChip != null) {
            updateAiChip();
        }
    }

    @Override
    protected void onDestroy() {
        signInFlow.cancel();
        releaseSignInResources();
        super.onDestroy();
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
                previousOnboardingStep();
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
        greeting.setText(DayGreeting.forHour(
                java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)));
        showWelcome();
        maybeShowUpdateBanner();
    }

    /** Start-screen empty state: what the app does and how it learns. */
    private void showWelcome() {
        contentContainer.removeAllViews();
        LinearLayout card = Ui.card(this);
        card.setBackground(Ui.rounded(this, Ui.SURFACE_MUTED, Ui.CARD_CORNER_DP));
        card.setElevation(0f);
        card.addView(Ui.headline(this, "Trzy proste pomysły na jedno dotknięcie", 19),
                matchWrap());
        card.addView(Ui.body(this, "Wybierz porę dnia, a podsunę dania, które łatwo "
                + "ugotować z tego, co zwykle jest w domu."), marginTop(8));
        TextView learning = Ui.callout(this, "„Lubię to” i „Nie lubię” uczą Mealspire "
                + "Waszego gustu — z każdą reakcją propozycje trafiają lepiej.",
                Ui.HERB_SOFT, Ui.HERB);
        card.addView(learning, marginTop(14));
        contentContainer.addView(card, matchWrap());
        // Przerwana ankieta czeka tu na dokończenie; bez ankiety (pominięty quiz,
        // aktualizacja ze starszej wersji) — zaproszenie, żeby ją zrobić.
        if (survey == null || isSurveyUnfinished()) {
            Button surveyButton = new Button(this);
            surveyButton.setId(R.id.survey_resume_button);
            surveyButton.setTextSize(16);
            if (survey == null) {
                surveyButton.setText("Ankieta gustu: A czy B?");
                Ui.outlinedButton(surveyButton);
            } else {
                surveyButton.setText("Dokończ ankietę gustu (" + survey.position() + " z "
                        + survey.size() + ")");
                Ui.primary(surveyButton);
            }
            surveyButton.setOnClickListener(v -> openSurvey(0));
            contentContainer.addView(surveyButton, marginTop(16));
        }
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
        Ui.tonal(banner);
        banner.setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.RIPPLE_ON_LIGHT),
                Ui.rounded(this, Ui.ACCENT_SOFT, 16), Ui.rounded(this, Color.WHITE, 16)));
        banner.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(18), dp(14), dp(18), dp(14));
        banner.setCompoundDrawablesWithIntrinsicBounds(
                tinted(R.drawable.ic_download, Ui.ACCENT_DEEP), null, null, null);
        banner.setCompoundDrawablePadding(dp(12));
        banner.setOnClickListener(v -> startActivity(
                com.mealspire.app.update.UpdateActivity.intent(this, latest)));
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(16);
        contentContainer.addView(banner, 0, params);
    }

    /** Opens the meal carried by a tapped reminder notification, if any. */
    private void handleMealIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra(EXTRA_FOOD_LOG, false)) {
            intent.removeExtra(EXTRA_FOOD_LOG);
            if (isOnboardingActive()) {
                pendingFoodLogPrompt = true;
            } else {
                showFoodLogDialog();
            }
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
        com.mealspire.app.domain.HttpTransport transport = backendTransportOverride != null
                ? backendTransportOverride : new HttpUrlTransport();
        chatGptAccount = new ChatGptAccount(new SharedPreferencesChatGptSessionStore(this),
                transport, new ChatGptOAuth(new SecureRandom()), new IdTokenVerifier(),
                System::currentTimeMillis);
        backendStore = new SharedPreferencesBackendStore(this);
        backendClient = new BackendClient(backendStore, chatGptAccount, transport);
        recipeService = backendClient;
        dishRater = backendClient;
        dishImporter = backendClient;
    }

    private boolean isAiAvailable() {
        return chatGptAccount.isSignedIn() && backendStore.serverNoticeShown();
    }

    private void offerChatGptSignIn() {
        new AlertDialog.Builder(this)
                .setTitle(SIGN_IN_DIALOG_TITLE)
                .setMessage("AI korzysta z Twojego konta ChatGPT (plan Plus lub Pro) — "
                        + "logujesz się w przeglądarce, aplikacja nie ma własnego klucza. "
                        + "Zapytania AI przechodzą przez serwer Mealspire, który otrzymuje dane "
                        + "gustu i krótkotrwały token dostępu ChatGPT. "
                        + "Bez logowania aplikacja działa offline (losuje dania z bazy).")
                .setPositiveButton("Zaloguj się", (dialog, which) -> {
                    backendStore.markServerNoticeShown();
                    startChatGptSignIn();
                })
                .setNegativeButton("Pomiń", null)
                .show();
    }

    /** AI stays offline until the user accepts the built-in server notice. */
    private void showServerNotice() {
        new AlertDialog.Builder(this)
                .setTitle(SERVER_NOTICE_TITLE)
                .setMessage("Zapytania AI przechodzą przez serwer Mealspire, który otrzymuje "
                        + "dane gustu i krótkotrwały token dostępu ChatGPT. Jeśli wolisz tryb "
                        + "offline, wyloguj się z ChatGPT.")
                .setPositiveButton("OK", (dialog, which) -> {
                    backendStore.markServerNoticeShown();
                    updateAiChip();
                    // An open meal refreshes the catalog first, then refills its own pool.
                    if (currentMealIndex >= 0) generateProposals(); else warmReadyPools();
                })
                .setNegativeButton("Wyloguj z ChatGPT", (dialog, which) -> signOutOfChatGpt())
                .show();
    }

    /**
     * Sign in with ChatGPT: a loopback listener catches the browser redirect,
     * then the code is exchanged (PKCE) for the user's own tokens. While one
     * attempt waits, a second tap explains it and offers to start over — the
     * browser page may have shown its own error while the app was frozen.
     */
    private void startChatGptSignIn() {
        if (!signInFlow.isRunning()) {
            beginChatGptSignIn();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(SIGN_IN_PENDING_TITLE)
                .setMessage("Dokończ logowanie w przeglądarce i wróć do aplikacji. "
                        + "Jeśli strona logowania pokazała błąd albo ją zamknięto, "
                        + "zacznij od nowa.")
                .setPositiveButton(SIGN_IN_RESTART_LABEL, (dialog, which) -> beginChatGptSignIn())
                .setNegativeButton("Czekaj", null)
                .show();
    }

    private void beginChatGptSignIn() {
        dismissSignInFailure();
        final int attempt = signInFlow.start();
        releaseSignInResources();
        signInDeadline = SystemClock.uptimeMillis() + SIGN_IN_TIMEOUT_MS;
        signInTimeout = () -> onSignInFailed(attempt, SIGN_IN_TIMEOUT_MESSAGE);
        signInHandler.postAtTime(signInTimeout, signInDeadline);
        updateAiChip();
        executeSignIn(() -> {
            try {
                final LoopbackCallbackServer server = new LoopbackCallbackServer();
                if (!adoptSignInServer(attempt, server)) {
                    closeSignInServer(server);
                    return;
                }
                final ChatGptOAuth.PendingSignIn pending =
                        chatGptAccount.beginSignIn(server.redirectUri());
                runOnUiThread(() -> launchSignInBrowser(attempt, server, pending));
            } catch (IOException e) {
                runOnUiThread(() -> onSignInFailed(attempt, e.getMessage()));
            } catch (RuntimeException e) {
                runOnUiThread(() -> onSignInFailed(attempt, null));
            }
        });
    }

    private synchronized boolean adoptSignInServer(int attempt, LoopbackCallbackServer server) {
        if (!signInFlow.isCurrent(attempt)) {
            return false;
        }
        signInServer = server;
        return true;
    }

    private void launchSignInBrowser(int attempt, LoopbackCallbackServer server,
                                     ChatGptOAuth.PendingSignIn pending) {
        if (!canContinueSignIn(attempt)) {
            closeSignInServer(server);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(pending.authorizeUrl)));
        } catch (android.content.ActivityNotFoundException e) {
            onSignInFailed(attempt, "Brak przeglądarki do zalogowania w ChatGPT.");
            return;
        } catch (RuntimeException e) {
            onSignInFailed(attempt, null);
            return;
        }
        final long deadline = signInDeadline;
        executeSignIn(() -> {
            try (LoopbackCallbackServer listener = server) {
                int remainingMs = (int) Math.max(0, deadline - SystemClock.uptimeMillis());
                String query = listener.awaitCallbackQuery(remainingMs);
                ChatGptSession session = chatGptAccount.exchangeSignIn(pending, query);
                runOnUiThread(() -> onSignInSucceeded(attempt, session));
            } catch (IOException e) {
                runOnUiThread(() -> onSignInFailed(attempt, e.getMessage()));
            } catch (RuntimeException e) {
                runOnUiThread(() -> onSignInFailed(attempt, null));
            }
        });
    }

    private void executeSignIn(Runnable command) {
        if (signInExecutorOverride != null) {
            signInExecutorOverride.execute(command);
        } else {
            new Thread(command, "chatgpt-sign-in").start();
        }
    }

    private boolean canContinueSignIn(int attempt) {
        if (!signInFlow.isCurrent(attempt) || isFinishing() || isDestroyed()) {
            return false;
        }
        if (SystemClock.uptimeMillis() >= signInDeadline) {
            onSignInFailed(attempt, SIGN_IN_TIMEOUT_MESSAGE);
            return false;
        }
        return true;
    }

    private synchronized void releaseSignInResources() {
        if (signInTimeout != null) {
            signInHandler.removeCallbacks(signInTimeout);
            signInTimeout = null;
        }
        if (signInServer != null) {
            closeSignInServer(signInServer);
            signInServer = null;
        }
    }

    private static void closeSignInServer(LoopbackCallbackServer server) {
        try {
            server.close();
        } catch (IOException ignored) {
            // The attempt is already ending.
        }
    }

    private void onSignInSucceeded(int attempt, ChatGptSession session) {
        if (!canContinueSignIn(attempt)) {
            return;
        }
        try {
            chatGptAccount.saveSignIn(session);
        } catch (RuntimeException e) {
            onSignInFailed(attempt, null);
            return;
        }
        signInFlow.succeeded(attempt);
        releaseSignInResources();
        dismissSignInFailure();
        updateAiChip();
        warmReadyPools();
        // Refresh the open recipe so the AI-only "Zmień przepis" button appears.
        if (currentRecipe != null) {
            showFullRecipe(currentRecipe);
        }
        String email = session.email;
        toast(email.isEmpty() ? "Zalogowano do ChatGPT — możesz generować dania."
                : "Zalogowano do ChatGPT jako " + email + ".");
    }

    private void onSignInFailed(int attempt, String message) {
        if (signInFlow.failed(attempt) != ChatGptSignInFlow.Outcome.SHOW_FAILURE) {
            return;
        }
        releaseSignInResources();
        if (isFinishing() || isDestroyed()) {
            return;
        }
        updateAiChip();
        dismissSignInFailure();
        signInFailureDialog = new AlertDialog.Builder(this)
                .setTitle(SIGN_IN_FAILED_TITLE)
                .setMessage(message != null ? message
                        : "Nie udało się zalogować do ChatGPT. Spróbuj ponownie.")
                .setPositiveButton(SIGN_IN_RETRY_LABEL, (dialog, which) -> beginChatGptSignIn())
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void dismissSignInFailure() {
        if (signInFailureDialog != null && signInFailureDialog.isShowing()) {
            signInFailureDialog.dismiss();
        }
        signInFailureDialog = null;
    }

    /** GPT Sol (default) or GPT Luna; a model missing on the account is marked, not swapped. */
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
                            updateAiChip();
                            dialog.dismiss();
                            toast("AI używa teraz modelu " + models[which].label + ".");
                        })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void signOutOfChatGpt() {
        signInFlow.cancel();
        releaseSignInResources();
        new Thread(() -> {
            chatGptAccount.signOut();
            runOnUiThread(this::updateAiChip);
        }).start();
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
    private void startOnboarding(int step) {
        onboardingStep = step;
        setMealButtonsEnabled(false);
        homeSection.setVisibility(View.GONE);
        Ui.setEnabledWithFade(moreButton, false);
        renderOnboardingStep();
    }

    private boolean isSurveyUnfinished() {
        return survey != null && !survey.isFinished();
    }

    /**
     * Plans the survey lazily — after the diet question, so exclusions ticked
     * moments earlier already filter the pairs. A diet change before anything
     * was saved replans (dropping answers that may now be invalid); after
     * that, only the pairs still ahead that break the diet are dropped.
     * Pairs ahead with a dish missing from the current catalog (a name saved
     * before an update renamed it) are dropped the same way — or the survey
     * is replanned when it has no answer at all — so a stale name is never shown
     * and never slips past the diet unchecked.
     */
    private void ensureSurvey() {
        // One catalog snapshot per survey screen: planning, pruning and rendering see the same dishes
        // (after Back: the snapshot the rewound pair was approved against).
        if (!surveySnapshotPinned || surveySnapshot == null) {
            surveySnapshot = surveyCatalog();
        }
        surveySnapshotPinned = false;
        String dietKey = surveyDietKey();
        if (survey != null && survey.needsCommitRecovery()) {
            commitSurveyPicks(); // nowa ankieta nie może zgubić niezapisanych wyborów
            if (survey.isFinished() && survey.needsCommitRecovery()) {
                return; // zapis nadal się nie udaje: stara ankieta zostaje do ponowienia
            }
        }
        if (survey == null || survey.isFinished()
                || (!dietKey.equals(survey.dietKey()) && survey.committed() == 0)) {
            planSurvey();
            return;
        }
        Recipe[][] catalog = surveySnapshot;
        java.util.Map<String, String> details = surveyDetailsByTitle(catalog);
        // Pairs are planned from the active catalog only (not the cookbook, which may keep old names).
        java.util.Set<String> catalogTitles = new java.util.HashSet<>();
        for (Recipe[] meal : catalog) {
            for (Recipe recipe : meal) {
                catalogTitles.add(recipe.getTitle());
            }
        }
        TasteSurvey current = survey.withoutUnknownDishes(catalogTitles);
        if (current != survey && current.committed() == 0 && !current.hasAnswers()) {
            planSurvey();
            return;
        }
        // Pairs ahead are rechecked on every screen: the diet or the catalog's ingredients may change.
        TasteSurvey filtered = current.withoutDisallowed(householdProfile.getDiet(), details, dietKey);
        if (filtered.size() != current.size() || !dietKey.equals(current.dietKey())) {
            current = filtered;
        }
        if (current != survey) {
            saveSurvey(current);
        }
    }

    private void planSurvey() {
        Recipe[][] pools = surveySnapshot != null ? surveySnapshot : surveyCatalog();
        saveSurvey(TasteSurvey.start(surveyPlanner.plan(pools, householdProfile.getDiet(),
                appSettings.loadSurveyLength(), random), surveyDietKey()));
    }

    private String surveyDietKey() {
        return householdProfile.getDiet().getExclusions().toString();
    }

    private void saveSurvey(TasteSurvey updated) {
        survey = updated;
        surveyStore.save(updated);
    }

    /**
     * Dish details for the survey's pairs: the active catalog wins (pairs come only from it, so
     * diet checks and option texts use its ingredients); the cookbook covers earlier picks.
     */
    private java.util.Map<String, String> surveyDetailsByTitle() {
        return surveyDetailsByTitle(surveySnapshot != null ? surveySnapshot : surveyCatalog());
    }

    /** Details of the active catalog only (no cookbook): what a survey pair may show. */
    private java.util.Map<String, String> surveyCatalogDetails() {
        java.util.Map<String, String> details = new java.util.HashMap<>();
        for (Recipe[] meal : surveySnapshot != null ? surveySnapshot : surveyCatalog()) {
            for (Recipe recipe : meal) {
                details.put(recipe.getTitle(), recipe.getDetails());
            }
        }
        return details;
    }

    private Recipe[][] surveyCatalog() {
        return backendStore.catalog(MEAL_TYPES.length);
    }

    private java.util.Map<String, String> surveyDetailsByTitle(Recipe[][] catalog) {
        java.util.Map<String, String> details = BuiltInRecipes.detailsByTitle(cookbook);
        for (Recipe[] meal : catalog) {
            for (Recipe recipe : meal) {
                details.put(recipe.getTitle(), recipe.getDetails());
            }
        }
        return details;
    }

    private void renderOnboardingStep() {
        contentContainer.removeAllViews();
        if (onboardingStep == SURVEY_STEP) {
            ensureSurvey();
            if (survey.isFinished()) {
                // Empty survey (diet excludes the whole pool): nothing to ask.
                advanceOnboarding();
            } else if (survey.awaitsDecision()) {
                renderSurveyCheckpoint();
            } else {
                renderSurveyPair();
            }
            return;
        }

        if (onboardingStep == 0) {
            contentContainer.addView(Ui.overline(this, "Poznajmy się"), matchWrap());
            contentContainer.addView(Ui.body(this, "Parę szybkich pytań o Was i Wasze "
                    + "jedzenie, potem ankieta gustu — żeby pierwsze propozycje od razu "
                    + "pasowały. Każde pytanie możesz pominąć."), marginTop(6));
        }
        contentContainer.addView(buildQuizProgress(),
                marginTop(onboardingStep == 0 ? 18 : 4));

        TextView progress = Ui.text(this,
                "Pytanie " + (onboardingStep + 1) + " z " + ONBOARDING_QUESTIONS
                        + ", potem ankieta gustu", 14, Ui.INK_SOFT);
        contentContainer.addView(progress, marginTop(10));

        TextView question = Ui.headline(this, "", 24);
        question.setId(R.id.onboarding_question);
        contentContainer.addView(question, marginTop(4));

        switch (onboardingStep) {
            case 0:
                question.setText(Q_AUDIENCE);
                for (int i = 0; i < AUDIENCE_LABELS.length; i++) {
                    final HouseholdProfile.Audience value = AUDIENCE_VALUES[i];
                    addOnboardingOption(i + 1, AUDIENCE_LABELS[i], () -> {
                        saveProfile(householdProfile.withAudience(value));
                        advanceOnboarding();
                    });
                }
                break;
            case 1:
                question.setText(Q_DIET);
                TextView dietNote = Ui.text(this, "Tego nigdy nie zaproponuję. Możesz "
                        + "zaznaczyć kilka odpowiedzi albo nic.", 15, Ui.INK_SOFT);
                contentContainer.addView(dietNote, marginTop(6));
                LinearLayout dietCard = Ui.card(this);
                dietCard.setElevation(0f);
                dietCard.setPadding(dp(8), dp(6), dp(16), dp(6));
                for (final DietConstraints.Exclusion exclusion
                        : DietConstraints.Exclusion.values()) {
                    CheckBox dietBox = quizCheckBox(exclusion.label(),
                            householdProfile.getDiet().getExclusions().contains(exclusion));
                    dietBox.setOnCheckedChangeListener((view, checked) ->
                            toggleExclusion(exclusion, checked));
                    dietCard.addView(dietBox, matchWrap());
                }
                contentContainer.addView(dietCard, marginTop(16));
                addQuizNextButton();
                break;
            case 2:
                question.setText(Q_TIME);
                for (int i = 0; i < TIME_LABELS.length; i++) {
                    final HouseholdProfile.CookingTime value = TIME_VALUES[i];
                    addOnboardingOption(i + 1, TIME_LABELS[i], () -> {
                        saveProfile(householdProfile.withTime(value));
                        advanceOnboarding();
                    });
                }
                break;
            case 3:
                question.setText(Q_SKILL);
                for (int i = 0; i < SKILL_LABELS.length; i++) {
                    final HouseholdProfile.CookingSkill value = SKILL_VALUES[i];
                    addOnboardingOption(i + 1, SKILL_LABELS[i], () -> {
                        saveProfile(householdProfile.withSkill(value));
                        advanceOnboarding();
                    });
                }
                break;
            case 4:
                question.setText(Q_CUISINES);
                contentContainer.addView(Ui.text(this, "Zaznacz dowolnie wiele albo nic — "
                        + "inne kuchnie też będą się pojawiać.", 15, Ui.INK_SOFT), marginTop(6));
                LinearLayout cuisineCard = Ui.card(this);
                cuisineCard.setElevation(0f);
                cuisineCard.setPadding(dp(8), dp(6), dp(16), dp(6));
                for (final String cuisine : CUISINE_OPTIONS) {
                    CheckBox cuisineBox = quizCheckBox(cuisine,
                            householdProfile.getCuisines().contains(cuisine));
                    cuisineBox.setOnCheckedChangeListener((view, checked) ->
                            toggleCuisine(cuisine, checked));
                    cuisineCard.addView(cuisineBox, matchWrap());
                }
                contentContainer.addView(cuisineCard, marginTop(16));
                addQuizNextButton();
                break;
            case 5:
                question.setText(Q_SPICE);
                addProfileOptions(SPICE_LABELS, SPICE_VALUES, HouseholdProfile::withSpice);
                break;
            case 6:
                question.setText(Q_STYLE);
                addProfileOptions(STYLE_LABELS, STYLE_VALUES, HouseholdProfile::withStyle);
                break;
            case 7:
                question.setText(Q_BUDGET);
                addProfileOptions(BUDGET_LABELS, BUDGET_VALUES, HouseholdProfile::withBudget);
                break;
            case 8:
                question.setText(Q_NOVELTY);
                addProfileOptions(NOVELTY_LABELS, NOVELTY_VALUES, HouseholdProfile::withNovelty);
                break;
        }

        addQuizSkipButton("Pomiń");
    }

    /** Single-choice quiz answers: each saves into the profile and moves on. */
    private <T> void addProfileOptions(String[] labels, T[] values,
                                       final ProfileUpdate<T> update) {
        for (int i = 0; i < labels.length; i++) {
            final T value = values[i];
            addOnboardingOption(i + 1, labels[i], () -> {
                saveProfile(update.apply(householdProfile, value));
                advanceOnboarding();
            });
        }
    }

    private CheckBox quizCheckBox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(17);
        box.setTextColor(Ui.INK);
        box.setButtonTintList(ColorStateList.valueOf(Ui.ACCENT));
        box.setMinHeight(dp(52));
        box.setPadding(dp(8), 0, 0, 0);
        box.setChecked(checked);
        return box;
    }

    private void addQuizNextButton() {
        Button next = new Button(this);
        next.setId(R.id.onboarding_next_button);
        next.setText("Dalej");
        next.setTextSize(17);
        Ui.primary(next);
        next.setOnClickListener(v -> advanceOnboarding());
        contentContainer.addView(next, marginTop(16));
    }

    /** Saves each (un)ticked cuisine immediately, so skipping keeps the answers. */
    private void toggleCuisine(String cuisine, boolean liked) {
        List<String> cuisines = new ArrayList<>(householdProfile.getCuisines());
        if (liked) {
            if (!cuisines.contains(cuisine)) {
                cuisines.add(cuisine);
            }
        } else {
            cuisines.remove(cuisine);
        }
        saveProfile(householdProfile.withCuisines(cuisines));
    }

    private void addQuizSkipButton(String label) {
        Button skip = new Button(this);
        skip.setId(R.id.onboarding_skip_button);
        skip.setText(label);
        skip.setTextSize(16);
        Ui.ghost(skip);
        skip.setOnClickListener(v -> endOnboarding());
        contentContainer.addView(skip, marginTop(20));
    }

    /**
     * One A/B pair of the taste survey: two dishes, „Żadne z tych", and the
     * progress through the whole survey. Every answer is saved at once, so an
     * interrupted survey resumes at this pair.
     */
    private void renderSurveyPair() {
        if (survey.position() == 0) {
            contentContainer.addView(Ui.overline(this, "Ankieta gustu"), matchWrap());
            contentContainer.addView(Ui.body(this, "Wybieraj szybko, bez zastanawiania — "
                    + "z odpowiedzi zbiorę dania, które lubicie, i od nich zacznę "
                    + "propozycje. Możesz przerwać i dokończyć później."), marginTop(6));
        }
        // Postęp lokalny dla bieżącej serii (1–20, ostatnia może być krótsza), nie całej ankiety.
        contentContainer.addView(buildProgressBar(survey.seriesPosition(), survey.seriesLength()),
                marginTop(survey.position() == 0 ? 18 : 4));
        TextView progress = Ui.text(this, "Porównanie " + (survey.seriesPosition() + 1) + " z "
                + survey.seriesLength(), 14, Ui.INK_SOFT);
        progress.setId(R.id.survey_progress);
        contentContainer.addView(progress, marginTop(10));

        TasteSurvey.Pair pair = survey.current();
        int meal = pair.getMeal();
        TextView question = Ui.headline(this, meal >= 0 && meal < MEAL_ACCUSATIVE.length
                ? "Co wolisz na " + MEAL_ACCUSATIVE[meal] + "?" : "Co wolisz?", 24);
        question.setId(R.id.onboarding_question);
        contentContainer.addView(question, marginTop(4));

        java.util.Map<String, String> details = surveyDetailsByTitle();
        addSurveyOption(1, pair.getDishA(), details.get(pair.getDishA()), TasteSurvey.CHOICE_A);
        TextView or = Ui.text(this, "albo", 14, Ui.INK_SOFT);
        or.setGravity(android.view.Gravity.CENTER);
        contentContainer.addView(or, marginTop(8));
        addSurveyOption(2, pair.getDishB(), details.get(pair.getDishB()), TasteSurvey.CHOICE_B);

        // Wymuszony wybór to fałszywy sygnał — „Żadne z tych" nic nie zapisuje.
        Button none = new Button(this);
        none.setId(R.id.onboarding_option_none);
        none.setText("Żadne z tych");
        none.setTextSize(16);
        Ui.outlinedButton(none);
        none.setOnClickListener(v -> answerSurvey(TasteSurvey.CHOICE_NEITHER));
        contentContainer.addView(none, marginTop(12));

        addQuizSkipButton("Przerwij — dokończę później");
    }

    /**
     * End of a series: carry on with the next one ("Dalej", answers kept) or
     * finish with what was answered ("Na razie wystarczy") — the recommender
     * learns from the partial survey, nothing forces the rest.
     */
    private void renderSurveyCheckpoint() {
        int next = Math.min(TasteSurvey.SERIES_PAIRS, survey.size() - survey.position());
        contentContainer.addView(Ui.overline(this, "Ankieta gustu"), matchWrap());
        contentContainer.addView(buildProgressBar(1, 1), marginTop(18));
        TextView question = Ui.headline(this, "Masz za sobą " + survey.position()
                + " porównań. Gramy dalej?", 24);
        question.setId(R.id.onboarding_question);
        contentContainer.addView(question, marginTop(10));
        contentContainer.addView(Ui.body(this, "Z tego, co już wybraliście, zacznę "
                + "propozycje. Kolejne " + next + " porównań pomoże lepiej poznać Wasz "
                + "gust — ale nie musisz."), marginTop(6));

        Button more = new Button(this);
        more.setId(R.id.survey_continue_button);
        more.setText("Dalej");
        more.setTextSize(16);
        Ui.primary(more);
        more.setOnClickListener(v -> {
            saveSurvey(survey.continueSeries());
            renderOnboardingStep();
        });
        contentContainer.addView(more, marginTop(18));

        Button enough = new Button(this);
        enough.setId(R.id.survey_enough_button);
        enough.setText("Na razie wystarczy");
        enough.setTextSize(16);
        Ui.outlinedButton(enough);
        enough.setOnClickListener(v -> {
            saveSurvey(survey.finishEarly());
            advanceOnboarding();
        });
        contentContainer.addView(enough, marginTop(10));
        // Pauza: odpowiedzi się zapisują, a wznowienie wraca do tej samej decyzji.
        addQuizSkipButton("Przerwij — dokończę później");
    }

    private void addSurveyOption(int index, String dish, String details, int choice) {
        addOnboardingOption(index, dish, () -> answerSurvey(choice));
        String description = DishReaction.describe(details);
        if (!description.isEmpty()) {
            contentContainer.addView(Ui.text(this, description, 13, Ui.INK_SOFT), marginTop(4));
        }
    }

    private void answerSurvey(int choice) {
        saveSurvey(survey.answer(choice));
        if (survey.isFinished()) {
            advanceOnboarding();
        } else {
            renderOnboardingStep();
        }
    }

    /** One step back in the quiz: the previous pair, question, or out of a standalone survey. */
    private void previousOnboardingStep() {
        if (onboardingStep == SURVEY_STEP && survey != null) {
            surveySnapshot = surveyCatalog(); // the catalog the rewound pair is checked against
        }
        if (onboardingStep == SURVEY_STEP && survey != null
                && survey.canGoBack(householdProfile.getDiet(), surveyCatalogDetails())) {
            surveySnapshotPinned = true;
            saveSurvey(survey.back());
        } else if (onboardingStep == SURVEY_STEP && surveyOnly) {
            endOnboarding();
            return;
        } else {
            onboardingStep--;
        }
        renderOnboardingStep();
    }

    /** Thin filled bar for the long survey (segments would be unreadably small). */
    private View buildProgressBar(int done, int total) {
        LinearLayout bar = Ui.row(this);
        float fraction = total <= 0 ? 0f : (float) done / total;
        View filled = new View(this);
        filled.setBackground(Ui.rounded(this, Ui.ACCENT, 3));
        bar.addView(filled, new LinearLayout.LayoutParams(0, dp(6), Math.max(fraction, 0.01f)));
        View rest = new View(this);
        rest.setBackground(Ui.rounded(this, Ui.OUTLINE, 3));
        bar.addView(rest, new LinearLayout.LayoutParams(0, dp(6), Math.max(1f - fraction, 0.01f)));
        return bar;
    }

    /** Segmented progress bar: one segment per quiz step, done ones filled. */
    private View buildQuizProgress() {
        LinearLayout bar = Ui.row(this);
        for (int i = 0; i < ONBOARDING_STEPS; i++) {
            View segment = new View(this);
            segment.setBackground(Ui.rounded(this,
                    i <= onboardingStep ? Ui.ACCENT : Ui.OUTLINE, 3));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(6), 1f);
            params.leftMargin = i == 0 ? 0 : dp(4);
            bar.addView(segment, params);
        }
        return bar;
    }

    /** One big, readable answer card; index picks the stable test id. */
    private void addOnboardingOption(int index, String label, final Runnable action) {
        Button option = new Button(this);
        int[] optionIds = {R.id.onboarding_option_1, R.id.onboarding_option_2,
                R.id.onboarding_option_3};
        if (index >= 1 && index <= optionIds.length) {
            option.setId(optionIds[index - 1]);
        }
        option.setText(label);
        option.setTextSize(17);
        Ui.choice(option);
        Drawable chevron = tinted(R.drawable.ic_chevron, Ui.ACCENT);
        option.setCompoundDrawablesWithIntrinsicBounds(null, null, chevron, null);
        option.setCompoundDrawablePadding(dp(12));
        option.setOnClickListener(v -> action.run());
        contentContainer.addView(option, marginTop(10));
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
     * so far stay saved, and a meal tapped in a notification opens now. Also
     * ends (or interrupts) a survey started later from the start screen.
     */
    private void endOnboarding() {
        boolean standalone = surveyOnly;
        surveyOnly = false;
        // Najpierw zapis wyborów (z trwałym znacznikiem i zamiarem pauzy), dopiero potem
        // flaga: zabicie procesu pomiędzy nie zostawi pauzy bez odzyskania ani nie
        // otworzy ankiety ponownie przy starcie.
        if (survey != null && !survey.isFinished() && !survey.isPaused()) {
            saveSurvey(survey.withPaused(true));
        }
        boolean learned = commitSurveyPicks();
        appSettings.markOnboardingDone();
        onboardingStep = -1;
        homeSection.setVisibility(View.VISIBLE);
        Ui.setEnabledWithFade(moreButton, true);
        if (survey != null && survey.isFinished() && learned) {
            toast("Ankieta gotowa — lubiane dania: " + survey.likedDishes().size()
                    + ". Od nich zaczną się propozycje.");
        }
        if (!standalone) {
            showStartupPrompts();
        }
        if (learned) {
            warmReadyPools(); // nowe polubienia zmieniają podpis gustu gotowej puli
        }
        if (pendingFoodLogPrompt) {
            pendingFoodLogPrompt = false;
            showFoodLogDialog();
        }
        if (pendingMealIndex >= 0) {
            int meal = pendingMealIndex;
            pendingMealIndex = -1;
            setMealButtonsEnabled(true);
            selectMeal(meal);
        } else {
            showStartScreen();
        }
    }

    /**
     * Saves the survey's not-yet-saved picks as ordinary likes — the shared
     * reaction list, likes and ONBOARDING_PICK taste events — and marks them
     * final. Returns whether anything new was learned.
     */
    private boolean commitSurveyPicks() {
        if (survey == null) {
            return false;
        }
        if (survey.committed() == survey.position()) {
            if (survey.isCommitting()) {
                saveSurvey(survey.commit());
            }
            return false;
        }
        // Najpierw utrwal znacznik tej partii: po zabiciu procesu w trakcie zapisu
        // ponowienie dopisze tylko to, czego z niej jeszcze brakuje. Bez trwałego
        // znacznika nic nie dopisujemy — ponowienie nie umiałoby uniknąć duplikatów.
        survey = survey.beginCommit(System.currentTimeMillis());
        if (!surveyStore.saveDurably(survey)) {
            return false;
        }
        long stamp = survey.commitStamp();
        List<String> picks = survey.pendingPicks();
        List<String> recordedReactions = new ArrayList<>();
        for (DishReaction reaction : reactions.all()) {
            if (reaction.isLiked() && reaction.getTimeMillis() == stamp) {
                recordedReactions.add(reaction.getDish());
            }
        }
        List<String> recordedEvents = new ArrayList<>();
        for (TasteEvent event : tasteEvents.events()) {
            if (event.getType() == TasteEvent.Type.ONBOARDING_PICK
                    && event.getTimestamp() == stamp) {
                recordedEvents.add(event.getDishTitle());
            }
        }
        java.util.Map<String, String> detailsByTitle = surveyDetailsByTitle();
        for (String pick : TasteSurvey.missingFrom(picks, recordedReactions)) {
            reactions = reactions.append(new DishReaction(pick,
                    DishReaction.describe(detailsByTitle.get(pick)), true, stamp));
        }
        for (String pick : picks) {
            preferences = preferences.withLike(pick);
        }
        List<TasteEvent> newEvents = new ArrayList<>();
        for (String pick : TasteSurvey.missingFrom(picks, recordedEvents)) {
            newEvents.add(new TasteEvent(TasteEvent.Type.ONBOARDING_PICK, pick,
                    TasteEvent.NO_MEAL, stamp, false));
        }
        // Kolejność: reakcje, zdarzenia (jeden zapis), polubienia na końcu — to polubienia
        // uruchamiają przy starcie migracje do pustych reakcji/dziennika, więc nie mogą
        // trafić na dysk przed nimi. Pierwszy nieudany zapis przerywa resztę.
        boolean durable = picks.isEmpty() || reactionStore.saveDurably(reactions);
        // Zapis dziennika także przy pustej partii: zdarzenia z nieudanej próby są już
        // w pamięci, ale nie na dysku.
        durable = durable && (picks.isEmpty() || appendTasteEvents(newEvents, true));
        durable = durable && (picks.isEmpty() || preferenceStore.saveDurably(preferences));
        if (!durable) {
            // Znacznik zostaje: start aplikacji (albo następna próba) dokończy zapis.
            return false;
        }
        saveSurvey(survey.commit());
        return !picks.isEmpty();
    }

    /** Opens the survey on its own (start-screen card or "Więcej…"), resuming if unfinished. */
    private void openSurvey(int newLength) {
        if (newLength > 0) {
            appSettings.saveSurveyLength(newLength);
            commitSurveyPicks(); // wybory starej ankiety zostają, zanim zniknie
            if (survey == null || !survey.needsCommitRecovery()) {
                survey = null; // ensureSurvey plans a fresh one
            }
        }
        if (survey != null && survey.isPaused()) {
            saveSurvey(survey.withPaused(false)); // wznowienie kończy pauzę
        }
        surveyOnly = true;
        contentEpoch++;
        currentRecipe = null;
        currentMealIndex = -1;
        highlightSelectedMeal();
        startOnboarding(SURVEY_STEP);
    }

    /** "Więcej…" → "Ankieta gustu": resume, or pick how long the new one is. */
    private void showSurveyMenu() {
        if (isSurveyUnfinished()) {
            openSurvey(0);
            return;
        }
        final int[] lengths = TasteSurvey.LENGTH_CHOICES;
        String[] labels = new String[lengths.length];
        String[] names = {"Krótka", "Średnia", "Długa", "Bardzo długa"};
        for (int i = 0; i < lengths.length; i++) {
            labels[i] = (i < names.length ? names[i] + " — " : "") + lengths[i] + " porównań";
        }
        new AlertDialog.Builder(this)
                .setTitle("Ankieta gustu: ile porównań?")
                .setItems(labels, (dialog, which) -> openSurvey(lengths[which]))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    // ----- Meal selection + proposals -------------------------------------

    private void selectMeal(int index) {
        currentMealIndex = index;
        seriesShown.clear(); // a tapped meal starts a new series
        highlightSelectedMeal();
        generateProposals();
    }

    private void highlightSelectedMeal() {
        for (int i = 0; i < mealButtons.length; i++) {
            styleMealTile(i, i == currentMealIndex);
        }
    }

    /**
     * Offer a fresh set of proposals for the current meal, at once: signed in,
     * from the ready pool rated by the LLM earlier in the background (shortfall
     * filled offline, refill kicked off); otherwise the offline pool, never
     * pretending to be AI. No dish repeats within a series.
     */
    private void generateProposals() {
        if (currentMealIndex < 0) {
            return;
        }
        contentEpoch++;
        currentScreen = BackNavigation.Screen.PROPOSALS;
        refreshCatalog(isAiAvailable()
                ? this::generateReadyProposals : this::generateOfflineProposals);
    }

    private void generateOfflineProposals() {
        // Shared offline pipeline: pool -> shuffle -> at most one taste-led pick,
        // the rest kept varied (so liking three chicken dishes does not turn every
        // suggestion into chicken).
        List<Recipe> chosen = generateOfflineRecipes(PROPOSAL_COUNT);
        if (chosen.isEmpty() && seriesShown.isEmpty()) {
            showHint(NO_DISHES_FOR_DIET);
            return;
        }
        proposalSource = chosen.isEmpty()
                ? ReadyProposals.Source.EXHAUSTED : ReadyProposals.Source.READY;
        List<DishProposal> newProposals = new ArrayList<>();
        List<Recipe> newRecipes = new ArrayList<>();
        for (Recipe recipe : chosen) {
            newProposals.add(proposalFromRecipe(recipe));
            newRecipes.add(recipe);
        }
        showProposals(newProposals, newRecipes, null);
    }

    /** The shared offline pipeline for the current meal, diet-filtered, minus this series. */
    private List<Recipe> generateOfflineRecipes(int count) {
        return offlineProposalGenerator.generate(
                backendStore.forMeal(currentMealIndex), cookbook, preferences,
                buildTasteProfile(), count, random, history,
                System.currentTimeMillis(), householdProfile.getDiet(), seriesShown);
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
     * Bez czekania: dania z gotowej puli (ocenione wcześniej w tle przez LLM),
     * braki z puli offline. Pusta pula to jawny stan na ekranie, nie spinner;
     * uzupełnianie rusza w tle.
     */
    private void generateReadyProposals() {
        final int meal = currentMealIndex;
        ReadyProposals.Selection selection = ReadyProposals.take(
                backendStore.readyPool(meal), readyPoolSignature(), currentMealPool(meal),
                householdProfile.getDiet(), reactions, seriesShown,
                generateOfflineRecipes(PROPOSAL_COUNT * 2), PROPOSAL_COUNT);
        backendStore.saveReadyPool(meal, selection.getRemaining());
        if (selection.size() == 0 && seriesShown.isEmpty()) {
            showHint(NO_DISHES_FOR_DIET);
            return;
        }
        proposalSource = selection.getSource();
        List<DishProposal> newProposals = new ArrayList<>();
        for (int i = 0; i < selection.size(); i++) {
            Recipe recipe = selection.getRecipes().get(i);
            // New AI dishes have no recipe yet: "Pokaż przepis" fetches it on demand.
            newProposals.add(recipe != null ? proposalFromRecipe(recipe)
                    : selection.getGenerated().get(i));
        }
        showProposals(newProposals, new ArrayList<>(selection.getRecipes()),
                new ArrayList<>(selection.getReasons()));
        refillReadyPool(meal);
    }

    private List<Recipe> currentMealPool(int meal) {
        return mealPoolBuilder.build(backendStore.forMeal(meal), cookbook, preferences,
                householdProfile.getDiet());
    }

    private String readyPoolSignature() {
        return ReadyDishPool.signature(reactions, householdProfile.getDiet(),
                chatGptAccount.modelChoice().name());
    }

    /** Fills every meal's ready pool in the background, so the first tap is instant too. */
    private void warmReadyPools() {
        for (int meal = 0; meal < MEAL_TYPES.length; meal++) {
            refillReadyPool(meal);
        }
    }

    /**
     * Uzupełnia gotową pulę posiłku w tle: LLM generuje nowe dania (i ocenia
     * nieocenione z katalogu). Rusza, gdy pula jest pusta, nieaktualna albo
     * spada poniżej dwóch zestawów — zanim się wyczerpie. Najwyżej jedno
     * uzupełnienie naraz na posiłek. Błąd zostawia pulę bez zmian; kolejna
     * propozycja spróbuje ponownie.
     */
    private void refillReadyPool(final int meal) {
        if (!isAiAvailable() || refillingMeals.contains(meal)) {
            return;
        }
        final Set<String> shown = meal == currentMealIndex
                ? new HashSet<>(seriesShown) : new HashSet<String>();
        final String signature = readyPoolSignature();
        final ReadyDishPool pool = backendStore.readyPool(meal);
        if (!ReadyProposals.needsRefill(pool, signature, shown, PROPOSAL_COUNT)) {
            return;
        }
        final DishReactionLog reactionSnapshot = reactions;
        final DietConstraints diet = householdProfile.getDiet();
        final List<Recipe> mealPool = currentMealPool(meal);
        final MealHistory historySnapshot = history;
        final long now = System.currentTimeMillis();
        final RecipeRequest request = buildRequest(meal,
                ReadyProposals.avoidList(pool, shown, history.recentTitles(8)));
        refillingMeals.add(meal);
        readyPoolExecutor().execute(() -> {
            ReadyProposals.Refill refill = null;
            try {
                refill = ReadyProposals.refill(dishRater, recipeService, request, signature,
                        pool, reactionSnapshot, mealPool, shown, diet, historySnapshot, now,
                        random);
            } catch (IOException | RuntimeException ignored) {
                // Offline proposals keep working; the next proposal retries.
            }
            final ReadyProposals.Refill result = refill;
            runOnUiThread(() -> {
                refillingMeals.remove(meal);
                if (result != null && !result.isEmpty()) {
                    backendStore.saveReadyPool(meal, result.applyTo(backendStore.readyPool(meal),
                            meal == currentMealIndex ? seriesShown
                                    : java.util.Collections.<String>emptySet()));
                }
            });
        });
    }

    private Executor readyPoolExecutor() {
        if (readyPoolExecutorOverride != null) {
            return readyPoolExecutorOverride;
        }
        if (readyPoolExecutor == null) {
            readyPoolExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        }
        return readyPoolExecutor;
    }

    /** Aktualny model gustu (pochodna dziennika + agregatu, tanio liczona). */
    private TasteModel currentTasteModel(java.util.Map<String, String> detailsByTitle) {
        return TasteModel.build(tasteEvents, tasteAggregate, dishTagger,
                detailsByTitle, System.currentTimeMillis());
    }

    private RecipeRequest buildRequest() {
        return buildRequest(currentMealIndex, history.recentTitles(8));
    }

    /** Request for one meal; {@code recentToAvoid} lists names the AI should not repeat. */
    private RecipeRequest buildRequest(int meal, List<String> recentToAvoid) {
        final String mealType = MEAL_TYPES[meal];
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
        // Co użytkownik napisał, że już zjadł — AI ma tego nie powtarzać.
        List<String> avoid = new ArrayList<>(recentToAvoid);
        avoid.addAll(foodLogStore.load().promptAvoidItems(System.currentTimeMillis()));
        return new RecipeRequest(mealType, preferences, avoid,
                fragments, knownDishes, buildTasteProfile().getAffinities(),
                householdProfile)
                .withTasteContext(tasteContextBuilder.build(
                        currentTasteModel(detailsByTitle), tasteEvents,
                        householdProfile.getDiet(), meal)
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
            seriesShown.add(proposal.getName().trim().toLowerCase());
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

    /** Jawny stan gotowej puli nad kartami; "" gdy wszystko z puli (albo zwykły offline). */
    static String proposalSourceNote(ReadyProposals.Source source) {
        switch (source) {
            case PARTIAL:
                return "Część propozycji z puli offline — gotowe dania AI dobierają się w tle.";
            case OFFLINE:
                return "Pula gotowych dań AI jeszcze się przygotowuje — na razie propozycje offline.";
            case EXHAUSTED:
                return "To już wszystkie pasujące dania w tej serii — bez powtórek.";
            default:
                return "";
        }
    }

    private void renderProposals() {
        currentScreen = BackNavigation.Screen.PROPOSALS;
        contentContainer.removeAllViews();
        if (currentMealIndex >= 0) {
            contentContainer.addView(Ui.overline(this,
                    "Pomysły na " + MEAL_ACCUSATIVE[currentMealIndex]), matchWrap());
        }
        String note = proposalSourceNote(proposalSource);
        if (!note.isEmpty()) {
            TextView status = Ui.callout(this, note, Ui.SURFACE_MUTED, Ui.INK_BODY);
            status.setId(R.id.ready_pool_status);
            contentContainer.addView(status, marginTop(10));
        }
        for (int i = 0; i < proposals.size(); i++) {
            contentContainer.addView(buildProposalCard(i), marginTop(i == 0 ? 10 : 14));
        }

        Button refresh = new Button(this);
        refresh.setId(R.id.refresh_button);
        boolean exhausted = proposalSource == ReadyProposals.Source.EXHAUSTED;
        // Koniec serii: „od nowa" otwiera nową serię zamiast pustego odświeżenia.
        refresh.setText(exhausted ? "Zacznij od nowa" : "Inne propozycje");
        refresh.setTextSize(16);
        Ui.tonal(refresh);
        refresh.setCompoundDrawablesWithIntrinsicBounds(
                tinted(R.drawable.ic_refresh, Ui.ACCENT_DEEP), null, null, null);
        refresh.setCompoundDrawablePadding(dp(8));
        refresh.setOnClickListener(v -> {
            if (exhausted) {
                selectMeal(currentMealIndex);
                return;
            }
            recordTrioRerolled();
            generateProposals();
        });
        LinearLayout.LayoutParams refreshParams = Ui.wrap();
        refreshParams.topMargin = dp(18);
        refreshParams.gravity = Gravity.CENTER_HORIZONTAL;
        contentContainer.addView(refresh, refreshParams);
    }

    private View buildProposalCard(int index) {
        DishProposal proposal = proposals.get(index);

        LinearLayout card = Ui.card(this);

        LinearLayout titleRow = Ui.row(this);
        titleRow.setGravity(Gravity.TOP);
        card.addView(titleRow, matchWrap());

        TextView badge = Ui.text(this, String.valueOf(index + 1), 14, Ui.ACCENT_DEEP);
        badge.setTypeface(Ui.MEDIUM);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.circle(Ui.ACCENT_SOFT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        badgeParams.rightMargin = dp(12);
        badgeParams.topMargin = dp(1);
        titleRow.addView(badge, badgeParams);

        TextView name = Ui.headline(this, proposal.getName(), 20);
        if (index == 0) {
            name.setId(R.id.recipe_title); // first card title is the testable anchor
        }
        titleRow.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String summary = proposal.summary();
        if (!summary.isEmpty()) {
            TextView body = Ui.text(this, summary, 15, Ui.INK_BODY);
            body.setLineSpacing(dp(2), 1.0f);
            card.addView(body, marginTop(8));
        }

        String reason = index < proposalReasons.size() ? proposalReasons.get(index) : "";
        if (!reason.isEmpty()) {
            SpannableStringBuilder why = new SpannableStringBuilder("Dlaczego: ");
            why.setSpan(new StyleSpan(Typeface.BOLD), 0, why.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            why.append(reason);
            card.addView(Ui.callout(this, why, Ui.HERB_SOFT, Ui.HERB), marginTop(12));
        }

        Button show = new Button(this);
        if (index == 0) {
            show.setId(R.id.accept_button);
        }
        show.setText("Pokaż przepis");
        show.setTextSize(15);
        Ui.primary(show);
        show.setOnClickListener(v -> openRecipe(index));
        card.addView(show, marginTop(14));

        LinearLayout reactions = Ui.row(this);
        LinearLayout.LayoutParams reactionsParams = marginTop(8);
        reactionsParams.leftMargin = -dp(4);
        reactionsParams.rightMargin = -dp(4);
        card.addView(reactions, reactionsParams);

        final Button like = new Button(this);
        if (index == 0) {
            like.setId(R.id.like_button);
        }
        like.setText("Lubię to");
        like.setTextSize(15);
        reactions.addView(like, Ui.weighted(this, 8));

        final Button dislike = new Button(this);
        if (index == 0) {
            dislike.setId(R.id.dislike_button);
        }
        dislike.setText("Nie lubię");
        dislike.setTextSize(15);
        reactions.addView(dislike, Ui.weighted(this, 8));

        styleReactionPair(like, dislike, null);
        like.setOnClickListener(v -> {
            reactToProposal(index, true);
            styleReactionPair(like, dislike, true);
        });
        dislike.setOnClickListener(v -> {
            reactToProposal(index, false);
            styleReactionPair(like, dislike, false);
        });

        return card;
    }

    /**
     * Like/dislike pair; once tapped, the given reaction stays marked (herb
     * green tick for "lubię", muted fill for "nie lubię") so the tap is visible.
     */
    private void styleReactionPair(Button like, Button dislike, Boolean liked) {
        if (Boolean.TRUE.equals(liked)) {
            Ui.confirmed(like);
        } else {
            Ui.outlinedButton(like);
        }
        like.setText(Boolean.TRUE.equals(liked) ? "Lubię to ✓" : "Lubię to");
        Ui.outlinedButton(dislike);
        if (Boolean.FALSE.equals(liked)) {
            dislike.setBackground(Ui.rounded(this, Ui.SURFACE_MUTED, Ui.PILL_CORNER_DP));
            dislike.setTextColor(Ui.INK);
        }
        like.setGravity(Gravity.CENTER);
        dislike.setGravity(Gravity.CENTER);
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
        showLoading("Przygotowuję przepis…");
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

        contentContainer.addView(Ui.overline(this, currentMealIndex >= 0
                ? MEAL_TYPES[currentMealIndex] + " · przepis" : "Przepis"), matchWrap());

        TextView name = Ui.headline(this, recipe.getTitle(), 27);
        name.setId(R.id.recipe_title);
        contentContainer.addView(name, marginTop(4));

        LinearLayout card = Ui.card(this);
        TextView details = Ui.body(this, formatRecipe(recipe.getDetails()));
        details.setId(R.id.recipe_details);
        details.setTextSize(16);
        details.setLineSpacing(dp(4), 1.0f);
        details.setTextIsSelectable(true);
        card.addView(details, matchWrap());
        contentContainer.addView(card, marginTop(16));

        LinearLayout actions = Ui.row(this);
        LinearLayout.LayoutParams actionsParams = marginTop(16);
        actionsParams.leftMargin = -dp(4);
        actionsParams.rightMargin = -dp(4);
        contentContainer.addView(actions, actionsParams);

        Button like = new Button(this);
        like.setId(R.id.like_button);
        like.setText("Lubię to");
        like.setTextSize(16);
        Ui.primary(like);
        like.setOnClickListener(v -> {
            rememberReaction(recipe.getTitle(), DishReaction.describe(recipe.getDetails()), true);
            Ui.confirmed(like);
            like.setText("Lubię to ✓");
        });
        actions.addView(like, Ui.weighted(this, 8));

        Button dislike = new Button(this);
        dislike.setId(R.id.dislike_button);
        dislike.setText("Nie lubię");
        dislike.setTextSize(16);
        Ui.outlinedButton(dislike);
        dislike.setOnClickListener(v -> rememberReaction(recipe.getTitle(),
                DishReaction.describe(recipe.getDetails()), false));
        actions.addView(dislike, Ui.weighted(this, 8));

        if (isAiAvailable()) {
            Button change = new Button(this);
            change.setId(R.id.change_button);
            change.setText("Zmień przepis");
            change.setTextSize(16);
            Ui.tonal(change);
            change.setOnClickListener(v -> showModifyRecipeDialog());
            contentContainer.addView(change, marginTop(10));
        }

        // Kitchen helpers, also under "Więcej" — here they are one tap away.
        LinearLayout tools = Ui.row(this);
        LinearLayout.LayoutParams toolsParams = marginTop(10);
        toolsParams.leftMargin = -dp(4);
        toolsParams.rightMargin = -dp(4);
        contentContainer.addView(tools, toolsParams);
        Button shopping = new Button(this);
        shopping.setText("Lista zakupów");
        shopping.setTextSize(15);
        Ui.outlinedButton(shopping);
        shopping.setOnClickListener(v -> showShoppingList());
        tools.addView(shopping, Ui.weighted(this, 8));
        Button save = new Button(this);
        save.setText("Zapisz danie");
        save.setTextSize(15);
        Ui.outlinedButton(save);
        save.setOnClickListener(v -> saveCurrentToCookbook());
        tools.addView(save, Ui.weighted(this, 8));

        // The button mirrors the system back button exactly (one shared path),
        // so a recipe that has no proposals to return to goes to the start screen.
        Button back = new Button(this);
        back.setId(R.id.back_button);
        back.setText(recipeFromProposals && !proposals.isEmpty()
                ? "Wróć do propozycji" : "Wróć");
        back.setTextSize(16);
        Ui.ghost(back);
        back.setOnClickListener(v -> onBackPressed());
        contentContainer.addView(back, marginTop(12));
    }

    /**
     * Recipe text as readable blocks ({@link RecipeLayout}): small caps
     * headings, bulleted ingredients, numbered steps. Falls back to the raw
     * text when nothing could be parsed.
     */
    private CharSequence formatRecipe(String text) {
        List<RecipeLayout.Block> blocks = RecipeLayout.parse(text);
        if (blocks.isEmpty()) {
            return text;
        }
        SpannableStringBuilder out = new SpannableStringBuilder();
        RecipeLayout.Type previous = null;
        for (RecipeLayout.Block block : blocks) {
            if (out.length() > 0) {
                boolean tight = block.getType() == RecipeLayout.Type.ITEM
                        && (previous == RecipeLayout.Type.ITEM
                        || previous == RecipeLayout.Type.HEADING);
                out.append(tight ? "\n" : "\n\n");
            }
            int start = out.length();
            switch (block.getType()) {
                case HEADING:
                    out.append(block.getText().toUpperCase());
                    out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new ForegroundColorSpan(Ui.ACCENT_DEEP), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new RelativeSizeSpan(0.8f), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case ITEM:
                    out.append(block.getText());
                    out.setSpan(new BulletSpan(dp(10), Ui.ACCENT), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STEP:
                    out.append(block.getNumber()).append(".  ");
                    out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new ForegroundColorSpan(Ui.ACCENT), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.append(block.getText());
                    break;
                default:
                    out.append(block.getText());
                    break;
            }
            previous = block.getType();
        }
        return out;
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
        appendTasteEvents(java.util.Collections.singletonList(new TasteEvent(
                type, dish, mealIndex, System.currentTimeMillis(), exploratory)), false);
    }

    /**
     * Appends a batch of events, compacts once and persists log (+ aggregate) in one
     * write. Routine events save asynchronously; {@code durable} (survey commit) waits
     * for the disk and returns whether the write succeeded.
     */
    private boolean appendTasteEvents(List<TasteEvent> events, boolean durable) {
        boolean compactedNow = false;
        for (TasteEvent event : events) {
            tasteEvents = tasteEvents.append(event);
        }
        if (tasteEvents.size() > TasteEventLog.MAX_EVENTS) {
            // Najstarsze zdarzenia zwijają się do zamrożonego agregatu —
            // model liczony dalej wychodzi ten sam, dziennik nie puchnie.
            TasteEventCompactor.Result compacted = tasteEventCompactor.compact(
                    tasteEvents, tasteAggregate, dishTagger,
                    BuiltInRecipes.detailsByTitle(cookbook),
                    System.currentTimeMillis(), TasteEventLog.MAX_EVENTS);
            tasteEvents = compacted.getLog();
            tasteAggregate = compacted.getAggregate();
            compactedNow = true;
        }
        if (compactedNow) {
            if (durable) {
                return tasteEventStore.saveDurably(tasteEvents, tasteAggregate);
            }
            tasteEventStore.save(tasteEvents, tasteAggregate);
            return true;
        }
        if (durable) {
            return tasteEventStore.saveDurably(tasteEvents);
        }
        tasteEventStore.save(tasteEvents);
        return true;
    }

    // ----- Modify the shown recipe ----------------------------------------

    private void showModifyRecipeDialog() {
        if (currentRecipe == null || currentRecipe.getTitle().trim().isEmpty()) {
            return;
        }
        if (!isAiAvailable()) {
            Toast.makeText(this, "Zmiana przepisu wymaga konta ChatGPT i serwera Mealspire.",
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
        showLoading("Zmieniam przepis…");
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
        labels.add(isSurveyUnfinished() ? "Dokończ ankietę gustu" : "Ankieta gustu (A czy B?)");
        actions.add(this::showSurveyMenu);
        labels.add(FoodLogReminder.TITLE);
        actions.add(this::showFoodLogDialog);
        labels.add(FOOD_LOG_HOUR_MENU_PREFIX
                + FoodLogReminder.hourLabel(appSettings.loadFoodLogHour()));
        actions.add(this::showFoodLogHourDialog);
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
            actions.add(this::offerChatGptSignIn);
        }

        new AlertDialog.Builder(this)
                .setTitle("Więcej")
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    // ----- "Co dzisiaj jedliście?" (evening question + food log) ----------

    /**
     * The answer dialog — from the evening notification (Android 6, or a tap
     * instead of a direct reply) or from "Więcej…". Shows the last few answers
     * so the user sees what the app already knows.
     */
    void showFoodLogDialog() {
        final EditText input = new EditText(this);
        input.setId(R.id.food_log_input);
        input.setHint(FoodLogReminder.REPLY_HINT);
        input.setMinLines(2);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(FoodLog.MAX_TEXT_LENGTH)});
        List<String> recent = foodLogStore.load().recentTexts(System.currentTimeMillis(), 3);
        StringBuilder message = new StringBuilder(FoodLogReminder.TEXT);
        if (!recent.isEmpty()) {
            message.append("\n\nOstatnio zapisane:");
            for (String text : recent.subList(0, Math.min(3, recent.size()))) {
                message.append("\n• ").append(text);
            }
        }
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(4), dp(20), 0);
        box.addView(input, matchWrap());
        new AlertDialog.Builder(this)
                .setTitle(FoodLogReminder.TITLE)
                .setMessage(message.toString())
                .setView(box)
                .setPositiveButton("Zapisz", (dialog, which) -> {
                    String text = input.getText().toString();
                    if (text.trim().isEmpty()) {
                        return;
                    }
                    foodLogStore.save(foodLogStore.load().append(text,
                            System.currentTimeMillis()));
                    FoodLogNotifications.cancel(this);
                    toast(FoodLogReminder.SAVED_TEXT);
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    /** "Więcej…" → when the evening question comes, or switch it off. */
    private void showFoodLogHourDialog() {
        final int[] hours = new int[FoodLogReminder.HOUR_CHOICES.length + 1];
        final String[] labels = new String[hours.length];
        for (int i = 0; i < FoodLogReminder.HOUR_CHOICES.length; i++) {
            hours[i] = FoodLogReminder.HOUR_CHOICES[i];
            labels[i] = FoodLogReminder.hourLabel(hours[i])
                    + (hours[i] == FoodLogReminder.DEFAULT_HOUR ? " (domyślnie)" : "");
        }
        hours[hours.length - 1] = FoodLogReminder.DISABLED;
        labels[hours.length - 1] = FoodLogReminder.hourLabel(FoodLogReminder.DISABLED);
        int current = appSettings.loadFoodLogHour();
        int checked = -1;
        for (int i = 0; i < hours.length; i++) {
            if (hours[i] == current) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Kiedy pytać, co dziś jedliście?")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    appSettings.saveFoodLogHour(hours[which]);
                    new MealReminderScheduler().scheduleAll(this);
                    dialog.dismiss();
                    toast(hours[which] == FoodLogReminder.DISABLED
                            ? "Wieczorne pytanie wyłączone."
                            : "Zapytam codziennie o " + FoodLogReminder.hourLabel(hours[which])
                                    + ".");
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    /** The server is contacted only after ChatGPT sign-in (consent covers the server). */
    private void refreshCatalog(Runnable after) {
        if (!isAiAvailable() || !backendStore.needsCatalog()) { if (after != null) after.run(); return; }
        final String endpoint = backendStore.baseUrl();
        final int epoch = contentEpoch;
        new Thread(() -> {
            try { backendStore.cache(endpoint, backendClient.catalog()); }
            catch (IOException ignored) { /* Last catalog or bootstrap remains available. */ }
            runOnUiThread(() -> { if (epoch == contentEpoch && after != null) after.run(); });
        }).start();
    }

    // ----- Household profile (answers from the onboarding quiz) ------------

    /** Lets the user change the quiz answers later, one simple dialog each. */
    private void showHouseholdProfileDialog() {
        final String[] items = {Q_AUDIENCE, Q_DIET,
                "Ile masz czasu na gotowanie?", Q_SKILL,
                "Ulubione kuchnie", Q_SPICE, Q_STYLE, Q_BUDGET, Q_NOVELTY};
        final Runnable[] actions = {this::showAudienceDialog, this::showDietDialog,
                this::showTimeDialog, this::showSkillDialog, this::showCuisinesDialog,
                () -> showProfileChoiceDialog(Q_SPICE, SPICE_LABELS, SPICE_VALUES,
                        householdProfile.getSpice(), HouseholdProfile::withSpice),
                () -> showProfileChoiceDialog(Q_STYLE, STYLE_LABELS, STYLE_VALUES,
                        householdProfile.getStyle(), HouseholdProfile::withStyle),
                () -> showProfileChoiceDialog(Q_BUDGET, BUDGET_LABELS, BUDGET_VALUES,
                        householdProfile.getBudget(), HouseholdProfile::withBudget),
                () -> showProfileChoiceDialog(Q_NOVELTY, NOVELTY_LABELS, NOVELTY_VALUES,
                        householdProfile.getNovelty(), HouseholdProfile::withNovelty)};
        new AlertDialog.Builder(this)
                .setTitle("Profil domowników")
                .setItems(items, (dialog, which) -> actions[which].run())
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
            Toast.makeText(this, "Dodawanie z linku/opisu wymaga konta ChatGPT i serwera Mealspire.",
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
        showLoading("Dodaję do bazy…");
        new Thread(() -> {
            try {
                final CookbookEntry entry = dishImporter.importDish(input);
                runOnUiThread(() -> {
                    // Always persist the imported dish, even if the view moved on.
                    cookbook = cookbook.add(entry);
                    cookbookStore.save(cookbook);
                    preferences = preferences.withLike(entry.getTitle());
                    preferenceStore.save(preferences);
                    // Dodanie znanego i lubianego dania to jawne „lubię" dla oceny LLM.
                    reactions = reactions.append(new DishReaction(entry.getTitle(),
                            DishReaction.describe(entry.getRecipe()), true,
                            System.currentTimeMillis()));
                    reactionStore.save(reactions);
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

    /** A single informational message in the content area (empty diet pool, errors). */
    private void showHint(String text) {
        contentContainer.removeAllViews();
        TextView hint = Ui.callout(this, text, Ui.SURFACE_MUTED, Ui.INK_BODY);
        hint.setTextSize(16);
        hint.setPadding(dp(18), dp(16), dp(18), dp(16));
        contentContainer.addView(hint, matchWrap());
    }

    /** Waiting for the server/AI: a spinner card, so the wait reads as work. */
    private void showLoading(String text) {
        contentContainer.removeAllViews();
        LinearLayout card = Ui.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(Ui.ACCENT));
        card.addView(spinner, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView label = Ui.text(this, text, 16, Ui.INK_BODY);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = dp(14);
        card.addView(label, labelParams);
        contentContainer.addView(card, matchWrap());
    }

    private void setMealButtonsEnabled(boolean enabled) {
        for (Button button : mealButtons) {
            Ui.setEnabledWithFade(button, enabled);
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

    private LinearLayout.LayoutParams matchWrap() {
        return Ui.matchWrap();
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        return Ui.marginTop(this, topDp);
    }

    private int dp(int value) {
        return Ui.dp(this, value);
    }
}
