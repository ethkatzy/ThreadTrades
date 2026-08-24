-- Soft-ban support for the admin moderation console: reversible (unban), and
-- doesn't cascade-delete content other users still reference (shared match
-- messages, reviews) the way a hard account delete would.
ALTER TABLE app_user
    ADD COLUMN banned BOOLEAN NOT NULL DEFAULT FALSE;
