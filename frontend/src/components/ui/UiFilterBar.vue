<script setup>
import { computed, useSlots } from 'vue'

import UiButton from './UiButton.vue'
import UiSearchInput from './UiSearchInput.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  ariaLabel: { type: String, default: 'Фильтры списка' },
  searchPlaceholder: { type: String, default: 'Поиск' },
  searchLabel: { type: String, default: '' },
  resultCount: { type: Number, default: null },
  resultText: { type: String, default: '' },
  resetLabel: { type: String, default: 'Сбросить' },
  resetDisabled: { type: Boolean, default: false },
  showSearch: { type: Boolean, default: true },
  showReset: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue', 'reset'])
const slots = useSlots()

const hasFilters = computed(() => Boolean(slots.filters))
const hasActions = computed(() => Boolean(slots.actions))
const hasMeta = computed(() => Boolean(slots.meta) || props.resultCount !== null || props.resultText)
const visibleResultText = computed(() => {
  if (props.resultText) return props.resultText
  if (props.resultCount !== null) return `Найдено: ${props.resultCount}`
  return ''
})
</script>

<template>
  <section class="st-ui-filter-bar" :aria-label="ariaLabel">
    <div class="st-ui-filter-bar__controls">
      <div v-if="showSearch" class="st-ui-filter-bar__search">
        <UiSearchInput
          :model-value="modelValue"
          :label="searchLabel"
          :placeholder="searchPlaceholder"
          @update:model-value="emit('update:modelValue', $event)"
        />
      </div>

      <div v-if="hasFilters" class="st-ui-filter-bar__filters">
        <slot name="filters" />
      </div>

      <div v-if="showReset" class="st-ui-filter-bar__reset">
        <UiButton
          variant="ghost"
          size="sm"
          :label="resetLabel"
          icon="pi pi-filter-slash"
          :disabled="resetDisabled"
          @click="emit('reset')"
        />
      </div>

      <div v-if="hasActions" class="st-ui-filter-bar__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="hasMeta" class="st-ui-filter-bar__meta">
      <slot name="meta">
        <span>{{ visibleResultText }}</span>
      </slot>
    </div>
  </section>
</template>
