-- Align existing game result values with the Kotlin GameResult enum names.
-- The original schema used 'not_played' (lowercase) as the column default,
-- and 'DRAW' was used as a fallback for unknown results. Both are replaced
-- by 'NOT_PLAYED', which is the canonical Kotlin enum constant name.
UPDATE game SET result = 'NOT_PLAYED' WHERE result = 'not_played';
UPDATE game SET result = 'NOT_PLAYED' WHERE result = 'DRAW';
ALTER TABLE game ALTER COLUMN result SET DEFAULT 'NOT_PLAYED';
