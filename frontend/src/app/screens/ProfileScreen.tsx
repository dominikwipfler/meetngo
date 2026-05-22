import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Settings, BarChart2, Plus } from "lucide-react";
import { Button } from "../components/ui/button";
import { Avatar } from "../components/ui/avatar";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "../components/ui/alert-dialog";
import { useAuth } from "../../context/AuthContext";
import { getEvents, getImageUrl } from "../../api/events";
import type { ApiEvent } from "../../api/events";

const interests = ["Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor"];

export function ProfileScreen() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [myEvents, setMyEvents] = useState<ApiEvent[]>([]);

  useEffect(() => {
    getEvents({ sort: "date", order: "asc" })
      .then((data) => setMyEvents(data.filter((e) => e.organizer_id === user?.id)))
      .catch(() => {});
  }, [user?.id]);

  const handleLogout = () => {
    setShowLogoutDialog(false);
    logout();
    navigate("/");
  };

  return (
    <div className="bg-background pb-6">
      <div className="relative bg-gradient-to-br from-primary to-accent h-32">
        <button
          onClick={() => navigate("/profile/settings")}
          className="absolute top-4 right-4 text-white min-w-[44px] min-h-[44px] flex items-center justify-center"
        >
          <Settings className="w-6 h-6" />
        </button>
      </div>

      <div className="px-4 -mt-16 pb-6">
        <div className="flex flex-col items-center">
          <Avatar className="w-24 h-24 border-4 border-card bg-muted" />
          <h2 className="mt-4">{user?.username ?? "Gast"}</h2>
          <p className="text-sm text-muted-foreground">@{user?.username?.toLowerCase().replace(/\s/g, "") ?? "gast"}</p>
          {user?.email && (
            <p className="text-xs text-muted-foreground mt-1">{user.email}</p>
          )}

          <div className="flex gap-6 mt-6">
            <div className="text-center">
              <p className="text-2xl">{myEvents.length}</p>
              <p className="text-xs text-muted-foreground">Events</p>
            </div>
            <div className="text-center">
              <p className="text-2xl">0</p>
              <p className="text-xs text-muted-foreground">Favoriten</p>
            </div>
            <div className="text-center">
              <p className="text-2xl">0</p>
              <p className="text-xs text-muted-foreground">Abonnenten</p>
            </div>
          </div>
        </div>

        <div className="mt-8">
          <h3 className="mb-3">Interessen</h3>
          <div className="flex flex-wrap gap-2">
            {interests.map((interest) => (
              <Badge key={interest} variant="secondary" className="px-3 py-1">
                {interest}
              </Badge>
            ))}
          </div>
        </div>

        <div className="mt-8">
          <div className="flex items-center justify-between mb-3">
            <h3>Meine Events</h3>
            <button
              onClick={() => navigate("/organizer-dashboard")}
              className="flex items-center gap-1 text-sm text-primary font-medium"
            >
              <BarChart2 className="w-4 h-4" />
              Dashboard
            </button>
          </div>

          {myEvents.length === 0 ? (
            <div className="border border-dashed border-border rounded-xl p-6 flex flex-col items-center gap-3 text-center">
              <p className="text-sm text-muted-foreground">Du hast noch keine Events erstellt.</p>
              <Button
                size="sm"
                className="bg-accent hover:bg-accent/90 gap-2"
                onClick={() => navigate("/create-event")}
              >
                <Plus className="w-4 h-4" />
                Event erstellen
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              {myEvents.slice(0, 4).map((event) => {
                const imageUrl = getImageUrl(event.image_path);
                return (
                  <Card
                    key={event.id}
                    className="aspect-square rounded-xl overflow-hidden cursor-pointer hover:shadow-lg transition-shadow"
                    onClick={() => navigate(`/event/${event.id}`)}
                  >
                    <div
                      className="w-full h-full bg-cover bg-center"
                      style={{
                        backgroundImage: imageUrl
                          ? `url(${imageUrl})`
                          : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                      }}
                    >
                      <div className="w-full h-full bg-gradient-to-t from-black/60 via-transparent to-transparent flex items-end p-3">
                        <p className="text-sm text-white font-medium line-clamp-2">
                          {event.name}
                        </p>
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}

          {myEvents.length > 0 && (
            <Button
              variant="outline"
              className="w-full mt-3 gap-2"
              onClick={() => navigate("/organizer-dashboard")}
            >
              <BarChart2 className="w-4 h-4" />
              Alle Events & Statistiken
            </Button>
          )}
        </div>

        <div className="mt-8">
          <Button
            variant="outline"
            className="w-full h-12 text-destructive hover:text-destructive"
            onClick={() => setShowLogoutDialog(true)}
          >
            Ausloggen
          </Button>
        </div>
      </div>

      <AlertDialog open={showLogoutDialog} onOpenChange={setShowLogoutDialog}>
        <AlertDialogContent className="max-w-sm mx-4">
          <AlertDialogHeader>
            <AlertDialogTitle>Bist du sicher?</AlertDialogTitle>
            <AlertDialogDescription>
              Möchtest du dich wirklich ausloggen?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Nein</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleLogout}
              className="bg-accent hover:bg-accent/90"
            >
              Ja, ausloggen
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
