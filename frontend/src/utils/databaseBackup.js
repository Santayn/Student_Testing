import {
  normalizeApiError,
} from '@/api/error'

export const DATABASE_BACKUP_MAX_FILE_BYTES =
  50 * 1024 * 1024

export const DATABASE_BACKUP_FILE_ACCEPT =
  '.sql,application/sql,text/plain'

export const DATABASE_BACKUP_FILE_ERRORS =
  Object.freeze({
    REQUIRED: 'backup_file_required',
    INVALID_EXTENSION:
      'backup_file_invalid_extension',
    TOO_LARGE: 'backup_file_too_large',
  })

function responseHeader(
  response,
  headerName
) {
  const headers = response?.headers

  if (!headers) {
    return ''
  }

  if (
    typeof headers.get ===
    'function'
  ) {
    return (
      headers.get(headerName) ??
      headers.get(
        headerName.toLowerCase()
      ) ??
      ''
    )
  }

  const expected =
    headerName.toLowerCase()

  const entry =
    Object.entries(headers)
      .find(
        ([key]) =>
          String(key)
            .toLowerCase() ===
          expected
      )

  return entry?.[1] ?? ''
}

function fallbackBackupFileName(
  now = new Date()
) {
  const stamp = now
    .toISOString()
    .replace(/[:.]/g, '-')

  return (
    'student-test-database-backup-' +
    `${stamp}.sql`
  )
}

export function databaseBackupFileName(
  response,
  now = new Date()
) {
  const disposition = String(
    responseHeader(
      response,
      'Content-Disposition'
    ) ?? ''
  )

  const utf8Name =
    disposition.match(
      /filename\*=UTF-8''([^;]+)/i
    )

  if (utf8Name?.[1]) {
    try {
      return decodeURIComponent(
        utf8Name[1]
          .trim()
          .replace(/^"|"$/g, '')
      )
    } catch {
      // Fall through to the plain filename or generated fallback.
    }
  }

  const plainName =
    disposition.match(
      /filename="?([^";]+)"?/i
    )

  if (plainName?.[1]) {
    return plainName[1].trim()
  }

  return fallbackBackupFileName(now)
}

export function validateDatabaseBackupFile(
  file
) {
  if (!file) {
    return {
      valid: false,
      code:
        DATABASE_BACKUP_FILE_ERRORS
          .REQUIRED,
      message:
        'Выберите SQL-файл резервной копии.',
    }
  }

  const name = String(
    file.name ?? ''
  ).trim()

  if (
    !name ||
    !name.toLowerCase()
      .endsWith('.sql')
  ) {
    return {
      valid: false,
      code:
        DATABASE_BACKUP_FILE_ERRORS
          .INVALID_EXTENSION,
      message:
        'Выберите файл резервной копии с расширением .sql.',
    }
  }

  const size = Number(file.size)

  if (
    Number.isFinite(size) &&
    size >
      DATABASE_BACKUP_MAX_FILE_BYTES
  ) {
    return {
      valid: false,
      code:
        DATABASE_BACKUP_FILE_ERRORS
          .TOO_LARGE,
      message:
        'Размер SQL-файла не должен превышать 50 МБ.',
    }
  }

  return {
    valid: true,
    code: null,
    message: '',
  }
}

export function downloadDatabaseBackupBlob(
  blob,
  fileName,
  {
    documentRef = globalThis.document,
    urlApi = globalThis.URL,
  } = {}
) {
  if (
    !blob ||
    !documentRef ||
    !urlApi?.createObjectURL
  ) {
    return false
  }

  const url =
    urlApi.createObjectURL(blob)

  const link =
    documentRef.createElement('a')

  link.href = url
  link.download = fileName
  documentRef.body.appendChild(link)

  try {
    link.click()
  } finally {
    link.remove()
    urlApi.revokeObjectURL(url)
  }

  return true
}

async function blobPayload(data) {
  if (
    !data ||
    typeof data.text !== 'function'
  ) {
    return null
  }

  try {
    const text = await data.text()

    if (!text.trim()) {
      return null
    }

    return JSON.parse(text)
  } catch {
    return null
  }
}

export async function normalizeDatabaseBackupError(
  error,
  fallback =
    'Не удалось выполнить операцию с резервной копией'
) {
  const data =
    error?.response?.data

  const parsed =
    await blobPayload(data)

  if (!parsed) {
    return normalizeApiError(
      error,
      fallback
    )
  }

  return normalizeApiError(
    {
      ...error,
      response: {
        ...error.response,
        data: parsed,
      },
    },
    fallback
  )
}

export function normalizeDatabaseRestoreResult(
  payload
) {
  const size = Number(
    payload?.sizeBytes
  )

  return {
    status:
      payload?.status == null
        ? ''
        : String(payload.status),

    fileName:
      payload?.fileName == null
        ? ''
        : String(payload.fileName),

    sizeBytes:
      Number.isFinite(size) &&
      size >= 0
        ? size
        : null,

    restoredAtUtc:
      payload?.restoredAtUtc == null
        ? ''
        : String(
            payload.restoredAtUtc
          ),
  }
}


export function formatDatabaseBackupBytes(
  value
) {
  const bytes = Number(value)

  if (
    !Number.isFinite(bytes) ||
    bytes < 0
  ) {
    return ''
  }

  if (bytes < 1024) {
    return `${bytes} Б`
  }

  const units = [
    'КБ',
    'МБ',
    'ГБ',
  ]

  let size = bytes / 1024
  let unitIndex = 0

  while (
    size >= 1024 &&
    unitIndex < units.length - 1
  ) {
    size /= 1024
    unitIndex += 1
  }

  const precision =
    size >= 10 ? 1 : 2

  return (
    `${size.toFixed(precision)}` +
    ` ${units[unitIndex]}`
  )
}

export function formatDatabaseRestoreTime(
  value,
  locale = 'ru-RU'
) {
  if (!value) {
    return ''
  }

  const date = new Date(value)

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return ''
  }

  return new Intl.DateTimeFormat(
    locale,
    {
      dateStyle: 'medium',
      timeStyle: 'short',
    }
  ).format(date)
}
