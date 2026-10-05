# Analiza projektu Mealspire (stan: `main` @ 2281e39)

Data analizy: 2026-10-04 · Zapis w repo: 2026-10-05

Analizowany stan: `main` @ `2281e39` (merge PR #15, 2026-07-05).
Analiza dotyczy świeżego klonu publicznego repo. Wcześniejsza wersja opisywała
nieaktualną lokalną kopię `3dd1db0` i została zastąpiona tą korektą.
Dokument jest wyłącznie analizą: nie zmienia kodu.

**Oznaczenia dowodów.**
- Wyniki uruchomień (testy, build, lint, skany) **pochodzą z raportu runu
  analitycznego z 2026-10-04**. Przy zapisie tego dokumentu nie były
  powtarzane, a od tego czasu `main` się nie zmienił.
- W tabeli ryzyk i w sekcji 11 rozróżniono ustalenia potwierdzone, wnioski
  oraz rzeczy niezweryfikowane.

## 1. Repozytorium i proces

- Repo publiczne, licencja PolyForm Noncommercial 1.0.0.
- 79 commitów. Wszystkie 15 PR-ów zmergowane, otwartych brak. Ostatnia
  aktywność to 2026-07-05.
- Z gałęzi `claude/*` tylko `claude/ai-agent-prompt-79y3cy` ma niezmergowany
  commit (sam dokument promptu).
- Kod tworzą agenci w TDD, przez PR-y z review. Opierają się na `CLAUDE.md`,
  `docs/steering/*` i `docs/design/*`.
- CI (GitHub Actions) na `main` zielone.

## 2. Produkt i funkcjonalności (README, steering, kod)

Natywna aplikacja Android (PL). Jedno dotknięcie (Śniadanie/Obiad/Kolacja) daje
3 krótkie propozycje dań; pełny przepis powstaje na żądanie.

- **Onboarding:** quiz przy pierwszym uruchomieniu daje `HouseholdProfile`.
  Pyta o domowników, wykluczenia diety, czas i umiejętności, a na koniec
  przeprowadza 3 rundy kontrastowych dań.
- **Offline najpierw:**
  - ok. 60 wbudowanych dań (`BuiltInRecipes`);
  - Claude przejmuje propozycje po ≥5 polubieniach
    (`PersonalizationReadiness`).
- **Agent gustu** (`docs/design/agent-gustu.md`, etapy 1–7):
  - dziennik zdarzeń z kompaktowaniem;
  - model wymiarowy z wygaszaniem (półokres 60 dni);
  - reguła 2+1: trzecia propozycja jest eksploracyjna;
  - wykrywanie monotonii i ciche negatywy;
  - zero telemetrii.
- **Dieta jako twardy wymóg:** egzekwowana w prompcie, w filtrze puli
  offline i w walidacji odpowiedzi AI (`ProposalValidator`).
- Pozostałe funkcje:
  - „Zmień przepis”;
  - import dania z linku lub opisu;
  - liczba osób pytana raz;
  - zarządzanie danymi.
- **Powiadomienia** o 8/12/18, liczone offline (AlarmManager inexact,
  `BootReceiver`).
- **Klucz API:**
  - zaszyfrowany (AES-GCM + PBKDF2) w `res/values/secrets.xml`;
  - odblokowywany hasłem raz, potem trzymany w
    `SharedPreferencesSecretStore`;
  - alternatywnie wstrzykiwany przy buildzie.
- **Aktualizacje:** raz na dobę aplikacja czyta `dist/wersja.json`. Gdy jest
  nowsza wersja, pokazuje baner z linkiem do APK.
- **Dystrybucja:** debug APK w `dist/mealspire-debug.apk` (wersja 1.1).

## 3. Architektura

- Java 17, AGP 8.7.3, Gradle 8.9, compileSdk/targetSdk 35, minSdk 23.
- Jeden moduł, **zero zależności runtime** (HttpURLConnection, org.json).
- Pakiety: `domain/` (84 pliki), `storage/`, `net/`, `notify/`, `MainActivity`.
  Kod produkcyjny ma ok. 8 tys. linii.
- Domena jest dobrze rozbita na małe, czyste klasy z portami do warstwy
  Androida.
- **`MainActivity.java` ma 1825 linii**, mimo że steering deklaruje „cienką”
  Activity. Mieści cały UI programowy, nawigację, quiz, dialogi i 6×
  `new Thread`.
- Spóźnione odpowiedzi odcina ręczny `contentEpoch`. Obrót ekranu obsługuje
  `configChanges`, nie ViewModel.

## 4. Jakość i testy (raportowane z runu 2026-10-04)

Polecenie: `./gradlew test assembleDebug lintDebug`, JDK 17, build bez
`local.properties`.

- **Testy:** 87 zestawów, 400 testów, 0 błędów, 1 skip. Pominięty jest
  `EncryptedApiKeyTest.unlocksWithSuppliedPassword`, który celowo wymaga
  hasła spoza repo.
- **assembleDebug:** OK.
- **lintDebug: FAIL, 6 błędów `NewApi`.**
  - Chodzi o `List#sort`, `Comparator#comparingLong`, `Map#merge` i
    `Integer::sum`.
  - Miejsca: `ExplorationPlanner.java:92`, `RecentlyShownFilter.java:39`,
    `TasteProfiler.java:65,74`.
  - To API 24+, a minSdk to 23 i nie ma desugaringu.
  - Lint zgłosił też ostrzeżenia `DefaultLocale` (28) i `GradleDependency`
    (3).
- **Wniosek:** ścieżki propozycji i gustu prawdopodobnie rzucą
  `NoSuchMethodError` na Androidzie 6. Nie sprawdzono tego na urządzeniu.
- CI uruchamia tylko `test` i `assembleDebug`, bez lintu. Dlatego regresja
  przeszła.
- Plusy: testy obrotu ekranu (`RotationSafetyTest`), spójności wydania
  (`ReleaseConsistencyTest`) i kompaktowania dziennika. Testy nie wołają
  sieci.

## 5. Ścieżki UX (z kodu i README; nie uruchamiano na urządzeniu)

- **Pierwsze uruchomienie:** quiz → liczba osób → hasło do klucza → zgoda na
  powiadomienia. To do 4 interakcji przed pierwszą wartością.
- **Główna ścieżka:** pora dnia → 3 propozycje → „Pokaż przepis” (z cache) →
  „Zmień przepis”/polubienie. Systemowe „Cofnij” jest obsłużone.
- Bez klucza aplikacja nadal działa: zostaje pula offline i powiadomienia.
- Aktualizacja wymaga ręcznej instalacji APK spoza sklepu.

## 6. Bezpieczeństwo i prywatność

- **Jawnego klucza nie ma w historii git.** Wystąpienia `sk-ant-` to fikcyjne
  wartości w testach.
- **Jawnego klucza nie ma też w bieżącym `dist/mealspire-debug.apk`.** Starszych
  APK z historii nie sprawdzono.
- **Zaszyfrowany klucz leży w publicznym repo i APK.** PBKDF2 (120 000
  iteracji) i AES-GCM są poprawne, ale każdy może prowadzić offline
  brute-force hasła. Ochrona zależy wyłącznie od siły hasła, której nie
  oceniano.
- **Po odblokowaniu klucz leży jawnym tekstem w SharedPreferences.** Ryzyko
  zwiększają:
  - `allowBackup="true"` bez reguł wykluczeń;
  - dystrybucja buildu debug (`run-as` przy fizycznym dostępie).
- **Publiczny keystore z jawnym hasłem** (`signing/`). Każdy może podpisać APK,
  który zainstaluje się jako aktualizacja i przejmie dane aplikacji. To
  świadomy, udokumentowany kompromis
  (`docs/design/aktualizacja-aplikacji.md`), ale ryzyko rośnie, bo w danych
  leży klucz API.
- **Kanał aktualizacji** ufa kontu GitHub. `VersionInfoParser` wymaga
  `https://`, ale nie przypina domeny.
- **Wywołania Claude idą prosto z telefonu**, bez proxy i limitów. Profil
  domowników trafia do Anthropic, a aplikacja nie ma polityki prywatności.
- **`HttpPageFetcher`** czyta całą odpowiedź przed obcięciem do 6000 znaków.
  Treść obcej strony trafia do promptu (prompt injection o lokalnych
  skutkach).

## 7. Utrzymanie

- Dokumentacja bardzo dobra: README, `CLAUDE.md`, steeringi i designy z
  ryzykami.
- Wydanie jest ręczne i pilnowane testem spójności.
- APK w git rozdmuchuje historię (ok. 20 wersji).
- Model `claude-sonnet-4-6` i `MAX_TOKENS=2048` są zahardkodowane. Brak
  prompt cachingu.

## 8. Mocne strony

1. Przemyślany produkt: personalizacja z eksploracją, twarde wykluczenia
   diety, offline-first, zero telemetrii.
2. Czysta domena i bardzo dobre pokrycie testami.
3. Zero zależności runtime: mały APK i mała powierzchnia supply-chain.
4. Wzorowa dokumentacja decyzji, działające CI, proces PR i review.
5. Sekret nie wycieka jawnie do git ani do bieżącego APK.

## 9. Ryzyka (priorytet)

| # | Ryzyko/brak | Waga | Status dowodu |
|---|---|---|---|
| R1 | `NewApi` (API 24) przy minSdk 23: prawdopodobne crashe na Androidzie 6 | Wysoka | lint potwierdzony; crash wnioskowany |
| R2 | Zaszyfrowany klucz w publicznym repo (offline brute-force), jawny klucz w SharedPreferences, `allowBackup`, build debug | Wysoka | potwierdzone w kodzie/manifeście |
| R3 | Publiczny keystore + aktualizacje: możliwe podszycie się i przejęcie danych/klucza | Średnia (świadomy kompromis) | potwierdzone, udokumentowane |
| R4 | CI bez lintu | Średnia | potwierdzone |
| R5 | `MainActivity` 1825 linii, ręczne wątki | Średnia | potwierdzone |
| R6 | API bez proxy i limitów; brak polityki prywatności | Średnia | potwierdzone |
| R7 | `HttpPageFetcher` bez limitu bajtów; prompt injection z importu | Niska–średnia | potwierdzone w kodzie |
| R8 | APK w historii git; ręczny proces wydania | Niska | potwierdzone |

## 10. Rekomendowane kroki

1. **Naprawić `NewApi`.** Użyć `Collections.sort` i ręcznego `merge` albo
   włączyć desugaring. **Dodać `lintDebug` do CI** jako bramkę.
2. **Klucz API:**
   - trzymać odszyfrowany klucz zabezpieczony Android Keystore;
   - dodać `dataExtractionRules`/`fullBackupContent` wykluczające sekrety;
   - rozważyć rotację, jeśli hasło jest słabe;
   - docelowo przenieść wywołania za proxy z limitem.
3. **Release zamiast debug APK** (R8, `debuggable=false`). Rozważyć prywatny
   keystore albo GitHub Releases. Prywatny keystore świadomie zrywa ciągłość
   aktualizacji.
4. **Podzielić `MainActivity`** na kontrolery ekranów. Zastąpić `new Thread`
   executorem z anulowaniem.
5. **Ograniczyć import stron:** limit bajtów w `HttpPageFetcher` i oznaczanie
   treści strony jako niezaufanej.
6. **Wywołania Claude:**
   - structured output (JSON) dla propozycji i przepisów;
   - retry na 429/529;
   - prompt caching;
   - model w konfiguracji.
7. Polityka prywatności w aplikacji.
8. **Rozwój produktu:**
   - plan tygodniowy z listą zakupów;
   - „co mam w lodówce”;
   - synchronizacja domowników;
   - faza 2 aktualizacji;
   - audyt dostępności.

## 11. Potwierdzone vs wnioski vs niezweryfikowane

- **Potwierdzone (run 2026-10-04):**
  - stan repo, PR-ów i CI;
  - 400 testów (1 celowy skip), `assembleDebug` OK, 6 błędów lint `NewApi`;
  - brak jawnego klucza w historii git i w bieżącym APK;
  - konfiguracja keystore, backupu i przechowywania klucza;
  - rozmiar `MainActivity`.
- **Wnioski:**
  - crash na API 23;
  - siła ochrony zaszyfrowanego klucza (zależy od nieznanego hasła);
  - realność podszycia się pod aktualizację.
- **Niezweryfikowane:**
  - testy Espresso i zachowanie na urządzeniu;
  - dostępność;
  - realne wywołania API (celowo pominięte);
  - jawny klucz w starszych APK z historii `dist/`.
