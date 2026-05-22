import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { ArrowLeft, MapPin, Calendar, Users, UserPlus } from "lucide-react";
import { Button } from "../components/ui/button";
import { Avatar } from "../components/ui/avatar";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "../components/ui/sheet";
import { Label } from "../components/ui/label";
import { AccessibilityButton } from "../components/AccessibilityButton";
import { getEventById } from "../data/events";

export function EventDetailScreen() {
  const navigate = useNavigate();
  const { id } = useParams();
  const event = getEventById(Number(id));

  const [showTicketSheet, setShowTicketSheet] = useState(false);
  const [ticketQuantity, setTicketQuantity] = useState(1);

  if (!event) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <p className="text-muted-foreground">Event nicht gefunden</p>
      </div>
    );
  }

  const handlePurchase = () => {
    setShowTicketSheet(false);
    navigate("/tickets");
  };

  const priceValue = parseFloat(event.price.replace(",", "."));
  const isFree = event.price === "Kostenlos";

  return (
    <div className="min-h-screen bg-background">
      <div className="relative h-72">
        {event.imageUrl ? (
          <div
            className="w-full h-full bg-cover bg-center"
            style={{ backgroundImage: `url(${event.imageUrl})` }}
          >
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/30" />
          </div>
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-primary to-accent" />
        )}
        <button
          onClick={() => navigate(-1)}
          className="absolute top-4 left-4 bg-card/90 backdrop-blur rounded-full p-2 min-w-[44px] min-h-[44px] flex items-center justify-center shadow-lg"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
      </div>

      <div className="px-4 -mt-6 pb-24">
        <div className="bg-card rounded-xl p-6 shadow-lg space-y-6">
          <div>
            <h1 className="text-2xl mb-3 break-words">{event.name}</h1>
            <div className="space-y-2 text-muted-foreground">
              <div className="flex items-start gap-2">
                <Calendar className="w-4 h-4 mt-0.5 flex-shrink-0" />
                <span className="text-sm">{event.date}</span>
              </div>
              <div className="flex items-start gap-2">
                <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0" />
                <span className="text-sm">{event.location}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Avatar className="w-10 h-10 bg-primary/10" />
              <div>
                <p className="text-sm">{event.organizer}</p>
                <p className="text-xs text-muted-foreground">Veranstalter</p>
              </div>
            </div>
            <Button variant="outline" size="sm" className="gap-2">
              <UserPlus className="w-4 h-4" />
              Folgen
            </Button>
          </div>

          <div>
            <div className="flex items-center gap-2 mb-2">
              <Users className="w-4 h-4 text-muted-foreground" />
              <span className="text-sm">{event.attendees} Teilnehmer</span>
            </div>
            <div className="flex -space-x-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <Avatar
                  key={i}
                  className="w-8 h-8 border-2 border-card bg-primary/20"
                />
              ))}
            </div>
          </div>

          <div>
            <h3 className="mb-2">Beschreibung</h3>
            <p className="text-sm text-muted-foreground leading-relaxed">
              {event.description}
            </p>
          </div>

          {!isFree && (
            <div className="pt-4 border-t border-border">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Preis pro Ticket</span>
                <span className="text-xl">{event.price} €</span>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="fixed bottom-0 left-0 right-0 p-4 bg-card border-t border-border">
        <Button
          onClick={() => setShowTicketSheet(true)}
          className="w-full h-12 bg-accent hover:bg-accent/90"
        >
          {isFree ? "Kostenlos teilnehmen" : "Ticket kaufen"}
        </Button>
      </div>

      <Sheet open={showTicketSheet} onOpenChange={setShowTicketSheet}>
        <SheetContent side="bottom" className="rounded-t-xl">
          <SheetHeader>
            <SheetTitle>
              {isFree ? "Teilnahme bestätigen" : "Ticket kaufen"}
            </SheetTitle>
          </SheetHeader>
          <div className="space-y-6 mt-6">
            <div>
              <Label>Ticketart</Label>
              <div className="mt-2 p-4 border border-border rounded-lg bg-muted/50">
                <div className="flex justify-between items-center">
                  <div>
                    <p>Standard Ticket</p>
                    <p className="text-sm text-muted-foreground">
                      Zugang zum Event
                    </p>
                  </div>
                  <p className="font-medium">
                    {isFree ? "Kostenlos" : `${event.price} €`}
                  </p>
                </div>
              </div>
            </div>

            <div>
              <Label>Anzahl</Label>
              <div className="flex items-center gap-4 mt-2">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setTicketQuantity(Math.max(1, ticketQuantity - 1))}
                  className="h-10 w-10"
                >
                  -
                </Button>
                <span className="text-xl w-12 text-center">{ticketQuantity}</span>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setTicketQuantity(ticketQuantity + 1)}
                  className="h-10 w-10"
                >
                  +
                </Button>
              </div>
            </div>

            {!isFree && (
              <div>
                <Label>Zahlungsmethode</Label>
                <div className="mt-2 space-y-2">
                  <div className="p-4 border border-primary rounded-lg bg-primary/5">
                    <p>PayPal</p>
                  </div>
                  <div className="p-4 border border-border rounded-lg">
                    <p>Kreditkarte</p>
                  </div>
                  <div className="p-4 border border-border rounded-lg">
                    <p>Google Pay</p>
                  </div>
                </div>
              </div>
            )}

            <div className="pt-4 border-t border-border">
              {!isFree && (
                <div className="flex justify-between mb-4">
                  <span>Gesamt</span>
                  <span className="text-xl">
                    {(priceValue * ticketQuantity).toFixed(2).replace(".", ",")} €
                  </span>
                </div>
              )}
              <Button
                onClick={handlePurchase}
                className="w-full h-12 bg-accent hover:bg-accent/90"
              >
                {isFree ? "Bestätigen" : "Jetzt kaufen"}
              </Button>
            </div>
          </div>
        </SheetContent>
      </Sheet>

      <AccessibilityButton />
    </div>
  );
}
