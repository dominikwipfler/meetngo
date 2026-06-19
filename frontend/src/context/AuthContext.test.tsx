import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AuthProvider, useAuth } from "./AuthContext";

function TestConsumer() {
  const { user, isAuthenticated, login, logout } = useAuth();
  return (
    <div>
      <p data-testid="status">{isAuthenticated ? "in" : "out"}</p>
      <p data-testid="username">{user?.username ?? "none"}</p>
      <button onClick={() => login("token-123", { id: 1, username: "alice", email: "a@b.com" })}>
        login
      </button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

function renderWithProvider() {
  return render(
    <AuthProvider>
      <TestConsumer />
    </AuthProvider>,
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("starts unauthenticated when localStorage is empty", () => {
    renderWithProvider();
    expect(screen.getByTestId("status")).toHaveTextContent("out");
  });

  it("login() persists the session and updates state", async () => {
    const user = userEvent.setup();
    renderWithProvider();

    await user.click(screen.getByText("login"));

    expect(screen.getByTestId("status")).toHaveTextContent("in");
    expect(screen.getByTestId("username")).toHaveTextContent("alice");
    expect(localStorage.getItem("token")).toBe("token-123");
    expect(JSON.parse(localStorage.getItem("user")!)).toMatchObject({ username: "alice" });
  });

  it("logout() clears the session", async () => {
    const user = userEvent.setup();
    renderWithProvider();

    await user.click(screen.getByText("login"));
    await user.click(screen.getByText("logout"));

    expect(screen.getByTestId("status")).toHaveTextContent("out");
    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("user")).toBeNull();
  });

  it("hydrates an existing session from localStorage on mount", async () => {
    localStorage.setItem("token", "stored-token");
    localStorage.setItem("user", JSON.stringify({ id: 2, username: "bob", email: "b@b.com" }));

    renderWithProvider();

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("in"));
    expect(screen.getByTestId("username")).toHaveTextContent("bob");
  });

  it("clears a corrupted stored session instead of crashing", async () => {
    localStorage.setItem("token", "stored-token");
    localStorage.setItem("user", "{not valid json");

    renderWithProvider();

    await waitFor(() => expect(localStorage.getItem("token")).toBeNull());
    expect(screen.getByTestId("status")).toHaveTextContent("out");
  });
});
