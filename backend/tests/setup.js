// Runs before every test file: isolates tests from the real dev database
// and gives JWT signing a fixed secret instead of the insecure fallback.
//
// NODE_ENV explizit auf "test" setzen (nicht auf Vitests Default verlassen),
// damit der Rate-Limiter deterministisch deaktiviert ist — sonst können die
// vielen Register-/Login-Aufrufe einer Testdatei sporadisch ein 429 auslösen.
process.env.NODE_ENV = "test";
process.env.DB_PATH = ":memory:";
process.env.JWT_SECRET = "test-secret-do-not-use-in-production";
