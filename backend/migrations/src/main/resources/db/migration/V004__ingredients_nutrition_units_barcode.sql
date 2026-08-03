CREATE TABLE IF NOT EXISTS ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL CHECK (char_length(trim(name)) BETWEEN 2 AND 120),
    default_unit TEXT NOT NULL,
    custom_created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    barcode TEXT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients USING gin (to_tsvector('simple', name));

CREATE TABLE IF NOT EXISTS ingredient_allergens (
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    allergen_code TEXT NOT NULL,
    PRIMARY KEY (ingredient_id, allergen_code)
);

CREATE TABLE IF NOT EXISTS ingredient_nutrition (
    ingredient_id UUID PRIMARY KEY REFERENCES ingredients(id) ON DELETE CASCADE,
    calories DOUBLE PRECISION,
    protein DOUBLE PRECISION,
    fat DOUBLE PRECISION,
    carbs DOUBLE PRECISION,
    nutrition_complete BOOLEAN GENERATED ALWAYS AS (
        calories IS NOT NULL AND protein IS NOT NULL AND fat IS NOT NULL AND carbs IS NOT NULL
    ) STORED
);

CREATE TABLE IF NOT EXISTS ingredient_density_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE CASCADE,
    unit TEXT NOT NULL DEFAULT 'ml',
    grams_per_ml DOUBLE PRECISION NOT NULL CHECK (grams_per_ml > 0),
    source TEXT NOT NULL DEFAULT 'manual'
);

CREATE TABLE IF NOT EXISTS ingredient_substitutes (
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    substitute_ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    PRIMARY KEY (ingredient_id, substitute_ingredient_id)
);

INSERT INTO allergens (code, name) VALUES
    ('milk', 'Milk'),
    ('gluten', 'Gluten'),
    ('peanut', 'Peanut'),
    ('egg', 'Egg'),
    ('soy', 'Soy')
ON CONFLICT (code) DO NOTHING;

INSERT INTO equipment (code, name) VALUES
    ('oven', 'Oven'),
    ('blender', 'Blender'),
    ('stove', 'Stove'),
    ('scale', 'Kitchen scale')
ON CONFLICT (code) DO NOTHING;

INSERT INTO ingredients (id, name, default_unit) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Tomato', 'g'),
    ('22222222-2222-2222-2222-222222222222', 'Milk', 'ml'),
    ('33333333-3333-3333-3333-333333333333', 'Oat milk', 'ml'),
    ('44444444-4444-4444-4444-444444444444', 'Wheat flour', 'g')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ingredient_nutrition (ingredient_id, calories, protein, fat, carbs) VALUES
    ('11111111-1111-1111-1111-111111111111', 18.0, 0.9, 0.2, 3.9),
    ('22222222-2222-2222-2222-222222222222', 42.0, 3.4, 1.0, 5.0),
    ('33333333-3333-3333-3333-333333333333', 46.0, 1.0, 1.5, 7.0),
    ('44444444-4444-4444-4444-444444444444', 364.0, 10.0, 1.0, 76.0)
ON CONFLICT (ingredient_id) DO NOTHING;

INSERT INTO ingredient_allergens (ingredient_id, allergen_code) VALUES
    ('22222222-2222-2222-2222-222222222222', 'milk'),
    ('44444444-4444-4444-4444-444444444444', 'gluten')
ON CONFLICT (ingredient_id, allergen_code) DO NOTHING;

INSERT INTO ingredient_substitutes (ingredient_id, substitute_ingredient_id, reason) VALUES
    ('22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'dairy_free')
ON CONFLICT (ingredient_id, substitute_ingredient_id) DO NOTHING;
