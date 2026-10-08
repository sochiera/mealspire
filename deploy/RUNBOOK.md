# Publikacja Mealspire: APK na VPS i aktualizacja w aplikacji

Stan potwierdzony przez SSH i HTTPS 2026-10-08. Publikacja wymaga zgody
ship-it, PR, review innej rodziny modelu i zielonego CI przed merge.

## Istniejące kanały

- VPS OVH: `ubuntu@51.83.199.206` (`vps-fe469e06.vps.ovh.net`). SSH kluczem,
  sudo bez interakcji. Lokalnie klucz wdrożeniowy: `~/.ssh/pbn_vps`;
  nie kopiować klucza do repo, logów ani na serwer.
- Nginx: `sochiera.pl` i `www.sochiera.pl`, root `/var/www/sochiera`, HTTPS.
  Strona `/index.html` ma jeden link Mealspire do `/pobierz/mealspire-VERSION.apk`.
  Przed Fazą 2 wskazywał na `mealspire-1.2.apk`. Nie wdrażać całej strony ani
  projektów PBN/poker; nie zmieniać konfiguracji Nginx i nie restartować usług.
- Aktualizator zainstalowanych aplikacji odczytuje
  `https://raw.githubusercontent.com/sochiera/mealspire/main/dist/wersja.json`.
  `apkUrl` nadal wskazuje na APK w `main/dist/mealspire-debug.apk`.
  Merge wydania **publikuje już kanał aktualizacji GitHub**; VPS jest dodatkowym
  kanałem pobierania. CI buduje artefakt, ale nie wdraża VPS.
- Na VPS publikujemy te same bajty APK oraz kopię metadanych:
  `https://sochiera.pl/pobierz/mealspire-wersja.json` (do weryfikacji/operatora).
  To nie zmienia URL metadanych używanego przez aplikację.

## Przygotowanie wydania w osobnym branchu

Wymagane: JDK 17, SDK platform 35/build-tools 35, Python 3, SSH/SCP, curl, gh.
Ustaw `JAVA_HOME`, `ANDROID_HOME`, dodaj `$JAVA_HOME/bin` do PATH;
`local.properties` może zawierać wyłącznie lokalne `sdk.dir` i nie trafia do Git.

1. Zacznij od aktualnego `origin/main`; sprawdź, że implementacja Fazy 2 jest
   zmergowana. Nie wykonuj jej ponownie.
2. Podbij `app/build.gradle` i `dist/wersja.json` razem (Faza 2: 4 / 1.3).
   Zachowaj `applicationId=com.mealspire.app`, dotychczasowe signingConfig i
   `signing/mealspire.keystore`; **nie generuj nowego klucza**.
3. `./gradlew assembleDebug`, następnie
   `cp app/build/outputs/apk/debug/app-debug.apk dist/mealspire-debug.apk`.
   Zapisz SHA-256 APK w polu `sha256` metadanych. Dopiero teraz wykonaj
   `./gradlew test assembleDebug` — test spójności porównuje hash w dist.
4. Pobierz dotychczasowy APK przez HTTPS z linku na stronie. Wykonaj
   `$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs` dla obu
   APK i porównaj SHA-256 certyfikatu. `aapt dump badging` musi potwierdzić
   tożsamość aplikacji i nową wersję. Nie publikuj przy rozbieżności podpisu.
5. Commit APK, metadanych, wersji i dokumentacji; PR + niezależne review,
   wymagane poprawki, zielone CI, merge. Zapamiętaj SHA main. Na VPS wysyłaj
   dokładnie zatwierdzone pliki `dist/`, nie odrębny build CI.

## Publikacja na VPS

Z katalogu zatwierdzonego checkoutu (poniższe zmienne nie zawierają sekretów):

