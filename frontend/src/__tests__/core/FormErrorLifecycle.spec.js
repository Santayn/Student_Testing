// @vitest-environment jsdom

import { ref } from 'vue'
import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  clearFormFieldError,
  focusFirstInvalidField,
  FORM_FIELD_ERROR_SUMMARY,
  setFormFieldError,
} from '@/utils/formErrorLifecycle'

describe('form error lifecycle', () => {
  it('clears only the edited backend field aliases', () => {
    const fieldErrors = ref({
      facultyId: ['Выберите факультет.'],
      faculty: ['Факультет недоступен.'],
      name: ['Введите название.'],
    })
    const formError = ref(
      FORM_FIELD_ERROR_SUMMARY
    )

    clearFormFieldError(
      fieldErrors,
      formError,
      'facultyId',
      'faculty'
    )

    expect(fieldErrors.value).toEqual({
      name: ['Введите название.'],
    })
    expect(formError.value).toBe(
      FORM_FIELD_ERROR_SUMMARY
    )
  })

  it('removes the generic summary after the last field error is edited', () => {
    const fieldErrors = ref({
      name: ['Введите название.'],
    })
    const formError = ref(
      FORM_FIELD_ERROR_SUMMARY
    )

    clearFormFieldError(
      fieldErrors,
      formError,
      'name'
    )

    expect(fieldErrors.value).toEqual({})
    expect(formError.value).toBe('')
  })

  it('maps client validation to the same field error channel', () => {
    const fieldErrors = ref({})
    const formError = ref('')

    setFormFieldError(
      fieldErrors,
      formError,
      'code',
      'Введите код.'
    )

    expect(fieldErrors.value).toEqual({
      code: ['Введите код.'],
    })
    expect(formError.value).toBe(
      FORM_FIELD_ERROR_SUMMARY
    )
  })

  it('focuses the first invalid control inside the submitted form', async () => {
    const form =
      document.createElement('form')
    const first =
      document.createElement('input')
    const second =
      document.createElement('input')

    first.setAttribute(
      'aria-invalid',
      'true'
    )
    second.setAttribute(
      'aria-invalid',
      'true'
    )

    form.append(first, second)
    document.body.appendChild(form)

    await expect(
      focusFirstInvalidField(form)
    ).resolves.toBe(true)

    expect(
      document.activeElement
    ).toBe(first)

    form.remove()
  })
})
