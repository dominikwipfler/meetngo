# MeetNGo Mobile App UI

Dieses Repository enthält die UI-Implementierung der MeetNGo Mobile App, basierend auf einem Figma-Design.

## Projektübersicht

- Framework: React mit Vite
- Styling: Tailwind CSS / shadcn UI Komponenten
- Paketmanager: pnpm (verwendet durch `pnpm-workspace.yaml`)
- Ziel: lokale Entwicklung und schnelle Vorschau im Browser

## Voraussetzungen

- Node.js 18 oder neuer
- pnpm installiert (`npm install -g pnpm`)
- Git installiert, wenn du das Projekt versionieren möchtest

## Lokaler Start

1. Abhängigkeiten installieren:

```bash
pnpm install
```

2. Entwicklungsserver starten:

```bash
pnpm dev
```

3. Öffne die angezeigte URL im Browser (standardmäßig `http://localhost:5173`)

## Nützliche Befehle

- `pnpm dev` – Startet den lokalen Entwicklungsserver
- `pnpm build` – Erstellt das Produktions-Build

## Figma-Quelle

Das Design basiert auf dem Figma-Projekt unter:
https://www.figma.com/design/XFbfqoyFt7IFZrnVYUjUzJ/MeetNGo-Mobile-App-UI

## Git-Vorbereitung

Eine `.gitignore`-Datei wurde hinzugefügt, damit lokale Abhängigkeiten und temporäre Dateien nicht im Repository landen.

Wenn du ein neues Git-Repository initialisieren möchtest, verwende:

```bash
git init
git add .
git commit -m "Initial commit: MeetNGo Mobile App UI"
```
