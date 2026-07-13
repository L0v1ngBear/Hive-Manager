import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

function read(path) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
}

const inventory = read('src/views/function/inventory/inventory.vue')
const detail = read('src/views/function/inventory/InventoryModelDetail.vue')

function assertElementComponents(source, components, label) {
  for (const component of components) {
    const tag = component.replace(/^El/, '').replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`).slice(1)
    assert.match(source, new RegExp(`<el-${tag}(?:\\s|>)`), `${label} must render ${component}`)
    assert.match(source, new RegExp(`import\\s*\\{[^}]*\\b${component}\\b[^}]*\\}\\s*from\\s*['\"]element-plus['\"]`, 's'), `${label} must explicitly import ${component}`)
  }
}

test('inventory migrates standard controls while protecting dynamic native table', () => {
  assertElementComponents(inventory, ['ElButton', 'ElInput', 'ElInputNumber', 'ElSelect', 'ElOption', 'ElPagination', 'ElDrawer', 'ElForm', 'ElFormItem', 'ElTag', 'ElEmpty', 'ElTooltip'], 'inventory')
  assert.match(inventory, /v-loading="loading"/)
  assert.match(inventory, /<table[^>]*class="responsive-data-table/)
  assert.match(inventory, /:data-label="column\.label"/)
  for (const contract of ['imageRecognitionCandidates', 'TableColumnSettings', 'DragAttachmentUpload', 'BusinessTimeCorrectionPanel', 'DateFilterInput']) {
    assert.match(inventory, new RegExp(`\\b${contract}\\b`), `inventory must preserve ${contract}`)
  }
})

test('inventory preserves payload types, recognition drafts, attachments and time correction', () => {
  assert.match(inventory, /status:\s*query\.status === '' \? undefined : Number\(query\.status\)/)
  assert.match(inventory, /outCloth\(\{ barcode: outForm\.barcode, meters: Number\(outForm\.meters\) \}\)/)
  assert.match(inventory, /inCloth\(payload\)/)
  assert.match(inventory, /candidate\.manualVerified/)
  assert.match(inventory, /<DragAttachmentUpload/)
  assert.match(inventory, /formatBusinessDateTimePayload/)
})

test('inventory permission commands remain visible, disabled with reasons and handler guarded', () => {
  for (const permission of ['inventory:warning:setting', 'inventory:cloth:in', 'inventory:cloth:out']) {
    assert.match(inventory, new RegExp(permission.replaceAll(':', '\\:')))
    assert.match(inventory, new RegExp(`requireUiPermission\\(['\"]${permission}['\"]\\)`))
  }
  assert.match(inventory, /<el-tooltip/)
  assert.match(inventory, /:disabled="!can[A-Z][^"]+"/)
})

test('warning settings use an owned numeric drawer instead of a prompt', () => {
  assert.match(inventory, /<el-drawer v-model="warningSettingVisible"/)
  assert.match(inventory, /<el-input-number v-model="warningSettingForm\.threshold"/)
  assert.match(inventory, /async function saveWarningSetting\(\)/)
  assert.doesNotMatch(inventory, /ElMessageBox\.prompt/)
})

test('inventory list exposes mutually exclusive loading empty permission and failure states', () => {
  assert.match(inventory, /const listLoadError = ref\(null\)/)
  assert.match(inventory, /const listLoaded = ref\(false\)/)
  assert.match(inventory, /rows\.value = \[\]/)
  assert.match(inventory, /v-if="!canReadInventory"/)
  assert.match(inventory, /v-else-if="listLoadError"/)
  assert.match(inventory, /v-else-if="loading"/)
  assert.match(inventory, /<el-empty v-else-if="listLoaded"/)
  assert.match(inventory, /@click="fetchData"/)
})

test('inventory detail migrates controls and protects dynamic responsive table', () => {
  assertElementComponents(detail, ['ElButton', 'ElInput', 'ElInputNumber', 'ElSelect', 'ElOption', 'ElDrawer', 'ElForm', 'ElFormItem', 'ElTag', 'ElEmpty', 'ElTooltip'], 'inventory detail')
  assert.match(detail, /v-loading="loading"/)
  assert.match(detail, /<table[^>]*class="responsive-data-table/)
  assert.match(detail, /:data-label="column\.label"/)
  assert.match(detail, /TableColumnSettings/)
})

test('cloth detail clears stale content and only latest request may commit', () => {
  assert.match(detail, /const requestId = \+\+clothDetailRequestId/)
  assert.match(detail, /clothDetail\.value = null[\s\S]*detailLoadError\.value = null[\s\S]*detailLoading\.value = true/)
  assert.match(detail, /if \(requestId !== clothDetailRequestId\) return/)
  assert.match(detail, /v-if="detailLoading"/)
  assert.match(detail, /v-else-if="detailLoadError"/)
  assert.match(detail, /v-else-if="clothDetail"/)
  assert.match(detail, /<el-empty v-else/)
})

test('outbound preview follows the exact submitted barcode and permission is guarded', () => {
  assert.match(detail, /watch\(\(\) => outForm\.barcode/)
  assert.match(detail, /outPreviewBarcode/)
  assert.match(detail, /outPreviewBarcode\.value === outForm\.barcode/)
  assert.match(detail, /if \(!requireUiPermission\('inventory:cloth:out'\)\) return/)
  assert.match(detail, /outCloth\(\{ barcode: outForm\.barcode, meters: Number\(outForm\.meters\) \}\)/)
})

test('inventory detail preserves route query and numeric status contracts', () => {
  assert.match(detail, /const modelCode = computed\(\(\) => String\(route\.query\.modelCode/)
  assert.match(detail, /const spec = computed\(\(\) => route\.query\.spec \?\? ''\)/)
  assert.match(detail, /const status = computed\(\(\) => route\.query\.status === undefined[\s\S]*Number\(route\.query\.status\)\)/)
  assert.match(detail, /timeOrder\.value/)
})
