# Design: agent propozycji posiłków — uczenie gustu, dane, generowanie

Plan rozwoju „mózgu" Mealspire: jak sterować agentem AI proponującym dania,
jak system ma się uczyć, co użytkownik lubi, jak zapisywać dane i jak
generować nowe przepisy. Dokument opisuje stan docelowy i etapy dojścia —
każdy etap jest samodzielnie wartościowy i zgodny z zasadami projektu
(Java, zero third-party, logika w `domain/`, TDD).

## 1. Zasada naczelna: model niczego nie pamięta — appka pamięta wszystko

LLM nie jest bazą wiedzy o użytkowniku. Cała wiedza o guście mieszka
lokalnie w appce i jest **deterministycznie** kompresowana do kontekstu
promptu przez czyste, testowalne klasy domenowe. Dzięki temu:

- zachowanie agenta da się testować bez wołania AI (tak jak dziś),
- wymiana modelu nie zeruje personalizacji,
- prywatność: żadne dane gustu nie żyją nigdzie poza telefonem.

Architektura pętli:

```
zdarzenia (sygnały) ──► dziennik gustu (append-only)
                              │  kompakcja + wygaszanie
                              ▼
                       model gustu (agregat wymiarowy)
                              │  budżetowana kompresja
                              ▼
                       kontekst promptu ──► AI ──► propozycje
                              ▲                        │
                              └──── feedback użytkownika ┘
```

Stan obecny (punkt wyjścia): tylko polubienia tytułów (`UserPreferences`),
profil z quizu (`HouseholdProfile`), historia „ugotowane"
(`MealHistory`), pochodny `TasteProfile` (do 8 termów z tokenizacji
tytułów/składników) i próg `PersonalizationReadiness.MIN_LIKED_DISHES = 5`.
To dobra baza, ale uczy się wolno (jeden sygnał = jawny lajk) i „płasko"
(worek słów zamiast wymiarów gustu).

## 2. Sygnały: czego i jak system się uczy

### 2.1 Katalog sygnałów i wag

Zasada produktowa **„uczenie tylko pozytywne"** zostaje w UI: nigdy nie
pytamy „czego nie lubisz?" o konkretne dania i nie pokazujemy list
negatywnych. Ale zachowanie użytkownika niesie też ciche sygnały — wolno
je zbierać, o ile mają małe wagi i nigdy nie stają się twardym zakazem.

| Sygnał | Zdarzenie w appce | Waga | Typ |
|---|---|---|---|
| `LIKED` | „Lubię to" | **+3.0** | jawny |
| `IMPORTED` | „Dodaj danie" (własny przepis/link) | **+3.0** | jawny |
| `RECIPE_VIEWED` | „Pokaż przepis" dla propozycji | **+1.0** | dorozumiany |
| `COOKED` | zapis do `MealHistory` | **+1.5** | dorozumiany |
| `ONBOARDING_PICK` | wybór w rundzie quizu | **+2.0** | jawny (prior) |
| `REROLLED` | „Inne propozycje" — cała trójka | **−0.3** | dorozumiany |
| `SHOWN_NOT_CHOSEN` | propozycja widziana, wybrano inną | **−0.1** | dorozumiany |

Reguły bezpieczeństwa uczenia:

- Sygnały ujemne mają **sufit**: łączny wynik cechy nie spada poniżej
  ustalonego minimum (np. −1.0). Kilka rerolli nie „banuje" kurczaka.
- Sygnały ujemne trafiają do promptu co najwyżej jako „proponuj rzadziej",
  nigdy „nie proponuj". Twardy zakaz to wyłącznie **wykluczenie** (§2.3).
- Nie uczymy się z propozycji, których użytkownik nie widział (np.
  odpowiedź AI przyszła po zmianie widoku — `contentEpoch` już to wykrywa).
- Propozycja **eksploracyjna** (§4) nie generuje sygnałów ujemnych: nie
  dostaje `SHOWN_NOT_CHOSEN` ani udziału w `REROLLED`. Z definicji zwykle
  przegrywa z faworytami — gdyby za to płaciła, system karałby własne
  eksperymenty i po kilku tygodniach „nauczyłby się", że eksplorowane
  wymiary są złe, czyli bańka wróciłaby tylnymi drzwiami. Zdarzenie niesie
  flagę pochodzenia; z eksploracji liczy się wyłącznie sukces (wybór, lajk).
