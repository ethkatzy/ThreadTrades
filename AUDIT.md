# ThreadTrades — Legacy Codebase Audit

**Audit date:** 2026-08-06
**Audited by:** Claude Code (static analysis of source + git history; app not run)
**Purpose:** This is the full spec + checklist carried forward into the ThreadTrades rewrite. The old repository will not be accessible once the new project starts, so anything worth knowing must live in this file.

---

## 0. TL;DR

ThreadTrades is a JHipster-generated Spring Boot + Angular monolith, built by a 6-person student team (Jan–Apr 2024) as a Tinder-style clothing-swap app. **The domain concept and data model are sound and worth keeping conceptually.** Almost nothing else is worth keeping literally:

- **Swipe/match detection is dead code** — an async/sync bug means it always evaluates to "not a match." A real mutual-match algorithm was never actually working in production.
- **Messaging is 100% fake** — the real backend (`Message` entity, custom repository query, REST endpoints) is fully built and never called. The UI is a hardcoded array of ~18 chat messages between literal teammate usernames.
- **Swap accept/reject is CRUD-only** — there's no real "propose → accept/reject" transition; "accepting" a swap creates a brand-new row with undefined ids (a dead button).
- **Two competing, disconnected image-upload systems** exist (DB blob vs. disk file) because two team members built the same feature independently and never reconciled it — the textbook symptom of "independently developed parts never integrated."
- **No authorization checks** on any mutating endpoint for swipes/swaps/messages — any authenticated user can write/read/delete another user's records by supplying arbitrary IDs in the request body.
- **CI never ran a single test** — deliberately stripped out (`-DskipTests`), documented as intentional in the README.
- **Database is PostgreSQL 14** (prod), H2 disk-based (dev) — clean, standard, JHipster-generated schema via Liquibase, though later edited in a way that breaks Liquibase's checksum model.

Treat this repo as a **requirements document and a bug-avoidance list**, not a codebase to extend.

---

## 1. What ThreadTrades Is

A Tinder-style marketplace for clothes: users upload photos of clothing items, swipe left/right on other users' items, a mutual right-swipe is meant to create a "match," matched users message each other and arrange a physical swap, and the swap is tracked to completion.

Built as a university group project ("Team Project 2024", University of Birmingham, module contact Madasar Shah). Team of 6: Izaac Roberts, Ethan Katz, Zhaochen Zhang, Yujin Lee, Junchen Wu, Libin Zheng. Roughly one feature per person, developed in parallel with minimal shared code review, integrated in a crunch during the last week before the deadline (18–24 Apr 2024).

---

## 2. Old Tech Stack

| Layer | Tech | Version |
|---|---|---|
| Backend framework | Spring Boot | 2.7.3 |
| Backend language | Java | 11 |
| Scaffolding generator | JHipster | 7.9.4 |
| ORM | Hibernate / Spring Data JPA | (via Spring Boot 2.7.3) |
| Auth | Spring Security, stateless JWT | — |
| DB migrations | Liquibase | — |
| Cache | Ehcache (Hibernate 2nd-level cache) | — |
| Frontend framework | Angular | 14.2.0 |
| Frontend language | TypeScript | 4.8.2 |
| State/reactivity | RxJS | 7.5.6 |
| UI theme | Bootstrap 5.2.0 + Bootswatch "Flatly" | — |
| Build | Webpack 5.74.0 (via Angular CLI 14.2.1) | — |
| Test runner (frontend) | Jest 28.1.3 | never run in CI |
| Test runner (backend) | JUnit (via `mvnw verify`) | never run in CI |
| CI | GitLab CI (`.gitlab-ci.yml`), staff-maintained deploy pipeline | — |
| Deployment | Docker (Jib), ARM64 VM, Caddy reverse proxy, Let's Encrypt **staging** certs | — |

All of this is now 1-2 major versions behind current (Spring Boot 2.7 is EOL; Angular 14 is many majors behind current Angular; Java 11 vs. current LTS 21).

---

## 3. Database

**Confirmed: PostgreSQL 14.5** in production/dev-docker (`src/main/docker/postgresql.yml`, `app.yml`), **H2 (disk-based)** for local dev without Docker. This was a clean, deliberate JHipster prompt choice, not a default — the JDBC URL, Liquibase URL, and docker-compose all target Postgres consistently. No other datastore (no Redis, no Mongo, no search engine) is in use.

