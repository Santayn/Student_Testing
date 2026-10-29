import { inject } from 'vue'

export const questionsWorkspaceKey = Symbol('teacher-questions-workspace')

export function useQuestionsWorkspaceContext() {
  const workspace = inject(questionsWorkspaceKey, null)
  if (!workspace) {
    throw new Error('Questions presentation requires QuestionsView container')
  }
  return workspace
}
