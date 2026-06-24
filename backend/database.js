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

  CREATE TABLE IF NOT EXISTS follows (
    follower_id INTEGER NOT NULL,
    organizer_id INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, organizer_id),
    FOREIGN KEY (follower_id) REFERENCES users(id),
    FOREIGN KEY (organizer_id) REFERENCES users(id)
  );

  CREATE TABLE IF NOT EXISTS favorites (
    user_id INTEGER NOT NULL,
    event_id INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, event_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (event_id) REFERENCES events(id)
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

// Migration: add active flag so organizers can deactivate events without deleting
if (!eventColumns.some((col) => col.name === "active")) {
  db.exec("ALTER TABLE events ADD COLUMN active INTEGER DEFAULT 1");
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
    "2026-06-29T19:00:00",
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
    "2026-08-08T20:00:00",
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
    "2026-07-25T21:00:00",
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
    "2026-09-19T16:00:00",
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
    "2026-10-10T09:00:00",
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
    "2026-08-16T10:00:00",
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
    "2026-07-12T09:30:00",
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
    "2027-01-05T11:00:00",
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
    "2026-07-20T12:00:00",
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
    "2026-08-02T17:00:00",
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
    "2026-07-15T18:00:00",
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
    "2026-08-29T09:00:00",
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
    "2026-07-23T19:30:00",
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
    "2026-10-24T18:00:00",
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
    "2026-07-06T10:00:00",
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
    "2026-07-27T10:00:00",
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
    "2026-08-09T09:00:00",
    "Hardtwald Karlsruhe",
    49.042,
    8.412,
    "Alpenverein KA",
    "18,00",
    25,
    "Outdoor",
  );
  seedEvent(
    "Stand-Up-Paddling Schnupperkurs",
    "Einsteigerkurs für Stand-Up-Paddling im Rheinhafen. Board und Paddel werden gestellt, Schwimmkenntnisse erforderlich.",
    "2026-08-02T10:00:00",
    "Rheinhafen Karlsruhe",
    48.975,
    8.33,
    "Kanu Club Karlsruhe",
    "20,00",
    18,
    "Outdoor",
  );

  // weitere Musik
  seedEvent(
    "Silent Disco im Stadtgarten",
    "Tanzen mit Kopfhörern statt Lautsprechern: drei Kanäle, drei DJs, eine stille Nacht für die Nachbarschaft.",
    "2026-08-22T21:00:00",
    "Stadtgarten Karlsruhe",
    49.0116,
    8.4031,
    "KA Events",
    "10,00",
    150,
    "Musik",
  );

  // weiterer Sport
  seedEvent(
    "Basketball Streetball Cup",
    "3-gegen-3-Turnier für Hobby- und Vereinsspieler auf dem Freiluftcourt. Anmeldung als Team oder einzeln möglich.",
    "2026-09-06T11:00:00",
    "Otto-Dullenkopf-Park",
    49.021,
    8.422,
    "Basketball Verband Karlsruhe",
    "Kostenlos",
    90,
    "Sport",
  );

  // weiteres Food
  seedEvent(
    "Craft Beer Festival",
    "Über 20 badische Mikrobrauereien stellen ihre Biere vor, dazu Live-Musik und Foodtrucks auf dem Gelände.",
    "2026-07-31T17:00:00",
    "Alter Schlachthof Karlsruhe",
    49.0024,
    8.4145,
    "Karlsruher Brauverein",
    "3,00",
    560,
    "Food",
  );

  // weiteres Tech
  seedEvent(
    "Startup Pitch Night",
    "Fünf lokale Startups pitchen vor Investoren und Publikum, im Anschluss Networking bei Getränken.",
    "2026-08-20T18:30:00",
    "CyberForum Karlsruhe",
    49.008,
    8.39,
    "CyberForum e.V.",
    "Kostenlos",
    140,
    "Tech",
  );

  // weitere Kunst
  seedEvent(
    "Streetart Walking Tour",
    "Geführter Rundgang zu den größten Wandgemälden und Murals der Oststadt, mit Hintergründen zu den Künstlern.",
    "2026-07-19T15:00:00",
    "Oststadt Karlsruhe",
    49.0125,
    8.425,
    "Kulturamt Karlsruhe",
    "8,00",
    35,
    "Kunst",
  );

  // Sonstiges
  seedEvent(
    "Flohmarkt am Schlossplatz",
    "Trödeln, stöbern, schnäppchen: privater Flohmarkt mit Ständen für Kleidung, Bücher und Vintage-Fundstücke.",
    "2026-07-11T10:00:00",
    "Schlossplatz Karlsruhe",
    49.0136,
    8.4039,
    "Stadt Karlsruhe",
    "Kostenlos",
    420,
    "Sonstiges",
  );
  seedEvent(
    "Brettspiele-Abend",
    "Gemütlicher Spieleabend mit über 100 Brett- und Kartenspielen für alle Altersgruppen, Anfänger willkommen.",
    "2026-07-18T19:00:00",
    "Spielebar Würfelglück",
    49.0094,
    8.3858,
    "Spielebar Würfelglück",
    "5,00",
    28,
    "Sonstiges",
  );
}

