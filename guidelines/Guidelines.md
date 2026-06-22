# MeetNGo — Guidelines

Diese Guidelines fassen die Konventionen für die beiden Projektteile zusammen: die
native **Android-App** (`android/`, Jetpack Compose, Kotlin, osmdroid) und das
**Backend** (`backend/`, Express + better-sqlite3, JWT). Es gibt **kein** Web-Frontend.

## Architektur-Überblick

- **Android-App** ← REST → **Express-Backend** ← → **SQLite**
- Die App kommuniziert ausschließlich über die REST-API (`/api/...`).
- Netzwerk-Layer: Retrofit/OkHttp; ein Interceptor hängt das JWT automatisch als
  `Authorization: Bearer …` an.
- Emulator erreicht den Host über `10.0.2.2` (nicht `localhost`); die Basis-URL liegt
  in `data/api/ApiClient.kt`.
- Karten-Kacheln werden über den Backend-Proxy (`/tiles/...`) geladen, weil der
  Emulator keine externen DNS-Namen auflöst.

## Android (Jetpack Compose)

- **Material 3** als Design-System; das zentrale Theme (inkl. Dark Mode) liegt in
  `ui/theme/`. Farben/Typografie nicht hartcodieren, sondern aus dem Theme beziehen.
- **Navigation** über Navigation Compose (`ui/navigation/NavGraph.kt`) mit
  Bottom-Navigation; geschützte Routen erfordern ein gültiges Token.
- **Struktur**: Screens nach Feature getrennt unter `ui/screens/` (auth, map, search,
  eventdetail, createevent, tickets, scanner, organizer, profile). Datenmodelle,
  Repositories und API-Definitionen unter `data/`.
- **Mobile-First-UX**: ausreichend große Touch-Targets, Bottom-Navigation für
  einhändige Bedienung, Bestätigungsdialoge nur bei kritischen/destruktiven Aktionen
  (Logout, Event löschen), sichtbare Lade- und Fehlerzustände bei jeder Interaktion.
- Alle API-Aufrufe sind `suspend`-Funktionen und laufen auf einem IO-Dispatcher.

## Backend (Express)

- **Schichtentrennung**: `server.js` (Prozess-Einstieg, `.env`, Listener) →
  `app.js` (Express-App ohne `.listen`, testbar) → `routes/` → `database.js`.
- **Auth** zentral über `middleware/auth.js` (JWT). Geschützte Routen prüfen den
  Token identisch — keine Duplikation der Auth-Logik.
- Fehler immer als **JSON** zurückgeben (kein Express-HTML), interne Fehler nicht an
  den Client leaken (zentraler Error-Handler in `app.js`).
- Geschäftsregeln (Kapazität, Berechtigungen, Eindeutigkeit) **serverseitig**
  durchsetzen, nicht der App vertrauen. Kapazitätskritische Schreibvorgänge in
  `db.transaction(...)` kapseln.
- Konsistente HTTP-Statuscodes: `400` Validierung, `401` nicht authentifiziert,
  `403` keine Berechtigung, `404` nicht gefunden, `409` Konflikt.

## Code-Qualität

- ESLint + Prettier für das Backend (`pnpm lint`, `pnpm format`).
- Backend-Tests mit Vitest + Supertest gegen eine isolierte In-Memory-DB; die lokale
  `meetngo.db` darf nie berührt werden.
- Keine Geheimnisse committen: `backend/.env` ist gitignored, nur `.env.example`
  mit Variablennamen (`PORT`, `JWT_SECRET`) versionieren.

## Design-Referenz

Figma: https://www.figma.com/design/XFbfqoyFt7IFZrnVYUjUzJ/MeetNGo-Mobile-App-UI
