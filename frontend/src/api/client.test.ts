import { describe, it, expect, vi, beforeEach } from "vitest";
import { apiRequest } from "./client";

function mockFetchOnce(body: unknown, init: { ok?: boolean; status?: number } = {}) {
  const ok = init.ok ?? true;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok,
      status: init.status ?? (ok ? 200 : 400),
      json: () => Promise.resolve(body),
    }),
  );
}

describe("apiRequest", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("does not send an Authorization header when no token is stored", async () => {
    mockFetchOnce({ ok: true });
    await apiRequest("/events");

    const headers = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1].headers;
    expect(headers.Authorization).toBeUndefined();
  });

  it("sends a Bearer Authorization header when a token is stored", async () => {
    localStorage.setItem("token", "abc123");
    mockFetchOnce({ ok: true });
    await apiRequest("/events");

    const headers = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1].headers;
    expect(headers.Authorization).toBe("Bearer abc123");
  });

  it("sets Content-Type: application/json for plain object bodies", async () => {
    mockFetchOnce({});
    await apiRequest("/events", { method: "POST", body: JSON.stringify({ a: 1 }) });

    const headers = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1].headers;
    expect(headers["Content-Type"]).toBe("application/json");
  });

  it("omits Content-Type for FormData bodies (browser sets the multipart boundary)", async () => {
    mockFetchOnce({});
    await apiRequest("/events", { method: "POST", body: new FormData() });

    const headers = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1].headers;
    expect(headers["Content-Type"]).toBeUndefined();
  });

  it("returns the parsed JSON body on success", async () => {
    mockFetchOnce({ id: 1, name: "Test Event" });
    const result = await apiRequest<{ id: number; name: string }>("/events/1");
    expect(result).toEqual({ id: 1, name: "Test Event" });
  });

  it("throws the server-provided error message on failure", async () => {
    mockFetchOnce({ error: "Ungültige Anmeldedaten" }, { ok: false, status: 401 });
    await expect(apiRequest("/auth/login")).rejects.toThrow("Ungültige Anmeldedaten");
  });

  it("falls back to a generic message when the error body isn't valid JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: () => Promise.reject(new Error("not json")),
      }),
    );
    await expect(apiRequest("/events")).rejects.toThrow("Netzwerkfehler");
  });
});
