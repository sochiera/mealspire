# Mealspire

Aplikacja Android, która pomaga wymyślić, co ugotować. Po uruchomieniu wybierasz
jednym dotknięciem porę dnia (`Śniadanie`, `Obiad`, `Kolacja`), a aplikacja **od
razu podsuwa kilka prostych propozycji** naraz. Pełny przepis powstaje dopiero,
gdy któryś pomysł Ci się spodoba. Idea: jak najmniej klikania, a aplikacja uczy
się Twojej kuchni.

## Pierwsze uruchomienie: krótki test gustu

Świeża instalacja zaczyna od **krótkiego quizu** (jedno pytanie na ekran, duże
przyciski, na każdym kroku widoczny przycisk **„Pomiń"**):

1. **Dla kogo gotujesz?** — tylko dorośli / dorośli i dzieci.
2. **Czego nie jadacie?** — wielokrotny wybór twardych wykluczeń (wegetariańsko,
   bez wieprzowiny, bez glutenu, bez laktozy, bez orzechów, bez ryb). Tego
   aplikacja **nigdy nie zaproponuje** — ani offline, ani z AI.
3. **Ile masz zwykle czasu na gotowanie w dzień powszedni?** — do 20 minut /
   około pół godziny / godzina i więcej.
4. **Jak Ci idzie gotowanie?** — dopiero zaczynam / radzę sobie / gotuję dobrze
   i lubię wyzwania.
5. **Ankieta gustu „Co wolisz?” — A czy B.** Seria par dań (domyślnie 20,
   długość do zmiany), każda para inna; kolejne pary kontrastują na zmianę
   bazą (mięso/ryba/bezmięsne), charakterem (zupa/zapiekane/świeże) i kuchnią,
   a dania przewijają się równomiernie przez cały katalog. Pasek i licznik
   („Porównanie 7 z 20”) pokazują postęp. Wybrane danie zapisuje się jako
   reakcja „lubię” na tej samej liście, z której AI ocenia kolejne dania —
   tak powstaje początkowa grupa lubianych dań; **„Żadne z tych"** uczciwie
   nie zapisuje nic. Pary respektują wykluczenia z pytania 2.
   Każda odpowiedź jest od razu zapamiętana: po zamknięciu aplikacji ankieta
   **wznawia się od tej samej pary**, a **„Przerwij — dokończę później”**
   zapisuje dotychczasowe wybory i zostawia na ekranie startowym przycisk
   **„Dokończ ankietę gustu”**. Nową ankietę (z wyborem długości: 10/20/30/40
   porównań) uruchamia **„Więcej…” → „Ankieta gustu”**.

Odpowiedzi z pytań 1–4 trafiają do **profilu domowników** i realnie wpływają na
zapytania do AI („Gotuję też dla dzieci…", „Bezwzględny wymóg diety…",
„Dopiero uczę się gotować…") — także przy **„Zmień przepis"**. Quiz pokazuje się
**tylko raz** — „Pomiń" na dowolnym kroku kończy go na zawsze, a już udzielone
odpowiedzi zostają. Profil można później zmienić w menu **„Więcej…" → „Profil
domowników"**. Systemowe „Cofnij" wraca w quizie do poprzedniego pytania
(z pierwszego pytania działa jak „Pomiń") i pozwala **zmienić wybór dania** —
liczy się ostatni wybór w parze; polubienia zapisują się przy zakończeniu albo
przerwaniu ankiety (zapisanych już wyborów „Cofnij” nie zmienia). Jednorazowe pytania startowe (liczba osób, logowanie kontem ChatGPT, zgoda na
powiadomienia) pojawiają się dopiero po zakończeniu lub pominięciu quizu —
nic nie zasłania pierwszego pytania.

## Jeden dotyk: pora dnia → kilka propozycji

Na ekranie startowym są trzy kafelki: **Śniadanie / Obiad / Kolacja**. Dotknij
jeden, a aplikacja od razu pokaże **trzy propozycje** dań. Każda propozycja to
tylko: nazwa, krótki opis, przybliżony czas i kluczowe składniki — bez czekania
na cały przepis.

Przy każdej propozycji masz trzy przyciski:

- **„Pokaż przepis”** — pełny przepis na to danie.
- **„Lubię to”** / **„Nie lubię”** — jawna ocena, z której AI uczy się Twojego
  gustu (patrz niżej). Dotknięta reakcja zostaje zaznaczona na karcie.

Pełny przepis jest podzielony na czytelne części: składniki jako lista, kroki
i akapity osobno. Pod przepisem są skróty **„Lista zakupów”** i **„Zapisz
danie”** (te same akcje co w menu „Więcej”).

Pod propozycjami jest **„Inne propozycje”** — jeden dotyk podsuwa kolejny zestaw,
więc nie musisz nic odrzucać po kolei. W jednej serii (od dotknięcia posiłku)
żadne danie się nie powtarza; gdy pokażesz już wszystkie pasujące, aplikacja
mówi to wprost, a przycisk zmienia się w **„Zacznij od nowa”**.

Systemowy przycisk **„Cofnij"** cofa o jeden widok: z pełnego przepisu do listy
propozycji (bez ponownego pytania AI — przepis jest zapamiętany), z propozycji
na ekran startowy, a dopiero z ekranu startowego zamyka aplikację. Przepis
otwarty **spoza propozycji** (np. danie dodane z linku/opisu) wraca od razu na
ekran startowy — nie ma propozycji, do których można by wrócić.

Bez zalogowania kontem ChatGPT działa offline: losuje dania z **wbudowanej puli** (blisko 60 prostych,
codziennych dań: śniadania, obiady, kolacje) i z Twojej bazy. Dania są celowo
proste, do zrobienia z tego, co zwykle jest w kuchni — a tam, gdzie naturalnie
pasują dodatki (owsianka, płatki, tosty…), są one opcją do dopisania, a nie
osobnym, bardziej skomplikowanym daniem.

Gdy jesteś zalogowany kontem ChatGPT, te same dania (wbudowana pula + Twoja baza)
**ocenia** model GPT z Twojego planu ChatGPT (domyślnie GPT Luna, do
przełączenia na GPT Sol; OpenAI Responses API, `store:false`) — pokazane są trzy
najlepiej dopasowane do Twoich reakcji, z krótkim powodem.

## Codzienne powiadomienia z propozycjami (8 / 12 / 18)

Aplikacja sama przypomina o posiłkach: **codziennie o 8:00, 12:00 i 18:00**
wysyła powiadomienie z kilkoma propozycjami odpowiednio na **śniadanie**,
**obiad** i **kolację**. Dotknięcie powiadomienia otwiera aplikację od razu na tej
porze dnia, z gotowymi pomysłami.

Propozycje w powiadomieniach powstają **offline** (z wbudowanej puli i Twojej
bazy, z uwzględnieniem polubień), więc działają zawsze — także bez internetu i bez
logowania. Przypomnienia korzystają z `AlarmManager` (lekko, bez dodatkowych
bibliotek) i przeżywają restart telefonu. Na Androidzie 13+ aplikacja poprosi raz
o zgodę na powiadomienia; bez zgody reszta działa normalnie, tylko bez przypomnień.

## Powiadomienie o nowej wersji

Aplikacja raz na dobę po cichu sprawdza, czy w repozytorium jest nowsze wydanie
(pobiera mały plik `dist/wersja.json`, bez żadnego konta ani tokenu). Jeśli tak,
na ekranie startowym pojawia się baner **„Dostępna nowa wersja … — dotknij, aby
pobrać”**. Dotknięcie otwiera link do APK w przeglądarce; po pobraniu otwierasz
plik, a system instaluje nową wersję **po wierzchu** starej — bez utraty danych
(od wersji 1.1 wszystkie wydania są podpisane tym samym kluczem). Brak internetu
niczego nie psuje — baner po prostu się nie pokazuje, a aplikacja działa dalej.

## Jak aplikacja dobiera dania: „lubisz Y — czy polubisz Z?”

Pod każdą propozycją i przepisem są przyciski **„Lubię to”** i **„Nie lubię”**.
Każde dotknięcie dopisuje do jednej, lokalnej listy reakcji: nazwę dania, krótki
skład, lubię/nie lubię i czas. Tylko to jest oceną — „Pokaż przepis” i „Inne
propozycje” nią nie są. „Nie lubię” nie jest zakazem: danie nie znika z puli,
AI po prostu oceni je (i podobne) niżej. Twarde zakazy to wyłącznie wykluczenia
diety z profilu domowników.

**Zalogowany kontem ChatGPT** — dania są **ocenione wcześniej, w tle**, więc
„Śniadanie / Obiad / Kolacja” i „Inne propozycje” pokazują je od razu, bez
czekania na AI. Aplikacja trzyma dla każdego posiłku małą gotową pulę
ocenionych dań i uzupełnia ją w tle (po uruchomieniu i gdy się kończy, zmienia
się gust albo dieta). Każde uzupełnienie to **jedno** wywołanie AI przez
serwer (`/v1/rate`): wysyła ograniczoną
listę ostatnich reakcji (nowsze ważą więcej) i kilkunastu kandydatów z
wbudowanej bazy oraz Twojej bazy dań (nazwa + skład, już przefiltrowanych
dietą). AI ocenia każdego kandydata od 0 do 10 według smaku, składników i
sposobu przygotowania i podaje jednozdaniowy powód. Aplikacja jeszcze raz
odrzuca naruszenia diety, do puli trafiają najlepiej ocenione (od 5/10), a
karty pokazują je po kolei **razem z powodem** („Dlaczego: …”). Pełny przepis
jest od razu, po wybraniu karty. Przy pustej liście reakcji AI ocenia
kandydatów po popularności. Gdy pula jest jeszcze pusta (pierwsze użycie, brak
sieci), od razu widać propozycje offline z informacją, że gotowe dania AI
dopiero się przygotowują — nic się nie zawiesza.

**Bez logowania, bez sieci albo gdy AI zwróci nieczytelną odpowiedź** —
propozycje pochodzą z lokalnej puli offline (z uwzględnieniem polubień), bez
powodu i bez udawania, że oceniało je AI. Aplikacja się nie wysypuje.

Lista reakcji zostaje na telefonie (zero telemetrii); wyczyścisz ją w
**„Więcej…” → „Zarządzaj moimi danymi” → „Wyczyść reakcje (lubię / nie
lubię)”**. Polubienia sprzed tej wersji są przenoszone na listę jednorazowo.

## Zmiana przepisu (zamienniki przez AI)

Przy pokazanym przepisie jest przycisk **„Zmień przepis”**. Możesz wpisać własną
prośbę do AI, np. *„nie mam jogurtu — czym zastąpić?”*, a aplikacja zwróci
poprawiony przepis z sensownym zamiennikiem. Wymaga zalogowania kontem ChatGPT.

### AI na Twoim koncie ChatGPT („Zaloguj się kontem ChatGPT”)

Aplikacja **nie ma własnego klucza API** — ani w repo, ani w zbudowanym APK.
Funkcje AI działają na **Twoim** koncie ChatGPT (plan Plus lub Pro) przez
oficjalne „Sign in with ChatGPT” od OpenAI:

1. Po quizie (albo w **„Więcej…” → „Zaloguj się kontem ChatGPT”**) aplikacja
   otwiera przeglądarkę ze stroną logowania OpenAI.
2. Logujesz się i zgadzasz, by Mealspire korzystał z Twojego planu ChatGPT
   (w ustawieniach ChatGPT możesz ustawić tygodniowy limit dla tej aplikacji).
3. Przeglądarka pokazuje „Gotowe — możesz wrócić do aplikacji Mealspire”.
   Wracasz do aplikacji i AI jest odblokowane.

Technicznie: OAuth 2.0 Authorization Code + PKCE z dynamiczną rejestracją
klienta (ścieżka dla aplikacji open source), przekierowanie na
`http://127.0.0.1:PORT/callback` obsługiwane przez aplikację na czas logowania.
Nie istnieje żaden sekret klienta. Tokeny (dostępowy ważny godzinę, odświeżający
30 dni, odnawiany przy każdym użyciu) są tylko w prywatnym magazynie aplikacji
i są **wyłączone z kopii zapasowej Androida**. Zapytania idą do OpenAI Responses
API i są liczone z limitu Twojego planu, nie z czyjegokolwiek klucza.

**Model AI**: domyślnie **GPT Luna** (szybki i oszczędny dla limitów planu).
W **„Więcej…” → „Model AI: …”** możesz przełączyć na **GPT Sol** (mocniejszy,
z mniejszym limitem w planie) i z powrotem; wybór jest zapamiętywany. Dokładny
identyfikator modelu aplikacja bierze z katalogu Twojego konta (`GET /v1/models`,
np. `gpt-6-luna`, `gpt-6.1-sol`). Jeśli wybranego modelu nie ma na Twoim koncie,
aplikacja to pokaże i **nie podstawi innego modelu**.

Wylogowanie: **„Więcej…” → „Wyloguj z ChatGPT”** — usuwa tokeny z telefonu
i unieważnia je po stronie OpenAI. Gdy sesja wygaśnie (np. po 30 dniach
nieużywania), aplikacja poprosi o ponowne zalogowanie.

Bez zalogowania aplikacja działa w trybie offline — po dotknięciu pory
dnia losuje kilka dań z wbudowanej puli i z Twojej bazy.

## Dla ilu osób (pytane tylko raz)

Aplikacja pyta o liczbę osób **tylko przy pierwszym uruchomieniu**. Potem już
nigdy nie pyta — pokazuje zapamiętaną wartość jako etykietę „Gotuję dla N osób”
i dołącza ją do zapytań do AI, więc przepis jest dobrany do wielkości rodziny.
Liczbę osób można w każdej chwili zmienić, dotykając tej etykiety albo w menu
**„Więcej…” → „Zmień liczbę osób”**. Obok jest etykieta trybu: **„Tryb
offline”** albo **„AI · <model>”** — dotknięcie prowadzi do logowania albo wyboru
modelu. Ustawienie przeżywa obrót ekranu i restart aplikacji.

## Menu „Więcej…”

Aby utrzymać główny ekran prostym, dodatkowe akcje są pod przyciskiem „Więcej”
(trzy kropki w prawym górnym rogu):

- **Zmień liczbę osób** — zmienia zapamiętaną liczbę osób, dla których gotujesz.
- **Ankieta gustu** — dokończenie przerwanej albo nowa ankieta A/B o wybranej
  długości; wybory dokładają się do listy reakcji „lubię”.
- **Profil domowników** — zmienia odpowiedzi z quizu startowego (dla kogo
  gotujesz, czego nie jadacie, ile masz czasu, jak Ci idzie gotowanie) oraz
  ulubione kuchnie (deklaracja opcjonalna — gustu kuchni aplikacja i tak uczy
  się z wyborów).
- **Zapisz danie do mojej bazy** — zapisuje aktualnie pokazane danie.
- **Lista zakupów** — wyciąga składniki z aktualnego przepisu i pokazuje je jako
  odhaczaną listę.
- **Dodaj danie, które znasz i lubisz** — możesz **wkleić link do przepisu** albo
  **krótko opisać danie**. Aplikacja rozpozna danie (przy linku pobiera treść
  strony), zapisze je w Twojej bazie i oznaczy jako lubiane — dzięki temu trafia
  do puli podpowiedzi. Wymaga zalogowania kontem ChatGPT.
- **Zarządzaj moimi danymi** — przejrzyj i usuwaj pojedyncze dania z bazy oraz
  wyczyść reakcje lubię/nie lubię, historię podpowiedzi lub całą bazę. Masz pełną kontrolę
  nad tym, co aplikacja o Tobie pamięta.

## Urozmaicenie — codziennie nowy zestaw, powroty po jakimś czasie

Aplikacja zapamiętuje, które dania ostatnio pokazywała — na ekranie i w
powiadomieniach (historia przeżywa obrót ekranu i restart). W trybie offline
`RecentlyShownFilter` odkłada na bok dania pokazane w ciągu **ostatnich 3 dni**,
więc każdy dzień przynosi inny zestaw zamiast tych samych kilku dań w kółko. Gdy
te 3 dni miną, danie samo "wraca" do puli — nic nie znika na stałe. Jeśli
świeżych dań zabrakłoby (bardzo mała baza albo bardzo częste odświeżanie),
podpowie się najdawniej pokazane danie zamiast pustej listy — więc powtórka
zdarza się od czasu do czasu, ale nie od razu. Tak samo dobierani są kandydaci
do oceny przez AI.

## Tryb offline: profil z polubień

Bez logowania aplikacja nie pyta AI. Z polubionych dań `TasteProfiler` wyciąga
powracające składniki i słowa-klucze, a `VariedMealPicker` bierze najwyżej
**jedną** propozycję „pod gust”, resztę losując — więc obok dań w Twoim stylu
zawsze jest coś innego. Ten profil służy wyłącznie trybowi offline (ekran bez
logowania, fallback i powiadomienia); po zalogowaniu o tym, co pokazać,
decyduje jedna ocena AI opisana wyżej.

## Jak uruchomić w Android Studio

1. Zainstaluj Android Studio: <https://developer.android.com/studio>
2. Otwórz Android Studio i wybierz `Open`.
3. Wskaż ten katalog: `/home/jan/Sources/mealspire`.
4. Poczekaj, aż Android Studio pobierze Gradle, Android Gradle Plugin i SDK.
5. Kliknij `Run`.
6. Wybierz emulator albo podłączony telefon.

## Jak zainstalować na telefonie

Najłatwiejsza droga na start:

1. W telefonie włącz `Opcje programistyczne`.
2. Włącz `Debugowanie USB`.
3. Podłącz telefon kablem USB.
4. W Android Studio kliknij `Run` i wybierz swój telefon.

Android Studio zbuduje aplikację i od razu zainstaluje ją na telefonie.

## Jak zbudować i zainstalować z terminala

W tym środowisku jest już lokalnie zainstalowany minimalny zestaw narzędzi:

- JDK: `/home/jan/.local/opt/jdk-17`
- Android SDK: `/home/jan/.local/android-sdk`
- Gradle Wrapper: `./gradlew`

Zbuduj APK:

```bash
cd /home/jan/Sources/mealspire
JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew assembleDebug
```

Podłącz telefon z włączonym `Debugowaniem USB`, zaakceptuj komunikat RSA na ekranie telefonu i sprawdź, czy jest widoczny:

```bash
/home/jan/.local/android-sdk/platform-tools/adb devices
```

Zainstaluj aplikację:

```bash
/home/jan/.local/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Jak uruchomić testy

Logika domenowa (wybór, filtrowanie, preferencje, budowanie zapytań do LLM)
jest pokryta szybkimi testami jednostkowymi JVM, które nie wymagają emulatora:

```bash
cd /home/jan/Sources/mealspire
JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew test
```

Testy obejmują też warstwę UI uruchamianą na JVM przez Robolectric (`MainActivity`).

### Testy instrumentacyjne (Espresso)

Testy z `app/src/androidTest` wymagają podłączonego urządzenia lub emulatora:

```bash
JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew connectedDebugAndroidTest
```

## Jak zrobić plik APK

W Android Studio wybierz:

`Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`

Po zakończeniu Android Studio pokaże link `locate`. APK będzie zwykle tutaj:

`app/build/outputs/apk/debug/app-debug.apk`

Taki plik można skopiować na telefon i otworzyć, ale telefon może poprosić o zgodę na instalację aplikacji spoza sklepu.

### Aktualizacja zainstalowanej aplikacji

Aplikacja sprawdza nowe wydania raz na dobę. Baner na ekranie startowym otwiera
pobieranie z postępem w Mealspire. Przy pierwszym użyciu zezwól Mealspire na
instalowanie aplikacji w ustawieniach systemowych (Android 6–7: „Nieznane
źródła”), wróć do aplikacji i potwierdź instalację w oknie Androida.
Pobrany plik jest sprawdzany przed instalacją; błędy i anulowanie pozwalają
ponowić próbę. Wyjście podczas pobierania je anuluje.
Ten mechanizm znajdzie się w następnym wydaniu APK; paczka 1.2 w `dist/`
korzysta jeszcze z przeglądarki.

Od wersji **1.1** wszystkie wydania są podpisane tym samym kluczem, więc nowszy
APK instaluje się **po wierzchu** starego — polubienia, profil i książka
kucharska zostają na miejscu.

Uwaga jednorazowa: jeśli masz zainstalowaną wersję **starszą niż 1.1**
(podpisaną dawnym, przypadkowym kluczem debug), telefon odmówi nadpisania.
Trzeba raz odinstalować starą wersję i zainstalować nową — od tego momentu
kolejne aktualizacje przechodzą już bez deinstalacji.

## Licencja

Projekt jest udostępniony na licencji **PolyForm Noncommercial License 1.0.0** —
możesz go używać, modyfikować i rozpowszechniać **za darmo do celów
niekomercyjnych**. Użycie komercyjne wymaga osobnej zgody autora. Pełny tekst
znajdziesz w pliku [`LICENSE.md`](LICENSE.md).

## Backend VPS (przygotowanie przed wydaniem)

Klient wykonuje operacje AI przez serwer Mealspire
(`https://sochiera.pl/mealspire-api`, wbudowany — aplikacja nie pyta o adres).
Po zalogowaniu kontem ChatGPT serwer otrzymuje krótkotrwały token ChatGPT
i dane gustu; logowanie i odświeżanie sesji zostają na telefonie. Bez
logowania aplikacja działa offline i nie łączy się z serwerem. Katalog backendu zapisuje się na telefonie; awaria serwera
nie usuwa cache, przepisów ani polubień. Powiadomienia działają bez sieci.

Katalog, ocena gustu LLM, propozycje, przepisy, zmiany i import obsługuje
moduł JVM `backend`. Link podczas importu jest traktowany jako opis, bez
pobierania HTML na VPS. Zmiany promptów i katalogu wymagają redeployu
backendu; zmiany UI/uprawnień Androida mogą nadal wymagać nowego APK.

[Kontrakt API](docs/design/backend-api.md) i
[bezpieczny test/wdrożenie OVH](deploy/BACKEND-RUNBOOK.md).
Przygotowany PR nie uruchamia produkcyjnego API i nie publikuje APK.
