export function listFromResponse(response) {
  const data = response?.data

  if (Array.isArray(data)) {
    return data
  }

  if (Array.isArray(data?.content)) {
    return data.content
  }

  if (Array.isArray(data?.items)) {
    return data.items
  }

  if (Array.isArray(data?.data)) {
    return data.data
  }

  return []
}

export function uniqueNumbers(values) {
  return [
    ...new Set(
      values
        .map(Number)
        .filter(
          (value) =>
            Number.isFinite(value) &&
            value > 0
        )
    ),
  ]
}