- Prior z onboardingu ma słabnąć: realne zachowania (świeższe zdarzenia)
  naturalnie go przykrywają dzięki wygaszaniu (§3.2).

### 2.2 Wymiary gustu zamiast worka słów

`TasteProfiler` liczy dziś częstotliwość pojedynczych tokenów. Docelowo
każde danie opisujemy w kilku **wymiarach** i uczymy się per wymiar:

- **baza/białko**: kurczak, wołowina, wieprzowina, ryba, jaja, strączki,
  nabiał, wege…
- **kuchnia/styl**: polska, włoska, azjatycka, śródziemnomorska…
- **technika/charakter**: zupa, zapiekanka, patelnia „one-pan", sałatka,
  wypiek, na słodko/na słono…
- **wysiłek**: szybkie (≤20 min) / średnie / dłuższe.
- **pora posiłku**: profile liczone **osobno per slot** (śniadanie ≠ obiad;
  to, że ktoś lubi owsiankę rano, nic nie mówi o kolacji).

Skąd cechy dania: dla `BuiltInRecipes` — otagowane ręcznie przy daniach
(stała, mała pula ~60 pozycji, tagowanie jednorazowe). Dla dań od AI —
prosta klasyfikacja słownikowa po stronie appki (`DishTagger`), pracująca
na nazwie **i linii „Składniki:" z propozycji** (parser już ją wyodrębnia
— to znacznie mocniejszy materiał niż sam tytuł). Świadomie **bez**
wołania AI do tagowania — ma działać offline i deterministycznie w testach.

Słowniki nie zawsze trafią, więc zachowanie przy braku dopasowania jest
częścią kontraktu: **nierozpoznany wymiar = brak obserwacji** dla tego
wymiaru (nie zapisujemy fałszywego „neutralne", które rozwadniałoby
średnią), a polubienie takiego dania nadal pracuje przez tytuł
i tokenowy `TasteProfile`. Odsetek dań bez rozpoznanej bazy/kuchni jest
licznikiem diagnostycznym (§7) — gdy rośnie, słowniki wymagają
uzupełnienia; to jedyny element systemu, który się „starzeje" i wymaga
ręcznej pielęgnacji przy wydaniach.

Obecny `TasteProfile` (tokeny) zostaje jako sygnał uzupełniający —
wyłapuje niuanse, których słowniki nie znają (np. „kurkuma", „feta").

### 2.3 Wykluczenia to nie preferencje

Osobna, twarda kategoria `DietConstraints` (z onboardingu i z ustawień):
wegetariańsko / bez wieprzowiny / bez glutenu / bez laktozy / bez orzechów
/ bez ryb i owoców morza. Różnice względem gustu:

- do promptu idą jako **wymóg** („Nigdy nie proponuj…"), nie wskazówka,
- filtrują też pulę offline (`BuiltInRecipes`) i walidują odpowiedzi AI
  (§5.4) — AI może się mylić, filtr appki nie,
- **maskują wyuczony profil**: `TasteContextBuilder` (§5.1) pomija
  wartości wymiarów, przykładowe dania i termy `TasteProfile` kolidujące
  z aktualnymi wykluczeniami. Kto przechodzi na wegetarianizm, nie może
  dostawać do promptu „najchętniej: wieprzowina" z własnej historii —
  a czyszczenie dziennika nie wchodzi w grę, bo wykluczenie bywa czasowe
  (dieta) i po jego zdjęciu stary gust ma wrócić,
- nie podlegają wygaszaniu ani wagom; zmienia się je tylko ręcznie
  w „Profilu domowników".

## 3. Model danych i zapisywanie

### 3.1 Dziennik zdarzeń + model pochodny

Dwa poziomy, oba w `SharedPreferences` (wolumen jest mały — nie ma powodu
łamać zasady „zero third-party" dla SQLite/Room):

1. **`TasteEvent` — dziennik append-only** (nowy `TasteEventStore`):
   `typ zdarzenia | tytuł dania | slot posiłku | timestamp | pochodzenie
   (zwykłe/eksploracyjne)`. Surowa prawda; model można przeliczyć od zera
   ze zdarzeń — to główny powód, by trzymać zdarzenia, nie tylko agregat.
   Limit ~400 najnowszych zdarzeń; starsze są **kompaktowane** do
   zamrożonego agregatu: sum wag per (wymiar, wartość, slot),
   zdyskontowanych na moment kompakcji `T` i opatrzonych tym znacznikiem.
   Przy odczycie całość mnoży się przez `0.5^((now − T) / 60 dni)` —
   wygaszanie wykładnicze składa się poprawnie, więc wspólny mnożnik dla
   całego kompaktatu jest matematycznie dokładny.

   Uczciwe ograniczenie: „przeliczenie od zera" (np. po zmianie słowników
   tagów) działa wstecz tylko na zdarzenia wciąż obecne w dzienniku;
   kompaktat pozostaje w starej interpretacji i po prostu wygasa (po
   jednym półokresie waży połowę, po dwóch ćwierć). Limit 400 jest dobrany
   tak, by przy typowym użyciu (kilka zdarzeń dziennie) dziennik pokrywał
   co najmniej jeden półokres — czyli zdecydowaną większość efektywnej
   masy modelu.

2. **`TasteModel` — agregat pochodny**: wynik per (wymiar, wartość, slot)
   + licznik obserwacji per wymiar (pewność). Przeliczany przy zapisie
   zdarzenia (tanio — jedno zdarzenie to kilka inkrementów), cache'owany
   z `schemaVersion`; niezgodna wersja ⇒ przeliczenie z dziennika.

Serializacja jak dotąd: własne serializery tekstowe z testami
(wzorzec `MealHistorySerializer` / `PreferencesSerializer`).

**Migracja**: istniejące polubienia z `UserPreferences` stają się
zdarzeniami `LIKED` z bieżącym timestampem. `UserPreferences` zostaje jako
widok „lista polubionych dań" (UI „Więcej… → Polubione"), ale przestaje
być jedynym źródłem gustu.

