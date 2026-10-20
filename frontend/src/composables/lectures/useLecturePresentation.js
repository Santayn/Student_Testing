import {
  computed,
  effectScope,
  onScopeDispose,
  reactive,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  lecturesApi,
} from '@/api'
import { aiApi } from '@/api/ai.api'

import { useAiJob } from '@/composables/ai/useAiJob'
import { listFromResponse } from '@/utils/apiData'

export const SLIDE_LAYOUT_OPTIONS = Object.freeze([
  { value: 'bullets', label: 'Тезисы' },
  { value: 'two_columns', label: 'Сравнение в две колонки' },
  { value: 'process', label: 'Шаги процесса' },
  { value: 'section', label: 'Титул раздела' },
  { value: 'quote', label: 'Определение или цитата' },
])

export const SLIDE_IMAGE_OPTIONS = Object.freeze([
  { value: 'none', label: 'Без картинки' },
  { value: 'search', label: 'Найти в банке изображений' },
  { value: 'generate', label: 'Сгенерировать ИИ' },
  { value: 'material', label: 'Из материалов лекции' },
])

const IMAGE_FILE = /\.(png|jpe?g)$/i

let slideKey = 0

function lines(text) {
  return String(text ?? '')
    .split('\n')
    .map((line) => line.replace(/^\s*[-•*]\s*/, '').trim())
    .filter(Boolean)
}

function joinLines(values) {
  return Array.isArray(values) ? values.join('\n') : ''
}

function slideToForm(slide = {}) {
  return reactive({
    key: ++slideKey,
    layout: slide.layout || 'bullets',
    title: slide.title || '',
    bulletsText: joinLines(slide.bullets),
    leftTitle: slide.left?.title || '',
    leftText: joinLines(slide.left?.bullets),
    rightTitle: slide.right?.title || '',
    rightText: joinLines(slide.right?.bullets),
    stepsText: joinLines(slide.steps),
    notes: slide.notes || '',
    image: {
      mode: slide.image?.mode || 'none',
      query: slide.image?.query || '',
      prompt: slide.image?.prompt || '',
      selected: null,
      previewUrl: '',
      results: [],
      searching: false,
      generating: false,
      error: '',
      materialId: null,
    },
  })
}

function slideFromForm(slide) {
  const image = {
    mode: slide.image.mode,
    query: slide.image.query,
    prompt: slide.image.prompt,
  }

  if (slide.image.mode === 'material' && slide.image.materialId) {
    image.selected = { kind: 'material', materialId: Number(slide.image.materialId) }
  } else if (slide.image.selected) {
    image.selected = slide.image.selected
  }

  return {
    layout: slide.layout,
    title: slide.title.trim(),
    bullets: lines(slide.bulletsText),
    left: { title: slide.leftTitle.trim(), bullets: lines(slide.leftText) },
    right: { title: slide.rightTitle.trim(), bullets: lines(slide.rightText) },
    steps: lines(slide.stepsText),
    notes: slide.notes.trim(),
    image,
  }
}

/**
 * Мастер презентации по лекции: план от ИИ → правка слайдов и выбор картинок →
 * сборка PPTX/PDF в материалы лекции.
 */
