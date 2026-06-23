# MeetNGo

[![CI](https://github.com/dominikwipfler/meetngo/actions/workflows/ci.yml/badge.svg)](https://github.com/dominikwipfler/meetngo/actions/workflows/ci.yml)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-≥18-339933?logo=node.js&logoColor=white)
![Express](https://img.shields.io/badge/Express-4-000000?logo=express&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-better--sqlite3-003B57?logo=sqlite&logoColor=white)

Eine mobile Event-Discovery-App für Android: Events auf einer interaktiven Karte finden, durchsuchen, filtern, Tickets mit QR-Code "kaufen" und eigene Events veranstalten. Native Android-App mit Jetpack Compose, Backend mit Express und SQLite.

---

## Inhalt

- [Schnellstart](#schnellstart)
- [Features](#features)
- [Vorgaben & Extras](#vorgaben--extras)
- [Tech-Stack](#tech-stack)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Installation](#installation)
- [Umgebungsvariablen](#umgebungsvariablen)
- [Backend ausführen](#backend-ausführen)
- [Android-App ausführen](#android-app-ausführen)
- [Tests](#tests)
- [Code-Qualität](#code-qualität)
- [CI/CD](#cicd)
- [API-Übersicht](#api-übersicht)
- [Datenbank](#datenbank)
- [Architektur](#architektur)
- [Design](#design)
- [Bekannte Verbesserungspunkte](#bekannte-verbesserungspunkte)

---

## Schnellstart

Du brauchst **Node.js ≥ 18** und **pnpm** (`npm install -g pnpm`). Für die Android-App zusätzlich **Android Studio** inkl. Android SDK und **JDK 17**.

```bash
git clone https://github.com/dominikwipfler/meetngo.git
cd meetngo
pnpm install
pnpm dev
```

Das Backend läuft danach auf **http://localhost:3001** — **ohne weitere Konfiguration**:

- Es ist **keine `.env` nötig**, um loszulegen: In der Entwicklung erzeugt das Backend bei fehlendem `JWT_SECRET` automatisch ein lokales Secret (`backend/.jwt-dev-secret`, gitignored). Für einen Produktiv-Betrieb muss `JWT_SECRET` gesetzt werden — siehe [Umgebungsvariablen](#umgebungsvariablen).
- Die SQLite-Datenbank wird beim ersten Start automatisch unter `backend/meetngo.db` angelegt und mit Demo-Events befüllt.

Für die **Android-App** (das laufende Backend vorausgesetzt):

```bash
cd android
./gradlew installDebug   # baut & installiert die App auf laufendem Emulator/Gerät
```

Alternativ `android/` in Android Studio öffnen und die App auf einem Emulator starten. Die App verbindet sich automatisch über `10.0.2.2:3001` mit dem Backend. Details: [Android-App ausführen](#android-app-ausführen).

---

## Features

- **Authentifizierung** — Registrierung & Login mit JWT, Passwörter werden mit bcrypt gehasht
- **Kartenansicht** — Events als farbcodierte Marker nach Kategorie auf einer interaktiven OpenStreetMap-Karte (osmdroid), deren Kacheln über einen lokalen Backend-Proxy geladen werden
- **Suche & Filter** — Volltextsuche, Kategorie-Filter, Preisfilter (kostenlos/kostenpflichtig), Sortierung nach Datum, Name, Beliebtheit oder Preis
- **Event-Details** — Detailansicht mit Ticket-Kauf-Flow (Mengenwahl, Zahlungsmethoden-Auswahl)
- **Echtes Ticket-System** — Tickets werden serverseitig angelegt (eigene `tickets`-Tabelle, an Event und Käufer gebunden), Kapazitätsgrenzen werden respektiert, Teilnehmerzahl wird automatisch hochgezählt
- **Tickets mit QR-Code** — jedes Ticket erzeugt einen echten, scanbaren QR-Code (in der App generiert, kein externer Dienst); ein integrierter Scanner ermöglicht den Check-in
- **Eigene Events erstellen** — inkl. Bild-Upload (JPG/PNG/WebP, max. 5 MB), Kategorie, Preis, Kapazität
- **Favoriten & Folgen** — Events favorisieren und Veranstaltern folgen
- **Organizer-Dashboard** — Statistiken und Verwaltung der eigenen Events
- **Profil & Einstellungen** — Benutzername, E-Mail und Interessen werden serverseitig gespeichert (`PATCH /api/users/me`), Passwortänderung, Dark Mode
- **Mobile-First-UI** — Bottom-Navigation, für Android-Bildschirme optimiert

## Vorgaben & Extras

### Pflicht-Features

| Vorgabe | Umsetzung |
|---|---|
| Login/out | JWT-Login (`LoginScreen`) und Logout mit Sicherheitsabfrage (`ProfileScreen` → Bestätigungsdialog → Token-Reset im `AuthRepository`) |
| Startseite mit Karte | Die Karte (`MapScreen`) ist die geschützte Landing-Route direkt nach dem Login; interaktive osmdroid-Karte mit kategorie-farbigen Markern, Live-Suche und Filtern |
| Event Infos einsehbar | `EventDetailScreen` zeigt Bild, Beschreibung, Datum, Ort, Preis, Kapazität/Teilnehmerzahl und Veranstalter; erreichbar von Karte, Suche und Profil |
| Event erstellen → Bild hochladen | `CreateEventScreen` mit Bild-Upload (JPG/PNG/WebP, serverseitig per Multer auf Typ und 5 MB validiert und gespeichert unter `backend/uploads/`) |
| Bei der Suche filtern können | `SearchScreen`: Volltextsuche, Kategorie-Filter, Preisfilter (kostenlos/kostenpflichtig), Sortierung nach Datum/Name/Beliebtheit/Preis — zusätzlich auch direkt auf der Kartenansicht filterbar |
| Gutes Design & Nutzerfreundlichkeit | Mobile-First-UI mit Bottom-Navigation, große Touch-Targets, Bestätigungsdialog nur bei kritischen Aktionen wie Logout, Lade-/Fehlerzustände als Feedback bei jeder Interaktion, Dark Mode |

### Extras über die Vorgaben hinaus

Die Vorgaben verlangten fünf Kernfunktionen plus gutes Design. Umgesetzt wurde stattdessen eine **vollständige, produktionsnahe Anwendung** mit echtem Backend, persistenter Datenhaltung, automatisierten Tests und CI/CD — kein Mock-Datensatz in der App, sondern eine REST-API mit eigener Datenbank, die alle Geschäftsregeln serverseitig durchsetzt.

#### 🎟️ Echtes Ticket-System statt Simulation

- Tickets sind keine reine UI-Animation, sondern werden serverseitig in einer eigenen `tickets`-Tabelle angelegt, per Fremdschlüssel an Event *und* Käufer gebunden.
- **Kapazitätsprüfung mit Race-Condition-Schutz**: Ein Kauf-Versuch für ein ausgebuchtes Event wird mit `409 Conflict` abgelehnt (`backend/routes/tickets.js`). Insert des Tickets *und* Hochzählen von `events.attendees` laufen dabei in einer einzigen `db.transaction(...)` — würde man das stattdessen als zwei separate Schreibzugriffe ausführen, könnten bei (fast) gleichzeitigen Käufen mehr Tickets verkauft werden als Plätze vorhanden sind. Die Transaktion macht den Kauf atomar.
- **Ein Ticket pro Nutzer und Event**: Ein zweiter Kauf desselben Events durch denselben Nutzer wird mit `409 Conflict` abgelehnt — das verhindert verfälschte Teilnehmerzahlen und doppelte QR-Codes für dieselbe Person.
- **Echte, scanbare QR-Codes** pro Ticket — in der App generiert, kein externer Dienst, kein API-Limit. Ein eingebauter Scanner (`ScannerScreen`) erlaubt den Ticket-Check-in (`POST /api/tickets/:id/checkin`).
- **Organizer-Dashboard**: Veranstalter sehen Teilnehmerzahlen, Kapazitätsauslastung und Einnahmen ihrer eigenen Events live aus der Datenbank, nicht aus statischen Werten.

#### 🗺️ Karte, die auch im Emulator funktioniert

- Die Karte nutzt **osmdroid** mit OpenStreetMap-Kacheln. Im Emulator-Setup kann der Emulator jedoch keine externen DNS-Namen auflösen — die Kacheln von `tile.openstreetmap.org` würden nie ankommen und die Karte bliebe grau.
- Lösung: Ein **lokaler Kachel-Proxy** im Backend (`backend/routes/tiles.js`) holt die Kacheln über den Host (mit Internet) und reicht sie an die App durch — erreichbar über `10.0.2.2:3001/tiles/:z/:x/:y.png`. Die App setzt diese URL als osmdroid-`XYTileSource` (siehe `MapScreen.kt`).
- Der Proxy validiert die `z/x/y`-Koordinaten (Schutz vor Path-Traversal/SSRF), sendet einen aussagekräftigen `User-Agent` (von der OSM-Nutzungsrichtlinie verlangt) und legt die Kacheln in einem **Festplatten-Cache** (`backend/tiles-cache/`) ab, damit der OSM-Server nicht bei jedem Karten-Schwenk neu angefragt wird.

#### 🔐 Sicherheit, die über "Login klappt" hinausgeht

- Passwörter werden mit **bcrypt** gehasht (nie im Klartext gespeichert), Login/Registrierung über `POST /api/auth/*`.
- **Brute-Force-Schutz**: Auth-Endpunkte sind rate-limited (max. 10 Versuche pro IP je 15 Minuten, `backend/middleware/rateLimit.js`).
- **JWT-Auth zentral über eine gemeinsame Middleware** (`backend/middleware/auth.js`) — jede geschützte Route prüft den Token identisch, statt die Logik mehrfach zu duplizieren.
- **Security-Header** werden über eine eigene Middleware gesetzt (`backend/middleware/securityHeaders.js`).
- **Profil-Updates mit echter Konfliktbehandlung**: Ändert ein Nutzer Benutzername oder E-Mail auf einen bereits vergebenen Wert, fängt das Backend den `UNIQUE constraint`-Fehler der Datenbank ab und antwortet mit einer verständlichen `409`-Fehlermeldung statt eines rohen SQL-Fehlers.
- **Validierte Datei-Uploads**: Bild-Uploads werden serverseitig auf Dateityp (JPG/PNG/WebP) und Größe (max. 5 MB) geprüft. Ein dokumentierter Workaround für ein bekanntes Verhalten von `multer@1.x` sorgt dafür, dass fehlerhafte Uploads zuverlässig als JSON-Fehler statt als hängende Verbindung ankommen (`backend/routes/events.js`).
- **Berechtigungsprüfung beim Löschen**: Nur der Ersteller eines Events darf es löschen (`organizer_id`-Abgleich gegen den eingeloggten Nutzer) — jeder andere Versuch wird mit `403 Forbidden` abgelehnt.
- **Keine verwaisten Dateien**: Schlägt die Validierung beim Event-Erstellen fehl, wird ein bereits hochgeladenes Bild sofort wieder von der Platte gelöscht; löscht ein Veranstalter sein Event, wird auch das zugehörige Bild entfernt.
- **Sicheres `JWT_SECRET`-Handling je Umgebung** (`backend/middleware/auth.js`): In Produktion (`NODE_ENV=production`) ist das Secret zwingend — fehlt es, bricht der Start ab, statt mit einem erratbaren Schlüssel Tokens zu signieren. In der Entwicklung wird stattdessen automatisch ein lokales, persistiertes Zufalls-Secret erzeugt, damit ein frisch geklontes Projekt sofort startfähig ist, ohne dass je ein hartkodierter Default im Code steht.

#### 🗄️ Datenbankdesign nach Lehrbuch

- **Normalisiertes Schema** mit Fremdschlüsselbeziehungen entlang der 1:n-Kardinalitäten (`events.organizer_id → users.id`, `tickets.event_id → events.id`, `tickets.user_id → users.id`) statt redundanter, eingebetteter Daten.
- **Fremdschlüssel-Constraints tatsächlich aktiv** — `PRAGMA foreign_keys = ON` wird explizit gesetzt; SQLite erzwingt referentielle Integrität standardmäßig *nicht*, dieser Schritt wird in der Praxis häufig vergessen.
- **Bewusster Kompromiss bei der Preis-Spalte**: Preise werden als lesbarer Anzeige-String gespeichert (`"29,00"`, `"Kostenlos"`), zusätzlich aber als numerischer Wert in `price_value` dupliziert. Grund: Ein `ORDER BY` auf der reinen Text-Spalte würde lexikographisch sortieren — `"100,00"` käme vor `"29,00"`. Die zusätzliche numerische Spalte ist eine bewusste, kommentierte Abweichung von strikter Normalisierung zugunsten korrekter Sortierung (`backend/utils/price.js`).
- **Abwärtskompatible Schema-Migrationen**: Wird die App auf einer bereits existierenden, älteren Datenbank gestartet, erkennt `backend/database.js` fehlende Spalten über `PRAGMA table_info` und ergänzt sie samt Backfill — statt die Datenbank zu löschen und neu zu erzeugen.

#### ✅ Qualitätssicherung, wie sie in echten Projekten verlangt wird

- **Automatisierte Backend-Tests** mit Vitest + Supertest (`auth`, `events`, `tickets`, `users`, `price`, `tiles`, `rateLimit`, `securityHeaders`) gegen eine isolierte In-Memory-SQLite-Datenbank, die nie die lokale `meetngo.db` berührt.
- **GitHub-Actions-CI-Pipeline** (`.github/workflows/ci.yml`): Jeder Push und Pull Request durchläuft automatisch Install → Lint → Test für das Backend sowie einen Debug-APK-Build der Android-App.

## Tech-Stack

| Bereich | Technologien |
|---|---|
| Android-App | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| Karte | osmdroid (OpenStreetMap), Backend-Kachel-Proxy |
| Netzwerk | Retrofit, OkHttp, Gson |
| Backend | Node.js, Express 4, better-sqlite3, JWT, bcryptjs, Multer |
| Tests | Vitest, Supertest (Backend); JUnit (Android, `src/test/`) |
| Tooling | ESLint, Prettier, pnpm, Gradle (mit Version Catalog), GitHub Actions |

## Projektstruktur

```
meetngo/
├── android/                              # Native Android-App (Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/meetngo/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── api/
│   │   │   │   │   ├── ApiClient.kt       # Retrofit/OkHttp-Setup, BASE_URL (10.0.2.2)
│   │   │   │   │   └── ApiService.kt      # Alle REST-Endpunkte als Retrofit-Interface
│   │   │   │   ├── model/Models.kt        # Datenmodelle (Event, Ticket, User, …)
│   │   │   │   └── repository/            # z. B. AuthRepository (hält JWT)
│   │   │   ├── ui/
│   │   │   │   ├── navigation/            # NavGraph, BottomNavBar
│   │   │   │   ├── screens/               # auth, map, search, eventdetail,
│   │   │   │   │                          #   createevent, tickets, scanner,
│   │   │   │   │                          #   organizer, profile
│   │   │   │   └── theme/                 # Compose-Theme, Dark Mode
│   │   │   ├── util/
│   │   │   └── res/xml/network_security_config.xml  # erlaubt Klartext-HTTP zu 10.0.2.2
│   │   ├── src/test/java/com/meetngo/app/  # JUnit-Unit-Tests (reine Kotlin-Logik, kein Android-SDK nötig)
│   │   └── build.gradle.kts              # applicationId com.meetngo.app, minSdk 26, targetSdk 34
│   ├── gradle/libs.versions.toml         # zentraler Gradle Version Catalog
│   ├── build.gradle.kts                  # AGP 8.5.2, Kotlin 1.9.24
│   └── gradlew / gradlew.bat
│
├── backend/                              # Express.js REST-API
│   ├── app.js                            # Express-App (ohne .listen — testbar)
│   ├── server.js                         # Einstiegspunkt, startet app.js auf Port 3001
│   ├── database.js                       # SQLite via better-sqlite3 + Seed-Daten
│   ├── middleware/
│   │   ├── auth.js                       # JWT-Auth-Middleware
│   │   ├── rateLimit.js                  # Brute-Force-Schutz für Auth-Routen
│   │   └── securityHeaders.js            # Security-Header
│   ├── utils/                            # price.js (Sortierung), dbErrors.js
│   ├── routes/
│   │   ├── auth.js                       # POST /api/auth/register, /api/auth/login
│   │   ├── events.js                     # CRUD /api/events + Favoriten
│   │   ├── tickets.js                    # /api/tickets + Check-in
│   │   ├── users.js                      # /api/users/me + Folgen
│   │   └── tiles.js                      # OSM-Kachel-Proxy (/tiles/:z/:x/:y.png)
│   ├── tests/                            # Vitest + Supertest (isolierte In-Memory-DB)
│   ├── uploads/                          # Hochgeladene Event-Bilder (gitignored)
│   ├── tiles-cache/                      # Festplatten-Cache des Kachel-Proxys (gitignored)
│   ├── .env.example                      # Vorlage für lokale .env
│   └── package.json
│
├── .github/workflows/ci.yml              # Backend Lint/Test + Android-APK-Build
├── guidelines/                           # Design-Guidelines
├── package.json                          # Root-Skripte (delegieren ans Backend)
└── .gitignore
```

## Voraussetzungen

- **Node.js** 18 oder neuer (Backend)
- **pnpm**: `npm install -g pnpm`
- **Android Studio** (für die App) inkl. Android SDK; ein Emulator (AVD) oder ein per ADB verbundenes Gerät
- **JDK 17** (für den Gradle-Build)

## Installation

```bash
git clone https://github.com/dominikwipfler/meetngo.git
cd meetngo
pnpm install
```

`pnpm install` installiert die Backend-Dependencies. Die Android-App wird separat über Android Studio bzw. Gradle gebaut (siehe [Android-App ausführen](#android-app-ausführen)).

## Umgebungsvariablen

**Für die lokale Entwicklung ist keine Konfiguration nötig** — `pnpm dev` läuft direkt nach dem Clone (siehe [Schnellstart](#schnellstart)). Optional liest das Backend Konfiguration aus einer `.env`-Datei (Vorlage: [`backend/.env.example`](backend/.env.example)):

```bash
cp backend/.env.example backend/.env
```

| Variable | Beschreibung | Default |
|---|---|---|
| `PORT` | Port, auf dem das Backend lauscht | `3001` |
| `JWT_SECRET` | Geheimnis zum Signieren der JWTs | In der Entwicklung automatisch erzeugt; in **Produktion erforderlich** |

**`JWT_SECRET` – Verhalten je nach Umgebung:**

- **Entwicklung/Test** (Default): Ist `JWT_SECRET` nicht gesetzt, erzeugt das Backend einmalig ein zufälliges Secret und legt es unter `backend/.jwt-dev-secret` (gitignored) ab, sodass ausgestellte Logins auch über Neustarts hinweg gültig bleiben. Es ist also nichts zu tun.
- **Produktion** (`NODE_ENV=production`): `JWT_SECRET` ist **zwingend** — fehlt es, bricht der Start mit einem Fehler ab (`backend/middleware/auth.js`), damit der Server nie mit einem erratbaren Schlüssel Tokens signiert.

Einen sicheren `JWT_SECRET` für die Produktion generieren:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

`backend/.env` ist gitignored und darf keine echten Geheimnisse im Repo hinterlassen.

## Backend ausführen

```bash
pnpm install      # einmalig, im Projekt-Root
pnpm dev          # startet das Backend im Watch-Modus (nodemon)
```

Das Backend lauscht anschließend auf `http://localhost:3001`. Alternativ direkt im Backend-Verzeichnis:

```bash
cd backend
npm run dev       # nodemon (Watch-Modus)
npm start         # ohne Watch
```

Beim ersten Start wird die SQLite-Datenbank automatisch unter `backend/meetngo.db` angelegt und mit Demo-Events befüllt.

## Android-App ausführen

1. **Backend starten** (siehe oben) — die App benötigt die laufende API.
2. `android/` in **Android Studio** öffnen (Gradle-Sync abwarten) **oder** im Terminal bauen:

   ```bash
   cd android
   ./gradlew assembleDebug        # baut die Debug-APK
   ./gradlew installDebug         # installiert auf laufendem Emulator/Gerät
   ```

3. Einen **Emulator (AVD)** starten oder ein Gerät per ADB verbinden und die App ausführen.

### Netzwerk: 10.0.2.2 statt localhost

Der Android-Emulator erreicht den Host (auf dem das Backend läuft) **nicht** über `localhost` — das zeigt auf den Emulator selbst. Stattdessen wird die Sonderadresse `10.0.2.2` verwendet, die der Emulator auf den Host umleitet. Die Basis-URL ist deshalb fest auf

```
http://10.0.2.2:3001/
```

gesetzt (`android/app/src/main/java/com/meetngo/app/data/api/ApiClient.kt`). Da es sich um Klartext-HTTP handelt, ist `10.0.2.2` in `res/xml/network_security_config.xml` explizit für unverschlüsselten Verkehr freigegeben.

> **Echtes Gerät:** Auf einem physischen Gerät statt eines Emulators muss `BASE_URL` auf die LAN-IP des Entwicklungsrechners zeigen (z. B. `http://192.168.x.x:3001/`), und die `network_security_config.xml` muss diese Adresse zulassen.

### Karten-Kacheln (Tile-Proxy)

Die Karte lädt ihre OpenStreetMap-Kacheln **nicht** direkt von `tile.openstreetmap.org`, sondern über den Backend-Proxy unter `10.0.2.2:3001/tiles/...`. Grund: Der Emulator kann externe DNS-Namen nicht auflösen; das Backend (auf dem Host mit Internet) holt die Kacheln und reicht sie durch (`backend/routes/tiles.js`). Es muss daher kein zusätzlicher Dienst konfiguriert werden — das laufende Backend genügt.

### Event-Bilder (lokales Serving)

Hochgeladene Event-Bilder liegen unter `backend/uploads/` und werden vom Backend als statische Dateien unter `/uploads/...` ausgeliefert. Die App baut die Bild-URLs relativ zur `BASE_URL` (`10.0.2.2:3001`), sodass die Bilder im Emulator korrekt geladen werden, ohne dass ein externer Bild-Host/CDN nötig ist.

## Tests

```bash
pnpm test                    # Backend-Tests (Vitest + Supertest)
# oder direkt:
cd backend && npm test
```

Backend-Tests laufen gegen eine isolierte In-Memory-SQLite-Datenbank und beeinflussen nie die lokale `meetngo.db`.

```bash
cd android && ./gradlew testDebugUnitTest   # Android-Unit-Tests (JUnit, src/test/)
cd android && ./gradlew assembleDebug       # Debug-APK-Build
```

Die Android-Unit-Tests decken reine Kotlin-Logik ohne Android-Framework-Abhängigkeit ab (Datumsformatierung, Fehlermeldungs-Extraktion, JSON-Mapping der Datenmodelle). UI-/Instrumented-Tests (`src/androidTest/`) gibt es bisher nicht.

## Code-Qualität

```bash
pnpm lint        # ESLint (Backend)
pnpm lint:fix    # ESLint mit Autofix
pnpm format      # Prettier (Backend)
```

## CI/CD

Jeder Push und Pull Request auf `master` durchläuft via [GitHub Actions](.github/workflows/ci.yml) zwei Jobs:

- **build-and-test**: `pnpm install` → `pnpm lint` → `pnpm test` (Backend)
- **android-build**: JDK 17 + Gradle → `./gradlew testDebugUnitTest` (Android-Unit-Tests) → `./gradlew assembleDebug` (Android Debug-APK)

## API-Übersicht

Das Backend läuft auf `http://localhost:3001` (vom Emulator als `http://10.0.2.2:3001` erreichbar).

| Methode | Endpoint | Beschreibung |
|--------|----------|-------------|
| POST | `/api/auth/register` | Neues Konto erstellen, gibt JWT zurück |
| POST | `/api/auth/login` | Einloggen, gibt JWT zurück |
| GET | `/api/events` | Events abrufen (mit Filter/Sort) |
| GET | `/api/events/:id` | Ein Event abrufen |
| POST | `/api/events` | Event erstellen (Auth + Bild-Upload) |
| PATCH | `/api/events/:id` | Event-Felder aktualisieren (nur Ersteller) |
| DELETE | `/api/events/:id` | Event löschen (nur Ersteller) |
| GET | `/api/events/favorites` | Eigene favorisierte Events (Auth) |
| GET | `/api/events/:id/favorite-status` | Favoriten-Status eines Events (Auth) |
| POST/DELETE | `/api/events/:id/favorite` | Event favorisieren / entfavorisieren (Auth) |
| GET | `/api/tickets` | Eigene Tickets abrufen (Auth) |
| POST | `/api/tickets` | Ticket für ein Event kaufen (Auth, `{ eventId }`) |
| DELETE | `/api/tickets/:id` | Ticket stornieren (Auth) |
| POST | `/api/tickets/:id/checkin` | Ticket einchecken (Scanner) |
| GET | `/api/users/me` | Eigenes Profil abrufen (Auth) |
| PATCH | `/api/users/me` | Profil aktualisieren (Auth, `{ username?, email?, interests? }`) |
| PATCH | `/api/users/me/password` | Passwort ändern (Auth) |
| GET | `/api/users/:id/follow-status` | Folge-Status eines Veranstalters (Auth) |
| POST/DELETE | `/api/users/:id/follow` | Veranstalter folgen / entfolgen (Auth) |
| GET | `/tiles/:z/:x/:y.png` | OSM-Kachel-Proxy für die Karte |
| GET | `/uploads/:file` | Statisch ausgelieferte Event-Bilder |

### Filter-Parameter für `GET /api/events`

| Parameter | Werte | Beschreibung |
|-----------|-------|-------------|
| `search` | Freitext | Sucht in Name, Ort, Beschreibung, Veranstalter |
| `category` | Musik, Sport, … | Kategorie-Filter |
| `sort` | `date`, `name`, `attendees`, `price` | Sortierfeld (`price` sortiert numerisch über die interne `price_value`-Spalte) |
| `order` | `asc`, `desc` | Sortierreihenfolge |
| `priceFilter` | `free`, `paid` | Kostenlos / Kostenpflichtig |
| `organizerId` | Zahl | Nur Events eines bestimmten Veranstalters |

## Datenbank

Die SQLite-Datenbank wird beim ersten Start automatisch unter `backend/meetngo.db` erstellt und mit Demo-Events befüllt. Die Datei ist gitignored und wird **nicht** versioniert.

Preise werden als Anzeige-String gespeichert (z. B. `"29,00"` oder `"Kostenlos"`) plus einer numerischen `price_value`-Spalte für korrekte Sortierung.

Hochgeladene Event-Bilder liegen unter `backend/uploads/` und gecachte Karten-Kacheln unter `backend/tiles-cache/` — beide sind gitignored.

Tickets sind in einer eigenen `tickets`-Tabelle abgelegt (`event_id`, `user_id`, `status`) und per Foreign Key an Events und Nutzer gebunden. Ein Kauf erhöht `events.attendees` und schlägt fehl (`409`), sobald `events.capacity` erreicht ist oder der Nutzer bereits ein Ticket für dieses Event besitzt (ein Ticket pro Nutzer und Event).

Nutzer-Interessen liegen als JSON-Array in der `interests`-Spalte der `users`-Tabelle. Änderungen an Benutzername oder E-Mail werden auf Eindeutigkeit geprüft (`409` bei Konflikt).

## Architektur

Das Projekt besteht aus zwei Teilen: der **nativen Android-App** und dem **Express-Backend**.

- **Android ↔ Backend**: Die App spricht ausschließlich über die REST-API (`/api/...`) mit dem Backend. Der Netzwerk-Layer basiert auf Retrofit/OkHttp (`data/api/`); ein OkHttp-Interceptor hängt das JWT automatisch als `Authorization: Bearer …`-Header an jeden Request an. Das Token hält der `AuthRepository`/`ApiClient` als Singleton-Zustand.
- **UI**: Jetpack Compose mit Navigation Compose (`ui/navigation/NavGraph.kt`), Bottom-Navigation und einem zentralen Compose-Theme (inkl. Dark Mode). Screens liegen nach Feature getrennt unter `ui/screens/`.
- **ViewModel-Layer**: Die Auth-Screens (`LoginScreen`/`RegisterScreen`) halten ihren UI-Zustand in einem `ViewModel` mit `StateFlow` (`LoginViewModel`, `RegisterViewModel`) statt direkt im Composable — Validierung und API-/Repository-Aufrufe laufen dort, der Composable liest nur noch den Zustand und ruft Aktionen auf. Die übrigen Screens nutzen noch das einfachere Composable-lokale `remember`-Pattern (siehe „Bekannte Verbesserungspunkte").
- **Karte**: osmdroid mit einer eigenen `XYTileSource`, die auf den Backend-Kachel-Proxy (`/tiles/`) zeigt (siehe oben).
- **Backend-Schichten**: `server.js` (Prozess-Einstieg, lädt `.env`, startet den Listener) → `app.js` (Express-App, ohne Seiteneffekt testbar) → `routes/` (HTTP-Handler) → `database.js` (SQLite-Zugriff). Diese Trennung erlaubt es, `app.js` in Tests direkt mit Supertest anzusprechen, ohne einen echten Port zu öffnen.
- **Auth**: JWT im `Authorization: Bearer …`-Header, serverseitig per gemeinsamer Middleware (`middleware/auth.js`) geprüft; Events kennen ihren Ersteller über `organizer_id` für Lösch-Berechtigungen, Tickets ihren Käufer über `user_id`.
- **Ticket-Kauf**: Insert + Attendees-Update laufen in einer SQLite-Transaktion (`db.transaction(...)`), damit beide Schreiboperationen atomar zusammen erfolgen.

## Design

Das UI-Design basiert auf dem Figma-Projekt:
https://www.figma.com/design/XFbfqoyFt7IFZrnVYUjUzJ/MeetNGo-Mobile-App-UI

Design-Guidelines: siehe [`guidelines/Guidelines.md`](guidelines/Guidelines.md).

## Bekannte Verbesserungspunkte

Nicht alles ist fertig im Sinne von "nichts mehr zu tun" — bewusst zurückgestellte Punkte für eine spätere Iteration:

- **Build-Tooling**: `compileSdk`/`targetSdk = 34`, AGP 8.5.2, Kotlin 1.9.24 und Gradle 8.7 sind Stand Mitte 2024. Für eine echte Veröffentlichung müsste vor allem `targetSdk` auf das aktuelle Android-Level angehoben werden (Play-Store-Vorgabe); ein Wechsel auf Kotlin 2.x ändert zusätzlich, wie der Compose-Compiler eingebunden wird (eigenes Gradle-Plugin statt `kotlinCompilerExtensionVersion`).
- **Architektur**: Die ViewModel-Schicht ist bisher nur für die Auth-Screens eingeführt (siehe „Architektur" oben); die übrigen Screens (Karte, Suche, Tickets, Event-Erstellung, Organizer-Dashboard, Profil, Scanner) rufen Repository/API-Funktionen weiterhin direkt aus dem Composable auf. Funktioniert für den aktuellen Umfang, eine vollständige Migration auf das Google-empfohlene Pattern wäre der nächste Schritt.
- **Zahlungsmethoden-Auswahl**: Die Auswahl im Ticket-Kauf-Flow (`EventDetailScreen.kt`) ist bewusst rein dekorativ und ohne echte Zahlungsabwicklung — passend dazu gibt es in der Datenbank kein eigenes `Zahlung`/`Zahlungsmethode`-Modell. Eine echte Zahlungs-Integration wäre der nächste Schritt, falls die App über den Prototyp hinaus weiterentwickelt wird.
