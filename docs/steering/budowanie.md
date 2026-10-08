# Steering: budowanie, testy, wydania

## Standardowy build

```bash
./gradlew test assembleDebug
```

- Gradle wrapper: 8.9, AGP 8.7.3, JDK 17, compileSdk 35.
- Testy: ~220 testów JVM/Robolectric, wszystkie muszą przechodzić przed pushem.
- APK wyjściowy: `app/build/outputs/apk/debug/app-debug.apk` (~80 KB — appka
  nie ma zależności runtime poza platformą).

## Zbudowana appka w repo — checklist wydania

W `dist/mealspire-debug.apk` trzymamy **aktualny debug APK**, a w
`dist/wersja.json` metadane, z których zainstalowana aplikacja dowiaduje się
o nowej wersji. Po każdej zmianie kodu, która trafia do gałęzi/PR-a:

1. Podbij `versionCode` (+1) i `versionName` w `app/build.gradle`.
2. Zaktualizuj `dist/wersja.json` na te same wartości.
3. `./gradlew test assembleDebug` (spójności wersji pilnuje
   `ReleaseConsistencyTest` — rozjazd wywala testy).
4. `cp app/build/outputs/apk/debug/app-debug.apk dist/mealspire-debug.apk`

APK jest podpisywany wspólnym keystore `signing/mealspire.keystore`
(hasło `mealspire`, celowo jawne — patrz steering projektu). **Nie generować
nowego keystore** — zerwałoby to ciągłość aktualizacji u użytkowników.

CI (`.github/workflows/build.yml`) buduje APK przy pushu/PR. Artefakt
`mealspire-debug-apk` wystawia tylko przy pushu do main; przygotowanie PR nie
publikuje paczki. W pracach przed ship-it pozostaw numery wydania i APK w dist
bez zmian. Po zgodzie na wydanie wykonaj checklistę oraz zapisz SHA-256 nowego
APK w `dist/wersja.json` (`sha256sum dist/mealspire-debug.apk`).

## Budowanie w środowiskach z proxy/sandboksem (np. Claude Code web)

- Wrapper może nie pobrać dystrybucji Gradle: `services.gradle.org`
  przekierowuje na `github.com`, a sesyjne proxy potrafi blokować GitHuba poza
  repo projektu. Obejście: użyj lokalnego Gradle ≥ 8.9 (np. `/opt/gradle`).
- Brak Android SDK: pobierz cmdline-tools z `dl.google.com` (zwykle dozwolone),
  potem:

  ```bash
  yes | sdkmanager --sdk_root=/opt/android-sdk \
      "platforms;android-35" "build-tools;35.0.0" "platform-tools"
  export ANDROID_HOME=/opt/android-sdk
  /opt/gradle/bin/gradle test assembleDebug
  ```

- JVM za proxy: truststore i proxy są wstrzykiwane przez `JAVA_TOOL_OPTIONS` —
  nie wyłączać weryfikacji TLS.

## Konwencje commitów i PR

- Commity po polsku, prefiksy typu `feat:`, `fix:`, `docs+build:`, autor
  `Jan Sochiera <jan@sochiera.pl>`.
- Nie commitować `local.properties`. Aplikacja nie potrzebuje żadnego klucza API
  (AI działa na koncie ChatGPT użytkownika) — nie dodawać kluczy do buildu.
- README opisuje funkcje użytkownika — aktualizować przy zmianach zachowania.
