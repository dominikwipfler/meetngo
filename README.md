# MeetNGo

[![CI](https://github.com/dominikwipfler/meetngo/actions/workflows/ci.yml/badge.svg)](https://github.com/dominikwipfler/meetngo/actions/workflows/ci.yml)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-strict-3178C6?logo=typescript&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-≥18-339933?logo=node.js&logoColor=white)
![Express](https://img.shields.io/badge/Express-4-000000?logo=express&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-better--sqlite3-003B57?logo=sqlite&logoColor=white)

Eine mobile Event-Discovery-App für Android: Events auf einer interaktiven Karte finden, durchsuchen, filtern, Tickets mit QR-Code "kaufen" und eigene Events veranstalten. Frontend mit React + Vite, Backend mit Express und SQLite.

---

## Inhalt

- [Features](#features)
- [Vorgaben & Extras](#vorgaben--extras)
- [Tech-Stack](#tech-stack)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Installation](#installation)
- [Umgebungsvariablen](#umgebungsvariablen)
- [Ausführung](#ausführung)
- [Tests](#tests)
- [Code-Qualität](#code-qualität)
- [CI/CD](#cicd)
- [API-Übersicht](#api-übersicht)
- [Datenbank](#datenbank)
- [Architektur](#architektur)
- [Android-Build (Capacitor)](#android-build-capacitor)
- [Screenshots](#screenshots)
- [Design](#design)

---

## Features

- **Authentifizierung** — Registrierung & Login mit JWT, Passwörter werden mit bcrypt gehasht
- **Kartenansicht** — Events als farbcodierte Marker nach Kategorie auf einer interaktiven Leaflet-Karte
- **Suche & Filter** — Volltextsuche, Kategorie-Filter, Preisfilter (kostenlos/kostenpflichtig), Sortierung nach Datum, Name, Beliebtheit oder Preis
- **Event-Details** — Detailansicht mit Ticket-Kauf-Flow (Mengenwahl, Zahlungsmethoden-Auswahl)
- **Echtes Ticket-System** — Tickets werden serverseitig angelegt (eigene `tickets`-Tabelle, an Event und Käufer gebunden), Kapazitätsgrenzen werden respektiert, Teilnehmerzahl wird automatisch hochgezählt
- **Tickets mit QR-Code** — jedes Ticket erzeugt einen echten, scanbaren QR-Code (clientseitig gerendert, kein externer Dienst)
- **Eigene Events erstellen** — inkl. Bild-Upload (JPG/PNG/WebP, max. 5 MB), Kategorie, Preis, Kapazität
- **Organizer-Dashboard** — Statistiken und Verwaltung der eigenen Events
- **Profil & Einstellungen** — Benutzername, E-Mail und Interessen werden serverseitig gespeichert (`PATCH /api/users/me`), Dark Mode
- **Barrierefreiheits-Menü** — Dark-Mode- und Kontrast-Umschalter, jederzeit erreichbar
- **Mobile-First-UI** — Bottom-Navigation, für Android-Bildschirme optimiert

## Vorgaben & Extras

### Pflicht-Features

| Vorgabe | Umsetzung |
|---|---|
| Login/out | JWT-Login (`LoginScreen`) und Logout mit Sicherheitsabfrage (`ProfileScreen` → `AlertDialog` → `AuthContext.logout()`) |
| Startseite mit Karte | `/map` ist die geschützte Landing-Route direkt nach dem Login (`LoginScreen` navigiert nach erfolgreichem Login auf `/map`); interaktive Leaflet-Karte mit kategorie-farbigen Markern, Live-Suche und Filter-Badges direkt auf der Karte |
| Event Infos einsehbar | `EventDetailScreen` zeigt Bild, Beschreibung, Datum, Ort, Preis, Kapazität/Teilnehmerzahl und Veranstalter; erreichbar von Karte, Suche und Profil |
| Event erstellen → Bild hochladen | `CreateEventScreen` mit Bild-Upload (JPG/PNG/WebP, clientseitig auf 5 MB geprüft, serverseitig per Multer validiert und gespeichert unter `backend/uploads/`) |
| Bei der Suche filtern können | `SearchScreen`: Volltextsuche, Kategorie-Filter, Preisfilter (kostenlos/kostenpflichtig), Sortierung nach Datum/Name/Beliebtheit/Preis — zusätzlich auch direkt auf der Kartenansicht filterbar |
| Gutes Design & Nutzerfreundlichkeit | Mobile-First-UI mit Bottom-Navigation, durchgängig ≥44px große Touch-Targets (Daumenfreundlichkeit), Bestätigungsdialog nur bei kritischen Aktionen wie Logout, Lade-/Fehlerzustände als Feedback bei jeder Interaktion, Dark Mode & Kontrast-Umschalter |

### Extras über die Vorgaben hinaus

Die Vorgaben verlangten fünf Kernfunktionen plus gutes Design. Umgesetzt wurde stattdessen eine **vollständige, produktionsnahe Anwendung** mit echtem Backend, persistenter Datenhaltung, automatisierten Tests und CI/CD — kein Mock-Datensatz im Frontend, sondern eine REST-API mit eigener Datenbank, die alle Geschäftsregeln serverseitig durchsetzt.

#### 🎟️ Echtes Ticket-System statt Simulation

- Tickets sind keine reine UI-Animation, sondern werden serverseitig in einer eigenen `tickets`-Tabelle angelegt, per Fremdschlüssel an Event *und* Käufer gebunden.
- **Kapazitätsprüfung mit Race-Condition-Schutz**: Ein Kauf-Versuch für ein ausgebuchtes Event wird mit `409 Conflict` abgelehnt (`backend/routes/tickets.js`). Insert des Tickets *und* Hochzählen von `events.attendees` laufen dabei in einer einzigen `db.transaction(...)` — würde man das stattdessen als zwei separate Schreibzugriffe ausführen, könnten bei (fast) gleichzeitigen Käufen mehr Tickets verkauft werden als Plätze vorhanden sind. Die Transaktion macht den Kauf atomar.
- **Echte, scanbare QR-Codes** pro Ticket — clientseitig generiert, kein externer Dienst, kein API-Limit.
- **Organizer-Dashboard**: Veranstalter sehen Teilnehmerzahlen, Kapazitätsauslastung und Einnahmen ihrer eigenen Events live aus der Datenbank, nicht aus statischen Werten.

#### 🔐 Sicherheit, die über "Login klappt" hinausgeht

- Passwörter werden mit **bcrypt** gehasht (nie im Klartext gespeichert), Login/Registrierung über `POST /api/auth/*`.
- **JWT-Auth zentral über eine gemeinsame Middleware** (`backend/middleware/auth.js`) — jede geschützte Route prüft den Token identisch, statt die Logik mehrfach zu duplizieren.
- **Profil-Updates mit echter Konfliktbehandlung**: Ändert ein Nutzer Benutzername oder E-Mail auf einen bereits vergebenen Wert, fängt das Backend den `UNIQUE constraint`-Fehler der Datenbank ab und antwortet mit einer verständlichen `409`-Fehlermeldung statt eines rohen SQL-Fehlers oder eines stillen Fehlschlags.
- **Validierte Datei-Uploads**: Bild-Uploads werden client- *und* serverseitig auf Dateityp (JPG/PNG/WebP) und Größe (max. 5 MB) geprüft. Ein dokumentierter Workaround für ein bekanntes Verhalten von `multer@1.x` (ein per `cb(error)` abgelehnter Dateifilter kann den Request hängen lassen) sorgt dafür, dass fehlerhafte Uploads zuverlässig als JSON-Fehler statt als hängende Verbindung oder Express-HTML-Fehlerseite beim Client ankommen (`backend/routes/events.js`).

#### 🗄️ Datenbankdesign nach Lehrbuch

- **Normalisiertes Schema** mit drei Tabellen (`users`, `events`, `tickets`) und expliziten Fremdschlüsselbeziehungen entlang der 1:n-Kardinalitäten (`events.organizer_id → users.id`, `tickets.event_id → events.id`, `tickets.user_id → users.id`) statt redundanter, eingebetteter Daten.
- **Fremdschlüssel-Constraints tatsächlich aktiv** — `PRAGMA foreign_keys = ON` wird explizit gesetzt; SQLite erzwingt referentielle Integrität standardmäßig *nicht*, dieser Schritt wird in der Praxis häufig vergessen.
- **Bewusster Kompromiss bei der Preis-Spalte**: Preise werden als lesbarer Anzeige-String gespeichert (`"29,00"`, `"Kostenlos"`), zusätzlich aber als numerischer Wert in `price_value` dupliziert. Grund: Ein `ORDER BY` auf der reinen Text-Spalte würde lexikographisch sortieren — `"100,00"` käme vor `"29,00"`, weil `"1" < "2"` als Zeichen gilt. Die zusätzliche numerische Spalte ist eine bewusste, kommentierte Abweichung von strikter Normalisierung zugunsten korrekter Sortierung (`backend/utils/price.js`).
- **Abwärtskompatible Schema-Migrationen**: Wird die App auf einer bereits existierenden, älteren Datenbank gestartet (z. B. ohne die Spalten `price_value` oder `interests`), erkennt `backend/database.js` das über `PRAGMA table_info` und ergänzt die fehlenden Spalten samt Backfill der vorhandenen Zeilen — statt die Datenbank zu löschen und neu zu erzeugen, was im Betrieb Datenverlust bedeuten würde.

#### ✅ Qualitätssicherung, wie sie in echten Projekten verlangt wird

- **68 automatisierte Tests**: 49 Backend-Tests (`auth`, `events`, `tickets`, `users`, `price`) mit Vitest + Supertest gegen eine isolierte In-Memory-SQLite-Datenbank, die nie die lokale `meetngo.db` berührt; 19 Frontend-Tests (API-Client, Events, Tickets, `AuthContext`) mit Vitest + React Testing Library.
- **GitHub-Actions-CI-Pipeline** (`.github/workflows/ci.yml`): Jeder Push und Pull Request durchläuft automatisch Install → Lint → Typecheck → Test → Build — schlägt einer dieser Schritte fehl, ist das in der PR sofort sichtbar, bevor fehlerhafter Code in `master` landet.
- **TypeScript im `strict`-Modus** für das gesamte Frontend, **ESLint + Prettier** für Frontend und Backend mit einheitlicher Konfiguration.

#### 📱 Mobile-UX-Details, die über "sieht gut aus" hinausgehen

- **Durchgängig ≥44×44px große Touch-Targets** (Android-Design-Richtlinie für sichere Daumenbedienung), konsequent auch bei Icon-Buttons in Headern und Floating-Buttons.
- **Bestätigungsdialoge gezielt nur bei kritischen/destruktiven Aktionen** (Logout, Event löschen), nicht bei jedem Tap — vermeidet "Dialog-Müdigkeit" und unnötige Reibung.
- **Sichtbares Feedback bei jeder Interaktion**: Lade- und Fehlerzustände statt stiller Wartezeiten, z. B. "Speichern…" während des Profil-Updates oder klare Fehlertexte bei fehlgeschlagenem Login.
- **Barrierefreiheits-Menü** mit Dark Mode und Hoch-Kontrast-Modus, jederzeit über einen fixen Button erreichbar, unabhängig vom aktuellen Screen.
- **Bottom-Navigation** für einhändige Bedienung auf großen Smartphone-Displays statt einer oberen Navigationsleiste.

#### 📦 Android-Build vorbereitet

Die App lässt sich über Capacitor direkt als native Android-App verpacken, ohne den React-Code umzuschreiben (siehe [Android-Build](#android-build-capacitor)).

## Tech-Stack

| Bereich | Technologien |
|---|---|
| Frontend | React 18, TypeScript (strict), Vite 6, Tailwind CSS 4, React Router 7 |
| UI-Komponenten | Radix UI (shadcn-Pattern), Lucide Icons, React-Leaflet |
| Backend | Node.js, Express 4, better-sqlite3, JWT, bcryptjs, Multer |
| Tests | Vitest, React Testing Library, Supertest |
| Tooling | ESLint, Prettier, pnpm Workspaces, GitHub Actions |

## Projektstruktur

```
meetngo/
├── frontend/                       # React/Vite App (Android UI)
│   ├── src/
│   │   ├── api/                    # API-Client und Service-Funktionen
│   │   │   ├── client.ts           # Fetch-Wrapper mit JWT-Authentifizierung
│   │   │   ├── auth.ts             # login(), register()
│   │   │   ├── events.ts           # CRUD-Operationen für Events
│   │   │   ├── tickets.ts          # createTicket(), getMyTickets()
│   │   │   └── users.ts            # getMyProfile(), updateProfile()
│   │   ├── app/
│   │   │   ├── components/         # Wiederverwendbare UI-Komponenten
│   │   │   │   └── ui/             # shadcn/Radix-Primitives (generiert)
│   │   │   └── screens/            # App-Screens (Login, Map, Search, …)
│   │   ├── context/
│   │   │   └── AuthContext.tsx     # Globaler Auth-State (JWT + User)
│   │   ├── tests/setup.ts          # Vitest-Setup (jest-dom Matcher)
│   │   └── main.tsx
│   ├── *.test.ts(x)                # Unit-/Komponententests, neben dem Code
│   ├── eslint.config.js
│   ├── vitest.config.ts
│   ├── vite.config.ts              # Vite + Proxy zu Backend (Port 3001)
│   └── package.json
│
├── backend/                        # Express.js REST-API
│   ├── app.js                      # Express-App (ohne .listen — testbar)
│   ├── server.js                   # Einstiegspunkt, startet app.js auf Port 3001
│   ├── database.js                 # SQLite via better-sqlite3 + Seed-Daten
│   ├── middleware/auth.js          # JWT-Auth-Middleware (geteilt von allen Routen)
│   ├── utils/price.js              # Preis-String → numerischer Wert (Sortierung)
│   ├── routes/
│   │   ├── auth.js                 # POST /api/auth/register, /api/auth/login
│   │   ├── events.js               # GET/POST/DELETE /api/events
│   │   ├── tickets.js              # GET/POST /api/tickets
│   │   └── users.js                # GET/PATCH /api/users/me
│   ├── tests/                      # Vitest + Supertest (isolierte In-Memory-DB)
│   ├── uploads/                    # Hochgeladene Event-Bilder (gitignored)
│   ├── .env.example                # Vorlage für lokale .env
│   └── package.json
│
├── .github/workflows/ci.yml        # Lint, Typecheck, Test, Build bei jedem Push/PR
├── guidelines/                     # Design-Guidelines aus Figma
├── pnpm-workspace.yaml             # pnpm Monorepo-Konfiguration
├── package.json                    # Root-Skripte (dev, build, lint, test, …)
└── .gitignore
```

## Voraussetzungen

- **Node.js** 18 oder neuer
- **pnpm** (empfohlen): `npm install -g pnpm`

## Installation

```bash
git clone https://github.com/dominikwipfler/meetngo.git
cd meetngo
pnpm install
```

Das installiert die Dependencies für Root, `frontend/` und `backend/` in einem Schritt (pnpm-Workspace).

## Umgebungsvariablen

Das Backend liest Konfiguration aus einer `.env`-Datei (siehe [`backend/.env.example`](backend/.env.example)):

```bash
cd backend
cp .env.example .env
```

Einen sicheren `JWT_SECRET` generieren:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

Ohne gesetztes `JWT_SECRET` startet der Server trotzdem (mit unsicherem Default-Wert) und gibt eine Warnung aus — praktisch für schnelles lokales Ausprobieren, aber nicht für den produktiven Einsatz gedacht.

## Ausführung

### Beide gleichzeitig (empfohlen)

```bash
pnpm dev
```

Startet Backend (Port 3001) und Frontend (Port 5173) gleichzeitig. Anschließend `http://localhost:5173` öffnen.

### Einzeln starten

```bash
pnpm dev:backend    # nur Backend
pnpm dev:frontend   # nur Frontend
```

Vite proxied alle `/api`- und `/uploads`-Anfragen automatisch zum Backend.

## Tests

```bash
pnpm test                       # Frontend- und Backend-Tests
pnpm --filter frontend test     # nur Frontend (Vitest + React Testing Library)
pnpm --filter backend test      # nur Backend (Vitest + Supertest)
```

Backend-Tests laufen gegen eine isolierte In-Memory-SQLite-Datenbank (`DB_PATH=:memory:`) und beeinflussen nie die lokale `meetngo.db`.

## Code-Qualität

```bash
pnpm lint        # ESLint (Frontend + Backend)
pnpm lint:fix     # ESLint mit Autofix
pnpm format       # Prettier
pnpm typecheck    # TypeScript --strict (Frontend)
```

shadcn/Radix-UI-Primitives unter `frontend/src/app/components/ui/` sind generierter Code und bewusst von Prettier sowie der `react-refresh`-ESLint-Regel ausgenommen.

## CI/CD

Jeder Push und Pull Request auf `master` durchläuft via [GitHub Actions](.github/workflows/ci.yml): Install → Lint → Typecheck → Test → Build.

## API-Übersicht

Das Backend läuft auf `http://localhost:3001`.

| Methode | Endpoint | Beschreibung |
|--------|----------|-------------|
| POST | `/api/auth/register` | Neues Konto erstellen |
| POST | `/api/auth/login` | Einloggen, gibt JWT zurück |
| GET | `/api/events` | Events abrufen (mit Filter/Sort) |
| GET | `/api/events/:id` | Ein Event abrufen |
| POST | `/api/events` | Event erstellen (Auth + Bild-Upload) |
| DELETE | `/api/events/:id` | Event löschen (nur Ersteller) |
| GET | `/api/tickets` | Eigene Tickets abrufen (Auth) |
| POST | `/api/tickets` | Ticket für ein Event kaufen (Auth, `{ eventId }`) |
| GET | `/api/users/me` | Eigenes Profil abrufen (Auth) |
| PATCH | `/api/users/me` | Profil aktualisieren (Auth, `{ username?, email?, interests? }`) |

### Filter-Parameter für `GET /api/events`

| Parameter | Werte | Beschreibung |
|-----------|-------|-------------|
| `search` | Freitext | Sucht in Name, Ort, Beschreibung, Veranstalter |
| `category` | Musik, Sport, … | Kategorie-Filter |
| `sort` | `date`, `name`, `attendees`, `price` | Sortierfeld (`price` sortiert numerisch über die interne `price_value`-Spalte) |
| `order` | `asc`, `desc` | Sortierreihenfolge |
| `priceFilter` | `free`, `paid` | Kostenlos / Kostenpflichtig |

## Datenbank

Die SQLite-Datenbank wird beim ersten Start automatisch unter `backend/meetngo.db` erstellt und mit Demo-Events befüllt. Die Datei ist gitignored und wird **nicht** versioniert.

Preise werden als Anzeige-String gespeichert (z. B. `"29,00"` oder `"Kostenlos"`) plus einer numerischen `price_value`-Spalte für korrekte Sortierung — ein reiner Textvergleich würde z. B. `"100,00"` vor `"29,00"` einsortieren.

Hochgeladene Event-Bilder liegen unter `backend/uploads/` und sind ebenfalls gitignored.

Tickets sind in einer eigenen `tickets`-Tabelle abgelegt (`event_id`, `user_id`, `status`) und per Foreign Key an Events und Nutzer gebunden. Ein Kauf erhöht `events.attendees` und schlägt fehl (`409`), sobald `events.capacity` erreicht ist.

Nutzer-Interessen liegen als JSON-Array in der `interests`-Spalte der `users`-Tabelle. Änderungen an Benutzername oder E-Mail werden auf Eindeutigkeit geprüft (`409` bei Konflikt).

## Architektur

- **Frontend ↔ Backend**: Das Frontend spricht ausschließlich über die REST-API (`/api/...`) mit dem Backend; Vite proxied im Dev-Modus dorthin. Auth-Status liegt in `AuthContext` (React Context) und im `localStorage` (JWT + User-Objekt).
- **Routing**: React Router v7 mit geschützten Routen (`ProtectedRoute`) für alles außer Login/Registrierung.
- **Backend-Schichten**: `server.js` (Prozess-Einstieg) → `app.js` (Express-App, ohne Seiteneffekt testbar) → `routes/` (HTTP-Handler) → `database.js` (SQLite-Zugriff). Diese Trennung erlaubt es, `app.js` in Tests direkt mit Supertest anzusprechen, ohne einen echten Port zu öffnen.
- **Auth**: JWT im `Authorization: Bearer …`-Header, serverseitig per gemeinsamer Middleware (`middleware/auth.js`) geprüft; Events kennen ihren Ersteller über `organizer_id` für Lösch-Berechtigungen, Tickets ihren Käufer über `user_id`.
- **Ticket-Kauf**: Insert + Attendees-Update laufen in einer SQLite-Transaktion (`db.transaction(...)`), damit beide Schreiboperationen atomar zusammen erfolgen.

## Android-Build (Capacitor)

Da die App mit React/Vite gebaut ist, kann sie über Capacitor als native Android-App verpackt werden:

```bash
cd frontend
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init MeetNGo com.meetngo.app --web-dir dist
npm run build
npx cap add android
npx cap open android
```

> Das Backend muss auf einem erreichbaren Server laufen. Die API-Basis-URL in `frontend/src/api/client.ts` muss für den Android-Build auf die Serveradresse angepasst werden (z. B. `http://192.168.1.x:3001`).

## Screenshots

<!-- TODO: Screenshots der App hier einfügen, z. B.: -->
<!-- ![Karte](docs/screenshots/map.png) ![Tickets](docs/screenshots/tickets.png) -->

## Design

Das UI-Design basiert auf dem Figma-Projekt:
https://www.figma.com/design/XFbfqoyFt7IFZrnVYUjUzJ/MeetNGo-Mobile-App-UI

Verwendete Drittanbieter-Komponenten und -Inhalte: siehe [`frontend/ATTRIBUTIONS.md`](frontend/ATTRIBUTIONS.md) (shadcn/ui, Unsplash).
