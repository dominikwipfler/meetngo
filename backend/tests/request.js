const supertest = require("supertest");
const app = require("../app");

// Persistenter Test-Server statt request(app) pro Aufruf.
//
// supertest(app) fährt bei JEDEM Request intern einen neuen Ephemeral-Server
// hoch und gleich wieder runter. Bei den vielen Aufrufen einer Testdatei führt
// dieses ständige Auf-/Zumachen unter Last zu sporadischen Transportfehlern
// ("Parse Error: Expected HTTP/…") bzw. zu vereinzelt fehlschlagenden Requests
// (ein frisch registrierter Nutzer wirkt dann z. B. als 404). Ein einziger,
// dauerhaft lauschender Server pro Testdatei beseitigt diese Flakiness.
//
// Jede Testdatei lädt dieses Modul einmal (frisch dank Vitest-Isolation), startet
// genau einen Server und schließt ihn nach allen Tests wieder.
const server = app.listen(0);

afterAll(() => {
  server.close();
});

// Drop-in-Ersatz für supertest: `request(app)` wie bisher aufrufbar, das Argument
// wird ignoriert und stattdessen der gemeinsame, bereits lauschende Server genutzt.
module.exports = () => supertest(server);
