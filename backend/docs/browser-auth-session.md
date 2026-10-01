# Browser authentication session contract

## Goal

The browser must not receive or persist the refresh token in JavaScript-accessible storage.
The access token remains short-lived and is kept only in application memory.

## Token transport

- `POST /api/v1/auth/login` and `POST /api/v1/auth/register` return the access token and non-secret session metadata in JSON.
- The refresh token is excluded from JSON and is written to the configured `HttpOnly` cookie.
- `POST /api/v1/auth/refresh` reads the refresh token only from that cookie, rotates the refresh token, and writes the replacement cookie.
- `POST /api/v1/auth/revoke` reads and revokes the cookie-based refresh session and clears both refresh and CSRF cookies.
- `POST /api/v1/auth/change-password` invalidates backend sessions and clears the browser session cookies.

Default cookie properties:

```text
Name:     student_test_refresh
Path:     /api/v1/auth
HttpOnly: true
Secure:   true (production default)
SameSite: Lax (configurable)
```

The local Spring profile explicitly disables `Secure` so an HTTP-only local development environment can work. Production deployment must use HTTPS when `Secure=true`.

## CSRF model

The API continues to use bearer access tokens for ordinary application requests. Those requests do not rely on ambient cookies for authorization, so Spring's global form-CSRF mechanism remains disabled for the stateless API.

The two commands that consume the ambient refresh cookie (`refresh` and `revoke`) use an explicit double-submit token:

1. Browser calls `GET /api/v1/auth/csrf`.
2. Backend returns the same random value in the JSON response and the non-HttpOnly `student_test_csrf` cookie.
3. Browser sends that value in `X-CSRF-Token` when calling refresh/revoke.
4. Backend compares the header and cookie in constant time before reading the refresh cookie.

The CSRF endpoint does not expose the refresh token. Cross-origin browser access is additionally limited by the configured CORS origin allow-list.

## CORS

Credentialed requests are enabled because the browser must send the refresh cookie. Only configured `app.cors.allowed-origins` / `APP_CORS_ALLOWED_ORIGINS` patterns are accepted. Do not configure a wildcard origin for an internet-facing deployment when credentials are enabled.

## Frontend storage

Pinia persistence stores only non-secret session metadata (`refreshTokenExpiresAtUtc`, lifetime kind, and user read model). It does not persist:

- refresh token;
- access token;
- CSRF token.

After a page reload the frontend always attempts one refresh against the HttpOnly cookie, even if JavaScript persistence metadata is missing or stale. The cookie/backend is the source of truth; `refreshTokenExpiresAtUtc` is only non-secret UI/session metadata and must never be used to decide that no refresh cookie exists.

## Configuration

```text
APP_AUTH_REFRESH_COOKIE_NAME
APP_AUTH_CSRF_COOKIE_NAME
APP_AUTH_REFRESH_COOKIE_SECURE
APP_AUTH_REFRESH_COOKIE_SAME_SITE
APP_CORS_ALLOWED_ORIGINS
```

For a cross-site frontend/API deployment, choose the `SameSite` value according to the deployment topology; `SameSite=None` requires `Secure=true` and HTTPS.
