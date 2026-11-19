import {
  computed,
  effectScope,
  onScopeDispose,
  reactive,
  ref,
  watch,
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
  { value: 'table', label: 'Таблица' },
])

export const SLIDE_IMAGE_OPTIONS = Object.freeze([
  { value: 'none', label: 'Без картинки' },
  { value: 'search', label: 'Найти в банке изображений' },
  { value: 'generate', label: 'Сгенерировать ИИ' },
  { value: 'material', label: 'Из материалов лекции' },
])

export const TEXT_AMOUNT_OPTIONS = Object.freeze([
  { value: 'short', label: 'Кратко' },
  { value: 'medium', label: 'Средне' },
  { value: 'detailed', label: 'Подробно' },
])

export const IMAGE_AMOUNT_OPTIONS = Object.freeze([
  { value: 'none', label: 'Без картинок' },
  { value: 'some', label: 'Немного' },
  { value: 'many', label: 'Много' },
])

const IMAGE_FILE = /\.(png|jpe?g)$/i
const STOP_WORDS = new Set(['with', 'from', 'into', 'that', 'this', 'their', 'about', 'using', 'used', 'based', 'image', 'photo'])

/** Похожа ли находка на запрос: в её названии не меньше половины значимых слов запроса (как на сервере). */
export function matchesQuery(result, query) {
  const title = String(result?.title ?? '').toLowerCase()
  const words = String(query ?? '').toLowerCase().split(/[^\p{L}\p{N}]+/u)
    .filter((word) => word.length >= 4 && !STOP_WORDS.has(word))
  const matched = words.filter((word) => title.includes(word.slice(0, 5))).length
  return matched > 0 && 2 * matched >= words.length
}
// Сколько поисков картинок идёт одновременно при автоподборе.
const AUTO_PICK_PARALLEL = 3

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

/** Таблица в редакторе: строка на строку таблицы, столбцы через «|», первая строка — заголовки. */
function tableToText(table) {
  const columns = table?.columns ?? []
  if (!columns.length) return ''
  return [columns, ...(table.rows ?? [])].map((row) => row.join(' | ')).join('\n')
}

function tableFromText(text) {
  const [columns = [], ...rows] = String(text ?? '').split('\n')
    .map((line) => line.trim().replace(/^\|/, '').replace(/\|$/, ''))
    .filter(Boolean)
    .map((line) => line.split('|').map((cell) => cell.trim()))
  return { columns, rows: rows.map((row) => columns.map((_, index) => row[index] ?? '')) }
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
    tableText: tableToText(slide.table),
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
    table: tableFromText(slide.tableText),
    notes: slide.notes.trim(),
    image,
  }
}

const SOURCE_NAMES = { wikimedia: 'Wikimedia Commons', openverse: 'Openverse' }

/** Банки картинок, которые не ответили на поиск (заголовок X-Image-Sources-Unavailable). */
function unavailableSources(response) {
  const header = response?.headers?.['x-image-sources-unavailable'] ?? ''
  return String(header).split(',').map((name) => name.trim()).filter(Boolean)
    .map((name) => SOURCE_NAMES[name] ?? name)
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
    textAmount: 'medium',
    imageAmount: 'some',
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

  // План, изменённый после последней сборки (или ещё не собранный), потеряется при уходе со страницы.
  const changedSinceBuild = ref(false)
  const unsavedOutline = computed(() => hasOutline.value && changedSinceBuild.value)
  watch(outline, () => {
    changedSinceBuild.value = true
  }, { deep: true })

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
        textAmount: settings.textAmount,
        imageAmount: settings.imageAmount,
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
    await autoPickImages()
  }

  /** Подбирает первую найденную картинку слайдам, где ИИ предложил поиск; её можно заменить. */
  async function autoPickImages() {
    const queue = outline.slides.filter((slide) => (
      slide.image.mode === 'search' && slide.image.query.trim() && !slide.image.selected
    ))
    const worker = async () => {
      for (let slide = queue.shift(); slide; slide = queue.shift()) {
        await searchImages(slide)
        // На абстрактные запросы банки находят случайные фото: берём только похожую находку.
        const first = slide.image.results.find((result) => matchesQuery(result, slide.image.query))
        if (first && !slide.image.selected && slide.image.mode === 'search') {
          selectFound(slide, first)
        }
      }
    }
    await Promise.all(Array.from({ length: AUTO_PICK_PARALLEL }, worker))
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
      slide.image.results = found.map((item) => reactive({ ...item, previewUrl: '', failed: false }))
      const unavailable = unavailableSources(response)
      if (!found.length) {
        slide.image.error = unavailable.length
          ? `${unavailable.join(' и ')} сейчас не ${unavailable.length > 1 ? 'отвечают' : 'отвечает'} — повторите поиск позже.`
          : 'Ничего не найдено — попробуйте другие слова, лучше по-английски.'
      }
      // Превью грузятся в фоне: часть банков отвечает медленно, искать снова можно сразу.
      slide.image.results.forEach(async (result) => {
        try {
          const preview = await aiApi.previewImage(result.imageUrl)
          result.previewUrl = remember(URL.createObjectURL(preview.data))
          // Картинка могла быть выбрана (автоподбором) раньше, чем загрузилось её превью.
          if (slide.image.selected?.url === result.imageUrl && !slide.image.previewUrl) {
            slide.image.previewUrl = result.previewUrl
          }
        } catch {
          result.failed = true
        }
      })
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
    if (builtResult.value) {
      changedSinceBuild.value = false
    }
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
    unsavedOutline,
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
