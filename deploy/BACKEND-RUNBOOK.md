# Backend Mealspire: test i konfiguracja OVH

Host wskazany przez Jana: `ubuntu@51.83.199.206`, klucz lokalny
`~/.ssh/pbn_vps`. Host obsługuje istniejące usługi i homepage; nie zmieniaj ich.
Wdrożenie produkcyjne backendu opisuje sekcja niżej; publikacja APK to osobny proces.
Dotychczasowy `RUNBOOK.md` dotyczy wydania APK i pozostaje osobnym procesem.

## Przygotowanie lokalne

JDK 17, Android SDK 35 do wspólnego buildu. Ustaw `JAVA_HOME` i `ANDROID_HOME`.
`./gradlew test assembleDebug :backend:installDist` buduje klienta i backend.
Backend jest samodzielnym programem Java; nie wymaga Androida, bazy ani klucza API.
Android używa platformowego org.json, JVM używa tej samej biblioteki JSON
(20240303) jako jedynej zależności runtime. Kod sieciowy pozostaje biblioteką JDK.
Artefakt do backendu: `backend/build/install/backend/`; zachowaj cały katalog.
Przed ship-it nie podbijaj wersji ani nie kopiuj APK do dist/homepage.

## Bezpieczny test na VPS (dozwolony przed ship-it)

Bez sudo, instalowania pakietów, kontenerów, konfiguracji Nginx i restartów.
Wymagane: SSH/SCP, Python 3 na VPS, wolne miejsce w /tmp, zgodna architektura x86_64.
Sprawdź aktualnie `uname -m`, `df -h /tmp`, możliwość wykonania plików w /tmp.
Przygotuj własne minimalne środowisko Java poleceniem `$JAVA_HOME/bin/jlink
--add-modules java.base,jdk.httpserver,jdk.crypto.ec --strip-debug --no-header-files
--no-man-pages --output /LOKALNY-KATALOG/runtime`. Na JDK 17 `jdk.crypto.ec`
jest wymagany: bez niego TLS do OpenAI zawodzi i każde żądanie AI kończy się 502.
Umieść runtime, backend z installDist i `backend-smoke.py` w jednym archiwum
(katalogi `runtime`, `backend`, plik `backend-smoke.py`). Nie dołączaj sekretów.

1. Utwórz przez SSH nowy `mktemp -d /tmp/mealspire-6ac75e93.XXXXXXXX` i zapisz ścieżkę.
2. Prześlij archiwum SCP wyłącznie do tego katalogu, rozpakuj w nim.
3. Uruchom `python3 STAGE/backend-smoke.py STAGE` przez SSH, ze zdalnym
   `timeout 45s`. Skrypt używa losowego portu **127.0.0.1** (`MEALSPIRE_PORT=0`),
   bez publikacji sieciowej. Sprawdza realny HTTP, katalog, wersję API, błędy,
   autoryzację, rozmiar danych; nie używa kont użytkowników ani modelu live.
4. Skrypt w finally zatrzymuje wyłącznie swój proces i czeka na zakończenie.
   Po teście usuń wyłącznie zapisany STAGE (weryfikuj prefiks przed `rm -rf`).
   Przy przerwanym SSH sprawdź proces **własnego** STAGE przed sprzątaniem,
   nigdy nie używaj `pkill java`, `docker prune` ani zatrzymywania istniejących usług.
5. Zapisz wynik, czas oraz potwierdzenie sprzątnięcia. Ten test nie oznacza deployu.

## Konfiguracja klienta

Adres produkcyjny jest wbudowany (`BackendClient.DEFAULT_BASE_URL` =
`https://sochiera.pl/mealspire-api`); aplikacja nie pyta o adres. Pusty lub
inny adres zapisany przez starszą wersję jest przy starcie zastępowany
domyślnym, a katalog ze starego adresu usuwany. Zmiana adresu wymaga nowego APK.
Dialog logowania ChatGPT informuje o przekazaniu krótkotrwałego tokenu ChatGPT
oraz gustu do serwera Mealspire; bez logowania klient nie łączy się z serwerem.
OAuth/PKCE, weryfikacja ID tokenu, refresh/revoke zostają na urządzeniu.
Backend nie otrzymuje refresh/ID tokenu ani e-maila. Nie podążamy za redirectami
HTTP. Backend weryfikuje access token i dostępność wybranego modelu w OpenAI.
Po 401 klient odświeża token na telefonie i ponawia tylko raz.

Katalog pobierany przy użyciu aplikacji nie częściej niż co 24h po udanym
odczycie, zapisuje się atomowo dopiero po walidacji. Awaria zostawia cache;
brak cache używa puli startowej. Powiadomienia korzystają z tej samej kopii
bez sieci. Zmiana adresu usuwa wcześniejszy katalog, odpowiedź ze starego
adresu nie zapisuje się. Zgoda/adres/cache nie podlegają backupowi.
Gust, domownicy, historia, zapisane przepisy i baner aktualizacji APK zachowują
istniejące magazyny. Cache przepisu w ekranie pozostaje bez ponownego żądania.

