<script setup>
import UiButton from './UiButton.vue'
import UiDialog from './UiDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: 'Есть несохранённые изменения' },
  message: {
    type: String,
    default: 'Если закрыть форму сейчас, внесённые изменения будут потеряны.',
  },
  continueLabel: { type: String, default: 'Продолжить редактирование' },
  discardLabel: { type: String, default: 'Закрыть без сохранения' },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'continue', 'discard'])

function continueEditing() {
  if (props.busy) return
  emit('continue')
  emit('update:modelValue', false)
}

function discardChanges() {
  if (props.busy) return
  emit('discard')
}
</script>

<template>
  <UiDialog
    :model-value="modelValue"
    :title="title"
    width="30rem"
    :closable="false"
    :close-on-escape="false"
    :dismissable-mask="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="m-0 leading-7 text-[var(--st-text-secondary)]">
      {{ message }}
    </p>

    <template #footer>
      <div class="flex w-full flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <UiButton
          variant="danger"
          :label="discardLabel"
          :disabled="busy"
          @click="discardChanges"
        />
        <UiButton
          variant="primary"
          :label="continueLabel"
          :disabled="busy"
          @click="continueEditing"
        />
      </div>
    </template>
  </UiDialog>
</template>
