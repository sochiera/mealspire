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
  gotujesz / jak idzie gotowanie / ulubione kuchnie + 3 rundy wyboru dań).
  Odpowiedzi 1–3 → `HouseholdProfile` (wpływa na prompty przez
  `RecipeRequest.getHouseholdProfile()`, także na „Zmień przepis"), wybory dań
  → zwykłe polubienia (celowo 3, nie 5 — AI nie ma przejmować propozycji po
  samym quizie). Wybory rund trzymane są w `onboardingPicks` i zapisywane
  dopiero na końcu quizu, żeby „Cofnij" + inny wybór **podmieniał** polubienie,
  a nie dokładał kolejne. Flaga ukończenia w `AppSettings.isOnboardingDone()`
  jest niezależna od profilu; „Pomiń" też ją ustawia, a udzielone odpowiedzi
  zostają. Jednorazowe dialogi startowe (liczba osób, hasło do klucza API,
  uprawnienie do powiadomień) przechodzą przez `showStartupPrompts()` i czekają
  do końca quizu. Zmiana odpowiedzi później: „Więcej…" → „Profil domowników"
  (etykiety odpowiedzi współdzielone z quizem przez stałe `*_LABELS`/`*_VALUES`).

- **Uczenie tylko pozytywne**: zapamiętujemy wyłącznie polubienia
  („Lubię to"). Odrzucenie = po prostu „Inne propozycje".
- **AI dopiero po nauce**: świeża instalacja proponuje z wbudowanej puli
  (`BuiltInRecipes`, ~60 dań). AI przejmuje propozycje dopiero po ≥5
  polubieniach (`PersonalizationReadiness.MIN_LIKED_DISHES`). Inne funkcje AI
  (pełny przepis, „Zmień przepis", import z linku) działają od razu z kluczem.
- **Wspólny pipeline offline** (`OfflineProposalGenerator`): pula → shuffle →
  filtr „ostatnio pokazane" (3 dni) → `VariedMealPicker` (max 1 wybór wg gustu,
  reszta różnorodna). Używany przez ekran i przez powiadomienia — nie rozjeżdżać.
- **Powiadomienia 8/12/18** liczone offline (bez sieci i klucza), AlarmManager
  `setInexactRepeating` (bez uprawnienia exact-alarm), przeżywają reboot przez
  `BootReceiver`. W Doze mogą być opóźnione — świadomy kompromis.
- **Klucz API**: w repo tylko zaszyfrowany (AES/GCM + PBKDF2,
  `res/values/secrets.xml`), hasło poza repo. Po pierwszym odblokowaniu klucz
  ląduje w `SharedPreferencesSecretStore`, więc hasło podaje się raz.
  Build może też wstrzyknąć klucz przez `ANTHROPIC_API_KEY` / `local.properties`.
- **Model**: `claude-sonnet-4-6` (poprawne ID Anthropic Messages API),
  `MAX_TOKENS = 2048` (1024 ucinało dłuższe przepisy).

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
