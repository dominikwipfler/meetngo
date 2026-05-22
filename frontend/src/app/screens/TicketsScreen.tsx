import { useState, useEffect } from "react";
import { QrCode, RefreshCw } from "lucide-react";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { Button } from "../components/ui/button";
import { useNavigate } from "react-router";
import { getEvents, getImageUrl } from "../../api/events";
import type { ApiEvent } from "../../api/events";

interface Ticket {
  id: number;
  eventId: number;
  status: "active" | "pending" | "used";
  transferNote?: string;
}

const tickets: Ticket[] = [
  { id: 1, eventId: 1, status: "active" },
  { id: 2, eventId: 3, status: "active" },
  { id: 3, eventId: 4, status: "pending", transferNote: "Weiterverkauf läuft..." },
  { id: 4, eventId: 2, status: "used" },
];

export function TicketsScreen() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("active");
  const [apiEvents, setApiEvents] = useState<ApiEvent[]>([]);

  useEffect(() => {
    getEvents().then(setApiEvents).catch(() => {});
  }, []);

  const getStatusBadge = (status: string) => {
    const variants: Record<string, { label: string; className: string }> = {
      active: {
        label: "Aktiv",
        className: "bg-green-500 text-white hover:bg-green-500",
      },
      pending: {
        label: "Ausstehend",
        className: "bg-amber-500 text-white hover:bg-amber-500",
      },
      used: {
        label: "Verwendet",
        className: "bg-gray-500 text-white hover:bg-gray-500",
      },
    };

    const variant = variants[status] || variants.active;
    return <Badge className={variant.className}>{variant.label}</Badge>;
  };

  const getTicketsByStatus = (status: Ticket["status"]) => {
    return tickets
      .filter((ticket) => ticket.status === status)
      .map((ticket) => {
        const event = apiEvents.find((e) => e.id === ticket.eventId);
        return { ...ticket, event };
      })
      .filter((t) => t.event);
  };

  const activeTickets = getTicketsByStatus("active");
  const pendingTickets = getTicketsByStatus("pending");
  const pastTickets = getTicketsByStatus("used");

  return (
    <div className="flex-1 flex flex-col bg-background">
      <div className="sticky top-0 bg-card border-b border-border px-4 py-4 z-10">
        <h1>Meine Tickets</h1>
      </div>

      <div className="p-4">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-3 mb-6">
            <TabsTrigger value="active">Aktiv</TabsTrigger>
            <TabsTrigger value="pending">Ausstehend</TabsTrigger>
            <TabsTrigger value="past">Vergangen</TabsTrigger>
          </TabsList>

          <TabsContent value="active" className="space-y-3">
            {activeTickets.map((ticket) => (
              <Card
                key={ticket.id}
                className="p-4 cursor-pointer hover:shadow-lg transition-shadow"
                onClick={() => navigate(`/event/${ticket.event?.id}`)}
              >
                <div className="flex gap-3 mb-3">
                  <div
                    className="w-16 h-16 rounded-lg flex-shrink-0 bg-cover bg-center"
                    style={{
                      backgroundImage: getImageUrl(ticket.event?.image_path ?? null)
                        ? `url(${getImageUrl(ticket.event?.image_path ?? null)})`
                        : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <h3 className="truncate">{ticket.event?.name}</h3>
                      {getStatusBadge(ticket.status)}
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {ticket.event?.date ? new Date(ticket.event.date).toLocaleDateString("de-DE") : ""}
                    </p>
                    <p className="text-sm text-muted-foreground truncate">
                      {ticket.event?.location}
                    </p>
                  </div>
                </div>
                <div className="flex gap-2 pt-3 border-t border-border">
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 gap-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <QrCode className="w-4 h-4" />
                    QR-Code
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 gap-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <RefreshCw className="w-4 h-4" />
                    Weiterverkaufen
                  </Button>
                </div>
              </Card>
            ))}
            {activeTickets.length === 0 && (
              <div className="text-center py-12">
                <p className="text-muted-foreground">Keine aktiven Tickets</p>
              </div>
            )}
          </TabsContent>

          <TabsContent value="pending" className="space-y-3">
            {pendingTickets.map((ticket) => (
              <Card key={ticket.id} className="p-4 opacity-75">
                <div className="flex gap-3">
                  <div
                    className="w-16 h-16 rounded-lg flex-shrink-0 bg-cover bg-center"
                    style={{
                      backgroundImage: getImageUrl(ticket.event?.image_path ?? null)
                        ? `url(${getImageUrl(ticket.event?.image_path ?? null)})`
                        : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <h3 className="truncate">{ticket.event?.name}</h3>
                      {getStatusBadge(ticket.status)}
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {ticket.event?.date ? new Date(ticket.event.date).toLocaleDateString("de-DE") : ""}
                    </p>
                    <p className="text-sm text-muted-foreground truncate">
                      {ticket.event?.location}
                    </p>
                    {ticket.transferNote && (
                      <p className="text-sm text-amber-600 mt-2">
                        {ticket.transferNote}
                      </p>
                    )}
                  </div>
                </div>
              </Card>
            ))}
            {pendingTickets.length === 0 && (
              <div className="text-center py-12">
                <p className="text-muted-foreground">Keine ausstehenden Tickets</p>
              </div>
            )}
          </TabsContent>

          <TabsContent value="past" className="space-y-3">
            {pastTickets.map((ticket) => (
              <Card key={ticket.id} className="p-4 opacity-60">
                <div className="flex gap-3">
                  <div
                    className="w-16 h-16 rounded-lg flex-shrink-0 bg-cover bg-center"
                    style={{
                      backgroundImage: getImageUrl(ticket.event?.image_path ?? null)
                        ? `url(${getImageUrl(ticket.event?.image_path ?? null)})`
                        : "linear-gradient(135deg, #1D9E75 0%, #D85A30 100%)",
                    }}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <h3 className="truncate">{ticket.event?.name}</h3>
                      {getStatusBadge(ticket.status)}
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {ticket.event?.date ? new Date(ticket.event.date).toLocaleDateString("de-DE") : ""}
                    </p>
                    <p className="text-sm text-muted-foreground truncate">
                      {ticket.event?.location}
                    </p>
                  </div>
                </div>
              </Card>
            ))}
            {pastTickets.length === 0 && (
              <div className="text-center py-12">
                <p className="text-muted-foreground">Keine vergangenen Tickets</p>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>

    </div>
  );
}
