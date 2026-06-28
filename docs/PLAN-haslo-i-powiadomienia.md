# Plan: zapamiętane hasło + codzienne powiadomienia z propozycjami

Dokument planistyczny dla dwóch zmian zgłoszonych przez użytkownika:

1. **Zapamiętane hasło** — po pierwszym poprawnym podaniu hasła aplikacja nie pyta
   o nie ponownie przy kolejnych uruchomieniach.
2. **Codzienne powiadomienia** — o **8:00**, **12:00** i **18:00** aplikacja sama
   wysyła powiadomienie z propozycjami odpowiednio na **śniadanie**, **obiad** i
   **kolację**.

Styl pracy: TDD małymi krokami, logika domenowa w czystej Javie (szybkie testy
JVM), warstwa Androida testowana Robolectrikiem tam, gdzie to sensowne.

---

## Część 1 — Zapamiętane hasło

### Stan obecny

`MainActivity.onCreate` przy każdym uruchomieniu, jeśli nie ma klucza wbudowanego
(`BuildConfig.ANTHROPIC_API_KEY`) a jest klucz zaszyfrowany w zasobach
(`R.string.encrypted_api_key`), woła `promptForApiKeyPassword`. Po udanym
odszyfrowaniu klucz trafia **tylko do pamięci** (`buildClaudeClients(key)`) i nie
jest nigdzie zapisywany — więc następny start znów pyta o hasło.

### Cel

Po pierwszym poprawnym haśle zapamiętać odblokowany klucz w pamięci aplikacji,
tak by kolejne uruchomienia od razu miały dostęp do AI bez pytania o hasło.
Użytkownik zachowuje kontrolę: może klucz „zapomnieć" (zablokować AI ponownie).

### Podejście

Nowa abstrakcja `SecretStore` (domena) + implementacja
`SharedPreferencesSecretStore` (warstwa `storage`), analogicznie do istniejącego
`AppSettings`:

```
interface SecretStore {
    String loadApiKey();   // "" gdy brak
    boolean hasApiKey();
    void saveApiKey(String key);
    void clear();
}
```

Przepływ wyboru źródła klucza w `onCreate` (priorytety):

1. `BuildConfig.ANTHROPIC_API_KEY` niepuste → użyj (bez pytania). *(stan obecny)*
2. inaczej, jeśli `secretStore.hasApiKey()` → użyj zapamiętanego klucza (bez
   pytania). *(nowość)*
3. inaczej, jeśli jest klucz zaszyfrowany w zasobach → zapytaj o hasło. *(stan
   obecny)* Po udanym odszyfrowaniu **zapisz** klucz w `SecretStore`.

W menu „Zarządzaj moimi danymi" dodajemy opcję **„Zablokuj AI (zapomnij
hasło)"**, która woła `secretStore.clear()` i przywraca tryb offline — pełna
kontrola nad tym, co aplikacja pamięta (spójne z resztą zarządzania danymi).

### Bezpieczeństwo (świadomy kompromis)

Odblokowany klucz zapisujemy w **prywatnych** preferencjach aplikacji
(`MODE_PRIVATE`) jako tekst. To **ten sam, umiarkowany** model zagrożeń, który
README już opisuje: „kto ma aplikację **i** hasło, odzyska klucz". Pliki
prywatne aplikacji są chronione piaskownicą systemu (inne aplikacje ich nie
czytają). Szyfrowanie hasłem w repo chroni klucz w **repozytorium**; po
odblokowaniu na urządzeniu i tak żyje w pamięci procesu.

Twardsze opcje (np. `EncryptedSharedPreferences` z kluczem w Android Keystore)
są możliwe jako późniejsze wzmocnienie, ale dokładają zależność i utrudniają
szybkie testy JVM/Robolectric. Świadomie zostajemy przy prostym, testowalnym
rozwiązaniu spójnym z obecną postawą bezpieczeństwa; kompromis udokumentujemy w
README.

### Komponenty

