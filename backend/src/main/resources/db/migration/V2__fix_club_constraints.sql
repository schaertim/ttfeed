-- Ensure the old broken constraint is gone
ALTER TABLE club DROP CONSTRAINT IF EXISTS club_knob_id_key;

-- Add the permanent, correct constraint
ALTER TABLE club ADD CONSTRAINT club_name_unique UNIQUE (name);

ALTER TABLE club DROP COLUMN knob_id;