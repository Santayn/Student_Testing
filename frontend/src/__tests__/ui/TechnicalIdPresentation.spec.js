// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = (relativePath) =>
  readFileSync(resolve(process.cwd(), 'src', relativePath), 'utf8')

describe('technical IDs in ordinary UI', () => {
  it('uses semantic fallbacks instead of raw database ids in student-facing views', () => {
    const files = [
      'views/subjects/SubjectsView.vue',
      'views/subjects/SubjectDetailsView.vue',
      'views/lectures/SubjectLecturesView.vue',
      'views/lectures/LectureDetailsView.vue',
      'views/tests/TestView.vue',
      'views/results/ResultsView.vue',
      'components/results/ResultAttemptCard.vue',
      'navigation/navigation.js',
    ]

    const combined = files.map((relativePath) => readSource(relativePath)).join('\n')

    expect(combined).not.toMatch(
      /(Предмет|Лекция|Тест|Материал|Студент|Группа|Факультет|Вопрос|Вариант) #\$\{/
    )
    expect(readSource('views/subjects/SubjectDetailsView.vue')).not.toContain('`ID ${subject.id')
  })

  it('keeps ordinary teacher UI semantic while explicit admin and recovery ids remain allowed elsewhere', () => {
    const files = [
      'components/teacher/LectureEditorDrawer.vue',
      'components/teacher/LectureMaterialsManager.vue',
      'views/teacher/TeacherWorkloadView.vue',
      'views/teacher/CourseTemplatesView.vue',
      'views/teacher/LectureManagementView.vue',
      'views/teacher/TestEditorView.vue',
      'composables/teacher/useTeacherWorkloadPresentation.js',
      'composables/teacher/useTeacherTopicsData.js',
      'composables/lectures/useLectureManagementData.js',
    ]

    const combined = files.map((relativePath) => readSource(relativePath)).join('\n')

    expect(combined).not.toMatch(
      /(Предмет|Лекция|Тест|Материал|Группа|Тип нагрузки|Статус|назначение) #\$\{/
    )
  })
})
