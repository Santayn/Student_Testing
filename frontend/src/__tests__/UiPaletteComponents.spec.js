// @vitest-environment node

import { describe, expect, it } from 'vitest'

import uiAlertSource from '@/components/ui/UiAlert.vue?raw'
import uiButtonSource from '@/components/ui/UiButton.vue?raw'
import uiEmptyStateSource from '@/components/ui/UiEmptyState.vue?raw'
import uiFileInputSource from '@/components/ui/UiFileInput.vue?raw'
import uiTagSource from '@/components/ui/UiTag.vue?raw'

describe('Ui palette component contracts', () => {
  it('keeps stable variant classes on UiButton', () => {
    expect(uiButtonSource).toContain('st-ui-button--${variant}')
    expect(uiButtonSource).toContain('st-ui-link-button--${variant}')
  })

  it('keeps a stable semantic class on UiAlert', () => {
    expect(uiAlertSource).toContain('st-ui-alert--${variant}')
  })

  it('keeps a stable semantic class on UiTag', () => {
    expect(uiTagSource).toContain('st-ui-tag--${variant}')
  })

  it('keeps UiEmptyState compact state on a stable class', () => {
    expect(uiEmptyStateSource).toContain('st-ui-empty')
    expect(uiEmptyStateSource).toContain('st-ui-empty--compact')
  })

  it('keeps UiFileInput validation state and aria contract', () => {
    expect(uiFileInputSource).toContain('st-ui-file-input')
    expect(uiFileInputSource).toContain('st-ui-file-input--invalid')
    expect(uiFileInputSource).toContain(':aria-invalid="error ? \'true\' : undefined"')
  })
})
