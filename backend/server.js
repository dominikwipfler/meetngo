const path = require("path");
// Load .env next to this file so the backend works regardless of the working
// directory it's started from (IDE run configs, monorepo scripts, etc.).
require("dotenv").config({ path: path.join(__dirname, ".env") });

// require("./app") loads the auth middleware. In Entwicklung erzeugt diese bei
// fehlendem JWT_SECRET automatisch ein lokales Dev-Secret (frischer Klon läuft
// sofort); in Produktion (NODE_ENV=production) bricht der Start dagegen ab, wenn
// JWT_SECRET fehlt. Siehe middleware/auth.js.
const app = require("./app");

const PORT = process.env.PORT || 3001;

app.listen(PORT, () => {
  console.log(`MeetNGo Backend läuft auf http://localhost:${PORT}`);
});
