import { useState } from "react";
import { Settings } from "lucide-react";
import { useTheme } from "next-themes";
import { Switch } from "./ui/switch";
import { Label } from "./ui/label";

export function AccessibilityButton() {
  const [showMenu, setShowMenu] = useState(false);
  const { theme, setTheme } = useTheme();

  return (
    <>
      <button
        onClick={() => setShowMenu(!showMenu)}
        className="fixed bottom-20 right-4 z-50 bg-primary text-primary-foreground rounded-full p-3 shadow-lg min-w-[44px] min-h-[44px] flex items-center justify-center"
        aria-label="Accessibility Settings"
      >
        <Settings className="w-5 h-5" />
      </button>

      {showMenu && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setShowMenu(false)} />
          <div className="fixed bottom-32 right-4 z-50 bg-card border border-border rounded-xl p-4 shadow-xl min-w-[240px]">
            <h3 className="mb-4">Barrierefreiheit</h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="dark-mode">Dark Mode</Label>
                <Switch
                  id="dark-mode"
                  checked={theme === "dark"}
                  onCheckedChange={(checked) => setTheme(checked ? "dark" : "light")}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label htmlFor="high-contrast">Hoher Kontrast</Label>
                <Switch id="high-contrast" />
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
