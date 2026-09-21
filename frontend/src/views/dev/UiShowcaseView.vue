<script setup>
import {
  nextTick,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  useThemeStore,
} from '@/stores/theme'

const themeStore = useThemeStore()
const tokenValues = ref({})

const paletteGroups = [
  {
    title: 'Brand',
    description: 'Основной акцент и его состояния.',
    tokens: [
      { token: '--st-primary', label: 'Primary' },
      { token: '--st-primary-hover', label: 'Primary hover' },
      { token: '--st-primary-soft', label: 'Primary soft' },
      { token: '--st-primary-soft-text', label: 'Primary soft text' },
    ],
  },
  {
    title: 'Workspace',
    description: 'Основные поверхности рабочей области.',
    tokens: [
      { token: '--st-page-bg', label: 'Page background' },
      { token: '--st-surface', label: 'Surface' },
      { token: '--st-surface-muted', label: 'Surface muted' },
      { token: '--st-border', label: 'Border' },
    ],
  },
  {
    title: 'Typography',
    description: 'Иерархия основного и вторичного текста.',
    tokens: [
      { token: '--st-text', label: 'Text' },
      { token: '--st-text-secondary', label: 'Text secondary' },
      { token: '--st-text-muted', label: 'Text muted' },
    ],
  },
  {
    title: 'Application shell',
    description: 'Палитра навигационного каркаса приложения.',
    tokens: [
      { token: '--st-shell-bg', label: 'Shell background' },
      { token: '--st-shell-elevated', label: 'Shell elevated' },
      { token: '--st-shell-hover', label: 'Shell hover' },
      { token: '--st-shell-border', label: 'Shell border' },
      { token: '--st-shell-text', label: 'Shell text' },
      { token: '--st-shell-muted', label: 'Shell muted' },
    ],
  },
  {
    title: 'Semantic',
    description: 'Статусы и их мягкие фоновые варианты.',
    tokens: [
      { token: '--st-success', label: 'Success' },
      { token: '--st-success-soft', label: 'Success soft' },
      { token: '--st-warning', label: 'Warning' },
      { token: '--st-warning-soft', label: 'Warning soft' },
      { token: '--st-danger', label: 'Danger' },
      { token: '--st-danger-soft', label: 'Danger soft' },
      { token: '--st-info', label: 'Info' },
      { token: '--st-info-soft', label: 'Info soft' },
    ],
  },
  {
    title: 'Overlay',
    description: 'Цвет затемнения под модальными слоями.',
    tokens: [
      { token: '--st-overlay-backdrop', label: 'Overlay backdrop', checker: true },
    ],
  },
]

const semanticPairs = [
  {
    label: 'Success',
    foreground: '--st-success',
    background: '--st-success-soft',
  },
  {
    label: 'Warning',
    foreground: '--st-warning',
    background: '--st-warning-soft',
  },
  {
    label: 'Danger',
    foreground: '--st-danger',
    background: '--st-danger-soft',
  },
  {
    label: 'Info',
    foreground: '--st-info',
    background: '--st-info-soft',
  },
]

function normalizeColorValue(value) {
  return String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ')
}

function readTokenValues() {
  if (typeof document === 'undefined') {
    return
  }

  const styles = getComputedStyle(document.documentElement)
  const nextValues = {}

  for (const group of paletteGroups) {
    for (const item of group.tokens) {
      nextValues[item.token] = normalizeColorValue(
        styles.getPropertyValue(item.token)
      )
    }
  }

  tokenValues.value = nextValues
}

async function setTheme(theme) {
  themeStore.setTheme(theme)
  await nextTick()
  readTokenValues()
}

watch(
  () => themeStore.resolvedTheme,
  async () => {
    await nextTick()
    readTokenValues()
  }
)

onMounted(() => {
  readTokenValues()
})
</script>

