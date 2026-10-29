import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import http from '@/api/http'
import { aiApi } from '@/api/ai.api'
import { learningApi } from '@/api/learning.api'
import { lecturesApi } from '@/api/lectures.api'
import { API_TIMEOUTS } from '@/api/timeouts'
import { responseFileName } from '@/utils/fileDownload'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('lecture text export and AI usage api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    http.get.mockResolvedValue({ data: {} })
    http.post.mockResolvedValue({ data: [] })
  })

  it('downloads lecture text as blob for teacher and student', async () => {
    await lecturesApi.exportText(3, 'docx')
    await learningApi.exportLectureText(3, 'pdf')

    expect(http.get).toHaveBeenNthCalledWith(1, '/lectures/3/text/export', {
      params: { format: 'docx' },
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
    expect(http.get).toHaveBeenNthCalledWith(2, '/public/learning/lectures/3/text/export', {
      params: { format: 'pdf' },
      responseType: 'blob',
      timeout: API_TIMEOUTS.fileTransfer,
    })
  })

  it('saves lecture text into materials', async () => {
    await lecturesApi.saveTextAsMaterial(3, 'pdf')

    expect(http.post).toHaveBeenCalledWith('/lectures/3/text/materials', null, { params: { format: 'pdf' } })
  })

  it('requests the admin usage report for a period', async () => {
    await aiApi.getUsageReport({ from: '2026-10-01', to: '2026-10-20' })

    expect(http.get).toHaveBeenCalledWith('/admin/ai-usage', {
      params: { from: '2026-10-01', to: '2026-10-20' },
    })
  })

  it('reads utf-8 file names from content-disposition', () => {
    const response = {
      headers: {
        'content-disposition': "attachment; filename=\"lecture.pdf\"; filename*=UTF-8''%D0%9B%D0%B5%D0%BA%D1%86%D0%B8%D1%8F.pdf",
      },
    }

    expect(responseFileName(response, 'x.pdf')).toBe('Лекция.pdf')
    expect(responseFileName({ headers: {} }, 'x.pdf')).toBe('x.pdf')
  })
})
