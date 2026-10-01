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
  config() {
    return authHttp.get(
      '/auth/config'
    )
  },

  csrf() {
    return authHttp.get(
      '/auth/csrf'
    )
  },

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

  refresh(csrfToken) {
    return authHttp.post(
      '/auth/refresh',
      undefined,
      {
        headers: {
          'X-CSRF-Token': csrfToken,
        },
      }
    )
  },

  revoke(csrfToken) {
    return authHttp.post(
      '/auth/revoke',
      undefined,
      {
        headers: {
          'X-CSRF-Token': csrfToken,
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
      }
    )
  },

  me() {
    return http.get(
      '/auth/me'
    )
  },
}
