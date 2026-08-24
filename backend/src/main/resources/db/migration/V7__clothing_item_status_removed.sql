-- Admin item removal is a status flag, not a hard delete: clothing_item is
-- referenced by item_match (ON DELETE CASCADE), so hard-deleting a matched/
-- swapped item would silently cascade-delete match/swap/message history.
ALTER TABLE clothing_item DROP CONSTRAINT clothing_item_status_check;
ALTER TABLE clothing_item
    ADD CONSTRAINT clothing_item_status_check CHECK (status IN ('AVAILABLE', 'SWAPPED', 'REMOVED'));
