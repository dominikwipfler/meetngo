import { useState, useEffect, useCallback } from "react";
import { Search, MapPin, Calendar, SlidersHorizontal, ArrowUpDown } from "lucide-react";
import { useNavigate } from "react-router";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "../components/ui/sheet";
import { getEvents, getImageUrl } from "../../api/events";
import type { ApiEvent, EventFilters } from "../../api/events";

const CATEGORIES = ["Alle", "Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor"];

const SORT_OPTIONS = [
  { value: "date", label: "Datum" },
  { value: "name", label: "Name" },
  { value: "attendees", label: "Beliebtheit" },
  { value: "price", label: "Preis" },
] as const;

function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("de-DE", {
      day: "2-digit",
      month: "long",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
}

export function SearchScreen() {
  const navigate = useNavigate();
  const [events, setEvents] = useState<ApiEvent[]>([]);
  const [loading, setLoading] = useState(false);

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Alle");
  const [sort, setSort] = useState<EventFilters["sort"]>("date");
  const [order, setOrder] = useState<"asc" | "desc">("asc");
  const [priceFilter, setPriceFilter] = useState<"" | "free" | "paid">("");
  const [showFilterSheet, setShowFilterSheet] = useState(false);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getEvents({
        search: searchQuery,
        category: selectedCategory,
        sort,
        order,
        priceFilter,
      });
      setEvents(data);
    } catch {
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, [searchQuery, selectedCategory, sort, order, priceFilter]);

  useEffect(() => {
    const timer = setTimeout(fetchEvents, 300);
    return () => clearTimeout(timer);
  }, [fetchEvents]);

  const activeFilterCount = [
    priceFilter !== "",
    sort !== "date",
    order !== "asc",
  ].filter(Boolean).length;

  return (
    <div className="flex-1 flex flex-col bg-background">
      <div className="sticky top-0 bg-card border-b border-border px-4 py-4 z-10 space-y-4">
        <div className="flex items-center gap-2">
          <h1 className="flex-1">Suche</h1>
          <Button
            variant="outline"
            size="sm"
            className="relative gap-2"
            onClick={() => setShowFilterSheet(true)}
          >
            <SlidersHorizontal className="w-4 h-4" />
            Filter
            {activeFilterCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-accent text-accent-foreground text-xs rounded-full w-4 h-4 flex items-center justify-center">
                {activeFilterCount}
              </span>
            )}
          </Button>
        </div>

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
          {CATEGORIES.map((category) => (
            <Badge
              key={category}
              variant={selectedCategory === category ? "default" : "outline"}
              className={`cursor-pointer whitespace-nowrap px-4 py-2 ${
                selectedCategory === category ? "bg-primary text-primary-foreground" : ""
              }`}
              onClick={() => setSelectedCategory(category)}
            >
              {category}
            </Badge>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <ArrowUpDown className="w-4 h-4 text-muted-foreground" />
          <span className="text-sm text-muted-foreground">Sortiert nach:</span>
          <button
            className="text-sm text-primary font-medium"
            onClick={() => setShowFilterSheet(true)}
          >
            {SORT_OPTIONS.find((o) => o.value === sort)?.label} ({order === "asc" ? "↑" : "↓"})
          </button>
        </div>
      </div>

      <div className="p-4">
        <p className="text-sm text-muted-foreground mb-4">
          {loading ? "Suche..." : `${events.length} Event${events.length !== 1 ? "s" : ""} gefunden`}
        </p>

        <div className="space-y-3">
          {events.map((event) => {
            const imageUrl = getImageUrl(event.image_path);
            return (
              <Card
                key={event.id}
                className="p-4 cursor-pointer hover:shadow-lg transition-shadow"
                onClick={() => navigate(`/event/${event.id}`)}
              >
                <div className="flex gap-4">
                  <div
                    className="w-20 h-20 rounded-xl flex-shrink-0 bg-cover bg-center"
                    style={{
                      backgroundImage: imageUrl
                        ? `url(${imageUrl})`
                        : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <h3 className="mb-1 truncate">{event.name}</h3>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <Calendar className="w-4 h-4" />
                        <span className="truncate">{formatDate(event.date)}</span>
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
                      <span className="text-xs font-medium text-primary ml-auto">
                        {event.price === "Kostenlos" ? "Kostenlos" : `${event.price} €`}
                      </span>
                    </div>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>

        {!loading && events.length === 0 && (
          <div className="text-center py-12">
            <p className="text-muted-foreground">Keine Events gefunden</p>
            <p className="text-sm text-muted-foreground mt-2">
              Versuche es mit anderen Suchbegriffen oder Filtern
            </p>
          </div>
        )}
      </div>

      <Sheet open={showFilterSheet} onOpenChange={setShowFilterSheet}>
        <SheetContent side="bottom" className="rounded-t-xl">
          <SheetHeader>
            <SheetTitle>Sortierung & Filter</SheetTitle>
          </SheetHeader>

          <div className="space-y-6 mt-6 pb-4">
            <div>
              <p className="text-sm font-medium mb-3">Sortieren nach</p>
              <div className="grid grid-cols-2 gap-2">
                {SORT_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    onClick={() => setSort(option.value)}
                    className={`p-3 rounded-lg border text-sm font-medium transition-colors ${
                      sort === option.value
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border"
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-medium mb-3">Reihenfolge</p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setOrder("asc")}
                  className={`p-3 rounded-lg border text-sm font-medium transition-colors ${
                    order === "asc"
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border"
                  }`}
                >
                  Aufsteigend ↑
                </button>
                <button
                  onClick={() => setOrder("desc")}
                  className={`p-3 rounded-lg border text-sm font-medium transition-colors ${
                    order === "desc"
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border"
                  }`}
                >
                  Absteigend ↓
                </button>
              </div>
            </div>

            <div>
              <p className="text-sm font-medium mb-3">Preis</p>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { value: "" as const, label: "Alle" },
                  { value: "free" as const, label: "Kostenlos" },
                  { value: "paid" as const, label: "Kostenpflichtig" },
                ].map((option) => (
                  <button
                    key={option.value}
                    onClick={() => setPriceFilter(option.value)}
                    className={`p-3 rounded-lg border text-sm font-medium transition-colors ${
                      priceFilter === option.value
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border"
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-3">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => {
                  setSort("date");
                  setOrder("asc");
                  setPriceFilter("");
                }}
              >
                Zurücksetzen
              </Button>
              <Button
                className="flex-1 bg-accent hover:bg-accent/90"
                onClick={() => setShowFilterSheet(false)}
              >
                Anwenden
              </Button>
            </div>
          </div>
        </SheetContent>
      </Sheet>

    </div>
  );
}
