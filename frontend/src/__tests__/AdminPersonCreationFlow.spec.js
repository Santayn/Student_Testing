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

describe('admin person creation flow', () => {
  it('creates Person from inside the user drawer without a nested working dialog', () => {
    expect(personEditor).toContain('Создать новый профиль')
    expect(personEditor).toContain('admin-user-person-creator')
    expect(personEditor).toContain("emit('create-person')")
    expect(usersView).toContain('@create-person="createPersonFromDrawer"')
    expect(personEditor).not.toContain('title="Новый профиль Person"')
  })

  it('uses the existing backend person endpoint', () => {
    expect(usersApi).toContain('createPerson(payload)')
    expect(usersApi).toContain("http.post('/users/people', payload)")
    expect(personState).toContain('usersApi.createPerson(')
  })

  it('matches the current backend PersonRequest fields and limits', () => {
    expect(personEditor).toContain('userForm.newPerson.firstName')
    expect(personEditor).toContain('userForm.newPerson.lastName')
    expect(personEditor).toContain('userForm.newPerson.dateOfBirth')
    expect(personEditor).toContain('userForm.newPerson.email')
    expect(personEditor).toContain('userForm.newPerson.phone')
    expect(personEditor).toContain('maxlength="100"')
    expect(personEditor).toContain('maxlength="255"')
    expect(personEditor).toContain('maxlength="50"')
    expect(personEditor).toContain('min="1900-01-01"')
  })

  it('selects the created person but leaves account binding to the main save flow', () => {
    expect(personState).toContain('userForm.personId = createdId')
    expect(usersView).toContain('usersApi.updatePersonBinding(')
    expect(personState).toContain('Чтобы привязать его к учётной записи, сохраните изменения пользователя.')
  })
})
