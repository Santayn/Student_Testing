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

describe('admin person creation flow', () => {
  it('creates Person from inside the user drawer without a nested working dialog', () => {
    expect(usersView).toContain('Создать новый профиль')
    expect(usersView).toContain('admin-user-person-creator')
    expect(usersView).toContain('@click="createPersonFromDrawer"')
    expect(usersView).not.toContain('title="Новый профиль Person"')
  })

  it('uses the existing backend person endpoint', () => {
    expect(usersApi).toContain('createPerson(payload)')
    expect(usersApi).toContain("http.post('/users/people', payload)")
    expect(usersView).toContain('usersApi.createPerson(')
  })

  it('matches the current backend PersonRequest fields and limits', () => {
    expect(usersView).toContain('userForm.newPerson.firstName')
    expect(usersView).toContain('userForm.newPerson.lastName')
    expect(usersView).toContain('userForm.newPerson.dateOfBirth')
    expect(usersView).toContain('userForm.newPerson.email')
    expect(usersView).toContain('userForm.newPerson.phone')
    expect(usersView).toContain('maxlength="100"')
    expect(usersView).toContain('maxlength="255"')
    expect(usersView).toContain('maxlength="50"')
    expect(usersView).toContain('min="1900-01-01"')
  })

  it('selects the created person but leaves account binding to the main save flow', () => {
    expect(usersView).toContain('userForm.personId = createdId')
    expect(usersView).toContain('usersApi.updatePersonBinding(')
    expect(usersView).toContain('Чтобы привязать его к учётной записи, сохраните изменения пользователя.')
  })
})
