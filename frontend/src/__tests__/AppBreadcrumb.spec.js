import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  mount,
} from '@vue/test-utils'

let currentRoute

vi.mock('vue-router', () => ({
  useRoute: () => currentRoute,
}))

import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'

function mountBreadcrumb() {
  return mount(AppBreadcrumb, {
    global: {
      stubs: {
        RouterLink: {
          name: 'RouterLink',
          props: {
            to: {
              type: [String, Object],
              required: true,
            },
          },
          template:
            '<a class="router-link-stub"><slot /></a>',
        },
      },
    },
  })
}

describe('AppBreadcrumb', () => {
  beforeEach(() => {
    currentRoute = {
      name: 'home',
      params: {},
      query: {},
      meta: {
        breadcrumbKey: 'home',
      },
    }
  })

  it('does not duplicate a top-level page title with a one-item breadcrumb', () => {
    const wrapper = mountBreadcrumb()

    expect(
      wrapper.find('.app-breadcrumb').exists()
    ).toBe(false)
  })

  it('renders a deep route as links followed by one current item', () => {
    currentRoute = {
      name: 'lecture-details',
      params: {
        lectureId: '9',
      },
      query: {
        subjectId: '42',
        facultyId: '3',
      },
      meta: {
        breadcrumbKey:
          'lecture-details',
      },
    }

    const wrapper = mountBreadcrumb()

    expect(
      wrapper.find('.app-breadcrumb').exists()
    ).toBe(true)

    expect(wrapper.text()).toContain('Предметы')
    expect(wrapper.text()).toContain('Предмет #42')
    expect(wrapper.text()).toContain('Лекции')
    expect(wrapper.text()).toContain('Лекция #9')

    const links = wrapper.findAllComponents({
      name: 'RouterLink',
    })

    expect(links).toHaveLength(3)
    expect(links[0].props('to')).toEqual({
      name: 'subjects',
      query: {
        facultyId: '3',
      },
    })

    expect(
      wrapper
        .find('[aria-current="page"]')
        .text()
    ).toBe('Лекция #9')
  })

  it('builds the full test hierarchy when navigation carries subject and lecture ids', () => {
    currentRoute = {
      name: 'test',
      params: {
        testId: '17',
      },
      query: {
        assignmentId: '5',
        subjectId: '42',
        lectureId: '9',
      },
      meta: {
        breadcrumbKey: 'test',
      },
    }

    const wrapper = mountBreadcrumb()

    expect(
      wrapper
        .findAll('.app-breadcrumb__item')
        .map((item) => item.text())
    ).toEqual([
      'Предметы',
      'Предмет #42',
      'Лекции',
      'Лекция #9',
      'Тест #17',
    ])
  })
})
