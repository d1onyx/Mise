CREATE UNIQUE INDEX IF NOT EXISTS uq_ingredients_normalized_name
    ON ingredients ((lower(trim(name))));

ALTER TABLE recipe_ingredients
    ADD COLUMN IF NOT EXISTS canonical_tags TEXT[] NOT NULL DEFAULT '{}';
