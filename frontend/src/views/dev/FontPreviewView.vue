<script setup>
import {
  nextTick,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  UiButton,
  UiInput,
  UiTag,
} from '@/components/ui'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const fontAvailability = ref({})
const sampleText = ref(
  'Методы программирования и структуры данных'
)

const fontFamilies = [
  {
    id: 'inter',
    name: 'Inter',
    note: 'Текущий целевой UI-шрифт проекта.',
    family:
      'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    check: 'Inter',
  },
  {
    id: 'ibm-plex',
    name: 'IBM Plex Sans',
    note: 'Более академичный и технический характер.',
    family:
      '"IBM Plex Sans", ui-sans-serif, system-ui, sans-serif',
    check: 'IBM Plex Sans',
  },
  {
    id: 'manrope',
    name: 'Manrope',
    note: 'Более мягкий и современный продуктовый характер.',
    family:
      'Manrope, ui-sans-serif, system-ui, sans-serif',
    check: 'Manrope',
  },
  {
    id: 'system',
    name: 'System UI',
    note: 'Текущий fallback без отдельного web-font.',
    family:
      'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    check: null,
  },
]

const weightSamples = [
  { weight: 400, label: '400 · Regular' },
  { weight: 500, label: '500 · Medium' },
  { weight: 600, label: '600 · Semibold' },
  { weight: 700, label: '700 · Bold' },
]

function ensurePreviewFonts() {
  if (typeof document === 'undefined') {
    return
  }

  const id = 'student-testing-font-preview-fonts'

  if (document.getElementById(id)) {
    return
  }

  const preconnectGoogle = document.createElement('link')
  preconnectGoogle.rel = 'preconnect'
  preconnectGoogle.href = 'https://fonts.googleapis.com'

  const preconnectStatic = document.createElement('link')
  preconnectStatic.rel = 'preconnect'
  preconnectStatic.href = 'https://fonts.gstatic.com'
  preconnectStatic.crossOrigin = 'anonymous'

  const stylesheet = document.createElement('link')
  stylesheet.id = id
  stylesheet.rel = 'stylesheet'
  stylesheet.href =
    'https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=Inter:wght@400;500;600;700&family=Manrope:wght@400;500;600;700&display=swap'

  document.head.append(
    preconnectGoogle,
    preconnectStatic,
    stylesheet
  )
}

async function readFontAvailability() {
  if (
    typeof document === 'undefined' ||
    !document.fonts
  ) {
    return
  }

  await document.fonts.ready

  const next = {}

  for (const font of fontFamilies) {
    next[font.id] = font.check
      ? document.fonts.check(`16px "${font.check}"`)
      : true
  }

  fontAvailability.value = next
}

async function setTheme(theme) {
  themeStore.setTheme(theme)
  await nextTick()
}

watch(
  () => themeStore.resolvedTheme,
  () => readFontAvailability()
)

onMounted(async () => {
  ensurePreviewFonts()
  await readFontAvailability()

  window.setTimeout(
    readFontAvailability,
    800
  )
})
</script>

