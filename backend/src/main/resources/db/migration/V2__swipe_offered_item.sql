-- A swipe on a target item is only meaningful in the context of which of the
-- swiper's own items they're offering for it -- liking item Z while offering
-- item A doesn't imply liking Z while offering item B. V1 didn't capture
-- that, so swipe/match/swap/message rows created under V1 don't carry an
-- offered item and can't be backfilled -- this is pre-launch dev data only,
-- so they're cleared rather than migrated.
TRUNCATE TABLE message, swap, item_match, swipe RESTART IDENTITY CASCADE;

ALTER TABLE swipe
    ADD COLUMN offered_clothing_item_id BIGINT NOT NULL REFERENCES clothing_item (id) ON DELETE CASCADE;

ALTER TABLE swipe DROP CONSTRAINT swipe_swiper_user_profile_id_swiped_clothing_item_id_key;

ALTER TABLE swipe ADD CONSTRAINT swipe_swiper_offered_swiped_key
    UNIQUE (swiper_user_profile_id, offered_clothing_item_id, swiped_clothing_item_id);

CREATE INDEX idx_swipe_offered_item ON swipe (offered_clothing_item_id);
