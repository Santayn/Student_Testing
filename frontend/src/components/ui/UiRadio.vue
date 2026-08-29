<script setup>
import { computed } from 'vue'

let radioSequence = 0

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: null,
  },

  value: {
    type: [String, Number, Boolean],
    required: true,
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

  name: {
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
  `ui-radio-${++radioSequence}`

const controlId = computed(() => {
  return props.id || generatedId
})

const checked = computed(() => {
  return (
    props.modelValue === props.value ||
    (
      props.modelValue !== null &&
      props.modelValue !== undefined &&
      String(props.modelValue) ===
        String(props.value)
    )
  )
})

function select(event) {
  if (!event.target.checked) {
    return
  }

  emit(
    'update:modelValue',
    props.value
  )

  emit(
    'change',
    event
  )
}
</script>

<template>
  <label
    class="ui-radio"
    :class="{
      'ui-radio--disabled':
        disabled,
    }"
    :for="controlId"
  >
    <input
      :id="controlId"
      class="ui-radio__input"
      type="radio"
      :name="name || undefined"
      :value="value"
      :checked="checked"
      :disabled="disabled"
      @change="select"
    >

    <span class="ui-radio__copy">
      <span
        v-if="label || $slots.default"
        class="ui-radio__label"
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
        class="ui-radio__description"
      >
        <slot name="description">
          {{ description }}
        </slot>
      </span>
    </span>
  </label>
</template>

<style scoped>
.ui-radio {
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

.ui-radio:hover {
  border-color: var(--brand);
}

.ui-radio--disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.ui-radio__input {
  margin-top: 2px;

  accent-color: var(--brand);
}

.ui-radio__copy {
  min-width: 0;

  display: grid;
  gap: 2px;
}

.ui-radio__label {
  font-weight: 700;
}

.ui-radio__description {
  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.4;
}
</style>
