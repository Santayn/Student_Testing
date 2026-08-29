# Theme refactor

Единая система тем для всех уже созданных Vue-компонентов и страниц.

## Новое

```text
src/assets/theme.css
src/stores/theme.js
```

`ThemeStore` отвечает только за выбор темы (`system`, `light`, `dark`) и записывает фактически применённую тему в `data-theme` у `<html>`.

Все цвета находятся только в `theme.css`. Компоненты используют семантические токены вида:

```css
background: var(--surface);
color: var(--text);
border-color: var(--border);
```

Fallback-цветы вроде `var(--surface, #fff)` из компонентов удалены.

## Обновлены

```text
App.vue
AppHeader.vue
AppSidebar.vue
AppFooter.vue
AuthLayout.vue
LoginView.vue
RegisterView.vue
RequireAuthView.vue
NotFoundView.vue
ForbiddenView.vue
HomeView.vue
ProfileView.vue
main.js
```

## main.js

Глобальная тема подключается один раз:

```js
import '@/assets/theme.css'
```

После установки Pinia вызывается:

```js
const themeStore = useThemeStore()
themeStore.init()
```

## Header

Только Header импортирует ThemeStore, потому что именно там находится кнопка переключения темы. Остальным компонентам ThemeStore не нужен: они автоматически меняют оформление через CSS variables.

## Новые страницы

Для новых компонентов не пишите цвета напрямую. Используйте токены из `theme.css`. Если появляется новый семантический цвет, сначала добавьте токен для light/dark в `theme.css`, а уже затем используйте его в компоненте.
