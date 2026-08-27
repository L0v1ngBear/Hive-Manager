import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const globalStyle = read('src/style.css')
const inventory = read('src/views/function/inventory/inventory.vue')
const inventoryDetail = read('src/views/function/inventory/InventoryModelDetail.vue')
const quality = read('src/views/function/badProduct/badProduct.vue')
const priceEditor = read('src/views/function/price/priceCreate.vue')
const equipment = read('src/views/function/equipment/equipment.vue')
const organization = read('src/views/function/organization/organization.vue')
const employeeEditor = read('src/views/function/employee/employeeCreate.vue')

function assertOpaqueDrawer(source, className, expectedCount) {
  assert.equal(
    (source.match(new RegExp(`class="${className}"`, 'g')) || []).length,
    expectedCount,
    `${className} should be applied to every intended drawer`
  )
  assert.match(
    source,
    new RegExp(
      `:global\\(\\.${className}\\.el-drawer\\)\\s*\\{` +
      `[\\s\\S]*?--el-bg-color:\\s*#fff;` +
      `[\\s\\S]*?--el-dialog-bg-color:\\s*#fff;` +
      `[\\s\\S]*?background:\\s*#fff\\s*!important;` +
      `[\\s\\S]*?backdrop-filter:\\s*none;`
    )
  )
}

test('global styles no longer make every Element Plus drawer translucent', () => {
  const drawerAppearanceRules = [...globalStyle.matchAll(/\.el-drawer(?:\s*,[^{}]+)?\s*\{([^}]*)\}/g)]
    .map((match) => match[1])
    .filter((body) => /--el-bg-color|backdrop-filter/.test(body))

  assert.deepEqual(drawerAppearanceRules, [])
  assert.doesNotMatch(globalStyle, /\.el-dialog\s*\{[\s\S]*?(?:--el-bg-color|backdrop-filter)/)
})

test('inventory and quality drawers own opaque module-local surfaces', () => {
  assertOpaqueDrawer(inventory, 'inventory-opaque-drawer', 5)
  assertOpaqueDrawer(inventoryDetail, 'inventory-opaque-drawer', 2)
  assertOpaqueDrawer(quality, 'quality-drawer', 3)
})

test('price equipment organization and employee editors own opaque page-local surfaces', () => {
  assertOpaqueDrawer(priceEditor, 'price-editor-drawer', 1)
  assertOpaqueDrawer(equipment, 'equipment-editor-drawer', 1)
  assertOpaqueDrawer(organization, 'organization-editor-drawer', 2)
  assertOpaqueDrawer(employeeEditor, 'employee-editor-drawer', 1)
})
