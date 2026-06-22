// Setzt gängige HTTP-Sicherheits-Header — eine schlanke, dependency-freie
// Alternative zu helmet (für ein Demo-Backend ausreichend). Schützt vor
// MIME-Sniffing, Clickjacking und Referrer-Leaks und beschränkt per CSP die
// Quellen, aus denen API-Antworten Ressourcen laden dürfen.
function securityHeaders(req, res, next) {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("X-DNS-Prefetch-Control", "off");
  // Erlaubt der Android-App (anderer Origin), die /uploads-Bilder einzubinden.
  res.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
  // API liefert nur eigene JSON/Bild-Ressourcen — default-src 'self' genügt.
  res.setHeader("Content-Security-Policy", "default-src 'self'");
  next();
}

module.exports = { securityHeaders };
