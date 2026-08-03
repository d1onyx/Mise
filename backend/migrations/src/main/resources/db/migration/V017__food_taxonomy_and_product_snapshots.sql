CREATE TABLE IF NOT EXISTS food_concepts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name CITEXT NOT NULL UNIQUE,
    parent_id UUID REFERENCES food_concepts(id) ON DELETE SET NULL,
    origin TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK (
        origin IN ('ANIMAL', 'PLANT', 'FUNGI', 'ALGAE', 'MICROBIAL', 'MINERAL', 'SYNTHETIC', 'MIXED', 'UNKNOWN')
    ),
    taxonomy_version INTEGER NOT NULL DEFAULT 1 CHECK (taxonomy_version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS food_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES food_concepts(id) ON DELETE CASCADE,
    language TEXT NOT NULL DEFAULT 'und',
    alias TEXT NOT NULL,
    normalized_alias TEXT NOT NULL,
    source TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0 CHECK (confidence BETWEEN 0.0 AND 1.0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (concept_id, language, normalized_alias)
);

CREATE TABLE IF NOT EXISTS food_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES food_concepts(id) ON DELETE CASCADE,
    canonical_name CITEXT NOT NULL,
    origin TEXT NOT NULL DEFAULT 'UNKNOWN',
    preparation_state TEXT NOT NULL DEFAULT 'AS_SOLD',
    physical_form TEXT NOT NULL DEFAULT 'UNKNOWN',
    carbonation TEXT NOT NULL DEFAULT 'NOT_APPLICABLE',
    preservation TEXT NOT NULL DEFAULT 'UNKNOWN',
    facets JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_food_variants_identity
    ON food_variants (
        concept_id,
        lower(canonical_name::text),
        origin,
        preparation_state,
        physical_form,
        carbonation,
        preservation,
        md5(facets::text)
    );

CREATE TABLE IF NOT EXISTS retail_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barcode TEXT NOT NULL UNIQUE CHECK (barcode ~ '^[0-9]{4,24}$'),
    concept_id UUID REFERENCES food_concepts(id) ON DELETE SET NULL,
    variant_id UUID REFERENCES food_variants(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    brand TEXT NOT NULL DEFAULT '',
    generic_name TEXT NOT NULL DEFAULT '',
    language TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT '',
    source_provider TEXT,
    source_revision BIGINT,
    product_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS product_source_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retail_product_id UUID NOT NULL REFERENCES retail_products(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,
    schema_version INTEGER,
    source_revision BIGINT NOT NULL DEFAULT 0,
    source_updated_at TIMESTAMPTZ,
    client_provided BOOLEAN NOT NULL DEFAULT false,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (retail_product_id, provider, source_revision)
);

CREATE INDEX IF NOT EXISTS idx_food_aliases_lookup
    ON food_aliases (language, normalized_alias);
CREATE INDEX IF NOT EXISTS idx_food_variants_concept
    ON food_variants (concept_id);
CREATE INDEX IF NOT EXISTS idx_retail_products_concept
    ON retail_products (concept_id);
CREATE INDEX IF NOT EXISTS idx_retail_products_variant
    ON retail_products (variant_id);
CREATE INDEX IF NOT EXISTS idx_product_source_snapshots_latest
    ON product_source_snapshots (retail_product_id, received_at DESC);
