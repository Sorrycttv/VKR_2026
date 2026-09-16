package ru.pershin.nutriplan

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.http.DELETE

// Учётные данные для регистрации и входа.
data class RegisterRequest(
    val email: String,
    val password: String
)

// Параметры пользователя, на основании которых рассчитывается рацион.
data class ProfileRequest(
    val sex: String,
    val age: Int,
    @SerializedName("height_cm")
    val heightCm: Double,
    @SerializedName("weight_kg")
    val weightKg: Double,
    val activity: String,
    val goal: String,
    val diet: String,
    val restrictions: List<String>,
    @SerializedName("preferred_foods")
    val preferredFoods: List<String>,
    @SerializedName("target_calories")
    val targetCalories: Double? = null,
    @SerializedName("monthly_budget")
    val monthlyBudget: Double?
)

data class ProfileResponse(
    val profile: ProfileRequest?
)

data class StatusResponse(
    val status: String
)

// Ответ сервера после успешной регистрации или входа.
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

data class RecipeIngredient(
    val product: String,
    val grams: Double
)

data class RecipeItem(
    val name: String,
    val calories: Int = 0,
    val ingredients: List<RecipeIngredient>,
    @SerializedName("preparation_minutes")
    val preparationMinutes: Int,
    val instructions: String,
    @SerializedName("portion_weight_grams")
    val portionWeightGrams: Int
)

data class RecipesResponse(
    val recipes: List<RecipeItem>
)

// Один продукт, который пользователь указал в домашнем запасе.
data class PantryItemRequest(
    val product: String,
    val grams: Double
)

data class FridgeResponse(
    val items: List<PantryItemRequest>
)

data class MissingIngredient(
    val product: String,
    val grams: Int
)

data class FridgeRecipe(
    val name: String,
    val calories: Int,
    @SerializedName("portion_weight_grams")
    val portionWeightGrams: Int,
    val missing: List<MissingIngredient>
)

data class FridgeRecommendationsResponse(
    @SerializedName("can_cook")
    val canCook: List<FridgeRecipe>,
    @SerializedName("need_to_buy")
    val needToBuy: List<FridgeRecipe>
)

// генерация рациона
data class PlanRequest(
    val days: Int,
    val pantry: List<PantryItemRequest> = emptyList(),

    @SerializedName("desired_ingredients")
    val desiredIngredients: List<String> = emptyList(),

    @SerializedName("surprise_me")
    val surpriseMe: Boolean,

    @SerializedName("cooking_mode")
    val cookingMode: String
)

// Одно блюдо
data class PlanMeal(
    val meal: String,
    val recipe: String,

    @SerializedName("portion_weight_grams")
    val portionWeightGrams: Int,

    val calories: Int,

    @SerializedName("estimated_cost")
    val estimatedCost: Int,

    @SerializedName("display_minutes")
    val displayMinutes: Int,

    @SerializedName("time_kind")
    val timeKind: String
)

// меню 1 дня
data class PlanDay(
    val date: String,
    val meals: List<PlanMeal>,

    @SerializedName("estimated_cost")
    val estimatedCost: Int
)

// список покупок
data class ShoppingItem(
    val product: String,
    val grams: Int,

    @SerializedName("packages_to_buy")
    val packagesToBuy: Int,

    @SerializedName("package_grams")
    val packageGrams: Int
)

//ответ сервера
data class PlanResponse(
    val currency: String = "RUB",

    @SerializedName("period_days")
    val periodDays: Int,

    @SerializedName("target_calories")
    val targetCalories: Double,

    val days: List<PlanDay>,

    @SerializedName("shopping_list")
    val shoppingList: List<ShoppingItem>,

    @SerializedName("estimated_period_cost")
    val estimatedPeriodCost: Int,

    @SerializedName("estimated_monthly_cost")
    val estimatedMonthlyCost: Int
)

data class CurrentPlanResponse(
    val plan: PlanResponse?
)

// вопрос пользователя ИИ-коучу
data class CoachHistoryItem(
    val role: String,
    val content: String
)

data class CoachRequest(
    val message: String,
    val history: List<CoachHistoryItem> = emptyList()
)

// Ответ ИИ
data class CoachResponse(
    val answer: String,

    // true — ответила языковая модель
    // false — сервер вернул резервное объяснение
    val available: Boolean,

    // Варианты следующих вопросов.
    val suggestions: List<String>
)

interface ApiService {
    // Регистрация
    @POST("api/v1/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<AuthResponse>

    // Вход
    @POST("api/v1/auth/login")
    fun login(
        @Body request: RegisterRequest
    ): Call<AuthResponse>

    @GET("api/v1/recipes")
    fun getRecipes(
        @Header("Authorization") authorization: String,
        @Query("q") query: String? = null
    ): Call<RecipesResponse>

    @GET("api/v1/fridge")
    fun getFridge(
        @Header("Authorization") authorization: String
    ): Call<FridgeResponse>

    @PUT("api/v1/fridge")
    fun saveFridge(
        @Header("Authorization") authorization: String,
        @Body items: List<PantryItemRequest>
    ): Call<StatusResponse>

    @GET("api/v1/fridge/recommendations")
    fun getFridgeRecommendations(
        @Header("Authorization") authorization: String
    ): Call<FridgeRecommendationsResponse>

    // вопрос ИИ-коучу
    @POST("api/v1/coach/chat")
    fun sendCoachMessage(
        @Header("Authorization") authorization: String,
        @Body request: CoachRequest
    ): Call<CoachResponse>

    @GET("api/v1/profile")
    fun getProfile(
        @Header("Authorization") authorization: String
    ): Call<ProfileResponse>

    // Сохранение параметров рациона
    @PUT("api/v1/profile")
    fun saveProfile(
        @Header("Authorization")
        authorization: String,
        @Body
        request: ProfileRequest

    ): Call<StatusResponse>

    @POST("api/v1/plans/generate")
    fun generatePlan(
        @Header("Authorization") authorization: String,
        @Body request: PlanRequest
    ): Call<PlanResponse>

    @GET("api/v1/plans/current")
    fun getCurrentPlan(
        @Header("Authorization") authorization: String
    ): Call<CurrentPlanResponse>

    @DELETE("api/v1/plans/current")
    fun clearCurrentPlan(
        @Header("Authorization") authorization: String
    ): Call<StatusResponse>
}

// Единая точка доступа к серверному API.
object ApiClient {
    private const val BASE_URL =
        "http://176.125.193.183:28081/"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    val service: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
