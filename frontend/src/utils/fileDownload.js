/** Имя файла из Content-Disposition: сначала filename* (UTF-8, кириллица), потом filename. */
export function responseFileName(response, fallback) {
  const header =
    response?.headers?.['content-disposition'] ??
    response?.headers?.get?.('content-disposition') ??
    ''

  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i)
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].replace(/"/g, ''))
    } catch {
      // некорректная кодировка — берём запасное имя ниже
    }
  }

  const simple = header.match(/filename="?([^";]+)"?/i)
  return simple?.[1] || fallback
}

/** Сохраняет ответ с responseType: 'blob' как файл в браузере. */
export function saveBlobResponse(response, fallbackName) {
  const blob = response?.data instanceof Blob ? response.data : new Blob([response?.data ?? ''])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = responseFileName(response, fallbackName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
