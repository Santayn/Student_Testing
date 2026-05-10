const StudentTestingApi = (() => {
    const API_BASE = "/api/v1";
    const TOKEN_KEY = "student-testing.access-token";
    const USER_KEY = "student-testing.current-user";
    const SIDEBAR_ITEMS = [
        { id: "profile", label: "О пользователе", href: "/profile.html", aliases: ["/profile"], profileButton: true },
        { id: "main", label: "Главная", href: "/main.html", aliases: ["/", "/index.html", "/kubstuTest"] },
        { id: "subjects", label: "Предметы", href: "/subjects.html", aliases: ["/kubstuTest/subjects"], roles: ["STUDENT", "TEACHER"] },
        { id: "instructions", label: "Инструкция", href: "/instructions.html" },
        { id: "about", label: "О платформе", href: "/about.html", aliases: ["/kubstuTest/about"] },
        { id: "manage-students", label: "Управление студентами", href: "/manage_students.html", aliases: ["/kubstuTest/manage-students"], roles: ["TEACHER"] },
        { id: "teaching-load", label: "Персональная нагрузка", href: "/manage-teachers.html", aliases: ["/kubstuTest/manage-teachers"], roles: ["TEACHER"] },
        { id: "users", label: "Управление ролями", href: "/users.html", roles: ["ADMIN"] },
        { id: "teacher-groups-admin", label: "Шаблоны нагрузки", href: "/manage-teacher-groups.html", aliases: ["/kubstuTest/manage-teachers-groups"], roles: ["ADMIN"] },
        { id: "teacher-subjects", label: "Преподаватели и предметы", href: "/manage-teachers-subject.html", aliases: ["/kubstuTest/manage-teachers-subject"], roles: ["ADMIN"] },
        { id: "lectures", label: "Лекции", href: "/manage-lectures-subject.html", aliases: ["/kubstuTest/manage-lectures-subject"], roles: ["TEACHER"] },
        { id: "topic-library", label: "Тематики", href: "/manage-topic-library.html", aliases: ["/kubstuTest/manage-topic-library"], roles: ["TEACHER"] },
        { id: "questions", label: "Вопросы", href: "/questions-page.html", roles: ["TEACHER"] },
        { id: "create-test", label: "Создать тест", href: "/create-test.html", aliases: ["/kubstuTest/tests/create-test"], roles: ["TEACHER"] },
        { id: "results", label: "Результаты тестов", href: "/result-list.html", aliases: ["/kubstuTest/results"], roles: ["STUDENT", "TEACHER"] },
        { id: "groups", label: "Группы", href: "/groups.html", roles: ["ADMIN"] },
        { id: "faculties", label: "Факультеты", href: "/create-faculty.html", roles: ["ADMIN"] },
        { id: "subject-faculty", label: "Предметы факультетов", href: "/manage-subject-faculty.html", roles: ["ADMIN"] },
        { id: "create-subject", label: "Создание предмета", href: "/create-subject.html", roles: ["ADMIN"] }
    ];
    const AUTH_PATHS = new Set(["/login", "/login.html", "/register", "/register.html"]);

    function getToken() {
        if (window.__studentTestingPreviewToken) {
            return window.__studentTestingPreviewToken;
        }
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        if (window.__studentTestingPreviewMode) {
            window.__studentTestingPreviewToken = token;
            return;
        }
        localStorage.setItem(TOKEN_KEY, token);
    }

    function clearToken() {
        if (window.__studentTestingPreviewMode) {
            window.__studentTestingPreviewToken = null;
            window.__studentTestingPreviewUser = null;
            applySidebarState(null);
            return;
        }
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        applySidebarState(null);
    }

    function setCurrentUser(user) {
        if (window.__studentTestingPreviewMode) {
            window.__studentTestingPreviewUser = user ?? null;
            applySidebarState(user);
            return;
        }
        localStorage.setItem(USER_KEY, JSON.stringify(user ?? null));
        applySidebarState(user);
    }

    function getCurrentUserFromStorage() {
        if (window.__studentTestingPreviewUser) {
            return window.__studentTestingPreviewUser;
        }
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

        if (payload.detail) {
            return payload.detail;
        }

        if (payload.title) {
            return payload.title;
        }

        if (payload.errors && typeof payload.errors === "object") {
            const firstKey = Object.keys(payload.errors)[0];
            const firstError = firstKey ? payload.errors[firstKey] : null;
            if (Array.isArray(firstError) && firstError.length) {
                return firstError[0];
            }
            if (typeof firstError === "string") {
                return firstError;
            }
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
            throw new Error("Ответ не содержит access token");
        }

        setToken(payload.accessToken);
        try {
            await loadCurrentUser();
        } catch (error) {
            setCurrentUser(payload.user ?? null);
        }

        return payload;
    }

    async function register(form) {
        const payload = await apiFetch("/auth/register", {
            method: "POST",
            body: JSON.stringify(form)
        });

        if (!payload || !payload.accessToken) {
            throw new Error("Ответ не содержит access token");
        }

        setToken(payload.accessToken);
        try {
            await loadCurrentUser();
        } catch (error) {
            setCurrentUser(payload.user ?? null);
        }

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

    function normalizePath(path) {
        const pathname = new URL(path, window.location.origin).pathname;
        return pathname.length > 1 && pathname.endsWith("/") ? pathname.slice(0, -1) : pathname;
    }

    function normalizeRoleNames(user) {
        const values = [];

        function add(value) {
            if (!value) {
                return;
            }
            if (Array.isArray(value)) {
                value.forEach(add);
                return;
            }
            if (typeof value === "object") {
                add(value.name ?? value.role ?? value.roleName ?? value.authority);
                return;
            }
            values.push(String(value).toUpperCase());
        }

        add(user?.roles);
        add(user?.role);
        add(user?.authorities);

        return values;
    }

    function itemVisibleForRoles(item, roles) {
        if (!Array.isArray(item.roles) || !item.roles.length) {
            return true;
        }
        if (!roles.length) {
            return false;
        }
        return item.roles.some(requiredRole =>
            roles.some(role => role.includes(String(requiredRole).toUpperCase()))
        );
    }

    function getSidebarItemById(id) {
        return SIDEBAR_ITEMS.find(item => item.id === id) || null;
    }

    function shouldInstallSidebar() {
        if (!document.body) {
            return false;
        }
        if (window.__studentTestingSkipSidebar || document.body.dataset.skipSidebar === "true") {
            return false;
        }
        const currentPath = normalizePath(window.location.pathname);
        return !AUTH_PATHS.has(currentPath) && !document.body.classList.contains("auth-page");
    }

    function populateSidebar(sidebar) {
        sidebar.replaceChildren();

        const title = document.createElement("h3");
        title.textContent = "Меню";
        sidebar.appendChild(title);

        SIDEBAR_ITEMS.forEach(item => {
            const link = document.createElement("a");
            link.href = item.href;
            link.textContent = item.label;
            if (item.id) {
                link.dataset.sidebarItemId = item.id;
            }
            if (Array.isArray(item.roles) && item.roles.length) {
                link.dataset.sidebarRoles = item.roles.join(",");
            }
            sidebar.appendChild(link);
        });

        const logoutButton = document.createElement("button");
        logoutButton.type = "button";
        logoutButton.dataset.sidebarLogout = "";
        logoutButton.textContent = "Выйти";
        const hasLogoutButtonOutsideSidebar = [...document.querySelectorAll("#logoutBtn")]
            .some(button => !sidebar.contains(button));
        if (!hasLogoutButtonOutsideSidebar) {
            logoutButton.id = "logoutBtn";
        }
        sidebar.appendChild(logoutButton);

        sidebar.dataset.sidebarManaged = "true";
    }

    function createSidebarElement() {
        const sidebar = document.createElement("aside");
        sidebar.className = "sidebar";
        populateSidebar(sidebar);
        return sidebar;
    }

    function installSidebarIfMissing() {
        if (document.querySelector(".sidebar") || !shouldInstallSidebar()) {
            return;
        }

        const body = document.body;
        const sidebar = createSidebarElement();
        const content = document.createElement("div");
        content.className = "content";
        content.dataset.generatedContent = "true";

        while (body.firstChild) {
            const node = body.firstChild;
            if (node.nodeType === Node.ELEMENT_NODE && ["SCRIPT", "NOSCRIPT"].includes(node.tagName)) {
                break;
            }
            content.appendChild(node);
        }

        body.classList.add("app-shell");
        body.insertBefore(sidebar, body.firstChild);
        body.insertBefore(content, sidebar.nextSibling);
    }

    function applySidebarState(user = getCurrentUserFromStorage()) {
        const roles = normalizeRoleNames(user);
        const hasRoleData = roles.length > 0;

        document.querySelectorAll(".sidebar [data-sidebar-item-id]").forEach(element => {
            const item = getSidebarItemById(element.dataset.sidebarItemId);
            if (!item) {
                return;
            }

            if (!Array.isArray(item.roles) || !item.roles.length) {
                element.classList.remove("hidden");
                return;
            }

            if (!hasRoleData) {
                element.classList.add("hidden");
                return;
            }

            element.classList.toggle("hidden", !itemVisibleForRoles(item, roles));
        });

        document.querySelectorAll("#logoutBtn, [data-sidebar-logout]").forEach(button => {
            button.classList.toggle("hidden", !getToken());
        });
    }

    async function refreshSidebarState(force = false) {
        const storedUser = getCurrentUserFromStorage();
        const hasRoleData = normalizeRoleNames(storedUser).length > 0;

        if (!getToken() || window.__studentTestingSidebarUserLoading) {
            return;
        }

        if (!force && hasRoleData) {
            applySidebarState(storedUser);
            return;
        }

        window.__studentTestingSidebarUserLoading = true;
        try {
            applySidebarState(await loadCurrentUser());
        } catch (error) {
            applySidebarState(null);
        } finally {
            window.__studentTestingSidebarUserLoading = false;
        }
    }

    function renderSidebar() {
        installSidebarIfMissing();
        const sidebars = [...document.querySelectorAll(".sidebar")];
        if (!sidebars.length) {
            return;
        }

        const currentPath = normalizePath(window.location.pathname);
        const items = SIDEBAR_ITEMS;

        sidebars.forEach(sidebar => {
            if (sidebar.dataset.sidebarManaged !== "true") {
                populateSidebar(sidebar);
            }
        });

        sidebars.forEach(sidebar => {
            sidebar.querySelectorAll("a, button").forEach(item => {
                item.classList.remove("active");
                item.removeAttribute("aria-current");
            });

            items.forEach(item => {
                const paths = [item.href, ...(item.aliases || [])].map(normalizePath);
                const active = paths.includes(currentPath);
                const link = sidebar.querySelector(`a[href="${item.href}"]`);

                if (item.profileButton && active) {
                    const existingButton = sidebar.querySelector("#toggleUserInfoBtn");
                    if (existingButton) {
                        existingButton.classList.add("active");
                        existingButton.setAttribute("aria-current", "page");
                        return;
                    }
                    if (!link) {
                        return;
                    }
                    const button = document.createElement("button");
                    button.type = "button";
                    button.id = "toggleUserInfoBtn";
                    button.textContent = link.textContent;
                    button.className = link.className;
                    link.replaceWith(button);
                    button.classList.add("active");
                    button.setAttribute("aria-current", "page");
                    return;
                }

                if (active) {
                    link?.classList.add("active");
                    link?.setAttribute("aria-current", "page");
                }
            });

            document.querySelectorAll("#logoutBtn, [data-sidebar-logout]").forEach(button => {
                if (button.dataset.sidebarLogoutBound === "true") {
                    return;
                }
                button.dataset.sidebarLogoutBound = "true";
                button.addEventListener("click", logout);
            });
        });

        if (!getToken()) {
            applySidebarState(null);
            return;
        }

        if (!window.__studentTestingSidebarRefreshRequested) {
            window.__studentTestingSidebarRefreshRequested = true;
            applySidebarState(null);
            refreshSidebarState(true);
            return;
        }

        applySidebarState(getCurrentUserFromStorage());
    }

    function installFetchInterceptor() {
        if (!window.fetch || window.__studentTestingFetchPatched) {
            return;
        }

        const nativeFetch = window.fetch.bind(window);
        window.__studentTestingFetchPatched = true;

        window.fetch = (input, init = {}) => {
            const rawUrl = typeof input === "string" || input instanceof URL ? String(input) : input.url;
            const url = new URL(rawUrl, window.location.origin);

            if (!url.pathname.startsWith("/api/")) {
                return nativeFetch(input, init);
            }

            const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
            const token = getToken();

            if (token && !headers.has("Authorization")) {
                headers.set("Authorization", `Bearer ${token}`);
            }

            if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
                headers.set("Content-Type", "application/json;charset=UTF-8");
            }

            return nativeFetch(input, {
                ...init,
                headers
            });
        };
    }

    installFetchInterceptor();
    renderSidebar();
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", renderSidebar, { once: true });
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
        normalizeRoleNames,
        applySidebarState,
        renderSidebar,
        escapeHtml,
        studentDisplayName
    };
})();
