// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
const source = (...parts) => readFileSync(resolve(process.cwd(), 'src', ...parts), 'utf8')
describe('UI Stage 3 teacher experience contracts', () => {
  it('provides task-oriented teacher actions on Home', () => { const s=source('views','HomeView.vue'); expect(s).toContain('teacherActions'); expect(s).toContain("name: 'teacher-test-create'"); expect(s).toContain("name: 'teacher-workload'"); })
  it('keeps topic CRUD secondary', () => { const s=source('views','teacher','TopicLibraryView.vue'); expect(s).toContain('UiActionMenu'); expect(s).toContain('topicActionItems'); })
  it('presents test authoring as four steps', () => { const s=source('views','teacher','TestEditorView.vue'); for (const step of ['Шаг 1 · Контекст','Шаг 2 · Параметры теста','Шаг 3 · Правила отбора вопросов','Шаг 4 · Публикация и доступ']) expect(s).toContain(step); expect(s).toContain('questionPreviewOpen'); })
  it('reveals versions only after template selection', () => { const s=source('views','teacher','CourseTemplatesView.vue'); expect(s).toContain('v-if="selectedTemplate"'); })
  it('groups teacher result filters', () => { const s=source('views','results','ResultsView.vue'); expect(s).toContain('1. Контекст теста'); expect(s).toContain('2. Аудитория'); expect(s).toContain('v-if="!teacherMode || resultData"'); })
})
