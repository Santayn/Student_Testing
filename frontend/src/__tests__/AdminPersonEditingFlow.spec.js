import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    new URL(
      relativePath,
      import.meta.url
    ),
    'utf8'
  )
}

const usersView = source(
  '../views/admin/UsersView.vue'
)
const userDrawer = source(
  '../components/admin/AdminUserDrawer.vue'
)
const personEditor = source(
  '../components/admin/AdminPersonEditor.vue'
)
const personState = source(
  '../composables/useAdminPersonEditor.js'
)
const usersApi = source(
  '../api/users.api.js'
)

describe('admin person editing flow', () => {
  it('edits the selected Person inside the existing user drawer', () => {
    expect(personEditor).toContain('Изменить профиль')
    expect(personEditor).toContain('Редактирование профиля')
    expect(personEditor).toContain('admin-user-person-editor')
    expect(personEditor).toContain("emit('open-person-editor')")
    expect(personEditor).toContain("emit('update-person')")
    expect(usersView).toContain('@open-person-editor="openPersonEditor"')
    expect(usersView).toContain('@update-person="updatePersonFromDrawer"')
  })

  it('uses the existing backend update Person endpoint', () => {
    expect(usersApi).toContain('updatePerson(personId, payload)')
    expect(usersApi).toContain('`/users/people/${personId}`')
    expect(personState).toContain('usersApi.updatePerson(')
  })

  it('validates the edit draft without treating the current email as a duplicate', () => {
    expect(personState).toContain('{ excludePersonId: personId }')
    expect(personState).toContain('Number(person.id) !== Number(excludePersonId)')
    expect(personEditor).toContain('personEditDraft.firstName')
    expect(personEditor).toContain('personEditDraft.lastName')
    expect(personEditor).toContain('personEditDraft.dateOfBirth')
    expect(personEditor).toContain('personEditDraft.email')
    expect(personEditor).toContain('personEditDraft.phone')
  })

  it('updates the local Person collection immediately after a successful PUT', () => {
    expect(personState).toContain('people.value.splice(')
    expect(personState).toContain('updatedPerson')
    expect(personState).toContain('Профиль обновлён. Изменения уже отображаются во всех разделах')
  })

  it('protects a dirty Person editor before selection changes or drawer close', () => {
    expect(usersView).toContain('personEditorDirty')
    expect(usersView).toContain('requestPersonSelectionChange')
    expect(usersView).toContain('requestCloseUserDrawerSafely')
    expect(personState).toContain('personEditGuardVisible')
    expect(personState).toContain('discardPersonEditAndContinue')
    expect(usersView).toContain('Есть несохранённые изменения профиля')
  })

  it('keeps Person persistence separate from account persistence', () => {
    expect(personEditor).toContain('Сохранить профиль')
    expect(userDrawer).toContain('Сохранить изменения')
    expect(usersView).toContain('usersApi.updatePersonBinding(')
    expect(personEditor).toContain('Изменения Person сохраняются отдельно')
  })
})
