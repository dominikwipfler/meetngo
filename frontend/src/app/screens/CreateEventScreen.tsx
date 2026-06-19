import { useState, useRef } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Upload, X } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { Switch } from "../components/ui/switch";
import { createEvent } from "../../api/events";

const CATEGORIES = ["Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor", "Sonstiges"];

export function CreateEventScreen() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [date, setDate] = useState("");
  const [price, setPrice] = useState("");
  const [capacity, setCapacity] = useState("");
  const [category, setCategory] = useState("Sonstiges");
  const [featured, setFeatured] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      setError("Datei ist zu groß. Maximum 5MB erlaubt.");
      return;
    }
    setImageFile(file);
    const reader = new FileReader();
    reader.onloadend = () => setImagePreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handlePublish = async () => {
    setError("");
    if (!title || !description || !location || !date) {
      setError("Bitte fülle alle Pflichtfelder aus");
      return;
    }

    setLoading(true);
    try {
      const formData = new FormData();
      formData.append("name", title);
      formData.append("description", description);
      formData.append("location", location);
      formData.append("date", date);
      formData.append("price", price || "Kostenlos");
      formData.append("capacity", capacity);
      formData.append("category", category);
      formData.append("featured", String(featured));
      if (imageFile) formData.append("image", imageFile);

      await createEvent(formData);
      navigate("/organizer-dashboard");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Fehler beim Erstellen des Events");
    } finally {
      setLoading(false);
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
        <h1>Event erstellen</h1>
      </div>

      <div className="px-4 py-6 space-y-6">
        <div className="space-y-2">
          <Label htmlFor="image">Event-Bild</Label>
          {imagePreview ? (
            <div className="relative">
              <div
                className="w-full h-48 rounded-lg bg-cover bg-center"
                style={{ backgroundImage: `url(${imagePreview})` }}
              />
              <button
                onClick={handleRemoveImage}
                className="absolute top-2 right-2 bg-destructive text-destructive-foreground rounded-full p-2 shadow-lg hover:bg-destructive/90"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-border rounded-lg p-8 flex flex-col items-center justify-center cursor-pointer hover:border-primary transition-colors"
            >
              <Upload className="w-8 h-8 text-muted-foreground mb-2" />
              <p className="text-sm text-muted-foreground text-center">Bild hochladen (max. 5MB)</p>
              <p className="text-xs text-muted-foreground mt-1">
                JPG, PNG oder WebP — wird in <code>/backend/uploads/</code> gespeichert
              </p>
            </div>
          )}
          <input
            ref={fileInputRef}
            id="image"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={handleImageSelect}
            className="hidden"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="title">Titel *</Label>
          <Input
            id="title"
            placeholder="z.B. Karlsruhe Jazz Festival"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="h-12"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="description">Beschreibung *</Label>
          <Textarea
            id="description"
            placeholder="Beschreibe dein Event..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="min-h-[120px]"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="category">Kategorie</Label>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                onClick={() => setCategory(cat)}
                className={`px-4 py-2 rounded-full text-sm border transition-colors ${
                  category === cat
                    ? "bg-primary text-primary-foreground border-primary"
                    : "border-border hover:border-primary"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="location">Ort *</Label>
          <Input
            id="location"
            placeholder="z.B. Konzerthaus Karlsruhe"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            className="h-12"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="date">Datum & Uhrzeit *</Label>
          <Input
            id="date"
            type="datetime-local"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="h-12"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="price">Ticketpreis (€)</Label>
            <Input
              id="price"
              type="number"
              placeholder="29.00"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="h-12"
            />
            <p className="text-xs text-muted-foreground">Leer lassen für kostenloses Event</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="capacity">Kontingent</Label>
            <Input
              id="capacity"
              type="number"
              placeholder="500"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              className="h-12"
            />
          </div>
        </div>

        <div className="flex items-center justify-between p-4 border border-border rounded-lg">
          <div>
            <Label htmlFor="featured">Event hervorheben</Label>
            <p className="text-sm text-muted-foreground">Mehr Sichtbarkeit für dein Event</p>
          </div>
          <Switch id="featured" checked={featured} onCheckedChange={setFeatured} />
        </div>

        {error && <p className="text-sm text-destructive text-center">{error}</p>}

        <Button
          onClick={handlePublish}
          disabled={loading}
          className="w-full h-12 bg-accent hover:bg-accent/90"
        >
          {loading ? "Wird veröffentlicht..." : "Veröffentlichen"}
        </Button>
      </div>
    </div>
  );
}
