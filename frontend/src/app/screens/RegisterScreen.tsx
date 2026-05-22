import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { useAuth } from "../../context/AuthContext";
import { register as apiRegister } from "../../api/auth";

export function RegisterScreen() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    setError("");

    if (!username || !email || !password || !passwordConfirm) {
      setError("Alle Felder sind erforderlich");
      return;
    }
    if (password !== passwordConfirm) {
      setError("Passwörter stimmen nicht überein");
      return;
    }
    if (password.length < 6) {
      setError("Passwort muss mindestens 6 Zeichen lang sein");
      return;
    }

    setLoading(true);
    try {
      const res = await apiRegister(username, email, password);
      login(res.token, res.user);
      navigate("/map");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Registrierung fehlgeschlagen");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col p-6">
      <button
        onClick={() => navigate("/")}
        className="self-start min-w-[44px] min-h-[44px] flex items-center justify-center -ml-2 mb-4"
      >
        <ArrowLeft className="w-5 h-5" />
      </button>

      <div className="flex-1 flex flex-col justify-center w-full max-w-sm mx-auto space-y-8">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-primary mb-4">
            <span className="text-2xl text-primary-foreground">M&G</span>
          </div>
          <h1 className="text-2xl">Konto erstellen</h1>
          <p className="text-muted-foreground mt-2 text-sm">
            Werde Teil der MeetNGo-Community
          </p>
        </div>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="username">Benutzername</Label>
            <Input
              id="username"
              type="text"
              placeholder="max.mustermann"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="h-12"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">E-Mail</Label>
            <Input
              id="email"
              type="email"
              placeholder="max@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Passwort</Label>
            <Input
              id="password"
              type="password"
              placeholder="Mindestens 6 Zeichen"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="h-12"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="passwordConfirm">Passwort bestätigen</Label>
            <Input
              id="passwordConfirm"
              type="password"
              placeholder="••••••••"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              className="h-12"
              onKeyDown={(e) => e.key === "Enter" && handleRegister()}
            />
          </div>

          {error && (
            <p className="text-sm text-destructive text-center">{error}</p>
          )}

          <Button
            onClick={handleRegister}
            disabled={loading}
            className="w-full h-12 bg-accent hover:bg-accent/90"
          >
            {loading ? "Konto wird erstellt..." : "Konto erstellen"}
          </Button>

          <div className="text-center text-sm">
            <span className="text-muted-foreground">Bereits ein Konto? </span>
            <button
              className="text-primary hover:underline font-medium"
              onClick={() => navigate("/")}
            >
              Einloggen
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