## Wdrożenie produkcyjne (2026-10-08, main 2fa096f)

Adres w aplikacji: `https://sochiera.pl/mealspire-api` (TLS istniejącego vhostu
`sochiera.pl`; osobna subdomena wymagałaby nowego rekordu DNS i certyfikatu).
Pliki w `deploy/backend/`:
- `mealspire-backend.service` → `/etc/systemd/system/`; użytkownik systemowy
  `mealspire` (nologin), port 127.0.0.1:8794, utwardzony sandbox, MemoryMax 400M.
- `mealspire-api-limits.conf` → `/etc/nginx/conf.d/` (limit 30/min na IP, burst 20,
  4 połączenia na IP).
- `mealspire-api.conf` → `/etc/nginx/snippets/`, dołączony w vhoście TLS
  (`/etc/nginx/sites-enabled/pbn`) linią `include` zaraz po `forge.conf`.
  Body 256 KiB, timeout body 10s, proxy 240s, bez access logu.

Release: archiwum `runtime/` + `backend/` + `REVISION` rozpakowane jako root do
`/opt/mealspire/releases/<krótki-sha>`; `/opt/mealspire/current` to symlink na
aktywny release. Nowa wersja: rozpakuj nowy katalog, `ln -sfn` current,
`systemctl restart mealspire-backend`, sprawdź `/health`, `/v1/catalog` i żądanie
z fałszywym tokenem (oczekiwane 401 `unauthorized` = TLS do OpenAI działa;
502 = brak `jdk.crypto.ec` lub awaria upstream).

Rollback backendu: `ln -sfn` current na poprzedni release i restart usługi.
Wycofanie całości: `systemctl disable --now mealspire-backend`, przywróć
`/root/nginx-backup-mealspire-20261008/pbn.orig` do `sites-enabled/pbn`, usuń
oba pliki nginx Mealspire, `nginx -t` i `systemctl reload nginx`.
Nie dotykaj pozostałych bloków vhostu, usług `ew-web`, `pbn-test-tunnel` ani Dockera.

## Wymagania produkcyjne

Przygotuj osobną usługę/user i katalog wersjonowanych release backendu; nigdy
nie nadpisuj katalogów homepage/ew/PBN. `MEALSPIRE_PORT` wybiera osobny port
loopback (domyślnie 8794); sprawdź, czy jest wolny. Start przez `bin/backend`
wymaga JRE 17 i JAVA_HOME. Bind inny niż loopback jest odrzucany.
Nie loguj nagłówków, tokenów, treści żądań/odpowiedzi ani pełnych błędów OpenAI.
Serwis jest bezstanowy na dysku; ocena gustu ma ograniczony cache w RAM
(256 wpisów, 15 min, klucz SHA-256 z tokenu/modelu/preferencji/diety). Restart czyści cache.

Przed wystawieniem API wymagany jest osobny zatwierdzony reverse proxy TLS:
oddzielna domena/vhost, certyfikat, limit body 256 KiB, timeout request body 10s,
limit żądań na IP i równoległości, timeout proxy 240s; wyłącz logowanie body
oraz Authorization. Proxy ma blokować dostęp do backendu poza HTTPS.
Przykładowe dyrektywy dla nowego vhost: `client_max_body_size 256k`,
`client_body_timeout 10s`, `proxy_read_timeout 240s`, `proxy_pass
http://127.0.0.1:8794`. Limit żądań wymaga osobnej `limit_req_zone` w http;
nie dokładaj jej do istniejącego Nginx w ramach tej karty.
Nie wystawiaj wbudowanego HttpServer bezpośrednio do Internetu: limity
połączeń i wolnych klientów należą do proxy. Backend ogranicza rozmiar danych
oraz do 2 równoległych operacji LLM, błędy API nie zawierają sekretów.
Import linków przekazuje adres jako opis do LLM; nie pobiera stron z VPS
(ochrona SSRF, ograniczenie względem wcześniejszego importu zawartości HTML).

Sprawdź health/catalog, pełny przepływ Android → HTTPS → backend → OpenAI
na osobnym koncie testowym (bez wypisywania tokenów), dietę, wybór modelu,
refresh, awarię i restart backendu. Testy ze stubem LLM nie zastępują live.
Urządzenie Android 6/8+ musi też potwierdzić logowanie, offline, cache,
powiadomienia i instalowanie aktualizacji. Brak tych testów odnotuj w raporcie.
Rollback: uruchom poprzedni release backendu na tym samym kontrakcie `/v1`;
nie cofaj APK ani danych użytkownika. Najpierw weryfikacja staging/CI, potem
osobna zgoda/ship-it, review i merge. Zmiany Nginx/usług wymagają odrębnego
zakresu i zgody; ten runbook nie udziela takiej zgody.
