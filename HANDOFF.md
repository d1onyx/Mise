# HANDOFF: t-133 — debounce кнопок від мультітапів

## Мета

Task t-133 (`in_progress`, роль `mobile-dev`): захистити всі кнопки додатку
від мультітапів — швидкий подвійний тап не повинен виконувати дію (навігацію,
мережевий запит, тогл) двічі.

Гілка/worktree: `fix/t-133-debounce-buttons` у
`/home/denis/.herdr/worktrees/DishLab/task-t-133`.

## Поточний прогрес

Три коміти в worktree (**не змержено, не запушено** — мердж робить лише
`product-manager`, mobile-dev тільки комітить):

1. `6199687` — перша версія: новий `Modifier.debouncedClickable` (design-system)
   на основі `remember { mutableStateOf(true) }`, підключений у
   `MisePrimaryButton`/`MiseGhostButton`/`MiseTextAction`/`MiseCircleButton`/
   `MisePanel` та напряму в `OnboardingScreen`, `ConnectionOverviewScreen`,
   `GraphScreen`, `RecipeCard`.
2. `51518ef` — виправлення бага: перша версія **ніколи фактично не
   дебаунсила**. `enabled` перемикався всередині `@Composable fun
   Modifier.debouncedClickable()`; зміна `mutableStateOf` там інвалідує лише
   власну recompose-групу цієї функції, а не `Box`/`Modifier`-вираз виклика,
   тож оновлене значення `enabled` ніколи не долітало до реального
   `clickable`. Переписано на імперативну перевірку часу (`TimeSource`)
   прямо всередині лямбди `onClick`, без реактивного стану.
3. `01a4820` — фінальна переробка на прохання власника: власник хотів явну
   **візуальну** ознаку (кнопка видимо неактивна після тапу), а не тиху
   ігнорацію повторного кліку. Замінено на `rememberDebouncedClick()` →
   `DebouncedClick` (`design-system/.../DebouncedClickable.kt`) — клас з
   `enabled: Boolean by mutableStateOf(true)`, що читається **напряму** в
   тому ж composable, що будує `Box`/`Row` (не через проміжну
   `Modifier`-функцію), і візуально притлумлюється (`alpha 0.45–0.7`) одразу
   після тапу до кінця вікна дебаунсу (500мс, `DEFAULT_CLICK_DEBOUNCE_MILLIS`).

`./gradlew build` (весь репозиторій, Android + iOS klibs, тести) — зелений
після кожного з трьох комітів.

## Що спрацювало

- Візуальний фідбек (alpha) + читання `enabled`-стану прямо в тому composable,
  що будує реальний `Box`/`Row`/`Text` — саме так треба гейтити
  `Modifier.clickable(enabled = ...)`, а не ховати логіку в окрему
  `@Composable fun Modifier.xxx(): Modifier` функцію.
- `rememberDebouncedClick(onClick)` — маленький клас `DebouncedClick :
  () -> Unit`, що інкапсулює `enabled` + таймер скидання через
  `rememberCoroutineScope().launch { delay(...) }`. Один інстанс на
  composable-виклик (через `remember`), стабільний між рекомпозиціями.

## Що НЕ спрацювало

- **Реактивний `mutableStateOf` всередині окремої `@Composable fun
  Modifier.debouncedClickable()`** — класична пастка Compose:
  зміна стану інвалідує власну recompose-групу функції, не викликача.
  Симптом на пристрої: "дебаунс не працює взагалі, дія все одно виконується
  двічі" — і саме так спершу проявилось (перший баг-репорт користувача).
- **Чиста time-based перевірка без видимого стану** (коміт `51518ef`) —
  технічно правильна (реально блокує повторний клік), але користувач хотів
  явну візуальну ознаку неактивності кнопки, а не мовчазне ігнорування —
  тому знадобився ще один раунд (`01a4820`).

## Наступні кроки

1. **Пересібрати APK з гілки `fix/t-133-debounce-buttons` (останній коміт
   `01a4820`) і перевірити вручну** — власник кілька разів тестував
   попередні версії й знаходив, що дебаунс не працює. Перед тим, як
   рапортувати DoD виконаним, обов'язково дочекайся підтвердження від
   власника, що зараз (v3) все ок — попередні два рази виявлялось, що ні.
2. Якщо власник підтвердить — **не комітити `herdr task complete t-133`
   без явного "ок" власника** (правило з пам'яті: не завершувати задачу без
   згоди, навіть якщо DoD виглядає виконаним).
3. Мердж і push — виключно `product-manager`, mobile-dev цього не робить.
4. Файли, змінені за t-133 (для довідки, усі в `design-system` +
   4 feature-модулі):
   - `design-system/src/commonMain/kotlin/.../component/DebouncedClickable.kt` (новий)
   - `design-system/src/commonMain/kotlin/.../component/Buttons.kt`
   - `design-system/src/commonMain/kotlin/.../component/Surfaces.kt`
   - `feature/home/.../onboarding/OnboardingScreen.kt`
   - `feature/products/.../connections/ConnectionOverviewScreen.kt`
   - `feature/products/.../graph/GraphScreen.kt`
   - `feature/recipes/.../components/RecipeCard.kt`
5. Є ще купа інших `in_progress`/`ready` задач role mobile-dev (t-21, t-25,
   t-103, t-104, t-123, t-125, t-130, t-138) — жодна з них не зачіпалась у
   цій сесії, це окрема робота.
