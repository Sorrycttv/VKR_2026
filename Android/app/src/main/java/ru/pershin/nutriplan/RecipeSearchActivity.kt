package ru.pershin.nutriplan

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Один экран обслуживает каталог блюд и точный поиск рецепта.
class RecipeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_search)

        val title = findViewById<TextView>(R.id.recipeScreenTitle)
        val input = findViewById<EditText>(R.id.dishInput)
        val button = findViewById<Button>(R.id.searchButton)
        val progress = findViewById<ProgressBar>(R.id.searchProgressBar)
        val result = findViewById<TextView>(R.id.resultText)

        // Null означает загрузку всей книги блюд, строка — фильтрацию каталога.
        fun loadRecipes(query: String?) {
            val token = getSharedPreferences(
                "nutriplan_session",
                MODE_PRIVATE
            ).getString("access_token", null)

            if (token == null) {
                result.text = "Сессия истекла. Выполните вход заново."
                return
            }

            progress.visibility = View.VISIBLE
            button.isEnabled = false

            ApiClient.service.getRecipes("Bearer $token", query).enqueue(
                object : Callback<RecipesResponse> {
                    override fun onResponse(
                        call: Call<RecipesResponse>,
                        response: Response<RecipesResponse>
                    ) {
                        progress.visibility = View.GONE
                        button.isEnabled = true

                        val recipes = response.body()?.recipes.orEmpty()
                        if (query == null) {
                            result.text = if (recipes.isEmpty()) {
                                "Каталог пока пуст."
                            } else {
                                "Доступные блюда:\n\n" + recipes.joinToString("\n") {
                                    "• ${it.name} — ${it.calories} ккал, ${it.preparationMinutes} мин"
                                } + "\n\nВведите название блюда выше, чтобы открыть рецепт."
                            }
                            return
                        }

                        result.text = formatSearchResult(recipes, query)
                    }

                    override fun onFailure(
                        call: Call<RecipesResponse>,
                        error: Throwable
                    ) {
                        progress.visibility = View.GONE
                        button.isEnabled = true
                        result.text = "Не удалось загрузить каталог: ${error.localizedMessage}"
                    }
                }
            )
        }

        button.setOnClickListener {
            val query = input.text.toString().trim()
            if (query.length < 2) {
                input.error = "Введите минимум 2 символа"
                return@setOnClickListener
            }
            loadRecipes(query)
        }

        if (intent.getBooleanExtra("catalog_mode", false)) {
            title.text = "Книга блюд"
            input.hint = "Введите название блюда из каталога"
            loadRecipes(null)
        }
    }

    private fun formatSearchResult(recipes: List<RecipeItem>, query: String): String {
        val matches = recipes.filter { recipe ->
            recipe.name.contains(query, ignoreCase = true) ||
                    recipe.ingredients.any {
                        it.product.contains(query, ignoreCase = true)
                    }
        }
        val exact = recipes.firstOrNull { it.name.equals(query, ignoreCase = true) }
        val selected = exact ?: matches.singleOrNull()

        if (selected != null) return formatRecipe(selected)
        if (matches.isEmpty()) return "Точного проверенного рецепта не найдено."

        val variants = matches.take(5).joinToString("\n") { recipe ->
            val ingredient = recipe.ingredients.firstOrNull {
                it.product.contains(query, ignoreCase = true)
            }
            when {
                recipe.name.contains(query, ignoreCase = true) ->
                    "• ${recipe.name} — совпадение в названии"
                ingredient != null ->
                    "• ${recipe.name} — ингредиент: ${ingredient.product}"
                else -> "• ${recipe.name}"
            }
        }
        return "Выберите более точное название:\n\n$variants"
    }

    private fun formatRecipe(recipe: RecipeItem): String {
        val ingredients = recipe.ingredients.joinToString("\n") {
            "• ${it.product}: ${it.grams.toInt()} г"
        }
        return """
            ${recipe.name}

            Порция: ${recipe.portionWeightGrams} г
            Время: ${recipe.preparationMinutes} мин

            Ингредиенты:
            $ingredients

            Как приготовить:
            ${recipe.instructions}
        """.trimIndent()
    }
}
