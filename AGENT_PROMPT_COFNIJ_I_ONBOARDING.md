# Prompt dla agenta: systemowy „Cofnij" + onboarding gustu

Jesteś autonomicznym agentem programistycznym pracującym w repozytorium
aplikacji Android **Mealspire**. Twoim zadaniem są dwie funkcje opisane
poniżej. Pracuj małymi etapami w TDD; po każdym zakończonym etapie wykonaj
commit i push.

Zanim zaczniesz, przeczytaj obowiązkowo:

- `CLAUDE.md`,
- `docs/steering/projekt.md` — architektura, decyzje produktowe, pułapki UI,
- `docs/steering/budowanie.md` — build/testy, odświeżanie `dist/`, proxy.

Skrót najważniejszych zasad: Java (bez Kotlina), **zero bibliotek
third-party w kodzie produkcyjnym**, minSdk 23, UI składane programowo w
cienkiej `MainActivity` (dziedziczy po `android.app.Activity`, nie po
AppCompat), cała logika w czystych klasach `app/src/main/java/com/mealspire/app/domain/`
testowalnych JVM-owo, teksty UI i prompty po polsku.

---

## Zadanie 1: poprawna obsługa systemowego przycisku „Cofnij"

**Stan obecny:** `MainActivity` nie nadpisuje `onBackPressed()` — systemowe
„cofnij" zamyka aplikację z każdego widoku (lista propozycji, pełny przepis,
lista zakupów). Wstecz da się wrócić tylko przyciskiem w UI
(„Wróć do propozycji").

**Wymagane zachowanie:**

- pełny przepis → powrót do listy propozycji (bez ponownego wołania API —
  przepisy są cache'owane w `proposalRecipes`, patrz steering),
- lista zakupów → powrót do pełnego przepisu,
- lista propozycji → powrót do ekranu startowego (podpowiedź
  „Wybierz porę dnia…"),
- ekran startowy → zachowanie domyślne (wyjście z aplikacji),
- w trakcie onboardingu (zadanie 2) → poprzednie pytanie; z pierwszego
  pytania działa jak „Pomiń".

**Wskazówki implementacyjne:**

- Decyzję „co się dzieje po cofnięciu" wydziel do czystej klasy domenowej
  (np. `BackNavigation` operująca na enumie ekranów), z testami JVM.
  `MainActivity` tylko śledzi aktualny ekran i wykonuje wskazane przejście.
- Nadpisanie `onBackPressed()` jest OK przy obecnej konfiguracji — **nie
  włączaj** `android:enableOnBackInvokedCallback` w manifeście, bo wtedy
  `onBackPressed()` przestaje być wołane.
- Uważaj na `MainActivity.contentEpoch`: cofnięcie w trakcie trwającego
  żądania (propozycje AI, pobieranie przepisu) musi podbić epokę, żeby
  spóźniona odpowiedź nie nadpisała widoku, do którego użytkownik wrócił.
- Test Robolectric (wzoruj się na `MainActivityRobolectricTest`, ścieżka
  offline, bez AI): back z widoku przepisu wraca do propozycji, z propozycji
  na ekran startowy, ze startowego kończy Activity (`isFinishing()`).

---

## Zadanie 2: onboarding — wstępny test gustu przy pierwszym uruchomieniu

**Cel:** świeża instalacja zaczyna od krótkiego quizu, dzięki któremu appka
od razu wie coś o domownikach i guście użytkownika, zamiast startować
całkiem na zimno.

**Przebieg quizu** (jedno pytanie na ekran, duże czytelne przyciski,
wszystko po polsku, na każdym kroku widoczny przycisk „Pomiń"):

1. „Dla kogo gotujesz?" — np. tylko dorośli / dorośli i dzieci.
2. „Jak Ci idzie gotowanie?" — np. dopiero zaczynam / radzę sobie /
   gotuję dobrze i lubię wyzwania.
3. „Jakie kuchnie lubicie najbardziej?" — wielokrotny wybór z kilku pozycji
   (np. polska, włoska, azjatycka, meksykańska, bliskowschodnia).
4.–6. Trzy rundy „Które danie najbardziej Ci pasuje?" — w każdej rundzie
   3 dania wylosowane z `BuiltInRecipes`, dobrane różnorodnie (różne pory
   posiłku i style, bez powtórek między rundami). Wybrane danie = polubienie.

**Zasady:**

- Wybory dań z rund 4–6 zapisuj jako **zwykłe polubienia** przez istniejący
  mechanizm (`ChoiceLearning` / `UserPreferences` / `PreferenceStore`).
  Trzy rundy dają 3 z 5 polubień potrzebnych do progu AI
  (`PersonalizationReadiness.MIN_LIKED_DISHES = 5`) — celowo nie rób pięciu
  rund: AI nie ma przejmować propozycji na podstawie samego quizu, resztę
  ma douczyć realne używanie.
- Odpowiedzi z pytań 1–3 zapisz w nowej klasie domenowej `HouseholdProfile`
  (niemutowalna, z serializerem i store'em w `storage/` na
  SharedPreferences — wzoruj się na parach
  `PreferencesSerializer`/`SharedPreferencesPreferenceStore`).
- `HouseholdProfile` musi **realnie wpływać na prompty**: rozszerz
  `ProposalPromptBuilder` i `RecipePromptBuilder` o zdania w stylu
  „Gotuję też dla dzieci — proponuj dania, które dzieci chętnie jedzą",
  „Dopiero uczę się gotować — tylko proste przepisy z niewielu kroków",
  „Preferowane kuchnie: …". Dodaj testy promptów wzorem
  `PreferenceAwarePromptTest`. Pusty/pominięty profil nie dodaje nic.
- Dobór trójek dań do rund wydziel do czystej klasy domenowej (np.
  `OnboardingDishSampler`) z wstrzykiwanym `Random`/seedem, żeby testy były
  deterministyczne: 3 rundy × 3 dania, bez duplikatów, zróżnicowane.
- Onboarding pokazuje się tylko raz: osobna flaga ukończenia w
  `AppSettings`/SharedPreferences (niezależna od samego profilu). „Pomiń"
  na dowolnym kroku ustawia flagę i przechodzi do normalnego ekranu
  startowego; już udzielone odpowiedzi zachowaj.
- Start z powiadomienia (`EXTRA_MEAL_INDEX`): jeśli onboarding nieukończony,
  pokaż quiz, a po jego zakończeniu/pominięciu kontynuuj do wybranego
  posiłku — nie gub intencji użytkownika.
- W menu „Więcej" dodaj pozycję „Profil domowników" pozwalającą później
  zmienić odpowiedzi z pytań 1–3 (prosty dialog wystarczy).
- Obrót ekranu (manifest ma `android:configChanges`) nie może resetować
  kroku quizu — stan trzymaj w polach Activity jak reszta widoków.
- Dostępność: sensowne rozmiary tekstu, informacja nigdy tylko kolorem.

---

## Wymagania procesowe

- TDD: najpierw test, który wykazuje brak funkcji, potem minimalna
  implementacja, potem refaktor. Testy JVM dla domeny, Robolectric tylko
  dla ścieżek Androidowych (wyłącznie offline — AI nigdy nie jest wołane
  w testach).
- Kolejność: najpierw zadanie 1 (mniejsze), potem zadanie 2. W zadaniu 2
  najpierw domena (`HouseholdProfile`, sampler dań, wpływ na prompty),
  na końcu UI quizu w `MainActivity`.
- Przed każdym pushem: `./gradlew test assembleDebug` musi przechodzić
  (problemy z proxy/SDK — patrz `docs/steering/budowanie.md`).
- Po zmianach kodu odśwież `dist/mealspire-debug.apk`
  (kopia `app/build/outputs/apk/debug/app-debug.apk`).
- Zaktualizuj README (sekcja o funkcjach) i — jeśli zmieniasz reguły gry —
  `docs/steering/projekt.md`.
- Commity po polsku, prefiksy `feat:` / `test:` / `refactor:` / `docs:`,
  po każdym commicie push. Nie twórz pull requesta, chyba że użytkownik
  o niego poprosi.
- Nie kończ pracy po samej analizie: implementuj, testuj, commituj
  i pushuj każdy etap.
