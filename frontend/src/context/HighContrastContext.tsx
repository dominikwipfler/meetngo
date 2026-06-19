import { createContext, useContext, useEffect, useState, ReactNode } from "react";

interface HighContrastContextType {
  highContrast: boolean;
  setHighContrast: (value: boolean) => void;
}

const HighContrastContext = createContext<HighContrastContextType>({
  highContrast: false,
  setHighContrast: () => {},
});

const STORAGE_KEY = "high-contrast";

export function HighContrastProvider({ children }: { children: ReactNode }) {
  const [highContrast, setHighContrast] = useState(
    () => localStorage.getItem(STORAGE_KEY) === "true",
  );

  useEffect(() => {
    document.documentElement.classList.toggle("high-contrast", highContrast);
    localStorage.setItem(STORAGE_KEY, String(highContrast));
  }, [highContrast]);

  return (
    <HighContrastContext.Provider value={{ highContrast, setHighContrast }}>
      {children}
    </HighContrastContext.Provider>
  );
}

export const useHighContrast = () => useContext(HighContrastContext);
