Jesteś autonomicznym agentem programistycznym pracującym w repozytorium:

/home/jan/Sources/mealspire

Twoim zadaniem jest rozszerzyć aplikację Android Mealspire zgodnie z wymaganiami poniżej. Pracuj ostrożnie, małymi etapami, w TDD. Po każdym zakończonym etapie wykonaj commit i push.

Kontekst aplikacji
- Mealspire to prosta natywna aplikacja Android.
- Aktualny stack: Java, Gradle, Android Gradle Plugin, minSdk 23, targetSdk 35.
- Główny kod jest w `app/src/main/java/com/mealspire/app/MainActivity.java`.
- UI jest obecnie budowany programowo, bez XML layoutów.
- Aplikacja pozwala wybrać `Śniadanie`, `Obiad` albo `Kolacja` i losuje jeden z kilku przepisów.
- README zawiera komendy budowania i instalacji. Używaj istniejących narzędzi z repozytorium.

Cel produktu
Rozwiń Mealspire z prostej losowarki przepisów w bardziej użyteczną aplikację do inspirowania posiłkami.

Docelowa grupa użytkowników
Ktoś, kto gotuje dla siebie i swojej rodziny w domu.

Najważniejszy scenariusz użycia
Użytkownik pyta o pomysł i przepis na posiłek, który ma ugotować.

Funkcjonalności do zbudowania
Uzupełnij i realizuj funkcjonalności w podanej kolejności. Każda funkcjonalność ma być osobnym etapem TDD, o ile nie zaznaczono inaczej.

1. Appka wykorzytuje LLM przez API (podam ci klucz API potem, nie możesz losować przepis z jakiejś listy np), żeby wymyslić pomysł na danie i wygenerować przepis
2. Appka zapisuje preferencje użytkowników i uczy się co lubią jeść i co potrafią gotowąć
3. Appka podsyła pomysły na dania, których dawno nie było
4. Appka daje użytkownikowi dużo możliwości wyboru i przy każdym wyborze uczy się


Wymagania techniczne
- Pracuj w TDD: najpierw test, potem minimalna implementacja, potem refaktor.
- Nie rób dużego przepisywania aplikacji bez potrzeby.
- Preferuj małe, czytelne klasy domenowe, które da się testować testami JVM bez emulatora.
- Oddziel logikę wyboru, filtrowania, przechowywania i modelowania przepisów od `MainActivity`.
- `MainActivity` powinna być możliwie cienka: składa UI, reaguje na zdarzenia i deleguje logikę.
- Jeśli dodajesz zależności, uzasadnij je krótko w commit message albo opisie etapu. Nie dodawaj ciężkich bibliotek dla prostych zadań.
- Zachowaj kompatybilność z minSdk 23.
- Zachowaj polski język interfejsu, chyba że wymagania funkcjonalne mówią inaczej.
- Dbaj o dostępność: czytelne etykiety, sensowne rozmiary tekstu, brak informacji przekazywanej wyłącznie kolorem.
- Dbaj o stan po obrocie ekranu i wznowieniu aplikacji, jeśli funkcjonalność przechowuje wybory użytkownika.
- Nie usuwaj istniejącej funkcji losowania przepisu, chyba że wymagania wyraźnie mówią inaczej.

Wymagania testowe
- Jeśli w repo nie ma infrastruktury testowej, dodaj ją w pierwszym etapie technicznym.
- Preferuj szybkie testy jednostkowe JVM dla logiki domenowej.
- Dodaj testy Android/instrumentacyjne tylko wtedy, gdy funkcjonalności nie da się rozsądnie pokryć testami JVM.
- Dla każdej funkcjonalności dodaj testy kryteriów akceptacji.
- Uruchamiaj właściwy zestaw testów po każdym etapie.
- Przed każdym commitem uruchom co najmniej:
  - `JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew test`
- Przed końcowym podsumowaniem uruchom:
  - `JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew test assembleDebug`

Praca etapami
Dla każdego etapu wykonaj dokładnie ten cykl:

1. Krótko opisz plan etapu.
2. Sprawdź aktualny stan repo:
   - `git status --short --branch`
3. Napisz testy, które na początku powinny wykazać brak funkcji.
4. Uruchom testy i upewnij się, że nowy test faktycznie wykrywa brak implementacji.
5. Zaimplementuj minimalną zmianę.
6. Uruchom testy ponownie.
7. Zrób mały refaktor, jeśli poprawia czytelność bez zmiany zakresu.
8. Uruchom testy po refaktorze.
9. Zaktualizuj README, jeśli zmienia się sposób uruchamiania, testowania albo używania aplikacji.
10. Pokaż krótki diff/status.
11. Wykonaj commit z opisem w formacie:
    - `feat: ...`
    - `test: ...`
    - `refactor: ...`
    - `docs: ...`
12. Wykonaj `git push`.
13. Dopiero potem przejdź do następnego etapu.

Zasady commitów i pushowania
- Każdy etap musi kończyć się osobnym commitem.
- Po każdym commicie natychmiast wykonaj `git push`.
- Jeśli push nie działa, nie obchodź problemu po cichu. Zatrzymaj się, podaj dokładny błąd i zaproponuj najbezpieczniejsze rozwiązanie.
- Nie wykonuj `git reset --hard`, `git checkout -- .` ani innych destrukcyjnych operacji bez wyraźnej zgody użytkownika.
- Nie nadpisuj zmian użytkownika. Jeśli pojawią się obce zmiany w plikach, z którymi pracujesz, zatrzymaj się i wyjaśnij konflikt.

Oszczędność tokenów i kontekstu
- Czytaj tylko pliki potrzebne do bieżącego etapu.
- Nie wklejaj całych dużych plików do odpowiedzi, jeśli wystarczy ścieżka, krótki diff albo streszczenie.
- Grupuj niezależne odczyty plików i komendy, gdy to możliwe.
- Nie uruchamiaj wielu agentów pomocniczych, jeśli jeden agent może wykonać zadanie.
- Stosuj krótkie, konkretne komunikaty robocze.

Wymagania końcowe
Po zrealizowaniu wszystkich funkcjonalności:
- Uruchom `JAVA_HOME=/home/jan/.local/opt/jdk-17 ./gradlew test assembleDebug`.
- Sprawdź `git status --short --branch`.
- Upewnij się, że wszystkie etapy są wypchnięte do remote.
- Podsumuj:
  - zrealizowane funkcje,
  - dodane lub zmienione testy,
  - komendy weryfikacyjne i ich wynik,
  - listę commitów utworzonych w ramach pracy,
  - ewentualne ryzyka lub dalsze kroki.

Nie kończ pracy po samej analizie. Implementuj, testuj, commituj i pushuj każdy etap.

