package ru.pershin.nutriplan

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlanActivity : AppCompatActivity() {

    private lateinit var periodSpinner: Spinner
    private lateinit var modeSpinner: Spinner
    private lateinit var desiredInput: EditText
    private lateinit var surpriseCheckBox: CheckBox
    private lateinit var generateButton: Button
    private lateinit var resetButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var result: TextView

    private val periods = listOf(1, 3, 7, 30)
    private val modes = listOf("variety", "balanced", "batch")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plan)

        periodSpinner = findViewById(R.id.periodSpinner)
        modeSpinner = findViewById(R.id.cookingModeSpinner)
        desiredInput = findViewById(R.id.desiredIngredientsInput)
        surpriseCheckBox = findViewById(R.id.surpriseCheckBox)
        generateButton = findViewById(R.id.generatePlanButton)
        resetButton = findViewById(R.id.resetPlanButton)
        progress = findViewById(R.id.planProgressBar)
        result = findViewById(R.id.planResultText)

        // Подписи, которые видит пользователь.
        periodSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("1 день", "3 дня", "7 дней", "30 дней")
        )

        modeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "Разнообразное меню",
                "Заготовки на 2 дня",
                "Заготовки на 2–3 дня"
            )
        )

        generateButton.setOnClickListener {
            generatePlan()
        }

        resetButton.setOnClickListener {
            clearPlan()
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )
            insets
        }

        // При повторном открытии показываем сохранённый рацион.
        loadCurrentPlan()
    }

    private fun generatePlan() {
        val token = getToken() ?: return

        val desiredProducts = desiredInput.text.toString()
            .split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val request = PlanRequest(
            days = periods[periodSpinner.selectedItemPosition],
            pantry = emptyList(),
            desiredIngredients = desiredProducts,
            surpriseMe = surpriseCheckBox.isChecked,
            cookingMode = modes[modeSpinner.selectedItemPosition]
        )

        showLoading(true)

        ApiClient.service.generatePlan(
            "Bearer $token",
            request
        ).enqueue(object : Callback<PlanResponse> {

            override fun onResponse(
                call: Call<PlanResponse>,
                response: Response<PlanResponse>
            ) {
                showLoading(false)
                val plan = response.body()

                if (response.isSuccessful && plan != null) {
                    showPlan(plan)
                } else {
                    result.text =
                        "Не удалось сформировать рацион. Код: ${response.code()}"
                }
            }

            override fun onFailure(
                call: Call<PlanResponse>,
                error: Throwable
            ) {
                showLoading(false)
                result.text = "Ошибка подключения: ${error.localizedMessage}"
            }
        })
    }

    private fun loadCurrentPlan() {
        val token = getToken() ?: return
        showLoading(true)

        ApiClient.service.getCurrentPlan(
            "Bearer $token"
        ).enqueue(object : Callback<CurrentPlanResponse> {

            override fun onResponse(
                call: Call<CurrentPlanResponse>,
                response: Response<CurrentPlanResponse>
            ) {
                showLoading(false)

                val plan = response.body()?.plan
                if (plan != null) {
                    showPlan(plan)
                } else {
                    result.text = "Сохранённого плана пока нет."
                }
            }

            override fun onFailure(
                call: Call<CurrentPlanResponse>,
                error: Throwable
            ) {
                showLoading(false)
                result.text = "Не удалось загрузить сохранённый план."
            }
        })
    }

    private fun clearPlan() {
        val token = getToken() ?: return
        showLoading(true)

        ApiClient.service.clearCurrentPlan(
            "Bearer $token"
        ).enqueue(object : Callback<StatusResponse> {

            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                showLoading(false)
                result.text = "План сброшен. Можно сформировать новый."
                resetButton.visibility = View.GONE
            }

            override fun onFailure(
                call: Call<StatusResponse>,
                error: Throwable
            ) {
                showLoading(false)
                result.text = "Не удалось сбросить план."
            }
        })
    }

    // Преобразует ответ сервера в понятный пользователю текст.
    private fun showPlan(plan: PlanResponse) {
        // При загрузке сохранённого плана переключатель показывает его период.
        periods.indexOf(plan.periodDays).takeIf { it >= 0 }?.let {
            periodSpinner.setSelection(it)
        }

        val mealNames = mapOf(
            "breakfast" to "Завтрак",
            "lunch" to "Обед",
            "dinner" to "Ужин",
            "snack" to "Перекус"
        )

        val daysText = plan.days.joinToString("\n\n") { day ->
            val mealsText = day.meals.joinToString("\n\n") { meal ->
                val action = if (meal.timeKind == "reheat") {
                    "разогрев"
                } else {
                    "приготовление"
                }

                "${mealNames[meal.meal] ?: meal.meal} — ${meal.recipe}\n" +
                        "${meal.portionWeightGrams} г · " +
                        "${meal.calories} ккал · " +
                        "${meal.estimatedCost} ₽ · " +
                        "${meal.displayMinutes} мин ($action)"
            }

            "${day.date} · ${day.estimatedCost} ₽\n\n$mealsText"
        }

        val shoppingText = if (plan.shoppingList.isEmpty()) {
            "Дополнительные покупки не требуются."
        } else {
            plan.shoppingList.joinToString("\n") {
                "• ${it.product}: ${it.grams} г " +
                        "(${it.packagesToBuy} уп. по ${it.packageGrams} г)"
            }
        }

        result.text =
            "Период плана: ${plan.periodDays} дн.\n" +
                    "Цель: ${plan.targetCalories.toInt()} ккал в день\n" +
                    "Стоимость периода: ${plan.estimatedPeriodCost} ₽\n" +
                    "Прогноз на месяц: ${plan.estimatedMonthlyCost} ₽\n\n" +
                    daysText +
                    "\n\nСписок покупок:\n" +
                    shoppingText

        resetButton.visibility = View.VISIBLE
    }

    private fun showLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        generateButton.isEnabled = !loading
        resetButton.isEnabled = !loading
    }

    private fun getToken(): String? {
        val token = getSharedPreferences(
            "nutriplan_session",
            MODE_PRIVATE
        ).getString("access_token", null)

        if (token == null) {
            result.text = "Сессия истекла. Выполните вход заново."
        }

        return token
    }
}
