ALTER TABLE recipe_versions
    ADD COLUMN IF NOT EXISTS image_url TEXT;
