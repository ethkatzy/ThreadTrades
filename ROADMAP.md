# ThreadTrades — Roadmap / Deferred Decisions

Decisions intentionally punted past initial scaffolding. Each one is designed to be
swappable later without a rewrite (see the `storage/` interface pattern and Docker Compose
setup in the initial scaffold) — revisit when the trigger condition hits, not on a fixed date.

---

## 0. MVP scope

Core loop for v1: **upload an item → swipe → match → message → complete a swap.**
Everything not load-bearing for that loop is deferred, even where the old app had a
version of it nominally "working." Feature numbers refer to AUDIT.md §4.

### In v1

| # | Feature | Status | Why it's in |
|---|---|---|---|
| 1 | Registration / login | ✅ Done (backend JWT auth + frontend login/register pages) | Table stakes. |
| 3+4 | Clothing upload — **one** unified path (object storage, replacing both the old BLOB and disk-file systems) | ✅ Done (`StorageService`, upload/list/get endpoints; frontend upload page) | Core loop start. Also where the old app's worst anti-pattern (two disconnected upload systems, AUDIT.md §9) gets fixed by construction — there will only ever be one path. |
| 2 | Home dashboard | ✅ Done (minimal — your items + entry points) | Minimal version (your items + entry points) — needed as a landing page, not over-built. |
| 5 | Swipe deck | ✅ Done (`GET /api/swipes/deck`, `POST /api/swipes`; frontend `/swipe` page with drag gesture + buttons) | Core loop. |
| 6 | Match detection | ✅ Done (mutual-LIKE detection built atomically into `POST /api/swipes` via `SwipeService`/`ItemMatch`, not deferred — per AUDIT.md §10/§13) | Core loop — atomic server-side detection from day one, not bolted on later like the old app's dead async logic. |
| 7 | Matches list | ⬜ Not started (matches are detected and persisted; swiping surfaces an inline "it's a match" banner, but there's no `/matches` page or list endpoint yet) | Real matches, not the old hardcoded-minus-one-user hack. |
| 11 | Item detail view | ⬜ Not started (backend `GET /api/clothing-items/{id}` exists; no frontend page) | Needed to make swipe/matches meaningful — you have to be able to see what you matched on. |
| 12 | Messaging | ⬜ Not started | Real, persisted messaging tied to a match, over WebSocket (STOMP) per the tech-stack decision. Core loop, not deferrable. |
| 8+9 | Swap accept/reject | ⬜ Not started | Core loop close-out — real state transitions on one `swap` row (see `V1__init_schema.sql`), not new rows per accept/reject. |
| 13+14 | Profile view + edit | ⬜ Not started (backend `GET /api/users/me` exists; no edit endpoint or frontend pages) | Needed for a usable account, low effort. |

### Deferred past v1

| # | Feature | Why it waits |
|---|---|---|
| 10 | Swap history | Once accept/reject exist and are correct, this is a filtered list view over the same table — cheap to add later. |
| 15 | Dark mode | Pure polish, no product risk in deferring. |
| 17 | Reviews/ratings | Never shipped in the old app either (scaffolded via JDL, no product UI) — treat as a genuinely new feature to scope later, not a resurrection. |
| 18 | Privacy/GDPR page | Needed before any real deployment with real users, but not before you can use the app yourself. Must be templated/generic this time — the old page leaked real personal contact info (AUDIT.md §9). |
| 19–20 | Admin CRUD / admin console | Free with JHipster before; not free without it. Direct DB access covers a solo dev's admin needs at MVP stage. |

---

## 1. Object / file storage backend

**Current state:** Local disk storage behind a `StorageService` interface
(`backend/.../storage/`), so switching providers later is a config + one new
implementation class, not a rewrite. Revisit when you need the app reachable from
somewhere other than your own machine (see deployment, below) — local disk doesn't
survive a redeploy or scale past one instance.

| Option | Pros | Cons |
|---|---|---|
| **Cloudflare R2** | S3-compatible API (same SDK/code as S3); **no egress fees**, which matters for an image-heavy app people browse a lot; generous free tier | Smaller ecosystem than AWS; fewer tutorials/Stack Overflow answers if something goes wrong |
| **AWS S3** | Most standard/ubiquitous choice; best tooling and docs; pairs naturally if you ever deploy on AWS (EC2/ECS/Lambda) | Egress fees can add up for an image-browsing app; more complex IAM/permissions model to get right |
| **Self-hosted MinIO (Docker)** | Zero external account/cost; runs alongside Postgres in the same `docker-compose.yml`; fully S3-API-compatible so code doesn't change if you migrate to real S3/R2 later | You own the ops (backups, disk space, uptime); doesn't solve the "reachable from the internet" problem on its own — still needs a real host with persistent storage |
| **Local disk (current default)** | Zero setup, zero cost, works today | Not portable, doesn't survive redeploys/scaling, only viable for local development |

**Leaning:** R2 is the natural next step when you leave local disk — cheapest at this
app's likely traffic pattern (lots of image reads, few writes) and a drop-in S3-compatible
swap given the interface is already abstracted.

---

## 2. Deployment target

**Current state:** Docker Compose, local machine only. No public-facing instance exists yet.
Revisit once the core loop (upload → swipe → match → message → swap) works end-to-end locally
and you want it reachable by anyone other than you.

| Option | Pros | Cons |
|---|---|---|
| **Fly.io** | Docker-native (deploys the same image you already build for Compose); generous free tier; easy managed Postgres add-on; global edge if latency ever matters | Free tier limits can be tight for always-on + a DB; less beginner-hand-holding than Railway |
| **Railway** | Lowest friction — connect GitHub repo, it builds and deploys automatically; built-in Postgres with a nice dashboard; usually fastest path from "code" to "live URL" | Free tier is usage-capped and can get pricey faster than Fly.io at real usage; less control over infra details |
| **VPS (self-managed)** | Full control — same shape as the old app's Caddy/ARM64 VM setup, just done correctly this time (real Let's Encrypt certs, not staging); no platform lock-in or vendor pricing surprises | You own all the ops: OS patching, Docker/Caddy config, TLS renewal, monitoring, backups — meaningfully more work for a solo dev |
| **Not yet / Compose only (current)** | No decision needed, no cost, fastest to keep iterating on features | Not reachable by anyone but you; no real-world testing of network/auth edge cases until you pick one |

