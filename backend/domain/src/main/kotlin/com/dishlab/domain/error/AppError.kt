package com.dishlab.domain.error

open class AppError(
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap(),
    val httpStatusCode: Int = 400,
) : RuntimeException(message)

class UnauthorizedError(
    message: String = "Потрібна авторизація",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "UNAUTHORIZED",
    message = message,
    details = details,
    httpStatusCode = 401,
)

class ValidationError(
    message: String = "Перевірте поля запиту",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "VALIDATION_ERROR",
    message = message,
    details = details,
    httpStatusCode = 400,
)

class ForbiddenError(
    message: String = "Недостатньо прав для цієї дії",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "FORBIDDEN",
    message = message,
    details = details,
    httpStatusCode = 403,
)

class NotFoundError(
    message: String = "Ресурс не знайдено",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "NOT_FOUND",
    message = message,
    details = details,
    httpStatusCode = 404,
)

class ConflictError(
    message: String = "Запит конфліктує з поточним станом",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "CONFLICT",
    message = message,
    details = details,
    httpStatusCode = 409,
)

class DensityRequiredError(
    message: String = "Для конвертації між обʼємом і масою потрібна щільність інгредієнта",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "DENSITY_REQUIRED",
    message = message,
    details = details,
    httpStatusCode = 400,
)

class ConversionImpossibleError(
    message: String = "Неможливо конвертувати між цими одиницями",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "CONVERSION_IMPOSSIBLE",
    message = message,
    details = details,
    httpStatusCode = 400,
)

class PublishValidationError(
    message: String = "Рецепт має містити щонайменше один інгредієнт і один крок перед публікацією",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "PUBLISH_VALIDATION_ERROR",
    message = message,
    details = details,
    httpStatusCode = 400,
)


class IngredientValidationUnavailableError(
    message: String = "AI-перевірка інгредієнтів тимчасово недоступна. Спробуйте ще раз пізніше",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "INGREDIENT_VALIDATION_UNAVAILABLE",
    message = message,
    details = details,
    httpStatusCode = 503,
)

class MediaFileTooLargeError(
    message: String = "Файл перевищує дозволений розмір",
    details: Map<String, String> = emptyMap(),
) : AppError(
    code = "MEDIA_FILE_TOO_LARGE",
    message = message,
    details = details,
    httpStatusCode = 400,
)
