# Mealspire

Aplikacja Android, która pomaga wymyślić, co ugotować. Wybierasz tylko porę dnia
(`Śniadanie`, `Obiad`, `Kolacja`) oraz dla ilu osób, a resztą zajmuje się aplikacja.
Interfejs jest celowo prosty: jeden główny przycisk **„Wygeneruj danie”**, ocena
dania (`Lubię to` / `Nie dla mnie`) oraz menu **„Więcej…”** z dodatkowymi opcjami.

## Jeden przycisk: „Wygeneruj danie”

Przycisk „Wygeneruj danie” zawsze podaje jedno danie wraz z przepisem. Gdy
skonfigurowany jest klucz API, danie generuje model Claude (Anthropic Messages
API) — może przy tym **sięgnąć po danie z Twojej bazy** albo **wymyślić zupełnie
nowe**, w zależności od tego, co pasuje. Bez klucza API aplikacja działa offline:
losuje danie z wbudowanej puli i z Twojej bazy.

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
puli i z Twojej bazy), a przycisk „Wygeneruj danie” korzysta wtedy z tej puli.

## Dla ilu osób (zapamiętywane)

Liczba osób, którą wybierzesz, jest **zapamiętywana** — przy kolejnym
uruchomieniu aplikacja startuje z ostatnio wybraną wartością (przeżywa obrót
ekranu i restart). Liczba osób trafia też do zapytania do AI, więc przepis jest
dobrany do wielkości rodziny.

## Menu „Więcej…”

Aby utrzymać główny ekran prostym, dodatkowe akcje są pod przyciskiem „Więcej…”:

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
