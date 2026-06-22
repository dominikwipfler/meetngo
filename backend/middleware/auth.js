const jwt = require("jsonwebtoken");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

// JWT-Secret bestimmen. Ziel: ein frisch geklontes Projekt startet ohne manuelle
// Schritte (`git clone` → `pnpm install` → `pnpm dev` läuft), ohne dabei die
// Sicherheit in Produktion zu opfern.
//
//  - Ist `JWT_SECRET` gesetzt, wird genau dieses verwendet — der einzige für
//    Produktion vorgesehene Weg.
//  - In Produktion (`NODE_ENV=production`) ist es Pflicht: fehlt es, brechen wir
//    bewusst ab, statt mit einem erratbaren Schlüssel Tokens zu signieren.
//  - In Entwicklung/Test wird, falls kein Secret gesetzt ist, einmalig ein
//    zufälliges erzeugt und in `backend/.jwt-dev-secret` (gitignored) abgelegt,
//    damit ausgestellte Tokens auch über (nodemon-)Neustarts hinweg gültig
//    bleiben und man sich nicht ständig neu einloggen muss.
function resolveJwtSecret() {
  if (process.env.JWT_SECRET) return process.env.JWT_SECRET;

  if (process.env.NODE_ENV === "production") {
    throw new Error(
      "JWT_SECRET ist nicht gesetzt. In Produktion zwingend erforderlich — siehe backend/.env.example.",
    );
  }

  const devSecretFile = path.join(__dirname, "..", ".jwt-dev-secret");
  try {
    const existing = fs.readFileSync(devSecretFile, "utf8").trim();
    if (existing) return existing;
  } catch {
    // Datei fehlt oder ist unlesbar — unten neu erzeugen.
  }

  const generated = crypto.randomBytes(48).toString("hex");
  try {
    fs.writeFileSync(devSecretFile, generated, { mode: 0o600 });
  } catch {
    // Schreiben fehlgeschlagen (read-only FS o. Ä.) — der In-Memory-Wert reicht
    // für diesen Prozesslauf trotzdem aus.
  }
  if (process.env.NODE_ENV !== "test") {
    console.warn(
      "[auth] JWT_SECRET nicht gesetzt — verwende ein lokales Entwicklungs-Secret " +
        "(backend/.jwt-dev-secret). Für Produktion JWT_SECRET als Umgebungsvariable setzen.",
    );
  }
  return generated;
}

const JWT_SECRET = resolveJwtSecret();

// Pin the signing/verification algorithm so a token can't be downgraded
// (e.g. to "none") or verified under a different scheme than it was issued with.
const JWT_OPTIONS = { algorithm: "HS256", expiresIn: "7d" };

function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Nicht authentifiziert" });
  try {
    req.user = jwt.verify(token, JWT_SECRET, { algorithms: ["HS256"] });
    next();
  } catch {
    res.status(401).json({ error: "Ungültiger Token" });
  }
}

module.exports = { authMiddleware, JWT_SECRET, JWT_OPTIONS };
