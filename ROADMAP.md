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

| # | Feature | Why it's in |
|---|---|---|
| 1 | Registration / login | Table stakes. |
| 3+4 | Clothing upload — **one** unified path (object storage, replacing both the old BLOB and disk-file systems) | Core loop start. Also where the old app's worst anti-pattern (two disconnected upload systems, AUDIT.md §9) gets fixed by construction — there will only ever be one path. |
| 2 | Home dashboard | Minimal version (your items + entry points) — needed as a landing page, not over-built. |
| 5 | Swipe deck | Core loop. |
| 6 | Match detection | Core loop — atomic server-side detection from day one (AUDIT.md §10/§13), not bolted on later like the old app's dead async logic. |
| 7 | Matches list | Real matches, not the old hardcoded-minus-one-user hack. |
| 11 | Item detail view | Needed to make swipe/matches meaningful — you have to be able to see what you matched on. |
| 12 | Messaging | Real, persisted messaging tied to a match, over WebSocket (STOMP) per the tech-stack decision. Core loop, not deferrable. |
| 8+9 | Swap accept/reject | Core loop close-out — real state transitions on one `swap` row (see `V1__init_schema.sql`), not new rows per accept/reject. |
| 13+14 | Profile view + edit | Needed for a usable account, low effort. |

### Deferred past v1

| # | Feature | Why it waits |
|---|---|---|
| 10 | Swap history | Once accept/reject exist and are correct, this is a filtered list view over the same table — cheap to add later. |
| 15 | Dark mode | Pure polish, no product risk in deferring. |
| 17 | Reviews/ratings | Never shipped in the old app either (scaffolded via JDL, no product UI) — treat as a genuinely new feature to scope later, not a resurrection. |
| 18 | Privacy/GDPR page | Needed before any real deployment with real users, but not before you can use the app yourself. Must be templated/generic this time — the old page leaked real personal contact info (AUDIT.md §9). |
| 19–20 | Admin CRUD / admin console | Free with JHipster before; not free without it. Direct DB access covers a solo dev's admin needs at MVP stage. |
| 16 | "Rainbow text" | Confirmed abandoned/never shipped in the old app (AUDIT.md §11) — not a requirement unless specifically revived. |

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
