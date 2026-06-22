// Events store price as a display string ("Kostenlos" or German decimal
// notation like "29,00"). Sorting/filtering needs a numeric value, since
// `ORDER BY` on the TEXT column compares lexicographically (e.g. "100,00"
// would sort before "29,00").
function parsePriceValue(price) {
  if (!price || price === "Kostenlos") return 0;
  const parsed = parseFloat(String(price).replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
}

// Normalizes a raw price input into the canonical display label used across the
// app: "Kostenlos" for 0/empty, otherwise a German-formatted amount with two
// decimals and a comma separator (e.g. "29" -> "29,00", "29.5" -> "29,50").
// This keeps user-entered prices consistent with the seeded demo data and
// prevents mixed "." / "," separators in the UI.
function formatPriceLabel(price) {
  const value = parsePriceValue(price);
  if (value <= 0) return "Kostenlos";
  return `${value.toFixed(2).replace(".", ",")}`;
}

module.exports = { parsePriceValue, formatPriceLabel };
