#!/usr/bin/env python3
"""Build data/recipe-catalog.db from dataset_clean.csv (t-91).

Rebuilds the whole SQLite recipe catalog read by SqliteRecipeCatalogRepository
from the new CSV source. Run from the backend/ directory:

    python3 scripts/build_recipe_catalog.py [--csv dataset_clean.csv] [--out data/recipe-catalog.db]

The output file is built at a temporary path and atomically renamed into
place, so a crash mid-run never leaves a half-written db where the app
expects one.
"""
from __future__ import annotations

import argparse
import ast
import csv
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

csv.field_size_limit(10_000_000)

SCHEMA = """
CREATE TABLE recipes (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    author_name TEXT,
    cook_time TEXT,
    prep_time TEXT,
    total_time TEXT,
    description TEXT,
    images TEXT,
    category TEXT,
    keywords TEXT,
    aggregated_rating REAL,
    calories REAL,
    fat_content REAL,
    saturated_fat_content REAL,
    cholesterol_content REAL,
    sodium_content REAL,
    carbohydrate_content REAL,
    fiber_content REAL,
    sugar_content REAL,
    protein_content REAL,
    instructions TEXT,
    instruction_times_seconds TEXT,
    is_active INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE ingredients (
    id INTEGER PRIMARY KEY,
    canonical_name TEXT NOT NULL UNIQUE
);
CREATE TABLE ingredient_aliases (
    normalized_alias TEXT PRIMARY KEY,
    original_alias TEXT NOT NULL,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
    source TEXT NOT NULL,
    confidence REAL NOT NULL
);
CREATE TABLE recipe_ingredients (
    recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
    original_text TEXT NOT NULL,
    quantity TEXT,
    PRIMARY KEY (recipe_id, position)
);
CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients(ingredient_id, recipe_id);
CREATE INDEX idx_recipe_rating ON recipes(aggregated_rating DESC);
CREATE INDEX idx_recipes_category_name ON recipes(category, name COLLATE NOCASE);
CREATE INDEX idx_recipes_active_rating ON recipes(is_active, aggregated_rating DESC);
"""

# --- Ingredient name normalization, ported from
# infrastructure/.../SqliteRecipeCatalogRepository.kt (IngredientNameNormalizer)
# so canonical names stay consistent with pantry-match / search lookups.

_SPACES = re.compile(r"\s+")
_NON_WORD = re.compile(r"[^a-z0-9%+ ]+")
_LANGUAGE_TAG = re.compile(r"^[a-z]{2,3}:")
_DESCRIPTORS = {
    "baking", "cooking", "crisp", "dessert", "dried", "fresh", "frozen", "green",
    "ground", "large", "medium", "minced", "peeled", "raw", "red", "ripe", "sliced",
    "small", "sour", "sweet", "tart", "yellow",
}
_EXCEPTIONS = {"asparagus", "couscous", "glass", "grass", "molasses"}
_IRREGULAR = {
    "berries": "berry", "cherries": "cherry", "cloves": "clove",
    "leaves": "leaf", "loaves": "loaf", "mangoes": "mango",
    "potatoes": "potato", "tomatoes": "tomato",
}


def normalize_alias(value: str) -> str:
    decomposed = "".join(
        c for c in unicodedata.normalize("NFKD", value)
        if unicodedata.category(c) != "Mn"
    )
    text = decomposed.strip().strip("\"'").lower()
    text = _LANGUAGE_TAG.sub("", text)
    text = text.replace("_", " ").replace("-", " ")
    text = _NON_WORD.sub(" ", text)
    text = _SPACES.sub(" ", text)
    return text.strip()


def _singularize(word: str) -> str:
    if word in _EXCEPTIONS:
        return word
    if word in _IRREGULAR:
        return _IRREGULAR[word]
    if word.endswith("ies") and len(word) > 4:
        return word[:-3] + "y"
    if word.endswith(("ches", "shes", "xes", "zes")):
        return word[:-2]
    if word.endswith("ses") and not word.endswith("sses"):
        return word[:-1]
    if word.endswith("s") and not word.endswith("ss") and not word.endswith("us"):
        return word[:-1]
    return word


