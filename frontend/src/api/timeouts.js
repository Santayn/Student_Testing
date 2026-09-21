function readTimeout(name, fallback, { allowZero = false } = {}) {
  const raw = import.meta.env?.[name]

  if (raw === undefined || raw === null || raw === '') {
    return fallback
  }

  const value = Number(raw)

  if (!Number.isFinite(value)) {
    return fallback
  }

  if (allowZero && value === 0) {
    return 0
  }

  return value > 0
    ? Math.trunc(value)
    : fallback
}

export const API_TIMEOUTS = Object.freeze({
  standard: readTimeout(
    'VITE_API_TIMEOUT_MS',
    15_000
  ),

  submitAttempt: readTimeout(
    'VITE_SUBMIT_TIMEOUT_MS',
    180_000
  ),

  fileTransfer: readTimeout(
    'VITE_FILE_TRANSFER_TIMEOUT_MS',
    0,
    {
      allowZero: true,
    }
  ),
})