<template>
  <main class="font-preview-page">
    <header class="font-preview-hero">
      <div class="font-preview-hero__copy">
        <p class="font-preview-eyebrow">
          Student Testing · typography laboratory
        </p>

        <h1>Сравнение шрифтов</h1>

        <p class="font-preview-lead">
          Все варианты показаны на одинаковых элементах интерфейса,
          размерах и весах. Эта страница доступна только в DEV-сборке.
        </p>
      </div>

      <div class="font-preview-theme" aria-label="Выбор темы">
        <UiButton
          size="sm"
          :variant="themeStore.theme === 'light' ? 'primary' : 'secondary'"
          @click="setTheme('light')"
        >
          Светлая
        </UiButton>

        <UiButton
          size="sm"
          :variant="themeStore.theme === 'dark' ? 'primary' : 'secondary'"
          @click="setTheme('dark')"
        >
          Тёмная
        </UiButton>

        <UiButton
          size="sm"
          :variant="themeStore.theme === 'system' ? 'primary' : 'secondary'"
          @click="setTheme('system')"
        >
          Системная
        </UiButton>
      </div>
    </header>

    <section class="font-preview-controls" aria-labelledby="preview-text-title">
      <div>
        <p class="font-preview-eyebrow">Образец</p>
        <h2 id="preview-text-title">Свой текст</h2>
        <p>
          Измените строку — она обновится во всех четырёх вариантах.
        </p>
      </div>

      <UiInput
        v-model="sampleText"
        label="Текст для сравнения"
        placeholder="Введите русский текст"
      />
    </section>

    <section class="font-preview-grid" aria-label="Варианты шрифтов">
      <article
        v-for="font in fontFamilies"
        :key="font.id"
        class="font-specimen"
        :style="{ fontFamily: font.family }"
      >
        <header class="font-specimen__header">
          <div>
            <p class="font-preview-eyebrow">{{ font.id }}</p>
            <h2>{{ font.name }}</h2>
            <p>{{ font.note }}</p>
          </div>

          <UiTag
            :variant="fontAvailability[font.id] === false ? 'warning' : 'success'"
            :value="fontAvailability[font.id] === false ? 'Fallback' : 'Loaded'"
          />
        </header>

        <section class="font-specimen__hero">
          <p class="font-specimen__kicker">Предмет</p>
          <h3>{{ sampleText || 'Методы программирования и структуры данных' }}</h3>
          <p>
            Лекция 4 · Алгоритмы поиска и сортировки. Проверьте читаемость
            кириллицы, плотность строки и различимость знаков препинания.
          </p>
        </section>

        <section class="font-weight-list" aria-label="Начертания">
          <p
            v-for="item in weightSamples"
            :key="item.weight"
            class="font-weight-row"
            :style="{ fontWeight: item.weight }"
          >
            <span>{{ item.label }}</span>
            <span>Съешь ещё этих мягких французских булок</span>
          </p>
        </section>

        <section class="font-ui-card">
          <div class="font-ui-card__heading">
            <div>
              <span class="font-ui-card__label">Тестирование</span>
              <h3>Контрольная работа № 2</h3>
            </div>

            <span class="font-ui-badge">Активен</span>
          </div>

          <p class="font-ui-card__body">
            Отвечено 18 из 25 вопросов. Осталось 07:42. Разрешено попыток: 3.
          </p>

          <div class="font-ui-stats">
            <div>
              <span>Результат</span>
              <strong>87%</strong>
            </div>
            <div>
              <span>Баллы</span>
              <strong>43 / 50</strong>
            </div>
            <div>
              <span>Попытка</span>
              <strong>2 из 3</strong>
            </div>
          </div>

          <label class="font-ui-field">
            <span>Название теста</span>
            <input
              type="text"
              value="Основы объектно-ориентированного программирования"
              readonly
            >
          </label>

          <div class="font-ui-actions">
            <button type="button" class="font-ui-button font-ui-button--secondary">
              Отмена
            </button>
            <button type="button" class="font-ui-button font-ui-button--primary">
              Сохранить изменения
            </button>
          </div>
        </section>

        <section class="font-glyphs">
          <p class="font-preview-eyebrow">Glyph check</p>
          <p>АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ</p>
          <p>абвгдеёжзийклмнопрстуфхцчшщъыьэюя</p>
          <p>0123456789 · 10% · 24/30 · № 17 · @ # ₽ € $</p>
          <p>Il1 | O0 | 5S | 2Z | {} [] () / \\ → ←</p>
        </section>
      </article>
    </section>

    <section class="font-preview-note">
      <strong>Как сравнивать</strong>
      <p>
        Смотрите прежде всего на 12–14 px текст, русские буквы,
        цифры, плотность кнопок и различие весов 500/600/700.
        Если рядом с шрифтом показан «Fallback», браузер не загрузил
        web-font и сравнение для него недостоверно.
      </p>
    </section>
  </main>
</template>

<style scoped>
.font-preview-page {
  min-height: 100vh;
  padding: clamp(18px, 3vw, 36px);

  display: grid;
  gap: 24px;

  color: var(--st-text);
  background: var(--st-page-bg);

  font-family: var(--st-font-sans);
}

.font-preview-hero,
.font-preview-controls,
.font-preview-note {
  max-width: var(--st-content-width);
  width: 100%;
  margin-inline: auto;
}

.font-preview-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.font-preview-hero__copy {
  max-width: 760px;
}

.font-preview-hero h1,
.font-preview-controls h2,
.font-specimen h2,
.font-specimen h3,
.font-preview-page p {
  margin-top: 0;
}

.font-preview-hero h1 {
  margin-bottom: 10px;
  font-size: var(--st-font-page-title);
  line-height: var(--st-line-tight);
}

.font-preview-lead,
.font-preview-controls p,
.font-specimen__header p,
.font-specimen__hero p,
.font-preview-note p {
  color: var(--st-text-secondary);
  line-height: var(--st-line-relaxed);
}