<template>
  <main class="palette-page">
    <header class="palette-page__header">
      <div>
        <p class="palette-page__eyebrow">
          Student Testing · color laboratory
        </p>
        <h1>Цветовая палитра</h1>
        <p class="palette-page__lead">
          На этой странице проверяются только цветовые токены.
          Компоненты интерфейса намеренно исключены.
        </p>
      </div>

      <div class="theme-switch" aria-label="Выбор темы">
        <button
          type="button"
          class="theme-switch__button"
          :class="{ 'is-active': themeStore.theme === 'light' }"
          @click="setTheme('light')"
        >
          Светлая
        </button>
        <button
          type="button"
          class="theme-switch__button"
          :class="{ 'is-active': themeStore.theme === 'dark' }"
          @click="setTheme('dark')"
        >
          Тёмная
        </button>
        <button
          type="button"
          class="theme-switch__button"
          :class="{ 'is-active': themeStore.theme === 'system' }"
          @click="setTheme('system')"
        >
          Системная
        </button>
      </div>
    </header>

    <section class="palette-summary" aria-label="Текущая тема">
      <div class="palette-summary__item">
        <span class="palette-summary__label">Режим</span>
        <strong>{{ themeStore.theme }}</strong>
      </div>
      <div class="palette-summary__item">
        <span class="palette-summary__label">Фактически</span>
        <strong>{{ themeStore.resolvedTheme }}</strong>
      </div>
      <div class="palette-summary__item palette-summary__item--grow">
        <span class="palette-summary__label">Фон страницы</span>
        <code>{{ tokenValues['--st-page-bg'] }}</code>
      </div>
      <div class="palette-summary__item palette-summary__item--grow">
        <span class="palette-summary__label">Основной текст</span>
        <code>{{ tokenValues['--st-text'] }}</code>
      </div>
    </section>

    <section class="palette-section palette-section--contrast">
      <div class="palette-section__heading">
        <div>
          <p class="palette-section__kicker">Контраст</p>
          <h2>Основные сочетания</h2>
        </div>
        <p>
          Быстрая проверка текста на главных поверхностях темы.
        </p>
      </div>

      <div class="contrast-grid">
        <article class="contrast-card contrast-card--page">
          <span class="contrast-card__label">Page</span>
          <strong>Основной текст</strong>
          <p>Вторичный текст на фоне рабочей области.</p>
          <small>Приглушённый текст</small>
        </article>

        <article class="contrast-card contrast-card--surface">
          <span class="contrast-card__label">Surface</span>
          <strong>Основной текст</strong>
          <p>Вторичный текст на основной поверхности.</p>
          <small>Приглушённый текст</small>
        </article>

        <article class="contrast-card contrast-card--muted">
          <span class="contrast-card__label">Muted surface</span>
          <strong>Основной текст</strong>
          <p>Вторичный текст на вспомогательной поверхности.</p>
          <small>Приглушённый текст</small>
        </article>

        <article class="contrast-card contrast-card--shell">
          <span class="contrast-card__label">Shell</span>
          <strong>Основной текст</strong>
          <p>Приглушённый текст навигационного каркаса.</p>
          <small>Граница shell показана по контуру.</small>
        </article>
      </div>
    </section>

    <section
      v-for="group in paletteGroups"
      :key="group.title"
      class="palette-section"
    >
      <div class="palette-section__heading">
        <div>
          <p class="palette-section__kicker">Tokens</p>
          <h2>{{ group.title }}</h2>
        </div>
        <p>{{ group.description }}</p>
      </div>

      <div class="swatch-grid">
        <article
          v-for="item in group.tokens"
          :key="item.token"
          class="swatch-card"
        >
          <div
            class="swatch-card__color"
            :class="{ 'swatch-card__color--checker': item.checker }"
          >
            <span
              class="swatch-card__fill"
              :style="{ backgroundColor: `var(${item.token})` }"
            />
          </div>

          <div class="swatch-card__meta">
            <div>
              <strong>{{ item.label }}</strong>
              <code>{{ item.token }}</code>
            </div>
            <span class="swatch-card__value">
              {{ tokenValues[item.token] || '—' }}
            </span>
          </div>
        </article>
      </div>
    </section>

    <section class="palette-section">
      <div class="palette-section__heading">
        <div>
          <p class="palette-section__kicker">Semantic pairs</p>
          <h2>Семантические сочетания</h2>
        </div>
        <p>
          Основной статусный цвет поверх соответствующего soft-фона.
        </p>
      </div>

      <div class="semantic-grid">
        <article
          v-for="pair in semanticPairs"
          :key="pair.label"
          class="semantic-card"
          :style="{
            backgroundColor: `var(${pair.background})`,
            color: `var(${pair.foreground})`,
          }"
        >
          <strong>{{ pair.label }}</strong>
          <span>{{ tokenValues[pair.foreground] }}</span>
          <code>{{ tokenValues[pair.background] }}</code>
        </article>
      </div>
    </section>

    <section class="palette-section">
      <div class="palette-section__heading">
        <div>
          <p class="palette-section__kicker">Interaction</p>
          <h2>Акцентные состояния</h2>
        </div>
        <p>
          Только цвета normal / hover / soft без привязки к конкретному UI-компоненту.
        </p>
      </div>

      <div class="interaction-strip">
        <div class="interaction-chip interaction-chip--primary">
          <span>Primary</span>
          <code>{{ tokenValues['--st-primary'] }}</code>
        </div>
        <div class="interaction-chip interaction-chip--hover">
          <span>Hover</span>
          <code>{{ tokenValues['--st-primary-hover'] }}</code>
        </div>
        <div class="interaction-chip interaction-chip--soft">
          <span>Soft</span>
          <code>{{ tokenValues['--st-primary-soft'] }}</code>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.palette-page {
  min-height: 100%;
  padding: 32px;
  background: var(--st-page-bg);
  color: var(--st-text);
  font-family: var(--st-font-sans);
}

