-- t-95: Cooking Mode (mobile) is removed in t-90, so the per-step timer that
-- only fed the interactive cooking session UI has no remaining consumer.

ALTER TABLE recipe_steps DROP COLUMN IF EXISTS timer_seconds;
