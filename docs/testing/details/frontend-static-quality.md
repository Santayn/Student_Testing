# Frontend static quality

`frontend/scripts/static-quality.mjs` — отдельный проектный quality gate поверх ESLint.

## Проверяемые запрещённые конструкции

Скрипт ищет в production JS/Vue:

- `debugger;`;
- `console.log(...)` и `console.debug(...)`;
- `v-html`;
- присваивание `.innerHTML`;
- `eval(...)`;
- `new Function(...)`.

## Проверка внутренних imports

Для относительных и `@/` imports скрипт проверяет, что production target реально существует. Допускаются отдельные asset extensions.

## Reachability graph

Строится ориентированный import graph от:

```text
src/main.js
```

Все production `.js` и `.vue` модули должны быть достижимы от entrypoint. Недостижимый production module считается ошибкой.

## Экспортируемые символы

Для именованных `function`, `const` и `class` exports выполняется простая corpus-проверка использования. Если имя встречается только в месте объявления, export помечается как потенциально неиспользуемый.

## Ограничения проверки

Скрипт использует lexical/regex анализ, а не полноценный JavaScript AST. Поэтому он является проектным guardrail, но не заменяет ESLint/type system/static analyzer.

## Проверенный результат

Для текущего audited snapshot:

```text
Static quality check passed: 201 production JS/Vue modules are reachable and clean.
```
