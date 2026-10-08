# Backend Mealspire — uruchomienie i redeploy

Cel: `ubuntu@51.83.199.206`. Ta zmiana nie jest wdrożona ani połączona z main.
Nie zmieniaj homepage/ew, jej proxy, portów 80/443 ani plików APK na homepage.
Przed pierwszym wdrożeniem potrzebna jest zgoda na produkcję. Ten run jej nie ma.

## Kontrakt i podział

API `/v1`: GET `catalog`; POST `taste`, `proposals`, `recipe`, `modify`, `import`.
GET `/health` zwraca status i wersję API. Backend Java 17 słucha wyłącznie na
127.0.0.1:18081. Android ma stały adres `https://51.83.199.206:8443`.
`BackendWire` opisuje JSON: mealType, likes, recent, choices, known, household;
proposals dodaje count, recipe dodaje dish. Modify wysyła recipe, instruction,
household, import wysyła input. Każdy POST zawiera model wybrany przez użytkownika.
Nie zmieniaj kompatybilności v1 przy redeployu; nowe pola są opcjonalne, a zmiana
niekompatybilna potrzebuje nowej wersji API i migracji klienta.

Telefon obsługuje OAuth/PKCE i rotację refresh tokenów, magazyn danych, cache,
offline, powiadomienia i baner APK. W operacjach online wysyła dane domenowe,
bez promptów i bez refresh tokenów. VPS dobiera prompty, ocenia gust przez LLM,
generuje i waliduje propozycje oraz udostępnia katalog. Wbudowana baza i lokalne
uczenie pozostają tylko dla offline. Katalog cache'uje się w SharedPreferences;
pierwszy start zasiewa zegar, następne odświeżenie po 24h. Awaria zachowuje
poprzedni katalog; błąd propozycji AI przełącza ekran na lokalną pulę.
Import linku traktuje URL jako tekst dla modelu, bez pobierania strony na VPS
(ochrona przed SSRF); do wiarygodnego importu można wkleić opis/składniki.

Token dostępowy użytkownika trafia przez TLS do VPS, potem do stałego endpointu
Responses API. Backend nie zapisuje tokenów, polubień ani odpowiedzi. 401 od
OpenAI wraca jako 401 do telefonu: jedna rotacja i ponowienie. Inne błędy są
redagowane. Nie włączaj logowania nagłówków ani request/response body w proxy.
Ocena gustu to dodatkowe wywołanie LLM przed propozycjami (brak polubień pomija
ocenę); katalog jest publiczny. Brak backendowego klucza API.

## Sprawdzenie lokalne

Z JDK 17 i Android SDK 35:

```bash
./gradlew test assembleDebug :backend:installDist
JAVA_HOME=/path/to/jdk17 backend/build/install/backend/bin/backend
curl --fail http://127.0.0.1:18081/health
curl --fail http://127.0.0.1:18081/v1/catalog
```

Testy backendu używają fałszywego LLM i lokalnego HTTP, bez kont i sekretów.
Nie jest to dowód działania na produkcji ani rzeczywistej inferencji konta.

## Pierwsze uruchomienie na wskazanym VPS (osobna operacja)

1. Sprawdź zajętość 18081 i 8443, obecne jednostki i konfigurację homepage.
   Gdy którekolwiek są zajęte lub izolacja jest niejasna, zatrzymaj wdrożenie.
2. Zapewnij JRE 17, użytkownika `mealspire`, katalogi `/opt/mealspire/releases`,
   `/etc/mealspire` i prawa odczytu tylko dla użytkownika usługi.
3. Przygotuj certyfikat TLS z SAN IP 51.83.199.206 i łańcuchem zaufanym przez
   Android 23+. Bez poprawnego TLS nie wystawiaj tokenów na publicznym porcie.
   Nie wyłączaj walidacji certyfikatów w APK. Przy zmianie adresu potrzebna będzie
   jednorazowa aktualizacja powłoki; późniejsze funkcje zachowują adres i API v1.
4. Zbuduj `:backend:distTar`, skopiuj wyłącznie archiwum backendu do osobnego
   katalogu `/opt/mealspire/releases/<SHA>`. Nie kopiuj repo, signing/ ani APK.
   Rozpakuj; uruchom na wolnym loopbackowym porcie testowym, np. 18082, sprawdź
   health i catalog, zatrzymaj proces próbny.
5. Katalog JSON (schema=1, meals: trzy niepuste tablice title/details) zapisz
   w `/etc/mealspire/catalog.json`. Wzorzec wygenerujesz z lokalnego GET catalog.
   Ustaw symlink `/opt/mealspire/current` i zainstaluj osobną jednostkę
   `deploy/mealspire-backend.service`. Sprawdź ścieżkę JAVA_HOME na hoście.
6. Konfiguracja `deploy/nginx-isolated.conf` to szablon osobnego procesu proxy
   na 8443 z własnym pid/config. Sprawdź ją `nginx -t -c <pełna ścieżka>`.
   Nie włączaj jej do istniejącego proxy homepage. Uruchom pod osobną jednostką,
   z niezależnym zarządzaniem certyfikatem. Udostępnij wyłącznie port 8443.
7. Sprawdź HTTPS health/catalog z klienta i operacje konta testowego (propozycje,
   przepis, modify, import, 401), dietę, powrót offline i cache po restarcie.
   Zweryfikuj, że homepage pozostała dostępna. Nie publikuj APK bez osobnej zgody.

## Następny redeploy bez nowego APK

Zbuduj i przetestuj nową wersję backendu. Rozpakuj archiwum w nowym katalogu
releases, uruchom próbnie na wolnym porcie loopback i sprawdź API v1. Zachowaj
SHA i poprzedni cel symlinku. Atomowo przełącz symlink current (nowy symlink
+ `mv -T`) i wykonaj `systemctl restart mealspire-backend` — tylko tę jednostkę.
Sprawdź health/catalog przez TLS i testowe propozycje. Proxy oraz homepage nie
wymagają restartu. Przy błędzie przywróć poprzedni symlink i zrestartuj wyłącznie
mealspire-backend. Aktualizacja katalogu również wymaga restartu backendu;
telefon odbierze ją po następnej dobowej próbie, bez zmiany APK.

Przed pierwszym użyciem architektury użytkownik potrzebuje jednorazowej wersji
powłoki z tego PR. APK w dist jest artefaktem do review, nie publikacją na homepage.
