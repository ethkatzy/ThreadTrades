-- Extra items can now be proposed for either side, not just your own: a
-- participant can add their own item ("offer") or add the other participant's
-- item ("request"). `side` (added in V9) already tracks who *owns* the item;
-- this adds who *proposed* adding it, so the app can tell "you're offering
-- this" apart from "they're requesting this from you" even though both cases
-- put the item on the same side. Existing rows all predate the request
-- feature, so they were necessarily self-proposed -- backfill from `side`.
ALTER TABLE swap_extra_item
    ADD COLUMN proposed_by_side VARCHAR(1) CHECK (proposed_by_side IN ('A', 'B'));

UPDATE swap_extra_item SET proposed_by_side = side;

ALTER TABLE swap_extra_item ALTER COLUMN proposed_by_side SET NOT NULL;
