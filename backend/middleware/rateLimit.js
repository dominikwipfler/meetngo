// Minimaler In-Memory-Rate-Limiter (Fixed-Window) ohne externe Dependency.
// Schützt sensible Endpunkte (Login/Register) vor Brute-Force. Der Zustand lebt
// im Prozessspeicher — für einen Single-Instance-Demo-Backend völlig ausreichend;
// für horizontale Skalierung müsste ein geteilter Store (z. B. Redis) her.

// Pro Schlüssel (IP+Route) gemerkte Fenster: { count, resetAt }.
const buckets = new Map();

/**
 * Erzeugt eine Middleware, die pro IP höchstens `max` Anfragen je `windowMs`
 * zulässt. Darüber hinausgehende Anfragen erhalten 429 mit deutscher Meldung.
 */
function rateLimit({ windowMs, max, message, enabled }) {
  return function rateLimitMiddleware(req, res, next) {
    // Standardmäßig in Tests deaktiviert (NODE_ENV === "test"), damit wiederholte
    // Aufrufe nicht fälschlich blocken. `enabled` kann das explizit übersteuern
    // (z. B. im Unit-Test des Limiters), ohne global process.env zu verändern.
    const isEnabled = enabled !== undefined ? enabled : process.env.NODE_ENV !== "test";
    if (!isEnabled) return next();

    const key = `${req.ip}:${req.baseUrl}${req.path}`;
    const now = Date.now();
    const bucket = buckets.get(key);

    if (!bucket || now > bucket.resetAt) {
      buckets.set(key, { count: 1, resetAt: now + windowMs });
      return next();
    }

    if (bucket.count >= max) {
      const retryAfter = Math.ceil((bucket.resetAt - now) / 1000);
      res.set("Retry-After", String(retryAfter));
      return res.status(429).json({
        error: message || "Zu viele Anfragen. Bitte später erneut versuchen.",
      });
    }

    bucket.count += 1;
    next();
  };
}

// Periodisch abgelaufene Fenster entfernen, damit die Map nicht unbegrenzt wächst.
// unref(), damit dieser Timer den Prozess (und Tests) nicht am Beenden hindert.
const cleanup = setInterval(() => {
  const now = Date.now();
  for (const [key, bucket] of buckets) {
    if (now > bucket.resetAt) buckets.delete(key);
  }
}, 60_000);
if (typeof cleanup.unref === "function") cleanup.unref();

module.exports = { rateLimit };
