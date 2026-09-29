import { readFile, readdir } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { dirname, extname, resolve } from 'node:path'
import process from 'node:process'

const frontendRoot = process.cwd()
const srcRoot = resolve(frontendRoot, 'src')
const supportedExtensions = new Set(['.js', '.vue'])

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const result = []

  for (const entry of entries) {
    const path = resolve(directory, entry.name)
    if (entry.isDirectory()) {
      result.push(...await walk(path))
    } else {
      result.push(path)
    }
  }

  return result
}

function isProductionSource(path) {
  return (
    supportedExtensions.has(extname(path)) &&
    !path.includes(`${process.platform === 'win32' ? '\\' : '/'}__tests__${process.platform === 'win32' ? '\\' : '/'}`)
  )
}

function resolveInternalImport(importer, specifier, sourceFiles) {
  if (!specifier.startsWith('.') && !specifier.startsWith('@/')) {
    return null
  }

  const base = specifier.startsWith('@/')
    ? resolve(srcRoot, specifier.slice(2))
    : resolve(dirname(importer), specifier)

  const candidates = [
    base,
    `${base}.js`,
    `${base}.vue`,
    resolve(base, 'index.js'),
  ]

  return candidates.find((candidate) => sourceFiles.has(resolve(candidate))) ?? null
}

function relativeSource(path) {
  return path.slice(srcRoot.length + 1).replaceAll('\\', '/')
}

const allFiles = await walk(srcRoot)
const productionFiles = allFiles.filter(isProductionSource).map((path) => resolve(path))
const productionSet = new Set(productionFiles)
const sources = new Map()

for (const path of productionFiles) {
  sources.set(path, await readFile(path, 'utf8'))
}

const errors = []
const forbiddenPatterns = [
  { label: 'debugger statement', pattern: /\bdebugger\s*;/ },
  { label: 'console.log/debug call', pattern: /\bconsole\.(?:log|debug)\s*\(/ },
  { label: 'v-html', pattern: /\bv-html\s*=/ },
  { label: 'innerHTML assignment', pattern: /\.innerHTML\s*=/ },
  { label: 'eval()', pattern: /\beval\s*\(/ },
  { label: 'new Function()', pattern: /\bnew\s+Function\s*\(/ },
]

for (const [path, source] of sources) {
  for (const rule of forbiddenPatterns) {
    if (rule.pattern.test(source)) {
      errors.push(`${relativeSource(path)}: ${rule.label}`)
    }
  }
}

const importPattern = /(?:\bfrom\s*|\bimport\s*\(\s*|\bimport\s*)['"]([^'"]+)['"]/g
const graph = new Map()

for (const [path, source] of sources) {
  const imports = new Set()
  for (const match of source.matchAll(importPattern)) {
    const specifier = match[1]
    const resolved = resolveInternalImport(path, specifier, productionSet)

    if (resolved) {
      imports.add(resolve(resolved))
      continue
    }

    if (
      (specifier.startsWith('.') || specifier.startsWith('@/')) &&
      !/\.(?:css|scss|sass|less|png|jpe?g|gif|svg|webp|ico|woff2?|ttf|otf)$/i.test(specifier)
    ) {
      errors.push(`${relativeSource(path)}: unresolved internal import ${specifier}`)
    }
  }
  graph.set(path, imports)
}

const entry = resolve(srcRoot, 'main.js')
if (!existsSync(entry)) {
  errors.push('src/main.js: application entrypoint is missing')
} else {
  const reachable = new Set()
  const stack = [entry]

  while (stack.length) {
    const current = stack.pop()
    if (reachable.has(current)) continue
    reachable.add(current)
    for (const dependency of graph.get(current) ?? []) {
      stack.push(dependency)
    }
  }

  for (const path of productionFiles) {
    if (!reachable.has(path)) {
      errors.push(`${relativeSource(path)}: unreachable production module`)
    }
  }
}

const corpus = [...sources.values()].join('\n')
for (const [path, source] of sources) {
  const declarations = [
    ...source.matchAll(/\bexport\s+(?:async\s+)?function\s+([A-Za-z_$][\w$]*)/g),
    ...source.matchAll(/\bexport\s+const\s+([A-Za-z_$][\w$]*)/g),
    ...source.matchAll(/\bexport\s+class\s+([A-Za-z_$][\w$]*)/g),
  ]

  for (const match of declarations) {
    const name = match[1]
    const occurrences = corpus.match(new RegExp(`\\b${name}\\b`, 'g'))?.length ?? 0
    if (occurrences === 1) {
      errors.push(`${relativeSource(path)}: exported symbol ${name} is unused by production code`)
    }
  }
}

if (errors.length) {
  console.error(`Static quality check failed with ${errors.length} issue(s):`)
  for (const error of errors.sort()) {
    console.error(`- ${error}`)
  }
  process.exitCode = 1
} else {
  console.log(`Static quality check passed: ${productionFiles.length} production JS/Vue modules are reachable and clean.`)
}
