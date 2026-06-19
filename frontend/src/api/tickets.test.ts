import { describe, it, expect, vi, beforeEach } from "vitest";
import * as client from "./client";
import { createTicket, getMyTickets } from "./tickets";

describe("tickets api", () => {
  beforeEach(() => {
    vi.spyOn(client, "apiRequest").mockResolvedValue([]);
  });

  it("getMyTickets requests the tickets endpoint", async () => {
    await getMyTickets();
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets");
  });

  it("createTicket POSTs the eventId as JSON", async () => {
    await createTicket(42);
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets", {
      method: "POST",
      body: JSON.stringify({ eventId: 42 }),
    });
  });
});
