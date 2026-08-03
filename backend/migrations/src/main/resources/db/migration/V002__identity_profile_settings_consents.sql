CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid TEXT NOT NULL UNIQUE,
    email CITEXT NULL,
    display_name TEXT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    locale TEXT NOT NULL DEFAULT 'uk-UA',
    timezone TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revision BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT user_profiles_display_name_length CHECK (char_length(trim(display_name)) BETWEEN 2 AND 80)
);

CREATE TABLE IF NOT EXISTS allergens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_allergens (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    allergen_id UUID NOT NULL REFERENCES allergens(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, allergen_id)
);

CREATE TABLE IF NOT EXISTS equipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_equipment (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    equipment_id UUID NOT NULL REFERENCES equipment(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, equipment_id)
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    language TEXT NOT NULL DEFAULT 'uk-UA',
    units TEXT NOT NULL DEFAULT 'metric',
    notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS user_consents (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    granted BOOLEAN NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revision BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, type)
);

INSERT INTO allergens (code, name) VALUES
    ('gluten', 'Gluten'),
    ('peanut', 'Peanut'),
    ('milk', 'Milk'),
    ('egg', 'Egg'),
    ('soy', 'Soy')
ON CONFLICT (code) DO NOTHING;

INSERT INTO equipment (code, name) VALUES
    ('oven', 'Oven'),
    ('blender', 'Blender'),
    ('microwave', 'Microwave'),
    ('stove', 'Stove')
ON CONFLICT (code) DO NOTHING;
