# Analiza: mechanizm aktualizacji aplikacji

Stan: analiza wykonalności (nic jeszcze nie zaimplementowane).
Data: 2026-07-05.

## Cel

Użytkownik ma zainstalowaną Mealspire z sideloadu (`dist/mealspire-debug.apk`).
Chcemy, żeby mógł dostać nowszą wersję **bez utraty danych** (profil domowników,
dziennik gustu, książka kucharska, klucz API w SharedPreferences) i najlepiej
bez ręcznego szukania pliku APK.

## Stan obecny — co mamy

- Dystrybucja: debug APK w repo (`dist/mealspire-debug.apk`, repo **publiczne**)
  oraz artefakt CI `mealspire-debug-apk` z każdego pusha.
- `versionCode 1`, `versionName "1.0"` — **nigdy nie podbijane**. Android nie
  odróżnia „nowszej" wersji, a my nie wiemy, co użytkownik ma zainstalowane.
- Brak keystore w repo i brak `signingConfig` — każdy build debug jest
  podpisywany **lokalnym** `~/.android/debug.keystore` środowiska, w którym
  powstał (runner CI, kontener Claude, laptop). Każde środowisko generuje
  własny klucz.
- Warstwa sieciowa: goły `HttpURLConnection` (`net/HttpClaudeClient`,
  `net/HttpPageFetcher`) — wzorzec łatwy do powielenia dla pobierania
  metadanych wersji.
- Ograniczenia projektowe: Java, **zero bibliotek third-party w kodzie
  produkcyjnym** (także androidx!), minSdk 23 / targetSdk 35, logika w
  `domain/` rozwijana w TDD.

## Bloker nr 1: niestabilny podpis APK

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
| **Keystore zacommitowany do repo** (hasło jawne lub w `gradle.properties`) | Zero konfiguracji; każdy build (CI, kontener agenta) podpisuje tak samo | Repo jest publiczne → każdy może podpisać APK „udający" Mealspire. Dla appki hobbystycznej bez Play Store ryzyko realnie niskie (atakujący i tak musiałby nakłonić użytkownika do sideloadu) |
| Keystore w sekrecie CI (base64 w GitHub Secrets), lokalnie przez `local.properties` | Klucz prywatny nie wycieka | APK z sesji agentowych (odświeżanie `dist/`) miałby inny podpis niż z CI — wraca problem. Wymagałoby przeniesienia odświeżania `dist/` w całości do CI |

Rekomendacja: **keystore w repo**, analogicznie do już przyjętego kompromisu
z zaszyfrowanym kluczem API. To świadoma decyzja bezpieczeństwa — odnotować
w steeringu. (Uwaga: istniejące instalacje podpisane starym kluczem debug
i tak wymagają **jednorazowej** reinstalacji po zmianie podpisu.)

## Bloker nr 2: wersjonowanie

- `versionCode` musi rosnąć przy każdym wydaniu. Najprościej: podbijać ręcznie
  w `app/build.gradle` przy zmianach trafiających do `dist/` (wpisać do
  steeringu jako regułę), albo wyliczać automatycznie z gita
  (`git rev-list --count HEAD`) — automat jest kuszący, ale build musi wtedy
  zawsze mieć dostęp do `.git` i pełnej historii (CI robi płytki checkout —
  trzeba `fetch-depth: 0`).
- Do sprawdzania „czy jest nowsza wersja" potrzebne jest **źródło prawdy
  o najnowszej wersji** dostępne po HTTP — patrz niżej.

## Skąd aplikacja ma wiedzieć o nowej wersji i skąd ją pobrać

Repo jest publiczne, więc bez żadnego tokenu działają:

1. **Plik metadanych w repo** — np. `dist/wersja.json`
   (`{"versionCode": 7, "versionName": "1.6", "apkUrl": "...", "sha256": "..."}`)
   czytany z `https://raw.githubusercontent.com/sochiera/mealspire/main/dist/wersja.json`,
   APK z `https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk`.
   - Plusy: zero dodatkowej infrastruktury, spójne z obecnym workflow
     („odśwież `dist/`" rozszerza się o „odśwież `wersja.json`").
   - Minusy: łatwo zapomnieć o aktualizacji JSON-a ręcznie (można pilnować
     testem à la `BuildWorkflowTest`); raw.githubusercontent ma cache ~5 min.
2. **GitHub Releases** — CI publikuje release z APK przy tagu; aplikacja pyta
   `https://api.github.com/repos/sochiera/mealspire/releases/latest`.
   - Plusy: wersje niemutowalne, naturalne miejsce na changelog, API zwraca
     wszystko (tag = wersja, URL assetu).
   - Minusy: nowy workflow wydawniczy (tagowanie), API GitHuba ma limit
     60 zapytań/h/IP bez tokenu (dla jednej appki sprawdzającej raz dziennie —
     bez znaczenia), parsowanie większego JSON-a.

Rekomendacja: zacząć od **wariantu 1** (najmniej ruchomych części, pasuje do
obecnego procesu), z opcją migracji na Releases, gdy proces wydawniczy dojrzeje.

## Jak dostarczyć aktualizację użytkownikowi — trzy poziomy ambicji

