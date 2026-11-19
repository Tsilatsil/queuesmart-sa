import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "QueueSmart SA",
  description:
    "Appointment booking and queue management for public service offices in South Africa. Demo project.",
};

const screenshots = [
  { file: "01-login.png", caption: "Log in or sign up" },
  { file: "02-branches.png", caption: "Choose a branch" },
  { file: "03-documents.png", caption: "Documents to bring" },
  { file: "04-time-slots.png", caption: "Pick a day and time" },
  { file: "05-confirmed.png", caption: "Booking confirmed" },
  { file: "06-my-bookings.png", caption: "My bookings" },
];

const features = [
  "Sign up and log in with hashed passwords and signed tokens",
  "Branches with opening hours, and a checklist of documents to bring for each service",
  "30-minute time slots with a capacity limit, so a slot can never be overbooked",
  "Safe booking under load: a database transaction locks the branch while it counts the slot",
  "My bookings, with cancellation that frees the place straight away",
];

const liveLinks = [
  { path: "/api/health", note: "Is the service running" },
  { path: "/api/branches", note: "List of demo branches" },
  { path: "/api/services", note: "Services and the documents each one needs" },
];

export default function Home() {
  return (
    <main className="min-h-screen bg-white text-zinc-900">
      <div className="mx-auto max-w-4xl px-6 py-16">
        <p className="text-sm font-medium uppercase tracking-wide text-emerald-700">
          Demo project
        </p>
        <h1 className="mt-2 text-4xl font-bold tracking-tight">QueueSmart SA</h1>
        <p className="mt-4 text-lg text-zinc-600">
          Appointment booking and queue management for public service offices in
          South Africa. An Android app, a REST API and a PostgreSQL database,
          deployed online.
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <a
            href="https://github.com/Tsilatsil/queuesmart-sa"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
          >
            View the code on GitHub
          </a>
          <a
            href="/api/health"
            className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium hover:bg-zinc-100"
          >
            Check the live API
          </a>
        </div>

        <h2 className="mt-14 text-2xl font-semibold">What it does</h2>
        <ul className="mt-4 list-disc space-y-2 pl-5 text-zinc-700">
          {features.map((feature) => (
            <li key={feature}>{feature}</li>
          ))}
        </ul>

        <h2 className="mt-14 text-2xl font-semibold">The app</h2>
        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
          {screenshots.map((shot) => (
            <figure key={shot.file}>
              <img
                src={`/screenshots/${shot.file}`}
                alt={shot.caption}
                className="w-full rounded-lg border border-zinc-200"
              />
              <figcaption className="mt-2 text-center text-sm text-zinc-600">
                {shot.caption}
              </figcaption>
            </figure>
          ))}
        </div>

        <h2 className="mt-14 text-2xl font-semibold">Try the live API</h2>
        <ul className="mt-4 space-y-2">
          {liveLinks.map((link) => (
            <li key={link.path}>
              <a
                href={link.path}
                className="font-mono text-emerald-700 underline"
              >
                {link.path}
              </a>
              <span className="text-zinc-600"> {link.note}</span>
            </li>
          ))}
        </ul>

        <p className="mt-16 border-t border-zinc-200 pt-6 text-sm text-zinc-500">
          Demo project. Not affiliated with the Department of Home Affairs or any
          government body. All data is fictional.
        </p>
      </div>
    </main>
  );
}