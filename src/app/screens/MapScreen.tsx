import { useState } from "react";
import { Search, Plus, MapPin } from "lucide-react";
import { useNavigate } from "react-router";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { BottomNav } from "../components/BottomNav";
import { AccessibilityButton } from "../components/AccessibilityButton";
import { events } from "../data/events";

const filters = ["Heute", "Musik", "Sport", "Kunst", "Food", "Tech"];

// Karlsruhe center coordinates
const CENTER_LAT = 49.0069;
const CENTER_LNG = 8.4037;

// Simple coordinate to pixel conversion for our map view
const coordToPixel = (lat: number, lng: number, mapWidth: number, mapHeight: number) => {
  const latRange = 0.02; // Zoom level
  const lngRange = 0.03;

  const x = ((lng - (CENTER_LNG - lngRange / 2)) / lngRange) * mapWidth;
  const y = ((CENTER_LAT + latRange / 2 - lat) / latRange) * mapHeight;

  return { x, y };
};

export function MapScreen() {
  const navigate = useNavigate();
  const [activeFilters, setActiveFilters] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  const toggleFilter = (filter: string) => {
    setActiveFilters((prev) =>
      prev.includes(filter)
        ? prev.filter((f) => f !== filter)
        : [...prev, filter]
    );
  };

  const filteredEvents = events.filter((event) => {
    const matchesSearch =
      searchQuery === "" ||
      event.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      event.location.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter =
      activeFilters.length === 0 || activeFilters.includes(event.category);
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="h-screen flex flex-col bg-background">
      <div className="relative flex-1">
        {/* Map Background with embedded tile */}
        <div className="absolute inset-0 bg-muted overflow-hidden">
          {/* Static map tile from OpenStreetMap */}
          <img
            src={`https://tile.openstreetmap.org/14/8572/5605.png`}
            alt="Karlsruhe Map"
            className="absolute inset-0 w-full h-full object-cover opacity-80"
            style={{ imageRendering: 'crisp-edges' }}
          />
          {/* Grid overlay for multiple tiles */}
          <div className="absolute inset-0 grid grid-cols-2 grid-rows-2">
            <img
              src="https://tile.openstreetmap.org/14/8571/5604.png"
              alt=""
              className="w-full h-full object-cover opacity-80"
            />
            <img
              src="https://tile.openstreetmap.org/14/8572/5604.png"
              alt=""
              className="w-full h-full object-cover opacity-80"
            />
            <img
              src="https://tile.openstreetmap.org/14/8571/5605.png"
              alt=""
              className="w-full h-full object-cover opacity-80"
            />
            <img
              src="https://tile.openstreetmap.org/14/8572/5605.png"
              alt=""
              className="w-full h-full object-cover opacity-80"
            />
          </div>

          {/* Event Markers */}
          <div className="absolute inset-0">
            {filteredEvents.map((event, index) => {
              // Simple positioning based on index for visual distribution
              const positions = [
                { top: '35%', left: '40%' },
                { top: '55%', left: '60%' },
                { top: '45%', left: '30%' },
                { top: '60%', left: '45%' },
                { top: '40%', left: '55%' },
              ];
              const position = positions[index % positions.length];

              return (
                <button
                  key={event.id}
                  className="absolute transform -translate-x-1/2 -translate-y-full group"
                  style={position}
                  onClick={() => navigate(`/event/${event.id}`)}
                >
                  <div className="relative">
                    {/* Marker Pin */}
                    <div className="w-10 h-10 bg-primary rounded-full border-4 border-white shadow-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                      <MapPin className="w-5 h-5 text-primary-foreground" />
                    </div>
                    {/* Tooltip on hover */}
                    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                      <div className="bg-card border border-border rounded-lg p-2 shadow-xl whitespace-nowrap">
                        <p className="text-sm font-medium">{event.name}</p>
                      </div>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Search and Filters */}
        <div className="absolute top-0 left-0 right-0 p-4 space-y-3 z-10">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              placeholder="Events suchen..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 h-12 bg-card shadow-lg"
            />
          </div>

          <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
            {filters.map((filter) => (
              <Badge
                key={filter}
                variant={activeFilters.includes(filter) ? "default" : "outline"}
                className={`cursor-pointer whitespace-nowrap px-4 py-2 ${
                  activeFilters.includes(filter)
                    ? "bg-primary text-primary-foreground"
                    : "bg-card"
                }`}
                onClick={() => toggleFilter(filter)}
              >
                {filter}
              </Badge>
            ))}
          </div>
        </div>

        {/* Event Preview Cards */}
        <div className="absolute bottom-20 left-0 right-0 px-4 z-10">
          <div className="space-y-3 max-h-64 overflow-y-auto">
            {filteredEvents.slice(0, 3).map((event) => (
              <Card
                key={event.id}
                className="p-4 cursor-pointer hover:shadow-lg transition-shadow bg-card"
                onClick={() => navigate(`/event/${event.id}`)}
              >
                <div className="flex items-start gap-3">
                  <div
                    className="w-16 h-16 rounded-xl flex-shrink-0 bg-cover bg-center"
                    style={{
                      backgroundImage: event.imageUrl
                        ? `url(${event.imageUrl})`
                        : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <h3 className="truncate">{event.name}</h3>
                    <p className="text-sm text-muted-foreground">
                      {event.date.split(",")[0]}
                    </p>
                    <p className="text-sm text-muted-foreground">{event.location}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {event.attendees} Teilnehmer
                    </p>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>

        {/* Floating Add Button */}
        <button
          className="absolute bottom-32 left-1/2 -translate-x-1/2 z-20 bg-accent text-accent-foreground rounded-full p-4 shadow-xl min-w-[56px] min-h-[56px] flex items-center justify-center hover:scale-110 transition-transform"
          onClick={() => navigate("/create-event")}
        >
          <Plus className="w-6 h-6" />
        </button>
      </div>

      <BottomNav />
      <AccessibilityButton />
    </div>
  );
}