### 3.2 Wygaszanie (recency)

Wkład zdarzenia maleje wykładniczo z **półokresem ~60 dni**:
`waga_efektywna = waga * 0.5^(wiek_dni / 60)`. Skutki:

- gust „płynie" za użytkownikiem (dieta, sezon, dzieci rosną),
- prior z onboardingu po 2–3 miesiącach realnego używania niemal znika,
- stare kompaktowane agregaty dostają jeden wspólny mnożnik przy kompakcji.

Dla determinizmu testów czas jest wstrzykiwany (`long now` w API klas
domenowych — tak jak w `RecentlyShownFilter`).

### 3.3 Gotowość personalizacji

Próg `MIN_LIKED_DISHES = 5` zostaje, ale liczony z modelu: suma wag
**jawnych** sygnałów pozytywnych ≥ 5 × waga lajka. Efekt praktyczny jak
dziś (5 lajków), ale import własnych dań też przybliża do progu — bo jest
równie mocną deklaracją gustu.

## 4. Optymalizacja uczenia: eksploracja vs eksploatacja

Największe ryzyko systemu uczącego się z własnych propozycji to **pętla
zwężająca** (filter bubble): model proponuje kurczaka → użytkownik wybiera
kurczaka (bo nic innego nie ma) → model uczy się, że kurczak. Środki
zaradcze, w kolejności ważności:

1. **Reguła 2+1**: z trzech propozycji dwie celują w profil, jedna jest
   **celowo eksploracyjna** — spoza dominujących wartości wymiarów
   (inne białko *lub* inna kuchnia). Offline już to robi
   (`VariedMealPicker`: max 1 wybór wg gustu); w AI wymuszamy promptem
   (§5.2). Nie oznaczamy propozycji eksploracyjnej w UI — dla użytkownika
   to po prostu trzecia propozycja.
2. **Eksploracja kierowana pewnością**: wymiar z małą liczbą obserwacji
   (np. nigdy nie pokazaliśmy ryby) jest preferowanym celem trzeciej
   propozycji. Prosta heurystyka „najmniej zbadana wartość wymiaru"
   wystarczy — nie budujemy pełnego banditа, bo sygnałów jest za mało,
   a koszt złej propozycji jest niski (jeden reroll).
3. **Anty-monotonia w prompcie**: jeśli w ostatnich N propozycjach
   dominowała jedna wartość wymiaru (np. 4× makaron), dokładamy zdanie
   „ostatnio dużo było X — zaproponuj co innego". Uogólnia dzisiejsze
   `recentToAvoid` (tytuły) na wymiary.
4. **Wybory uczą relatywnie**: `SHOWN_NOT_CHOSEN` odejmuje odrobinę tylko
   temu, co przegrało z czymś wybranym w tej samej trójce — uczy się
   z porównania, nie z absolutnej oceny (to samo podejście co pairwise
   preference elicitation w literaturze rekomendacji). Propozycja
   eksploracyjna jest z tego wyłączona (§2.1) — nie wolno karać własnych
   eksperymentów.

