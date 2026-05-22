import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Edit, Star, XCircle, Trash2, QrCode, TrendingUp } from "lucide-react";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "../components/ui/alert-dialog";
import { AccessibilityButton } from "../components/AccessibilityButton";
import { events } from "../data/events";

export function OrganizerDashboardScreen() {
  const navigate = useNavigate();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  // Using the first event as example for organizer dashboard
  const event = events[0];

  const stats = [
    { label: "Verkaufte Tickets", value: "234", icon: TrendingUp },
    { label: "Einnahmen", value: "6.786€", icon: TrendingUp },
    { label: "Aufrufe", value: "1.523", icon: TrendingUp },
  ];

  const handleDelete = () => {
    setShowDeleteDialog(false);
    navigate("/map");
  };

  return (
    <div className="min-h-screen bg-background pb-6">
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
        <Card className="p-4">
          <div className="flex gap-4">
            <div
              className="w-20 h-20 rounded-lg flex-shrink-0 bg-cover bg-center"
              style={{
                backgroundImage: event.imageUrl
                  ? `url(${event.imageUrl})`
                  : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
              }}
            />
            <div className="flex-1 min-w-0">
              <h2 className="truncate">{event.name}</h2>
              <p className="text-sm text-muted-foreground">
                {event.date.split(",")[0]}
              </p>
              <Badge variant="secondary" className="mt-2">
                Aktiv
              </Badge>
            </div>
          </div>
        </Card>

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
              Bearbeiten
            </Button>

            <Button
              variant="outline"
              className="w-full h-12 justify-start gap-3"
            >
              <Star className="w-5 h-5" />
              Event hervorheben
            </Button>

            <Button
              variant="outline"
              className="w-full h-12 justify-start gap-3"
            >
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

      <AccessibilityButton />

      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="max-w-sm mx-4">
          <AlertDialogHeader>
            <div className="flex justify-center mb-4">
              <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center">
                <Trash2 className="w-6 h-6 text-destructive" />
              </div>
            </div>
            <AlertDialogTitle className="text-center">
              Event löschen?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-center">
              Diese Aktion kann nicht rückgängig gemacht werden. Alle Tickets
              werden storniert.
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
