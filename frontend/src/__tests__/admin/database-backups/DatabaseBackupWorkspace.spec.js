// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'

import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(
      process.cwd(),
      'src',
      relativePath
    ),
    'utf8'
  )
}

describe('database backup admin workspace', () => {
  const view = source(
    'views/admin/DatabaseBackupsView.vue'
  )

  it('uses the shared admin and overlay UI architecture', () => {
    expect(view).toContain(
      '<AdminPageShell'
    )
    expect(view).toContain(
      '<UiCard'
    )
    expect(view).toContain(
      '<UiFileInput'
    )
    expect(view).toContain(
      '<UiDialog'
    )
    expect(view).toContain(
      'restoreConfirmVisible'
    )
    expect(view).not.toContain(
      'window.confirm'
    )
  })

  it('keeps restore destructive and explicitly reloadable after success', () => {
    expect(view).toContain(
      'variant="danger"'
    )
    expect(view).toContain(
      'База данных восстановлена'
    )
    expect(view).toContain(
      'Перезагрузить приложение'
    )
    expect(view).toContain(
      'globalThis.location?.reload?.()'
    )
  })

  it('surfaces selected file metadata and backend restore result', () => {
    expect(view).toContain(
      'selectedFileSizeBytes'
    )
    expect(view).toContain(
      'restoreResult.fileName'
    )
    expect(view).toContain(
      'restoreResult.value?.sizeBytes'
    )
    expect(view).toContain(
      'restoreResult.value?.restoredAtUtc'
    )
  })

  it('keeps backup and restore mutually blocked while an operation is running', () => {
    expect(view).toContain(
      'operationsBusy'
    )
    expect(view).toContain(
      ':disabled="restoring"'
    )
    expect(view).toContain(
      ':disabled="!canRestore || creating"'
    )
  })
})