- `domain/SecretStore.java` *(nowy, interfejs)*
- `storage/SharedPreferencesSecretStore.java` *(nowy)*
- `MainActivity` *(zmiana: użycie SecretStore, pominięcie pytania, zapis po
  odblokowaniu, opcja „zapomnij")*

### Testy (TDD)

- `SecretStoreRobolectricTest`: zapis→odczyt między instancjami, `hasApiKey`,
  `clear`, domyślnie pusto.
- `RememberPasswordRobolectricTest` (UI): gdy `SecretStore` ma zapamiętany klucz,
  na starcie **nie** pojawia się dialog hasła; gdy nie ma klucza ani wbudowanego,
  a jest zaszyfrowany — dialog się pojawia. (Dialog identyfikujemy po tytule, jak
  w `RememberServingsRobolectricTest`.)

---

## Część 2 — Codzienne powiadomienia (8 / 12 / 18)

### Cel

Codziennie o 8:00, 12:00 i 18:00 wysłać powiadomienie z kilkoma propozycjami dań
odpowiednio na śniadanie, obiad i kolację. Dotknięcie powiadomienia otwiera
aplikację z już wybraną porą dnia.

### Decyzje projektowe

- **Źródło propozycji: tryb offline.** Powiadomienie wyzwala się w tle (także bez
  internetu i bez klucza API). Generowanie przez AI w odbiorniku w tle jest
  zawodne (sieć, czas, brak klucza). Używamy tej samej, sprawdzonej ścieżki co
  offline na ekranie: `MealPoolBuilder` + `VariedMealPicker` na wbudowanej puli i
  bazie użytkownika. Dzięki temu powiadomienia działają zawsze.
- **Harmonogram: `AlarmManager` + `BroadcastReceiver`.** Lekkie, bez nowych
  zależności (brak WorkManagera w projekcie). Trzy dzienne alarmy, każdy z osobnym
  `requestCode` i porą dnia w extrach.
- **`setInexactRepeating(RTC_WAKEUP, …, INTERVAL_DAY)`** zakotwiczone na najbliższym
  wystąpieniu danej godziny. Nie wymaga uprawnienia `SCHEDULE_EXACT_ALARM` (API 31+
  mocno je ogranicza). Kompromis: system może dostarczyć alarm z lekkim
  opóźnieniem (minuty) — dla przypomnień o posiłkach to akceptowalne.
- **Przeładowanie po restarcie telefonu:** alarmy giną przy reboocie, więc
  `BootReceiver` na `BOOT_COMPLETED` planuje je ponownie.
- **Uprawnienia:** `POST_NOTIFICATIONS` (API 33+, prośba w czasie działania),
  `RECEIVE_BOOT_COMPLETED`. Kanał powiadomień tworzony na API 26+.

### Logika domenowa (czysta Java, TDD)

- `MealSlot` — trzy pory dnia z godziną, etykietą i indeksem posiłku:
  `BREAKFAST(8, "Śniadanie", 0)`, `LUNCH(12, "Obiad", 1)`, `DINNER(18, "Kolacja", 2)`.
- `NextAlarmTime` — `long nextOccurrence(long nowMillis, int hour, TimeZone tz)`:
  najbliższe wystąpienie danej godziny (dziś, jeśli jeszcze nie minęła; inaczej
  jutro), minuty/sekundy = 0. Czysta funkcja — testowalna na stałej strefie.
- `MealNotificationContent` — z etykiety posiłku i listy nazw propozycji buduje
  tytuł i treść powiadomienia, np. tytuł „Śniadanie — pomysły na dziś", treść
  „Owsianka z jabłkiem · Jajecznica · Tost z awokado". Obsługa pustej listy.

### Refactor wspólnej puli

Wbudowane przepisy `RECIPES[][]` są dziś prywatne w `MainActivity`. Wyciągamy je
do `domain/BuiltInRecipes` (zwraca przepisy dla danego indeksu posiłku), by
współdzieliły je `MainActivity` i odbiornik powiadomień. Czyste, testowalne.

### Warstwa Androida (glue)

- `notify/MealReminderScheduler` — planuje 3 dzienne alarmy (PendingIntent →
  `MealReminderReceiver`, slot w extrach). Idempotentne.
- `notify/MealReminderReceiver` (`BroadcastReceiver`) — na alarmie: wczytaj
  preferencje i bazę, zbuduj propozycje offline dla slotu, złóż treść
  (`MealNotificationContent`), wyślij powiadomienie. PendingIntent dotknięcia →
  `MainActivity` z extrą `meal_index`.
- `notify/BootReceiver` (`BroadcastReceiver`) — na `BOOT_COMPLETED` przeplanuj.
- `notify/MealNotifications` — pomocnik: tworzenie kanału + wysyłanie
  powiadomienia.
- `MainActivity` — na starcie: utwórz kanał, zaplanuj przypomnienia
  (idempotentnie), na API 33+ poproś o `POST_NOTIFICATIONS`; obsłuż wejściową
  extrę `meal_index` (auto-wybór posiłku).
- `AndroidManifest.xml` — uprawnienia + rejestracja odbiorników.

### Testy (TDD)

- `MealSlotTest`, `NextAlarmTimeTest`, `MealNotificationContentTest`,
  `BuiltInRecipesTest` — czyste testy JVM.
- `MealReminderSchedulerRobolectricTest` — po zaplanowaniu w `ShadowAlarmManager`
  są **3** alarmy.
- `MealReminderReceiverRobolectricTest` — po `onReceive` w
  `ShadowNotificationManager` jest powiadomienie z niepustym tytułem.

---

## Podział na taski

| # | Task | Typ testu |
|---|------|-----------|
| 1 | `SecretStore` + `SharedPreferencesSecretStore` | Robolectric |
| 2 | Integracja: zapamiętane hasło w `MainActivity` (+ „zapomnij") | Robolectric |
| 3 | `BuiltInRecipes` (refactor wspólnej puli) | JVM |
| 4 | `MealSlot` | JVM |
| 5 | `NextAlarmTime` | JVM |
| 6 | `MealNotificationContent` | JVM |
| 7 | `MealReminderScheduler` + `MealNotifications` (kanał) | Robolectric |
| 8 | `MealReminderReceiver` (offline propozycje → powiadomienie) | Robolectric |
| 9 | `BootReceiver` + manifest (uprawnienia, odbiorniki) | Robolectric/JVM |
| 10 | `MainActivity`: kanał, planowanie, prośba o uprawnienie, wejściowa extra | Robolectric |
| 11 | README + odświeżenie dołączonego APK w `dist/` | — |

Każdy task: **najpierw test (czerwony) → implementacja (zielony) → commit**.

## Ryzyka i jak je tniemy

- **Brak Android SDK w środowisku** → instalujemy cmdline-tools/SDK, by `./gradlew
  test` i `assembleDebug` działały (wymagane też do TDD).
- **Powiadomienia w tle bez sieci** → świadomie offline, zero zależności od AI.
- **Ograniczenia exact-alarm (API 31+)** → `setInexactRepeating`, bez
  `SCHEDULE_EXACT_ALARM`.
- **Uprawnienie POST_NOTIFICATIONS (API 33+)** → prośba w czasie działania;
  starsze API nie wymagają.
- **Reboot kasuje alarmy** → `BootReceiver`.

---

## Review planu — znajdźki i poprawki

Krytyczny przegląd planu przed implementacją. Każdą znajdźkę od razu uwzględniono
w powyższym opisie i w podziale tasków.

1. **Duplikacja logiki offline.** `MainActivity.generateOfflineProposals` i
   przyszły odbiornik powiadomień potrzebują tej samej ścieżki (pula → shuffle →
   `VariedMealPicker`) oraz tej samej budowy profilu gustu. **Poprawka:** wyciągamy
   `domain/OfflineProposalGenerator`, którego używają oba miejsca (DRY, testowalne
   na JVM). `BuiltInRecipes` dostarcza pulę dań. *(nowy task 4)*

2. **Praca w `BroadcastReceiver.onReceive`.** Działa na głównym wątku z limitem
   ~10 s. **Poprawka:** świadomie tylko tania praca w pamięci (SharedPreferences +
   wybór offline, bez sieci), więc bez `goAsync()`. Udokumentowane.

3. **Dotknięcie powiadomienia, gdy aplikacja już działa.** Sam `getIntent()` w
   `onCreate` nie wystarczy. **Poprawka:** `MainActivity` z `launchMode="singleTop"`
   + obsługa `onNewIntent`, oba czytają extrę `meal_index`.

4. **Idempotentność planowania.** Planowanie przy każdym starcie nie może mnożyć
   alarmów. **Poprawka:** stałe `requestCode` na slot + `FLAG_UPDATE_CURRENT` →
   istniejący alarm jest nadpisywany, nie duplikowany.

5. **Uprawnienie `POST_NOTIFICATIONS` bez nowych zależności.** Brak `androidx.core`
   w projekcie. **Poprawka:** surowe `checkSelfPermission` / `requestPermissions`
   (API 23+), prośba tylko gdy `SDK_INT >= 33 (TIRAMISU)`. Odmowa nie wywraca
   aplikacji — reszta działa, powiadomienia po prostu się nie pokażą.

6. **Powtarzanie alarmu.** `setInexactRepeating(INTERVAL_DAY)` sam się powtarza co
   dobę od zakotwiczenia, więc nie trzeba przeplanowywać po każdym strzale — tylko
   po reboocie (`BootReceiver`). Udokumentowane.

7. **Pusta lista propozycji w powiadomieniu.** **Poprawka:** `MealNotificationContent`
   ma sensowny fallback („Otwórz, by zobaczyć pomysły"), gdy pula jest pusta.

### Zaktualizowany podział na taski

| # | Task | Typ testu |
|---|------|-----------|
| 1 | `SecretStore` + `SharedPreferencesSecretStore` | Robolectric |
| 2 | Integracja: zapamiętane hasło w `MainActivity` (+ „zapomnij") | Robolectric |
| 3 | `BuiltInRecipes` (refactor wspólnej puli) | JVM |
| 4 | `OfflineProposalGenerator` (DRY: ekran + powiadomienia) | JVM |
| 5 | `MealSlot` | JVM |
| 6 | `NextAlarmTime` | JVM |
| 7 | `MealNotificationContent` (z fallbackiem) | JVM |
| 8 | `MealReminderScheduler` + `MealNotifications` (kanał) | Robolectric |
| 9 | `MealReminderReceiver` (offline → powiadomienie) | Robolectric |
| 10 | `BootReceiver` + manifest (uprawnienia, odbiorniki, singleTop) | Robolectric/JVM |
| 11 | `MainActivity`: kanał, planowanie, prośba o uprawnienie, wejściowa extra | Robolectric |
| 12 | README + odświeżenie dołączonego APK w `dist/` | — |
</content>
</invoke>
