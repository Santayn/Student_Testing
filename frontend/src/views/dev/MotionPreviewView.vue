<script setup>
import { ref } from 'vue'
import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiDisclosure,
  UiField,
  UiLoadingState,
  UiMotion,
  UiTable,
  UiToastHost,
  UiSelect,
  useUiToast,
} from '@/components/ui'
import { MOTION_PRESETS, STANDARD_MOTION_PRESETS } from '@/config/motionPresets'
import { SURFACE_PRESETS, SHINE_PRESETS, SHINE_INTENSITIES } from '@/config/glassPresets'
import { useMotionPreferences } from '@/composables/motion/useMotionPreferences'

const glassSurface = ref('glass')
const shinePreset = ref('sweep')
const shineIntensity = ref('normal')
const buttonMotion = ref('press')
const cardMotion = ref('lift')
const dialogMotion = ref('scale-fade')
const selectMotion = ref('focused')
const selectedExample = ref('one')
const transitionMotion = ref('fade-slide')
const tableMotion = ref('row-highlight')
const fieldMotion = ref('error-reveal')
const loadingMotion = ref('soft-pulse')
const toastMotion = ref('fade-slide')
const uiToast = useUiToast()
const disclosureMotion = ref('expand')
const exampleError = ref(false)
const exampleRows = [
  { id: 1, name: 'Алгоритмы', score: 85 },
  { id: 2, name: 'Базы данных', score: 92 },
]
const exampleColumns = [
  { key: 'name', label: 'Предмет', sortable: true },
  { key: 'score', label: 'Баллы', sortable: true },
]
const showNotice = ref(true)
const dialogVisible = ref(false)
const { prefersReducedMotion } = useMotionPreferences()

function optionsFor(surface) {
  return MOTION_PRESETS[surface].map((value) => ({ label: value, value }))
}
</script>

