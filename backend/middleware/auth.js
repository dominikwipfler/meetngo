const jwt = require("jsonwebtoken");

const JWT_SECRET = process.env.JWT_SECRET || "meetngo-secret-key-change-in-production";

function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Nicht authentifiziert" });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    res.status(401).json({ error: "Ungültiger Token" });
  }
}

module.exports = { authMiddleware, JWT_SECRET };
