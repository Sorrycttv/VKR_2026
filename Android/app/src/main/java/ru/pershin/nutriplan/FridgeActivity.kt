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

// Экран хранит домашний запас и показывает блюда из имеющихся продуктов.
class FridgeActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var progress: ProgressBar
    private lateinit var saveButton: Button
    private lateinit var recommendationsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fridge)

        input = findViewById(R.id.fridgeInput)
        result = findViewById(R.id.fridgeResult)
        progress = findViewById(R.id.fridgeProgress)
        saveButton = findViewById(R.id.saveFridgeButton)
        recommendationsButton = findViewById(R.id.fridgeRecommendationsButton)

        saveButton.setOnClickListener { saveFridge() }
        recommendationsButton.setOnClickListener { loadRecommendations() }
        loadFridge()
    }

    private fun token(): String? = getSharedPreferences(
        "nutriplan_session",
        MODE_PRIVATE
    ).getString("access_token", null)

    private fun loadFridge() {
        val accessToken = token() ?: return
        setLoading(true)
        ApiClient.service.getFridge("Bearer $accessToken").enqueue(
            object : Callback<FridgeResponse> {
                override fun onResponse(
                    call: Call<FridgeResponse>,
                    response: Response<FridgeResponse>
                ) {
                    setLoading(false)
                    val items = response.body()?.items.orEmpty()
                    input.setText(items.joinToString("\n") {
                        "${it.product} ${it.grams.toInt()} г"
                    })
                    result.text = if (items.isEmpty()) {
                        "Добавьте продукты в свободной форме."
                    } else {
                        "Сохранено продуктов: ${items.size}"
                    }
                }

                override fun onFailure(call: Call<FridgeResponse>, error: Throwable) {
                    setLoading(false)
                    result.text = "Не удалось загрузить домашний запас."
                }
            }
        )
    }

    private fun saveFridge() {
        val accessToken = token() ?: return
        val (items, invalid) = parseProducts(input.text.toString())
        if (items.isEmpty()) {
            input.error = "Укажите продукт и количество, например: курица 500 г"
            return
        }
        if (invalid.isNotEmpty()) {
            input.error = "Не распознано количество: ${invalid.joinToString()}"
            return
        }

        setLoading(true)
        ApiClient.service.saveFridge("Bearer $accessToken", items).enqueue(
            object : Callback<StatusResponse> {
                override fun onResponse(
                    call: Call<StatusResponse>,
                    response: Response<StatusResponse>
                ) {
                    setLoading(false)
                    result.text = if (response.isSuccessful) {
                        "Домашний запас сохранён."
                    } else {
                        "Не удалось сохранить продукты. Код: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<StatusResponse>, error: Throwable) {
                    setLoading(false)
                    result.text = "Ошибка подключения: ${error.localizedMessage}"
                }
            }
        )
    }

    private fun loadRecommendations() {
        val accessToken = token() ?: return
        setLoading(true)
        ApiClient.service.getFridgeRecommendations("Bearer $accessToken").enqueue(
            object : Callback<FridgeRecommendationsResponse> {
                override fun onResponse(
                    call: Call<FridgeRecommendationsResponse>,
                    response: Response<FridgeRecommendationsResponse>
                ) {
                    setLoading(false)
                    val data = response.body()
                    if (!response.isSuccessful || data == null) {
                        result.text = "Не удалось подобрать блюда. Код: ${response.code()}"
                        return
                    }
                    val ready = data.canCook.take(5).joinToString("\n") { "• ${it.name}" }
                    val near = data.needToBuy.take(5).joinToString("\n") { recipe ->
                        val missing = recipe.missing.joinToString { "${it.product} ${it.grams} г" }
                        "• ${recipe.name}: докупить $missing"
                    }
                    result.text = buildString {
                        append("Можно приготовить:\n")
                        append(if (ready.isBlank()) "Пока нет полного совпадения." else ready)
                        append("\n\nБлижайшие варианты:\n")
                        append(if (near.isBlank()) "Нет вариантов." else near)
                    }
                }

                override fun onFailure(
                    call: Call<FridgeRecommendationsResponse>,
                    error: Throwable
                ) {
                    setLoading(false)
                    result.text = "Ошибка подключения: ${error.localizedMessage}"
                }
            }
        )
    }

    // Поддерживаются варианты «курица 500 г», «1 кг картофеля» и строки
    // с двоеточием или тире. Строгий шаблон ввода пользователю не требуется.
    private fun parseProducts(text: String): Pair<List<PantryItemRequest>, List<String>> {
        val quantity = Regex(
            """(\d+(?:[.,]\d+)?)\s*(кг|килограмм(?:а|ов)?|г|гр|грамм(?:а|ов)?)""",
            RegexOption.IGNORE_CASE
        )
        val items = mutableListOf<PantryItemRequest>()
        val invalid = mutableListOf<String>()

        text.split('\n', ';', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { row ->
                val match = quantity.find(row)
                if (match == null) {
                    invalid += row
                    return@forEach
                }
                val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull()
                val unit = match.groupValues[2].lowercase()
                val product = row.removeRange(match.range).trim(' ', ':', '-', '–', '—')
                if (amount == null || amount <= 0 || product.isBlank()) {
                    invalid += row
                    return@forEach
                }
                val grams = if (unit.startsWith("кг") || unit.startsWith("килограмм")) {
                    amount * 1000
                } else {
                    amount
                }
                items += PantryItemRequest(product, grams)
            }

        return items to invalid
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !loading
        recommendationsButton.isEnabled = !loading
    }
}