Schema is Liquibase-managed under `src/main/resources/config/liquibase/`. All 7 entity changelogs (`UserProfile`, `ClothingItem`, `Message`, `SidebarContent`, `Reviews`, `UserSwipe`, `Swap`) were generated in a single JDL-apply session on 2024-03-05, following JHipster's standard naming convention. **One entity — `ClothingUpload` — has no Liquibase changelog at all** and only exists in the DB because `hibernate.ddl-auto: update` is enabled (a genuine schema-drift risk: the table's structure is whatever Hibernate infers at boot, not something version-controlled).

---

## 4. Full Feature List & Status

| # | Feature | Status | Reachable from nav? |
|---|---|---|---|
| 1 | Registration / login / activation | **WORKING** (stock JHipster, but activation gate silently disabled — see §12) | Yes |
| 2 | Home dashboard (your items, quick links) | **PARTIALLY WORKING** (real data, several crash bugs) | Yes (default route) |
| 3 | Clothing upload (BLOB path via generated entity form) | **WORKING** | Yes (via edit-profile/entities) |
| 4 | Clothing upload (custom disk-file path, `/clothing-upload` page) | **PARTIALLY WORKING / ORPHANED FEATURE** (uploads succeed but item is disconnected from the rest of the app; broken image preview) | Yes (navbar) |
| 5 | Swipe deck | **BROKEN** (deck logic mostly works; match detection is dead code; dislike records are corrupted) | Yes (navbar) |
| 6 | Match detection (mutual right-swipe → Swap) | **BROKEN** (async bug means it never fires as designed) | — |
| 7 | Matches list | **PARTIALLY WORKING / HACKED** (shows all clothing items system-wide minus one hardcoded user id, not real matches) | Yes (navbar) |
| 8 | Complete Swap ("Accept") button | **BROKEN** (posts undefined ids; UI falsely reports success) | Via Matches page |
| 9 | Swap reject | **NOT IMPLEMENTED** (no UI path at all; only via generic admin CRUD) | No |
| 10 | Swap history | **PARTIALLY WORKING** (real data) but **ORPHANED** (no nav link) | No |
| 11 | Item detail view | **WORKING in isolation** but **ORPHANED** (unreachable except via the orphaned swap history page) | No |
| 12 | Messaging / chat | **STUBBED** (100% hardcoded fake data; real backend built and unused) | No (nav link commented out) |
| 13 | User profile view | **WORKING** | Yes (navbar) |
| 14 | Edit profile | **WORKING** (properly built Angular reactive form) | Via profile page only |
| 15 | Dark mode toggle | **PARTIALLY WORKING** (functional but not persisted, inconsistent coverage across pages, toggle button hidden on one page only) | Via profile page only |
| 17 | Reviews / ratings | **SCAFFOLDED ONLY** — entity + generated CRUD exist, zero product UI, not in the original JDL | Admin only |
| 18 | Privacy / GDPR page | **WORKING** (static) but leaks real personal emails/phone number in markup | Yes (navbar) |
| 19 | Admin entity management (generic JHipster CRUD for all 7 entities) | **WORKING**, admin-only | Admin only |
| 20 | User/role admin, metrics, health, logs | **WORKING** (stock JHipster admin console) | Admin only |

---

## 5. Domain Model (carry the *concepts* forward, not the implementation)

