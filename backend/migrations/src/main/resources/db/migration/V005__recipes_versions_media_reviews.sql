CREATE TABLE IF NOT EXISTS recipes (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    forked_from_recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    status TEXT NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    visibility TEXT NOT NULL CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    current_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS recipe_versions (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    changelog TEXT NOT NULL,
    title TEXT NOT NULL CHECK (char_length(title) BETWEEN 2 AND 120),
    description TEXT,
    servings INTEGER NOT NULL CHECK (servings BETWEEN 1 AND 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (recipe_id, version_number)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_recipes_current_version'
    ) THEN
        ALTER TABLE recipes
            ADD CONSTRAINT fk_recipes_current_version
            FOREIGN KEY (current_version_id) REFERENCES recipe_versions(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id UUID PRIMARY KEY,
    recipe_version_id UUID NOT NULL REFERENCES recipe_versions(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    amount NUMERIC(12, 4) NOT NULL CHECK (amount > 0),
    unit TEXT NOT NULL,
    note TEXT,
    position INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS recipe_steps (
    id UUID PRIMARY KEY,
    recipe_version_id UUID NOT NULL REFERENCES recipe_versions(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position > 0),
    text TEXT NOT NULL CHECK (char_length(trim(text)) > 0),
    timer_seconds INTEGER CHECK (timer_seconds IS NULL OR timer_seconds > 0),
    UNIQUE (recipe_version_id, position)
);

CREATE TABLE IF NOT EXISTS recipe_tags (
    recipe_version_id UUID NOT NULL REFERENCES recipe_versions(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    PRIMARY KEY (recipe_version_id, tag)
);

CREATE TABLE IF NOT EXISTS recipe_equipment (
    recipe_version_id UUID NOT NULL REFERENCES recipe_versions(id) ON DELETE CASCADE,
    equipment_key TEXT NOT NULL,
    PRIMARY KEY (recipe_version_id, equipment_key)
);

CREATE TABLE IF NOT EXISTS recipe_bookmarks (
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (recipe_id, user_id)
);

CREATE TABLE IF NOT EXISTS recipe_reviews (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (recipe_id, user_id)
);

CREATE TABLE IF NOT EXISTS recipe_media (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    media_type TEXT NOT NULL CHECK (media_type IN ('PHOTO', 'VIDEO')),
    object_key TEXT NOT NULL,
    public_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    content_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS recipe_reports (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    comment TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS recipe_moderation_events (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recipes_status_published_at ON recipes(status, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_recipes_owner_user_id ON recipes(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_recipe_versions_recipe_id ON recipe_versions(recipe_id, version_number DESC);
CREATE INDEX IF NOT EXISTS idx_recipe_tags_tag ON recipe_tags(tag);
CREATE INDEX IF NOT EXISTS idx_recipe_reviews_recipe_id ON recipe_reviews(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_reports_status ON recipe_reports(status);
