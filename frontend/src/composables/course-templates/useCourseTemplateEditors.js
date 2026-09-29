import {
  computed,
  ref,
} from 'vue'

import {
  useOverlayForm,
} from '@/components/ui'

export function useCourseTemplateEditors({
  versions,
  selectedTemplate,
  canWorkWithTemplates,
  templateCreationAllowed,
  notice,
}) {
  const templateFormError = ref('')
  const versionFormError = ref('')

  function templateToForm(template = null) {
    return {
      id: template?.id ?? null,
      name: template?.name ?? '',
      publicVisible: template ? Boolean(template.publicVisible) : true,
    }
  }

  function nextVersionNumber() {
    return versions.value.reduce(
      (max, version) =>
        Math.max(max, Number(version.versionNumber ?? 0)),
      0
    ) + 1
  }

  function versionToForm(version = null) {
    return {
      id: version?.id ?? null,
      versionNumber: version
        ? Number(version.versionNumber ?? 1)
        : nextVersionNumber(),
      title: version?.title ?? selectedTemplate.value?.name ?? '',
      description: version?.description ?? '',
      changeNotes: version?.changeNotes ?? '',
      published: version ? Boolean(version.published) : false,
    }
  }

  const templateOverlay = useOverlayForm({
    createDefault: () => templateToForm(),
    mapEntity: templateToForm,
  })

  const versionOverlay = useOverlayForm({
    createDefault: () => versionToForm(),
    mapEntity: versionToForm,
  })

  const templateDrawerTitle = computed(() => {
    return templateOverlay.isCreate.value
      ? 'Новый шаблон курса'
      : 'Редактирование шаблона'
  })

  const versionDrawerTitle = computed(() => {
    return versionOverlay.isCreate.value
      ? 'Новая версия курса'
      : 'Редактирование версии'
  })

  function closeTemplateDrawerImmediately() {
    templateFormError.value = ''
    templateOverlay.closeImmediately()
  }

  function closeVersionDrawerImmediately() {
    versionFormError.value = ''
    versionOverlay.closeImmediately()
  }

  function openCreateTemplate() {
    if (!canWorkWithTemplates.value) {
      notice.value = {
        type: 'danger',
        message: 'Выберите предмет преподавателя.',
      }
      return
    }

    if (!templateCreationAllowed.value) {
      notice.value = {
        type: 'info',
        message: 'Новый шаблон должен создать сам преподаватель.',
      }
      return
    }

    closeVersionDrawerImmediately()
    templateFormError.value = ''
    templateOverlay.openCreate()
  }

  function openEditTemplate(template) {
    closeVersionDrawerImmediately()
    templateFormError.value = ''
    templateOverlay.openEdit(template)
  }

  function requestTemplateDrawerClose() {
    return templateOverlay.requestClose()
  }

  function handleTemplateDrawerVisibility(nextValue) {
    if (!nextValue) {
      requestTemplateDrawerClose()
    }
  }

  function discardTemplateDrawer() {
    templateOverlay.discardAndClose()
    templateFormError.value = ''
  }

  function openCreateVersion() {
    if (!selectedTemplate.value) {
      notice.value = {
        type: 'danger',
        message: 'Выберите шаблон курса.',
      }
      return
    }

    closeTemplateDrawerImmediately()
    versionFormError.value = ''
    versionOverlay.openCreate({
      versionNumber: nextVersionNumber(),
      title: selectedTemplate.value.name ?? '',
    })
  }

  function openEditVersion(version) {
    closeTemplateDrawerImmediately()
    versionFormError.value = ''
    versionOverlay.openEdit(version)
  }

  function requestVersionDrawerClose() {
    return versionOverlay.requestClose()
  }

  function handleVersionDrawerVisibility(nextValue) {
    if (!nextValue) {
      requestVersionDrawerClose()
    }
  }

  function discardVersionDrawer() {
    versionOverlay.discardAndClose()
    versionFormError.value = ''
  }

  function templateValidationMessage() {
    if (!canWorkWithTemplates.value) {
      return 'Выберите предмет преподавателя.'
    }

    const name = String(templateOverlay.form.name ?? '').trim()

    if (!name) {
      return 'Введите название шаблона.'
    }

    if (name.length > 200) {
      return 'Название шаблона не может быть длиннее 200 символов.'
    }

    return ''
  }

  function versionValidationMessage() {
    if (!selectedTemplate.value) {
      return 'Выберите шаблон курса.'
    }

    const versionNumber = Number(versionOverlay.form.versionNumber)
    const title = String(versionOverlay.form.title ?? '').trim()
    const description = String(versionOverlay.form.description ?? '').trim()
    const changeNotes = String(versionOverlay.form.changeNotes ?? '').trim()

    if (!Number.isInteger(versionNumber) || versionNumber <= 0) {
      return 'Номер версии должен быть целым числом больше нуля.'
    }

    if (!title) {
      return 'Введите название версии.'
    }

    if (title.length > 200) {
      return 'Название версии не может быть длиннее 200 символов.'
    }

    if (description.length > 2000) {
      return 'Описание версии не может быть длиннее 2000 символов.'
    }

    if (changeNotes.length > 2000) {
      return 'Примечания к изменениям не могут быть длиннее 2000 символов.'
    }

    const duplicate = versions.value.find(
      (version) =>
        Number(version.versionNumber) === versionNumber &&
        String(version.id) !== String(versionOverlay.form.id ?? '')
    )

    if (duplicate) {
      return 'Версия с таким номером уже существует в выбранном шаблоне.'
    }

    return ''
  }

  return {
    templateOverlay,
    versionOverlay,
    templateFormError,
    versionFormError,
    templateDrawerTitle,
    versionDrawerTitle,
    nextVersionNumber,
    closeTemplateDrawerImmediately,
    closeVersionDrawerImmediately,
    openCreateTemplate,
    openEditTemplate,
    requestTemplateDrawerClose,
    handleTemplateDrawerVisibility,
    discardTemplateDrawer,
    openCreateVersion,
    openEditVersion,
    requestVersionDrawerClose,
    handleVersionDrawerVisibility,
    discardVersionDrawer,
    templateValidationMessage,
    versionValidationMessage,
  }
}
