-- V8: Drop the division table — groups sit directly under federation + season
--
-- The division layer added no value: it duplicated the group name and created
-- unnecessary joins. Groups now carry federation_id and season_id directly.

-- Drop FK and column linking groups to the division table
ALTER TABLE division_group DROP CONSTRAINT division_group_division_id_fkey;
ALTER TABLE division_group DROP COLUMN division_id;

-- Add federation and season directly on the group
ALTER TABLE division_group ADD COLUMN federation_id UUID NOT NULL REFERENCES federation(id);
ALTER TABLE division_group ADD COLUMN season_id     UUID NOT NULL REFERENCES season(id);

-- Drop the old knob unique index (was scoped to division_id) and recreate scoped to federation+season
DROP INDEX IF EXISTS idx_division_group_knob_unique;
CREATE UNIQUE INDEX idx_division_group_knob_unique
    ON division_group(federation_id, season_id, knob_gruppe)
    WHERE knob_gruppe IS NOT NULL;

-- Drop the old division_id index from V1
DROP INDEX IF EXISTS idx_division_group_division;

-- Add index for the new FK columns
CREATE INDEX idx_division_group_federation_season ON division_group(federation_id, season_id);

-- Drop the now-unused division table
DROP TABLE division;
