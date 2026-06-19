const express = require("express");
const cors = require("cors");
const path = require("path");

const authRoutes = require("./routes/auth");
const eventsRouter = require("./routes/events");
const ticketsRouter = require("./routes/tickets");
const usersRouter = require("./routes/users");

const app = express();

app.use(cors());
app.use(express.json());

// Serve uploaded event images as static files
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

app.use("/api/auth", authRoutes);
app.use("/api/events", eventsRouter);
app.use("/api/tickets", ticketsRouter);
app.use("/api/users", usersRouter);

module.exports = app;
