# Steering: projekt Mealspire

Najważniejsze zasady i decyzje projektowe. Przeczytaj przed zmianami w kodzie.

## Czym jest aplikacja

Natywna aplikacja Android (Java, bez Kotlina i bez bibliotek third-party w
kodzie produkcyjnym), która po jednym dotknięciu (Śniadanie / Obiad / Kolacja)
podsuwa 3 proste propozycje dań. Pełny przepis powstaje dopiero na żądanie.
Wszystkie teksty w UI i promptach są po polsku.

## Architektura — zasady, których pilnujemy

- `MainActivity` jest **cienka**: składa UI programowo (bez XML layoutów),
  reaguje na zdarzenia i deleguje do klas domenowych.
- Logika mieszka w `app/src/main/java/com/mealspire/app/domain/` — czyste,
  małe klasy testowalne JVM-owo (bez zależności od Androida).
- Warstwy Androidowe: `storage/` (SharedPreferences-owe implementacje
  interfejsów domenowych), `net/` (HttpURLConnection), `notify/`
  (AlarmManager + powiadomienia).
- **TDD**: najpierw test, potem implementacja. Testy JVM + Robolectric,
  bez emulatora. Testy Robolectric pokrywają wyłącznie ścieżkę offline —
  AI nigdy nie jest wołane w testach.
- minSdk 23, targetSdk/compileSdk 35, Java 17. Nie używaj API >23 bez guardów
  (`Build.VERSION.SDK_INT`), nie dodawaj ciężkich bibliotek.

## Kluczowe decyzje produktowe

