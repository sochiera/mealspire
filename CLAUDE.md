# Mealspire — instrukcje dla agentów

Zanim coś zmienisz, przeczytaj steeringi:

- `docs/steering/projekt.md` — architektura, decyzje produktowe, pułapki UI.
- `docs/steering/budowanie.md` — build/testy, odświeżanie `dist/`, praca za proxy.

Skrót najważniejszych reguł:

- Java (nie Kotlin), zero bibliotek third-party w kodzie produkcyjnym,
  minSdk 23, UI budowane programowo w cienkiej `MainActivity`.
- Logika w czystych klasach domenowych (`domain/`), rozwijana w TDD;
  `./gradlew test assembleDebug` musi przechodzić przed pushem.
- Po zmianach kodu odśwież `dist/mealspire-debug.apk` (kopia
  `app/build/outputs/apk/debug/app-debug.apk`).
- Teksty UI i prompty po polsku; commity po polsku z prefiksami `feat:`/`fix:`.
