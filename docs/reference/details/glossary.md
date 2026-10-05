# Термины и сокращения

| Термин | Значение |
|---|---|
| `User` | Учётная запись: login, password hash, роли, permissions, active state и ссылка на `Person`. |
| `Person` | Человек в академической модели: имя, фамилия, дата рождения, email, телефон. |
| `Role` | Системная роль пользователя (`USER`, `STUDENT`, `TEACHER`, `ADMIN`). Не путать с числовым `role` внутри membership. |
| Permission | Машинно-читаемое право вида `tests.manage` или `lectures.read`. |
| Membership | Связь `Person` с факультетом, группой или предметом и её жизненный цикл. |
| `FacultySubject` | Связь факультета и предмета. |
| `SubjectMembership` | Связь человека с предметом; в текущих teacher-flow используется для преподавателя. |
| `TeachingAssignment` | Учебная нагрузка: преподаватель/предмет/группа/период/тип нагрузки/часы. |
| `TeachingAssignmentEnrollment` | Включение конкретного студента в учебное назначение. |
| `LectureAssignment` | Назначение лекции внутри `TeachingAssignment`. |
| `StudentLectureProgress` | Прогресс студента по назначенной лекции. |
| `CourseTemplate` | Логический шаблон курса для предмета. |
| `CourseVersion` | Версия шаблона курса с собственным номером и состоянием публикации. |
| `Lecture` | Лекция курса. Физическая таблица называется `CourseLectures`. |
| `CourseLectureId` | Историческое/контрактное имя ID сущности `Lecture`; отдельной Entity `CourseLecture` нет. |
| `Topic` | Тематика/тема для учебного контента и банка вопросов. Физическая таблица — `LectureTopics`. |
| `Question` | Вопрос теста или банка вопросов. Физическая таблица — `TestQuestions`. |
| `Test` | Определение теста: название, правила отбора, лимит попыток и др. |
| `TestAssignment` | Контекст назначения теста: global/course version/lecture/teaching assignment, период и статус. |
| `TestAttempt` | Конкретное прохождение назначенного теста человеком. |
| `QuestionResponse` | Ответ конкретной попытки на конкретный вопрос. |
| `SelectedOption` | Выбранный вариант ответа в `QuestionResponse`. |
| Result / Результат | Не отдельная JPA Entity. API результатов строится из `TestAttempt`, `QuestionResponse` и связанного учебного контекста. |
| `public/learning` | Student-facing API. Несмотря на слово `public`, эти маршруты требуют authentication. |
| Workspace | Рабочее пространство frontend для `STUDENT`, `TEACHER` или `ADMIN`. |
| Assignment | Перегруженный термин: может означать `TeachingAssignment`, `LectureAssignment` или `TestAssignment`; всегда уточняйте тип. |
| DTO | Data Transfer Object — внешний request/response-контракт API. |
| SPA | Single Page Application — frontend Vue. |
| API | REST API под `/api/v1`. |
| LLM | Локальная языковая модель, опционально используемая для проверки текстовых ответов. |

Подробнее о предметной модели: [Technical: модель данных](../../technical/details/data-model.md).