The actual compiled Java entities diverge from `mvp.jdl` (which is stale/incomplete — it's missing `ClothingUpload` and `Reviews` entirely). This is the real, as-built model:

- **UserProfile** — `password`(unused duplicate field), `name`, `username`, `profilePicture` (BLOB), `email`, `bio`. 1:1 with JHipster's built-in `User` (auth identity). 1:1 with `SidebarContent`. 1:N to `Message`, `ClothingItem`, `Swap`, `UserSwipe`.
- **ClothingItem** — `name`, `image` (BLOB), `brand`, `itemType` (freeform string, no enum), `description`, `clothingSize` (Integer, no enum), `colour` (Integer code, no lookup table), `uploadDate`/`uploadTime` (duplicate-purpose fields), `condition` (enum), `gender` (enum), `lastAccessed`. Owned by `UserProfile` via a real FK (`userProfile`) **and** a redundant unsynchronized scalar `userID` field — two ways of expressing ownership, never kept in sync.
- **Message** — `messageID` (app-level ID separate from the DB PK — redundant), `senderID`, `recipientID`, `matchID` (all raw `Long`s, none are real FKs), `content`, `sentTime`, `readStatus` (enum: Sent/Delivered/Read), `picture` (BLOB). **No `Match` entity exists anywhere** — `matchID` is a dangling concept.
- **SidebarContent** — `bio` (duplicate of UserProfile.bio), `swaps`/`matches` (Integer counters). **Fully dead**: nothing increments these counters anywhere in the codebase.
- **UserSwipe** — `swiperClothingItemId`, `swiperUserId`, `swipedClothingItemId`, `swipedUserId` (raw Longs, no FKs), `decision` (raw String, not an enum — `'like'`/`'dislike'` is a frontend convention with zero DB-level enforcement).
- **Swap** — `user1Id`, `user2Id`, `clothingItemId1`, `clothingItemId2` (raw Longs, no FKs), `status` (enum: Pending/Accepted/Rejected).
- **ClothingUpload** — a completely separate, hand-written (non-JHipster-template) entity: `brand`, `type`, `size`, `color`, `condition` (all plain Strings, not reusing the `Condition`/`Gender` enums), `description`, `imageUrl` (filename on disk). **No FK to `UserProfile` or `ClothingItem` at all.**
- **Reviews** — `review` (String), `stars` (Float), FK to `UserProfile`. Scaffolded, never surfaced in the product.

**Lesson for the rewrite:** design the schema with real foreign keys from day one (a `Match` entity that a `Swap` references; a `swiper`/`swiped` FK pair with a DB-level unique constraint to prevent duplicate swipes; enums enforced at the DB layer, not just convention). Don't let "userId as a raw Long copied around by hand" happen again — it was the direct cause of several bugs below.

---

## 6. Core Business Logic Worth Preserving (Concepts, Not Code)

These are the pieces of *real, working* logic the team wrote — worth reimplementing correctly rather than rediscovering from scratch:

1. **"Get me a swipeable item that isn't mine"** — `ClothingItemRepository.findAllByUserProfileNot(userProfile)` + picking a random index. Simple and reasonable, but as-shipped it **crashes with `IllegalArgumentException` when there are zero other items** (`ThreadLocalRandom.nextInt(0, 0)`) — the rewrite must guard the empty-list case.
2. **"Most recently active item first" sort** — `ClothingItem.lastAccessed`, bumped via a dedicated `PUT /clothing-items/{id}/update-last-accessed` endpoint, used to order the swipe deck. A reasonable recency-ranking primitive worth keeping.
3. **Conversation list grouped by match** — `MessageRepository.findMostRecentMessagesForUser`: `SELECT m FROM Message m WHERE m.senderID = ?1 OR m.recipientID = ?1 ORDER BY m.matchID, m.sentTime DESC`. Never wired to any UI, but it's the right shape of query for a real inbox screen — group by conversation, most recent first.
4. **UUID-renamed file uploads on disk** — `ClothingUploadService.saveAll()` writes multipart files to disk under a randomized UUID filename rather than the original filename (avoids collisions/path traversal via filename). Worth keeping this *pattern* even though the surrounding feature is dead — and note this data is currently **write-only**: nothing serves `C:\team56\upload` back out over HTTP, so uploaded files can never actually be displayed.
5. **JWT stateless auth via Spring Security** — standard, solid, no reason to redo from scratch conceptually; just needs modernizing (see §13) and the ownership-check gap fixed (see §12).
6. **Swipe deck retry loop ("find an item I haven't swiped yet")** — capped at 10 attempts before giving up. The cap is a real scalability landmine (a user who's swiped ≥10 items in a small pool sees "no more items" even if unswiped items exist elsewhere), but the *intent* — bounded retry rather than unbounded loop — is reasonable and worth keeping as a pattern, just needs a real query (`WHERE NOT IN (already swiped ids)`) instead of client-side retry-and-hope.

---

## 7. Hard-Won Bugs & Gotchas (from git history — don't rediscover these)

1. **JHipster Blob fields need explicit `data:` URI construction to render.** `<img [src]="item.image">` does **not** work for a JHipster `ImageBlob` field — you need `[src]="'data:' + item.imageContentType + ';base64,' + item.image"`. This bug was independently hit and fixed **three separate times** in this repo (UserProfile, swipe page, and again on swipe page after a regression) because there was no shared pipe/directive for it. **In the rewrite: build one image-rendering helper/component and use it everywhere, once.**
2. **RxJS `subscribe()` callback returns don't return from the outer function.** The single most damaging bug in the app: `checkUserSwipe()`/`checkIfAlreadySwiped()` in the swipe component do `this.service.get(...).subscribe(x => { return true }); return false;` — the inner `return` only exits the callback; the outer function has already synchronously returned `false`. This silently killed match detection and duplicate-swipe prevention. **Any async boundary in the rewrite must use awaited/promise-based or explicit-callback patterns that make this class of bug structurally impossible** (e.g. `async/await`, or a state-management pattern that doesn't invite "return from inside a subscribe").
3. **JPQL is fragile to typos with no compile-time check.** `@Query("SELECt m FROM Message m WHERE ...")` (mis-cased `SELECt`) shipped and presumably failed at runtime, not compile time. Favor a typed query builder (JPA Criteria API, QueryDSL, jOOQ, or Spring Data derived query methods) over hand-written `@Query` strings where feasible.
4. **`~` is not expanded inside quotes in POSIX shell `test`.** `install-app.sh` had `if [ -e "~/prd.current.yml" ]` which silently always evaluates false — fixed to `"${HOME}/prd.current.yml"`. Audit any reused deploy scripts for the same mistake.
5. **Concurrent CI deploy jobs can race on one VM.** `deploy-dev`/`deploy-prod` GitLab CI jobs originally had no `resource_group`, so two deploys could run concurrently against the same machine. Fixed with `resource_group: deployment` to serialize them. Keep this pattern in any new CI/CD config.
6. **Editing an already-applied Liquibase changeset in place breaks its checksum.** The `ClothingItem`/`Message`/`UserSwipe` changelogs were hand-edited after their initial creation (e.g. adding a `user_id` column into the *same* changeset id) instead of adding a new incremental changelog. This either silently no-ops or throws a checksum-mismatch error depending on whether the DB already ran the original version. **Rule for the rewrite: migrations are append-only, never edit a migration that may have already run anywhere.**
7. **`ThreadLocalRandom.nextInt(0, 0)` throws.** Any "pick a random item from a possibly-empty list" code needs an explicit empty-check before calling into a random-index generator.
8. **Multipart form-data boundary headers should not be set manually in Angular.** `clothing-upload.service.ts` sets a literal `'Content-Type': 'multipart/form-data;boundary=----WebKitFormBoundary'` header by hand — this fights the browser's real, randomly-generated boundary and can corrupt uploads. Let the browser/HttpClient set this header itself when sending `FormData`.

---

## 8. Architecture Decisions & Reasoning (from commits, README, comments)

- **Monolith over microservices** — JHipster "monolith" application type chosen at generation time; reasonable for a 6-person student team on a semester timeline. No reason to second-guess for a solo rewrite either, unless there's a specific driver for splitting services.
- **JWT (stateless) over session auth** — chosen explicitly at JHipster generation prompts (see README's recorded prompt answers). Standard, fine choice, worth keeping the *stateless* property specifically since it simplifies horizontal scaling and removes server-side session storage.
- **PostgreSQL prod / H2 dev** — deliberate choice recorded in README's generation transcript, not a default fallen into by accident.
- **Ehcache + Hibernate 2nd-level cache** — enabled at generation time; there's no evidence any custom caching logic was added or tuned beyond the JHipster default — likely unnecessary complexity for the app's actual (probably light) load, worth reconsidering in the rewrite rather than cargo-culting.
- **ARM64 Docker builds** (`pom.xml` `jib-maven-plugin.architecture=arm64`) — the app was deployed to an ARM64 university VM; this was a deliberate, documented change from JHipster's amd64 default (see README).
- **Caddy as reverse proxy / TLS terminator**, with Let's Encrypt **staging** (not production) ACME endpoint even on what's called "dev" deployment — meaning the publicly deployed "dev" instance served untrusted certs. This looks like it was never promoted to a real production cert, i.e. the deployed app was permanently in a semi-finished state.
- **CI test stages deliberately removed** — README states outright: *".gitlab-ci.yml modified to remove unused test sections."* This wasn't an oversight; it was a conscious (if regrettable) decision to prioritize shipping over test coverage under time pressure. `maven-package` explicitly runs `-DskipTests`.
- **JDL used as the source-of-truth generator input, but not kept in sync afterward** — `mvp.jdl` reflects the *original* 7-entity plan; two entities (`ClothingUpload`, `Reviews`) and various field-level changes were added directly to the generated Java/Liquibase without ever being reflected back into the JDL. This means the JDL cannot be trusted as documentation of the final state — a process lesson, not just a code lesson.

---

## 9. Known Messes — Anti-Patterns to Avoid Repeating

**Process / integration:**
- Six independently-built features stitched together in a last-week crunch, with a revert-war on one feature (`clothing-upload` reverted then immediately un-reverted) and a wholesale revert of another entire feature branch (`match-history`) rather than resolving merge conflicts properly.
- Most integration was direct-to-`main` push-and-pray; no evidence of a PR/code-review culture (only one GitLab merge-request reference found in the whole history).
- Two team members independently built two entirely separate "upload a clothing item" systems (DB blob via generated form, and a hand-rolled disk-file upload) that were never reconciled — neither knows the other exists.
- Near-duplicate hardcoded chat/messaging code copy-pasted verbatim between `matches.component.ts` and `messages.component.ts` instead of being shared.
- Generic, uninformative commit messages ("Bug fix" ×7 with no further detail) that required manually inspecting diffs to recover intent — hurts anyone doing archaeology later. One "Bug fix on login page" commit doesn't touch the login page at all.
- Stray backup/scratch files committed straight into `src/` and never cleaned up: `matches.component3.text`, `swap.component.html.text`, `swap.component.ts2.text`, `swap.component.spec.ts2.text` — renamed copies made mid-merge, then abandoned.
- A stray local dev-DB config file (`.h2.server.properties`) got swept into a merge commit — a sign of an unclean local environment being committed by accident.

**Backend code smells:**
- Raw `Long` "foreign keys" everywhere (`user1Id`, `swiperUserId`, `senderID`, etc.) instead of real JPA relationships — no referential integrity, easy to transpose/typo, and the direct cause of the dislike-record corruption bug.
- Large blocks of commented-out alternate implementations left in permanently (`ClothingItemService`, `ClothingUploadResource`, `matches.component.ts`'s three versions of search) instead of being deleted (git history already preserves them).
- Debug leftovers shipped to what's nominally production: `System.out.println`, `log.debug("@@@@"+...)`, `log.warn("****"+...)`.
- Methods that throw raw `new Error(...)` instead of proper Spring exceptions, bypassing the app's structured error handling and surfacing as unstructured 500s.
- A `@RequestParam` attempting to bind a full JPA entity (`Message`) straight from a GET query string — fragile/likely non-functional as written.
- An accidental `@Column(unique = true)` on `ClothingItem.userID` — a copy-paste artifact from the JDL that (if actually enforced) would prevent any user from owning more than one clothing item.
- Duplicate imports, unused injected repositories (`UserProfileRepository` injected into `UserService` but never called once) — signs of incomplete refactors.

**Frontend code smells:**
- Raw DOM manipulation (`document.getElementById(...)`) mixed into an Angular app that otherwise uses reactive forms elsewhere — inconsistent patterns.
- Empty no-op event handlers left wired to UI controls that look functional (file preview "works" in the sense that nothing crashes, but never actually shows a preview).
- Inline `onclick="alert(...)"` mixed with Angular `(click)` bindings, and the alert firing regardless of whether the underlying API call actually succeeded — UI lying to the user about success.
- Hardcoded numeric user IDs used as stand-ins for "the current user" (`!== 7451`) that were meant to be temporary and never removed.
- Invalid nested `<html>/<head>/<body>` markup and CDN `<script>`/`<link>` tags pasted directly into Angular component templates (static-mockup leftovers that do nothing but bloat the DOM).
- A theme/dark-mode service with no persistence (resets every page load) and inconsistent application across pages (some pages never get the dark-mode class at all).
- Real personal contact information (student emails, a phone number) hardcoded into the shipped privacy/GDPR page.

**Security (see also §12):**
- The JHipster-generated JWT secret is committed directly in `.yo-rc.json`, and (identically) is the default in both `application-dev.yml` and `application-prod.yml` — trivially crackable if this key material is ever reused for a real deployment.

---

## 10. Security Issues to Fix (do not carry forward)

1. **No server-side ownership checks on any mutating swipe/swap/message/clothing-item endpoint.** `POST/PUT/PATCH/DELETE` handlers for `UserSwipe`, `Swap`, `Message`, `ClothingItem` accept whatever owner/user ID the client puts in the JSON body and persist it unchecked — any authenticated user can create, read, modify, or delete another user's records by ID. The correct pattern (already used correctly in the handful of `/current-user/...` GET endpoints via `SecurityUtils`/`getUserWithAuthorities()`) must be applied to **every** mutating endpoint in the rewrite: derive the actor's identity server-side from the authenticated principal, never trust a client-supplied owner/user ID.
2. **Account activation is silently disabled.** `UserService.registerUser()` sets `newUser.setActivated(true)` immediately (with a stale comment directly above it still claiming otherwise) — the email-activation gate exists in the code but does nothing. Decide deliberately in the rewrite whether email verification is wanted, rather than inheriting this by accident.
3. **User registration never creates a `UserProfile`.** Several `/current-user/*` endpoints hard-fault (unstructured 500 via raw `Error`) for a freshly-registered user with no `UserProfile` row, because nothing in the registration flow creates one. Registration and profile-creation must be one atomic step in the rewrite.
4. **Committed secret material.** The JWT signing secret is committed in plaintext in version control (`.yo-rc.json`) and duplicated as the default in both dev and prod config. Secrets must never be committed — use environment variables / a secrets manager from day one.
5. **Uploaded files may be write-only / unservable.** The disk-based `ClothingUpload` path writes files to a local filesystem path with no discovered static resource mapping to serve them back — combined with the hardcoded absolute Windows path `C:\team56/upload` in `application.yml`, this is not portable and not deployable as-is.

---

## 11. Anything Else Notable

- **Language mix in comments**: some contributors wrote comments in Chinese (`matches.component.ts`, `clothing-item.service.ts`) — not a problem, just useful context if anyone goes looking for prior art in translated form.
- **Uneven team contribution** — two contributors (Izaac Roberts, Libin Zheng) account for the large majority of commits; one contributor's commits are almost entirely merge-conflict/revert cleanup rather than feature work. Not directly actionable for a solo rewrite, but useful context for judging which parts of the app got the most attention (home, userProfile, matches/match-history) vs. the least (messaging, swap transitions).
- **No WebSocket/real-time infrastructure exists anywhere** in the old codebase, despite messaging being a core intended feature — a real-time layer (WebSocket/SSE) needs to be built from scratch in the rewrite, not migrated.
- **Reviews/ratings entity was scaffolded via JDL and immediately abandoned** — never appeared in any product screen. If a review/rating feature is still wanted, it should be treated as a fresh feature, not a resurrection.

---

## 12. Recommended New Tech Stack

Constraint: **Java must be used somewhere in the backend.** Given this is now a solo rewrite (not a 6-person team racing a deadline), the recommendation favors a leaner, more deliberately-designed stack over JHipster's heavyweight all-in-one scaffolding — JHipster optimizes for "generate a huge amount of boilerplate fast for a team," which isn't the right trade-off for one person doing a careful rewrite.

| Layer | Recommendation | Why |
|---|---|---|
| Backend language/runtime | **Java 21 (LTS)**, plain Spring Boot (no JHipster) | Meets the Java constraint; Java 21 brings records, pattern matching, virtual threads (great fit for a REST API with many small I/O-bound calls). Skipping JHipster avoids inheriting another pile of unused generated scaffolding (Ehcache, admin console, i18n, etc. that were never actually used last time). |
| Backend framework | **Spring Boot 3.x** + Spring Web (or Spring WebFlux if you want reactive) | Current, well-supported, same mental model as before but modern and non-EOL. |
| Auth | **Spring Security + JWT**, hand-configured (not generated) | Keep the concept that worked; implement ownership checks correctly this time from the first endpoint, and never commit the signing secret. |
| ORM / data access | **Spring Data JPA + Hibernate**, with a real relational schema (proper FKs, DB-level unique constraints, enums enforced at the DB) | Same tool, used correctly this time — no more raw "Long that means a foreign key" fields. |
| Migrations | **Flyway** instead of Liquibase | Flyway's plain, versioned, append-only SQL files make the "never edit an already-applied migration" rule (§7.6) much harder to violate by accident than Liquibase's mutable XML changesets. |
| Database | **PostgreSQL** (keep) | It worked, it's mature, no reason to change. Consider Postgres 16+ for the rewrite. |
| Image/file storage | **Object storage (S3-compatible — AWS S3, Cloudflare R2, or self-hosted MinIO)**, URLs stored in Postgres, not BLOBs | Fixes both the DB-blob approach (bloats the DB, slow to query/paginate) and the disk-upload approach (not servable, not portable) in one move — this was the single biggest structural mess in the old app (two disconnected image systems). |
| Real-time messaging | **Spring WebSocket (STOMP) or Server-Sent Events** | The old app never built any real-time layer despite messaging being core — build it properly this time instead of faking it with hardcoded data. |
| Match detection | A real backend algorithm: unique constraint on `(swiper_user_id, swiped_item_id)`, and a single transactional check-and-create ("does the reverse swipe exist? if so, atomically create one `Match` row") rather than client-side async logic — this removes the entire class of bug that broke matching last time. |
| Frontend framework | **Next.js (React) + TypeScript**, or modern **Angular (v18+, standalone components/signals)** if you'd rather stay in the Angular ecosystem you already know | Either is a legitimate choice; React/Next.js has a larger ecosystem of swipe-card libraries and is generally faster to iterate on solo. If Angular familiarity is valuable, upgrading in place (14 → 18+) is also reasonable — just don't regenerate via JHipster again. |
| Styling | **Tailwind CSS** (if going React) or continue with a modern Bootstrap/SCSS setup (if staying Angular) | Faster iteration for a solo developer than hand-written SCSS per component. |
| API contract | **OpenAPI-generated TypeScript client** from the Spring Boot backend | Removes an entire category of "frontend calls an endpoint that doesn't exist / has the wrong shape" bugs seen repeatedly in the old code (e.g. the broken `/user-profile/from-message` and `/users/loggedInUser` calls). |
| Testing | Real backend tests (JUnit 5 + Testcontainers against real Postgres) and frontend tests (Vitest/Jest), **actually run in CI** | The old project's CI explicitly skipped all tests — make "tests run on every push" non-negotiable this time, even solo. |
| CI/CD | GitHub Actions (or GitLab CI if preferred) running build + test + lint on every push; deploy via Docker to a modern host (Fly.io, Railway, Render, or a VPS) with **real** (non-staging) TLS certs | Simple, well-documented, avoids the ARM64/chromium CI quirks that plagued the old pipeline. |
| Deployment | Docker Compose for local dev (Postgres + app), single Docker image for the app in prod | Same shape as before, just without the ARM64-specific and Caddy-staging-cert issues. |

**Optional stretch, not a day-one requirement:** if a native mobile feel matters for a swipe-heavy UI, a PWA (installable, offline-capable) built on the Next.js/Angular web frontend gets most of the benefit without committing to a separate React Native/Flutter codebase — revisit only if the web PWA proves insufficient.

---

## 13. Rewrite Checklist (derived from this audit)

- [ ] Design a fresh schema with real FKs and DB-level constraints (esp. unique `(swiper, swiped_item)` on swipes, a real `Match` entity, enums enforced at the DB).
- [ ] Implement match detection as a single atomic, server-side, transactional operation — no client-side async match logic.
- [ ] Implement one image-upload path (object storage), used consistently by clothing items, profile pictures, and message attachments alike.
- [ ] Build real-time messaging (WebSocket/SSE) backed by the actual `Message` table — no hardcoded chat data.
- [ ] Implement swap lifecycle as real state transitions (`PATCH /swaps/{id}/accept`, `/reject`) on one row, not by creating parallel rows.
- [ ] Enforce ownership on every mutating endpoint — derive actor identity from the authenticated JWT, never trust client-supplied user IDs.
- [ ] Decide deliberately on email activation (implement fully or explicitly skip — don't leave a half-wired gate).
- [ ] Create a `UserProfile` atomically as part of registration.
- [ ] Never commit secrets (JWT signing key, DB credentials) — use environment variables from the first commit.
- [ ] Wire CI to actually run backend and frontend tests on every push.
- [ ] Use append-only migrations (Flyway) — never edit an already-applied migration file.
- [ ] Build one shared image-rendering component/helper instead of re-solving blob/URL rendering per page.
- [ ] Skip the two known-abandoned/never-shipped ideas (Reviews/ratings) unless deliberately re-scoped as new features.
