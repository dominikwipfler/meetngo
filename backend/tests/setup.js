// Runs before every test file: isolates tests from the real dev database
// and gives JWT signing a fixed secret instead of the insecure fallback.
process.env.DB_PATH = ":memory:";
process.env.JWT_SECRET = "test-secret-do-not-use-in-production";