- **Onboarding tylko raz**: świeża instalacja zaczyna od quizu (dla kogo
  gotujesz / czego nie jadacie / ile czasu na gotowanie / jak idzie gotowanie
  + 3 **kontrastowe** rundy wyboru dań z `ContrastiveDishSampler` — każda
  rozstrzyga jeden wymiar gustu; opcja „Żadne z tych" nie zapisuje nic).
  Odpowiedzi 1–4 → `HouseholdProfile` (wpływa na prompty przez
  `RecipeRequest.getHouseholdProfile()`, także na „Zmień przepis"), wybory dań
  → zwykłe polubienia i reakcje „lubię" na liście reakcji. Rundy losowane są **po** pytaniu o dietę i ją respektują;
  zmiana diety przez „Cofnij" przelosowuje rundy (`ensureOnboardingRounds`).
  Wybory rund trzymane są w `onboardingPicks` i zapisywane dopiero na końcu
  quizu, żeby „Cofnij" + inny wybór **podmieniał** polubienie, a nie dokładał
  kolejne. Flaga ukończenia w `AppSettings.isOnboardingDone()` jest niezależna
  od profilu; „Pomiń" też ją ustawia, a udzielone odpowiedzi zostają.
  Jednorazowe dialogi startowe (liczba osób, logowanie kontem ChatGPT, uprawnienie
  do powiadomień) przechodzą przez `showStartupPrompts()` i czekają do końca
  quizu. Zmiana odpowiedzi później: „Więcej…" → „Profil domowników" (etykiety
  odpowiedzi współdzielone z quizem przez stałe `*_LABELS`/`*_VALUES`;
  ulubione kuchnie zostały tylko w tym dialogu, wypadły z quizu).

- **Wykluczenia diety to wymóg, nie preferencja** (`DietConstraints`
  w `HouseholdProfile`): bezwzględne zdanie w każdym prompcie, twardy filtr
  puli offline (`MealPoolBuilder`, bez fallbacku!) i walidacja odpowiedzi AI
  (`ProposalValidator` — łamiące dietę propozycje zastępuje pula offline).
  Nie podlegają wygaszaniu; maskują też wyuczony profil w `TasteContextBuilder`.

- **Decyzja „co pokazać" = jedna ocena LLM** (`DishRecommender`). Jedyne
  wejście uczenia to `DishReactionLog`: jawne reakcje „Lubię to" / „Nie lubię"
  (nazwa, krótki skład, lubię/nie lubię, czas; wybory z quizu jako „lubię";
  polubienia sprzed listy przenoszone raz w `onCreate`). „Pokaż przepis" i
  „Inne propozycje" **nie są** oceną. „Nie lubię" nie jest zakazem — nie
  trafia do `UserPreferences.dislikes`. Po zalogowaniu: kandydaci = pula
  (`MealPoolBuilder`, już po diecie) → shuffle → `RecentlyShownFilter` → max
  `MAX_CANDIDATES`; **jedno** wywołanie `DishRatingPromptBuilder` (max
  `MAX_REACTIONS` najnowszych reakcji, po jednej na danie, z wiekiem) → JSON
  `{"oceny":[{danie, ocena 0–10, powod}]}` (`DishRatingParser`) → odrzucenie
  nazw spoza kandydatów i naruszeń diety → 3 najwyższe z powodem na karcie,
  braki dopełnia pula offline. Pusta lista reakcji = ocena po popularności.
  Bez logowania LLM nie jest wołany; zły JSON / sieć → offline + toast, bez
  crasha. Brak progu „5 polubień" i reguły 2+1 — `TasteModel`/`TasteProfiler`
  nie wybierają kart zalogowanego użytkownika (zostają: `TasteProfiler` w
  offline'owym `VariedMealPicker`, dziennik `TasteEvent` w kontekście promptu
  pełnego przepisu/„Zmień przepis"). „Wyczyść reakcje" czyści listę
  (`DataManager.clearPreferences`). Zero telemetrii.
- **Pełny przepis**: kandydaci pochodzą z katalogu, więc przepis po wybraniu
  karty jest od razu (bez drugiego wywołania AI). „Zmień przepis" i import
  dania działają od razu po zalogowaniu kontem ChatGPT.
- **Wspólny pipeline offline** (`OfflineProposalGenerator`): pula → shuffle →
  filtr „ostatnio pokazane" (3 dni) → `VariedMealPicker` (max 1 wybór wg gustu,
  reszta różnorodna). Używany przez ekran i przez powiadomienia — nie rozjeżdżać.
- **Powiadomienia 8/12/18** liczone offline (bez sieci i logowania), AlarmManager
  `setInexactRepeating` (bez uprawnienia exact-alarm), przeżywają reboot przez
  `BootReceiver`. W Doze mogą być opóźnione — świadomy kompromis.
- **Podpis APK**: wspólny keystore `signing/mealspire.keystore` (hasło
  `mealspire`, **celowo jawne** — repo publiczne, appka poza Play Store;
  kompromis opisany w `docs/design/aktualizacja-aplikacji.md`). Dzięki temu
  każdy build instaluje się po wierzchu poprzedniego bez utraty danych.
  Nie generować nowego keystore — to zerwałoby ciągłość aktualizacji.
- **AI = konto ChatGPT użytkownika, zero sekretów w buildzie**. Żadnego
  klucza API w repo, w `BuildConfig` ani w zasobach (pilnuje
  `NoBuiltInApiKeyTest`); żadnej ścieżki „wklej swój klucz” ani Claude API.
  Logowanie: Sign in with ChatGPT dla open source (`ChatGptOAuth`: PKCE,
  `client_id=dynamic_agent_client` → wydany `oaiapp_…`, redirect tylko loopback
  `http://127.0.0.1:PORT/callback` → `LoopbackCallbackServer`). ID token
  weryfikowany RS256 z JWKS (`IdTokenVerifier`). Sesja w
  `SharedPreferencesChatGptSessionStore` (wyłączona z backupu, `res/xml/`),
  odświeżanie i rotacja refresh tokenu w `ChatGptAccount`.
- **Inferencja**: `ChatGptLlmClient` → `POST /v1/responses` z dozwolonymi
  wyłącznie `model`, `input`, `store:false`, `stream:true` (plan usage nie
  pozwala na `temperature`/`max_output_tokens`); prompt systemowy idzie jako
  wiadomość `developer`. Model: `GptModel` LUNA (domyślny) / SOL, wybór
  użytkownika w `ChatGptSessionStore.modelChoice()` („Więcej…” → „Model AI”).
  Slugi rozwiązywane przy logowaniu z `GET /v1/models` (`visibility:"list"`):
  najpierw udokumentowane ID (`gpt-6-luna`; `gpt-6.1-sol`, `gpt-6-sol`), potem
  najnowszy `gpt-N[.M]-luna|sol` z katalogu. Brak modelu = komunikat, **nigdy
  podmiana na inny model**. Wymaga planu Plus/Pro.
  Docs: https://developers.openai.com/siwc
- **Aktualizacje**: raz na dobę `UpdateChecker` pobiera `dist/wersja.json`
  z raw.githubusercontent (bez tokenu) i porównuje z `BuildConfig.VERSION_CODE`;
  wyższy → baner na ekranie startowym otwierający pobieranie w aplikacji
  (`update/UpdateActivity`). Instalacja wymaga zgody na nieznane źródła i
  potwierdzenia w PackageInstaller; pobranie jest weryfikowane SHA-256. Decyzje w `domain/`
  (`UpdateChecker`, `VersionInfoParser`), stan w `SharedPreferencesUpdateStateStore`,
  transport w `net/HttpVersionJsonFetcher`. **Nigdy nie woła sieci w testach**:
  pierwszy odczyt znacznika czasu zasiewa „teraz" (świeża instalacja/test czeka
  dobę), a testy wstrzykują `versionJsonSourceOverride`/`updateCheckExecutorOverride`.
  Przy wydaniu podbijaj `versionCode`/`versionName` i `dist/wersja.json` razem —
  pilnuje `ReleaseConsistencyTest`. Pełny design: `docs/design/aktualizacja-aplikacji.md`.

## Wygląd UI

- Cały wygląd (paleta, typografia, przyciski, karty, chipy) jest w
  `ui/Ui.java`; `MainActivity` i `UpdateActivity` tylko składają widoki.
  Paleta ma odpowiednik w `res/values/colors.xml` (motyw, dialogi, ikona) —
  zmieniaj oba miejsca razem. Pary tekst/tło trzymają kontrast WCAG AA.
- Dialogi (`AlertDialog`) dostają wygląd z `AppTheme.Dialog` w `styles.xml`.
- Tekst przepisu dzieli na bloki czysta klasa `domain/RecipeLayout`
  (nagłówki, składniki, kroki); powitanie wg godziny — `domain/DayGreeting`.
- Ikona: adaptacyjna (`ic_launcher_background` + `ic_launcher_foreground`,
  wszystko w strefie bezpiecznej r=33 w siatce 108) i osobna
  `ic_launcher_monochrome` dla ikon tematycznych Androida 13+. Ikona
  powiadomienia (`ic_notification`) to ta sama sylwetka, biała.
- Podgląd ekranów bez emulatora: `UiScreenshotTest` (opt-in, patrz
  `budowanie.md`).

## Pułapki w UI (naprawione — nie regresować)

- `MainActivity.contentEpoch`: każda asynchroniczna odpowiedź (propozycje AI,
  pobranie przepisu, zmiana przepisu, import dania) sprawdza epokę i nie
  nadpisuje treści, jeśli użytkownik w międzyczasie zmienił widok.
- Błąd pobierania przepisu przywraca listę propozycji (toast z błędem) —
  nie zostawiamy użytkownika na samym komunikacie bez przycisków.
- Pobrany przepis jest cache'owany w `proposalRecipes` — powrót i ponowne
  „Pokaż przepis" nie woła API drugi raz.
- Manifest deklaruje `android:configChanges` dla orientacji — UI trzyma stan
  w Activity, więc obrót nie może jej odtwarzać (pilnuje `RotationSafetyTest`).
- Intent z powiadomienia konsumuje `EXTRA_MEAL_INDEX` (`removeExtra`), żeby
  ponowne dostarczenie intentu nie wymuszało wyboru posiłku. Przy nieukończonym
  onboardingu posiłek czeka w `pendingMealIndex` i otwiera się po quizie.
- Systemowe „Cofnij": `MainActivity.onBackPressed()` + czysta decyzja w
  `BackNavigation` (przepis → propozycje → start → wyjście; w quizie poprzednie
  pytanie, z pierwszego jak „Pomiń"). Przepis spoza propozycji (import przez
  „Dodaj danie") wraca na ekran startowy — pilnuje tego flaga
  `recipeFromProposals`. Przycisk „Wróć…" pod przepisem woła to samo
  `onBackPressed()`. Aktywność quizu ma jedno źródło prawdy
  (`onboardingStep >= 0`) — ekran `ONBOARDING` jest wyliczany, nie zapisywany.
  Każde cofnięcie podbija `contentEpoch`. **Nie włączać**
  `android:enableOnBackInvokedCallback` w manifeście — wtedy `onBackPressed()`
  przestaje być wołane.

## Backend VPS

Online MainActivity używa `BackendClient` (operacje `/v1`), a `RecipeService`
i prompty wykonuje moduł JVM `backend`. Android nie ma nowych zależności.
Samodzielny JVM korzysta z org.json jako odpowiednika platformowej biblioteki
Android. Offline, quiz, magazyny i aktualizator pozostają lokalne.
Kontrakt: `docs/design/backend-api.md`; staging: `deploy/BACKEND-RUNBOOK.md`.
Adres serwera jest stały: `BackendClient.DEFAULT_BASE_URL`
(`https://sochiera.pl/mealspire-api`), bez pola w UI. `SharedPreferencesBackendStore`
przy starcie zastępuje pusty/inny adres zapisany przez starsze wersje i czyści
jego katalog. Serwer jest wołany dopiero po zalogowaniu kontem ChatGPT.
Informację o serwerze daje dialog logowania (także z menu „Więcej…”); sesja
z wcześniejszej wersji dostaje raz dialog `SERVER_NOTICE_TITLE` (OK / wyloguj).
