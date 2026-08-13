-- Mutual-consent swap accept/reject: each side of a match accepts independently,
-- and the swap only finalizes to ACCEPTED once both flags are set. Still one row
-- per match (status transitions on it, per V1's comment) -- just two more columns
-- to track who has accepted so far.
ALTER TABLE swap
    ADD COLUMN accepted_by_user_a BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN accepted_by_user_b BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: every match created before this feature existed has no swap row yet
-- (nothing ever inserted into `swap` before now). One PENDING row per match.
INSERT INTO swap (match_id)
SELECT id FROM item_match
WHERE NOT EXISTS (SELECT 1 FROM swap WHERE swap.match_id = item_match.id);

-- An item that's part of an ACCEPTED swap is no longer available to swipe on.
ALTER TABLE clothing_item
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'SWAPPED'));
