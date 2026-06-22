const express = require("express");
const fs = require("fs");
const path = require("path");

const router = express.Router();

// Lokaler Kachel-Proxy für die Karte.
//
// Hintergrund: Die Android-App zeigt eine OpenStreetMap-Karte. osmdroid lädt die
// Kartenkacheln normalerweise direkt von externen Tile-Servern
// (tile.openstreetmap.org). Im Emulator-Setup kann der Emulator aber keine
// externen DNS auflösen — die Kacheln kämen nie an und die Karte bliebe grau.
// Das Backend läuft auf dem Host (mit Internet) und ist vom Emulator über
// 10.0.2.2 erreichbar, genau wie die Event-Bilder. Wir holen die Kacheln also
// hier und reichen sie durch — mit Festplatten-Cache, damit der OSM-Server
// (Nutzungsrichtlinie!) nicht bei jedem Schwenk neu angefragt wird.

// Mehrere OSM-Mirror-Subdomains, damit Folgeanfragen sich verteilen.
const TILE_HOSTS = ["a", "b", "c"];
const CACHE_DIR = path.join(__dirname, "..", "tiles-cache");
const MAX_ZOOM = 19;

// OSM verlangt einen aussagekräftigen User-Agent; anonyme Anfragen werden geblockt.
const USER_AGENT = "MeetNGo/1.0 (lokaler Entwicklungs-Tile-Proxy)";

// Cache-Verzeichnis einmalig anlegen.
fs.mkdirSync(CACHE_DIR, { recursive: true });

/**
 * Prüft, ob z/x/y gültige Kachel-Koordinaten sind. Verhindert, dass über die
 * Route beliebige Pfade/URLs angefragt werden können (Path-Traversal/SSRF).
 */
function parseTileCoords(zRaw, xRaw, yRaw) {
  const z = Number(zRaw);
  const x = Number(xRaw);
  const y = Number(yRaw);
  if (![z, x, y].every(Number.isInteger)) return null;
  if (z < 0 || z > MAX_ZOOM) return null;
  const max = 2 ** z; // gültige x/y liegen in [0, 2^z)
  if (x < 0 || x >= max || y < 0 || y >= max) return null;
  return { z, x, y };
}

router.get("/:z/:x/:y.png", async (req, res) => {
  const coords = parseTileCoords(req.params.z, req.params.x, req.params.y);
  if (!coords) {
    return res.status(400).json({ error: "Ungültige Kachel-Koordinaten" });
  }
  const { z, x, y } = coords;

  const cacheFile = path.join(CACHE_DIR, `${z}_${x}_${y}.png`);

  // Cache-Treffer: direkt ausliefern.
  if (fs.existsSync(cacheFile)) {
    res.set("Content-Type", "image/png");
    res.set("Cache-Control", "public, max-age=604800"); // 7 Tage
    res.set("X-Tile-Cache", "HIT");
    return res.sendFile(cacheFile);
  }

  // Cache-Miss: von einem OSM-Mirror laden, ablegen, ausliefern.
  const host = TILE_HOSTS[(x + y) % TILE_HOSTS.length];
  const url = `https://${host}.tile.openstreetmap.org/${z}/${x}/${y}.png`;
  try {
    const upstream = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
    if (!upstream.ok) {
      return res.status(502).json({ error: "Kachel konnte nicht geladen werden" });
    }
    const buffer = Buffer.from(await upstream.arrayBuffer());
    // Erst schreiben, dann senden; Schreibfehler dürfen die Antwort nicht verhindern.
    fs.writeFile(cacheFile, buffer, () => {});
    res.set("Content-Type", "image/png");
    res.set("Cache-Control", "public, max-age=604800");
    res.set("X-Tile-Cache", "MISS");
    return res.send(buffer);
  } catch {
    return res.status(502).json({ error: "Tile-Server nicht erreichbar" });
  }
});

module.exports = router;