## 5. Sterowanie agentem AI

### 5.1 Kontrakt kontekstu: budżet i priorytety

Prompt nie może puchnąć wraz z historią. Budowanie kontekstu
(`TasteContextBuilder`, czysty, TDD) ma stały budżet sekcji, w kolejności
ważności:

1. **Wykluczenia** (zawsze, jako wymóg) — max 1 zdanie.
2. **Profil domowników** (jak dziś: dzieci, poziom, czas) — max 3 zdania.
3. **Skompresowany profil gustu** — zamiast surowej listy wszystkich
   polubień: „Najchętniej: [top 3 bazy], kuchnie: [top 2], charakter:
   [top 2]. Zwykle ma ok. X min." — dane z `TasteModel` dla bieżącego
   slotu posiłku. **Fallback cienkiego slotu**: próg gotowości (§3.3)
   jest globalny, więc 5 lajków obiadowych włącza AI także dla śniadań,
   o których model nie wie nic. Slot z mniej niż ~8 obserwacjami dostaje
   profil ogólny (suma wszystkich slotów) zamiast pustej sekcji, a jego
   trójka propozycji przesuwa się w stronę eksploracji (2 eksploracyjne
   + 1 wg profilu ogólnego) — cienki slot ma się szybko douczać, nie
   udawać, że coś wie.
