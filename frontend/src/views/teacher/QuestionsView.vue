<script setup>
import { provide } from 'vue'
import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'
import { UiAlert } from '@/components/ui'
import ResponsivePresentation from '@/components/layout/ResponsivePresentation.vue'
import QuestionsOverlays from './questions/QuestionsOverlays.vue'
import { useQuestionsWorkspace } from '@/composables/questions/useQuestionsWorkspace'
import { questionsWorkspaceKey } from '@/composables/questions/questionsWorkspaceContext'

const workspace = useQuestionsWorkspace()
provide(questionsWorkspaceKey, workspace)
</script>

<template>
  <TeacherPageShell
    title="Вопросы предмета"
    subtitle="Создавайте, импортируйте и редактируйте вопросы выбранной темы."
  >
    <UiAlert
      v-if="workspace.notice.message"
      :variant="workspace.notice.type"
      :message="workspace.notice.message"
      closable
      @close="workspace.notice.message = ''"
    />

    <ResponsivePresentation view-key="teacher/questions" />

    <!-- Always mounted: resizing must not destroy edits / dialogs. -->
    <QuestionsOverlays />
  </TeacherPageShell>
</template>
