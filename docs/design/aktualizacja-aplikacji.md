# Aktualizacja aplikacji: analiza, design, taski

Stan: **Faza 0 i Faza 1 zaimplementowane** (TDD). Faza 2 w backlogu.
Data: 2026-07-05.

Zrealizowane: wspólny keystore + `signingConfig` (T1), `versionCode 2`/`1.1`
+ `dist/wersja.json` + `ReleaseConsistencyTest` (T2), `VersionInfo`/
`VersionInfoParser` (T3), `UpdateChecker` (T4), `UpdateStateStore` +
impl SharedPreferences (T5), baner + sprawdzanie w `MainActivity`
(`VersionJsonSource`, `HttpVersionJsonFetcher`, T6), dokumentacja (T7).

## Część 1 — Analiza wykonalności

### Cel

Użytkownik ma zainstalowaną Mealspire z sideloadu (`dist/mealspire-debug.apk`).
Chcemy, żeby mógł dostać nowszą wersję **bez utraty danych** (profil domowników,
dziennik gustu, książka kucharska, klucz API w SharedPreferences) i najlepiej
bez ręcznego szukania pliku APK.

### Stan obecny — co mamy

- Dystrybucja: debug APK w repo (`dist/mealspire-debug.apk`, repo **publiczne**)
  oraz artefakt CI `mealspire-debug-apk` z każdego pusha.
- `versionCode 1`, `versionName "1.0"` — **nigdy nie podbijane**. Android nie
  odróżnia „nowszej" wersji, a my nie wiemy, co użytkownik ma zainstalowane.
- Brak keystore w repo i brak `signingConfig` — każdy build debug jest
  podpisywany **lokalnym** `~/.android/debug.keystore` środowiska, w którym
  powstał (runner CI, kontener Claude, laptop). Każde środowisko generuje
  własny klucz.
- Warstwa sieciowa: goły `HttpURLConnection` (`net/HttpClaudeClient`,
  `net/HttpPageFetcher`) — wzorzec łatwy do powielenia. Uwaga: `PageFetcher`
  **nie nadaje się do reużycia** dla metadanych wersji — przepuszcza treść
  przez `HtmlTextExtractor` i tnie do 6000 znaków; JSON potrzebuje surowego
  body.
- Ograniczenia projektowe: Java, **zero bibliotek third-party w kodzie
  produkcyjnym** (także androidx!), minSdk 23 / targetSdk 35, logika w
  `domain/` rozwijana w TDD, żadnych wywołań sieciowych w testach.

### Bloker nr 1: niestabilny podpis APK

