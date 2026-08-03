CREATE TABLE IF NOT EXISTS pantry_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE SET NULL,
    name TEXT NOT NULL CHECK (char_length(trim(name)) BETWEEN 2 AND 120),
    quantity NUMERIC(14, 4) NOT NULL CHECK (quantity >= 0),
    unit TEXT NOT NULL,
    location TEXT NOT NULL,
    category TEXT NOT NULL,
    expiry_date DATE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pantry_transactions (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES pantry_items(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('ADD', 'UPDATE', 'CONSUME', 'WASTE', 'FREEZE', 'DELETE')),
    quantity_delta NUMERIC(14, 4) NOT NULL,
    unit TEXT NOT NULL,
    reason TEXT,
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pantry_activity_outbox (
    id UUID PRIMARY KEY,
    item_id UUID REFERENCES pantry_items(id) ON DELETE SET NULL,
    transaction_id UUID REFERENCES pantry_transactions(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pantry_items_user_active ON pantry_items(user_id, deleted, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_pantry_items_user_location ON pantry_items(user_id, location) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pantry_items_user_category ON pantry_items(user_id, category) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pantry_items_expiry_date ON pantry_items(user_id, expiry_date) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pantry_items_search_name ON pantry_items USING gin (to_tsvector('simple', name));
CREATE INDEX IF NOT EXISTS idx_pantry_transactions_actor_created ON pantry_transactions(actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pantry_transactions_item_created ON pantry_transactions(item_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pantry_activity_outbox_unpublished ON pantry_activity_outbox(created_at) WHERE published_at IS NULL;
