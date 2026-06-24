const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const db = require("../database");
const { parsePriceValue, formatPriceLabel } = require("../utils/price");
const { authMiddleware } = require("../middleware/auth");

const router = express.Router();

const uploadsDir = path.join(__dirname, "../uploads");
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

// Bundled demo images served from /uploads/seed, used as a fallback when an
// event is created without an uploaded image (keeps every event card pictured).
const SEED_IMAGES_BY_CATEGORY = {
  Musik: ["/uploads/seed/musik1.jpg", "/uploads/seed/musik2.jpg"],
  Sport: ["/uploads/seed/sport1.jpg", "/uploads/seed/sport2.jpg"],
  Food: ["/uploads/seed/food1.jpg", "/uploads/seed/food2.jpg"],
  Tech: ["/uploads/seed/tech1.jpg", "/uploads/seed/tech2.jpg"],
  Kunst: ["/uploads/seed/kunst1.jpg", "/uploads/seed/kunst2.jpg"],
  Outdoor: ["/uploads/seed/outdoor1.jpg", "/uploads/seed/outdoor2.jpg"],
  Sonstiges: ["/uploads/seed/sonstiges1.jpg", "/uploads/seed/sonstiges2.jpg"],
};

function seedImageForCategory(category) {
  const pool = SEED_IMAGES_BY_CATEGORY[category] || SEED_IMAGES_BY_CATEGORY.Sonstiges;
  return pool[Math.floor(Math.random() * pool.length)];
}

const storage = multer.diskStorage({
  destination: uploadsDir,
  filename: (req, file, cb) => {
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}${path.extname(file.originalname)}`;
    cb(null, unique);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    // multer@1.x's fileFilter can hang the request if it's rejected via
    // cb(error) — recording the error and skipping the file via cb(null,
    // false) is the documented workaround until the project moves to 2.x.
    if (["image/jpeg", "image/png", "image/webp"].includes(file.mimetype)) {
      cb(null, true);
    } else {
      req.fileValidationError = "Nur JPG, PNG oder WebP erlaubt";
      cb(null, false);
    }
  },
});

// Wraps multer's single-file upload so validation/size errors reach the
// client as JSON instead of being silently dropped or hitting Express's
// default HTML error page.
function uploadImage(req, res, next) {
  upload.single("image")(req, res, (err) => {
    if (err) {
      return res.status(400).json({ error: err.message || "Fehler beim Bild-Upload" });
    }
    if (req.fileValidationError) {
      return res.status(400).json({ error: req.fileValidationError });
    }
    next();
  });
}

// GET /api/events?search=&category=&sort=date|name|attendees|price&order=asc|desc
//   &priceFilter=free|paid&priceMax=&dateFrom=&dateTo=
router.get("/", (req, res) => {
  const {
    search = "",
    category = "",
    sort = "date",
    order = "asc",
    priceFilter = "",
    priceMax = "",
    dateFrom = "",
    dateTo = "",
    organizerId = "",
  } = req.query;

  const validSorts = {
    date: "date",
    name: "name",
    attendees: "attendees",
    price: "price_value",
  };
  const sortCol = validSorts[sort] || "date";
  const sortOrder = order === "desc" ? "DESC" : "ASC";

  let query = "SELECT * FROM events WHERE 1=1";
  const params = [];

  // Organizer view returns all of that organizer's events (including inactive);
  // the public browse view only shows active events.
  if (organizerId) {
    query += " AND organizer_id = ?";
    params.push(organizerId);
  } else {
    query += " AND active = 1";
  }

  if (search) {
    query += " AND (name LIKE ? OR location LIKE ? OR description LIKE ? OR organizer LIKE ?)";
    params.push(`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`);
  }

  if (category && category !== "Alle") {
    query += " AND category = ?";
    params.push(category);
  }

  if (priceFilter === "free") {
    query += " AND price = 'Kostenlos'";
  } else if (priceFilter === "paid") {
    query += " AND price != 'Kostenlos'";
  }

  // Maximalpreis: nur Events bis zu diesem numerischen Preiswert (kostenlose Events
  // mit price_value 0 sind dadurch automatisch eingeschlossen).
  const priceMaxValue = parseFloat(priceMax);
  if (!Number.isNaN(priceMaxValue) && priceMaxValue > 0) {
    query += " AND price_value <= ?";
    params.push(priceMaxValue);
  }

  // Zeitfenster-Filter über die ISO-Datums-Strings (lexikografischer Vergleich genügt,
  // da das Format "YYYY-MM-DDTHH:mm:ss" chronologisch sortierbar ist). Der Client liefert
  // bereits aufgelöste Grenzen (z. B. aus den Presets "Heute"/"Wochenende").
  if (dateFrom) {
    query += " AND date >= ?";
    params.push(dateFrom);
  }
  if (dateTo) {
    query += " AND date <= ?";
    params.push(dateTo);
  }

  // Gesamtzahl der gefilterten Treffer ermitteln, bevor ORDER BY/LIMIT greift —
  // wird als X-Total-Count-Header zurückgegeben, damit Clients paginieren können.
  const total = db.prepare(query.replace("SELECT *", "SELECT COUNT(*) AS count")).get(...params)
    .count;

  query += ` ORDER BY ${sortCol} ${sortOrder}`;

  // Optionale, rückwärtskompatible Pagination: ohne ?limit wird wie bisher die
  // komplette Liste geliefert. limit wird auf 100 gedeckelt, offset auf >= 0.
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 0, 0), 100);
  const offset = Math.max(parseInt(req.query.offset, 10) || 0, 0);
  if (limit > 0) {
    query += " LIMIT ? OFFSET ?";
    params.push(limit, offset);
  }

  const events = db.prepare(query).all(...params);
  res.set("X-Total-Count", String(total));
  res.json(events);
});

// GET /api/events/favorites — the authenticated user's favorited (active) events.
// Must be registered before "/:id" so "favorites" isn't parsed as an event id.
router.get("/favorites", authMiddleware, (req, res) => {
  const events = db
    .prepare(
      `SELECT events.* FROM events
       JOIN favorites ON favorites.event_id = events.id
       WHERE favorites.user_id = ? AND events.active = 1
       ORDER BY events.date ASC`,
    )
    .all(req.user.userId);
  res.json(events);
});

// GET /api/events/:id/favorite-status — whether the caller favorited :id
router.get("/:id/favorite-status", authMiddleware, (req, res) => {
  const favorited =
    db
      .prepare("SELECT 1 FROM favorites WHERE user_id = ? AND event_id = ?")
      .get(req.user.userId, req.params.id) != null;
  res.json({ favorited });
});

// POST /api/events/:id/favorite — favorite an event (idempotent)
router.post("/:id/favorite", authMiddleware, (req, res) => {
  const event = db.prepare("SELECT id FROM events WHERE id = ?").get(req.params.id);
  if (!event) return res.status(404).json({ error: "Event nicht gefunden" });
  db.prepare("INSERT OR IGNORE INTO favorites (user_id, event_id) VALUES (?, ?)").run(
    req.user.userId,
    req.params.id,
  );
  res.json({ favorited: true });
});

// DELETE /api/events/:id/favorite — remove from favorites (idempotent)
router.delete("/:id/favorite", authMiddleware, (req, res) => {
  db.prepare("DELETE FROM favorites WHERE user_id = ? AND event_id = ?").run(
    req.user.userId,
    req.params.id,
  );
  res.json({ favorited: false });
});

router.get("/:id", (req, res) => {
  const event = db.prepare("SELECT * FROM events WHERE id = ?").get(req.params.id);
  if (!event) return res.status(404).json({ error: "Event nicht gefunden" });
  res.json(event);
});

router.post("/", authMiddleware, uploadImage, (req, res) => {
  const { name, description, date, location, lat, lng, price, capacity, category, featured } =
    req.body;

  if (!name || !date || !location) {
    if (req.file) fs.unlinkSync(req.file.path);
    return res.status(400).json({ error: "Name, Datum und Ort sind erforderlich" });
  }

  const eventCategory = category || "Sonstiges";
  // When the organizer doesn't upload an image, fall back to a bundled,
  // category-appropriate demo image so every event card shows a picture
  // (same seed images served from /uploads/seed — see database.js).
  const imagePath = req.file
    ? `/uploads/${req.file.filename}`
    : seedImageForCategory(eventCategory);
  const priceLabel = formatPriceLabel(price);

  const stmt = db.prepare(`
    INSERT INTO events (name, description, date, location, lat, lng, organizer, organizer_id, price, price_value, capacity, category, image_path, featured)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  const result = stmt.run(
    name,
    description || null,
    date,
    location,
    parseFloat(lat) || 49.0069,
    parseFloat(lng) || 8.4037,
    req.user.username,
    req.user.userId,
    priceLabel,
    parsePriceValue(priceLabel),
    parseInt(capacity, 10) || null,
    eventCategory,
    imagePath,
    featured === "true" ? 1 : 0,
  );

  const event = db.prepare("SELECT * FROM events WHERE id = ?").get(result.lastInsertRowid);
  res.status(201).json(event);
});

