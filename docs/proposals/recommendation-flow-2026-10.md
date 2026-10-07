# Propozycja nowego flow rekomendacji posiłków

**Stan analizy:** `origin/main` @ `fb05314484958a252746f8b92407ff4500b2626b` (2026-10-07). To propozycja produktu i architektury; nie zmienia implementacji.

## Rekomendacja

Przenieść decyzję „co pokazać” z promptu modelu do lokalnego, mierzalnego rankera. Model ma dostarczać kandydatów i przepisy, a aplikacja ma wybierać, filtrować, mieszać i uczyć się z jawnych działań użytkownika. Profil, zdarzenia i historia pozostają na telefonie. Nie potrzeba autonomicznego zespołu agentów ani trenowania modelu na rozmowach.

## Jak działa obecny flow

Po wyborze pory aplikacja pokazuje trzy karty. Bez logowania ChatGPT albo przed pięcioma polubieniami bierze je z lokalnej puli wbudowanych dań i książki kucharskiej. Po osiągnięciu progu jedna prośba do Responses API generuje trzy propozycje tekstowe; prompt zawiera profil domowników, skrócony profil gustu i kilka polubionych dań, ostatnio pokazane tytuły i kontekst wybrany przez użytkownika. Aplikacja parsuje tekst, odrzuca duplikaty i wykryte naruszenia diety oraz uzupełnia braki daniami offline. Pełny przepis tworzy dopiero po otwarciu karty.

Dane personalizacji są lokalne, w SharedPreferences, a nie w zdalnej bazie. Profil domowników, polubienia, książka dań, dziennik gustu i jego kompaktowany agregat mają osobne magazyny. Dziennik gustu zawiera typ zdarzenia, tytuł, porę, czas i flagę eksploracji. Model wymiarowy waży zdarzenia, wygasza je z półokresem 60 dni, a prompt dostaje skrót zamiast całej historii. Aplikacja zapisuje też ostatni czas pokazania tytułu; ten wpis powstaje przy wyświetleniu propozycji, więc nie jest potwierdzeniem ugotowania. Propozycja jest bezstanowa po stronie modelu; bieżący klient ustawia `store:false`.

## Mocne strony

- Offline-first: brak konta i sieci nie blokuje propozycji, a pełny przepis jest tworzony dopiero na żądanie.
- Aplikacja zachowuje pamięć i odpowiedzialność za logikę: zdarzenia, wygaszanie, profile per pora, eksploracja, filtrowanie i fallback są lokalne i testowalne.
- Gust jest wskazówką, a wykluczenia diety odrębnym wymogiem. Jest też kontrola odpowiedzi modelu i reguły, które ograniczają monotonię.
- Brak telemetrii; model dostaje skompresowany kontekst zamiast dziennika. Użytkownik może wyczyścić dane.
- Prosty interfejs daje trzy krótkie pomysły bez czekania na pełny przepis.

## Słabe punkty obecnego podejścia

- Model jednocześnie wymyśla, porządkuje i różnicuje całą trójkę. Aplikacja nie liczy jakości dopasowania kandydatów, a separator tekstowy i słownikowe tagowanie są kruche. Gdy część kart odpadnie, offline fallback może dać zestaw o różnym pochodzeniu i jakości.
- Stały próg pięciu polubień mierzy liczbę, nie pewność profilu. Jedna osoba może polubić pięć podobnych dań, inna pięć bardzo różnych; obie trafiają w ten sam tryb.
- Ciche sygnały są niejednoznaczne: „Inne propozycje” lekko obniża ocenę każdej zwykłej karty, choć powód może dotyczyć tylko jednej albo dostępnego czasu. Otwarcie przepisu oznacza zainteresowanie, niekoniecznie aprobatę.
- Brak trwałego ID zestawu i karty oraz powiązania z rolą karty, wersją promptu/modelu i wynikiem. Nie da się odtworzyć, co ranker pokazał ani ocenić, która cecha doprowadziła do wyboru. Statystyki są zbiorcze.
- Rozpoznawanie diety i gustu opiera się częściowo na słownikach. Nieznane składniki nie uczą modelu, a tekstowa kontrola nie daje gwarancji bezpieczeństwa alergicznego. Pełny przepis wymaga osobnej kontroli.
- Dane o „ostatnio pokazanych” i zdarzenia gustu mają różne role, ale nazwa MealHistory i wcześniejsze opisy mogą sugerować posiłki faktycznie ugotowane. Aplikacja nie ma obecnie wiarygodnego sygnału „ugotowałam/ugotowałem”.

## Trzy proponowane zmiany

### 1. Aplikacja wybiera karty; model dostarcza kandydatów

