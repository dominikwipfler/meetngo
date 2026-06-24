const express = require("express");
const cors = require("cors");
const path = require("path");

const authRoutes = require("./routes/auth");
const eventsRouter = require("./routes/events");
const ticketsRouter = require("./routes/tickets");
const usersRouter = require("./routes/users");
const tilesRouter = require("./routes/tiles");
const geocodeRouter = require("./routes/geocode");
const { securityHeaders } = require("./middleware/securityHeaders");

const app = express();

app.use(securityHeaders);
// exposedHeaders, damit Clients bei Cross-Origin den Pagination-Header (X-Total-Count) lesen dürfen.
app.use(cors({ exposedHeaders: ["X-Total-Count"] }));
app.use(express.json());

// Serve uploaded event images as static files
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// Kartenkacheln über das Backend proxen (siehe routes/tiles.js): macht die
// Karte im Emulator nutzbar, der externe Tile-Server nicht direkt erreicht.
app.use("/tiles", tilesRouter);

app.use("/api/auth", authRoutes);
app.use("/api/events", eventsRouter);
app.use("/api/tickets", ticketsRouter);
app.use("/api/users", usersRouter);
app.use("/api/geocode", geocodeRouter);

// Unknown API route -> JSON 404 (not Express's default HTML page).
app.use("/api", (req, res) => {
  res.status(404).json({ error: "Nicht gefunden" });
});

// Central error handler: keeps internal errors/stack traces from leaking to
// clients and always responds with JSON. Must be the last middleware.
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Serverfehler" });
});

module.exports = app;
