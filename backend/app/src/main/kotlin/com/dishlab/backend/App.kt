package com.dishlab.backend

import com.dishlab.api.configureApi
import com.dishlab.application.service.CurrentUserResolver
import com.dishlab.application.service.IdentityService
import com.dishlab.application.service.InMemoryIngredientRepository
import com.dishlab.application.service.InMemoryRecipeRepository
import com.dishlab.application.service.InMemoryUserAccountRepository
import com.dishlab.application.service.RecipeRepository
import com.dishlab.infrastructure.ai.OpenRouterProductNameNormalizer
import com.dishlab.infrastructure.ai.OpenRouterRecipeIngredientValidator
import com.dishlab.infrastructure.ai.OpenRouterRecipeValidationProvider
import com.dishlab.infrastructure.ai.OpenRouterTagCategorizationProvider
import com.dishlab.infrastructure.ai.OpenRouterTagValidator
import com.dishlab.infrastructure.db.DatabaseFactory
import com.dishlab.infrastructure.db.PostgresRecipeRepository
import com.dishlab.infrastructure.db.PostgresIngredientRepository
import com.dishlab.infrastructure.db.PostgresFoodTaxonomyRepository
import com.dishlab.infrastructure.db.PostgresProductCatalogRepository
import com.dishlab.infrastructure.db.PostgresUserAccountRepository
import com.dishlab.infrastructure.db.SqliteRecipeCatalogRepository
import com.dishlab.infrastructure.catalog.OpenFoodFactsProductCatalogProvider
import com.dishlab.infrastructure.catalog.IngredientNameCatalog
import com.dishlab.infrastructure.catalog.CatalogProductNameNormalizer
import com.dishlab.application.service.IngredientService
import com.dishlab.infrastructure.firebase.FirebaseInitializer
import com.dishlab.application.service.RecipeService
import com.dishlab.application.service.RecipeCatalogService
import com.dishlab.application.service.RecipeCatalogRepository
import com.dishlab.application.service.ProductCatalogService
import com.dishlab.application.service.ProductCanonicalizationService
import com.dishlab.application.service.InMemoryFoodTaxonomyRepository
import com.dishlab.application.service.InMemoryProductCatalogRepository
import com.dishlab.application.service.UnitConversionService
import com.dishlab.infrastructure.firebase.DevFirebaseAuthVerifier
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifierImpl
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.nio.file.Path

private val fileEnv: Map<String, String> by lazy {
    val startDir = java.io.File(System.getProperty("user.dir"))
    val envFile = generateSequence(startDir) { it.parentFile }
        .take(4)
        .map { java.io.File(it, ".env") }
        .firstOrNull { it.exists() }
    envFile?.readLines()
        ?.filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }
        ?.associate { line ->
            val idx = line.indexOf('=')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        } ?: emptyMap()
}

fun env(key: String): String? = System.getenv(key) ?: fileEnv[key]

private fun resolvePath(value: String): Path {
    val configured = Path.of(value)
    if (configured.isAbsolute) return configured
    val startDir = java.io.File(System.getProperty("user.dir"))
    return generateSequence(startDir) { it.parentFile }
        .take(4)
        .map { it.toPath().resolve(configured).normalize() }
        .firstOrNull { it.toFile().exists() }
        ?: configured.toAbsolutePath()
}

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}


