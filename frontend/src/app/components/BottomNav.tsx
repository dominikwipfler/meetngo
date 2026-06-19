import { Map, Search, Ticket, User } from "lucide-react";
import { useNavigate, useLocation } from "react-router";

export function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { icon: Map, label: "Karte", path: "/map" },
    { icon: Search, label: "Suche", path: "/search" },
    { icon: Ticket, label: "Tickets", path: "/tickets" },
    { icon: User, label: "Profil", path: "/profile" },
  ];

  return (
    <nav
      className="flex-shrink-0 bg-card border-t border-border z-50"
      style={{ paddingBottom: "var(--safe-b)" }}
    >
      <div className="flex items-center justify-around h-16">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`flex flex-col items-center justify-center gap-1 flex-1 h-full px-2 transition-colors ${
                isActive ? "text-primary" : "text-muted-foreground"
              }`}
            >
              <Icon className={`w-5 h-5 transition-transform ${isActive ? "scale-110" : ""}`} />
              <span className="text-xs font-medium">{item.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
