require("dotenv").config();

const app = require("./app");

const PORT = process.env.PORT || 3001;

if (!process.env.JWT_SECRET) {
  console.warn(
    "WARNUNG: JWT_SECRET ist nicht gesetzt — es wird ein unsicherer Standardwert verwendet. " +
      "Siehe backend/.env.example.",
  );
}

app.listen(PORT, () => {
  console.log(`MeetNGo Backend läuft auf http://localhost:${PORT}`);
});
