// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'
import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const sourcePath = (...segments) =>
  resolve(
    process.cwd(),
    'src',
    ...segments
  )

const testEditor = readFileSync(
  sourcePath(
    'views',
    'teacher',
    'TestEditorView.vue'
  ),
  'utf8'
)

const guardSource = readFileSync(
  sourcePath(
    'composables',
    'shared',
    'useUnsavedNavigationGuard.js'
  ),
  'utf8'
)

const editorStateSource = readFileSync(
  sourcePath(
    'composables',
    'tests',
    'useTestEditorState.js'
  ),
  'utf8'
)

const saveFlowSource = readFileSync(
  sourcePath(
    'composables',
    'tests',
    'useTestEditorSaveFlow.js'
  ),
  'utf8'
)

describe('TestEditor unsaved navigation integration', () => {
  it('uses the shared route-level unsaved changes guard', () => {
    expect(testEditor).toContain(
      'useUnsavedNavigationGuard'
    )
    expect(testEditor).toContain(
      'UiUnsavedChangesConfirm'
    )
    expect(testEditor).toContain(
      'navigationConfirmVisible'
    )
    expect(testEditor).toContain(
      '@discard="discardAndNavigate"'
    )
  })

  it('tracks the complete editable test state through the shared editor-state composable', () => {
    expect(testEditor).toContain(
      'useTestEditorState'
    )
    expect(editorStateSource).toContain(
      'const editorState = computed'
    )
    expect(editorStateSource).toContain(
      'membershipId:'
    )
    expect(editorStateSource).toContain(
      'groupIds,'
    )
    expect(editorStateSource).toContain(
      'questionCount:'
    )
    expect(editorStateSource).toContain(
      'availableUntil:'
    )
  })

  it('keeps initial form values separate from asynchronously loaded context', () => {
    expect(testEditor).toContain(
      'markInitialContextClean'
    )
    expect(editorStateSource).toContain(
      'nextBaseline.form ='
    )
    expect(editorStateSource).toContain(
      'editorBaseline.value.form'
    )
  })

  it('marks the editor clean only after a fully successful create flow', () => {
    expect(testEditor).toContain(
      'useTestEditorSaveFlow'
    )

    const successMessageIndex =
      saveFlowSource.indexOf(
        'создан и назначен выбранным группам.'
      )
    const cleanIndex =
      saveFlowSource.indexOf(
        'markEditorClean()',
        successMessageIndex
      )
    const catchIndex =
      saveFlowSource.indexOf(
        '} catch (error)',
        successMessageIndex
      )

    expect(successMessageIndex).toBeGreaterThan(-1)
    expect(cleanIndex).toBeGreaterThan(successMessageIndex)
    expect(cleanIndex).toBeLessThan(catchIndex)
  })

  it('keeps browser unload handling inside the shared composable', () => {
    expect(guardSource).toContain(
      "window.addEventListener(\n      'beforeunload'"
    )
    expect(guardSource).toContain(
      'event.preventDefault()'
    )
    expect(testEditor).not.toContain(
      "addEventListener('beforeunload'"
    )
  })
})
