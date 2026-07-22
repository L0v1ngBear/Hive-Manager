import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('document files are opened through the authenticated blob download endpoint', () => {
  const api = read('src/views/function/document/api/document.js')
  const page = read('src/views/function/document/document.vue')

  assert.match(api, /url:\s*['"]\/document\/file\/download['"]/)
  assert.match(api, /responseType:\s*['"]blob['"]/)
  assert.match(page, /downloadDocumentFile\(doc\.id\)/)
  assert.doesNotMatch(page, /openDocumentUrl\(doc\.fileUrl\)/)
})
