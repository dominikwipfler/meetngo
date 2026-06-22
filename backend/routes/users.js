const express = require("express");
const bcrypt = require("bcryptjs");
const db = require("../database");
const { authMiddleware } = require("../middleware/auth");
const { sendUserUniqueConflict } = require("../utils/dbErrors");

const router = express.Router();

function serializeUser(row) {
  return {
    id: row.id,
    username: row.username,
    email: row.email,
    interests: JSON.parse(row.interests || "[]"),
  };
}

// Whether `viewerId` follows `organizerId`, plus that organizer's total followers.
function followStatus(organizerId, viewerId) {
  const followers = db
    .prepare("SELECT COUNT(*) AS c FROM follows WHERE organizer_id = ?")
    .get(organizerId).c;
  const following =
    db
      .prepare("SELECT 1 FROM follows WHERE follower_id = ? AND organizer_id = ?")
      .get(viewerId, organizerId) != null;
  return { following, followers };
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
    if (sendUserUniqueConflict(err, res)) return;
    throw err;
  }

  const updated = db.prepare("SELECT * FROM users WHERE id = ?").get(req.user.userId);
  res.json(serializeUser(updated));
});

// PATCH /api/users/me/password — change the authenticated user's password
router.patch("/me/password", authMiddleware, (req, res) => {
  const { currentPassword, newPassword } = req.body;
  if (!currentPassword || !newPassword) {
    return res.status(400).json({ error: "Aktuelles und neues Passwort sind erforderlich" });
  }
  if (String(newPassword).length < 6) {
    return res.status(400).json({ error: "Passwort muss mindestens 6 Zeichen lang sein" });
  }

  const user = db.prepare("SELECT * FROM users WHERE id = ?").get(req.user.userId);
  if (!user) return res.status(404).json({ error: "Benutzer nicht gefunden" });
  if (!bcrypt.compareSync(currentPassword, user.password_hash)) {
    return res.status(401).json({ error: "Aktuelles Passwort ist falsch" });
  }

  const newHash = bcrypt.hashSync(newPassword, 10);
  db.prepare("UPDATE users SET password_hash = ? WHERE id = ?").run(newHash, req.user.userId);
  res.json({ success: true });
});

// GET /api/users/:id/follow-status — does the caller follow :id, and :id's follower count
router.get("/:id/follow-status", authMiddleware, (req, res) => {
  const organizerId = parseInt(req.params.id, 10);
  if (!organizerId) return res.status(400).json({ error: "Ungültige Benutzer-ID" });
  res.json(followStatus(organizerId, req.user.userId));
});

// POST /api/users/:id/follow — follow an organizer (idempotent)
router.post("/:id/follow", authMiddleware, (req, res) => {
  const organizerId = parseInt(req.params.id, 10);
  if (!organizerId) return res.status(400).json({ error: "Ungültige Benutzer-ID" });
  if (organizerId === req.user.userId) {
    return res.status(400).json({ error: "Du kannst dir nicht selbst folgen" });
  }
  const target = db.prepare("SELECT id FROM users WHERE id = ?").get(organizerId);
  if (!target) return res.status(404).json({ error: "Benutzer nicht gefunden" });

  db.prepare("INSERT OR IGNORE INTO follows (follower_id, organizer_id) VALUES (?, ?)").run(
    req.user.userId,
    organizerId,
  );
  res.json(followStatus(organizerId, req.user.userId));
});

// DELETE /api/users/:id/follow — unfollow (idempotent)
router.delete("/:id/follow", authMiddleware, (req, res) => {
  const organizerId = parseInt(req.params.id, 10);
  if (!organizerId) return res.status(400).json({ error: "Ungültige Benutzer-ID" });
  db.prepare("DELETE FROM follows WHERE follower_id = ? AND organizer_id = ?").run(
    req.user.userId,
    organizerId,
  );
  res.json(followStatus(organizerId, req.user.userId));
});

module.exports = router;
