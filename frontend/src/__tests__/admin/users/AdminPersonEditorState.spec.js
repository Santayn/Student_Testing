import {
  computed,
  reactive,
  ref,
} from 'vue'
import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  emptyAdminPersonDraft,
  useAdminPersonEditor,
} from '@/composables/admin/users/useAdminPersonEditor'

function makeHarness() {
  const people = ref([
    {
      id: 5,
      firstName: 'Иван',
      lastName: 'Иванов',
      dateOfBirth: '1990-01-01',
      email: 'ivan@example.com',
      phone: '+70000000000',
    },
  ])
  const userForm = reactive({
    personId: 5,
    newPerson: emptyAdminPersonDraft(),
  })
  const selectedPerson = computed(() =>
    people.value.find(
      (person) => Number(person.id) === Number(userForm.personId)
    ) ?? null
  )
  const usersApi = {
    createPerson: vi.fn(),
    updatePerson: vi.fn(),
  }
  const fullName = (person) =>
    `${person.lastName} ${person.firstName}`
  const state = useAdminPersonEditor({
    userForm,
    selectedPerson,
    people,
    fullName,
    usersApi,
    getApiErrorMessage: (_error, fallback) => fallback,
  })

  return {
    people,
    userForm,
    selectedPerson,
    usersApi,
    state,
  }
}

describe('useAdminPersonEditor', () => {
  it('guards Person selection changes while the editor is dirty', () => {
    const { userForm, state } = makeHarness()

    state.openPersonEditor()
    state.personEditDraft.value.lastName = 'Сидоров'
    state.requestPersonSelectionChange(null)

    expect(state.personEditorDirty.value).toBe(true)
    expect(state.personEditGuardVisible.value).toBe(true)
    expect(userForm.personId).toBe(5)

    state.discardPersonEditAndContinue()

    expect(userForm.personId).toBeNull()
    expect(state.personEditorVisible.value).toBe(false)
  })

  it('creates Person, updates the local collection and selects it without binding the account', async () => {
    const { people, userForm, usersApi, state } = makeHarness()

    Object.assign(userForm.newPerson, {
      firstName: 'Пётр',
      lastName: 'Петров',
      email: 'PETR@example.com',
      phone: '+71111111111',
    })
    usersApi.createPerson.mockResolvedValue({
      data: {
        id: 6,
        firstName: 'Пётр',
        lastName: 'Петров',
        email: 'petr@example.com',
        phone: '+71111111111',
      },
    })

    await state.createPersonFromDrawer()

    expect(usersApi.createPerson).toHaveBeenCalledWith(
      expect.objectContaining({
        firstName: 'Пётр',
        lastName: 'Петров',
        email: 'petr@example.com',
      })
    )
    expect(userForm.personId).toBe(6)
    expect(people.value.some((person) => person.id === 6)).toBe(true)
    expect(state.personCreateMessage.value).toContain('сохраните изменения пользователя')
  })

  it('allows the selected Person to keep its own email during edit validation', async () => {
    const { people, usersApi, state } = makeHarness()

    state.openPersonEditor()
    state.personEditDraft.value.firstName = 'Иван обновлённый'
    usersApi.updatePerson.mockResolvedValue({
      data: {
        ...people.value[0],
        firstName: 'Иван обновлённый',
      },
    })

    await state.updatePersonFromDrawer()

    expect(usersApi.updatePerson).toHaveBeenCalledWith(
      5,
      expect.objectContaining({
        firstName: 'Иван обновлённый',
        email: 'ivan@example.com',
      })
    )
    expect(people.value[0].firstName).toBe('Иван обновлённый')
    expect(state.personEditorVisible.value).toBe(false)
    expect(state.personEditMessage.value).toContain('Профиль обновлён')
  })

  it('rejects duplicate email belonging to another Person', () => {
    const { people, state } = makeHarness()
    people.value.push({
      id: 6,
      firstName: 'Пётр',
      lastName: 'Петров',
      email: 'petr@example.com',
    })

    const message = state.personDraftValidationMessage({
      firstName: 'Иван',
      lastName: 'Иванов',
      email: 'PETR@example.com',
      phone: '',
      dateOfBirth: '',
    }, { excludePersonId: 5 })

    expect(message).toBe('Профиль с таким email уже существует.')
  })
})
