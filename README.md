# ThreadTrades

A Tinder-style clothing-swap app: upload an item, swipe on other people's items,
match on a mutual like, message the other person, and arrange a swap. Built to
completion of the core loop — upload → swipe → match → message → swap — plus
profiles, reviews, and an admin moderation console.

This is a from-scratch rewrite of an [earlier university group project](https://github.com/ethkatzy/ThreadTradesV1). See
[`AUDIT.md`](AUDIT.md) for the audit of that codebase (what was kept
conceptually vs. rebuilt) and [`ROADMAP.md`](ROADMAP.md) for deferred
decisions, current feature status, and implementation notes.

## [Live demo](https://threadtrades-frontend.onrender.com)

Both services run on Render's free tier, so the first request after a period
of inactivity can take up to a minute while the service spins back up. The backend takes longer to load, so 
if you can't log in, wait a few more minutes The database is hosted separately on Neon.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 4 (Web MVC, Security, Data JPA, WebSocket/STOMP, Actuator), Flyway migrations, PostgreSQL |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4 |
| Auth | httpOnly session cookie (JWT-backed) + CSRF double-submit cookie — see `ROADMAP.md` §3 |
| Realtime | STOMP over WebSocket for chat messages and swap status updates |
| Object storage | Pluggable `StorageService` — local disk in dev, Cloudflare R2 in production |
| Testing | JUnit + Testcontainers (backend, real Postgres), Vitest + Testing Library (frontend) |
| Deployment | Docker images on Render, Postgres on Neon/Supabase |

## How the hosted version works

The live demo is two separate Render web services built from
`backend/Dockerfile` and `frontend/Dockerfile` (see `render.yaml` for the
blueprint), backed by a Postgres instance on Neon or Supabase rather than
Render's own (which expires after 30 days). A few things differ from local
dev because of this split-host setup:

- **Auth cookie:** the session cookie is `SameSite=None; Secure` in
  production (frontend and backend are on different `onrender.com`
  subdomains, which count as cross-**site**, not just cross-origin) vs.
  `SameSite=Lax` locally.
- **File storage:** uploaded images go to a Cloudflare R2 bucket
  (`STORAGE_PROVIDER=r2`) instead of local disk, so uploads survive
  redeploys.
- **Cold starts:** free-tier Render services spin down after 15 minutes of
  inactivity and take up to ~1 minute to wake back up on the next request.

## Running locally

### Prerequisites

- Docker and Docker Compose
- (Only if running the backend or frontend outside Docker) Java 21 and Node.js 20+

### Steps

1. Copy the example environment file and fill in real values (a random
   string is fine for `JWT_SECRET`/`DB_PASSWORD` in dev):

   ```
   cp .env.example .env
   ```

2. Start everything:

   ```
   docker compose up -d
   ```

   This starts Postgres, the backend (Spring Boot, on http://localhost:8080),
   and the frontend (Next.js, on http://localhost:3000). Flyway migrations
   run automatically on backend startup.

3. Open http://localhost:3000, register an account, and go from there.

To stop everything: `docker compose down` (add `-v` to also drop the
Postgres volume — do this if you change `DB_PASSWORD` in `.env`, since the
volume persists the original credentials otherwise).

By default the app runs entirely locally with no external accounts needed:
images are stored on disk (`STORAGE_PROVIDER=local`) and auth uses relaxed
cookie flags (`COOKIE_SECURE=false`, `COOKIE_SAME_SITE=Lax`). Switching to
Cloudflare R2 for storage is optional — see the commented `R2_*` vars in
`.env.example` and `ROADMAP.md` §1.

### Running without Docker

Each service can also run directly against a Postgres instance of your
choosing:

- Backend: `cd backend && ./mvnw spring-boot:run` (reads config from
  `application.yml` / the same env vars as `.env.example`)
- Frontend: `cd frontend && npm install && npm run dev`

## Running tests

- Backend: `cd backend && ./mvnw verify` — runs against a real Postgres via
  Testcontainers, so Docker must be running even outside Compose.
- Frontend: `cd frontend && npm test`

CI (`.github/workflows/ci.yml`) runs both suites on every push.

## Project layout

```
backend/    Spring Boot API — one package per domain (auth, clothing, swipe,
            match, message, swap, review, admin, storage, security, user)
frontend/   Next.js App Router frontend — one route per page under src/app,
            shared components under src/components
.env.example      Full list of configuration variables, with comments
docker-compose.yml Local dev orchestration (Postgres + both services, hot-reloading)
render.yaml        Production deployment blueprint
AUDIT.md    Audit of the predecessor codebase this project replaces
ROADMAP.md  Feature status, deferred infrastructure decisions, and
            implementation gotchas worth knowing before touching related code
```

## License

MIT — see [`LICENSE`](LICENSE).