// Zusätzliche Demo-Events mit breiter Vielfalt (Neueröffnungen, Stadtveranstaltungen,
// Familien-/Bildungs-/Kulturangebote …). Anders als der Block oben läuft dieser bei JEDEM
// Start, fügt aber jedes Event nur dann ein, wenn noch keines mit gleichem Namen existiert.
// So bekommt auch eine bereits bestehende Datenbank die neuen Events – ohne Duplikate.
// image_path bleibt leer; der Backfill weiter unten ergänzt ein kategoriepassendes Bild.
const additionalDemoEvents = [
  // Neueröffnungen
  {
    name: "Neueröffnung: Rösterei & Café Bohnenwerk",
    description:
      "Die neue Kaffeerösterei in der Innenstadt lädt zur Eröffnung mit Gratis-Verkostung, Latte-Art-Show und Live-Musik. Schau vorbei und probiere frisch geröstete Bohnen.",
    date: "2026-07-04T10:00:00",
    location: "Kaiserstraße 88, Karlsruhe",
    lat: 49.0092,
    lng: 8.395,
    organizer: "Bohnenwerk Kaffeerösterei",
    price: "Kostenlos",
    attendees: 120,
    category: "Food",
  },
  {
    name: "Eröffnung Boulderhalle Vertical KA",
    description:
      "Karlsruhes neue Boulderhalle öffnet ihre Tore. Am Eröffnungstag freier Eintritt, Schnupperkurse für Anfänger und ein Wettkampf für Fortgeschrittene.",
    date: "2026-07-11T14:00:00",
    location: "Durlacher Allee 67, Karlsruhe",
    lat: 49.0083,
    lng: 8.43,
    organizer: "Vertical KA",
    price: "Kostenlos",
    attendees: 200,
    category: "Sport",
  },
  {
    name: "Neueröffnung Pop-up Concept Store",
    description:
      "Lokale Designerinnen und Designer präsentieren Mode, Schmuck und Wohnaccessoires im neuen Pop-up-Store. Mit Sektempfang zur Eröffnung.",
    date: "2026-07-19T11:00:00",
    location: "Erbprinzenstraße 14, Karlsruhe",
    lat: 49.0085,
    lng: 8.4015,
    organizer: "KA Kreativwirtschaft",
    price: "Kostenlos",
    attendees: 80,
    category: "Sonstiges",
  },
  // Stadtveranstaltungen
  {
    name: "Stadtfest am Marktplatz",
    description:
      "Drei Tage Bühnenprogramm, regionale Stände und Mitmach-Aktionen rund um den Marktplatz. Veranstaltet von der Stadt Karlsruhe für alle Generationen.",
    date: "2026-07-18T12:00:00",
    location: "Marktplatz Karlsruhe",
    lat: 49.0094,
    lng: 8.4044,
    organizer: "Stadt Karlsruhe",
    price: "Kostenlos",
    attendees: 4200,
    category: "Sonstiges",
  },
  {
    name: "Tag der offenen Tür im Rathaus",
    description:
      "Die Stadtverwaltung öffnet ihre Türen: Blicke hinter die Kulissen, Gespräche mit dem Oberbürgermeister und Infostände der Ämter.",
    date: "2026-09-12T10:00:00",
    location: "Rathaus am Marktplatz, Karlsruhe",
    lat: 49.009,
    lng: 8.404,
    organizer: "Stadt Karlsruhe",
    price: "Kostenlos",
    attendees: 650,
    category: "Sonstiges",
  },
  {
    name: "Bürgerdialog Mobilität",
    description:
      "Öffentliche Informations- und Diskussionsveranstaltung der Stadt zur Zukunft des Verkehrs in Karlsruhe. Bring deine Ideen und Fragen mit.",
    date: "2026-10-01T18:00:00",
    location: "Bürgersaal im Rathaus, Karlsruhe",
    lat: 49.0089,
    lng: 8.4041,
    organizer: "Stadt Karlsruhe",
    price: "Kostenlos",
    attendees: 90,
    category: "Sonstiges",
  },
  {
    name: "Adventsmarkt am Friedrichsplatz",
    description:
      "Stimmungsvoller Adventsmarkt mit Kunsthandwerk, Glühwein und einem täglichen Bühnenprogramm. Ein Angebot der Stadt für die Vorweihnachtszeit.",
    date: "2026-12-05T11:00:00",
    location: "Friedrichsplatz Karlsruhe",
    lat: 49.0072,
    lng: 8.404,
    organizer: "Stadt Karlsruhe",
    price: "Kostenlos",
    attendees: 1800,
    category: "Food",
  },
  // Bildung & Kultur
  {
    name: "Autorenlesung in der Stadtbibliothek",
    description:
      "Eine bekannte Autorin liest aus ihrem neuen Roman und beantwortet im Anschluss Fragen aus dem Publikum. Mit Signierstunde.",
    date: "2026-09-25T19:00:00",
    location: "Stadtbibliothek im Neuen Ständehaus, Karlsruhe",
    lat: 49.0079,
    lng: 8.4007,
    organizer: "Stadtbibliothek Karlsruhe",
    price: "5,00",
    attendees: 70,
    category: "Kunst",
  },
  {
    name: "Open-Air-Kino im Schlossgarten",
    description:
      "Lauschiger Filmabend unter freiem Himmel: aktuelle Kinohighlights auf großer Leinwand. Liegestühle und Snacks vor Ort.",
    date: "2026-08-14T21:00:00",
    location: "Schlossgarten Karlsruhe",
    lat: 49.015,
    lng: 8.405,
    organizer: "Kulturamt Karlsruhe",
    price: "9,00",
    attendees: 300,
    category: "Kunst",
  },
  {
    name: "Effekte – Wissenschaftsfestival",
    description:
      "Forschung zum Anfassen mitten in der Stadt: Mitmach-Experimente, Science-Slams und Vorträge der Karlsruher Hochschulen und Institute.",
    date: "2026-09-26T11:00:00",
    location: "Kronenplatz Karlsruhe",
    lat: 49.009,
    lng: 8.4115,
    organizer: "Stadt Karlsruhe & KIT",
    price: "Kostenlos",
    attendees: 900,
    category: "Tech",
  },
  {
    name: "Museumsnacht: Neue Ausstellung im ZKM",
    description:
      "Eröffnung der neuen Medienkunst-Ausstellung mit Führungen, Künstlergesprächen und Performances bis Mitternacht.",
    date: "2026-10-31T18:00:00",
    location: "ZKM Karlsruhe",
    lat: 49.0052,
    lng: 8.3895,
    organizer: "ZKM",
    price: "12,00",
    attendees: 500,
    category: "Kunst",
  },
  // Familie & Nachbarschaft
  {
    name: "Familienfest im Stadtgarten",
    description:
      "Großes Kinder- und Familienfest mit Bastelstationen, Ponyreiten, Hüpfburg und Bühnenshow. Ein Tag voller Spaß für Klein und Groß.",
    date: "2026-08-30T11:00:00",
    location: "Zoologischer Stadtgarten Karlsruhe",
    lat: 48.9966,
    lng: 8.4017,
    organizer: "Stadtgarten Karlsruhe",
    price: "8,00",
    attendees: 750,
    category: "Sonstiges",
  },
  {
    name: "Repair-Café Oststadt",
    description:
      "Gemeinsam reparieren statt wegwerfen: Ehrenamtliche helfen beim Instandsetzen von Elektrogeräten, Kleidung und Fahrrädern. Werkzeug ist vorhanden.",
    date: "2026-07-12T14:00:00",
    location: "Quartier Zukunft, Rintheimer Straße, Karlsruhe",
    lat: 49.0165,
    lng: 8.4365,
    organizer: "Quartier Zukunft",
    price: "Kostenlos",
    attendees: 45,
    category: "Sonstiges",
  },
  {
    name: "Regionaler Bauernmarkt",
    description:
      "Frische Erzeugnisse direkt von Höfen aus der Region: Obst, Gemüse, Käse, Honig und Backwaren. Jeden Samstag rund um den Stephanplatz.",
    date: "2026-07-25T08:00:00",
    location: "Stephanplatz Karlsruhe",
    lat: 49.01,
    lng: 8.398,
    organizer: "Marktgemeinschaft Karlsruhe",
    price: "Kostenlos",
    attendees: 350,
    category: "Food",
  },
  // Wirtschaft & Karriere
  {
    name: "Jobmesse Karriere KA",
    description:
      "Über 80 regionale Arbeitgeber stellen sich vor. Mit Bewerbungsmappen-Check, Vorträgen und der Möglichkeit für Spontan-Interviews.",
    date: "2026-10-15T09:00:00",
    location: "Messe Karlsruhe, Rheinstetten",
    lat: 48.97,
    lng: 8.333,
    organizer: "Agentur für Arbeit Karlsruhe",
    price: "Kostenlos",
    attendees: 1600,
    category: "Tech",
  },
  // Sport & Outdoor
  {
    name: "Drachenbootrennen am Rhein",
    description:
      "Spannende Teamrennen auf dem Wasser: Firmen-, Vereins- und Hobbyteams treten gegeneinander an. Mit Rahmenprogramm am Ufer.",
    date: "2026-08-23T10:00:00",
    location: "Rheinhafen Karlsruhe",
    lat: 48.975,
    lng: 8.33,
    organizer: "Kanu Club Karlsruhe",
    price: "5,00",
    attendees: 480,
    category: "Sport",
  },
  {
    name: "Nachtwächter-Stadtführung",
    description:
      "Eine unterhaltsame Führung durch die Karlsruher Altstadt bei Einbruch der Dunkelheit – mit Geschichten und Anekdoten vergangener Zeiten.",
    date: "2026-08-07T20:30:00",
    location: "Treffpunkt Marktplatz, Karlsruhe",
    lat: 49.0095,
    lng: 8.4043,
    organizer: "Stadtführungen Karlsruhe",
    price: "14,00",
    attendees: 25,
    category: "Outdoor",
  },
  // Unterhaltung
  {
    name: "Comedy Night im Tollhaus",
    description:
      "Ein Abend voller Lacher mit aufstrebenden und etablierten Comedians der deutschen Stand-up-Szene.",
    date: "2026-10-09T20:00:00",
    location: "Tollhaus, Alter Schlachthof Karlsruhe",
    lat: 49.0024,
    lng: 8.4145,
    organizer: "Tollhaus e.V.",
    price: "22,00",
    attendees: 260,
    category: "Sonstiges",
  },
  {
    name: "Weinfest am Gutenbergplatz",
    description:
      "Badische Winzer schenken ihre besten Tropfen aus, dazu regionale Spezialitäten und entspannte Live-Musik am Abend.",
    date: "2026-09-05T16:00:00",
    location: "Gutenbergplatz Karlsruhe",
    lat: 49.005,
    lng: 8.3855,
    organizer: "Winzergenossenschaft Baden",
    price: "Kostenlos",
    attendees: 600,
    category: "Food",
  },
];

