const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const db = require("../database");
const { JWT_SECRET, JWT_OPTIONS } = require("../middleware/auth");
const { sendUserUniqueConflict } = require("../utils/dbErrors");
const { rateLimit } = require("../middleware/rateLimit");

const router = express.Router();

// Brute-Force-Schutz: max. 10 Auth-Versuche pro IP je 15 Minuten.
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: "Zu viele Anmeldeversuche. Bitte in einigen Minuten erneut versuchen.",
});

router.post("/register", authLimiter, (req, res) => {
  const { username, email, password } = req.body;

  if (!username || !email || !password) {
    return res.status(400).json({ error: "Alle Felder sind erforderlich" });
  }
  if (password.length < 6) {
    return res.status(400).json({ error: "Passwort muss mindestens 6 Zeichen lang sein" });
  }
  if (!/\S+@\S+\.\S+/.test(email)) {
    return res.status(400).json({ error: "Ungültige E-Mail-Adresse" });
  }

  // Normalize once so the stored value, the token and the response all agree.
  const cleanUsername = username.trim();
  const cleanEmail = email.toLowerCase().trim();

  try {
    const passwordHash = bcrypt.hashSync(password, 10);
    const stmt = db.prepare("INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)");
    const result = stmt.run(cleanUsername, cleanEmail, passwordHash);

    const token = jwt.sign(
      { userId: result.lastInsertRowid, username: cleanUsername },
      JWT_SECRET,
      JWT_OPTIONS,
    );

    res.status(201).json({
      token,
      user: { id: result.lastInsertRowid, username: cleanUsername, email: cleanEmail },
    });
  } catch (err) {
    if (sendUserUniqueConflict(err, res)) return;
    console.error(err);
    res.status(500).json({ error: "Serverfehler" });
  }
});

router.post("/login", authLimiter, (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: "E-Mail und Passwort sind erforderlich" });
  }

  const user = db.prepare("SELECT * FROM users WHERE email = ?").get(email.toLowerCase().trim());
  if (!user || !bcrypt.compareSync(password, user.password_hash)) {
    return res.status(401).json({ error: "Ungültige Anmeldedaten" });
  }

  const token = jwt.sign({ userId: user.id, username: user.username }, JWT_SECRET, JWT_OPTIONS);

  res.json({
    token,
    user: { id: user.id, username: user.username, email: user.email },
  });
});

module.exports = router;
