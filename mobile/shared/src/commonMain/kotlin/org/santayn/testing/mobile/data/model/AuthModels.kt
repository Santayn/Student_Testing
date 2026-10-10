package org.santayn.testing.mobile.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** UserRegisterService.AuthTokens. Даты — ISO-8601 UTC строки. */
@Serializable
data class AuthTokens(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val accessTokenExpiresAtUtc: String,
    val refreshToken: String,
    val refreshTokenExpiresAtUtc: String,
    val lifetimeKind: Int = LIFETIME_STANDARD,
) {
    companion object {
        /** access 15 мин, refresh 7 дней. */
        const val LIFETIME_STANDARD = 1

        /** access 120 мин, refresh 30 дней («Запомнить меня»). */
        const val LIFETIME_EXTENDED = 2
    }
}

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
    val lifetimeKind: Int? = null,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

/** UserRegisterService.CurrentUser (GET /auth/me). */
@Serializable
data class CurrentUser(
    val userId: Int,
    val login: String,
    @JsonNames("active")
    val isActive: Boolean = true,
    val personId: Int? = null,
    val fullName: String? = null,
    val person: PersonDto? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() }
            ?: person?.fullName?.takeIf { it.isNotBlank() }
            ?: login
}
