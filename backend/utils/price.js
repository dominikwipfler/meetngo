// Events store price as a display string ("Kostenlos" or German decimal
// notation like "29,00"). Sorting/filtering needs a numeric value, since
// `ORDER BY` on the TEXT column compares lexicographically (e.g. "100,00"
// would sort before "29,00").
function parsePriceValue(price) {
  if (!price || price === "Kostenlos") return 0;
  const parsed = parseFloat(String(price).replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
}

module.exports = { parsePriceValue };
