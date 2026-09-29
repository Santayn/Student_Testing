import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  teachingApi,
} from '@/api'

import {
  useOverlayForm,
} from '@/components/ui'

export function useAdminLoadTypeManager({
  loadTypes,
  reloadLoadTypes,
  showNotice,
} = {}) {
  const loadTypeFormError = ref('')
  const loadTypeSearchQuery = ref('')

  const {
    form: loadTypeForm,
    model: loadTypeDialogModel,
    isCreate: loadTypeIsCreate,
    dirty: loadTypeDirty,
    saving: loadTypeSaving,
    confirmCloseVisible: loadTypeCloseConfirmVisible,
    openCreate: openCreateLoadTypeForm,
    openEdit: openEditLoadTypeForm,
    discardAndClose: discardLoadTypeAndClose,
    continueEditing: continueLoadTypeEditing,
    beginSaving: beginLoadTypeSaving,
    finishSaving: finishLoadTypeSaving,
    failSaving: failLoadTypeSaving,
    resetToBaseline: resetLoadTypeToBaseline,
  } = useOverlayForm({
    createDefault: () => ({
      id: null,
      name: '',
      description: '',
    }),
    mapEntity: (loadType) => ({
      id: loadType?.id ?? null,
      name: loadType?.name ?? '',
      description: loadType?.description ?? '',
    }),
  })

  const loadTypeDialogTitle = computed(() => {
    return 'Типы нагрузки'
  })

  const loadTypeEditorTitle = computed(() => {
    return loadTypeIsCreate.value
      ? 'Новый тип нагрузки'
      : 'Редактирование типа'
  })

  const filteredLoadTypes = computed(() => {
    const query = loadTypeSearchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    return loadTypes.value
      .filter((item) => {
        if (!query) {
          return true
        }

        return [
          item.name,
          item.description,
        ]
          .filter(Boolean)
          .join(' ')
          .toLocaleLowerCase('ru-RU')
          .includes(query)
      })
      .sort((left, right) =>
        String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
          'ru'
        )
      )
  })

  function loadTypeValidationMessage() {
    const name = String(
      loadTypeForm.name ?? ''
    ).trim()
    const description = String(
      loadTypeForm.description ?? ''
    ).trim()

    if (!name) {
      return 'Введите название типа нагрузки.'
    }

    if (name.length > 100) {
      return 'Название типа нагрузки не может быть длиннее 100 символов.'
    }

    if (description.length > 1000) {
      return 'Описание типа нагрузки не может быть длиннее 1000 символов.'
    }

    const normalizedName = name.toLocaleLowerCase(
      'ru-RU'
    )
    const duplicate = loadTypes.value.find(
      (item) =>
        String(item.name ?? '')
          .trim()
          .toLocaleLowerCase('ru-RU') ===
          normalizedName &&
        String(item.id) !==
          String(loadTypeForm.id ?? '')
    )

    if (duplicate) {
      return `Тип нагрузки «${name}» уже существует.`
    }

    return ''
  }

  function openLoadTypeManager() {
    loadTypeFormError.value = ''
    loadTypeSearchQuery.value = ''
    openCreateLoadTypeForm()
  }

  function startNewLoadType() {
    if (loadTypeDirty.value) {
      loadTypeFormError.value =
        'Сначала сохраните или отмените изменения текущего типа нагрузки.'
      return
    }

    loadTypeFormError.value = ''
    openCreateLoadTypeForm()
  }

  function editLoadType(loadType) {
    if (loadTypeDirty.value) {
      loadTypeFormError.value =
        'Сначала сохраните или отмените изменения текущего типа нагрузки.'
      return
    }

    loadTypeFormError.value = ''
    openEditLoadTypeForm(loadType)
  }

  function cancelLoadTypeChanges() {
    resetLoadTypeToBaseline()
    loadTypeFormError.value = ''
  }

  async function saveLoadType() {
    if (loadTypeSaving.value) {
      return
    }

    loadTypeFormError.value = ''
    const validation =
      loadTypeValidationMessage()

    if (validation) {
      loadTypeFormError.value = validation
      return
    }

    const creating = loadTypeIsCreate.value

    beginLoadTypeSaving()

    const payload = {
      name: String(
        loadTypeForm.name
      ).trim(),
      description:
        String(
          loadTypeForm.description ?? ''
        ).trim() || null,
    }

    try {
      const response = creating
        ? await teachingApi.createLoadType(
            payload
          )
        : await teachingApi.updateLoadType(
            loadTypeForm.id,
            payload
          )

      const saved = {
        id: response.data?.id ??
          loadTypeForm.id,
        name: response.data?.name ??
          payload.name,
        description:
          response.data?.description ??
          payload.description ?? '',
      }

      await reloadLoadTypes()

      if (creating) {
        openEditLoadTypeForm(saved)
      } else {
        finishLoadTypeSaving({
          close: false,
          values: saved,
        })
      }

      showNotice(
        'success',
        creating
          ? 'Тип нагрузки создан.'
          : 'Тип нагрузки обновлён.'
      )
    } catch (error) {
      loadTypeFormError.value =
        getApiErrorMessage(
          error,
          creating
            ? 'Не удалось создать тип нагрузки.'
            : 'Не удалось обновить тип нагрузки.'
        )
      failLoadTypeSaving()
    }
  }

  return {
    loadTypeForm,
    loadTypeDialogModel,
    loadTypeIsCreate,
    loadTypeDirty,
    loadTypeSaving,
    loadTypeCloseConfirmVisible,
    loadTypeFormError,
    loadTypeSearchQuery,
    loadTypeDialogTitle,
    loadTypeEditorTitle,
    filteredLoadTypes,
    loadTypeValidationMessage,
    openLoadTypeManager,
    startNewLoadType,
    editLoadType,
    cancelLoadTypeChanges,
    saveLoadType,
    continueLoadTypeEditing,
    discardLoadTypeAndClose,
  }
}
