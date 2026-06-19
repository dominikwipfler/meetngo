import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft } from "lucide-react";
import { useTheme } from "next-themes";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Badge } from "../components/ui/badge";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "../components/ui/accordion";
import { Switch } from "../components/ui/switch";
import { useAuth } from "../../context/AuthContext";
import { getMyProfile, updateProfile } from "../../api/users";

const availableInterests = [
  "Musik",
  "Sport",
  "Kunst",
  "Food",
  "Tech",
  "Outdoor",
  "Theater",
  "Kino",
  "Gaming",
  "Literatur",
];

export function ProfileSettingsScreen() {
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();
  const { updateUser } = useAuth();
  const [highContrast, setHighContrast] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [selectedInterests, setSelectedInterests] = useState<string[]>([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getMyProfile()
      .then((profile) => {
        setUsername(profile.username);
        setEmail(profile.email);
        setSelectedInterests(profile.interests);
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "Profil konnte nicht geladen werden");
      });
  }, []);

  const toggleInterest = (interest: string) => {
    setSelectedInterests((prev) =>
      prev.includes(interest) ? prev.filter((i) => i !== interest) : [...prev, interest],
    );
  };

  const handleSave = async () => {
    setError("");
    setSaving(true);
    try {
      const updated = await updateProfile({ username, email, interests: selectedInterests });
      updateUser(updated);
      navigate("/profile");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Speichern fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-background pb-6">
      <div className="sticky top-0 bg-card border-b border-border px-4 py-4 flex items-center gap-4 z-10">
        <button
          onClick={() => navigate(-1)}
          className="min-w-[44px] min-h-[44px] flex items-center justify-center -ml-2"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h1>Einstellungen</h1>
      </div>

      <div className="px-4 py-6 space-y-6">
        <div className="space-y-4">
          <h3>Profil bearbeiten</h3>

          <div className="space-y-2">
            <Label htmlFor="username">Benutzername</Label>
            <Input
              id="username"
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
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12"
            />
          </div>

          <Button variant="outline" className="w-full h-12">
            Passwort zurücksetzen
          </Button>
        </div>

        <div className="space-y-4">
          <Accordion type="single" collapsible className="w-full">
            <AccordionItem value="interests" className="border rounded-lg px-4">
              <AccordionTrigger>
                <h3>Interessen verwalten</h3>
              </AccordionTrigger>
              <AccordionContent>
                <div className="flex flex-wrap gap-2 pt-2">
                  {availableInterests.map((interest) => (
                    <Badge
                      key={interest}
                      variant={selectedInterests.includes(interest) ? "default" : "outline"}
                      className={`cursor-pointer px-3 py-1 ${
                        selectedInterests.includes(interest)
                          ? "bg-primary text-primary-foreground"
                          : ""
                      }`}
                      onClick={() => toggleInterest(interest)}
                    >
                      {interest}
                    </Badge>
                  ))}
                </div>
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        </div>

        <div className="space-y-4">
          <h3>Benachrichtigungen</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <p>Event-Erinnerungen</p>
                <p className="text-sm text-muted-foreground">
                  Benachrichtigungen für bevorstehende Events
                </p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
            <div className="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <p>Neue Events</p>
                <p className="text-sm text-muted-foreground">Events in deiner Nähe</p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <h3>Darstellung</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <p>Dark Mode</p>
                <p className="text-sm text-muted-foreground">Dunkles Farbschema</p>
              </div>
              <Switch
                checked={theme === "dark"}
                onCheckedChange={(checked) => setTheme(checked ? "dark" : "light")}
              />
            </div>
            <div className="flex items-center justify-between p-4 border border-border rounded-lg">
              <div>
                <p>Hoher Kontrast</p>
                <p className="text-sm text-muted-foreground">Bessere Lesbarkeit</p>
              </div>
              <Switch checked={highContrast} onCheckedChange={setHighContrast} />
            </div>
          </div>
        </div>

        {error && <p className="text-sm text-destructive text-center">{error}</p>}

        <Button
          onClick={handleSave}
          disabled={saving}
          className="w-full h-12 bg-primary hover:bg-primary/90"
        >
          {saving ? "Speichern..." : "Speichern"}
        </Button>
      </div>
    </div>
  );
}
