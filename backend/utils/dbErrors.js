// Maps a better-sqlite3 UNIQUE-constraint error on the `users` table to a
// user-facing 409 response. `username` and `email` are the only unique columns,
// so the message is chosen based on which one the error mentions.
//
// Returns `true` if `res` was sent (the caller should stop), `false` otherwise
// (the caller should rethrow or handle the error itself).
function sendUserUniqueConflict(err, res) {
  if (!err.message.includes("UNIQUE constraint failed")) return false;

  if (err.message.includes("username")) {
    res.status(409).json({ error: "Benutzername bereits vergeben" });
  } else {
    res.status(409).json({ error: "E-Mail-Adresse bereits registriert" });
  }
  return true;
}

module.exports = { sendUserUniqueConflict };
