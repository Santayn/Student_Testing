function isNetworkError(error) {
  const isAxiosError =
    error?.isAxiosError === true ||
    Boolean(error?.config)

  return Boolean(
    isAxiosError &&
    !error?.response
  )
}

function responseStatus(error) {
  const status = Number(
    error?.response?.status
  )

  return Number.isFinite(status)
    ? status
    : 0
}

function operationFallback(operation) {
  switch (operation) {
    case 'login':
      return 'Не удалось войти в систему'
    case 'register':
      return 'Не удалось зарегистрироваться'
    case 'password':
      return 'Не удалось изменить пароль'
    default:
      return 'Не удалось выполнить запрос'
  }
}

export function getAuthErrorMessage(
  error,
  operation = 'generic'
) {
  if (error?.code === 'ECONNABORTED') {
    return 'Сервер слишком долго отвечает. Попробуйте ещё раз.'
  }

  if (isNetworkError(error)) {
    return 'Не удалось связаться с сервером. Проверьте подключение и повторите попытку.'
  }

  const status = responseStatus(error)

  if (status === 429) {
    return 'Слишком много попыток. Попробуйте позже.'
  }

  if (operation === 'login') {
    if (status === 401) {
      return 'Неверный логин или пароль.'
    }

    if (status === 400 || status === 422) {
      return 'Проверьте логин и пароль и повторите попытку.'
    }
  }

  if (operation === 'register') {
    if (status === 409) {
      return 'Пользователь с таким логином уже существует.'
    }

    if (status === 403) {
      return 'Самостоятельная регистрация отключена. Обратитесь к администратору.'
    }

    if (status === 400 || status === 422) {
      return 'Проверьте логин и пароль и повторите попытку.'
    }
  }

  if (operation === 'password') {
    if (status === 401) {
      return 'Текущий пароль указан неверно.'
    }

    if (status === 400 || status === 422) {
      return 'Проверьте текущий и новый пароль и повторите попытку.'
    }
  }

  if (status === 401) {
    return 'Сессия завершена. Войдите в систему снова.'
  }

  if (status === 403) {
    return 'Недостаточно прав для выполнения операции.'
  }

  if (status >= 500) {
    return 'Сервис временно недоступен. Попробуйте позже.'
  }

  return operationFallback(operation)
}
