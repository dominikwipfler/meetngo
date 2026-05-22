import { useState } from "react";
import { useNavigate } from "react-router";
import { Settings, Heart } from "lucide-react";
import { Button } from "../components/ui/button";
import { Avatar } from "../components/ui/avatar";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { BottomNav } from "../components/BottomNav";
import { AccessibilityButton } from "../components/AccessibilityButton";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "../components/ui/alert-dialog";
import { events } from "../data/events";

const interests = ["Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor"];

export function ProfileScreen() {
  const navigate = useNavigate();
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);

  const handleLogout = () => {
    setShowLogoutDialog(false);
    navigate("/");
  };

  // Show first 4 events as favorites
  const favorites = events.slice(0, 4);

  return (
    <div className="min-h-screen bg-background pb-20">
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
          <h2 className="mt-4">Max Mustermann</h2>
          <p className="text-sm text-muted-foreground">@maxmustermann</p>

          <div className="flex gap-6 mt-6">
            <div className="text-center">
              <p className="text-2xl">23</p>
              <p className="text-xs text-muted-foreground">Events</p>
            </div>
            <div className="text-center">
              <p className="text-2xl">42</p>
              <p className="text-xs text-muted-foreground">Favoriten</p>
            </div>
            <div className="text-center">
              <p className="text-2xl">156</p>
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
            <h3>Favoriten</h3>
            <Heart className="w-5 h-5 text-muted-foreground" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            {favorites.map((event) => (
              <Card
                key={event.id}
                className="aspect-square rounded-xl overflow-hidden cursor-pointer hover:shadow-lg transition-shadow"
                onClick={() => navigate(`/event/${event.id}`)}
              >
                <div
                  className="w-full h-full bg-cover bg-center"
                  style={{
                    backgroundImage: event.imageUrl
                      ? `url(${event.imageUrl})`
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
            ))}
          </div>
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

      <BottomNav />
      <AccessibilityButton />

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
