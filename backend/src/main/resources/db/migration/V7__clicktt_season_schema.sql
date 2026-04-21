-- V7: Add click-tt season scraping support
--
-- click-tt groups don't have a knob_gruppe, so the column must become nullable.
-- Each source gets its own ID column so lookups are always scoped correctly.

-- Make knob_gruppe nullable (required for click-tt groups which have no knob equivalent)
ALTER TABLE division_group ALTER COLUMN knob_gruppe DROP NOT NULL;

-- Replace the (division_id, knob_gruppe) unique constraint with a partial index so
-- the uniqueness still holds for knob rows but NULL knob_gruppe is allowed for click-tt rows.
ALTER TABLE division_group DROP CONSTRAINT division_group_division_id_knob_gruppe_key;
CREATE UNIQUE INDEX idx_division_group_knob_unique
    ON division_group(division_id, knob_gruppe)
    WHERE knob_gruppe IS NOT NULL;

-- click-tt group IDs (the `group=` URL param). Globally unique within click-tt.
ALTER TABLE division_group ADD COLUMN clicktt_id INTEGER;
CREATE UNIQUE INDEX idx_division_group_clicktt_unique
    ON division_group(clicktt_id)
    WHERE clicktt_id IS NOT NULL;

-- click-tt team table IDs (the `teamtable=` URL param). Globally unique within click-tt.
ALTER TABLE team ADD COLUMN clicktt_id INTEGER;
CREATE UNIQUE INDEX idx_team_clicktt_unique
    ON team(clicktt_id)
    WHERE clicktt_id IS NOT NULL;

-- click-tt match meeting IDs (the `meeting=` URL param). Globally unique within click-tt.
ALTER TABLE match ADD COLUMN clicktt_match_id INTEGER;
CREATE UNIQUE INDEX idx_match_clicktt_unique
    ON match(clicktt_match_id)
    WHERE clicktt_match_id IS NOT NULL;
