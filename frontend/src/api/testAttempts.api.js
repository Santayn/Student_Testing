import http from './http'

function positiveId(
  value,
  fieldName
) {
  const id = Number(value)

  if (
    !Number.isInteger(id) ||
    id <= 0
  ) {
    throw new TypeError(
      `${fieldName} должен быть положительным целым числом.`
    )
  }

  return id
}

export const testAttemptsApi = {
  /*
   * Намеренно не предоставляем обычному UI метод list() без фильтров.
   * Backend route /tests/attempts имеет слабый ownership-control,
   * поэтому клиентский слой допускает только пару:
   *
   * testAssignmentId + текущий personId.
   *
   * Это НЕ security boundary, но защищает frontend от случайного
   * вызова небезопасного GET /tests/attempts без фильтров.
   */
  getForAssignmentAndPerson(
    testAssignmentId,
    personId
  ) {
    return http.get(
      '/tests/attempts',
      {
        params: {
          testAssignmentId:
            positiveId(
              testAssignmentId,
              'testAssignmentId'
            ),

          personId:
            positiveId(
              personId,
              'personId'
            ),
        },
      }
    )
  },
}
