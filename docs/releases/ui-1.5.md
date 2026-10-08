# Mealspire 1.5 — UI i ikona

Wydanie obejmuje przebudowę UI i ikonę z PR #26 (merge `32ae65c`),
z poprawką zachowania przecinków dziesiętnych w składnikach (`2df4bba`).
Niezależne review: OpenAI gpt-6.1-sol, PASS po poprawce.

- `applicationId`: `com.mealspire.app`; `versionCode`: 6; `versionName`: 1.5.
- Zatwierdzony artefakt: `dist/mealspire-debug.apk`, podpisany istniejącym
  kluczem Mealspire, zgodnie z kanałem dystrybucji opisanym w RUNBOOKu.
- SHA-256 APK: `ff4af4f381c2a10bdedd60c2603d9ca8a243f3ab131b29c0f0e2c4dfb638bd54`.
- SHA-256 certyfikatu: `a501493a011593508af8bddc83fd76d06bce7291fb75c3cf705bc76392bd1ae9`,
  zgodny z APK 1.4 pobranym z produkcji.
- Kanał aktualizacji: metadane i APK w `main/dist/` na GitHubie.
- Kanał strony: `https://sochiera.pl/pobierz/mealspire-1.5.apk`;
  publikacja i świeży odczyt obu kanałów według `deploy/RUNBOOK.md`.

Testy JVM/Robolectric i kontrola podpisu/wersji APK nie zastępują testów
urządzenia. Nie wykonano w tym wydaniu testów emulatora, launchera,
TalkBack ani aktualizacji na urządzeniu; ścieżki AI z siecią nie były
przedmiotem weryfikacji wizualnej. Tryb ciemny pozostaje poza zakresem.
