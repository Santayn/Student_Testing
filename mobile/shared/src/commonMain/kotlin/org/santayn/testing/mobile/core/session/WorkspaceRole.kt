package org.santayn.testing.mobile.core.session

import org.santayn.testing.mobile.data.model.CurrentUser

/**
 * Рабочая роль (frontend/src/utils/workspaceRole.js).
 * Порядок объявления = приоритет: ADMIN > TEACHER > STUDENT.
 */
enum class WorkspaceRole(val apiName: String, val title: String) {
    ADMIN("ADMIN", "Администратор"),
    TEACHER("TEACHER", "Преподаватель"),
    STUDENT("STUDENT", "Студент");

    companion object {
        fun fromApi(name: String?): WorkspaceRole? =
            entries.firstOrNull { it.apiName.equals(name?.trim(), ignoreCase = true) }

        /** Рабочие роли пользователя в порядке приоритета. */
        fun available(roles: Collection<String>): List<WorkspaceRole> {
            val normalized = roles.map { it.trim().uppercase() }.toSet()
            return entries.filter { it.apiName in normalized }
        }

        /** Предпочитаемая роль, если доступна, иначе роль с наивысшим приоритетом. */
        fun resolve(roles: Collection<String>, preferred: String?): WorkspaceRole? {
            val available = available(roles)
            return available.firstOrNull { it.apiName == preferred?.uppercase() } ?: available.firstOrNull()
        }
    }
}

/**
 * Доступ к рабочим разделам (frontend/src/utils/accountAccess.js):
 * нужны и привязанная персона, и хотя бы одна рабочая роль.
 */
fun CurrentUser.hasWorkspaceAccess(): Boolean =
    personId != null && WorkspaceRole.available(roles).isNotEmpty()

enum class PendingReason { NO_ROLE, NO_PERSON, NO_ROLE_AND_PERSON }

fun CurrentUser.pendingReason(): PendingReason? {
    val noRole = WorkspaceRole.available(roles).isEmpty()
    val noPerson = personId == null
    return when {
        noRole && noPerson -> PendingReason.NO_ROLE_AND_PERSON
        noRole -> PendingReason.NO_ROLE
        noPerson -> PendingReason.NO_PERSON
        else -> null
    }
}
