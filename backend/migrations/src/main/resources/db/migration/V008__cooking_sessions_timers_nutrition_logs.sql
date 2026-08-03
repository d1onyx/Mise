CREATE TABLE cooking_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE RESTRICT,
    recipe_version_id UUID NOT NULL REFERENCES recipe_versions(id) ON DELETE RESTRICT,
    recipe_title TEXT NOT NULL,
    servings INTEGER NOT NULL CHECK (servings BETWEEN 1 AND 100),
    current_step_position INTEGER NOT NULL DEFAULT 1 CHECK (current_step_position > 0),
    status TEXT NOT NULL CHECK (status IN ('IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cooking_sessions_user_started ON cooking_sessions(user_id, started_at DESC);
CREATE INDEX idx_cooking_sessions_user_status ON cooking_sessions(user_id, status);
CREATE INDEX idx_cooking_sessions_recipe ON cooking_sessions(recipe_id);

CREATE TABLE cooking_timers (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES cooking_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    label TEXT NOT NULL CHECK (char_length(label) BETWEEN 2 AND 120),
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds BETWEEN 1 AND 86400),
    remaining_seconds INTEGER NOT NULL CHECK (remaining_seconds BETWEEN 0 AND 86400),
    step_position INTEGER CHECK (step_position > 0),
    status TEXT NOT NULL CHECK (status IN ('RUNNING', 'PAUSED', 'DONE', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cooking_timers_session ON cooking_timers(session_id, created_at);
CREATE INDEX idx_cooking_timers_user_status ON cooking_timers(user_id, status);

CREATE TABLE nutrition_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    cooking_session_id UUID UNIQUE REFERENCES cooking_sessions(id) ON DELETE SET NULL,
    recipe_title TEXT NOT NULL,
    servings INTEGER NOT NULL CHECK (servings BETWEEN 1 AND 100),
    logged_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nutrition_logs_user_date ON nutrition_logs(user_id, log_date DESC);

CREATE TABLE cooking_activity_outbox (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type TEXT NOT NULL CHECK (entity_type IN ('cooking_session', 'timer', 'nutrition_log')),
    entity_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_cooking_outbox_pending ON cooking_activity_outbox(created_at) WHERE processed_at IS NULL;