def canonicalize(value: str) -> str:
    words = [w for w in normalize_alias(value).split(" ") if w]
    while len(words) > 1 and words[0] in _DESCRIPTORS:
        words.pop(0)
    if words:
        words[-1] = _singularize(words[-1])
    return " ".join(words)


def r_vector(items: list[str]) -> str | None:
    cleaned = [i.strip() for i in items if i and i.strip()]
    if not cleaned:
        return None
    escaped = [i.replace("\\", "\\\\").replace('"', '\\"') for i in cleaned]
    return "c(" + ", ".join(f'"{i}"' for i in escaped) + ")"


def minutes_to_duration(raw: str) -> str | None:
    raw = (raw or "").strip()
    if not raw:
        return None
    try:
        total = round(float(raw))
    except ValueError:
        return None
    if total <= 0:
        return None
    hours, minutes = divmod(total, 60)
    if hours and minutes:
        return f"PT{hours}H{minutes}M"
    if hours:
        return f"PT{hours}H"
    return f"PT{minutes}M"


_NUMBER = re.compile(r"-?\d+(?:\.\d+)?")


def parse_nutrition_number(raw: str | None) -> float | None:
    if not raw:
        return None
    match = _NUMBER.search(raw)
    return float(match.group()) if match else None


def literal_eval_or(raw: str, default):
    raw = (raw or "").strip()
    if not raw:
        return default
    try:
        return ast.literal_eval(raw)
    except (ValueError, SyntaxError):
        return default


