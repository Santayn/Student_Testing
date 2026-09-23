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

  revoke(
    refreshToken,
    accessToken,
    tokenType = 'Bearer'
  ) {
    /*
     * Logout invalidates local state before this best-effort revoke.
     * Therefore revoke must use the access token captured from the
     * session being closed instead of the current store/interceptors.
     */
    return authHttp.post(
      '/auth/revoke',
      {
        refreshToken,
      },
      {
        headers: {
          Authorization:
            `${tokenType} ${accessToken}`,
        },
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
      },
      {
        /*
         * A 401 here means an incorrect current password, not necessarily
         * an expired access token. Request preflight already calls
         * ensureAccessToken(), so response refresh/retry must be disabled.
         */
        skipAuthRefresh: true,
      }
    )
  },

  me() {
    return http.get(
      '/auth/me'
    )
  },
}