.font-preview-eyebrow {
  margin-bottom: 6px;
  color: var(--st-primary);
  font-size: var(--st-font-xs);
  font-weight: 600;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.font-preview-theme {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.font-preview-controls {
  padding: 18px;

  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(280px, 1.2fr);
  gap: 20px;
  align-items: end;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.font-preview-controls h2 {
  margin-bottom: 6px;
  font-size: var(--st-font-xl);
}

.font-preview-controls p {
  margin-bottom: 0;
  font-size: var(--st-font-sm);
}

.font-preview-grid {
  max-width: var(--st-content-width);
  width: 100%;
  margin-inline: auto;

  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.font-specimen {
  min-width: 0;
  padding: 20px;

  display: grid;
  align-content: start;
  gap: 18px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.font-specimen__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.font-specimen__header h2 {
  margin-bottom: 4px;
  font-size: 1.375rem;
  line-height: 1.2;
  font-weight: 700;
}

.font-specimen__header p {
  margin-bottom: 0;
  font-size: var(--st-font-sm);
}

.font-specimen__hero {
  padding: 18px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-md);
}

.font-specimen__kicker,
.font-ui-card__label {
  margin-bottom: 6px;
  color: var(--st-text-muted);
  font-size: var(--st-font-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.font-specimen__hero h3 {
  margin-bottom: 9px;
  font-size: 1.5rem;
  line-height: 1.2;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.font-specimen__hero p {
  margin-bottom: 0;
  font-size: var(--st-font-md);
}

.font-weight-list {
  display: grid;
  gap: 4px;
}

.font-weight-row {
  margin-bottom: 0;
  padding: 8px 0;

  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px;

  border-bottom: 1px solid var(--st-border);

  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
}

.font-weight-row:last-child {
  border-bottom: 0;
}

.font-weight-row span:first-child {
  color: var(--st-text-muted);
  font-size: var(--st-font-xs);
  font-weight: 500;
}

.font-ui-card {
  padding: 16px;
  display: grid;
  gap: 14px;

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-md);
}

.font-ui-card__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.font-ui-card__heading h3 {
  margin-bottom: 0;
  font-size: var(--st-font-lg);
  line-height: var(--st-line-tight);
  font-weight: 600;
}

.font-ui-badge {
  padding: 4px 8px;
  color: var(--st-success-text);
  background: var(--st-success-soft);
  border-radius: 999px;
  font-size: var(--st-font-xs);
  font-weight: 600;
}

.font-ui-card__body {
  margin-bottom: 0;
  color: var(--st-text-secondary);
  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
}

.font-ui-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.font-ui-stats > div {
  padding: 10px;
  display: grid;
  gap: 3px;
  background: var(--st-surface-muted);
  border-radius: var(--st-radius-control);
}

.font-ui-stats span {
  color: var(--st-text-muted);
  font-size: var(--st-font-xs);
}

.font-ui-stats strong {
  font-size: var(--st-font-lg);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.font-ui-field {
  display: grid;
  gap: 6px;
  font-size: var(--st-font-sm);
  font-weight: 500;
}

.font-ui-field input {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;

  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
  outline: none;

  font: inherit;
  font-weight: 400;
}

.font-ui-field input:focus {
  border-color: var(--st-primary);
  box-shadow: var(--st-focus-shadow);
}

.font-ui-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.font-ui-button {
  min-height: 40px;
  padding: 0 14px;

  border: 1px solid transparent;
  border-radius: var(--st-radius-control);
  cursor: pointer;

  font: inherit;
  font-size: var(--st-font-sm);
  font-weight: 600;
}

.font-ui-button--primary {
  color: var(--st-on-primary);
  background: var(--st-primary);
}

.font-ui-button--secondary {
  color: var(--st-text);
  background: var(--st-surface);
  border-color: var(--st-border);
}

.font-glyphs {
  padding-top: 2px;
  display: grid;
  gap: 5px;
}

.font-glyphs p {
  margin-bottom: 0;
  overflow-wrap: anywhere;
  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
}

.font-glyphs p:not(.font-preview-eyebrow) {
  font-variant-numeric: tabular-nums;
}

.font-preview-note {
  padding: 16px 18px;
  background: var(--st-info-soft);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
}

.font-preview-note strong {
  color: var(--st-info-text);
}

.font-preview-note p {
  margin: 5px 0 0;
  font-size: var(--st-font-sm);
}

@media (max-width: 900px) {
  .font-preview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .font-preview-hero,
  .font-preview-controls {
    grid-template-columns: 1fr;
  }

  .font-preview-hero {
    flex-direction: column;
  }

  .font-preview-theme,
  .font-preview-theme > * {
    width: 100%;
  }

  .font-ui-stats {
    grid-template-columns: 1fr;
  }

  .font-weight-row {
    grid-template-columns: 1fr;
    gap: 3px;
  }
}

@media (max-width: 480px) {
  .font-preview-page {
    padding: 14px;
    gap: 14px;
  }

  .font-specimen,
  .font-preview-controls {
    padding: 15px;
  }

  .font-ui-actions,
  .font-ui-actions > * {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .font-preview-page *,
  .font-preview-page *::before,
  .font-preview-page *::after {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
