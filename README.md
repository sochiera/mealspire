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
5. Trzy **kontrastowe** rundy **„Które danie najbardziej Ci pasuje?"** — każda
   runda rozstrzyga co innego (mięso/ryba/bezmięsne, zupa/zapiekane/świeże,
   polskie/śródziemnomorskie/azjatyckie), więc każdy wybór uczy maksymalnie
   dużo. Wybór zapisuje się jako zwykłe polubienie (to 3 z 5 polubień
   potrzebnych, by AI przejęło propozycje — resztę douczy normalne używanie),
   a **„Żadne z tych"** uczciwie nie zapisuje nic. Rundy respektują wykluczenia
   z pytania 2.

Odpowiedzi z pytań 1–4 trafiają do **profilu domowników** i realnie wpływają na
zapytania do AI („Gotuję też dla dzieci…", „Bezwzględny wymóg diety…",
„Dopiero uczę się gotować…") — także przy **„Zmień przepis"**. Quiz pokazuje się
**tylko raz** — „Pomiń" na dowolnym kroku kończy go na zawsze, a już udzielone
odpowiedzi zostają. Profil można później zmienić w menu **„Więcej…" → „Profil
domowników"**. Systemowe „Cofnij" wraca w quizie do poprzedniego pytania
(z pierwszego pytania działa jak „Pomiń") i pozwala **zmienić wybór dania** —
liczy się ostatni wybór w rundzie, polubienia zapisują się dopiero na końcu
quizu. Jednorazowe pytania startowe (liczba osób, logowanie kontem ChatGPT, zgoda na
powiadomienia) pojawiają się dopiero po zakończeniu lub pominięciu quizu —
nic nie zasłania pierwszego pytania.

## Jeden dotyk: pora dnia → kilka propozycji

Na ekranie startowym są trzy przyciski: **Śniadanie / Obiad / Kolacja**. Dotknij
jeden, a aplikacja od razu pokaże **trzy propozycje** dań. Każda propozycja to
tylko: nazwa, krótki opis, przybliżony czas i kluczowe składniki — bez czekania
na cały przepis.

Przy każdej propozycji masz dwa przyciski:

- **„Pokaż przepis”** — dopiero teraz powstaje pełny przepis na to danie.
- **„Lubię to”** — uczysz aplikację swojej kuchni (patrz niżej).

Pod propozycjami jest **„Inne propozycje”** — jeden dotyk podsuwa kolejny zestaw,
więc nie musisz nic odrzucać po kolei.

Systemowy przycisk **„Cofnij"** cofa o jeden widok: z pełnego przepisu do listy
propozycji (bez ponownego pytania AI — przepis jest zapamiętany), z propozycji
na ekran startowy, a dopiero z ekranu startowego zamyka aplikację. Przepis
otwarty **spoza propozycji** (np. danie dodane z linku/opisu) wraca od razu na
ekran startowy — nie ma propozycji, do których można by wrócić.

Bez zalogowania kontem ChatGPT (albo zanim aplikacja zdąży się czegoś o Tobie nauczyć — patrz
niżej) działa offline: losuje dania z **wbudowanej puli** (blisko 60 prostych,
codziennych dań: śniadania, obiady, kolacje) i z Twojej bazy. Dania są celowo
proste, do zrobienia z tego, co zwykle jest w kuchni — a tam, gdzie naturalnie
pasują dodatki (owsianka, płatki, tosty…), są one opcją do dopisania, a nie
osobnym, bardziej skomplikowanym daniem.

Gdy jesteś zalogowany kontem ChatGPT **i** aplikacja ma już wystarczająco dużo
polubień, propozycje i przepisy zaczyna tworzyć model GPT z Twojego planu
ChatGPT (OpenAI Responses API) — może przy tym **sięgnąć po danie z Twojej bazy** albo
**wymyślić zupełnie nowe**.

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

## Najpierw wbudowana baza, AI dopiero gdy ma z czego wnioskować

Świeża instalacja nie ma żadnych danych o Twoim guście, więc na start aplikacja
**zawsze** proponuje z wbudowanej bazy prostych dań — nawet jeśli jesteś
zalogowany kontem ChatGPT. Dopiero gdy polubisz co najmniej **5 dań** (czyli `TasteProfiler`
ma z czego realnie zbudować profil gustu), aplikacja przełącza się na
personalizowane propozycje AI, które potrafią też wymyślać zupełnie nowe dania.
Próg pilnuje `PersonalizationReadiness`. Inne funkcje AI (pełny przepis na
żądanie, „Zmień przepis”, dodawanie dania z linku/opisu) działają niezależnie od
tego progu — dotyczy on tylko automatycznych propozycji na start.

## Aplikacja uczy się Twojej kuchni (przede wszystkim pozytywnie)

Aplikacja **nigdy nie pyta, czego nie lubisz** — dotknięcie „Lubię to” dodaje
danie do ulubionych i to jest główny sygnał gustu. Oprócz tego cicho obserwuje
zachowanie: „Pokaż przepis” liczy się jako zainteresowanie, dodanie własnego
dania jak mocne polubienie, a „Inne propozycje” i pominięte dania to tylko
**delikatna** korekta (kilka odrzuceń niczego nie „banuje" — twarde zakazy to
wyłącznie wykluczenia diety z profilu). Z tych sygnałów aplikacja buduje
lokalny **profil gustu w wymiarach** (główny składnik, styl kuchni, charakter
dania, osobno per pora posiłku), w którym świeże wybory ważą więcej niż stare —
gust może płynąć. Do AI trafia krótki, skompresowany profil i garść ostatnio
polubionych dań, a nie cała historia. Przy trzech propozycjach obowiązuje
reguła **2+1**: dwie w Twój gust, jedna celowo inna — żeby propozycje nie
zwęziły się do trzech dań w kółko. Wszystko liczy się wyłącznie na telefonie;
podgląd: **„Więcej…” → „Zarządzaj moimi danymi” → „Statystyki uczenia”**.

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

Wylogowanie: **„Więcej…” → „Wyloguj z ChatGPT”** — usuwa tokeny z telefonu
i unieważnia je po stronie OpenAI. Gdy sesja wygaśnie (np. po 30 dniach
nieużywania), aplikacja poprosi o ponowne zalogowanie.

Bez zalogowania aplikacja działa w trybie offline — po dotknięciu pory
dnia losuje kilka dań z wbudowanej puli i z Twojej bazy.

## Dla ilu osób (pytane tylko raz)

Aplikacja pyta o liczbę osób **tylko przy pierwszym uruchomieniu**. Potem już
nigdy nie pyta — pokazuje zapamiętaną wartość jako etykietę „Gotuję dla N osób”
i dołącza ją do zapytań do AI, więc przepis jest dobrany do wielkości rodziny.
Liczbę osób można w każdej chwili zmienić w menu **„Więcej…” → „Zmień liczbę
osób”**. Ustawienie przeżywa obrót ekranu i restart aplikacji.

## Menu „Więcej…”

Aby utrzymać główny ekran prostym, dodatkowe akcje są pod przyciskiem „Więcej…”:

- **Zmień liczbę osób** — zmienia zapamiętaną liczbę osób, dla których gotujesz.
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
  wyczyść preferencje, historię podpowiedzi lub całą bazę. Masz pełną kontrolę
  nad tym, co aplikacja o Tobie pamięta.

## Urozmaicenie — codziennie nowy zestaw, powroty po jakimś czasie

Aplikacja zapamiętuje, które dania ostatnio pokazywała — na ekranie i w
powiadomieniach (historia przeżywa obrót ekranu i restart). W trybie offline
`RecentlyShownFilter` odkłada na bok dania pokazane w ciągu **ostatnich 3 dni**,
więc każdy dzień przynosi inny zestaw zamiast tych samych kilku dań w kółko. Gdy
te 3 dni miną, danie samo "wraca" do puli — nic nie znika na stałe. Jeśli
świeżych dań zabrakłoby (bardzo mała baza albo bardzo częste odświeżanie),
podpowie się najdawniej pokazane danie zamiast pustej listy — więc powtórka
zdarza się od czasu do czasu, ale nie od razu. Generowanie z AI dostaje
dodatkowo listę ostatnich dań z prośbą o coś innego.

## Uczenie się preferencji (tylko polubienia)

Przy każdej propozycji oraz przy pokazanym przepisie jest przycisk „Lubię to”.
Twoje polubienia są zapamiętywane (przeżywają obrót ekranu i ponowne uruchomienie
aplikacji) i przy kolejnych pomysłach z AI są przekazywane do modelu, żeby
podpowiadał dania w podobnym duchu. Aplikacja **nie zapamiętuje nic negatywnie** —
jeśli pomysł Ci nie pasuje, po prostu poproś o „Inne propozycje”.

### Podpowiada nie tylko to, co już lubisz — i nie zapętla się na jednym

Aplikacja nie ogranicza się do dań, które już polubiłeś. Z Twoich polubień
wyciąga **cechy wspólne** — powracające składniki i słowa-klucze (np. *kurczak*,
*kasza*, *feta*) — i na tej podstawie podsuwa również **nowe** dania, które mają z
nimi coś wspólnego (podobne składniki, technika, charakter). Profil gustu buduje
`TasteProfiler` na podstawie nazw polubionych dań oraz — jeśli są znane — ich
składników (z Twojej bazy i z wbudowanej puli).

Gust to jednak tylko **wskazówka, nie reguła**. Jeśli polubisz np. trzy dania z
kurczakiem, aplikacja **nie będzie proponować samego kurczaka**:

- **AI** dostaje wyraźną prośbę o różnorodność — najlepiej każda z propozycji z
  innym głównym składnikiem i przynajmniej jedna zupełnie nowa, inna niż zwykle.
- **Offline** `VariedMealPicker` bierze najwyżej **jedną** propozycję „pod gust",
  a pozostałe dobiera losowo — więc dania pasujące do Twoich upodobań się
  pojawiają, ale obok nich zawsze jest coś innego.

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
