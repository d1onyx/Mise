CREATE TABLE health_nutrition_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    meal_type TEXT NOT NULL CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    title TEXT NOT NULL CHECK (char_length(title) BETWEEN 2 AND 120),
    calories NUMERIC(12, 3) NOT NULL CHECK (calories >= 0),
    protein_grams NUMERIC(12, 3) NOT NULL DEFAULT 0 CHECK (protein_grams >= 0),
    carbs_grams NUMERIC(12, 3) NOT NULL DEFAULT 0 CHECK (carbs_grams >= 0),
    fat_grams NUMERIC(12, 3) NOT NULL DEFAULT 0 CHECK (fat_grams >= 0),
    incomplete BOOLEAN NOT NULL DEFAULT FALSE,
    warning TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_health_nutrition_logs_user_date ON health_nutrition_logs(user_id, log_date DESC) WHERE deleted = FALSE;
CREATE INDEX idx_health_nutrition_logs_incomplete ON health_nutrition_logs(user_id, incomplete) WHERE deleted = FALSE;

CREATE TABLE water_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount_ml INTEGER NOT NULL CHECK (amount_ml BETWEEN 1 AND 5000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_water_logs_user_date ON water_logs(user_id, log_date DESC);

CREATE TABLE weight_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg NUMERIC(6, 2) NOT NULL CHECK (weight_kg BETWEEN 20 AND 400),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_weight_logs_user_date ON weight_logs(user_id, log_date DESC);

CREATE TABLE health_insights (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insight_type TEXT NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    message TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_health_insights_user_active ON health_insights(user_id, created_at DESC) WHERE dismissed = FALSE;
