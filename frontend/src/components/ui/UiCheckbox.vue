<script setup>
import {
  computed,
} from 'vue'

let checkboxSequence = 0

const props = defineProps({
  modelValue: {
    type: [Boolean, Array],
    default: false,
  },

  value: {
    type: [String, Number, Boolean, Object],
    default: true,
  },

  id: {
    type: String,
    default: '',
  },

  label: {
    type: String,
    default: '',
  },

  description: {
    type: String,
    default: '',
  },

  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'change',
])

const generatedId =
  `ui-checkbox-${++checkboxSequence}`

const controlId = computed(() => {
  return props.id || generatedId
})

/*
 * Нативный checkbox умеет работать через v-model как:
 *
 *   Boolean
 *   Array + :value
 *
 * Для multiple-choice TestView передаёт массив option.id.
 * Vue сама добавляет/удаляет конкретный :value из массива,
 * поэтому checkbox больше не управляется вручную через
 * :checked/@change и не может переключить соседнюю опцию.
 */
const model = computed({
  get() {
    return props.modelValue
  },

  set(value) {
    emit(
      'update:modelValue',
      value
    )
  },
})

function handleChange(event) {
  emit(
    'change',
    event
  )
}
</script>

<template>
  <label
    class="ui-checkbox"
    :class="{
      'ui-checkbox--disabled':
        disabled,
    }"
    :for="controlId"
  >
    <input
      :id="controlId"
      v-model="model"
      class="ui-checkbox__input"
      type="checkbox"
      :value="value"
      :disabled="disabled"
      @change="handleChange"
    >

    <span class="ui-checkbox__copy">
      <span
        v-if="label || $slots.default"
        class="ui-checkbox__label"
      >
        <slot>
          {{ label }}
        </slot>
      </span>

      <span
        v-if="
          description ||
          $slots.description
        "
        class="ui-checkbox__description"
      >
        <slot name="description">
          {{ description }}
        </slot>
      </span>
    </span>
  </label>
</template>

<style scoped>
.ui-checkbox {
  min-height: 42px;

  padding: 8px 10px;

  display: flex;
  align-items: flex-start;
  gap: 9px;

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 8px;

  font-size: 13px;

  cursor: pointer;

  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.ui-checkbox:hover {
  border-color: var(--brand);
}

.ui-checkbox--disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.ui-checkbox__input {
  margin-top: 2px;

  accent-color: var(--brand);
}

.ui-checkbox__copy {
  min-width: 0;

  display: grid;
  gap: 2px;
}

.ui-checkbox__label {
  font-weight: 700;
}

.ui-checkbox__description {
  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.4;
}
</style>
