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

describe('database backup technical contract', () => {
  it('keeps download and restore endpoints aligned with the backend contract', () => {
    const api = source(
      'api/databaseBackups.api.js'
    )

    expect(api).toContain(
      "'/admin/database-backups'"
    )
    expect(api).toContain(
      "'/admin/database-backups/restore'"
    )
    expect(api).toContain(
      "responseType: 'blob'"
    )
    expect(api).toContain(
      "formData.append('file', file)"
    )
    expect(api).toContain(
      'config.timeout ?? 0'
    )
  })

  it('registers the machine-readable backup error codes', () => {
    const codes = source(
      'api/errorCodes.js'
    )

    expect(codes).toContain(
      "DATABASE_BACKUP_FAILED"
    )
    expect(codes).toContain(
      "'database_backup_failed'"
    )
    expect(codes).toContain(
      "PAYLOAD_TOO_LARGE"
    )
    expect(codes).toContain(
      "'payload_too_large'"
    )
  })

  it('keeps destructive confirmation and visual redesign out of technical composables', () => {
    const download = source(
      'composables/admin/database-backups/useDatabaseBackupDownload.js'
    )
    const restore = source(
      'composables/admin/database-backups/useDatabaseRestore.js'
    )

    expect(download).toContain(
      'if (creating.value)'
    )
    expect(restore).toContain(
      'if (restoring.value)'
    )
    expect(restore).not.toContain(
      'window.confirm'
    )
    expect(restore).not.toContain(
      'window.location.reload'
    )
  })
})
