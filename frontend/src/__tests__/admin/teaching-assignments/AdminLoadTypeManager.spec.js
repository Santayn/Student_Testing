import {
  ref,
} from 'vue'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const {
  createLoadType,
  updateLoadType,
} = vi.hoisted(() => ({
  createLoadType: vi.fn(),
  updateLoadType: vi.fn(),
}))

vi.mock('@/api', () => ({
  teachingApi: {
    createLoadType,
    updateLoadType,
  },
  getApiErrorMessage: (_error, fallback) => fallback,
}))

import {
  useAdminLoadTypeManager,
} from '@/composables/admin/teaching-assignments/useAdminLoadTypeManager'

function setup() {
  const loadTypes = ref([
    {
      id: 2,
      name: 'Практика',
      description: 'Практические занятия',
    },
    {
      id: 1,
      name: 'Лекции',
      description: 'Лекционные занятия',
    },
  ])
  const reloadLoadTypes = vi.fn()
  const showNotice = vi.fn()

  return {
    manager: useAdminLoadTypeManager({
      loadTypes,
      reloadLoadTypes,
      showNotice,
    }),
    loadTypes,
    reloadLoadTypes,
    showNotice,
  }
}

beforeEach(() => {
  createLoadType.mockReset()
  updateLoadType.mockReset()
})

describe('useAdminLoadTypeManager', () => {
  it('opens in create mode, filters load types locally and validates duplicates', () => {
    const { manager } = setup()

    manager.openLoadTypeManager()
    manager.loadTypeSearchQuery.value = 'лекц'

    expect(manager.loadTypeDialogModel.value).toBe(true)
    expect(manager.loadTypeIsCreate.value).toBe(true)
    expect(manager.filteredLoadTypes.value.map((item) => item.id)).toEqual([1])

    manager.loadTypeForm.name = ' лекции '
    expect(manager.loadTypeValidationMessage()).toContain('уже существует')
  })

  it('does not switch records while current load type has unsaved changes', () => {
    const { manager, loadTypes } = setup()

    manager.openLoadTypeManager()
    manager.loadTypeForm.name = 'Черновик'
    manager.editLoadType(loadTypes.value[0])

    expect(manager.loadTypeIsCreate.value).toBe(true)
    expect(manager.loadTypeFormError.value).toContain('Сначала сохраните')

    manager.cancelLoadTypeChanges()
    manager.editLoadType(loadTypes.value[0])

    expect(manager.loadTypeIsCreate.value).toBe(false)
    expect(manager.loadTypeForm).toMatchObject({
      id: 2,
      name: 'Практика',
    })
  })

  it('creates a load type, refreshes the read-side list and keeps the saved entity open for editing', async () => {
    const {
      manager,
      reloadLoadTypes,
      showNotice,
    } = setup()

    createLoadType.mockResolvedValue({
      data: {
        id: 7,
        name: 'Лабораторные',
        description: 'Лабораторная работа',
      },
    })

    manager.openLoadTypeManager()
    manager.loadTypeForm.name = '  Лабораторные  '
    manager.loadTypeForm.description = '  Лабораторная работа  '

    await manager.saveLoadType()

    expect(createLoadType).toHaveBeenCalledWith({
      name: 'Лабораторные',
      description: 'Лабораторная работа',
    })
    expect(reloadLoadTypes).toHaveBeenCalledTimes(1)
    expect(manager.loadTypeIsCreate.value).toBe(false)
    expect(manager.loadTypeForm).toMatchObject({
      id: 7,
      name: 'Лабораторные',
      description: 'Лабораторная работа',
    })
    expect(showNotice).toHaveBeenCalledWith(
      'success',
      'Тип нагрузки создан.'
    )
  })

  it('updates the selected load type and preserves the editor after save', async () => {
    const {
      manager,
      loadTypes,
      reloadLoadTypes,
      showNotice,
    } = setup()

    updateLoadType.mockResolvedValue({
      data: {
        id: 2,
        name: 'Практические занятия',
        description: null,
      },
    })

    manager.openLoadTypeManager()
    manager.editLoadType(loadTypes.value[0])
    manager.loadTypeForm.name = ' Практические занятия '
    manager.loadTypeForm.description = ' '

    await manager.saveLoadType()

    expect(updateLoadType).toHaveBeenCalledWith(2, {
      name: 'Практические занятия',
      description: null,
    })
    expect(reloadLoadTypes).toHaveBeenCalledTimes(1)
    expect(manager.loadTypeDialogModel.value).toBe(true)
    expect(manager.loadTypeDirty.value).toBe(false)
    expect(showNotice).toHaveBeenCalledWith(
      'success',
      'Тип нагрузки обновлён.'
    )
  })
})