const insertAdditionalEvent = db.prepare(`
  INSERT INTO events (name, description, date, location, lat, lng, organizer, price, price_value, attendees, category)
  SELECT @name, @description, @date, @location, @lat, @lng, @organizer, @price, @price_value, @attendees, @category
  WHERE NOT EXISTS (SELECT 1 FROM events WHERE name = @name)
`);
db.transaction(() => {
  for (const e of additionalDemoEvents) {
    insertAdditionalEvent.run({ ...e, price_value: parsePriceValue(e.price) });
  }
})();

// Demo-Events feiner kategorisieren, damit die zusätzlichen Filter-Tags (Familie, Bildung, Markt,
// Stadtleben, Nightlife) echte Treffer liefern. Läuft idempotent bei jedem Start und betrifft nur
// Demo-Events (organizer_id IS NULL) – von Nutzern angelegte Events behalten ihre Kategorie.
const demoCategoryReassignments = {
  Stadtleben: [
    "Stadtfest am Marktplatz",
    "Tag der offenen Tür im Rathaus",
    "Bürgerdialog Mobilität",
    "Neueröffnung Pop-up Concept Store",
  ],
  Bildung: [
    "Autorenlesung in der Stadtbibliothek",
    "Effekte – Wissenschaftsfestival",
    "KI & Gesellschaft",
  ],
  Markt: [
    "Regionaler Bauernmarkt",
    "Adventsmarkt am Friedrichsplatz",
    "Flohmarkt am Schlossplatz",
    "Weihnachtsmarkt",
  ],
  Familie: ["Familienfest im Stadtgarten", "Brettspiele-Abend", "Repair-Café Oststadt"],
  Nightlife: ["Comedy Night im Tollhaus", "Silent Disco im Stadtgarten"],
};
const reassignCategory = db.prepare(
  "UPDATE events SET category = ? WHERE name = ? AND organizer_id IS NULL",
);
db.transaction(() => {
  for (const [cat, names] of Object.entries(demoCategoryReassignments)) {
    for (const n of names) reassignCategory.run(cat, n);
  }
})();