### Poziom A — „powiadom i otwórz przeglądarkę" (rekomendowany start)

Aplikacja raz na dobę (przy starcie, w tle, z timeoutem, wynik cache'owany
w SharedPreferences) pobiera `wersja.json`, porównuje z
`BuildConfig.VERSION_CODE` i jeśli jest nowsza wersja, pokazuje nieinwazyjny
element UI („Dostępna wersja 1.6 — pobierz"). Kliknięcie odpala
`Intent.ACTION_VIEW` z URL-em APK — przeglądarka pobiera plik, użytkownik
klika w pobrany plik i system przeprowadza instalację po wierzchu.

- Nowe uprawnienia: **żadne** (INTERNET już jest; „nieznane źródła" użytkownik
  ma już włączone dla przeglądarki/plików od pierwszej instalacji).
- Nowy kod: `domain/UpdateChecker` (porównanie wersji, decyzja „pokaż/nie
  pokazuj", czysty JVM — pełne TDD), `domain/VersionInfoParser` (JSON),
  `net/HttpVersionFetcher` (kopia wzorca `HttpPageFetcher`), drobny fragment
  UI w `MainActivity`. Szacunkowo ~150–250 linii + testy.
- Ryzyka: minimalne. Brak sieci / błąd HTTP → cicho nic nie pokazujemy
  (aktualizacja to nie funkcja krytyczna). Żadnych wywołań w testach
  Robolectric — ścieżka offline jak przy AI.

### Poziom B — pobieranie w aplikacji + systemowy instalator

Jak A, ale aplikacja sama pobiera APK (do `getExternalFilesDir`/`cacheDir`)
i uruchamia instalację.

Wymagania techniczne (tu robi się grubiej):

- **`REQUEST_INSTALL_PACKAGES`** w manifeście; na Androidzie 8+ użytkownik
  musi jednorazowo dać zgodę „instalowanie nieznanych aplikacji" **dla
  Mealspire** (przekierowanie przez `ACTION_MANAGE_UNKNOWN_APP_SOURCES`).
- Udostępnienie pliku instalatorowi: na targetSdk 35 `file://` rzuca
  `FileUriExposedException`. Standardowe wyjście to `FileProvider`, ale on
  jest z **androidx — zakazane**. Alternatywy zgodne z regułami projektu:
  - własny minimalny `ContentProvider` z `openFile()` serwujący pobrany APK
    (czysta platforma, ~50 linii), albo
  - **`PackageInstaller`** (API 21+, czyli w całości w naszym minSdk 23):
    strumieniujemy APK do sesji instalacyjnej, bez wystawiania URI w ogóle.
    To ścieżka „kanoniczna" i najczystsza.
- Weryfikacja pobrania: porównanie SHA-256 z `wersja.json` przed instalacją
  (HTTPS chroni transport, hash chroni przed uciętym plikiem/cache).
- Szacunkowo ~300–500 linii + testy; UI zgody, obsługa błędów pobierania,
  sprzątanie starych plików.

### Poziom C — pełny automat (cicha instalacja w tle)

Niemożliwy bez uprawnień systemowych/device-owner. Nie rozważamy.

## Rekomendowany plan wdrożenia

1. **Faza 0 (fundament, bez niej nic nie działa):**
   - keystore zacommitowany do repo + `signingConfig` dla debug (i release),
   - reguła podbijania `versionCode`/`versionName` przy każdym odświeżeniu
     `dist/` (wpis w `docs/steering/budowanie.md` + test pilnujący spójności
     `wersja.json` ↔ `build.gradle`),
   - `dist/wersja.json` jako źródło prawdy,
   - komunikat dla użytkownika: jednorazowa reinstalacja (stary podpis debug).
2. **Faza 1 = Poziom A:** sprawdzanie wersji + baner + otwarcie przeglądarki.
   Mały, bezpieczny krok, który daje 90 % wartości.
3. **Faza 2 = Poziom B (opcjonalnie, po sprawdzeniu Fazy 1):** pobieranie
   w aplikacji + `PackageInstaller`. Dopiero gdy klikanie przez przeglądarkę
   okaże się uciążliwe.

## Ryzyka i otwarte pytania

- **Publiczny keystore** — akceptowalny kompromis dla appki rodzinnej poza
  Play Store, ale decyzja należy do właściciela repo.
- **Jednorazowa migracja podpisu** — pierwsza wersja z nowym keystore wymaga
  odinstalowania starej appki (utrata danych *ten jeden raz*). Warto zrobić to
  szybko, zanim danych przybędzie; ewentualnie dopisać eksport/import danych
  w „Zarządzaj moimi danymi" jako koło ratunkowe.
- **Dyscyplina wersjonowania** — ręczne podbijanie łatwo pominąć; test
  w stylu `BuildWorkflowTest` (porównanie `versionCode` z `wersja.json`)
  redukuje ryzyko prawie do zera.
- **Cache raw.githubusercontent (~5 min)** — bez znaczenia przy sprawdzaniu
  raz dziennie.
- Czy chcemy przejść z debug na podpisany build **release**? Nie jest
  wymagane do aktualizacji, ale naturalnie wypada rozważyć przy okazji Fazy 0.
