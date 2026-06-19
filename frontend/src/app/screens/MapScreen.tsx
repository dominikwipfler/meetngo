import { useState, useEffect } from "react";
import { Search, Plus } from "lucide-react";
import { useNavigate } from "react-router";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { getEvents, getImageUrl } from "../../api/events";
import type { ApiEvent } from "../../api/events";

const CATEGORIES = ["Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor"];
const CENTER: [number, number] = [49.0069, 8.4037];

const CATEGORY_COLORS: Record<string, string> = {
  Musik: "#6366f1",
  Sport: "#22c55e",
  Food: "#f97316",
  Tech: "#0ea5e9",
  Kunst: "#ec4899",
  Outdoor: "#84cc16",
  Sonstiges: "#1D9E75",
};

function createEventIcon(category: string) {
  const bg = CATEGORY_COLORS[category] ?? CATEGORY_COLORS.Sonstiges;
  return L.divIcon({
    className: "",
    html: `<div style="
      width:36px;height:36px;background:${bg};
      border:3px solid white;border-radius:50% 50% 50% 0;
      transform:rotate(-45deg);box-shadow:0 2px 8px rgba(0,0,0,0.3);
    "></div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -40],
  });
}

function FlyToEvent({ event }: { event: ApiEvent | null }) {
  const map = useMap();
  useEffect(() => {
    if (event) map.flyTo([event.lat, event.lng], 15, { duration: 0.8 });
  }, [event, map]);
  return null;
}

export function MapScreen() {
  const navigate = useNavigate();
  const [activeFilters, setActiveFilters] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [allEvents, setAllEvents] = useState<ApiEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<ApiEvent | null>(null);

  useEffect(() => {
    getEvents({ sort: "date", order: "asc" })
      .then(setAllEvents)
      .catch(() => {});
  }, []);

  const toggleFilter = (f: string) =>
    setActiveFilters((prev) => (prev.includes(f) ? prev.filter((x) => x !== f) : [...prev, f]));

  const filtered = allEvents.filter((e) => {
    const q = searchQuery.toLowerCase();
    return (
      (!q || e.name.toLowerCase().includes(q) || e.location.toLowerCase().includes(q)) &&
      (activeFilters.length === 0 || activeFilters.includes(e.category))
    );
  });

  return (
    <div className="flex-1 flex flex-col min-h-0 relative">
      {/* ── Karte ── */}
      <MapContainer
        center={CENTER}
        zoom={14}
        style={{ flex: 1, minHeight: 0, zIndex: 0 }}
        zoomControl={false}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          attribution='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>'
          maxZoom={20}
        />
        <FlyToEvent event={selectedEvent} />
        {filtered.map((event) => (
          <Marker
            key={event.id}
            position={[event.lat, event.lng]}
            icon={createEventIcon(event.category)}
            eventHandlers={{ click: () => setSelectedEvent(event) }}
          >
            <Popup>
              <div
                className="cursor-pointer"
                style={{ minWidth: 170 }}
                onClick={() => navigate(`/event/${event.id}`)}
              >
                {event.image_path && (
                  <img
                    src={getImageUrl(event.image_path) ?? ""}
                    alt={event.name}
                    style={{
                      width: "100%",
                      height: 72,
                      objectFit: "cover",
                      borderRadius: 6,
                      marginBottom: 6,
                    }}
                  />
                )}
                <p style={{ fontWeight: 700, fontSize: 13, margin: "0 0 2px" }}>{event.name}</p>
                <p style={{ fontSize: 12, color: "#666", margin: "0 0 2px" }}>{event.location}</p>
                <p style={{ fontSize: 12, color: "#1D9E75", fontWeight: 600, margin: 0 }}>
                  {event.price === "Kostenlos" ? "Kostenlos" : `${event.price} €`}
                </p>
                <p style={{ fontSize: 11, color: "#aaa", marginTop: 4 }}>Tippen für Details →</p>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {/* ── Suchleiste + Filter (schwebt oben über Karte) ── */}
      <div className="absolute top-0 left-0 right-0 z-10 p-3 space-y-2 pointer-events-none">
        <div className="relative pointer-events-auto">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Events suchen..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-11 bg-card/95 backdrop-blur shadow-lg border-0 text-sm"
          />
        </div>
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide pointer-events-auto">
          {CATEGORIES.map((f) => (
            <Badge
              key={f}
              variant={activeFilters.includes(f) ? "default" : "outline"}
              className={`cursor-pointer whitespace-nowrap px-3 py-1.5 text-xs shadow ${
                activeFilters.includes(f)
                  ? "bg-primary text-primary-foreground"
                  : "bg-card/95 backdrop-blur"
              }`}
              onClick={() => toggleFilter(f)}
            >
              {f}
            </Badge>
          ))}
        </div>
      </div>

      {/* ── Event-Karten (horizontaler Scroll, direkt über BottomNav) ── */}
      <div className="absolute bottom-0 left-0 right-0 z-10 pb-3 pt-2">
        <div className="flex gap-3 overflow-x-auto px-3 scrollbar-hide">
          {filtered.length === 0 ? (
            <Card className="flex-shrink-0 w-full mx-3 p-3 bg-card/95 text-center">
              <p className="text-sm text-muted-foreground">Keine Events gefunden</p>
            </Card>
          ) : (
            filtered.map((event) => {
              const img = getImageUrl(event.image_path);
              const active = selectedEvent?.id === event.id;
              return (
                <Card
                  key={event.id}
                  className={`flex-shrink-0 w-60 p-3 cursor-pointer bg-card/97 shadow-lg border transition-all ${
                    active ? "border-primary ring-2 ring-primary/30" : "border-transparent"
                  }`}
                  onClick={() => {
                    setSelectedEvent(event);
                    navigate(`/event/${event.id}`);
                  }}
                >
                  <div className="flex gap-3 items-center">
                    <div
                      className="w-14 h-14 rounded-xl flex-shrink-0 bg-cover bg-center"
                      style={{
                        backgroundImage: img
                          ? `url(${img})`
                          : "linear-gradient(135deg,#1D9E75,#D85A30)",
                      }}
                    />
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-sm truncate leading-tight">{event.name}</p>
                      <p className="text-xs text-muted-foreground truncate">{event.location}</p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(event.date).toLocaleDateString("de-DE")}
                      </p>
                      <p className="text-xs font-semibold text-primary mt-0.5">
                        {event.price === "Kostenlos" ? "Kostenlos" : `${event.price} €`}
                      </p>
                    </div>
                  </div>
                </Card>
              );
            })
          )}
        </div>
      </div>

      {/* ── FAB: Event erstellen — unten rechts, über Event-Karten ── */}
      <button
        className="absolute bottom-32 right-4 z-20 bg-accent text-accent-foreground rounded-full shadow-xl flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
        style={{ width: 52, height: 52 }}
        onClick={() => navigate("/create-event")}
        aria-label="Event erstellen"
      >
        <Plus className="w-6 h-6" />
      </button>
    </div>
  );
}
