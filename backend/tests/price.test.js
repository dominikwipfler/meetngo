const { parsePriceValue } = require("../utils/price");

describe("parsePriceValue", () => {
  it("returns 0 for 'Kostenlos'", () => {
    expect(parsePriceValue("Kostenlos")).toBe(0);
  });

  it("returns 0 for empty/missing input", () => {
    expect(parsePriceValue("")).toBe(0);
    expect(parsePriceValue(null)).toBe(0);
    expect(parsePriceValue(undefined)).toBe(0);
  });

  it("parses German decimal notation", () => {
    expect(parsePriceValue("29,00")).toBe(29);
    expect(parsePriceValue("12,50")).toBe(12.5);
  });

  it("sorts numerically, not lexicographically", () => {
    const prices = ["100,00", "29,00", "8,00"];
    const sorted = [...prices].sort((a, b) => parsePriceValue(a) - parsePriceValue(b));
    expect(sorted).toEqual(["8,00", "29,00", "100,00"]);
  });

  it("falls back to 0 for unparseable input", () => {
    expect(parsePriceValue("auf Anfrage")).toBe(0);
  });
});
