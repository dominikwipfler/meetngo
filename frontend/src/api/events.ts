import { apiRequest } from './client';

export interface ApiEvent {
  id: number;
  name: string;
  description: string;
  date: string;
  location: string;
  lat: number;
  lng: number;
  organizer: string;
  organizer_id: number | null;
  price: string;
  capacity: number | null;
  attendees: number;
  category: string;
  image_path: string | null;
  featured: number;
  created_at: string;
}

export interface EventFilters {
  search?: string;
  category?: string;
  sort?: 'date' | 'name' | 'attendees' | 'price';
  order?: 'asc' | 'desc';
  priceFilter?: 'free' | 'paid' | '';
}

export function getEvents(filters: EventFilters = {}): Promise<ApiEvent[]> {
  const params = new URLSearchParams();
  if (filters.search) params.set('search', filters.search);
  if (filters.category && filters.category !== 'Alle') params.set('category', filters.category);
  if (filters.sort) params.set('sort', filters.sort);
  if (filters.order) params.set('order', filters.order);
  if (filters.priceFilter) params.set('priceFilter', filters.priceFilter);
  const qs = params.toString();
  return apiRequest(`/events${qs ? `?${qs}` : ''}`);
}

export function getEventById(id: number): Promise<ApiEvent> {
  return apiRequest(`/events/${id}`);
}

export function createEvent(formData: FormData): Promise<ApiEvent> {
  return apiRequest('/events', { method: 'POST', body: formData });
}

export function deleteEvent(id: number): Promise<{ success: boolean }> {
  return apiRequest(`/events/${id}`, { method: 'DELETE' });
}

export function getImageUrl(imagePath: string | null): string | undefined {
  if (!imagePath) return undefined;
  return imagePath;
}