```bash
set -euo pipefail
VPS=ubuntu@51.83.199.206
KEY="$HOME/.ssh/pbn_vps"
STAGE=$(ssh -o BatchMode=yes -i "$KEY" "$VPS" 'mktemp -d /tmp/mealspire-publish.XXXXXXXX')
scp -i "$KEY" dist/mealspire-debug.apk dist/wersja.json deploy/publish-vps.py "$VPS:$STAGE/"
INDEX_SHA=$(ssh -i "$KEY" "$VPS" 'sha256sum /var/www/sochiera/index.html' | cut -d ' ' -f1)
ssh -i "$KEY" "$VPS" "sudo python3 '$STAGE/publish-vps.py' '$STAGE' --expected-index-sha '$INDEX_SHA' --dry-run"
ssh -i "$KEY" "$VPS" "sudo python3 '$STAGE/publish-vps.py' '$STAGE' --expected-index-sha '$INDEX_SHA'"
ssh -i "$KEY" "$VPS" "rm -rf -- '$STAGE'"
```

Skrypt waliduje SHA-256, wersję, kanał metadanych, jeden znany link i hash
strony. Blokada serializuje publikacje Mealspire; zmiana strony przez inny
deploy wymaga ponownego odczytu i kontroli strony (nie powtarzaj na ślepo).
Inne systemy publikacji strony nie współdzielą tej blokady — nie uruchamiaj
równoległego wdrożenia całej strony. Skrypt nie weryfikuje certyfikatu APK:
porównanie podpisu i `aapt` z przygotowania wydania są obowiązkowe.

Backup strony i poprzednich metadanych powstaje w `/var/backups/mealspire/TIMESTAMP`.
APK i JSON zapisują się atomowo przed podmianą linku; stare APK zostają.
Wersjonowanego URL z inną zawartością skrypt nie nadpisze. Błąd po częściowym
zapisie nie oznacza udanej publikacji: sprawdź pliki i zastosuj wycofanie.

## Świeży odczyt produkcji i domknięcie

Do nowego katalogu tymczasowego pobierz przez `curl -fSL` (TLS włączony,
`Cache-Control: no-cache`, unikalny query string z czasem):

- `https://sochiera.pl/` oraz `https://www.sochiera.pl/`: link Mealspire ma
  prowadzić do nowego APK. Następnie pobierz APK **ze znalezionego linku**.
- `https://sochiera.pl/pobierz/mealspire-wersja.json`.
- Oba URL GitHub wskazane w sekcji kanałów.

Wymagaj HTTP 200, identycznych JSON z dist, identycznych bajtów APK i hashów
w obu kanałach, versionCode/versionName zgodnych z `aapt`, poprawnego podpisu
nowego APK i zgodności certyfikatu z poprzednim wydaniem. Zapisz UTC odczytu,
main SHA, URL, hash i wynik w raporcie publikacji; bez sekretów.
Nginx ma cache max-age=300, GitHub też może propagować zmianę z opóźnieniem.
Przy starym odczycie poczekaj na propagację i zweryfikuj ponownie; sam merge
lub SCP nie jest dowodem wdrożenia.

Testy JVM/Robolectric i sprawdzenie APK nie zastępują testów urządzenia.
Na Androidzie 6–7 oraz 8+ sprawdź aktualizację 1.2 → 1.3 z zachowaniem danych,
zgodę na nieznane źródła, potwierdzenie systemowe, postęp, błąd hash/sieci,
ponowienie, anulowanie oraz sprzątanie sesji. Przy braku urządzenia/emulatora
jawnie odnotuj, że te testy nie były wykonane.

Dopiero po merge + deploy + świeżym odczycie zaznacz `merged` i `deployed`
w Delivery, zweryfikuj odczytem i przenieś do Zrobione wyłącznie przy
`ship-it + merged + deployed`. Nigdy nie zmieniaj `ready-to-start`/`ship-it`.

## Wycofanie

Przy problemie na VPS, pod nieobecność innego deployu strony, przywróć
`index.html` z podanego przez skrypt backupu przez plik tymczasowy w tym
samym katalogu i atomowy `mv`. Analogicznie przywróć kopię
`pobierz/mealspire-wersja.json` (jeśli przed publikacją jej nie było, usuń tylko
nowo dodaną kopię). Najpierw sprawdź diff strony, aby nie cofnąć cudzych zmian.
Zachowaj nowe i stare wersjonowane APK; Nginx nie wymaga restartu.

VPS rollback nie cofa kanału GitHub. W przypadku błędu aplikacji przygotuj
poprawione wydanie z **wyższym** versionCode i tym samym podpisem przez PR.
Nie obniżaj wersji: Android nie obsługuje zwykłej instalacji downgrade.