export function useLecturePresentation(lectureId) {
  const lecture = ref(null)
  const imageMaterials = ref([])
  const loading = ref(false)
  const loadError = ref('')

  const settings = reactive({
    slideCount: 10,
    instructions: '',
    withPdf: true,
  })

  const outline = reactive({
    title: '',
    subtitle: '',
    slides: [],
  })

  const outlineJob = useAiJob()
  const buildJob = useAiJob()
  const builtResult = ref(null)
  const objectUrls = new Set()
  const imageJobs = new Map()
  // Задачи генерации картинок создаются по требованию — в отдельной области, чтобы их опрос останавливался вместе со страницей.
  const imageScope = effectScope(true)

  const hasOutline = computed(() => outline.slides.length > 0)

  async function load() {
    loading.value = true
    loadError.value = ''

    try {
      const [lectureResponse, materialsResponse] = await Promise.all([
        lecturesApi.getOne(lectureId.value),
        lecturesApi.getMaterials(lectureId.value),
      ])
      lecture.value = lectureResponse.data ?? null
      imageMaterials.value = listFromResponse(materialsResponse)
        .filter((material) => IMAGE_FILE.test(material.fileName ?? ''))
    } catch (requestError) {
      loadError.value = getApiErrorMessage(requestError, 'Не удалось загрузить лекцию.')
    } finally {
      loading.value = false
    }
  }

  async function generateOutline() {
    builtResult.value = null
    const done = await outlineJob.track(
      aiApi.createPresentationOutline(lectureId.value, {
        slideCount: Number(settings.slideCount) || 10,
        instructions: settings.instructions.trim() || null,
      })
    )

    if (!done?.result) {
      return
    }

    releasePreviews()
    outline.title = done.result.title || lecture.value?.title || ''
    outline.subtitle = done.result.subtitle || ''
    outline.slides = (done.result.slides ?? []).map(slideToForm)
  }

  function addSlide(index = outline.slides.length) {
    outline.slides.splice(index, 0, slideToForm({ title: 'Новый слайд' }))
  }

  function removeSlide(index) {
    const [removed] = outline.slides.splice(index, 1)
    if (removed?.image.previewUrl) {
      revoke(removed.image.previewUrl)
    }
  }

  function moveSlide(index, delta) {
    const target = index + delta
    if (target < 0 || target >= outline.slides.length) {
      return
    }
    const [slide] = outline.slides.splice(index, 1)
    outline.slides.splice(target, 0, slide)
  }

  async function searchImages(slide) {
    const query = slide.image.query.trim()
    if (!query || slide.image.searching) {
      return
    }

    slide.image.searching = true
    slide.image.error = ''
    slide.image.results.forEach((result) => revoke(result.previewUrl))
    slide.image.results = []

    try {
      const response = await aiApi.searchImages(query)
      const found = Array.isArray(response?.data) ? response.data : []
      slide.image.results = found.map((item) => reactive({ ...item, previewUrl: '' }))
      if (!found.length) {
        slide.image.error = 'Ничего не найдено — попробуйте другие слова, лучше по-английски.'
      }
      await Promise.all(slide.image.results.map(async (result) => {
        try {
          const preview = await aiApi.previewImage(result.imageUrl)
          result.previewUrl = remember(URL.createObjectURL(preview.data))
        } catch {
          result.previewUrl = ''
        }
      }))
    } catch (requestError) {
      slide.image.error = getApiErrorMessage(requestError, 'Поиск картинок не удался.')
    } finally {
      slide.image.searching = false
    }
  }

  function selectFound(slide, result) {
    if (slide.image.previewUrl && !slide.image.results.some((item) => item.previewUrl === slide.image.previewUrl)) {
      revoke(slide.image.previewUrl)
    }
    slide.image.selected = {
      kind: 'search',
      url: result.imageUrl,
      source: result.source,
      title: result.title,
      author: result.author,
      license: result.license,
      licenseUrl: result.licenseUrl,
      pageUrl: result.pageUrl,
    }
    slide.image.previewUrl = result.previewUrl
  }

  async function generateImage(slide) {
    const prompt = slide.image.prompt.trim()
    if (!prompt || slide.image.generating) {
      return
    }

    let job = imageJobs.get(slide.key)
    if (!job) {
      job = imageScope.run(() => useAiJob())
      imageJobs.set(slide.key, job)
    }

    slide.image.generating = true
    slide.image.error = ''

    try {
      const done = await job.track(aiApi.generateImage({ prompt, lectureId: Number(lectureId.value) }))
      if (!done) {
        slide.image.error = job.error.value || 'Картинка не сгенерирована.'
        return
      }
      const image = await aiApi.getGeneratedImage(done.id)
      if (slide.image.previewUrl) {
        revoke(slide.image.previewUrl)
      }
      slide.image.previewUrl = remember(URL.createObjectURL(image.data))
      slide.image.selected = { kind: 'generated', jobId: done.id }
    } catch (requestError) {
      slide.image.error = getApiErrorMessage(requestError, 'Не удалось показать картинку.')
    } finally {
      slide.image.generating = false
    }
  }

  function clearImage(slide) {
    slide.image.selected = null
    slide.image.materialId = null
    slide.image.previewUrl = ''
  }

  function outlinePayload() {
    return {
      title: outline.title.trim() || lecture.value?.title || 'Презентация',
      subtitle: outline.subtitle.trim(),
      slides: outline.slides.map(slideFromForm),
    }
  }

  async function build() {
    if (!hasOutline.value) {
      return
    }
    builtResult.value = null
    const done = await buildJob.track(
      aiApi.buildPresentation(lectureId.value, {
        outline: outlinePayload(),
        withPdf: settings.withPdf,
      })
    )
    builtResult.value = done?.result ?? null
  }

  async function downloadBuilt(materialId, fileName) {
    const response = await lecturesApi.downloadMaterial(lectureId.value, materialId)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName || 'presentation'
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  function remember(url) {
    objectUrls.add(url)
    return url
  }

  function revoke(url) {
    if (url && objectUrls.has(url)) {
      URL.revokeObjectURL(url)
      objectUrls.delete(url)
    }
  }

  function releasePreviews() {
    objectUrls.forEach((url) => URL.revokeObjectURL(url))
    objectUrls.clear()
  }

  onScopeDispose(() => {
    releasePreviews()
    imageScope.stop()
  })

  return {
    lecture,
    imageMaterials,
    loading,
    loadError,
    settings,
    outline,
    outlineJob,
    buildJob,
    builtResult,
    hasOutline,
    load,
    generateOutline,
    addSlide,
    removeSlide,
    moveSlide,
    searchImages,
    selectFound,
    generateImage,
    clearImage,
    build,
    downloadBuilt,
  }
}