4. **Przykładowe ulubione dania** — max 8 tytułów, ważone świeżością
   (nie alfabetycznie ani „wszystko"). Konkrety kotwiczą model lepiej niż
   abstrakcje, ale ogon ucinamy.
5. **Cechy wspólne** (`TasteProfile`, jak dziś) — max 8 termów.
6. **Anty-powtarzalność**: ostatnie tytuły + ewentualne zdanie
   o dominującym wymiarze (§4.3).
7. **Uwagi doraźne** (choiceFragments) — bez zmian.

Dzisiejsza sekcja „Dania, które użytkownik lubi: <wszystkie>" jest do
zastąpienia punktami 3–4: przy 50+ polubieniach surowa lista szumi
i rozmywa sygnał.

### 5.2 Struktura odpowiedzi: 2+1 w prompcie

Do promptu propozycji (przy gotowym profilu) wchodzi jawna instrukcja:

> „Pierwsze dwie propozycje dopasuj do profilu użytkownika. Trzecią
> zaproponuj celowo spoza jego utartych wyborów — [tu wstrzyknięty cel
> eksploracji, np. «danie z rybą» albo «kuchnia inna niż włoska»] — ale
> nadal prostą i zgodną z wykluczeniami."

Cel eksploracji wybiera appka (deterministycznie, §4.2), nie model — to
kluczowe dla sterowalności: eksploracja jest decyzją systemu, AI tylko ją
realizuje.

### 5.3 Generowanie pełnych przepisów

Pipeline bez zmian (propozycja → pełny przepis na żądanie), z dwiema
korektami:

- prompt przepisu dostaje **ten sam** kontekst wykluczeń + profil
  (już dostaje household; dodać wykluczenia), żeby „Zmień przepis" nie
  wyszedł poza dietę,
- przepis ma być **spójny z propozycją** (nazwa, kluczowe składniki
  z propozycji przekazywane do promptu przepisu — jak dziś) i respektować
  liczbę porcji.

### 5.4 Walidacja odpowiedzi AI (nieufność domyślna)

Po sparsowaniu propozycji appka sprawdza po swojej stronie:

- format kompletny (jest — parser),
- **wykluczenia**: słownikowy skan nazw i składników (np. wege ⇒ odrzuć
  propozycję zawierającą mięsne tokeny). Odrzucona propozycja jest
  zastępowana daniem z przefiltrowanej puli offline — użytkownik zawsze
  dostaje 3 pozycje,
- deduplikacja z `recentToAvoid` (tytuł ≈ tytuł, case-insensitive).

Zero ponownych wołań AI dla poprawek — fallback offline jest tańszy,
szybszy i przewidywalny.

## 6. Onboarding: mądrzejsze pytania

### 6.1 Zasady projektowania pytań

- Każde pytanie musi mieć **bezpośrednie ujście**: konkretne zdanie
  w prompcie albo filtr. Pytanie „do wiadomości" — wypada.
- Pytać o **fakty i ograniczenia**, nie o autodeklaracje gustu. Ludzie
  słabo raportują własny gust („lubię kuchnię włoską" bywa aspiracją);
  wybory konkretnych dań mówią więcej (dlatego literatura preferuje
  wybory par/trójek nad pytania o atrybuty).
- Maksimum ~7 ekranów; „Pomiń" zawsze dostępne (jest).

### 6.2 Docelowy zestaw: 4 pytania + 3 rundy kontrastowe

| # | Ekran | Ujście |
|---|---|---|
| 1 | **Dla kogo gotujesz?** (dorośli / z dziećmi) — bez zmian | zdanie o dzieciach w prompcie |
| 2 | **Czego nie jadacie?** (multi-wybór: wegetariańsko, bez wieprzowiny, bez glutenu, bez laktozy, bez orzechów, bez ryb; domyślnie nic) — **nowe, najważniejsze** | `DietConstraints`: wymóg w promptach + filtr puli offline + walidacja AI |
| 3 | **Ile masz zwykle czasu na gotowanie w dzień powszedni?** (do 20 min / ok. pół godziny / godzina i więcej) — **nowe** | prior wymiaru „wysiłek" + zdanie w prompcie („proponuj dania do X min") |
| 4 | **Jak Ci idzie gotowanie?** — bez zmian | jak dziś |
| 5–7 | **3 rundy wyboru dań — kontrastowe** (§6.3) | polubienia + priory wymiarów |

**Wypada pytanie o ulubione kuchnie** — to najsłabszy sygnał obecnego
quizu (abstrakcyjny, aspiracyjny). Kuchnie wywnioskujemy z rund wyboru
dań i z dalszych zachowań. Pole `cuisines` w `HouseholdProfile` zostaje
(edytowalne w „Profilu domowników" dla tych, którzy chcą to zadeklarować),
tylko znika z quizu. Netto: 7 ekranów zamiast 6 — jedno pytanie więcej,
ale każde z ujściem.

Braki odpowiedzi pozostają neutralne (wzorzec `UNKNOWN` — pominięte
pytanie nie wnosi nic do promptów).

### 6.3 Rundy kontrastowe zamiast losowych

Dziś `OnboardingDishSampler` losuje po jednym daniu z puli każdego slotu —
trójki bywają przypadkowe i wybór niesie mało informacji. Docelowo każda
runda **maksymalizuje rozstrzygalność jednego wymiaru** (active learning:
pytaj tak, żeby odpowiedź najwięcej mówiła):

- **Runda 1 — baza**: danie mięsne vs rybne vs bezmięsne
  (np. „Kotlety pożarskie / Łosoś z piekarnika / Leczo z cieciorką").
- **Runda 2 — styl**: polskie tradycyjne vs śródziemnomorskie vs azjatyckie.
- **Runda 3 — charakter**: zupa-jednogarnkowe vs zapiekane vs świeże/sałatka.

Wymaga otagowania `BuiltInRecipes` (§2.2) — sampler losuje w obrębie
przegródek tagów, więc rundy pozostają różnorodne między instalacjami,
ale zawsze kontrastowe. Wybór w rundzie zapisuje jak dziś polubienie
(**+ prior** dla wartości wymiaru, którą reprezentuje), respektując
mechanikę „Cofnij → podmiana, nie dokładanie" (`onboardingPicks`).

Do każdej rundy dochodzi czwarta opcja **„Żadne z tych"**: nie zapisuje
nic negatywnego o konkretnych daniach (zasada uczenia pozytywnego), po
prostu brak polubienia i brak prioru — a quiz idzie dalej. Bez tej opcji
zmuszamy do fałszywego sygnału.

Celowo zostaje **3 rundy** (nie 5): decyzja „AI nie ma przejmować
propozycji po samym quizie" obowiązuje — próg ≥5 nadal wymaga polubień
z realnego używania.

## 7. Miara sukcesu (lokalnie, bez telemetrii)

Appka jest offline-first i prywatna — żadnych zdarzeń nie wysyłamy.
Ale możemy mierzyć lokalnie i pokazać w ukrytym widoku diagnostycznym:

- **acceptance rate**: odsetek pokazanych trójek, z których coś wybrano
  („Pokaż przepis" / lajk) — powinien rosnąć w czasie,
- **reroll rate**: odsetek „Inne propozycje" — powinien spadać,
- rozkład propozycji per wymiar — sanity check, że eksploracja działa
  (żaden wymiar nie dominuje >60%),
- **odsetek dań AI bez rozpoznanych tagów** — wskaźnik starzenia się
  słowników `DishTaggera` (§2.2); rosnący = słowniki do uzupełnienia.

Te same liczniki służą do ręcznej oceny zmian algorytmu między wydaniami.

## 8. Etapy wdrożenia (każdy: testy → implementacja → `dist/`)

1. **Wykluczenia** (`DietConstraints` + store + pytanie w quizie + wymóg
   w promptach + filtr offline + walidacja odpowiedzi AI). Największa
   wartość użytkowa, niezależna od reszty.
2. **Dziennik zdarzeń** (`TasteEvent`, `TasteEventStore`, migracja
   polubień, zapis zdarzeń z istniejących akcji). Bez zmiany zachowania —
   tylko zbieranie.
3. **Tagowanie dań** (`DishTag`, otagowanie `BuiltInRecipes`,
   `DishTagger` słownikowy dla dań AI).
4. **Model wymiarowy** (`TasteModel` z wygaszaniem i pewnością;
   `TasteContextBuilder` — skompresowany profil zamiast pełnej listy
   polubień w promptach).
5. **Nowy onboarding** (pytanie o czas, rundy kontrastowe, „Żadne z tych";
   usunięcie pytania o kuchnie z quizu).
6. **Eksploracja 2+1** (cel eksploracji z pewności wymiarów, instrukcja
   w prompcie, anty-monotonia wymiarowa).
7. **Sygnały dorozumiane + liczniki** (reroll/shown-not-chosen z sufitem,
   acceptance/reroll rate, widok diagnostyczny).

Kolejność minimalizuje ryzyko: 1–3 nie zmieniają zachowania propozycji,
4–7 zmieniają je stopniowo i każdy etap można ocenić licznikami z 7
(warto rozważyć wciągnięcie samych liczników wcześniej, przy etapie 2).

## 9. Ryzyka i pułapki

- **Pętla zwężająca**: bez wymuszonej eksploracji system skończy na
  trzech daniach w kółko. Reguła 2+1 jest obowiązkowa, nie opcjonalna.
- **Szum sygnałów dorozumianych**: reroll bywa „nie mam dziś ochoty",
  nie „nie lubię". Stąd małe wagi, sufit ujemny i brak twardych zakazów.
- **Prompt bloat**: każda sekcja kontekstu ma limit; nowe pomysły na
  kontekst muszą się zmieścić w budżecie, nie obok niego.
- **Overfitting do onboardingu**: priory muszą wygasać — pilnuje tego
  półokres; test regresyjny: po 20 zdarzeniach z realnego używania priory
  quizu nie mogą dominować żadnego wymiaru.
- **AI ignoruje wykluczenia**: walidacja po stronie appki jest częścią
  definicji ukończenia etapu 1 — prompt sam nie wystarczy.
- **Rozjazd ekran/powiadomienia**: wszystkie zmiany doboru przechodzą
  przez wspólny pipeline (`OfflineProposalGenerator`) — nie dublować
  logiki w `notify/`.
- **Determinizm testów**: czas i losowość zawsze wstrzykiwane; Robolectric
  nadal pokrywa wyłącznie ścieżkę offline.

## 10. Inspiracje / literatura

- Trattner, Elsweiler — *Food Recommender Systems: Important Contributions,
  Challenges and Future Research Directions* (arXiv:1711.02760): przewaga
  jawnego feedbacku, problem cold-startu w domenie jedzenia.
- *Pairwise and Attribute-Aware Decision Tree-Based Preference Elicitation
  for Cold-Start Recommendation* (arXiv:2510.27342): wybory par/atrybutów
  w onboardingu niosą więcej informacji niż oceny pojedynczych pozycji.
- *Explainable Active Learning for Preference Elicitation*
  (arXiv:2309.00356): dobór pytań maksymalizujących informację skraca
  uczenie — stąd rundy kontrastowe.
- *Yum-me: A Personalized Nutrient-based Meal Recommender System*
  (arXiv:1605.07722): elicytacja gustu przez porównania wizualne dań.
- *Memory Assisted LLM for Personalized Recommendation System*
  (arXiv:2505.03824) i CoT-Rec (SIGIR'25): ekstrakcja i kompresja profilu
  użytkownika przed promptem zamiast surowej historii interakcji.