**Leaning:** Fly.io or Railway are both reasonable defaults for a solo project at this
stage — pick Railway if you want the least ops thinking, Fly.io if you want a bit more
control and don't mind a slightly steeper setup. A VPS only makes sense if you specifically
want the ops experience or hit a wall on managed-platform pricing/limits.

---

## 3. Notes from implementation (discovered along the way)

Gotchas and small unresolved trade-offs hit while building auth, upload, and the
dashboard. Not decisions that need to be made now — just things worth knowing before
they bite again.

- **Server-side vs. browser API URL diverge inside Docker.** `NEXT_PUBLIC_API_URL=http://localhost:8080`
  is correct for the browser but wrong for anything the Next.js *server process* itself
  needs to fetch from inside the `frontend` container (its own `next/image` optimizer
  today; any future Server Component or Route Handler that calls the backend directly
  tomorrow) — `localhost` there means the frontend container, not `backend`. Hasn't
  caused a real failure yet because the only server-side fetch so far was verified with
  the frontend running on the host, not in Compose. The day server-side code needs to
  reach the API from inside that container, it'll need its own env var pointed at
  `http://backend:8080`.
- **JWT lives in browser `localStorage`, not an httpOnly cookie.** A deliberate
  minimal-scope choice to get auth working without standing up a session/cookie layer.
  Trade-off: vulnerable to XSS token theft in a way an httpOnly cookie isn't. Revisit
  before any real deployment (ties into deployment target, #2 above) — likely via
  Next.js Route Handlers acting as a BFF that sets an httpOnly session cookie, per
  Next's own authentication guide.
- **Next.js 16's image optimizer blocks private/loopback IPs by default (SSRF guard).**
  `localhost` triggers it, so local dev needed `images.dangerouslyAllowLocalIP: true` in
  `next.config.ts`. Safe today because `remotePatterns` already pins requests to exactly
  the backend's configured host/port/`/uploads/**` path — re-check that reasoning if the
  API URL config ever becomes more dynamic (e.g. multiple allowed backends).
- **This Spring Boot 4 / Spring Framework 7 stack runs on Jackson 3 (`tools.jackson`)
  internally**, while the `jjwt-jackson` dependency still pulls Jackson 2
  (`com.fasterxml.jackson`) in as a runtime-only transitive. Don't autowire
  `com.fasterxml.jackson.databind.ObjectMapper` in tests — it's not reliably on the
  compile classpath. Use raw JSON strings + `JsonPath` instead (see
  `AuthControllerTest` / `ClothingItemControllerTest`).
- **Spring Boot 4 moved MockMvc's test annotations.** `@AutoConfigureMockMvc` now lives
  at `org.springframework.boot.webmvc.test.autoconfigure`, not the classic
  `org.springframework.boot.test.autoconfigure.web.servlet` path.
- **`@DynamicPropertySource` suppliers are re-invoked on every property lookup, not
  cached once.** A supplier like `() -> ... + UUID.randomUUID()` will hand different
  beans different values within the same test run. Compute anything that needs to stay
  stable once into a `static final` and have the supplier just return it.
- **Node 25 ships a native `localStorage` global that's a non-functional stub without a
  `--localstorage-file` flag**, and it silently shadows jsdom's working implementation
  in Vitest. Polyfilled manually in `frontend/vitest.setup.ts` — revisit if a future
  Node/jsdom/Vitest upgrade fixes the interaction upstream.
- **The `postgres-data` Docker volume persists across `docker compose down` and
  rebuilds.** If `.env`'s `DB_PASSWORD` ever changes, Postgres won't pick it up (only
  applied on first init of an empty data directory), and the backend fails to connect
  with an auth error. Reset the volume (`docker volume rm threadtrades_postgres-data`)
  when rotating dev credentials.
