import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  useDatabaseBackupDownload,
} from '@/composables/admin/database-backups/useDatabaseBackupDownload'

import {
  useDatabaseRestore,
} from '@/composables/admin/database-backups/useDatabaseRestore'

describe('database backup lifecycle', () => {
  it('serializes backup creation and delegates blob download', async () => {
    let release
    const pending = new Promise(
      (resolve) => {
        release = resolve
      }
    )

    const api = {
      create: vi.fn(() => pending),
    }
    const download = vi.fn()

    const state =
      useDatabaseBackupDownload({
        api,
        download,
      })

    const first = state.createBackup()
    const second =
      await state.createBackup()

    expect(second).toBe(false)
    expect(api.create).toHaveBeenCalledTimes(1)

    const blob = new Blob(['dump'])
    release({
      data: blob,
      headers: {
        'content-disposition':
          'attachment; filename="backup.sql"',
      },
    })

    const result = await first

    expect(result).toMatchObject({
      fileName: 'backup.sql',
      blob,
    })
    expect(download).toHaveBeenCalledWith(
      blob,
      'backup.sql'
    )
    expect(state.creating.value).toBe(false)
  })

  it('keeps blob error code and message for backup UI presentation', async () => {
    const api = {
      create: vi.fn().mockRejectedValue({
        response: {
          status: 503,
          data: new Blob([
            JSON.stringify({
              code: 'database_backup_failed',
              message: 'pg_dump failed',
              details: [],
            }),
          ]),
        },
      }),
    }

    const state =
      useDatabaseBackupDownload({
        api,
        download: vi.fn(),
      })

    await expect(
      state.createBackup()
    ).resolves.toBe(false)

    expect(state.failure.value).toMatchObject({
      status: 503,
      code: 'database_backup_failed',
      message: 'pg_dump failed',
    })
  })

  it('validates restore file before any mutation', async () => {
    const api = {
      restore: vi.fn(),
    }
    const state =
      useDatabaseRestore({ api })

    state.setFile({
      name: 'backup.zip',
      size: 100,
    })

    await expect(
      state.restoreBackup()
    ).resolves.toBe(false)

    expect(state.fileError.value).toContain(
      '.sql'
    )
    expect(api.restore).not.toHaveBeenCalled()
  })

  it('serializes restore and preserves the success dto', async () => {
    let release
    const pending = new Promise(
      (resolve) => {
        release = resolve
      }
    )

    const api = {
      restore: vi.fn(() => pending),
    }
    const state =
      useDatabaseRestore({ api })

    const file = {
      name: 'backup.sql',
      size: 2048,
    }
    state.setFile(file)

    const first = state.restoreBackup()
    const second =
      await state.restoreBackup()

    expect(second).toBe(false)
    expect(api.restore).toHaveBeenCalledTimes(1)
    expect(api.restore).toHaveBeenCalledWith(
      file
    )

    release({
      data: {
        status: 'restored',
        fileName: 'backup.sql',
        sizeBytes: 2048,
        restoredAtUtc:
          '2026-10-05T10:20:30Z',
      },
    })

    await expect(first).resolves.toEqual({
      status: 'restored',
      fileName: 'backup.sql',
      sizeBytes: 2048,
      restoredAtUtc:
        '2026-10-05T10:20:30Z',
    })

    expect(state.result.value).toEqual({
      status: 'restored',
      fileName: 'backup.sql',
      sizeBytes: 2048,
      restoredAtUtc:
        '2026-10-05T10:20:30Z',
    })
  })
})
