# Mealspire

Aplikacja Android, która pomaga wymyślić, co ugotować. Wybierasz porę dnia
(`Śniadanie`, `Obiad`, `Kolacja`) i albo prosisz AI o nowy pomysł na danie wraz
z przepisem, albo losujesz jeden z wbudowanych, prostych przepisów (tryb offline).

## Pomysły na dania z AI

Przycisk „Wymyśl danie (AI)” wysyła zapytanie do modelu Claude (Anthropic
Messages API) i pokazuje wygenerowaną nazwę dania oraz przepis. Wymaga klucza API.

### Konfiguracja klucza API

Klucz jest wstrzykiwany do aplikacji przy budowaniu (nie jest trzymany w repo).
Ustaw go na jeden z dwóch sposobów:

- zmienna środowiskowa `ANTHROPIC_API_KEY`, albo
- wpis `anthropic.api.key=...` w pliku `local.properties` (plik jest w `.gitignore`).

Bez klucza aplikacja nadal działa w trybie losowania gotowych przepisów, a przycisk
AI wyświetla instrukcję konfiguracji.

## Dodawanie dań, które już znasz i lubisz

W sekcji „Dodaj danie, które znasz i lubisz" możesz **wkleić link do przepisu**
albo **krótko opisać danie**. Aplikacja rozpozna danie (przy linku pobiera treść
strony), zapisze je w Twojej bazie i oznaczy jako lubiane — dzięki temu trafia do
puli podpowiedzi i wpływa na kolejne propozycje. Wymaga klucza API.

Każde pokazane danie (z AI lub z losowania) możesz też zapisać do bazy jednym
przyciskiem „Zapisz danie do mojej bazy".

Przycisk „Lista zakupów" wyciąga składniki z aktualnego przepisu i pokazuje je
jako odhaczaną listę.

## Wybory przy każdej propozycji

Przed wygenerowaniem dania możesz zaznaczyć dowolne preferencje na dziś
(np. „Szybkie”, „Wegetariańskie”, „Sycące”, „Lekkie”, „Tanie”, „Dla dzieci”).
Wybory trafiają do zapytania do AI i jednocześnie są zapamiętywane jako Twoje
upodobania — z każdym wyborem aplikacja coraz lepiej dopasowuje propozycje.

## Urozmaicenie — dania, których dawno nie było

Aplikacja zapamiętuje, które dania ostatnio pokazywała (historia przeżywa obrót
ekranu i restart). „Losuj gotowy przepis” podsuwa wtedy danie, którego najdłużej
nie było, a generowanie z AI dostaje listę ostatnich dań z prośbą o coś innego —
dzięki temu propozycje się nie powtarzają.

## Uczenie się preferencji

Pod każdym pokazanym daniem są przyciski „Lubię to” i „Nie dla mnie”. Twoje oceny
są zapamiętywane (przeżywają obrót ekranu i ponowne uruchomienie aplikacji) i przy
kolejnych pomysłach z AI są przekazywane do modelu, żeby podpowiadał dania bliższe
Twoim gustom i omijał te, których nie lubisz.

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
