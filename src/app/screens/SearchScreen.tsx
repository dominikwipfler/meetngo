import { useState } from "react";
import { Search, MapPin, Calendar } from "lucide-react";
import { useNavigate } from "react-router";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { BottomNav } from "../components/BottomNav";
import { AccessibilityButton } from "../components/AccessibilityButton";
import { events } from "../data/events";

const categories = [
  "Alle",
  "Musik",
  "Sport",
  "Kunst",
  "Food",
  "Tech",
  "Outdoor",
];

export function SearchScreen() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Alle");

  const filteredEvents = events.filter((event) => {
    const matchesSearch =
      event.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      event.location.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      selectedCategory === "Alle" || event.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="sticky top-0 bg-card border-b border-border px-4 py-4 z-10 space-y-4">
        <h1>Suche</h1>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <Input
            placeholder="Events, Orte, Kategorien..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10 h-12"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide -mx-4 px-4">
          {categories.map((category) => (
            <Badge
              key={category}
              variant={selectedCategory === category ? "default" : "outline"}
              className={`cursor-pointer whitespace-nowrap px-4 py-2 ${
                selectedCategory === category
                  ? "bg-primary text-primary-foreground"
                  : ""
              }`}
              onClick={() => setSelectedCategory(category)}
            >
              {category}
            </Badge>
          ))}
        </div>
      </div>

      <div className="p-4">
        {searchQuery && (
          <p className="text-sm text-muted-foreground mb-4">
            {filteredEvents.length} Ergebnisse gefunden
          </p>
        )}

        <div className="space-y-3">
          {filteredEvents.map((event) => (
            <Card
              key={event.id}
              className="p-4 cursor-pointer hover:shadow-lg transition-shadow"
              onClick={() => navigate(`/event/${event.id}`)}
            >
              <div className="flex gap-4">
                <div
                  className="w-20 h-20 rounded-xl flex-shrink-0 bg-cover bg-center"
                  style={{
                    backgroundImage: event.imageUrl
                      ? `url(${event.imageUrl})`
                      : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                  }}
                />
                <div className="flex-1 min-w-0">
                  <h3 className="mb-1 truncate">{event.name}</h3>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Calendar className="w-4 h-4" />
                      <span className="truncate">{event.date.split(",")[0]}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <MapPin className="w-4 h-4" />
                      <span className="truncate">{event.location}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 mt-2">
                    <Badge variant="secondary" className="text-xs">
                      {event.category}
                    </Badge>
                    <span className="text-xs text-muted-foreground">
                      {event.attendees} Teilnehmer
                    </span>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>

        {filteredEvents.length === 0 && searchQuery && (
          <div className="text-center py-12">
            <p className="text-muted-foreground">Keine Events gefunden</p>
            <p className="text-sm text-muted-foreground mt-2">
              Versuche es mit anderen Suchbegriffen
            </p>
          </div>
        )}
      </div>

      <BottomNav />
      <AccessibilityButton />
    </div>
  );
}
