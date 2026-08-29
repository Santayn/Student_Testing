<script setup>
import { computed } from 'vue'

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

const checked = computed(() => {
  if (
    Array.isArray(
      props.modelValue
    )
  ) {
    return props.modelValue.some(
      (item) =>
        item === props.value ||
        String(item) ===
          String(props.value)
    )
  }

  return Boolean(
    props.modelValue
  )
})

function toggle(event) {
  const isChecked =
    event.target.checked

  if (
    Array.isArray(
      props.modelValue
    )
  ) {
    const next =
      [...props.modelValue]

    const index =
      next.findIndex(
        (item) =>
          item === props.value ||
          String(item) ===
            String(props.value)
      )

    if (
      isChecked &&
      index === -1
    ) {
      next.push(
        props.value
      )
    }

    if (
      !isChecked &&
      index !== -1
    ) {
      next.splice(index, 1)
    }

    emit(
      'update:modelValue',
      next
    )
  } else {
    emit(
      'update:modelValue',
      isChecked
    )
  }

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
      class="ui-checkbox__input"
      type="checkbox"
      :checked="checked"
      :disabled="disabled"
      @change="toggle"
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
