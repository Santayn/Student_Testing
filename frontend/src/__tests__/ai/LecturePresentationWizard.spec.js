import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { effectScope, ref } from 'vue'

import { aiApi } from '@/api/ai.api'
import { useLecturePresentation } from '@/composables/lectures/useLecturePresentation'

vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  lecturesApi: {
    getOne: vi.fn(),
    getMaterials: vi.fn(),
  },
}))

vi.mock('@/api/ai.api', () => ({
  aiApi: {
    createPresentationOutline: vi.fn(),
    buildPresentation: vi.fn(),
    searchImages: vi.fn(),
    previewImage: vi.fn(),
  },
}))

vi.mock('@/composables/ai/useAiJob', () => ({
  useAiJob: () => ({
    track: async (request) => (await request).data,
    error: { value: '' },
  }),
}))

const found = (name, title = `File:${name} diagram.png`) => ({
  source: 'wikimedia',
  title,
  imageUrl: `https://upload.wikimedia.org/${name}.jpg`,
  author: 'a',
  license: 'CC0',
  licenseUrl: '',
  pageUrl: '',
})

describe('Lecture presentation wizard', () => {
  let scope

  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = vi.fn(() => 'blob:preview')
    URL.revokeObjectURL = vi.fn()
    scope = effectScope()
  })

  afterEach(() => {
    scope.stop()
  })

  it('sends text and image amounts and picks the first found image for search slides', async () => {
    aiApi.createPresentationOutline.mockResolvedValue({
      data: {
        result: {
          title: 'Спецификация ПО',
          slides: [
            { layout: 'bullets', title: 'Модели UML', bullets: ['Модель использования'],
              image: { mode: 'search', query: 'uml diagram' } },
            { layout: 'process', title: 'Отладка', steps: ['Найти', 'Исправить'], image: { mode: 'none' } },
          ],
        },
      },
    })
    // Первая находка не похожа на запрос (случайное фото) — автоподбор её пропускает.
    aiApi.searchImages.mockResolvedValue({
      data: [found('truck', 'File:Tow truck.jpg'), found('uml'), found('uml2')],
      headers: {},
    })
    aiApi.previewImage.mockResolvedValue({ data: new Blob() })

    const wizard = scope.run(() => useLecturePresentation(ref(7)))
    wizard.settings.textAmount = 'short'
    wizard.settings.imageAmount = 'many'

    await wizard.generateOutline()

    expect(aiApi.createPresentationOutline).toHaveBeenCalledWith(7, expect.objectContaining({
      textAmount: 'short',
      imageAmount: 'many',
    }))
    expect(aiApi.searchImages).toHaveBeenCalledTimes(1)
    expect(aiApi.searchImages).toHaveBeenCalledWith('uml diagram')
    const [withImage, withoutImage] = wizard.outline.slides
    expect(withImage.image.selected).toMatchObject({ kind: 'search', url: 'https://upload.wikimedia.org/uml.jpg' })
    expect(withoutImage.image.selected).toBeNull()

    await vi.waitFor(() => expect(withImage.image.previewUrl).toBe('blob:preview'))
  })

  it('edits a table slide as text and sends it back as columns and rows', async () => {
    aiApi.createPresentationOutline.mockResolvedValue({
      data: {
        result: {
          title: 'Трансляторы',
          slides: [{
            layout: 'table',
            title: 'Обзор решений',
            table: { columns: ['Решение', 'Представление'], rows: [['Roslyn', 'дерево, CIL']] },
            image: { mode: 'none' },
          }],
        },
      },
    })
    aiApi.buildPresentation.mockResolvedValue({ data: { result: { materialId: 1 } } })

    const wizard = scope.run(() => useLecturePresentation(ref(7)))
    await wizard.generateOutline()
    const [slide] = wizard.outline.slides
    expect(slide.tableText).toBe('Решение | Представление\nRoslyn | дерево, CIL')

    slide.tableText += '\n| LLVM | LLVM IR |'
    await wizard.build()

    const [, payload] = aiApi.buildPresentation.mock.calls[0]
    expect(payload.outline.slides[0].table).toEqual({
      columns: ['Решение', 'Представление'],
      rows: [['Roslyn', 'дерево, CIL'], ['LLVM', 'LLVM IR']],
    })
  })
})
