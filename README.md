# ThreadTrades

A Tinder-style clothing-swap app: upload an item, swipe, match, message, and arrange a
swap. This is a from-scratch rewrite of an earlier student project — see `AUDIT.md` for
the audit of that codebase and `ROADMAP.md` for deferred decisions and current status.

## Prerequisites

- Docker and Docker Compose
- (Only if running the backend or frontend outside Docker) Java 21 and Node.js 20+

## Running locally

1. Copy the example environment file and fill in real values (a random string is fine
   for `JWT_SECRET`/`DB_PASSWORD` in dev):

   ```
   cp .env.example .env
   ```

2. Start everything:

   ```
   docker compose up -d
   ```

   This starts Postgres, the backend (Spring Boot, on http://localhost:8080), and the
   frontend (Next.js, on http://localhost:3000).

3. Open http://localhost:3000, register an account, and go from there.

To stop everything: `docker compose down` (add `-v` to also drop the Postgres volume).

## Running tests

- Backend: `cd backend && ./mvnw verify` — runs against a real Postgres via
  Testcontainers, so Docker must be running.
- Frontend: `cd frontend && npm test`
