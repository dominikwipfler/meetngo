import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Edit, Star, XCircle, Trash2, QrCode, TrendingUp } from "lucide-react";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../components/ui/alert-dialog";
import { getEvents, deleteEvent, getImageUrl } from "../../api/events";
import type { ApiEvent } from "../../api/events";
import { useAuth } from "../../context/AuthContext";

export function OrganizerDashboardScreen() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [myEvents, setMyEvents] = useState<ApiEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<ApiEvent | null>(null);

  useEffect(() => {
    getEvents()
      .then((data) => {
        const mine = data.filter((e) => e.organizer_id === user?.id);
        setMyEvents(mine);
        if (mine.length > 0) setSelectedEvent(mine[0]);
      })
      .catch(() => {});
  }, [user?.id]);

  const stats = [
    { label: "Teilnehmer", value: String(selectedEvent?.attendees ?? 0), icon: TrendingUp },
    {
      label: "Kapazität",
      value: selectedEvent?.capacity ? String(selectedEvent.capacity) : "∞",
      icon: TrendingUp,
    },
    {
      label: "Preis",
      value: selectedEvent?.price === "Kostenlos" ? "Frei" : `${selectedEvent?.price}€`,
      icon: TrendingUp,
    },
  ];

  const handleDelete = async () => {
    setShowDeleteDialog(false);
    if (!selectedEvent) return;
    try {
      await deleteEvent(selectedEvent.id);
      navigate("/map");
    } catch {
      navigate("/map");
    }
  };

  if (myEvents.length === 0) {
    return (
      <div className="bg-background pb-6">
        <div className="sticky top-0 bg-card border-b border-border px-4 py-4 flex items-center gap-4 z-10">
          <button
            onClick={() => navigate(-1)}
            className="min-w-[44px] min-h-[44px] flex items-center justify-center -ml-2"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1>Event Dashboard</h1>
        </div>
        <div className="flex flex-col items-center justify-center p-8 mt-12 space-y-4">
          <p className="text-muted-foreground text-center">Du hast noch keine Events erstellt.</p>
          <Button
            onClick={() => navigate("/create-event")}
            className="bg-accent hover:bg-accent/90"
          >
            Event erstellen
          </Button>
        </div>
      </div>
    );
  }

  const event = selectedEvent;

  return (
    <div className="bg-background pb-6">
      <div className="sticky top-0 bg-card border-b border-border px-4 py-4 flex items-center gap-4 z-10">
        <button
          onClick={() => navigate(-1)}
          className="min-w-[44px] min-h-[44px] flex items-center justify-center -ml-2"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h1>Event Dashboard</h1>
      </div>

      <div className="px-4 py-6 space-y-6">
        {myEvents.length > 1 && (
          <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4">
            {myEvents.map((e) => (
              <button
                key={e.id}
                onClick={() => setSelectedEvent(e)}
                className={`px-4 py-2 rounded-full text-sm border whitespace-nowrap transition-colors ${
                  selectedEvent?.id === e.id
                    ? "bg-primary text-primary-foreground border-primary"
                    : "border-border"
                }`}
              >
                {e.name}
              </button>
            ))}
          </div>
        )}

        {event && (
          <Card className="p-4">
            <div className="flex gap-4">
              <div
                className="w-20 h-20 rounded-lg flex-shrink-0 bg-cover bg-center"
                style={{
                  backgroundImage: getImageUrl(event.image_path)
                    ? `url(${getImageUrl(event.image_path)})`
                    : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                }}
              />
              <div className="flex-1 min-w-0">
                <h2 className="truncate">{event.name}</h2>
                <p className="text-sm text-muted-foreground">
                  {new Date(event.date).toLocaleDateString("de-DE")}
                </p>
                <Badge variant="secondary" className="mt-2">
                  Aktiv
                </Badge>
              </div>
            </div>
          </Card>
        )}

        <div>
          <h3 className="mb-3">Statistiken</h3>
          <div className="grid grid-cols-3 gap-3">
            {stats.map((stat) => {
              const Icon = stat.icon;
              return (
                <Card key={stat.label} className="p-4 text-center">
                  <Icon className="w-5 h-5 mx-auto mb-2 text-primary" />
                  <p className="text-xl mb-1">{stat.value}</p>
                  <p className="text-xs text-muted-foreground">{stat.label}</p>
                </Card>
              );
            })}
          </div>
        </div>

        <div>
          <h3 className="mb-3">Aktionen</h3>
          <div className="space-y-3">
            <Button
              variant="outline"
              className="w-full h-12 justify-start gap-3"
              onClick={() => navigate("/create-event")}
            >
              <Edit className="w-5 h-5" />
              Neues Event erstellen
            </Button>

            <Button variant="outline" className="w-full h-12 justify-start gap-3">
              <Star className="w-5 h-5" />
              Event hervorheben
            </Button>

            <Button variant="outline" className="w-full h-12 justify-start gap-3">
              <XCircle className="w-5 h-5" />
              Deaktivieren
            </Button>

            <Button
              variant="outline"
              className="w-full h-12 justify-start gap-3 text-destructive hover:text-destructive"
              onClick={() => setShowDeleteDialog(true)}
            >
              <Trash2 className="w-5 h-5" />
              Löschen
            </Button>
          </div>
        </div>

        <div>
          <Button className="w-full h-12 gap-3 bg-primary hover:bg-primary/90">
            <QrCode className="w-5 h-5" />
            QR-Code Scanner öffnen
          </Button>
        </div>
      </div>

      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="max-w-sm mx-4">
          <AlertDialogHeader>
            <div className="flex justify-center mb-4">
              <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center">
                <Trash2 className="w-6 h-6 text-destructive" />
              </div>
            </div>
            <AlertDialogTitle className="text-center">Event löschen?</AlertDialogTitle>
            <AlertDialogDescription className="text-center">
              Diese Aktion kann nicht rückgängig gemacht werden. Alle Tickets werden storniert.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="flex-col sm:flex-col gap-2">
            <AlertDialogCancel className="m-0">Abbrechen</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="m-0 bg-destructive hover:bg-destructive/90"
            >
              Löschen
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