<template>
  <section class="motion-preview">
    <h1>Motion API — проверка пресетов</h1>
    <p>
      Базовые компоненты получили стандартные эффекты из Motion API.
      Примеры ниже позволяют сравнить автоматический эффект с отключённым.
    </p>
    <UiAlert
      variant="info"
      :message="prefersReducedMotion
        ? 'Система запросила сокращение анимаций.'
        : 'Системное сокращение анимаций выключено.'"
      motion="none"
    />

    <UiCard
      title="Adaptive Glass — светлый / тёмный блик"
      description="Однократный наклонный блик слева направо при наведении, без слежения за курсором."
      compact
    >
      <div class="motion-preview__glass-controls">
        <UiSelect
          v-model="glassSurface"
          label="Материал"
          :options="SURFACE_PRESETS.map((value) => ({ label: value, value }))"
        />
        <UiSelect
          v-model="shinePreset"
          label="Блик"
          :options="SHINE_PRESETS.map((value) => ({ label: value, value }))"
        />
        <UiSelect
          v-model="shineIntensity"
          label="Интенсивность"
          :options="SHINE_INTENSITIES.map((value) => ({ label: value, value }))"
        />
      </div>
      <div class="motion-preview__glass-samples">
        <UiCard
          title="Стеклянная карточка"
          description="Подведи указатель, чтобы увидеть один проход диагонального света."
          :surface="glassSurface"
          :shine="shinePreset"
          :shine-intensity="shineIntensity"
          motion="lift"
        >
          Внешний вид автоматически зависит от текущей темы приложения.
        </UiCard>
        <UiButton
          label="Сохранить изменения"
          variant="primary"
          :surface="glassSurface"
          :shine="shinePreset"
          :shine-intensity="shineIntensity"
          motion="press"
        />
      </div>
    </UiCard>

    <UiCard
      title="Стандартные эффекты — этап 14"
      description="Карта находится в config/motionPresets.js; стекло по умолчанию не включается."
      compact
    >
      <div class="motion-preview__defaults">
        <div>
          <strong>Кнопка: {{ STANDARD_MOTION_PRESETS.button }}</strong>
          <div class="motion-preview__inline">
            <UiButton label="Стандартная" variant="primary" />
            <UiButton label="Без Motion API" variant="secondary" motion="none" />
          </div>
        </div>
        <div>
          <strong>Карточка: {{ STANDARD_MOTION_PRESETS.card }}</strong>
          <UiCard title="Рабочая карточка">
            <UiSelect
              v-model="selectedExample"
              label="Сфокусируй поле"
              :options="[{ label: 'Первый', value: 'one' }, { label: 'Второй', value: 'two' }]"
            />
          </UiCard>
        </div>
        <div>
          <strong>Уведомление: {{ STANDARD_MOTION_PRESETS.alert }}</strong>
          <UiAlert message="Встроенный эффект появления" variant="success" />
        </div>
      </div>
    </UiCard>

    <div class="motion-preview__grid">
      <UiCard title="Кнопка" description="Hover и нажатие" compact>
        <UiSelect
          v-model="buttonMotion"
          label="Пресет кнопки"
          :options="optionsFor('button')"
        />
        <div class="motion-preview__sample">
          <UiButton label="Сохранить" variant="primary" :motion="buttonMotion" />
        </div>
      </UiCard>

      <UiCard title="Карточка" description="Подъём при наведении" compact>
        <UiSelect
          v-model="cardMotion"
          label="Пресет карточки"
          :options="optionsFor('card')"
        />
        <UiCard title="Пример предмета" description="Алгоритмы" :motion="cardMotion">
          Содержимое карточки остаётся обычным.
        </UiCard>
      </UiCard>

      <UiCard title="Поле выбора" description="Hover и фокус без смещения списка" compact>
        <UiSelect v-model="selectMotion" label="Пресет Select" :options="optionsFor('select')" />
        <UiSelect
          v-model="selectedExample"
          label="Пример Select"
          :motion="selectMotion"
          :surface="glassSurface"
          :options="[{ label: 'Первый', value: 'one' }, { label: 'Второй', value: 'two' }]"
        />
      </UiCard>

      <UiCard title="Появление и исчезновение" compact>
        <UiSelect
          v-model="transitionMotion"
          label="Пресет перехода"
          :options="optionsFor('transition')"
        />
        <UiButton
          label="Переключить сообщение"
          motion="press"
          @click="showNotice = !showNotice"
        />
        <UiMotion :show="showNotice" :preset="transitionMotion">
          <UiAlert variant="success" message="Изменения сохранены" motion="none" />
        </UiMotion>
      </UiCard>

      <UiCard title="Диалог" description="PrimeVue управляет фокусом" compact>
        <UiSelect
          v-model="dialogMotion"
          label="Пресет диалога"
          :options="optionsFor('dialog')"
        />
        <UiButton
          label="Открыть диалог"
          motion="press"
          @click="dialogVisible = true"
        />
      </UiCard>
    </div>

    <UiCard title="Сложные UI — этап 13" description="Анимация данных без пересоздания строк и форм" compact>
      <div class="motion-preview__grid">
        <div class="motion-preview__complex-example">
          <UiSelect v-model="tableMotion" label="Motion таблицы" :options="optionsFor('table')" />
          <UiTable :columns="exampleColumns" :rows="exampleRows" :motion="tableMotion" aria-label="Демонстрационная таблица" />
        </div>
        <div class="motion-preview__complex-example">
          <UiSelect v-model="fieldMotion" label="Motion поля" :options="optionsFor('field')" />
          <UiButton label="Показать / убрать ошибку" size="sm" variant="secondary" @click="exampleError = !exampleError" />
          <UiField id="motion-demo-field" label="Название" :error="exampleError ? 'Поле обязательно' : ''" :motion="fieldMotion">
            <input id="motion-demo-field" class="st-ui-control" placeholder="Введите название" />
          </UiField>
        </div>
        <div class="motion-preview__complex-example">
          <UiSelect v-model="loadingMotion" label="Motion загрузки" :options="optionsFor('loading')" />
          <UiLoadingState label="Загрузка материалов" compact :motion="loadingMotion" />
        </div>
        <div class="motion-preview__complex-example">
          <UiSelect v-model="toastMotion" label="Motion уведомления" :options="optionsFor('toast')" />
          <UiButton
            label="Показать Toast"
            variant="secondary"
            size="sm"
            @click="uiToast.success('Уведомление создано', 'Motion API', { group: 'motion-preview' })"
          />
          <UiToastHost :motion="toastMotion" group="motion-preview" position="top-right" />
        </div>
        <div class="motion-preview__complex-example">
          <UiSelect v-model="disclosureMotion" label="Motion раскрытия" :options="optionsFor('disclosure')" />
          <UiDisclosure title="Дополнительные параметры" :motion="disclosureMotion">
            <p>Внутренние элементы не размонтируются при сворачивании.</p>
            <input class="st-ui-control" aria-label="Заметка для демонстрации" placeholder="Проверка сохранения ввода" />
          </UiDisclosure>
        </div>
      </div>
    </UiCard>

    <UiDialog v-model="dialogVisible" title="Пример диалога" :motion="dialogMotion" :surface="glassSurface">
      <p>Выбранный эффект: {{ dialogMotion }}.</p>
      <template #footer>
        <UiButton label="Закрыть" motion="press" @click="dialogVisible = false" />
      </template>
    </UiDialog>
  </section>
</template>

<style scoped>
.motion-preview {
  max-width: 1200px;
  display: grid;
  gap: var(--st-space-section);
}

.motion-preview h1,
.motion-preview p {
  margin: 0;
}

.motion-preview__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: var(--st-space-section);
  align-items: start;
}

.motion-preview__grid :deep(.p-card-content) {
  display: grid;
  gap: 16px;
}

.motion-preview__glass-controls {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 180px), 1fr));
  gap: 12px;
}

.motion-preview__glass-samples {
  display: grid;
  gap: 16px;
  padding: 18px;
  border-radius: var(--st-radius-card);
  background: linear-gradient(130deg, var(--st-page-bg), var(--st-surface-muted));
  justify-items: start;
}

.motion-preview__complex-example {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 12px;
}

.motion-preview__defaults {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
}

.motion-preview__defaults > div {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
}

.motion-preview__inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.motion-preview__sample {
  padding-block: 12px;
}
</style>
