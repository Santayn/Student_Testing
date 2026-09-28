import {
  computed,
  ref,
  unref,
} from 'vue'

export function emptyAdminPersonDraft() {
  return {
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    email: '',
    phone: '',
  }
}

function mapPersonToDraft(person) {
  return {
    firstName: String(person?.firstName ?? ''),
    lastName: String(person?.lastName ?? ''),
    dateOfBirth: String(person?.dateOfBirth ?? ''),
    email: String(person?.email ?? ''),
    phone: String(person?.phone ?? ''),
  }
}

function normalizePersonEmail(value) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase('ru-RU')
}

function normalizedPersonDraft(draft) {
  return {
    firstName: String(draft?.firstName ?? '').trim(),
    lastName: String(draft?.lastName ?? '').trim(),
    dateOfBirth: String(draft?.dateOfBirth ?? '').trim(),
    email: normalizePersonEmail(draft?.email),
    phone: String(draft?.phone ?? '').trim(),
  }
}

function personDraftKey(draft) {
  return JSON.stringify(normalizedPersonDraft(draft))
}

function normalizeSelectedPersonId(value) {
  if (value === null || value === '') {
    return null
  }

  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

export function useAdminPersonEditor({
  userForm,
  selectedPerson,
  people,
  fullName,
  usersApi,
  getApiErrorMessage,
}) {
  const personCreatorVisible = ref(false)
  const personCreateError = ref('')
  const personCreateMessage = ref('')
  const creatingPerson = ref(false)

  const personEditorVisible = ref(false)
  const personEditError = ref('')
  const personEditMessage = ref('')
  const updatingPerson = ref(false)
  const personEditDraft = ref(
    emptyAdminPersonDraft()
  )
  const personEditBaseline = ref('')
  const personEditGuardVisible = ref(false)
  let pendingPersonAction = null

  const personEditorDirty = computed(() => {
    if (!personEditorVisible.value) {
      return false
    }

    return personDraftKey(personEditDraft.value) !==
      personEditBaseline.value
  })

  function resetPersonCreator() {
    personCreatorVisible.value = false
    personCreateError.value = ''
    personCreateMessage.value = ''

    if (userForm.newPerson) {
      Object.assign(
        userForm.newPerson,
        emptyAdminPersonDraft()
      )
    }
  }

  function openPersonCreator() {
    personCreateError.value = ''
    personCreateMessage.value = ''
    personEditMessage.value = ''
    personCreatorVisible.value = true
  }

  function cancelPersonCreator() {
    personCreateError.value = ''

    if (userForm.newPerson) {
      Object.assign(
        userForm.newPerson,
        emptyAdminPersonDraft()
      )
    }

    personCreatorVisible.value = false
  }

  function resetPersonEditor({ keepMessage = false } = {}) {
    personEditorVisible.value = false
    personEditError.value = ''

    if (!keepMessage) {
      personEditMessage.value = ''
    }

    personEditDraft.value = emptyAdminPersonDraft()
    personEditBaseline.value = ''
  }

  function openPersonEditor() {
    const person = unref(selectedPerson)

    if (!person) {
      return
    }

    cancelPersonCreator()
    personCreateMessage.value = ''
    personEditError.value = ''
    personEditMessage.value = ''
    personEditDraft.value = mapPersonToDraft(person)
    personEditBaseline.value = personDraftKey(
      personEditDraft.value
    )
    personEditorVisible.value = true
  }

  function continuePersonEditing() {
    pendingPersonAction = null
    personEditGuardVisible.value = false
  }

  function discardPersonEditAndContinue() {
    const action = pendingPersonAction
    pendingPersonAction = null
    personEditGuardVisible.value = false
    resetPersonEditor()

    if (typeof action === 'function') {
      action()
    }
  }

  function runAfterPersonEditorGuard(action) {
    if (updatingPerson.value) {
      return false
    }

    if (personEditorDirty.value) {
      pendingPersonAction = action
      personEditGuardVisible.value = true
      return false
    }

    resetPersonEditor()

    if (typeof action === 'function') {
      action()
    }

    return true
  }

  function requestOpenPersonCreator() {
    runAfterPersonEditorGuard(() => {
      openPersonCreator()
    })
  }

  function requestCancelPersonEditor() {
    runAfterPersonEditorGuard(() => {})
  }

  function requestPersonSelectionChange(nextValue) {
    const nextPersonId =
      normalizeSelectedPersonId(nextValue)

    if (nextPersonId === userForm.personId) {
      return
    }

    runAfterPersonEditorGuard(() => {
      personCreateMessage.value = ''
      userForm.personId = nextPersonId
    })
  }

  function personDraftValidationMessage(
    draft = userForm.newPerson ?? emptyAdminPersonDraft(),
    { excludePersonId = null } = {}
  ) {
    const firstName = String(draft.firstName ?? '').trim()
    const lastName = String(draft.lastName ?? '').trim()
    const email = normalizePersonEmail(draft.email)
    const phone = String(draft.phone ?? '').trim()
    const dateOfBirth = String(draft.dateOfBirth ?? '').trim()

    if (!firstName) {
      return 'Укажите имя.'
    }

    if (firstName.length > 100) {
      return 'Имя не должно превышать 100 символов.'
    }

    if (!lastName) {
      return 'Укажите фамилию.'
    }

    if (lastName.length > 100) {
      return 'Фамилия не должна превышать 100 символов.'
    }

    if (!email) {
      return 'Укажите email.'
    }

    if (email.length > 255) {
      return 'Email не должен превышать 255 символов.'
    }

    if (!/^[^\\s@]+@[^\\s@]+$/.test(email)) {
      return 'Укажите корректный email.'
    }

    if (phone.length > 50) {
      return 'Телефон не должен превышать 50 символов.'
    }

    if (
      dateOfBirth &&
      dateOfBirth < '1900-01-01'
    ) {
      return 'Дата рождения не может быть раньше 01.01.1900.'
    }

    const duplicateEmail = people.value.some(
      (person) =>
        Number(person.id) !== Number(excludePersonId) &&
        normalizePersonEmail(person.email) === email
    )

    if (duplicateEmail) {
      return 'Профиль с таким email уже существует.'
    }

    return ''
  }

  async function createPersonFromDrawer() {
    if (creatingPerson.value) {
      return
    }

    personCreateError.value = ''
    personCreateMessage.value = ''

    const validationMessage =
      personDraftValidationMessage(
        userForm.newPerson
      )

    if (validationMessage) {
      personCreateError.value = validationMessage
      return
    }

    const draft = userForm.newPerson
    const payload = {
      firstName: String(draft.firstName).trim(),
      lastName: String(draft.lastName).trim(),
      dateOfBirth:
        String(draft.dateOfBirth ?? '').trim() || null,
      email: normalizePersonEmail(draft.email),
      phone: String(draft.phone ?? '').trim(),
    }

    creatingPerson.value = true

    try {
      const response = await usersApi.createPerson(
        payload
      )
      const createdPerson = response?.data
      const createdId = Number(createdPerson?.id)

      if (!Number.isFinite(createdId)) {
        throw new Error(
          'Backend не вернул идентификатор созданного профиля.'
        )
      }

      const existingIndex = people.value.findIndex(
        (person) => Number(person.id) === createdId
      )

      if (existingIndex >= 0) {
        people.value.splice(
          existingIndex,
          1,
          createdPerson
        )
      } else {
        people.value.push(createdPerson)
      }

      people.value.sort((left, right) =>
        fullName(left).localeCompare(
          fullName(right),
          'ru'
        )
      )

      userForm.personId = createdId
      Object.assign(
        userForm.newPerson,
        emptyAdminPersonDraft()
      )
      personCreatorVisible.value = false
      personCreateMessage.value =
        'Профиль создан и выбран. Чтобы привязать его к учётной записи, сохраните изменения пользователя.'
    } catch (error) {
      personCreateError.value = getApiErrorMessage(
        error,
        'Не удалось создать профиль.'
      )
    } finally {
      creatingPerson.value = false
    }
  }

  async function updatePersonFromDrawer() {
    const person = unref(selectedPerson)

    if (
      updatingPerson.value ||
      !person
    ) {
      return
    }

    personEditError.value = ''
    personEditMessage.value = ''

    const personId = Number(person.id)
    const validationMessage =
      personDraftValidationMessage(
        personEditDraft.value,
        { excludePersonId: personId }
      )

    if (validationMessage) {
      personEditError.value = validationMessage
      return
    }

    const payload = normalizedPersonDraft(
      personEditDraft.value
    )

    updatingPerson.value = true

    try {
      const response = await usersApi.updatePerson(
        personId,
        {
          ...payload,
          dateOfBirth: payload.dateOfBirth || null,
        }
      )

      const updatedPerson = response?.data
      const updatedId = Number(updatedPerson?.id)

      if (!Number.isFinite(updatedId)) {
        throw new Error(
          'Backend не вернул идентификатор обновлённого профиля.'
        )
      }

      const existingIndex = people.value.findIndex(
        (entry) => Number(entry.id) === updatedId
      )

      if (existingIndex >= 0) {
        people.value.splice(
          existingIndex,
          1,
          updatedPerson
        )
      } else {
        people.value.push(updatedPerson)
      }

      people.value.sort((left, right) =>
        fullName(left).localeCompare(
          fullName(right),
          'ru'
        )
      )

      personEditMessage.value =
        'Профиль обновлён. Изменения уже отображаются во всех разделах, где используется этот Person.'
      resetPersonEditor({ keepMessage: true })
    } catch (error) {
      personEditError.value = getApiErrorMessage(
        error,
        'Не удалось обновить профиль.'
      )
    } finally {
      updatingPerson.value = false
    }
  }

  return {
    personCreatorVisible,
    personCreateError,
    personCreateMessage,
    creatingPerson,
    personEditorVisible,
    personEditError,
    personEditMessage,
    updatingPerson,
    personEditDraft,
    personEditGuardVisible,
    personEditorDirty,
    resetPersonCreator,
    resetPersonEditor,
    requestOpenPersonCreator,
    openPersonEditor,
    cancelPersonCreator,
    requestCancelPersonEditor,
    requestPersonSelectionChange,
    createPersonFromDrawer,
    updatePersonFromDrawer,
    runAfterPersonEditorGuard,
    continuePersonEditing,
    discardPersonEditAndContinue,
    personDraftValidationMessage,
  }
}
