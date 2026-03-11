const StudentTestingApi = (() => {
    const API_BASE = "/api/v1";
    const TOKEN_KEY = "student-testing.access-token";
    const USER_KEY = "student-testing.current-user";

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function setCurrentUser(user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user ?? null));
    }

    function getCurrentUserFromStorage() {
        const raw = localStorage.getItem(USER_KEY);
        if (!raw) {
            return null;
        }
        try {
            return JSON.parse(raw);
        } catch (error) {
            return null;
        }
    }

    function buildUrl(path) {
        if (path.startsWith("/api/")) {
            return path;
        }
        if (!path.startsWith("/")) {
            return `${API_BASE}/${path}`;
        }
        return `${API_BASE}${path}`;
    }

    function extractErrorMessage(payload, fallbackMessage) {
        if (!payload) {
            return fallbackMessage;
        }

        if (typeof payload === "string") {
            return payload;
        }

        if (payload.message) {
            return payload.message;
        }

        if (payload.error_description) {
            return payload.error_description;
        }

        if (payload.error) {
            return typeof payload.error === "string" ? payload.error : fallbackMessage;
        }

        return fallbackMessage;
    }

    async function apiFetch(path, options = {}) {
        const token = getToken();
        const headers = new Headers(options.headers || {});

        if (token && !headers.has("Authorization")) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json;charset=UTF-8");
        }

        const response = await fetch(buildUrl(path), {
            ...options,
            headers
        });

        const contentType = response.headers.get("content-type") || "";
        let payload = null;

        if (contentType.includes("application/json")) {
            try {
                payload = await response.json();
            } catch (error) {
                payload = null;
            }
        } else {
            try {
                payload = await response.text();
            } catch (error) {
                payload = null;
            }
        }

        if (!response.ok) {
            const message = extractErrorMessage(payload, `HTTP ${response.status}`);
            if (response.status === 401) {
                clearToken();
            }
            throw new Error(message);
        }

        return payload;
    }

    async function login(login, password) {
        const payload = await apiFetch("/auth/login", {
            method: "POST",
            body: JSON.stringify({ login, password })
        });

        if (!payload || !payload.accessToken) {
            throw new Error("Сервер не вернул access token");
        }

        setToken(payload.accessToken);
        setCurrentUser(payload.user ?? null);

        return payload;
    }

    async function register(form) {
        const payload = await apiFetch("/auth/register", {
            method: "POST",
            body: JSON.stringify(form)
        });

        if (!payload || !payload.accessToken) {
            throw new Error("Сервер не вернул access token");
        }

        setToken(payload.accessToken);
        setCurrentUser(payload.user ?? null);

        return payload;
    }

    async function loadCurrentUser() {
        const user = await apiFetch("/auth/me", {
            method: "GET"
        });
        setCurrentUser(user);
        return user;
    }

    function logout() {
        clearToken();
        window.location.href = "/login";
    }

    function requireAuth() {
        const token = getToken();
        if (!token) {
            const next = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `/login?next=${next}`;
            return false;
        }
        return true;
    }

    function redirectAfterLogin(defaultUrl = "/kubstuTest") {
        const params = new URLSearchParams(window.location.search);
        const next = params.get("next");
        window.location.href = next || defaultUrl;
    }

    function getQueryParam(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = value ?? "";
        return div.innerHTML;
    }

    function studentDisplayName(student) {
        if (!student) {
            return "—";
        }

        if (student.fullName && String(student.fullName).trim()) {
            return student.fullName;
        }

        if (student.name && String(student.name).trim()) {
            return student.name;
        }

        const firstName = student.firstName ?? student.user?.firstName ?? "";
        const lastName = student.lastName ?? student.user?.lastName ?? "";
        const fullName = `${firstName} ${lastName}`.trim();

        if (fullName) {
            return fullName;
        }

        return `Студент #${student.id ?? "?"}`;
    }

    return {
        apiFetch,
        login,
        register,
        loadCurrentUser,
        logout,
        requireAuth,
        redirectAfterLogin,
        getQueryParam,
        getToken,
        getCurrentUserFromStorage,
        escapeHtml,
        studentDisplayName
    };
})();