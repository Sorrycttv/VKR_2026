# NutriPlan Android

Клиентская часть выпускного проекта NutriPlan написана на Kotlin с XML-разметками экранов.

## Структура приложения

- `MainActivity.kt` — старт приложения и восстановление сохранённой сессии.
- `LoginActivity.kt`, `RegisterActivity.kt` — вход и регистрация.
- `ProfileActivity.kt` — параметры пользователя, влияющие на расчёт рациона.
- `HomeActivity.kt` — главное меню.
- `PlanActivity.kt` — формирование, сохранение и сброс плана питания.
- `RecipeSearchActivity.kt` — книга блюд и поиск проверенного рецепта.
- `FridgeActivity.kt` — ввод продуктов в свободной форме и рекомендации по остаткам.
- `acriv_coach.kt` — лента сообщений ИИ-коуча.
- `ApiService.kt` — модели запросов и интерфейс серверного API.
- `app/src/main/res/layout/` — XML-разметки экранов.

Адрес серверного API задаётся константой `BASE_URL` в `ApiService.kt`. Репозиторий не содержит паролей, токенов пользователей и локального файла `local.properties`.