Utrzymać trzy propozycje, ale rozdzielić pipeline na: kandydaci → twarde filtry → ranking → różnorodny zestaw. Kandydatami są dania wbudowane, zapisane przez użytkownika i krótkie propozycje AI. Każdy kandydat ma stabilne ID, źródło, czas, listę składników i rozpoznane cechy. Dla nowych idei model może przygotować większą pulę niż potrzeba; deterministyczny kod odrzuca braki i duplikaty, stosuje dietę, a potem punktuje dopasowanie, czas, zgodność z prośbą, świeżość i powtarzalność.

Trójka powinna zawierać najlepsze dopasowania oraz eksperyment dobrany do niepewnej cechy profilu — tylko gdy istnieje dość pewny punkt odniesienia. W przeciwnym razie pokazywać trzy dobre, różnorodne karty bez sztucznego „1 zaskoczenia”. Nieobecność reakcji nie jest negatywną oceną.

### 2. Zdarzenia odnoszą się do tego, co faktycznie pokazano i wybrano

Zapisywać lokalnie batch rekomendacji i jego karty: identyfikator batcha/kandydata, slot, pozycję i rolę (dopasowanie/eksperyment), źródło, czas pokazania, wersję polityki/promptu oraz niewielki zestaw cech użytych do rankingu. Działania zapisują ID karty i rodzaj sygnału. „Więcej propozycji” jest zdarzeniem dla batcha; nie karze automatycznie wszystkich kart. Samo nieotwarcie karty nie uczy negatywnie.

Na karcie można użyć „Lubię”, „Nie dla mnie” z opcjonalnym powodem (np. składnik, za długo, nie mam tego) i „Ugotowane” jako osobnego, dobrowolnego potwierdzenia. Otwarcie przepisu i zapis do książki zostają osobnymi, słabszymi zachowaniami. Jawna dieta ma nadal twardy filtr; opinie o guście nigdy nie stają się zakazem.

Ranker aktualizuje na urządzeniu wagi cech według slotu i kontekstu: jawny wybór ma większy wpływ niż samo otwarcie, a nowsze dane stopniowo wypierają stare. Zachowuje licznik pewności dla każdej cechy; eksploruje tam, gdzie brakuje danych. Nie wysyła dziennika do modelu i nie „trenuje” jego wag. Agregat jest odtwarzalny z ograniczonego czasowo dziennika; usuwanie danych czyści zdarzenia, agregat i historię równie konsekwentnie.

### 3. Jeden wąski agent z wersjonowanym kontraktem

Zdefiniować specjalistę „Pomysły na posiłek”: model wybrany przez użytkownika (Luna/Sol), stałe instrukcje, jawny format wejścia i wyjścia, wersja konfiguracji i lokalne kontrole. Wejście obejmuje tylko bieżącą prośbę, slot, potrzebny fragment profilu, dietę, czas/umiejętności i ograniczoną listę kandydatów. Nie przekazywać całego logu, całej książki ani wolnego tekstu, który nie jest potrzebny do tej decyzji. Nie dodawać trwałej pamięci rozmowy, swobodnych narzędzi ani zapisujących uprawnień. Jeśli później potrzebne będzie wyszukiwanie, dać agentowi wyłącznie konkretną, tylko do odczytu funkcję aplikacji.

