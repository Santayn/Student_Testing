export function parseBooleanFlag(value) {
  return [
    '1',
    'true',
    'yes',
    'on',
  ].includes(
    String(value ?? '')
      .trim()
      .toLowerCase()
  )
}

export const publicRegistrationEnabled =
  parseBooleanFlag(
    import.meta.env
      .APP_PUBLIC_REGISTRATION_ENABLED
  )
