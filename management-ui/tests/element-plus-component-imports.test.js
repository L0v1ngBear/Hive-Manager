import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = path.resolve('D:/HiveManager/management-ui/src')

function walkVueFiles(dir, files = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walkVueFiles(fullPath, files)
    } else if (entry.isFile() && fullPath.endsWith('.vue')) {
      files.push(fullPath)
    }
  }
  return files
}

function componentNameFromTag(tag) {
  return tag
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
}

function importedElementPlusComponents(source) {
  const imports = new Set()
  for (const match of source.matchAll(/import\s+\{([^}]*)\}\s+from\s+['"]element-plus['"]/g)) {
    match[1]
      .split(',')
      .map((item) => item.trim().split(/\s+as\s+/)[0].trim())
      .filter(Boolean)
      .forEach((name) => imports.add(name))
  }
  return imports
}

test('all locally used Element Plus template components are explicitly imported', () => {
  const missingByFile = []
  for (const file of walkVueFiles(root)) {
    const source = readFileSync(file, 'utf8')
    const tags = [...source.matchAll(/<\s*(el-[a-z0-9-]+)/gi)].map((match) => match[1].toLowerCase())
    if (!tags.length) continue

    const required = [...new Set(tags.map(componentNameFromTag))]
    const imported = importedElementPlusComponents(source)
    const missing = required.filter((name) => !imported.has(name))
    if (missing.length) {
      missingByFile.push(`${file.replace(/\\/g, '/')}: ${missing.join(', ')}`)
    }
  }

  assert.deepEqual(missingByFile, [])
})