// Backfill realistic, category-appropriate demo images for seeded events.
// Images are bundled in backend/uploads/seed and served by the backend itself
// (see app.js: app.use("/uploads", ...)), so they load reliably on every device
// without depending on external CDNs or internet access. Paths are relative;
// the apps' image helpers prepend the backend base URL.
//
// Fills any event that has no image at all (so every card shows a picture), and
// additionally replaces old absolute (http) URLs from earlier demo seeds.
// Locally uploaded images (/uploads/...) of real user events are never touched.
const CATEGORY_IMAGES = {
  Musik: ["/uploads/seed/musik1.jpg", "/uploads/seed/musik2.jpg"],
  Sport: ["/uploads/seed/sport1.jpg", "/uploads/seed/sport2.jpg"],
  Food: ["/uploads/seed/food1.jpg", "/uploads/seed/food2.jpg"],
  Tech: ["/uploads/seed/tech1.jpg", "/uploads/seed/tech2.jpg"],
  Kunst: ["/uploads/seed/kunst1.jpg", "/uploads/seed/kunst2.jpg"],
  Outdoor: ["/uploads/seed/outdoor1.jpg", "/uploads/seed/outdoor2.jpg"],
  Sonstiges: ["/uploads/seed/sonstiges1.jpg", "/uploads/seed/sonstiges2.jpg"],
  // Neue Kategorien greifen mangels eigener Motive auf passende vorhandene Bilder zurück.
  Familie: ["/uploads/seed/sonstiges1.jpg", "/uploads/seed/sonstiges2.jpg"],
  Bildung: ["/uploads/seed/tech1.jpg", "/uploads/seed/tech2.jpg"],
  Markt: ["/uploads/seed/food1.jpg", "/uploads/seed/food2.jpg"],
  Stadtleben: ["/uploads/seed/sonstiges1.jpg", "/uploads/seed/sonstiges2.jpg"],
  Nightlife: ["/uploads/seed/musik1.jpg", "/uploads/seed/musik2.jpg"],
};
const FALLBACK_IMAGES = CATEGORY_IMAGES.Sonstiges;

const demoEventsNeedingImage = db
  .prepare(
    `SELECT id, category FROM events
       WHERE image_path IS NULL
          OR image_path = ''
          OR (organizer_id IS NULL AND image_path LIKE 'http%')
       ORDER BY id`,
  )
  .all();
if (demoEventsNeedingImage.length > 0) {
  const setImage = db.prepare("UPDATE events SET image_path = ? WHERE id = ?");
  const perCategory = {};
  db.transaction(() => {
    for (const event of demoEventsNeedingImage) {
      const pool = CATEGORY_IMAGES[event.category] || FALLBACK_IMAGES;
      const index = perCategory[event.category] || 0;
      perCategory[event.category] = index + 1;
      setImage.run(pool[index % pool.length], event.id);
    }
  })();
}

module.exports = db;
