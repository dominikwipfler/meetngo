# MeetNGo

Eine Event-App für Android, entwickelt mit React (Vite) und einem Express.js-Backend mit SQLite-Datenbank.

---

## Projektstruktur

```
meetngo/
├── frontend/                   # React/Vite App (Android UI)
│   ├── src/
│   │   ├── api/                # API-Client und Service-Funktionen
│   │   │   ├── client.ts       # Fetch-Wrapper mit JWT-Authentifizierung
│   │   │   ├── auth.ts         # login(), register()
│   │   │   └── events.ts       # CRUD-Operationen für Events
│   │   ├── app/
│   │   │   ├── components/     # Wiederverwendbare UI-Komponenten
│   │   │   └── screens/        # App-Screens (Login, Map, Search, …)
│   │   ├── context/
│   │   │   └── AuthContext.tsx # Globaler Auth-State (JWT + User)
│   │   └── main.tsx
│   ├── index.html
│   ├── vite.config.ts          # Vite + Proxy zu Backend (Port 3001)
│   └── package.json
│
├── backend/                    # Express.js REST-API
│   ├── server.js               # Einstiegspunkt (Port 3001)
│   ├── database.js             # SQLite via better-sqlite3 + Seed-Daten
│   ├── routes/
│   │   ├── auth.js             # POST /api/auth/register, /api/auth/login
│   │   └── events.js           # GET/POST/DELETE /api/events
│   ├── uploads/                # Hochgeladene Event-Bilder (gitignored)
│   └── package.json
│
├── guidelines/                 # Design-Guidelines aus Figma
├── pnpm-workspace.yaml         # pnpm Monorepo-Konfiguration
├── package.json                # Root-Skripte (dev, build, …)
└── .gitignore
```

---

## Voraussetzungen

- **Node.js** 18 oder neuer
- **pnpm** (empfohlen): `npm install -g pnpm`

---

## Installation

```bash
# 1. Root-Abhängigkeiten installieren (concurrently)
npm install

# 2. Frontend-Abhängigkeiten installieren
cd frontend && npm install && cd ..

# 3. Backend-Abhängigkeiten installieren
cd backend && npm install && cd ..
```

---

## Starten

### Beide gleichzeitig (empfohlen)

```bash
npm run dev
```

Dies startet Backend (Port 3001) und Frontend (Port 5173) gleichzeitig.

### Einzeln starten

```bash
# Nur Backend
npm run dev:backend
# oder
cd backend && node --watch server.js

# Nur Frontend
npm run dev:frontend
# oder
cd frontend && npm run dev
```

Öffne anschließend `http://localhost:5173` im Browser.

---

## API-Übersicht

Das Backend läuft auf `http://localhost:3001`. Vite proxied automatisch alle `/api` und `/uploads` Anfragen dorthin.

| Methode | Endpoint | Beschreibung |
|--------|----------|-------------|
| POST | `/api/auth/register` | Neues Konto erstellen |
| POST | `/api/auth/login` | Einloggen, gibt JWT zurück |
| GET | `/api/events` | Events abrufen (mit Filter/Sort) |
| GET | `/api/events/:id` | Ein Event abrufen |
| POST | `/api/events` | Event erstellen (Auth + Bild-Upload) |
| DELETE | `/api/events/:id` | Event löschen (nur Ersteller) |

### Filter-Parameter für `GET /api/events`

| Parameter | Werte | Beschreibung |
|-----------|-------|-------------|
| `search` | Freitext | Sucht in Name, Ort, Beschreibung |
| `category` | Musik, Sport, … | Kategorie-Filter |
| `sort` | `date`, `name`, `attendees`, `price` | Sortierfeld |
| `order` | `asc`, `desc` | Sortierreihenfolge |
| `priceFilter` | `free`, `paid` | Kostenlos / Kostenpflichtig |

---

## Datenbank

Die SQLite-Datenbank wird beim ersten Start automatisch unter `backend/meetngo.db` erstellt und mit Demo-Events befüllt. Die Datei ist in `.gitignore` eingetragen und wird **nicht** versioniert.

Hochgeladene Event-Bilder werden unter `backend/uploads/` gespeichert und sind ebenfalls gitignored.

---

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

> Das Backend muss auf einem erreichbaren Server laufen. Die API-URL in `frontend/src/api/client.ts` muss für den Android-Build auf die Serveradresse angepasst werden (z.B. `http://192.168.1.x:3001`).

---

## Figma-Design

Das UI-Design basiert auf dem Figma-Projekt:  
https://www.figma.com/design/XFbfqoyFt7IFZrnVYUjUzJ/MeetNGo-Mobile-App-UI
