const express = require("express");
const db = require("../database");
const { authMiddleware } = require("../middleware/auth");

const router = express.Router();

function serializeUser(row) {
  return {
    id: row.id,
    username: row.username,
    email: row.email,
    interests: JSON.parse(row.interests || "[]"),
  };
}

router.get("/me", authMiddleware, (req, res) => {
  const user = db.prepare("SELECT * FROM users WHERE id = ?").get(req.user.userId);
  if (!user) return res.status(404).json({ error: "Benutzer nicht gefunden" });
  res.json(serializeUser(user));
});

router.patch("/me", authMiddleware, (req, res) => {
  const { username, email, interests } = req.body;
  const current = db.prepare("SELECT * FROM users WHERE id = ?").get(req.user.userId);
  if (!current) return res.status(404).json({ error: "Benutzer nicht gefunden" });

  const newUsername = username !== undefined ? String(username).trim() : current.username;
  const newEmail = email !== undefined ? String(email).toLowerCase().trim() : current.email;

  if (!newUsername || !newEmail) {
    return res.status(400).json({ error: "Benutzername und E-Mail dürfen nicht leer sein" });
  }
  if (email !== undefined && !/\S+@\S+\.\S+/.test(newEmail)) {
    return res.status(400).json({ error: "Ungültige E-Mail-Adresse" });
  }

  let newInterests = current.interests;
  if (interests !== undefined) {
    if (!Array.isArray(interests) || !interests.every((i) => typeof i === "string")) {
      return res.status(400).json({ error: "Interessen müssen eine Liste von Texten sein" });
    }
    newInterests = JSON.stringify(interests);
  }

  try {
    db.prepare("UPDATE users SET username = ?, email = ?, interests = ? WHERE id = ?").run(
      newUsername,
      newEmail,
      newInterests,
      req.user.userId,
    );
  } catch (err) {
    if (err.message.includes("UNIQUE constraint failed")) {
      if (err.message.includes("username")) {
        return res.status(409).json({ error: "Benutzername bereits vergeben" });
      }
      return res.status(409).json({ error: "E-Mail-Adresse bereits registriert" });
    }
    throw err;
  }

  const updated = db.prepare("SELECT * FROM users WHERE id = ?").get(req.user.userId);
  res.json(serializeUser(updated));
});

module.exports = router;
