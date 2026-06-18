# Mealspire

Aplikacja Android, która pomaga wymyślić, co ugotować. Wybierasz tylko porę dnia
(`Śniadanie`, `Obiad`, `Kolacja`), a resztą zajmuje się aplikacja. Interfejs jest
celowo prosty: jeden główny przycisk **„Zaproponuj danie”**, krótka propozycja,
a pełny przepis dopiero na żądanie. Każde kliknięcie ma natychmiastową akcję.

## Najpierw propozycja, dopiero potem przepis

Aplikacja **nie generuje od razu całego przepisu**. Najpierw pokazuje krótką
**propozycję** dania: nazwę, jedno zdanie opisu, przybliżony czas i kluczowe
składniki. Dzięki temu szybko ocenisz pomysł, nie czekając na cały przepis.

- **„Pokaż przepis”** — dopiero teraz powstaje pełny przepis na wybrane danie.
- **„Nie dla mnie”** — pytamy *dlaczego* (patrz niżej) i **od razu** podsuwamy
  nową propozycję, żebyś nie musiał czekać i klikać po raz drugi.

Gdy skonfigurowany jest klucz API, propozycje i przepisy tworzy model Claude
(Anthropic Messages API) — może przy tym **sięgnąć po danie z Twojej bazy** albo
**wymyślić zupełnie nowe**. Bez klucza API aplikacja działa offline: losuje danie
z wbudowanej puli i z Twojej bazy.

## „Nie dla mnie” — z powodem

Kiedy odrzucasz propozycję, wybierasz krótki powód:

- **Nie lubię tego wcale** — danie trafia na stałe na listę unikanych i już nie
  wróci w podpowiedziach.
- **Za trudne** — następna propozycja będzie prostsza i łatwiejsza do zrobienia.
- **Dzisiaj nie mam czasu** — następna propozycja będzie szybka (do ~20 minut).
- **Nie mam na to ochoty dzisiaj** — po prostu coś innego, bez zapamiętywania.

Powody „na dziś” sterują tylko kolejną propozycją w tej sesji; tylko „nie lubię
tego wcale” zostaje zapamiętane na stałe.

## Zmiana przepisu (zamienniki przez AI)

Przy pokazanym przepisie jest przycisk **„Zmień przepis”**. Możesz wpisać własną
prośbę do AI, np. *„nie mam jogurtu — czym zastąpić?”*, a aplikacja zwróci
poprawiony przepis z sensownym zamiennikiem. Wymaga klucza API.

### Klucz API zaszyfrowany hasłem (domyślnie)

Klucz API jest zapisany w repo **w postaci zaszyfrowanej** (AES/GCM, klucz
wyprowadzany z hasła przez PBKDF2) — w zasobie `app/src/main/res/values/secrets.xml`.
**Hasło nie jest nigdzie w repo.** Po uruchomieniu aplikacja pyta o hasło i dopiero
po jego podaniu odszyfrowuje klucz i odblokowuje generowanie przez AI. Błędne hasło
nie odblokuje klucza (chroni go znacznik uwierzytelniający GCM).

> To celowo umiarkowane zabezpieczenie: kto ma aplikację **i** hasło, odzyska klucz.
> Chodzi tylko o to, by klucz nie leżał w repo otwartym tekstem.

Możesz wygenerować nowy zaszyfrowany klucz tą samą metodą, której używa aplikacja
(`com.mealspire.app.domain.ApiKeyCipher#encrypt(klucz, hasło)`), i podmienić wartość
`encrypted_api_key` w `secrets.xml`.

### Alternatywnie: klucz przy budowaniu

Można też wstrzyknąć klucz przy budowaniu (wtedy pytanie o hasło się nie pojawia):

- zmienna środowiskowa `ANTHROPIC_API_KEY`, albo
- wpis `anthropic.api.key=...` w pliku `local.properties` (plik jest w `.gitignore`).

Bez klucza (i bez hasła) aplikacja działa w trybie offline (losowanie z wbudowanej
puli i z Twojej bazy), a przycisk „Zaproponuj danie” korzysta wtedy z tej puli.

## Dla ilu osób (pytane tylko raz)

Aplikacja pyta o liczbę osób **tylko przy pierwszym uruchomieniu**. Potem już
nigdy nie pyta — pokazuje zapamiętaną wartość jako etykietę „Gotuję dla N osób”
i dołącza ją do zapytań do AI, więc przepis jest dobrany do wielkości rodziny.
Liczbę osób można w każdej chwili zmienić w menu **„Więcej…” → „Zmień liczbę
osób”**. Ustawienie przeżywa obrót ekranu i restart aplikacji.

## Menu „Więcej…”

Aby utrzymać główny ekran prostym, dodatkowe akcje są pod przyciskiem „Więcej…”:

- **Zmień liczbę osób** — zmienia zapamiętaną liczbę osób, dla których gotujesz.
- **Zapisz danie do mojej bazy** — zapisuje aktualnie pokazane danie.
- **Lista zakupów** — wyciąga składniki z aktualnego przepisu i pokazuje je jako
  odhaczaną listę.
- **Dodaj danie, które znasz i lubisz** — możesz **wkleić link do przepisu** albo
  **krótko opisać danie**. Aplikacja rozpozna danie (przy linku pobiera treść
  strony), zapisze je w Twojej bazie i oznaczy jako lubiane — dzięki temu trafia
  do puli podpowiedzi. Wymaga klucza API.
- **Zarządzaj moimi danymi** — przejrzyj i usuwaj pojedyncze dania z bazy oraz
  wyczyść preferencje, historię podpowiedzi lub całą bazę. Masz pełną kontrolę
  nad tym, co aplikacja o Tobie pamięta.

## Urozmaicenie — dania, których dawno nie było

Aplikacja zapamiętuje, które dania ostatnio pokazywała (historia przeżywa obrót
ekranu i restart). Tryb offline podsuwa wtedy danie, którego najdłużej nie było,
a generowanie z AI dostaje listę ostatnich dań z prośbą o coś innego — dzięki
temu propozycje się nie powtarzają.

## Uczenie się preferencji

Pod każdą propozycją są przyciski „Lubię to” i „Nie dla mnie”. Twoje oceny są
zapamiętywane (przeżywają obrót ekranu i ponowne uruchomienie aplikacji) i przy
kolejnych pomysłach z AI są przekazywane do modelu, żeby podpowiadał dania bliższe
Twoim gustom i omijał te, których nie lubisz. „Nie dla mnie” dodatkowo pyta o
powód (patrz „Nie dla mnie — z powodem”).

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
