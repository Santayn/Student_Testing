<script setup>
import { computed } from 'vue'

import UiField from './UiField.vue'

defineOptions({
  inheritAttrs: false,
})

let selectSequence = 0

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: '',
  },

  options: {
    type: Array,
    default: () => [],
  },

  optionLabel: {
    type: [String, Function],
    default: 'label',
  },

  optionValue: {
    type: [String, Function],
    default: 'value',
  },

  id: {
    type: String,
    default: '',
  },

  label: {
    type: String,
    default: '',
  },

  hint: {
    type: String,
    default: '',
  },

  error: {
    type: String,
    default: '',
  },

  placeholder: {
    type: String,
    default: '',
  },

  required: {
    type: Boolean,
    default: false,
  },

  disabled: {
    type: Boolean,
    default: false,
  },

  size: {
    type: String,
    default: 'md',
    validator: (value) =>
      ['sm', 'md', 'lg'].includes(value),
  },
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'blur',
  'focus',
])

const generatedId =
  `ui-select-${++selectSequence}`

const controlId = computed(() => {
  return props.id || generatedId
})

const describedBy = computed(() => {
  if (props.error) {
    return `${controlId.value}-error`
  }

  if (props.hint) {
    return `${controlId.value}-hint`
  }

  return undefined
})

function optionLabelOf(option) {
  if (
    typeof props.optionLabel ===
    'function'
  ) {
    return props.optionLabel(option)
  }

  if (
    option !== null &&
    typeof option === 'object'
  ) {
    return option[
      props.optionLabel
    ]
  }

  return option
}

function optionValueOf(option) {
  if (
    typeof props.optionValue ===
    'function'
  ) {
    return props.optionValue(option)
  }

  if (
    option !== null &&
    typeof option === 'object'
  ) {
    return option[
      props.optionValue
    ]
  }

  return option
}

function updateValue(event) {
  const option =
    event.target
      .selectedOptions?.[0]

  const value =
    option?._value ??
    event.target.value

  emit(
    'update:modelValue',
    value
  )

  emit(
    'change',
    event
  )
}
</script>

<template>
  <UiField
    :id="controlId"
    :label="label"
    :hint="hint"
    :error="error"
    :required="required"
  >
    <select
      :id="controlId"
      class="ui-control ui-select"
      :class="[
        `ui-control--${size}`,
        {
          'ui-control--error': error,
        },
      ]"
      :value="modelValue"
      :required="required"
      :disabled="disabled"
      :aria-invalid="
        error ? 'true' : undefined
      "
      :aria-describedby="describedBy"
      v-bind="$attrs"
      @change="updateValue"
      @blur="
        emit('blur', $event)
      "
      @focus="
        emit('focus', $event)
      "
    >
      <option
        v-if="placeholder"
        value=""
        :disabled="required"
      >
        {{ placeholder }}
      </option>

      <slot>
        <option
          v-for="(option, index) in options"
          :key="
            `${optionValueOf(option)}-${index}`
          "
          :value="optionValueOf(option)"
        >
          {{ optionLabelOf(option) }}
        </option>
      </slot>
    </select>
  </UiField>
</template>

<style scoped>
.ui-select {
  color-scheme: light;
}

:global(html[data-theme='dark']) .ui-select {
  color-scheme: dark;
}

.ui-select option,
:slotted(option) {
  color: var(--text);
  background: var(--surface);
}

.ui-control {
  width: 100%;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 9px;

  font: inherit;

  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.ui-control--sm {
  min-height: 36px;
  padding: 7px 9px;

  font-size: 13px;
}

.ui-control--md {
  min-height: 42px;
  padding: 8px 10px;

  font-size: 14px;
}

.ui-control--lg {
  min-height: 46px;
  padding: 10px 12px;

  font-size: 15px;
}

.ui-control:focus {
  outline: 2px solid var(--focus-ring);

  border-color: var(--brand);
}

.ui-control--error {
  border-color: var(--danger);
}

.ui-control:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}
</style>
