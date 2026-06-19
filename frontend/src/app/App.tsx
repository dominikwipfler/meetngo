import { BrowserRouter, Routes, Route, Navigate } from "react-router";
import type { ReactNode } from "react";
import { ThemeProvider } from "./components/ThemeProvider";
import { AuthProvider, useAuth } from "../context/AuthContext";
import { BottomNav } from "./components/BottomNav";
import { AccessibilityButton } from "./components/AccessibilityButton";
import { LoginScreen } from "./screens/LoginScreen";
import { RegisterScreen } from "./screens/RegisterScreen";
import { MapScreen } from "./screens/MapScreen";
import { SearchScreen } from "./screens/SearchScreen";
import { TicketsScreen } from "./screens/TicketsScreen";
import { ProfileScreen } from "./screens/ProfileScreen";
import { ProfileSettingsScreen } from "./screens/ProfileSettingsScreen";
import { EventDetailScreen } from "./screens/EventDetailScreen";
import { CreateEventScreen } from "./screens/CreateEventScreen";
import { OrganizerDashboardScreen } from "./screens/OrganizerDashboardScreen";

// Scrollbarer Bereich + BottomNav (Suche, Tickets, Profil)
function NavLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col min-h-0 relative">
        {children}
      </div>
      <BottomNav />
    </>
  );
}

// Nicht-scrollbarer Bereich + BottomNav (Karte)
function NavLayoutFixed({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="flex-1 overflow-hidden flex flex-col min-h-0 relative">{children}</div>
      <BottomNav />
    </>
  );
}

// Vollbild ohne BottomNav (Login, Detail, Erstellen …)
function PageLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col min-h-0 relative">
      {children}
    </div>
  );
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/" replace />;
  return <>{children}</>;
}

// Schwebender Barrierefreiheits-Button, nur sichtbar wenn eingeloggt
function GlobalAccessibilityButton() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return null;
  return <AccessibilityButton />;
}

function AppRoutes() {
  return (
    <BrowserRouter>
      <GlobalAccessibilityButton />
      <Routes>
        <Route
          path="/"
          element={
            <PageLayout>
              <LoginScreen />
            </PageLayout>
          }
        />
        <Route
          path="/register"
          element={
            <PageLayout>
              <RegisterScreen />
            </PageLayout>
          }
        />

        <Route
          path="/map"
          element={
            <ProtectedRoute>
              <NavLayoutFixed>
                <MapScreen />
              </NavLayoutFixed>
            </ProtectedRoute>
          }
        />
        <Route
          path="/search"
          element={
            <ProtectedRoute>
              <NavLayout>
                <SearchScreen />
              </NavLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/tickets"
          element={
            <ProtectedRoute>
              <NavLayout>
                <TicketsScreen />
              </NavLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <NavLayout>
                <ProfileScreen />
              </NavLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile/settings"
          element={
            <ProtectedRoute>
              <PageLayout>
                <ProfileSettingsScreen />
              </PageLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/event/:id"
          element={
            <ProtectedRoute>
              <PageLayout>
                <EventDetailScreen />
              </PageLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/create-event"
          element={
            <ProtectedRoute>
              <PageLayout>
                <CreateEventScreen />
              </PageLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/organizer-dashboard"
          element={
            <ProtectedRoute>
              <PageLayout>
                <OrganizerDashboardScreen />
              </PageLayout>
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </ThemeProvider>
  );
}
