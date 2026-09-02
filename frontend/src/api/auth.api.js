import http, {
  authHttp,
} from './http'

function withOptionalLifetimeKind(
  data
) {
  const payload = {
    ...data,
  }

  if (
    payload.lifetimeKind ===
      undefined ||
    payload.lifetimeKind ===
      null ||
    payload.lifetimeKind === ''
  ) {
    delete payload.lifetimeKind
  }

  return payload
}

export const authApi = {
  login({
    login,
    password,
    lifetimeKind,
  }) {
    return authHttp.post(
      '/auth/login',
      withOptionalLifetimeKind({
        login,
        password,
        lifetimeKind,
      })
    )
  },

  register({
    login,
    password,
    lifetimeKind,
  }) {
    return authHttp.post(
      '/auth/register',
      withOptionalLifetimeKind({
        login,
        password,
        lifetimeKind,
      })
    )
  },

  refresh(refreshToken) {
    return authHttp.post(
      '/auth/refresh',
      {
        refreshToken,
      }
    )
  },

  revoke(refreshToken) {
    return http.post(
      '/auth/revoke',
      {
        refreshToken,
      },
      {
        /*
         * Нельзя делать response-refresh + retry:
         * refresh rotation заменит refreshToken,
         * а body исходного revoke содержит старое значение.
         */
        skipAuthRefresh: true,
      }
    )
  },

  changePassword({
    currentPassword,
    newPassword,
  }) {
    return http.post(
      '/auth/change-password',
      {
        currentPassword,
        newPassword,
      }
    )
  },

  me() {
    return http.get(
      '/auth/me'
    )
  },
}
