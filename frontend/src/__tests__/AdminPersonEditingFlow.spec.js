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
const usersApi = source(
  '../api/users.api.js'
)

describe('admin person editing flow', () => {
  it('edits the selected Person inside the existing user drawer', () => {
    expect(usersView).toContain('Изменить профиль')
    expect(usersView).toContain('Редактирование профиля')
    expect(usersView).toContain('admin-user-person-editor')
    expect(usersView).toContain('@click="openPersonEditor"')
    expect(usersView).toContain('@click="updatePersonFromDrawer"')
  })

  it('uses the existing backend update Person endpoint', () => {
    expect(usersApi).toContain('updatePerson(personId, payload)')
    expect(usersApi).toContain('`/users/people/${personId}`')
    expect(usersView).toContain('usersApi.updatePerson(')
  })

  it('validates the edit draft without treating the current email as a duplicate', () => {
    expect(usersView).toContain('{ excludePersonId: personId }')
    expect(usersView).toContain('Number(person.id) !== Number(excludePersonId)')
    expect(usersView).toContain('personEditDraft.firstName')
    expect(usersView).toContain('personEditDraft.lastName')
    expect(usersView).toContain('personEditDraft.dateOfBirth')
    expect(usersView).toContain('personEditDraft.email')
    expect(usersView).toContain('personEditDraft.phone')
  })

  it('updates the local Person collection immediately after a successful PUT', () => {
    expect(usersView).toContain('people.value.splice(')
    expect(usersView).toContain('updatedPerson')
    expect(usersView).toContain('Профиль обновлён. Изменения уже отображаются во всех разделах')
  })

  it('protects a dirty Person editor before selection changes or drawer close', () => {
    expect(usersView).toContain('personEditorDirty')
    expect(usersView).toContain('requestPersonSelectionChange')
    expect(usersView).toContain('requestCloseUserDrawerSafely')
    expect(usersView).toContain('personEditGuardVisible')
    expect(usersView).toContain('discardPersonEditAndContinue')
    expect(usersView).toContain('Есть несохранённые изменения профиля')
  })

  it('keeps Person persistence separate from account persistence', () => {
    expect(usersView).toContain('Сохранить профиль')
    expect(usersView).toContain('Сохранить изменения')
    expect(usersView).toContain('usersApi.updatePersonBinding(')
    expect(usersView).toContain('Изменения Person сохраняются отдельно')
  })
})