.palette-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  max-width: 1280px;
  margin: 0 auto 24px;
}

.palette-page__eyebrow,
.palette-section__kicker {
  margin: 0 0 8px;
  color: var(--st-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.palette-page h1,
.palette-page h2,
.palette-page p {
  margin-top: 0;
}

.palette-page h1 {
  margin-bottom: 10px;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.08;
}

.palette-page__lead {
  max-width: 700px;
  margin-bottom: 0;
  color: var(--st-text-secondary);
  line-height: 1.65;
}

.theme-switch {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--st-border);
  border-radius: 10px;
  background: var(--st-surface);
  box-shadow: var(--st-shadow-card);
}

.theme-switch__button {
  min-height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--st-text-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.theme-switch__button:hover {
  background: var(--st-surface-muted);
  color: var(--st-text);
}

.theme-switch__button:focus-visible {
  outline: none;
  box-shadow: var(--st-focus-shadow);
}

.theme-switch__button.is-active {
  background: var(--st-primary-soft);
  color: var(--st-primary-soft-text);
}

.palette-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1px;
  max-width: 1280px;
  margin: 0 auto 24px;
  overflow: hidden;
  border: 1px solid var(--st-border);
  border-radius: 12px;
  background: var(--st-border);
}

.palette-summary__item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 140px;
  padding: 14px 16px;
  background: var(--st-surface);
}

.palette-summary__item--grow {
  flex: 1 1 220px;
}

