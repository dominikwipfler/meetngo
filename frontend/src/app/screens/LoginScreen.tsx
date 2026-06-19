import { useState } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { useAuth } from "../../context/AuthContext";
import { login as apiLogin } from "../../api/auth";

export function LoginScreen() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setError("");
    if (!email || !password) {
      setError("Bitte E-Mail und Passwort eingeben");
      return;
    }
    setLoading(true);
    try {
      const res = await apiLogin(email, password);
      login(res.token, res.user);
      navigate("/map");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Anmeldung fehlgeschlagen");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-sm space-y-8">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-24 h-24 rounded-full bg-primary mb-6">
            <span className="text-3xl text-primary-foreground">M&G</span>
          </div>
          <h1 className="text-3xl">MeetNGo</h1>
          <p className="text-muted-foreground mt-2">Entdecke Events in deiner Nähe</p>
        </div>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email">E-Mail</Label>
            <Input
              id="email"
              type="email"
              placeholder="max@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Passwort</Label>
            <Input
              id="password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="h-12"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()}
            />
          </div>

          {error && <p className="text-sm text-destructive text-center">{error}</p>}

          <Button
            onClick={handleLogin}
            disabled={loading}
            className="w-full h-12 bg-accent hover:bg-accent/90"
          >
            {loading ? "Einloggen..." : "Einloggen"}
          </Button>

          <div className="text-center text-sm">
            <span className="text-muted-foreground">Noch kein Konto? </span>
            <button
              className="text-primary hover:underline font-medium"
              onClick={() => navigate("/register")}
            >
              Registrieren
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
