# ThreadTrades — Claude instructions

## Log implementation gotchas to the roadmap

Whenever you hit a non-obvious gotcha, trade-off, or operational detail while
implementing something — the kind of thing that isn't derivable just by reading the
code, and would bite someone (including future-you) if it went undocumented — add a
bullet for it to **`ROADMAP.md` § 3, "Notes from implementation"**, matching the
style of the existing bullets there (bold lead-in phrase, then the detail, and a
note on when/how to revisit it if applicable).

Do this proactively, without being asked each time. Examples of what belongs there:
infrastructure quirks (Docker/volume/env-var gotchas), version-specific framework
behavior that diverges from the obvious expectation, test-only workarounds, anything
that cost real time to figure out. Routine implementation choices that are already
obvious from the code or covered elsewhere (e.g. AUDIT.md, this file) don't need an
entry — only the surprising, hard-won stuff.