.palette-summary__label {
  color: var(--st-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.palette-summary code,
.swatch-card code,
.semantic-card code,
.interaction-chip code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.palette-section {
  max-width: 1280px;
  margin: 0 auto 24px;
  padding: 24px;
  border: 1px solid var(--st-border);
  border-radius: 14px;
  background: var(--st-surface);
  box-shadow: var(--st-shadow-card);
}

.palette-section__heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.palette-section__heading h2 {
  margin-bottom: 0;
  font-size: 20px;
}

.palette-section__heading > p {
  max-width: 520px;
  margin-bottom: 0;
  color: var(--st-text-secondary);
  font-size: 14px;
  line-height: 1.55;
  text-align: right;
}

.contrast-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.contrast-card {
  min-height: 160px;
  padding: 18px;
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.contrast-card strong,
.contrast-card p,
.contrast-card small {
  display: block;
}

.contrast-card strong {
  margin: 22px 0 8px;
}

.contrast-card p {
  margin-bottom: 8px;
  color: var(--st-text-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.contrast-card small {
  color: var(--st-text-muted);
}

.contrast-card__label {
  display: inline-block;
  color: var(--st-text-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.contrast-card--page {
  background: var(--st-page-bg);
}

.contrast-card--surface {
  background: var(--st-surface);
}

.contrast-card--muted {
  background: var(--st-surface-muted);
}

.contrast-card--shell {
  border-color: var(--st-shell-border);
  background: var(--st-shell-bg);
  color: var(--st-shell-text);
}

.contrast-card--shell .contrast-card__label,
.contrast-card--shell small,
.contrast-card--shell p {
  color: var(--st-shell-muted);
}

.swatch-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.swatch-card {
  overflow: hidden;
  border: 1px solid var(--st-border);
  border-radius: 10px;
  background: var(--st-surface);
}

.swatch-card__color {
  position: relative;
  height: 96px;
  overflow: hidden;
  background: var(--st-surface-muted);
}

.swatch-card__color--checker {
  background-color: var(--st-surface);
  background-image:
    linear-gradient(45deg, var(--st-border) 25%, transparent 25%),
    linear-gradient(-45deg, var(--st-border) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, var(--st-border) 75%),
    linear-gradient(-45deg, transparent 75%, var(--st-border) 75%);
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-size: 16px 16px;
}

.swatch-card__fill {
  position: absolute;
  inset: 0;
}

.swatch-card__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
}

.swatch-card__meta strong,
.swatch-card__meta code {
  display: block;
}

.swatch-card__meta strong {
  margin-bottom: 5px;
  font-size: 14px;
}

.swatch-card__meta code {
  color: var(--st-text-muted);
  font-size: 11px;
}

.swatch-card__value {
  max-width: 50%;
  color: var(--st-text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.4;
  text-align: right;
  word-break: break-word;
}

.semantic-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.semantic-card {
  min-height: 116px;
  padding: 18px;
  border: 1px solid currentColor;
  border-radius: 10px;
}

.semantic-card strong,
.semantic-card span,
.semantic-card code {
  display: block;
}

.semantic-card strong {
  margin-bottom: 20px;
  font-size: 15px;
}

.semantic-card span,
.semantic-card code {
  font-size: 11px;
  opacity: 0.82;
}

.semantic-card code {
  margin-top: 4px;
}

.interaction-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border-radius: 10px;
}

.interaction-chip {
  display: flex;
  min-height: 104px;
  padding: 18px;
  flex-direction: column;
  justify-content: space-between;
  color: #ffffff;
}

.interaction-chip span {
  font-weight: 800;
}

.interaction-chip code {
  font-size: 12px;
  opacity: 0.82;
}

.interaction-chip--primary {
  background: var(--st-primary);
}

.interaction-chip--hover {
  background: var(--st-primary-hover);
}

.interaction-chip--soft {
  background: var(--st-primary-soft);
  color: var(--st-primary-soft-text);
}

@media (max-width: 980px) {
  .palette-page__header,
  .palette-section__heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .palette-section__heading > p {
    text-align: left;
  }

  .contrast-grid,
  .swatch-grid,
  .semantic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .palette-page {
    padding: 18px;
  }

  .theme-switch {
    width: 100%;
  }

  .theme-switch__button {
    flex: 1;
  }

  .palette-section {
    padding: 18px;
  }

  .contrast-grid,
  .swatch-grid,
  .semantic-grid,
  .interaction-strip {
    grid-template-columns: 1fr;
  }

  .swatch-card__meta {
    flex-direction: column;
  }

  .swatch-card__value {
    max-width: 100%;
    text-align: left;
  }
}
</style>
