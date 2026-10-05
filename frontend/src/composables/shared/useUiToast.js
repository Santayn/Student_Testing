import { useToast } from 'primevue/usetoast'

const severityMap = {
  success: 'success',
  info: 'info',
  warning: 'warn',
  warn: 'warn',
  danger: 'error',
  error: 'error',
  secondary: 'secondary',
}

export function useUiToast() {
  const toast = useToast()

  function show({
    variant = 'info',
    title = '',
    message = '',
    life = 3200,
    group,
    sticky = false,
  } = {}) {
    toast.add({
      severity: severityMap[variant] ?? 'info',
      summary: title || undefined,
      detail: message || undefined,
      life: sticky ? undefined : life,
      group,
    })
  }

  return {
    show,
    success: (message, title = 'Готово', options = {}) => show({ ...options, variant: 'success', title, message }),
    info: (message, title = 'Информация', options = {}) => show({ ...options, variant: 'info', title, message }),
    warning: (message, title = 'Внимание', options = {}) => show({ ...options, variant: 'warning', title, message }),
    error: (message, title = 'Ошибка', options = {}) => show({ ...options, variant: 'danger', title, message }),
  }
}
