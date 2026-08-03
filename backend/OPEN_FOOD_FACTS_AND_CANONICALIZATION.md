# Open Food Facts і канонізація продуктів та рецептів

## Призначення

Цей документ описує, які дані DishLab може отримувати з Open Food Facts (OFF), як перетворювати їх на власну модель і як канонізувати товари та інгредієнти зі сторонніх рецептів. Цільова модель повинна:

- детально описувати відсканований товар, не прив'язуючи домен до JSON-схеми OFF;
- розпізнавати однакову харчову сутність за різними мовами, брендами й формами запису;
- враховувати походження, стан, обробку, алергени та дієтичні обмеження;
- розрізняти пряму відповідність, можливе приготування і справжню заміну;
- підтримувати швидкий пошук серед сотень тисяч рецептів без мережевих або AI-викликів у гарячому шляху.

OFF є зовнішнім джерелом фактів, але не доменною таксономією DishLab. Відповідь OFF слід зберігати як версійований знімок і мапити у власні сутності.

## Можливості Open Food Facts

Для нової інтеграції слід орієнтуватися на API v3.6. v2 застарів, хоча його структурований пошук ще може бути корисним. API v3 активно розвивається, тому потрібно явно вказувати мінорну версію і список `fields`.

Офіційні джерела:

- [огляд API і матриця можливостей](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/);
- [отримання продукту за штрихкодом](https://openfoodfacts.github.io/documentation/docs/Product-Opener/v3/products/get-api-v3-product-code/);
- [журнал змін API та схеми продукту](https://openfoodfacts.github.io/openfoodfacts-server/api/ref-api-and-product-schema-change-log/);
- [схема Product](https://openfoodfacts.github.io/documentation/docs/Product-Opener/schemas/schemas/product/).

### Ідентифікація та назви

OFF може надати:

- штрихкод `code`, тип продукту та внутрішні ідентифікатори;
- `product_name`, `generic_name`, скорочену назву та мовні варіанти;
- бренд, кількість в упаковці, одиницю, розмір порції;
- країни продажу, магазини, місця виробництва та походження;
- категорії, labels і канонічні taxonomy tags;
- `tags_sources` у v3.6: мінімальні теги окремо за джерелами з часом оновлення.

Для канонізації корисні насамперед `generic_name`, `categories_tags`, `ingredients` та `labels_tags`. Брендована назва на кшталт `Artesie jemne perliva` не повинна сама ставати назвою інгредієнта.

### Склад, алергени та харчові властивості

Доступні:

- оригінальний і локалізований `ingredients_text`;
- структурований список `ingredients` з taxonomy ID, вкладеними компонентами, приблизною часткою та порядком;
- `ingredients_tags`, `additives_tags`, `allergens_tags` і `traces_tags`;
- ознаки або оцінки vegan/vegetarian для інгредієнтів;
- palm oil, кількість добавок та інші похідні характеристики;
- NOVA group і відомості про ступінь промислової обробки.

`allergens_tags` означає заявлені дані конкретного товару, а не абсолютну гарантію безпеки. Відсутність алергену в OFF не дорівнює підтвердженій відсутності. Для суворого профілю значення `unknown` має блокувати автоматичну рекомендацію або вимагати попередження.

### Харчова цінність

У v3.6 поле `nutrition` містить структуровані набори даних:

- значення на 100 г, 100 мл або порцію;
- одиницю, джерело і вихідний спосіб подання;
- фактичне та обчислене значення;
- стан `as_sold` або `prepared`, якщо він відомий;
- енергію, білки, жири, насичені жири, вуглеводи, цукри, клітковину, сіль, натрій та інші нутрієнти.

Також доступні Nutri-Score grade/score/data, nutrient levels і прапорці повноти. DishLab повинен зберігати значення разом із базою вимірювання. Число `12` без `per = 100g`, одиниці та джерела недостатнє.

### Екологія, упаковка і зображення

OFF може надати:

- `environmental_score_grade`, score та деталізацію розрахунку;
- Agribalyse, оцінки походження й окремі екологічні коригування;
- компоненти упаковки: кількість, форма, матеріал, recycling instruction і місткість;
- вибрані та завантажені зображення лицьової сторони, складу, nutrition table й упаковки;
- URL різних розмірів, мову, ревізію та автора зображення.

Це дає змогу показувати не лише картку продукту, а й перевіряти склад за фото, пояснювати перероблення упаковки та поступово доповнювати базу OFF.

### Якість і походження даних

Корисні службові поля:

- `schema_version`, `rev`, creator, час створення та оновлення;
- completeness, data quality errors, warnings і info tags;
- джерела імпорту та `tags_sources`;
- `status`, `result`, `errors`, `warnings` на рівні API-відповіді.

Власний запис повинен містити `source`, `sourceProductId`, версію схеми, час отримання, сирий JSON або object-storage reference і результат мапінгу. Це дозволить повторно обробити старі товари після зміни канонізатора без нового сканування.

### Інші API-можливості

OFF також підтримує taxonomy canonicalization/suggestions, attribute groups, запис продуктів, завантаження та вибір зображень. Пошук продуктів у поточному backend використовує legacy `/cgi/search.pl`; його не слід змішувати з надійним barcode lookup. Для масового імпорту потрібні офіційні дампи, а не послідовне завантаження продуктів через API.

Запити повинні мати ідентифікований `User-Agent`, обмежений `fields`, timeout, retry з backoff і локальний cache. Повторне відкриття вже відсканованого товару не повинно залежати від доступності OFF.

### Розподіл API-трафіку

OFF обмежує product reads до 15 запитів за хвилину на IP, а search до 10 запитів за хвилину на IP. Для мобільних запитів ліміт застосовується окремо до IP кожного користувача. Тому звичайне сканування використовує device-first потік:

```text
local product cache
    -> OFF v3.6 з Android/iOS пристрою
    -> POST /api/v1/products/resolve у DishLab
    -> backend validation + canonicalization
    -> local product cache
```

Окремий OFF client не містить DishLab bearer token і надсилає власний `User-Agent`. На backend передається лише bounded snapshot запитаних полів, а не довільний raw JSON. Snapshot позначається `clientProvided`; його не можна вважати підтвердженням відсутності алергену або іншої safety-властивості.

Legacy `GET /api/v1/products/barcode/{barcode}` і server-side product search поки можуть звертатися до OFF із серверної IP. Мобільний scanner їх не використовує. Перед масовим запуском потрібно заповнити OFF API usage form; для великого серверного каталогу надалі слід використовувати daily exports або власний Product Opener instance.

## Поточний стан DishLab

`CatalogProduct` містить базові UI-поля та детальну source-модель: generic/localized identity, taxonomy lists, ingredient tree, allergens/traces/additives, structured nutrition, Nutri-Score, NOVA, environmental score, packaging і provenance. Мобільний OFF adapter читає v3.6 `nutrition`; legacy server-side `OpenFoodFactsProductCatalogProvider` ще використовує `/api/v3/product` і старе `nutriments`.

Міграція `V017` додає PostgreSQL persistence для `food_concepts`, `food_aliases`, `food_variants`, `retail_products` і `product_source_snapshots`. `ProductCanonicalizationService` відокремлений від provider і repository: він створює stable concept, variant із типізованими facets та aliases із provenance/confidence. Наприклад, `lightly sparkling water` зберігається як concept `water` і variant із `carbonation = LIGHTLY_SPARKLING`; `raw chicken meat` та `plant based meat` не об'єднуються в один concept.

Поточна канонізація намагається зіставити назву або останню категорію з текстовим каталогом. Це придатний fallback, але недостатній для станів, походження і безпечних замін. Теги не повинні одночасно означати синонім, властивість, стан і дозвіл на заміну.

## Цільова доменна модель

### Рівні продукту

| Сутність | Приклад | Відповідальність |
|---|---|---|
| `FoodConcept` | `water`, `chicken-meat`, `tofu` | Стабільна харчова сутність незалежно від бренду й мови |
| `FoodVariant` | lightly sparkling mineral water, raw chicken breast | Концепт плюс значущі фасети та стан |
| `RetailProduct` | Artesie jemne perliva, конкретний EAN | Брендований товар, упаковка, OFF snapshot і barcode |
| `PantryItem` | 750 ml відкритої пляшки | Наявність у користувача, кількість, строк і фактичний стан |
| `RecipeIngredientRequirement` | 250 ml still water; 500 g chicken to cook | Те, що потрібно рецепту, включно з допустимими перетвореннями |

`RetailProduct` посилається на `FoodVariant`, а не замінює його. Рецепт переважно посилається на `FoodConcept` і додає обмеження. Завдяки цьому новий бренд води не потребує зміни рецептів.

### Контракт детального продукту

Цільовий read model продукту повинен бути достатньо повним для картки товару, pantry, nutrition, recipe matching і повторної канонізації. Поля можуть зберігатися в кількох таблицях, але API має збирати їх в один `DetailedProduct`.

| Група | Рекомендовані дані |
|---|---|
| Identity | внутрішній ID, barcode scheme/value, OFF ID, product type, source revision |
| Names | display name, generic name, abbreviated name, primary language, localized names, brand, aliases |
| Classification | `FoodConcept` ID, variant ID, categories, parent categories, labels, countries, origins |
| Quantity | net quantity, unit, serving quantity/unit, number of portions, package count |
| Ingredients | original/localized text, ordered structured tree, percentages, compounds, additives, palm-oil flags |
| Safety | allergens, traces, evidence status, vegan/vegetarian status, dietary conflicts, unknown flags |
| State | origin, species, cut, preparation, physical form, carbonation, preservation, composition facets |
| Nutrition | basis, preparation, source, energy, macros, micronutrients, input and computed values |
| Scores | Nutri-Score, NOVA, nutrient levels, environmental score та пояснення/статус розрахунку |
| Packaging | components, count, shape, material, recycling, quantity, packaging tags |
| Media | front, ingredients, nutrition і packaging images за мовою та розміром |
| Commerce | stores, countries of sale, manufacturing/purchase places, producer codes |
| Quality | completeness, warnings, errors, data quality tags, missing critical fields |
| Provenance | джерело кожного важливого факту, fetchedAt, source timestamps, schema/canonicalizer versions |

Внутрішні поля не повинні один в один повторювати назви OFF. Наприклад, backend може читати `nutrition.aggregated_set.nutrients`, але віддавати стабільний `NutritionProfile` із `basis`, `preparation`, `nutrients` і `provenance`. Для невідомого значення використовується `null` або явний `UNKNOWN`, а не нуль чи порожній список, якщо вони можуть змінити рішення.

### Значущі фасети

Фасети мають бути типізованими полями або посиланнями на контрольований словник, а не довільним списком рядків:

- `origin`: animal, plant, fungi, algae, microbial/fermentation, mineral, synthetic, mixed, unknown;
- `species/source`: chicken, cattle, soy, pea, almond тощо;
- `anatomicalCut`: breast, thigh, liver, mince;
- `preparationState`: raw, cooked, boiled, baked, roasted, fried, smoked, fermented, dried;
- `physicalForm`: whole, sliced, minced, ground, powder, liquid, puree;
- `carbonation`: still, lightly-sparkling, sparkling, highly-sparkling, unknown;
- `preservation`: fresh, frozen, chilled, canned, pickled, shelf-stable;
- `composition`: fat level, sweetened/unsweetened, salt level, alcohol, concentration;
- `dietary`: vegan, vegetarian та інші підтверджені класи;
- `allergens` і `traces`: present, absent-confirmed або unknown;
- `culinaryRole`: main protein, liquid, fat, thickener, sweetener, garnish.

Не всі прикметники потрібно перетворювати на стан. Наприклад, `fresh water` у рецепті часто означає звичайну воду, тоді як `fresh meat` може бути суттєвим обмеженням preservation. Інтерпретація залежить від концепту та контексту кроків.

## Канонізація продуктів

Пайплайн для сканованого товару:

1. Нормалізувати barcode і спочатку перевірити локальний `RetailProduct` cache.
2. Отримати обмежену відповідь OFF v3.6 та зберегти source snapshot.
3. Витягнути назви, generic name, categories, ingredients, labels і структуровані фасети.
4. Побудувати кандидатів `FoodConcept` через barcode mapping, aliases і taxonomy IDs.
5. Застосувати детерміновані правила для стану, походження, газованості та дієтичних ознак.
6. Використовувати модель або embeddings лише для ранжування невизначених кандидатів, повертаючи structured result із confidence та evidence.
7. Зберегти рішення, версію канонізатора й окрему provenance для кожної ознаки.
8. Низьку впевненість залишити як `unknown`; не створювати глобальний концепт автоматично з назви бренду.

Пріоритет доказів: ручне підтвердження експертом або користувачем, точний barcode mapping, структуровані OFF taxonomy/ingredients, детерміноване правило, модельне припущення. Виправлення користувача не повинно без модерації змінювати глобальну таксономію.

## Канонізація імпортованих рецептів

Для кожного інгредієнтного рядка потрібно зберегти оригінальний текст і окремо отримати:

- amount, unit, діапазон і ознаку optional;
- базову назву та мову;
- `FoodConcept`;
- required facets і facets, які можна ігнорувати;
- preparation note (`chopped`, `cooked`, `for frying`);
- роль у рецепті й зв'язок із кроками;
- confidence, candidates і причину вибору.

Рекомендований порядок: parser одиниць -> словник aliases -> локальний taxonomy search -> контекст назви/опису/кроків -> модельний fallback -> ручна черга для неоднозначних випадків. Канонізація виконується один раз під час імпорту і повторюється лише після зміни версії правил.

Рецепт також потребує канонічних полів: cuisine, meal type, techniques, equipment, dietary claims, servings, normalized durations і nutrition basis. Дієтичний claim рецепту слід обчислювати зі складу, а не безумовно довіряти сторонньому тегу `vegan`.

## Відповідність і перетворення

Відповідність є напрямленою. Її не можна моделювати звичайним набором синонімів.

| Результат | Значення | Приклад |
|---|---|---|
| `EXACT` | Концепт і обов'язкові фасети збігаються | still water -> still water |
| `COMPATIBLE` | Відмінність не впливає на цей рецепт | брендована негазована вода -> water |
| `TRANSFORMABLE` | Користувач може отримати потрібний стан | raw chicken -> baked chicken через крок запікання |
| `SUBSTITUTE` | Інший концепт виконує роль із компромісом | tofu замість chicken у дозволеному варіанті |
| `INCOMPATIBLE` | Порушено hard constraint або неможливий напрям | cooked chicken -> raw chicken для тартару |
| `UNKNOWN` | Даних недостатньо для безпечного рішення | невідомий склад при суворій алергії |

Перетворення зберігаються як правила `fromVariant -> toState` з умовами: техніка, обладнання, час, зміна маси, допустимі recipe roles і cost. Наприклад, `raw -> boiled` дозволено, якщо рецепт містить відповідний крок або DishLab може його запропонувати. `boiled -> raw` не дозволено.

### Вода

`Artesie jemne perliva` канонізується як `water` із facets `mineral` і `lightly-sparkling`. Для загальної вимоги `water` це може бути `COMPATIBLE` з невеликим штрафом, якщо газованість неважлива. Для бульйону, варіння або рецепту з вимогою `still` це не точна відповідність; можливе перетворення через дегазацію, якщо воно практичне. Для тіста, де бульбашки мають функцію, sparkling water не можна мовчки замінити негазованою.

### М'ясо і plant-based продукти

Animal meat і plant-based meat не є одним `FoodConcept`. Вони можуть мати спільний culinary role і напрямлене substitution rule, але зберігають різне `origin` та склад.

- Для vegan профілю animal origin є `INCOMPATIBLE`, навіть якщо назва або смак схожі.
- Для користувача без обмежень plant-based аналог не повинен автоматично вважатися тим самим м'ясом: поведінка під час приготування може відрізнятися.
- Сире animal meat може відповідати готовому м'ясу через `TRANSFORMABLE`, якщо рецепт справді готує його.
- Якщо рецепт вимагає вже smoked або cured продукт як джерело смаку, сире м'ясо не є автоматично достатнім.

## Профіль користувача і безпека

Перед ранжуванням застосовуються hard constraints:

- підтверджені алергії та непереносимості;
- vegan/vegetarian та інші явно ввімкнені дієтичні правила;
- медичні обмеження, якщо продукт їх підтримуватиме;
- заборонені інгредієнти;
- вимоги, які неможливо отримати доступною технікою або обладнанням.

Після цього soft preferences впливають лише на порядок: улюблені кухні, мінімум замін, час, складність, бажана nutrition, використання продуктів до завершення строку. Hard constraint ніколи не можна компенсувати високим match score.

## Швидкий пошук рецептів

У поточному каталозі понад 522 тисячі рецептів і понад 4,1 мільйона ingredient links. Тому канонізація, переклад, fuzzy matching, embeddings та AI не повинні виконуватися під час кожного запиту.

### Write-time робота

Під час імпорту або оновлення потрібно наперед зберігати:

- `recipe_ingredient_requirements(recipe_id, concept_id, importance, required_facets, state_id)`;
- канонічні ID, а не повторені рядкові теги;
- обчислені dietary/allergen flags рецепту;
- кількість обов'язкових інгредієнтів;
- дозволені короткі transformation/substitution edges;
- версію канонізатора і статус індексації.

Корисні індекси:

```sql
recipe_ingredient_requirements(concept_id, recipe_id)
recipe_ingredient_requirements(recipe_id, required)
ingredient_aliases(language, normalized_alias)
retail_product_identifiers(scheme, identifier)
pantry_items(user_id, concept_id)
food_transformations(from_concept_id, from_state_id, to_state_id)
```

### Query-time робота

Пошук має складатися з двох фаз:

1. SQL candidate retrieval за canonical concept IDs, hard constraints і базовою coverage. Він повертає обмежений набір, наприклад 200-1000 recipe IDs.
2. Детальне ранжування кандидатів з урахуванням станів, transformation cost, строків придатності, вподобань і якості рецепту.

Орієнтовний score:

```text
score = requiredCoverage
      + optionalCoverage
      + expiringProductBonus
      + preferenceScore
      - missingRequiredPenalty
      - transformationCost
      - substitutionPenalty
      - uncertaintyPenalty
```

Рекомендовано зберігати коротку materialized search projection для рецепту. Для часто вживаних концептів можна кешувати candidate recipe IDs або posting lists. Векторний пошук корисний для розпізнавання нового тексту під час імпорту, але не повинен замінювати точні індексовані joins у фінальному пошуку.

Поточні порівняння через `LOWER(canonical_name)`, великі synonym groups і корельовані підзапити слід вважати перехідним рішенням. Оптимізацію треба робити після вимірювань: p50/p95 latency, кількість кандидатів, `EXPLAIN QUERY PLAN`, cache hit rate і частка рецептів, що потребують другого етапу.

## Рекомендована схема зберігання

Мінімальний набір нових сутностей:

- `food_concepts`: stable ID, canonical name, parent concept, taxonomy version;
- `food_aliases`: text, normalized text, language, concept ID, source, confidence;
- `food_variants`: concept ID і нормалізовані facets;
- `retail_products`: barcode, variant ID, brand, package facts, OFF reference;
- `source_product_snapshots`: provider, schema version, fetchedAt, hash, raw payload;
- `food_transformations`: напрям, умови, cost і safety status;
- `food_substitutions`: напрям, culinary role, profile constraints і penalty;
- `recipe_ingredient_requirements`: concept, amount, unit, state/facet constraints, importance;
- `canonicalization_decisions`: input, candidates, selected ID, evidence, confidence, model/rule version.

Для facets, які часто фільтруються, варто використовувати окремі колонки або компактні IDs. `JSONB` корисний для рідкісних властивостей і source snapshots, але не як єдиний пошуковий індекс. Алергени й дієтичні обмеження повинні мати контрольовані коди.

## Правила надійності

- Не створювати канонічний інгредієнт простим slugify назви товару.
- Не робити симетричними transformation і substitution edges.
- Не перетворювати `unknown` на `false` для алергенів, походження чи vegan status.
- Не дозволяти AI самостійно додавати глобальні safety rules.
- Зберігати оригінальний текст, всі кандидати, confidence і версію рішення.
- Повторно індексувати записи після зміни таксономії у фоні, не під час user request.
- Відділяти факт (`contains milk`) від висновку (`not vegan`) і рекомендації (`hide for this user`).
- Не віддавати API DTO Open Food Facts напряму мобільному клієнту.

## Поетапна реалізація

1. **Завершено:** розширити OFF adapter до явного v3.6 DTO, source snapshot і детальної внутрішньої моделі продукту.
2. **Базовий етап завершено:** додати `FoodConcept`, aliases, typed facets, PostgreSQL persistence і точний barcode mapping.
3. Перенести імпорт рецептів на `RecipeIngredientRequirement` з confidence та unresolved queue.
4. Реалізувати hard dietary/allergen filters і напрямлені стани `raw -> cooked`.
5. Перевести пошук із рядкових назв на indexed concept IDs і двофазне ранжування.
6. Додати контрольовані substitutions, contextual roles і фонову переіндексацію.
7. Оптимізувати лише за вимірами на повному каталозі та реальних pantry profiles.

Перший реліз не повинен намагатися описати всі продукти світу. Надійніше почати з невеликого контрольованого набору facets і трансформацій, залишаючи невідомі випадки як `UNKNOWN`, а потім розширювати таксономію на основі реальних імпортів і виправлень.