Android instaluje aktualizację „po wierzchu" tylko, gdy nowy APK ma **ten sam
podpis** i **wyższy versionCode**. Dziś kolejne APK z `dist/` są podpisywane
różnymi, jednorazowymi kluczami debug, więc każda podmiana wersji to w praktyce
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` → deinstalacja → **utrata wszystkich
danych użytkownika**. Dopóki tego nie naprawimy, żaden mechanizm aktualizacji
(ręczny ani automatyczny) nie ma sensu.

Rozwiązanie: dedykowany keystore o długiej ważności + `signingConfig` w
`app/build.gradle`, używany i lokalnie, i w CI.

Gdzie trzymać klucz — dwie opcje:

| Opcja | Plusy | Minusy |
|---|---|---|
| **Keystore zacommitowany do repo** (hasło jawne w `build.gradle`) | Zero konfiguracji; każdy build (CI, kontener agenta, laptop) podpisuje tak samo | Repo jest publiczne → każdy może podpisać APK „udający" Mealspire. Dla appki hobbystycznej bez Play Store ryzyko realnie niskie (atakujący i tak musiałby nakłonić użytkownika do sideloadu) |
| Keystore w sekrecie CI (base64 w GitHub Secrets), lokalnie przez `local.properties` | Klucz prywatny nie wycieka | APK z sesji agentowych (odświeżanie `dist/`) miałby inny podpis niż z CI — wraca problem. Wymagałoby przeniesienia odświeżania `dist/` w całości do CI |

Rekomendacja: **keystore w repo**. To świadomy kompromis bezpieczeństwa w duchu
wcześniejszych decyzji projektu (pragmatyzm appki rodzinnej ponad twierdzę) —
odnotować w steeringu. Uwaga: istniejące instalacje podpisane starym kluczem
debug i tak wymagają **jednorazowej** reinstalacji po zmianie podpisu.

### Bloker nr 2: wersjonowanie

- `versionCode` musi rosnąć przy każdym wydaniu. Najprościej: podbijać ręcznie
  w `app/build.gradle` przy zmianach trafiających do `dist/` (reguła w
  steeringu). Automat z gita (`git rev-list --count HEAD`) jest kuszący, ale
  build musiałby zawsze widzieć pełną historię `.git` (CI robi płytki
  checkout — trzeba by `fetch-depth: 0`), a build z brudnego drzewa roboczego
  dawałby mylące numery. Zostajemy przy ręcznym podbijaniu.
- Dyscyplinę pilnuje test (patrz design): testy JVM widzą wygenerowany
  `BuildConfig.VERSION_CODE`, więc mogą go wprost porównać z `dist/wersja.json`
  — bez parsowania `build.gradle`.

### Skąd aplikacja ma wiedzieć o nowej wersji i skąd ją pobrać

Repo jest publiczne, więc bez żadnego tokenu działają:

1. **Plik metadanych w repo** — `dist/wersja.json` czytany z
   `https://raw.githubusercontent.com/sochiera/mealspire/main/dist/wersja.json`,
   APK z `https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk`.
   - Plusy: zero dodatkowej infrastruktury, spójne z obecnym workflow
     („odśwież `dist/`" rozszerza się o „odśwież `wersja.json`").
   - Minusy: łatwo zapomnieć o aktualizacji JSON-a (pilnuje test);
     raw.githubusercontent cache'uje ~5 min, więc tuż po wydaniu metadane
     i APK mogą być chwilowo z różnych commitów — przy sprawdzaniu raz na
     dobę okno jest pomijalne.
2. **GitHub Releases** — CI publikuje release z APK przy tagu; aplikacja pyta
   `https://api.github.com/repos/sochiera/mealspire/releases/latest`.
   - Plusy: wersje niemutowalne, naturalne miejsce na changelog.
   - Minusy: nowy workflow wydawniczy (tagowanie), limit API 60 zapytań/h/IP
     bez tokenu (przy jednym sprawdzeniu dziennie bez znaczenia).

Rekomendacja: **wariant 1** (najmniej ruchomych części, pasuje do obecnego
procesu), z opcją migracji na Releases, gdy proces wydawniczy dojrzeje.

### Jak dostarczyć aktualizację — trzy poziomy ambicji

#### Poziom A — „powiadom i otwórz przeglądarkę" (rekomendowany start)

Aplikacja co najwyżej raz na dobę pobiera w tle `wersja.json`, porównuje
z `BuildConfig.VERSION_CODE` i jeśli jest nowsza wersja, pokazuje na ekranie
startowym nieinwazyjny baner („Dostępna wersja 1.1 — pobierz"). Kliknięcie
odpala `Intent.ACTION_VIEW` z URL-em APK — przeglądarka pobiera plik,
użytkownik go otwiera i system przeprowadza instalację po wierzchu.

- Nowe uprawnienia w manifeście: **żadne** (INTERNET już jest). Przy
  **pierwszej** takiej instalacji Android 8+ poprosi o zgodę „instalowanie
  nieznanych aplikacji" dla appki, z której otwarto plik (przeglądarka lub
  menedżer plików) — jednorazowe kliknięcie w systemowym oknie, potem już
  nie wraca.
