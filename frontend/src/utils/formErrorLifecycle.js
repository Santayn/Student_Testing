import { nextTick } from 'vue'

export const FORM_FIELD_ERROR_SUMMARY =
  'Проверьте выделенные поля.'

function errorKeys(fieldErrorsRef) {
  return Object.keys(
    fieldErrorsRef?.value ?? {}
  )
}

export function clearFormFieldError(
  fieldErrorsRef,
  formErrorRef,
  ...fieldNames
) {
  const current =
    fieldErrorsRef?.value ?? {}

  if (!fieldNames.some(
    (name) =>
      Object.prototype.hasOwnProperty.call(
        current,
        name
      )
  )) {
    return false
  }

  const next = {
    ...current,
  }

  for (const fieldName of fieldNames) {
    delete next[fieldName]
  }

  fieldErrorsRef.value = next

  if (
    formErrorRef?.value ===
      FORM_FIELD_ERROR_SUMMARY &&
    errorKeys(fieldErrorsRef).length === 0
  ) {
    formErrorRef.value = ''
  }

  return true
}

export function setFormFieldError(
  fieldErrorsRef,
  formErrorRef,
  fieldName,
  message
) {
  if (!fieldName || !message) {
    return false
  }

  fieldErrorsRef.value = {
    [fieldName]: [message],
  }

  if (formErrorRef) {
    formErrorRef.value =
      FORM_FIELD_ERROR_SUMMARY
  }

  return true
}

export async function focusFirstInvalidField(
  formElement
) {
  await nextTick()

  const invalid =
    formElement?.querySelector?.(
      '[aria-invalid="true"]'
    )

  if (
    !invalid ||
    typeof invalid.focus !== 'function'
  ) {
    return false
  }

  invalid.focus()
  return true
}
