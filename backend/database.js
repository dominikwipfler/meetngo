const Database = require("better-sqlite3");
const path = require("path");
const { parsePriceValue } = require("./utils/price");

// Tests inject DB_PATH=":memory:" for a clean, isolated database per run.
const dbPath = process.env.DB_PATH || path.join(__dirname, "meetngo.db");
const db = new Database(dbPath);

db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    interests TEXT DEFAULT '[]',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    date TEXT NOT NULL,
    location TEXT NOT NULL,
    lat REAL DEFAULT 49.0069,
    lng REAL DEFAULT 8.4037,
    organizer TEXT,
    organizer_id INTEGER,
    price TEXT DEFAULT 'Kostenlos',
    price_value REAL DEFAULT 0,
    capacity INTEGER,
    attendees INTEGER DEFAULT 0,
    category TEXT DEFAULT 'Sonstiges',
    image_path TEXT,
    featured INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organizer_id) REFERENCES users(id)
  );

  CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    status TEXT DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
  );
`);

// Migration: add price_value to databases created before this column existed
const eventColumns = db.prepare("PRAGMA table_info(events)").all();
if (!eventColumns.some((col) => col.name === "price_value")) {
  db.exec("ALTER TABLE events ADD COLUMN price_value REAL DEFAULT 0");
  const backfill = db.prepare("UPDATE events SET price_value = ? WHERE id = ?");
  const rows = db.prepare("SELECT id, price FROM events").all();
  for (const row of rows) {
    backfill.run(parsePriceValue(row.price), row.id);
  }
}

// Migration: add interests to databases created before this column existed
const userColumns = db.prepare("PRAGMA table_info(users)").all();
if (!userColumns.some((col) => col.name === "interests")) {
  db.exec("ALTER TABLE users ADD COLUMN interests TEXT DEFAULT '[]'");
}

// Seed demo events if table is empty
const count = db.prepare("SELECT COUNT(*) as count FROM events").get();
if (count.count === 0) {
  const insert = db.prepare(`
    INSERT INTO events (name, description, date, location, lat, lng, organizer, price, price_value, attendees, category)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  const seedEvent = (
    name,
    description,
    date,
    location,
    lat,
    lng,
    organizer,
    price,
    attendees,
    category,
  ) =>
    insert.run(
      name,
      description,
      date,
      location,
      lat,
      lng,
      organizer,
      price,
      parsePriceValue(price),
      attendees,
      category,
    );

  // Musik
  seedEvent(
    "Karlsruhe Jazz Festival",
    "Erlebe eine Nacht voller Jazz mit internationalen Künstlern. Das Festival präsentiert die besten Jazz-Acts aus ganz Europa in einer unvergesslichen Atmosphäre.",
    "2026-05-25T19:00:00",
    "Konzerthaus Karlsruhe",
    49.0069,
    8.4037,
    "Jazz Kulturverein",
    "29,00",
    234,
    "Musik",
  );
  seedEvent(
    "Open-Air Konzert im Schlosspark",
    "Klassische Musik unter freiem Himmel im wunderschönen Karlsruher Schlosspark. Bringt eure Picknick-Decken mit!",
    "2026-07-04T20:00:00",
    "Schlosspark Karlsruhe",
    49.0135,
    8.4044,
    "Philharmonie Karlsruhe",
    "Kostenlos",
    650,
    "Musik",
  );
  seedEvent(
    "Indie Rock Night",
    "Die besten lokalen Indie-Bands spielen live. Entdecke neue Talente aus der Karlsruher Musikszene.",
    "2026-06-20T21:00:00",
    "Substage Karlsruhe",
    49.0042,
    8.3934,
    "Substage e.V.",
    "12,00",
    180,
    "Musik",
  );
  seedEvent(
    "Elektro Festival KA",
    "Ein Wochenende voller elektronischer Musik mit DJs aus ganz Deutschland. Headliner werden noch bekannt gegeben.",
    "2026-08-15T16:00:00",
    "Europahalle Karlsruhe",
    49.0068,
    8.3952,
    "KA Events",
    "35,00",
    420,
    "Musik",
  );

  // Sport
  seedEvent(
    "Marathon 2026",
    "Der jährliche Karlsruhe Marathon führt durch die schönsten Teile der Stadt. Verschiedene Distanzen für alle Fitnesslevel.",
    "2026-09-05T09:00:00",
    "Stadtmitte Karlsruhe",
    49.0087,
    8.4043,
    "Laufverein Karlsruhe",
    "45,00",
    3200,
    "Sport",
  );
  seedEvent(
    "Volleyball Turnier",
    "Beachvolleyball-Turnier am Rhein. Teams mit 2–4 Personen können sich anmelden. Preise für die Top 3.",
    "2026-07-12T10:00:00",
    "Rheinpark Karlsruhe",
    48.9942,
    8.3712,
    "SV Karlsruhe",
    "8,00",
    96,
    "Sport",
  );
  seedEvent(
    "Yoga im Park",
    "Kostenloser Yoga-Kurs für alle Level jeden Samstag im Stadtgarten. Matte mitbringen!",
    "2026-06-07T09:30:00",
    "Stadtgarten Karlsruhe",
    49.0116,
    8.4031,
    "YogaKA",
    "Kostenlos",
    55,
    "Sport",
  );

  // Food
  seedEvent(
    "Weihnachtsmarkt",
    "Der traditionelle Weihnachtsmarkt bringt festliche Stimmung in die Innenstadt. Genieße Glühwein, Lebkuchen und regionale Spezialitäten.",
    "2026-12-01T11:00:00",
    "Marktplatz Karlsruhe",
    49.0094,
    8.4044,
    "Stadt Karlsruhe",
    "Kostenlos",
    1520,
    "Food",
  );
  seedEvent(
    "Street Food Festival",
    "Entdecke kulinarische Köstlichkeiten aus aller Welt. Über 50 Food-Trucks und Stände bieten internationale Spezialitäten.",
    "2026-06-15T12:00:00",
    "Stadtpark Karlsruhe",
    49.0086,
    8.4029,
    "Food Events GmbH",
    "Kostenlos",
    789,
    "Food",
  );
  seedEvent(
    "Weinprobe im Schloss",
    "Exklusive Weinverkostung mit regionalen Weinen aus Baden-Württemberg. Fachkundige Begleitung durch einen Sommelier.",
    "2026-06-28T17:00:00",
    "Karlsruher Schloss",
    49.0135,
    8.4044,
    "Weinhaus Baden",
    "24,00",
    40,
    "Food",
  );

  // Tech
  seedEvent(
    "Tech Meetup Karlsruhe",
    "Networking-Event für Tech-Enthusiasten. Vorträge über die neuesten Entwicklungen in AI, Web3 und Cloud Computing.",
    "2026-06-10T18:00:00",
    "Coworking Space Karlsruhe",
    49.0102,
    8.4051,
    "Tech Community KA",
    "15,00",
    120,
    "Tech",
  );
  seedEvent(
    "Hackathon KA 2026",
    "48 Stunden coden, netzwerken und Ideen umsetzen. Teams aus 2–5 Personen lösen reale Probleme der Stadt Karlsruhe.",
    "2026-07-25T09:00:00",
    "Hochschule Karlsruhe",
    49.0091,
    8.4117,
    "CyberForum e.V.",
    "Kostenlos",
    200,
    "Tech",
  );
  seedEvent(
    "KI & Gesellschaft",
    "Podiumsdiskussion über die Auswirkungen von Künstlicher Intelligenz auf Arbeit, Bildung und Demokratie.",
    "2026-06-18T19:30:00",
    "ZKM Karlsruhe",
    49.0052,
    8.3895,
    "ZKM Institut",
    "Kostenlos",
    250,
    "Tech",
  );

  // Kunst
  seedEvent(
    "Kunstnacht Karlsruhe",
    "Eine Nacht lang öffnen Galerien, Ateliers und Museen ihre Türen. Erlebe Kunst in ungewöhnlicher Atmosphäre.",
    "2026-09-19T18:00:00",
    "Innenstadt Karlsruhe",
    49.0094,
    8.4044,
    "Kulturamt Karlsruhe",
    "5,00",
    3000,
    "Kunst",
  );
  seedEvent(
    "Skulpturen im Park",
    "Interaktive Skulpturenausstellung im Günther-Klotz-Anlage. Lokale Künstler präsentieren ihre Werke unter freiem Himmel.",
    "2026-06-01T10:00:00",
    "Günther-Klotz-Anlage",
    49.0002,
    8.3945,
    "Kunstverein Karlsruhe",
    "Kostenlos",
    300,
    "Kunst",
  );

  // Outdoor
  seedEvent(
    "Stadtradeln Karlsruhe",
    "Gemeinsam durch die Stadt radeln und dabei das Klima schützen. Tages- und Wochentour für alle Altersgruppen.",
    "2026-06-22T10:00:00",
    "Marktplatz Karlsruhe",
    49.0094,
    8.4044,
    "ADFC Karlsruhe",
    "Kostenlos",
    180,
    "Outdoor",
  );
  seedEvent(
    "Klettern im Hardtwald",
    "Geführte Klettertour durch den Hardtwald nördlich von Karlsruhe. Ausrüstung wird gestellt, Anfänger willkommen.",
    "2026-07-05T09:00:00",
    "Hardtwald Karlsruhe",
    49.042,
    8.412,
    "Alpenverein KA",
    "18,00",
    25,
    "Outdoor",
  );
}

module.exports = db;
