import { describe, expect, it } from 'vitest'
import { getWorkspaceNavigationNodes } from '@/navigation'
import { getRailFlyoutLayout } from '@/navigation/railFlyout'

const roles = ['STUDENT', 'TEACHER', 'ADMIN']

describe('tablet rail root navigation', () => {
  it('uses only canonical role-scoped groups and direct links', () => {
    for (const role of roles) {
      const nodes = getWorkspaceNavigationNodes(role)
      expect(nodes.length).toBeGreaterThan(0)
      for (const node of nodes) {
        if (node.type === 'link') {
          expect(node.route?.name).toBeTruthy()
        } else {
          expect(node.type).toBe('group')
          expect(node.items.length).toBeGreaterThan(1)
          expect(node.items.every((item) => Boolean(item.route?.name))).toBe(true)
        }
      }
    }
  })

  it('never places a flyout outside the visible viewport', () => {
    for (const viewport of [
      { viewportWidth: 641, viewportHeight: 420 },
      { viewportWidth: 768, viewportHeight: 1024 },
      { viewportWidth: 960, viewportHeight: 500 },
    ]) {
      const layout = getRailFlyoutLayout(
        { right: 88, top: 390 },
        { ...viewport, itemCount: 9 }
      )
      const left = parseFloat(layout.left)
      const top = parseFloat(layout.top)
      const width = parseFloat(layout.width)
      const height = parseFloat(layout.maxHeight)
      expect(left).toBeGreaterThanOrEqual(12)
      expect(left + width).toBeLessThanOrEqual(viewport.viewportWidth - 12)
      expect(top).toBeGreaterThanOrEqual(12)
      expect(top + height).toBeLessThanOrEqual(viewport.viewportHeight - 12)
    }
  })
})