- Nowy kod: dwie małe klasy w `domain/` (pełne TDD), jedna w `net/`, jedna
  w `storage/`, baner w `MainActivity`. Szacunkowo ~200–300 linii + testy.
- Ryzyka: minimalne. Brak sieci / błąd HTTP → cicho nic nie pokazujemy
  (aktualizacja nie jest funkcją krytyczną).

#### Poziom B — pobieranie w aplikacji + systemowy instalator

Jak A, ale aplikacja sama pobiera APK i uruchamia instalację.

Wymagania techniczne (tu robi się grubiej):

- **`REQUEST_INSTALL_PACKAGES`** w manifeście — wymagane na Androidzie 8+
  niezależnie od drogi instalacji (intent czy `PackageInstaller`); użytkownik
  jednorazowo daje zgodę „instalowanie nieznanych aplikacji" **dla Mealspire**
  (przekierowanie przez `ACTION_MANAGE_UNKNOWN_APP_SOURCES`). Na Androidzie
  6.0–7.1 (minSdk 23–25) obowiązuje zamiast tego stary **globalny** przełącznik
  „Nieznane źródła".
- Udostępnienie pliku instalatorowi: na targetSdk 35 `file://` rzuca
  `FileUriExposedException`. Standardowe wyjście to `FileProvider`, ale on
  jest z **androidx — zakazane**. Alternatywy zgodne z regułami projektu:
  - własny minimalny `ContentProvider` z `openFile()` (czysta platforma,
    ~50 linii), albo
  - **`PackageInstaller`** (API 21+, w całości w naszym minSdk): strumieniujemy
    APK do sesji instalacyjnej, bez wystawiania URI. Ścieżka „kanoniczna";
    system i tak pokazuje użytkownikowi dialog potwierdzenia.
- Weryfikacja pobrania: SHA-256 z `wersja.json` przed instalacją (HTTPS chroni
  transport, hash chroni przed uciętym plikiem i oknem cache raw.github).
- Szacunkowo ~300–500 linii + testy; UI postępu, obsługa błędów, sprzątanie
  starych plików.

#### Poziom C — pełny automat (cicha instalacja w tle)

Niemożliwy bez uprawnień systemowych/device-owner. Nie rozważamy.

### Rekomendowany plan

1. **Faza 0:** stabilny podpis + wersjonowanie + `dist/wersja.json` (fundament).
2. **Faza 1 = Poziom A:** baner + przeglądarka. 90 % wartości przy minimalnym
   ryzyku.
3. **Faza 2 = Poziom B (opcjonalnie):** dopiero gdy ścieżka przez przeglądarkę
   okaże się uciążliwa.

### Ryzyka i decyzje

- **Publiczny keystore** — akceptowany kompromis (decyzja właściciela repo).
- **Jednorazowa migracja podpisu** — pierwsza wersja z nowym keystore wymaga
  odinstalowania starej appki. Utratę danych *ten jeden raz* może złagodzić
  Auto Backup (`android:allowBackup="true"` już jest w manifeście — Android 6+
  potrafi przywrócić SharedPreferences z kopii na koncie Google przy
  reinstalacji; niegwarantowane, zależy od włączonego backupu na telefonie).
  Warto wykonać migrację szybko, zanim danych przybędzie.
- **Dyscyplina wersjonowania** — pilnowana testem `ReleaseConsistencyTest`
  (porównanie `BuildConfig.VERSION_CODE` ↔ `wersja.json`), więc zapomnienie
  wywala build na CI.
- **Debug vs release** — do aktualizacji wystarczy stabilnie podpisany debug;
  przejście na build release to osobna, niezależna decyzja (nie blokuje nic
  z tego planu).

## Część 2 — Design (Faza 0 + Faza 1)

### 2.1 Podpisywanie

Nowy katalog `signing/` w repo:

```bash
keytool -genkeypair -keystore signing/mealspire.keystore \
    -alias mealspire -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass mealspire -keypass mealspire \
    -dname "CN=Mealspire, O=Sochiera"
```

