/**
 * Keep a native scrollbar gutter ONLY when the document itself overflows.
 * `scrollbar-gutter: stable` globally reserves a blank stripe even on short
 * pages. During scroll locks preserve the last *unlocked* overflow reading:
 * the modal's own scrollbar must never be mistaken for a document scrollbar.
 *
 * CSS uses st-document-scrollable to hold layout width while PrimeVue or the
 * navigation sheet temporarily locks the document. Does not style scrollbars.
 */
export function isDocumentScrollable(root) {
  return Boolean(root && root.scrollHeight > root.clientHeight + 1)
}

export function initDocumentScrollbar(doc = document, view = window) {
  const root = doc.documentElement
  const body = doc.body
  const app = doc.getElementById('app')
  if (!root || !body) return () => {}

  let frame = 0
  let disposed = false
  const isLocked = () =>
    root.classList.contains('navigation-sheet-open') ||
    body.classList.contains('navigation-sheet-open') ||
    body.classList.contains('p-overflow-hidden') ||
    root.classList.contains('p-overflow-hidden')

  function update() {
    frame = 0
    if (disposed || isLocked()) return
    root.classList.toggle('st-document-scrollable', isDocumentScrollable(root))
  }

  function schedule() {
    if (disposed || frame) return
    frame = view.requestAnimationFrame(update)
  }

  // PrimeVue/Dialog and our More sheet lock by changing CSS classes. Observe
  // their release so we can recompute after the original page can scroll again.
  const mutations = new MutationObserver(schedule)
  mutations.observe(root, { attributes: true, attributeFilter: ['class'] })
  mutations.observe(body, { attributes: true, attributeFilter: ['class'] })

  const resize = typeof ResizeObserver === 'function'
    ? new ResizeObserver(schedule)
    : null
  if (resize) {
    if (app) resize.observe(app)
    resize.observe(body)
  }
  view.addEventListener('resize', schedule, { passive: true })
  schedule()

  return () => {
    disposed = true
    if (frame) view.cancelAnimationFrame(frame)
    mutations.disconnect()
    resize?.disconnect()
    view.removeEventListener('resize', schedule)
    root.classList.remove('st-document-scrollable')
  }
}
