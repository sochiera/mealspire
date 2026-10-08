# Kontrakt backendu v1

Android jest powłoką: OAuth, lokalne magazyny, widoki, powiadomienia offline,
cache i aktualizator APK. Online: VPS buduje prompty, ocenia gust LLM,
generuje i waliduje propozycje/przepisy, modyfikuje przepisy, importuje opis
lub link jako opis, utrzymuje katalog. Lokalny model gustu/pula startowa
pozostają do pracy offline i quizu; nie są źródłem promptów online.
Dotychczasowe czyste klasy domeny kompilujemy również w module `backend`;
zmiana ich zachowania online wymaga tylko nowego release backendu.

Ścieżki są względne wobec konfigurowalnego adresu bazowego HTTPS:

| Metoda i ścieżka | Wejście | Wynik |
| --- | --- | --- |
| GET /health | brak | status: ok |
| GET /v1/catalog | brak, bez tokenu | revision, meals: 3 tablice {title, details} |
| POST /v1/proposals | request, count (1–6, domyślnie 3) | proposals: {name, description, time, ingredients[]} |
| POST /v1/recipe | request, dishName (opcjonalne) | recipe: {title, details} |
| POST /v1/modify | recipe, instruction, household | recipe |
| POST /v1/import | input | recipe, source |
| POST /v1/taste | request | taste: {profile: maks. 4 zdania} |
| POST /v1/rate | mealType, household, reactions[] (≤30), candidates[] (1–12) | ratings: {dish, score 0–10, reason} |

Każdy POST ma `apiVersion:1` (brak oznacza v1), `model` (slug wybranego Luna/Sol
z katalogu konta) i Authorization: Bearer access token. Każda odpowiedź,
łącznie z błędem, ma `apiVersion:1`, JSON UTF-8, Cache-Control: no-store.
Nie zapisujemy treści POST ani tokenów na dysku. Provider endpoint jest stały;
klient nie może narzucić URL inferencji ani system promptu. `store:false`,
`stream:true`, zgodnie z [OpenAI Docs](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference).
Brak dostępności wybranego modelu jest błędem, bez cichej podmiany modelu.

`request`: mealType (Śniadanie/Obiad/Kolacja), preferences {likes[],dislikes[]},
household {audience,skill,time,cuisines[],exclusions[]}, recent[], choices[],
known[], affinities[], taste {profile[],examples[],exploration,antiMonotony}.
Pola opcjonalne mają puste wartości; nieznane pola są ignorowane. `choices`
to aktualne wybory UI (np. porcja); sygnały eksploracji i antymonotonii pozostają
kompatybilne z lokalnym dziennikiem. Profil LLM online liczony jest na VPS
z polubień/diety; wynik zastępuje lokalne zdania profilu przy propozycjach.
Cache oceny gustu ogranicza powtórne inferencje. Server waliduje dietę i
uzupełnia brakujące propozycje bez kolejnego wywołania LLM; gotowy przepis
naruszający dietę zwraca 422. Nieznane wykluczenie diety zwraca 400,
aby nigdy nie zignorować wymagania nowszego klienta.

`/v1/rate` to jedyne wywołanie LLM przy propozycjach zalogowanej aplikacji
(od wersji z ocenami dań): reactions {dish, description, liked, time},
candidates {name, description ≤500 znaków — sam skład, bez pełnego przepisu}.
Serwer zwraca tylko oceny podanych kandydatów, bez naruszeń diety; telefon
filtruje dietę ponownie na pełnym przepisie i bierze 3 najwyżej ocenione.
Nieczytelny JSON modelu → 502 `invalid_rating_response`; klient wraca do puli offline.

Przykład POST /v1/recipe (bez nagłówka z rzeczywistym tokenem):

```json
{"apiVersion":1,"model":"gpt-6-luna","dishName":"Curry","request":{"mealType":"Obiad","preferences":{"likes":["Ryż"]},"household":{"exclusions":["VEGETARIAN"]},"recent":["Makaron"],"choices":["Dla 2 osób"]}}
```

Przykład odpowiedzi:

```json
{"apiVersion":1,"recipe":{"title":"Curry z warzywami","details":"Składniki: ...\nPrzygotowanie: ..."}}
```

Błędy mają `error` jako stabilny kod, np. unauthorized, invalid_request,
unsupported_diet, model_unavailable, invalid_recipe, upstream_unavailable.
HTTP: 400 złe dane/nieobsługiwany model/dieta; 401 token; 404 ścieżka;
405 metoda; 413 rozmiar; 415 Content-Type; 422 przepis; 426 wersja; 429 zajętość;
502 dostawca. Klient nie wyświetla surowych body błędów i retry wykonuje
wyłącznie raz po 401 (refresh lokalny). Propozycje przy awarii wracają do
cache/offline. Nie stosujemy automatycznego retry generacji na 5xx.

Zasada kompatybilności: w `/v1` wolno dodawać opcjonalne pola, nowe endpointy,
aktualizować katalog, prompty i logikę; nie usuwać/zmieniać znaczenia istniejących
pól. Nowe wymagane pola/typy/znaczenia wymagają `/v2`, z równoległym utrzymaniem
`/v1` dla zainstalowanych klientów. Nie zmieniaj `apiVersion` v1 odpowiedzi.
Klient odrzuca inne major, informując o aktualizacji APK. UI systemowe,
nowe typy widoków i uprawnienia Androida nadal wymagają wydania APK.
Weryfikacja rollout: odpal testy kontraktu starszego klienta przeciw nowemu
backendowi oraz staging; rollback przywraca poprzedni backend bez zmian danych.
