import { describe, it, expect, vi, beforeEach } from "vitest";
import * as client from "./client";
import { getEvents } from "./events";

describe("getEvents", () => {
  beforeEach(() => {
    vi.spyOn(client, "apiRequest").mockResolvedValue([]);
  });

  it("requests the plain endpoint when no filters are given", async () => {
    await getEvents();
    expect(client.apiRequest).toHaveBeenCalledWith("/events");
  });

  it("includes search, sort and order in the query string", async () => {
    await getEvents({ search: "Jazz", sort: "name", order: "desc" });
    const [endpoint] = (client.apiRequest as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(endpoint).toContain("search=Jazz");
    expect(endpoint).toContain("sort=name");
    expect(endpoint).toContain("order=desc");
  });

  it("omits the category filter when it's 'Alle'", async () => {
    await getEvents({ category: "Alle" });
    const [endpoint] = (client.apiRequest as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(endpoint).not.toContain("category");
  });

  it("includes the category filter for a specific category", async () => {
    await getEvents({ category: "Musik" });
    const [endpoint] = (client.apiRequest as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(endpoint).toContain("category=Musik");
  });

  it("includes the price filter when set", async () => {
    await getEvents({ priceFilter: "free" });
    const [endpoint] = (client.apiRequest as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(endpoint).toContain("priceFilter=free");
  });
});
