-- t-87: pantry inventory, cooking sessions/timers, and the health journal are
-- template-project scaffolding never wired to the DishLab client and PRD-MVP.md
-- explicitly lists them as "in the backend, not needed in the product" (§5,
-- "Не входить"). Recipe bookmarking is cut from MVP scope per t-11/t-22 (the
-- saved-recipes screen was removed; unlike the account feature, bookmarking
-- has no documented Release 3 return).

DROP TABLE IF EXISTS cooking_activity_outbox;
DROP TABLE IF EXISTS nutrition_logs;
DROP TABLE IF EXISTS cooking_timers;
DROP TABLE IF EXISTS cooking_sessions;

DROP TABLE IF EXISTS pantry_activity_outbox;
DROP TABLE IF EXISTS pantry_transactions;
DROP TABLE IF EXISTS pantry_items;

DROP TABLE IF EXISTS health_insights;
DROP TABLE IF EXISTS weight_logs;
DROP TABLE IF EXISTS water_logs;
DROP TABLE IF EXISTS health_nutrition_logs;

DROP TABLE IF EXISTS recipe_bookmarks;
