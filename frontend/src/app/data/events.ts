export interface Event {
  id: number;
  name: string;
  date: string;
  location: string;
  organizer: string;
  description: string;
  attendees: number;
  price: string;
  lat: number;
  lng: number;
  imageUrl?: string;
  category: string;
}

export const events: Event[] = [
  {
    id: 1,
    name: "Karlsruhe Jazz Festival",
    date: "25. Mai 2026, 19:00 Uhr",
    location: "Konzerthaus Karlsruhe",
    organizer: "Jazz Kulturverein",
    description:
      "Erlebe eine Nacht voller Jazz mit internationalen Künstlern. Das Festival präsentiert die besten Jazz-Acts aus ganz Europa in einer unvergesslichen Atmosphäre.",
    attendees: 234,
    price: "29,00",
    lat: 49.0069,
    lng: 8.4037,
    imageUrl: "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=800&q=80",
    category: "Musik",
  },
  {
    id: 2,
    name: "Weihnachtsmarkt",
    date: "1. Dezember 2026, 11:00 Uhr",
    location: "Marktplatz Karlsruhe",
    organizer: "Stadt Karlsruhe",
    description:
      "Der traditionelle Weihnachtsmarkt bringt festliche Stimmung in die Innenstadt. Genieße Glühwein, Lebkuchen und regionale Spezialitäten.",
    attendees: 1520,
    price: "Kostenlos",
    lat: 49.0094,
    lng: 8.4044,
    imageUrl: "https://images.unsplash.com/photo-1543589077-47d81606c1bf?w=800&q=80",
    category: "Food",
  },
  {
    id: 3,
    name: "Street Food Festival",
    date: "15. Juni 2026, 12:00 Uhr",
    location: "Stadtpark Karlsruhe",
    organizer: "Food Events GmbH",
    description:
      "Entdecke kulinarische Köstlichkeiten aus aller Welt. Über 50 Food-Trucks und Stände bieten internationale Spezialitäten.",
    attendees: 789,
    price: "Kostenlos",
    lat: 49.0086,
    lng: 8.4029,
    imageUrl: "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=800&q=80",
    category: "Food",
  },
  {
    id: 4,
    name: "Tech Meetup Karlsruhe",
    date: "10. Juni 2026, 18:00 Uhr",
    location: "Coworking Space Karlsruhe",
    organizer: "Tech Community KA",
    description:
      "Networking-Event für Tech-Enthusiasten. Vorträge über die neuesten Entwicklungen in AI, Web3 und Cloud Computing.",
    attendees: 120,
    price: "15,00",
    lat: 49.0102,
    lng: 8.4051,
    imageUrl: "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&q=80",
    category: "Tech",
  },
  {
    id: 5,
    name: "Marathon 2026",
    date: "5. September 2026, 09:00 Uhr",
    location: "Stadtmitte Karlsruhe",
    organizer: "Laufverein Karlsruhe",
    description:
      "Der jährliche Karlsruhe Marathon führt durch die schönsten Teile der Stadt. Verschiedene Distanzen für alle Fitnesslevel.",
    attendees: 3200,
    price: "45,00",
    lat: 49.0087,
    lng: 8.4043,
    imageUrl: "https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?w=800&q=80",
    category: "Sport",
  },
];

export function getEventById(id: number): Event | undefined {
  return events.find((event) => event.id === id);
}
