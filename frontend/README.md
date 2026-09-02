# Client protection + best attempt results

Patch закрывает три задачи.

## 1. Серверная проверка доступности теста

`LectureDetailsView.vue` использует данные публичного учебного API:

```text
GET /public/learning/lectures/{lectureId}/tests
```

Backend возвращает для каждого теста:

```text
attemptsRemaining
attemptsAllowed
canResume
available
```

Frontend только отображает рассчитанное сервером состояние:

```text
attemptsUsed = max(0, attemptsAllowed - attemptsRemaining)
attemptsLeft = attemptsRemaining
```

Состояния:

```text
canResume=true        -> Продолжить тест
canResume=false + left>0 -> Пройти тест
left=0                -> кнопка disabled
backend available=false -> кнопка disabled
```

Frontend не обращается к административному `/tests/attempts` и не передает
`personId` для подсчета попыток. Backend остается единственным источником истины.

## 2. Лучшая попытка вместо student aggregation

Для STUDENT серверный `resultData.stats`, агрегирующий несколько attempts,
больше не используется как итог выбранного теста.

При выбранном test:

```text
1. максимальный stats.percent
2. при равенстве — максимальный stats.right
3. при равенстве — более поздний completedAt
4. при равенстве — больший attemptOrdinal
```

выбирается как лучшая попытка.

Все завершённые attempts при этом остаются на странице и доступны для
раскрытия. Лучшая отмечается badge `Лучшая попытка`.

Если выбрано несколько разных тестов, frontend принципиально не строит
общий процент между разными тестами и предлагает выбрать конкретный test.

Teacher/admin aggregation оставлена без изменения, потому что там сводка
может относиться к группе/нескольким студентам.

## 3. Student filter по test

Добавлен selector:

```text
Предмет -> Тест
```

Список test строится только из собственных completed attempts, которые
backend уже вернул через `/results/student/data` для текущего контекста.

При выборе test используется:

```text
GET /results/student/data?subjectId=...&testId=...
```

Перед запросом frontend проверяет, что `testId` действительно присутствует
в текущем списке результатов студента. Это частично компенсирует то, что
backend отдаёт `testId` приоритет над `subjectId` и сам не проверяет их связь.

## Файлы

```text
src/api/index.js
src/api/results.api.js
src/api/learning.api.js
src/views/lectures/LectureDetailsView.vue
src/views/results/ResultsView.vue
src/components/results/ResultAttemptCard.vue
```

## После замены

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```
