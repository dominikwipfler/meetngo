import { BrowserRouter, Routes, Route, Navigate } from "react-router";
import { ThemeProvider } from "./components/ThemeProvider";
import { LoginScreen } from "./screens/LoginScreen";
import { MapScreen } from "./screens/MapScreen";
import { SearchScreen } from "./screens/SearchScreen";
import { TicketsScreen } from "./screens/TicketsScreen";
import { ProfileScreen } from "./screens/ProfileScreen";
import { ProfileSettingsScreen } from "./screens/ProfileSettingsScreen";
import { EventDetailScreen } from "./screens/EventDetailScreen";
import { CreateEventScreen } from "./screens/CreateEventScreen";
import { OrganizerDashboardScreen } from "./screens/OrganizerDashboardScreen";

export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LoginScreen />} />
          <Route path="/map" element={<MapScreen />} />
          <Route path="/search" element={<SearchScreen />} />
          <Route path="/tickets" element={<TicketsScreen />} />
          <Route path="/profile" element={<ProfileScreen />} />
          <Route path="/profile/settings" element={<ProfileSettingsScreen />} />
          <Route path="/event/:id" element={<EventDetailScreen />} />
          <Route path="/create-event" element={<CreateEventScreen />} />
          <Route path="/organizer-dashboard" element={<OrganizerDashboardScreen />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}