// PATCH /api/events/:id — supports toggling the "featured" and "active" flags.
// Owner-only, mirroring the DELETE handler's authorization check.
router.patch("/:id", authMiddleware, (req, res) => {
  const event = db.prepare("SELECT * FROM events WHERE id = ?").get(req.params.id);
  if (!event) return res.status(404).json({ error: "Event nicht gefunden" });
  if (event.organizer_id !== req.user.userId) {
    return res.status(403).json({ error: "Keine Berechtigung" });
  }

  const { featured, active } = req.body;
  if (featured !== undefined) {
    db.prepare("UPDATE events SET featured = ? WHERE id = ?").run(featured ? 1 : 0, req.params.id);
  }
  if (active !== undefined) {
    db.prepare("UPDATE events SET active = ? WHERE id = ?").run(active ? 1 : 0, req.params.id);
  }

  const updated = db.prepare("SELECT * FROM events WHERE id = ?").get(req.params.id);
  res.json(updated);
});

router.delete("/:id", authMiddleware, (req, res) => {
  const event = db.prepare("SELECT * FROM events WHERE id = ?").get(req.params.id);
  if (!event) return res.status(404).json({ error: "Event nicht gefunden" });
  if (event.organizer_id !== req.user.userId) {
    return res.status(403).json({ error: "Keine Berechtigung" });
  }

  // Remove dependent rows first: foreign_keys=ON would otherwise reject the
  // delete with a constraint error once an event has tickets or favorites.
  db.transaction(() => {
    db.prepare("DELETE FROM tickets WHERE event_id = ?").run(req.params.id);
    db.prepare("DELETE FROM favorites WHERE event_id = ?").run(req.params.id);
    db.prepare("DELETE FROM events WHERE id = ?").run(req.params.id);
  })();

  // Only delete per-event uploads — never the shared demo seed images under
  // /uploads/seed, which are referenced by many events at once.
  if (event.image_path && event.image_path.startsWith("/uploads/") && !event.image_path.startsWith("/uploads/seed/")) {
    const imgFile = path.join(__dirname, "..", event.image_path);
    if (fs.existsSync(imgFile)) fs.unlinkSync(imgFile);
  }

  res.json({ success: true });
});

module.exports = router;