fun Application.module() {
    val devAuth = env("DEV_AUTH") == "true"
    if (!devAuth) FirebaseInitializer.init()
    appModule(if (devAuth) DevFirebaseAuthVerifier() else FirebaseAuthVerifierImpl())
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.appModule(
    authVerifier: FirebaseAuthVerifier,
    testRecipeCatalogRepository: RecipeCatalogRepository? = null,
    testRecipeRepository: RecipeRepository? = null,
) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            },
        )
    }

    val dbDataSource = env("DATABASE_URL")?.trim()?.takeIf(String::isNotEmpty)?.let { dbUrl ->
        DatabaseFactory.create(
            url = dbUrl,
            user = env("DATABASE_USER") ?: "dishlab",
            password = env("DATABASE_PASSWORD") ?: "dishlab",
        )
    }
    val userRepository = dbDataSource
        ?.let { PostgresUserAccountRepository(it) }
        ?: InMemoryUserAccountRepository()
    val currentUserResolver = CurrentUserResolver(userRepository)
    val identityService = IdentityService(
        currentUserResolver = currentUserResolver,
        users = userRepository,
    )
    val ingredientRepository = dbDataSource
        ?.let { PostgresIngredientRepository(it) }
        ?: InMemoryIngredientRepository()
    val ingredientService = IngredientService(
        currentUserResolver = currentUserResolver,
        ingredients = ingredientRepository,
    )
    val unitConversionService = UnitConversionService()
    val openRouterApiKey = env("OPENROUTER_API_KEY")?.trim()?.takeIf(String::isNotBlank)
    val recipeRepo = testRecipeRepository
        ?: dbDataSource
            ?.let { PostgresRecipeRepository(it) }
        ?: InMemoryRecipeRepository()
    val recipeCatalogService = testRecipeCatalogRepository?.let(::RecipeCatalogService)
        ?: env("RECIPE_CATALOG_DB")
        ?.let(::resolvePath)
        ?.takeIf { it.toFile().isFile }
        ?.let(::SqliteRecipeCatalogRepository)
        ?.let(::RecipeCatalogService)
    val ingredientNameCatalog = env("RECIPE_CATALOG_DB")
        ?.let(::resolvePath)
        ?.takeIf { it.toFile().isFile }
        ?.let { IngredientNameCatalog.fromDb("jdbc:sqlite:${it.toAbsolutePath()}") }
    val ingredientValidationModel = env("OPENROUTER_INGREDIENT_VALIDATION_MODEL")
    val recipeIngredientValidator = if (openRouterApiKey != null && ingredientNameCatalog != null && ingredientValidationModel != null) {
        OpenRouterRecipeIngredientValidator(
            apiKey = openRouterApiKey,
            catalog = ingredientNameCatalog,
            model = ingredientValidationModel,
        )
    } else {
        com.dishlab.application.service.NoOpRecipeIngredientValidationProvider()
    }
    val recipeValidationModel = env("OPENROUTER_RECIPE_VALIDATION_MODEL")
    val recipeValidationProvider = if (openRouterApiKey != null && recipeValidationModel != null) {
        OpenRouterRecipeValidationProvider(apiKey = openRouterApiKey, model = recipeValidationModel)
    } else {
        com.dishlab.application.service.NoOpRecipeValidationProvider()
    }
    val tagValidationModel = env("OPENROUTER_TAG_VALIDATION_MODEL")
    val tagValidator = if (openRouterApiKey != null && tagValidationModel != null) {
        OpenRouterTagValidator(apiKey = openRouterApiKey, model = tagValidationModel)
    } else {
        null
    }
    val tagCategorizationModel = env("OPENROUTER_TAG_CATEGORIZATION_MODEL")
    val tagCategorizationProvider = if (openRouterApiKey != null && tagCategorizationModel != null) {
        OpenRouterTagCategorizationProvider(apiKey = openRouterApiKey, model = tagCategorizationModel)
    } else {
        null
    }
    val recipeService = RecipeService(
        currentUserResolver = currentUserResolver,
        recipes = recipeRepo,
        ingredientValidationProvider = recipeIngredientValidator,
        ingredientRepository = ingredientRepository,
        validationProvider = recipeValidationProvider,
        ingredientCatalog = ingredientNameCatalog,
        tagValidator = tagValidator,
        tagCategorizationProvider = tagCategorizationProvider,
    )
    val productModel = env("OPENROUTER_PRODUCT_MODEL")
    val productPreset = env("OPENROUTER_PRODUCT_PRESET")
    val productNameNormalizer = if (openRouterApiKey != null && ingredientNameCatalog != null && productModel != null) {
        OpenRouterProductNameNormalizer(
            apiKey = openRouterApiKey,
            catalog = ingredientNameCatalog,
            model = productModel,
            preset = productPreset ?: "",
        )
    } else if (ingredientNameCatalog != null) {
        CatalogProductNameNormalizer(ingredientNameCatalog)
    } else {
        com.dishlab.application.service.FallbackProductNameNormalizer()
    }
    val foodTaxonomyRepository = dbDataSource
        ?.let(::PostgresFoodTaxonomyRepository)
        ?: InMemoryFoodTaxonomyRepository()
    val productRepository = dbDataSource
        ?.let(::PostgresProductCatalogRepository)
        ?: InMemoryProductCatalogRepository()
    val productCatalogService = ProductCatalogService(
        provider = OpenFoodFactsProductCatalogProvider(),
        products = productRepository,
        canonicalizer = ProductCanonicalizationService(productNameNormalizer, foodTaxonomyRepository),
        tagCatalog = ingredientNameCatalog,
        tagValidator = tagValidator,
        tagCategorizationProvider = tagCategorizationProvider,
    )
    configureApi(
        authVerifier = authVerifier,
        identityService = identityService,
        currentUserResolver = currentUserResolver,
        ingredientService = ingredientService,
        unitConversionService = unitConversionService,
        recipeService = recipeService,
        recipeCatalogService = recipeCatalogService,
        productCatalogService = productCatalogService,
    )
}