Hasło `mealspire` jest **celowo jawne** (repo publiczne — patrz analiza).
W `app/build.gradle`:

```groovy
signingConfigs {
    mealspire {
        storeFile rootProject.file("signing/mealspire.keystore")
        storePassword "mealspire"
        keyAlias "mealspire"
        keyPassword "mealspire"
    }
}
buildTypes {
    debug   { signingConfig signingConfigs.mealspire }
    release { signingConfig signingConfigs.mealspire }
}
```

Od tej pory każdy build — CI, kontener agenta, laptop — daje APK
instalowalny po wierzchu poprzedniego.

### 2.2 Wersjonowanie i `dist/wersja.json`

- `versionCode` podbijany o 1 przy każdym odświeżeniu `dist/`; `versionName`
  wg uznania (np. `1.1`, `1.2`…). Pierwsze wydanie z nowym podpisem:
  `versionCode 2`, `versionName "1.1"`.
- Nowy plik `dist/wersja.json` (UTF-8, bez BOM):

```json
{
  "versionCode": 2,
  "versionName": "1.1",
  "apkUrl": "https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk"
}
```

Pole `sha256` dojdzie dopiero w Fazie 2 (w Fazie 1 nie mamy go jak
wykorzystać — pobiera przeglądarka).

- Nowy test `ReleaseConsistencyTest` (JVM, wzorowany na `BuildWorkflowTest` —
  ten sam trik z `repoRoot()`):
  - `dist/wersja.json` istnieje i parsuje się;
  - `wersja.json.versionCode == BuildConfig.VERSION_CODE` oraz zgodny
    `versionName` (BuildConfig jest widoczny w testach JVM — bez parsowania
    `build.gradle`);
  - `apkUrl` kończy się na `dist/mealspire-debug.apk` i zaczyna od
    `https://raw.githubusercontent.com/sochiera/mealspire/`;
  - `dist/mealspire-debug.apk` istnieje;
  - `signing/mealspire.keystore` istnieje, a `app/build.gradle` zawiera
    `signingConfig`.

Checklist wydania (trafi do `docs/steering/budowanie.md`):
podbij `versionCode`/`versionName` → `./gradlew test assembleDebug` →
skopiuj APK do `dist/` → zaktualizuj `dist/wersja.json`.

### 2.3 Nowe klasy — Faza 1

Zgodnie z architekturą: decyzje w `domain/` (czysty JVM, TDD), Android tylko
w `storage/`/`net/`/`MainActivity`.

