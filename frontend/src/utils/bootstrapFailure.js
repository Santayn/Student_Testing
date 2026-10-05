const ERROR_TITLE = 'Ошибка запуска — Student Testing'

function applyContainerStyles(container) {
  Object.assign(container.style, {
    minHeight: '100vh',
    display: 'grid',
    placeItems: 'center',
    padding: '24px',
    boxSizing: 'border-box',
    background: 'var(--st-page-bg, #f5f7fb)',
    color: 'var(--st-text, #172033)',
    fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  })
}

function applyPanelStyles(panel) {
  Object.assign(panel.style, {
    width: 'min(560px, 100%)',
    padding: '28px',
    border: '1px solid var(--st-border, #d8dee9)',
    borderRadius: '16px',
    background: 'var(--st-surface, #ffffff)',
    boxShadow: '0 16px 40px rgba(15, 23, 42, 0.12)',
  })
}

export function renderBootstrapFailure(
  error,
  {
    root = document.getElementById('app'),
    reload = () => window.location.reload(),
  } = {}
) {
  console.error(
    'Не удалось запустить приложение:',
    error
  )

  document.title = ERROR_TITLE

  if (!root) {
    return false
  }

  const panel = document.createElement('section')
  panel.setAttribute('role', 'alert')
  panel.setAttribute('aria-labelledby', 'bootstrap-failure-title')
  applyPanelStyles(panel)

  const title = document.createElement('h1')
  title.id = 'bootstrap-failure-title'
  title.tabIndex = -1
  title.textContent = 'Не удалось запустить приложение'
  Object.assign(title.style, {
    margin: '0 0 12px',
    fontSize: 'clamp(1.5rem, 4vw, 2rem)',
    lineHeight: '1.2',
  })

  const message = document.createElement('p')
  message.textContent =
    'Произошла непредвиденная ошибка при запуске. Попробуйте перезагрузить страницу.'
  Object.assign(message.style, {
    margin: '0 0 20px',
    lineHeight: '1.6',
  })

  const retry = document.createElement('button')
  retry.type = 'button'
  retry.textContent = 'Повторить'
  Object.assign(retry.style, {
    minHeight: '44px',
    padding: '10px 18px',
    border: '1px solid currentColor',
    borderRadius: '10px',
    cursor: 'pointer',
    font: 'inherit',
  })
  retry.addEventListener('click', reload)

  panel.append(title, message, retry)

  root.replaceChildren(panel)
  applyContainerStyles(root)

  title.focus()
  return true
}
