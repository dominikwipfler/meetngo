const express = require("express");

const router = express.Router();

// Adress-Geocoding-Proxy.
//
// Hintergrund: genau wie beim Kachel-Proxy (siehe routes/tiles.js) kann der
// Android-Emulator externe Hosts nicht direkt auflösen. Das Backend läuft auf
// dem Host (mit Internet) und reicht die Anfrage an Nominatim (OpenStreetMap)
// durch, statt dass die App selbst geocoded. Kein API-Key nötig.

const NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
// OSM verlangt einen aussagekräftigen User-Agent; anonyme Anfragen werden geblockt.
const USER_AGENT = "MeetNGo/1.0 (lokaler Entwicklungs-Geocoding-Proxy)";
const MAX_QUERY_LENGTH = 200;

router.get("/", async (req, res) => {
  const q = typeof req.query.q === "string" ? req.query.q.trim() : "";
  if (!q || q.length > MAX_QUERY_LENGTH) {
    return res.status(400).json({ error: "Ungültige Adresssuche" });
  }

  const url = `${NOMINATIM_URL}?format=json&limit=5&countrycodes=de&addressdetails=0&q=${encodeURIComponent(q)}`;
  try {
    const upstream = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
    if (!upstream.ok) {
      return res.status(502).json({ error: "Adresssuche fehlgeschlagen" });
    }
    const results = await upstream.json();
    const mapped = results.map((r) => ({
      label: r.display_name,
      lat: parseFloat(r.lat),
      lng: parseFloat(r.lon),
    }));
    res.json(mapped);
  } catch {
    return res.status(502).json({ error: "Geocoding-Dienst nicht erreichbar" });
  }
});

module.exports = router;
