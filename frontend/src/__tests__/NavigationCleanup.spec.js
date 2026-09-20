import {
  describe,
  expect,
  it,
} from 'vitest'

import footerSource from '@/components/layout/AppFooter.vue?raw'

import {
  publicRoutes,
} from '@/router/routes/public'

const subjectLectureViews = import.meta.glob(
  '../views/**/SubjectLecturesView.vue'
)

describe('frontend navigation cleanup', () => {
  it('provides a real public about route', () => {
    const aboutRoute = publicRoutes.find(
      (route) => route.name === 'about'
    )

    expect(aboutRoute).toBeTruthy()
    expect(aboutRoute.path).toBe('/about')
    expect(aboutRoute.meta?.public).toBe(true)
  })

  it('links footer to the named about route', () => {
    expect(footerSource).toContain(
      ":to=\"{ name: 'about' }\""
    )
    expect(footerSource).not.toContain(
      'to="/about"'
    )
  })

  it('keeps only the routed SubjectLecturesView implementation', () => {
    expect(
      Object.keys(subjectLectureViews).sort()
    ).toEqual([
      '../views/lectures/SubjectLecturesView.vue',
    ])
  })
})
