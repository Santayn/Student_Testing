# UI development

## Публичный UI API

`frontend/src/components/ui` — канонический слой UI.

Business code должен импортировать:

```javascript
import { UiButton, UiInput, UiSelect } from '@/components/ui'
```

а не PrimeVue напрямую.

## Зачем wrapper

Он унифицирует theme, размеры, accessibility defaults, touch targets, disabled/error state и даёт возможность менять underlying UI library.

## Новый UI component

```text
components/ui/UiSomething.vue
→ components/ui/index.js
→ README/showcase при необходимости
→ UI tests
```

## Accessibility

Проверить keyboard, focus, label/ARIA, touch target, dark/light contrast, mobile layout и reduced motion для animation.

## CSS

Использовать существующие tokens/layout patterns, не создавать локальную параллельную дизайн-систему.

## Static quality

Production code контролирует `debugger`, `console.log/debug`, `v-html`, `innerHTML`, `eval`, `new Function` и внутренние imports.