| Klasa | Warstwa | Odpowiedzialność |
|---|---|---|
| `VersionInfo` | `domain/` | Niemutowalna wartość: `versionCode`, `versionName`, `apkUrl`. |
| `VersionInfoParser` | `domain/` | `parse(String json)` → `VersionInfo`; przy błędnym/niepełnym JSON-ie zwraca `null` (cichy fail — aktualizacja nie jest krytyczna). Używa `org.json` z platformy (w testach prawdziwe `org.json` już jest w zależnościach). |
| `UpdateChecker` | `domain/` | Czyste decyzje: `shouldCheck(lastCheckMillis, nowMillis)` (próg 24 h) i `isUpdateAvailable(installedVersionCode, VersionInfo)` (null-safe). Stała `CHECK_INTERVAL_MS = 24h`, stała `VERSION_URL`. |
| `UpdateStateStore` | `domain/` (interfejs) | `loadLastCheckMillis(long now)` — **pierwsze wywołanie zapisuje i zwraca `now`** (patrz „bezpieczeństwo testów"); `saveLastCheckMillis(long)`; `saveLatestKnown(VersionInfo)`; `loadLatestKnown()` → `VersionInfo` lub `null`. |
| `SharedPreferencesUpdateStateStore` | `storage/` | Implementacja na SharedPreferences (`update_state`). |
| `VersionJsonSource` | `domain/` (interfejs) | `String fetchJson() throws IOException` — abstrakcja transportu, żeby domena i testy nie znały HTTP. |
| `HttpVersionJsonFetcher` | `net/` | Implementacja: GET `UpdateChecker.VERSION_URL` przez `HttpURLConnection`, timeout 20 s, surowe body (bez `HtmlTextExtractor`!), limit rozmiaru ~64 KB. Wzorzec jak `HttpPageFetcher`. |

### 2.4 Integracja w `MainActivity`

- Pola: `updateStateStore`, seam testowy
  `static VersionJsonSource versionJsonSourceOverride` (pakietowy; `null`
  w produkcji → `HttpVersionJsonFetcher`).
- `maybeCheckForUpdate()` wołane raz w `onCreate` **tylko gdy**
  `appSettings.isOnboardingDone()` (świeża instalacja z definicji ma najnowszą
  wersję, a quiz nie potrzebuje konkurencji o uwagę):
  1. `lastCheck = updateStateStore.loadLastCheckMillis(now)`;
  2. jeśli `!updateChecker.shouldCheck(lastCheck, now)` → koniec;
  3. w tle (`new Thread`, jak pozostałe wywołania sieciowe): fetch → parse →
     `saveLatestKnown` + `saveLastCheckMillis(now)` (czas próby zapisujemy
     **także przy błędzie** — max jedna próba na dobę, bez młócenia przy
     braku sieci);
  4. `runOnUiThread`: jeśli `currentScreen == START` i epoka niezmieniona →
     odśwież baner.
- **Baner**: budowany przez `showStartScreen()` jako pierwszy element
  `contentContainer`, tylko gdy
  `updateChecker.isUpdateAvailable(BuildConfig.VERSION_CODE, updateStateStore.loadLatestKnown())`.
  Dzięki temu zero nowych ścieżek widoczności — baner żyje i umiera razem
  z ekranem startowym, respektując `contentEpoch`. Tekst:
  „Dostępna nowa wersja {versionName} — dotknij, aby pobrać".
- Klik banera: `ACTION_VIEW` z `apkUrl`; `ActivityNotFoundException` → toast.

### 2.5 Bezpieczeństwo testów (żadnych wywołań sieciowych)

Dwie niezależne zapory:

1. `loadLastCheckMillis(now)` przy pierwszym wywołaniu zapisuje `now` jako
   „ostatnie sprawdzenie". Efekt: świeża instalacja (a więc i **każdy test
   Robolectric na czystych SharedPreferences**) nie sprawdza sieci przez
   pierwsze 24 h realnego czasu — istniejące testy nie wymagają żadnych zmian.
2. Testy, które ćwiczą sam mechanizm, wstrzykują fałszywe
   `versionJsonSourceOverride`; jeden test strażniczy ustawia źródło rzucające
   `AssertionError` i sprawdza, że świeży start go nie dotyka.

### 2.6 Testy (TDD, piszemy przed implementacją)

- `VersionInfoParserTest` (JVM): poprawny JSON; brakujące pola → `null`;
  śmieci/pusty string → `null`; nadmiarowe pola ignorowane.
- `UpdateCheckerTest` (JVM): progi 24 h (przed/po/równo); `isUpdateAvailable`
  dla wyższego/równego/niższego `versionCode` i `null`-owego `VersionInfo`.
- `ReleaseConsistencyTest` (JVM): jak w 2.2.
- `UpdateStateStoreRobolectricTest`: zapis/odczyt; semantyka pierwszego
  `loadLastCheckMillis`; round-trip `VersionInfo`.
- `UpdateBannerRobolectricTest`: (a) zasiany store z nowszą wersją → baner na
  ekranie startowym, klik → `ACTION_VIEW` z właściwym URL-em (shadow intent);
  (b) wersja równa zainstalowanej → brak banera; (c) świeży start nie dotyka
  `VersionJsonSource` (strażnik z 2.5); (d) fałszywe źródło z nowszą wersją +
  wyzerowany `lastCheck` → po sprawdzeniu baner się pojawia.

