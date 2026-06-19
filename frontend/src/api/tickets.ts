import { apiRequest } from "./client";

export interface ApiTicket {
  id: number;
  event_id: number;
  user_id: number;
  status: "active";
  created_at: string;
  event_name: string;
  event_date: string;
  event_location: string;
  event_image_path: string | null;
  event_price: string;
}

export function getMyTickets(): Promise<ApiTicket[]> {
  return apiRequest("/tickets");
}

export function createTicket(eventId: number): Promise<ApiTicket> {
  return apiRequest("/tickets", {
    method: "POST",
    body: JSON.stringify({ eventId }),
  });
}
