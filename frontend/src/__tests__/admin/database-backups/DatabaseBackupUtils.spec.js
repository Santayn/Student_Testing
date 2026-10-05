// @vitest-environment jsdom

import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  DATABASE_BACKUP_MAX_FILE_BYTES,
  databaseBackupFileName,
  downloadDatabaseBackupBlob,
  formatDatabaseBackupBytes,
  formatDatabaseRestoreTime,
  normalizeDatabaseBackupError,
  normalizeDatabaseRestoreResult,
  validateDatabaseBackupFile,
} from '@/utils/databaseBackup'

describe('database backup utilities', () => {
  it('reads UTF-8 and plain filenames from Content-Disposition', () => {
    expect(
      databaseBackupFileName({
        headers: {
          'content-disposition':
            "attachment; filename*=UTF-8''backup%20one.sql",
        },
      })
    ).toBe('backup one.sql')

    expect(
      databaseBackupFileName({
        headers: {
          'Content-Disposition':
            'attachment; filename="backup-two.sql"',
        },
      })
    ).toBe('backup-two.sql')
  })

  it('generates a deterministic fallback filename', () => {
    const name = databaseBackupFileName(
      { headers: {} },
      new Date('2026-10-05T10:20:30.000Z')
    )

    expect(name).toBe(
      'student-test-database-backup-2026-10-05T10-20-30-000Z.sql'
    )
  })

  it('accepts only sql files up to the restore contract limit', () => {
    expect(
      validateDatabaseBackupFile(null)
    ).toMatchObject({
      valid: false,
      code: 'backup_file_required',
    })

    expect(
      validateDatabaseBackupFile({
        name: 'backup.zip',
        size: 100,
      })
    ).toMatchObject({
      valid: false,
      code:
        'backup_file_invalid_extension',
    })

    expect(
      validateDatabaseBackupFile({
        name: 'backup.sql',
        size:
          DATABASE_BACKUP_MAX_FILE_BYTES +
          1,
      })
    ).toMatchObject({
      valid: false,
      code: 'backup_file_too_large',
    })

    expect(
      validateDatabaseBackupFile({
        name: 'BACKUP.SQL',
        size: 1024,
      })
    ).toEqual({
      valid: true,
      code: null,
      message: '',
    })
  })

  it('normalizes JSON errors returned as a blob', async () => {
    const blob = new Blob([
      JSON.stringify({
        code: 'database_backup_failed',
        message: 'pg_dump unavailable',
        details: [],
      }),
    ], {
      type: 'application/json',
    })

    const normalized =
      await normalizeDatabaseBackupError(
        {
          response: {
            status: 503,
            data: blob,
          },
        },
        'fallback'
      )

    expect(normalized).toMatchObject({
      status: 503,
      code: 'database_backup_failed',
      message: 'pg_dump unavailable',
    })
  })

  it('formats file metadata for the admin workspace', () => {
    expect(
      formatDatabaseBackupBytes(0)
    ).toBe('0 Б')

    expect(
      formatDatabaseBackupBytes(1024)
    ).toBe('1.00 КБ')

    expect(
      formatDatabaseBackupBytes(
        50 * 1024 * 1024
      )
    ).toBe('50.0 МБ')

    expect(
      formatDatabaseBackupBytes(-1)
    ).toBe('')

    expect(
      formatDatabaseRestoreTime(
        'not-a-date'
      )
    ).toBe('')

    expect(
      formatDatabaseRestoreTime(
        '2026-10-05T10:20:30Z'
      )
    ).toBeTruthy()
  })

  it('normalizes the restore success dto', () => {
    expect(
      normalizeDatabaseRestoreResult({
        status: 'restored',
        fileName: 'backup.sql',
        sizeBytes: '2048',
        restoredAtUtc:
          '2026-10-05T10:20:30Z',
      })
    ).toEqual({
      status: 'restored',
      fileName: 'backup.sql',
      sizeBytes: 2048,
      restoredAtUtc:
        '2026-10-05T10:20:30Z',
    })
  })

  it('downloads and always revokes the object URL', () => {
    const click = vi.fn()
    const remove = vi.fn()
    const appendChild = vi.fn()
    const link = {
      href: '',
      download: '',
      click,
      remove,
    }
    const documentRef = {
      body: { appendChild },
      createElement: vi.fn(() => link),
    }
    const urlApi = {
      createObjectURL: vi.fn(
        () => 'blob:test'
      ),
      revokeObjectURL: vi.fn(),
    }

    const blob = new Blob(['dump'])

    expect(
      downloadDatabaseBackupBlob(
        blob,
        'backup.sql',
        {
          documentRef,
          urlApi,
        }
      )
    ).toBe(true)

    expect(link.href).toBe('blob:test')
    expect(link.download).toBe(
      'backup.sql'
    )
    expect(appendChild).toHaveBeenCalledWith(
      link
    )
    expect(click).toHaveBeenCalledTimes(1)
    expect(remove).toHaveBeenCalledTimes(1)
    expect(
      urlApi.revokeObjectURL
    ).toHaveBeenCalledWith('blob:test')
  })
})