## Część 3 — Taski

Kolejność jest zależnościowa — T1→T2 to fundament, T3–T5 można robić
równolegle, T6 wymaga T3–T5, T7 zamyka całość. Każdy task kończy się zielonym
`./gradlew test assembleDebug`; commit po polsku z prefiksem.

- [ ] **T1 — Stabilny podpis APK** (Faza 0)
  Wygeneruj `signing/mealspire.keystore` (komenda w 2.1), dodaj
  `signingConfigs` + przypięcie do `debug`/`release` w `app/build.gradle`.
  Dopisz do `docs/steering/projekt.md` decyzję o jawnym keystore.
  DoD: dwa kolejne buildy (np. lokalny i po `clean`) dają APK z tym samym
  podpisem (`apksigner verify --print-certs` lub `keytool -printcert-jarfile`).

- [ ] **T2 — Wersjonowanie + `dist/wersja.json` + test spójności** (Faza 0)
  Podbij na `versionCode 2` / `versionName "1.1"`. Utwórz `dist/wersja.json`
  (format w 2.2). Napisz `ReleaseConsistencyTest` (najpierw testy — powinny
  być czerwone przed utworzeniem JSON-a). Odśwież `dist/mealspire-debug.apk`
  nowo podpisanym buildem. Dopisz checklist wydania do
  `docs/steering/budowanie.md` + notkę o jednorazowej reinstalacji do README.
  DoD: test czerwony przy rozjeździe wersji, zielony po; `dist/` spójne.

- [ ] **T3 — `VersionInfo` + `VersionInfoParser`** (Faza 1, czysty JVM)
  TDD wg 2.6. DoD: pełne pokrycie przypadków brzegowych parsera.

- [ ] **T4 — `UpdateChecker`** (Faza 1, czysty JVM)
  TDD wg 2.6; stałe `CHECK_INTERVAL_MS` i `VERSION_URL` tutaj.
  DoD: decyzje czasowe i wersjowe w 100 % pokryte testami.

- [ ] **T5 — `UpdateStateStore` + implementacja SharedPreferences** (Faza 1)
  Interfejs w `domain/`, implementacja w `storage/`, test Robolectric.
  Kluczowa jest semantyka „pierwszy odczyt zapisuje teraz" (zapora 2.5).
  DoD: round-trip + semantyka pierwszego odczytu przetestowane.

- [ ] **T6 — Integracja w `MainActivity`: sprawdzanie + baner** (Faza 1)
  `VersionJsonSource` + `HttpVersionJsonFetcher`, seam testowy,
  `maybeCheckForUpdate()`, baner w `showStartScreen()`, klik → `ACTION_VIEW`.
  Testy `UpdateBannerRobolectricTest` wg 2.6 (w tym strażnik antysieciowy).
  DoD: wszystkie scenariusze (a)–(d) zielone; ręczny smoke na telefonie:
  stara wersja + podbite `wersja.json` na mainie → baner → pobranie →
  instalacja po wierzchu **bez utraty danych**.

- [ ] **T7 — Dokumentacja i wydanie** (Faza 1)
  README (opis funkcji dla użytkownika), `docs/steering/projekt.md`
  (mechanizm w skrócie), odśwież `dist/` + `wersja.json` finalnym buildem.
  DoD: `ReleaseConsistencyTest` zielony, dokumenty spójne ze stanem kodu.

- [ ] **(Backlog) T8 — Faza 2: pobieranie w aplikacji + `PackageInstaller`**
  Dopiero po zebraniu doświadczeń z Fazy 1. Zakres opisany w Poziomie B;
  wymaga `REQUEST_INSTALL_PACKAGES`, pola `sha256` w `wersja.json`
  i decyzji, czy obsługujemy globalny przełącznik na Androidzie 6–7.
