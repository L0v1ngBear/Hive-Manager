import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const installationTask = readFileSync(
  new URL('../src/views/function/installationTask/installationTask.vue', import.meta.url),
  'utf8'
)
const quality = readFileSync(
  new URL('../src/views/function/badProduct/badProduct.vue', import.meta.url),
  'utf8'
)

const requiredElements = [
  ['el-table', 'ElTable'],
  ['el-pagination', 'ElPagination'],
  ['el-form', 'ElForm'],
  ['el-input', 'ElInput'],
  ['el-select', 'ElSelect'],
  ['el-tag', 'ElTag'],
  ['el-empty', 'ElEmpty']
]

function assertElementPlusPage(source, pageName) {
  const elementPlusImport = source.match(
    /import\s*\{([\s\S]*?)\}\s*from\s*['"]element-plus['"]/
  )

  assert.ok(elementPlusImport, `${pageName} should explicitly import Element Plus controls`)
  assert.match(source, /<el-(?:dialog|drawer)\b/, `${pageName} should use an Element Plus overlay`)
  assert.match(source, /v-loading\b/, `${pageName} should bind Element Plus loading state`)

  for (const [tag, component] of requiredElements) {
    assert.match(source, new RegExp(`<${tag}\\b`), `${pageName} should render ${tag}`)
    assert.match(
      elementPlusImport[1],
      new RegExp(`\\b${component}\\b`),
      `${pageName} should explicitly import ${component}`
    )
  }
}

test('migrates installation task controls to Element Plus without losing attachments', () => {
  assertElementPlusPage(installationTask, 'installation task')
  assert.match(installationTask, /import DragAttachmentUpload from /)
})

test('migrates quality controls to Element Plus without losing process dependencies', () => {
  assertElementPlusPage(quality, 'quality')
  assert.match(quality, /import DragAttachmentUpload from /)
  assert.match(quality, /import BusinessTimeCorrectionPanel from /)
})