Wyjście powinno mieć pola typu: nazwa, krótkie uzasadnienie, czas, składniki, cechy/tagi i ID istniejącego dania albo znacznik nowego kandydata. Używać Structured Outputs, jeśli parametr formatu jest obsługiwany dla wybranego modelu i token-sharing; w przeciwnym razie parser nadal waliduje lokalnie wynik i odrzuca niepoprawne elementy. Dieta, deduplikacja i ranking zostają w aplikacji. OpenAI zaleca zaczynać od jednego skoncentrowanego agenta oraz rozdzielać instrukcje, narzędzia, wyjście strukturalne i guardrails; to pasuje do Mealspire lepiej niż orkiestracja wielu agentów ([definiowanie agentów](https://developers.openai.com/api/docs/guides/agents/define-agents), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses)). Obecny flow logowania używa ChatGPT Responses API z `store:false`; dokumentacja SIWC pokazuje strumieniowanie i ustawienie `store:false`, dlatego obsługę schematu/narzędzi trzeba potwierdzić w tym konkretnym trybie przed zależnością od niej ([SIWC: modele i inferencja](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)).

## Proponowany bazowy flow

1. **Ustawienia i onboarding.** Zapis lokalny profilu domowników, twardych wykluczeń, czasu, umiejętności i jawnych preferencji. Krótkie rundy wyboru dań mogą być priorem startowym, lecz nie włączają AI wyłącznie przez osiągnięcie liczby polubień.
2. **Prośba o posiłek.** Użytkownik wybiera śniadanie/obiad/kolację i opcjonalnie szybkie filtry: „mam mało czasu”, „użyj tych składników”, „coś znanego/nowego”. Ustawienia domyślne pozwalają zachować flow jednego dotknięcia.
3. **Budowa zestawu.** Aplikacja ładuje lokalny profil, dozwolone dania i ostatnie pokazania; tworzy minimalny snapshot kontekstu. Twarde ograniczenia filtrują kandydatów przed modelem. Jeżeli potrzeba nowych pomysłów i dostępny jest ChatGPT, wąski agent tworzy kandydatów. W przeciwnym razie używany jest ten sam lokalny ranker na katalogu offline.
4. **Ranking i pokazanie.** Lokalny kod waliduje pełne pola, ponownie stosuje wykluczenia i duplikaty, liczy dopasowanie/pewność/świeżość/różnorodność i wybiera trzy pozycje. Każda karta pokazuje nazwę, czas, składniki oraz krótkie „dlaczego”: np. pasuje do polubionego stylu, mieści się w czasie, jest nowym eksperymentem. Eksperyment jest oznaczony jako taki.
5. **Działanie i pamięć.** Po otwarciu karta może otrzymać pełny przepis; przed wyświetleniem aplikacja kontroluje także jego składniki. Karta zapisuje batch i zdarzenia lokalnie. „Ugotowane” pozostaje opcjonalne i odróżnione od „pokazane”. Użytkownik może usunąć zdarzenie/profil, a profil gustu przelicza się ponownie.
6. **Uczenie i ocena.** Po jawnym feedbacku ranker aktualizuje profil cech na urządzeniu, osobno dla pory posiłku, z wygaszaniem i niepewnością. Lokalne statystyki pokazują m.in. wybór, „nie dla mnie”, ugotowanie, ponowne pokazanie i pokrycie tagami. Zmiany polityki najpierw porównuje się na stałym zestawie przypadków i testach domenowych; nie potrzeba telemetrii ani trenowania LLM na prywatnych historiach.

## Co zapisywać lokalnie

| Zapis | Cel |
|---|---|
| Profil, jawne wykluczenia, ustawienia i książka dań | Personalizacja, twarde filtrowanie, odtworzenie zapisanych przepisów. |
| Kandydaci zapisani przez użytkownika; AI-owe pomysły tylko wtedy, gdy zostały zapisane | Stabilne ID i tagi; nie zaśmiecać bazy ulotnymi wynikami. |
| Batch i pokazane karty z wersją rankera/agenta oraz minimalnym snapshotem cech | Ustalić, co system rzeczywiście zaproponował i po co. Bez przechowywania pełnych promptów. |
| Zdarzenia: pokazano, otwarto, polubiono, odrzucono z powodem, zapisano, opcjonalnie ugotowano; timestamp i ID | Rozróżnić ekspozycję, zainteresowanie, preferencję i wykonanie. |
| Wersjonowany profil pochodny z wynikami i pewnością cech | Tanie rankingowanie; zawsze możliwa odbudowa z dziennika. |

Nie utrwalać promptu/odpowiedzi całej sesji AI, tokenów w logach, lokalizacji ani surowej historii rozmów. Dane źródłowe i uczenie pozostają na telefonie; do ChatGPT wysyłany jest tylko minimalny kontekst potrzebny do aktualnego pomysłu, po zalogowaniu użytkownika. Dla batchy wystarczy retencja ograniczona do okresu potrzebnego na uczenie i debugowanie lokalne; starsze zdarzenia można agregować i wygaszać. Jeśli dane przestaną być małe, dopiero wtedy przenieść magazyn zdarzeń do lokalnej tabeli SQLite/Room z migracją ze SharedPreferences. Nie dodawać synchronizacji serwerowej w pierwszym kroku.

## Referencje do obecnego kodu

- Flow i rejestrowanie zdarzeń: `MainActivity.java`.
- Składanie promptu i kontekstu: `ProposalPromptBuilder.java`, `TasteContextBuilder.java`, `RecipeRequest.java`.
- Model, wygaszanie i eksploracja: `TasteModel.java`, `TasteEvent.java`, `ExplorationPlanner.java`.
- Walidacja i offline fallback: `ProposalValidator.java`, `OfflineProposalGenerator.java`, `DietConstraints.java`.
- Lokalne magazyny: `SharedPreferencesTasteEventStore.java`, `SharedPreferencesMealHistoryStore.java`, `SharedPreferencesCookbookStore.java`.
- Wcześniejszy design obecnego modelu: `docs/design/agent-gustu.md` (etapy 1–7 są już wdrożone).
