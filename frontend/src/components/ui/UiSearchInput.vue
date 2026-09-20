<script setup>
import { computed } from 'vue'
import InputText from 'primevue/inputtext'
import UiField from './UiField.vue'

defineOptions({ inheritAttrs: false })
let searchSequence = 0

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  placeholder: { type: String, default: 'Поиск' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change', 'blur', 'focus'])
const generatedId = `ui-search-${++searchSequence}`
const controlId = computed(() => props.id || generatedId)
const describedBy = computed(() => {
  if (props.error) return `${controlId.value}-error`
  if (props.hint) return `${controlId.value}-hint`
  return undefined
})
</script>

<template>
  <UiField :id="controlId" :label="label" :hint="hint" :error="error" :required="required">
    <div
      class="st-ui-search"
      :class="{
        'st-ui-search--invalid': error,
        'st-ui-search--disabled': disabled,
      }"
    >
      <span class="st-ui-search__icon" aria-hidden="true">
        <i class="pi pi-search" />
      </span>

      <InputText
        :id="controlId"
        :model-value="modelValue"
        class="st-ui-search__input"
        :placeholder="placeholder"
        :required="required"
        :disabled="disabled"
        :readonly="readonly"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        v-bind="$attrs"
        @update:model-value="emit('update:modelValue', $event)"
        @change="emit('change', $event)"
        @blur="emit('blur', $event)"
        @focus="emit('focus', $event)"
      />
    </div>
  </UiField>
</template>
