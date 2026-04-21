-- click-tt klass strings can be longer than 5 characters (e.g. "A1 NR", "B2 123")
ALTER TABLE player_season ALTER COLUMN klass TYPE VARCHAR(20);