def build(csv_path: Path, out_path: Path) -> None:
    tmp_path = out_path.with_suffix(out_path.suffix + ".tmp")
    tmp_path.unlink(missing_ok=True)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(tmp_path)
    conn.executescript(SCHEMA)

    ingredient_ids: dict[str, int] = {}
    alias_seen: set[str] = set()
    next_ingredient_id = 1
    next_recipe_id = 1
    skipped_unparseable = 0
    skipped_no_ingredients = 0

    recipe_rows = []
    ingredient_rows = []
    alias_rows = []
    ri_rows = []

    with csv_path.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = (row.get("Name") or "").strip()
            if not name:
                continue

            ingredients = literal_eval_or(row.get("Ingredients", ""), None)
            if not isinstance(ingredients, list) or not ingredients:
                skipped_unparseable += 1
                continue
            raw_texts = literal_eval_or(row.get("Ingredients_Raw", ""), [])
            if not isinstance(raw_texts, list):
                raw_texts = []
            instructions = literal_eval_or(row.get("Instructions", ""), [])
            if not isinstance(instructions, list):
                instructions = []
            category_list = literal_eval_or(row.get("Category", ""), [])
            cuisine_list = literal_eval_or(row.get("Cuisine", ""), [])
            methods_list = literal_eval_or(row.get("Cooking Methods", ""), [])
            implements_list = literal_eval_or(row.get("Implements", ""), [])
            nutrition = literal_eval_or(row.get("Nutrition", ""), {})
            if not isinstance(nutrition, dict):
                nutrition = {}

            parsed_ingredients = []
            for idx, item in enumerate(ingredients):
                if not isinstance(item, dict):
                    continue
                raw_name = (item.get("ingredient") or "").strip()
                if not raw_name:
                    continue
                canonical = canonicalize(raw_name)
                if not canonical:
                    continue
                text = raw_texts[idx].strip() if idx < len(raw_texts) and raw_texts[idx] else None
                if not text:
                    quantity = (item.get("quantity") or "").strip()
                    unit = (item.get("unit") or "").strip()
                    text = " ".join(p for p in (quantity, unit, raw_name) if p)
                quantity_value = (item.get("quantity") or "").strip() or None
                parsed_ingredients.append((raw_name, canonical, text, quantity_value))

            if not parsed_ingredients:
                skipped_no_ingredients += 1
                continue

            recipe_id = next_recipe_id
            next_recipe_id += 1

            category_primary = category_list[0] if isinstance(category_list, list) and category_list else None
            keywords_source = []
            for group in (category_list, cuisine_list, methods_list, implements_list):
                if isinstance(group, list):
                    keywords_source.extend(str(g) for g in group)
            keywords = r_vector(keywords_source)

            rating = parse_nutrition_number(row.get("Rating Value"))
            cook_time = minutes_to_duration(row.get("Cooking Time", ""))
            prep_time = minutes_to_duration(row.get("Preparation Time", ""))
            total_minutes = 0.0
            has_total = False
            for raw_val in (row.get("Preparation Time", ""), row.get("Cooking Time", "")):
                try:
                    total_minutes += float(raw_val)
                    has_total = True
                except (TypeError, ValueError):
                    pass
            total_time = minutes_to_duration(str(total_minutes)) if has_total else None

            instructions_text = r_vector([str(s) for s in instructions])

            recipe_rows.append((
                recipe_id,
                name,
                (row.get("Author") or "").strip() or None,
                cook_time,
                prep_time,
                total_time,
                None,  # description: not present in the new dataset
                None,  # images: not present in the new dataset
                category_primary,
                keywords,
                rating,
                parse_nutrition_number(nutrition.get("Calories")),
                parse_nutrition_number(nutrition.get("Fat")),
                parse_nutrition_number(nutrition.get("Saturated Fat")),
                parse_nutrition_number(nutrition.get("Cholesterol")),
                parse_nutrition_number(nutrition.get("Sodium")),
                parse_nutrition_number(nutrition.get("Carbohydrates")),
                parse_nutrition_number(nutrition.get("Fiber")),
                parse_nutrition_number(nutrition.get("Sugar")),
                parse_nutrition_number(nutrition.get("Protein")),
                instructions_text,
                None,  # instruction_times_seconds: no per-step timing in the new dataset
                1,
            ))

            for position, (raw_name, canonical, text, quantity_value) in enumerate(parsed_ingredients, start=1):
                ingredient_id = ingredient_ids.get(canonical)
                if ingredient_id is None:
                    ingredient_id = next_ingredient_id
                    next_ingredient_id += 1
                    ingredient_ids[canonical] = ingredient_id
                    ingredient_rows.append((ingredient_id, canonical))

                normalized_alias = normalize_alias(raw_name)
                if normalized_alias and normalized_alias not in alias_seen:
                    alias_seen.add(normalized_alias)
                    alias_rows.append((normalized_alias, raw_name, ingredient_id, "dataset_clean.csv", 1.0))

                ri_rows.append((recipe_id, position, ingredient_id, text, quantity_value))

    conn.executemany(
        "INSERT INTO recipes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        recipe_rows,
    )
    conn.executemany("INSERT INTO ingredients VALUES (?,?)", ingredient_rows)
    conn.executemany("INSERT INTO ingredient_aliases VALUES (?,?,?,?,?)", alias_rows)
    conn.executemany(
        "INSERT INTO recipe_ingredients VALUES (?,?,?,?,?)",
        ri_rows,
    )
    conn.commit()
    conn.execute("ANALYZE")
    conn.close()

    tmp_path.replace(out_path)

    print(f"recipes: {len(recipe_rows)}", file=sys.stderr)
    print(f"ingredients: {len(ingredient_rows)}", file=sys.stderr)
    print(f"recipe_ingredients: {len(ri_rows)}", file=sys.stderr)
    print(f"skipped (unparseable ingredients): {skipped_unparseable}", file=sys.stderr)
    print(f"skipped (no usable ingredient rows): {skipped_no_ingredients}", file=sys.stderr)
    print(f"wrote {out_path}", file=sys.stderr)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", type=Path, default=Path("dataset_clean.csv"))
    parser.add_argument("--out", type=Path, default=Path("data/recipe-catalog.db"))
    args = parser.parse_args()
    build(args.csv, args.out)


if __name__ == "__main__":
    main()
