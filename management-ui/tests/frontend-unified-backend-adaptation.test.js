import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = path.resolve(import.meta.dirname, '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('exports use the exact module permissions instead of retired table export permission', () => {
  const order = read('src/views/function/order/order.vue')
  const customer = read('src/views/function/customer/customer.vue')
  const document = read('src/views/function/document/document.vue')
  const source = `${order}\n${customer}\n${document}`

  assert.doesNotMatch(source, /table:export/)
  assert.match(order, /order:list/)
  assert.match(customer, /customer:export/)
  assert.match(document, /document:export/)
})

test('inventory navigation and requests use list detail and warning permissions independently', () => {
  const router = read('src/router/index.js')
  const inventory = read('src/views/function/inventory/inventory.vue')
  const detail = read('src/views/function/inventory/InventoryModelDetail.vue')

  assert.match(router, /inventory:list/)
  assert.match(router, /inventory:detail/)
  assert.match(router, /inventory:cloth:in/)
  assert.match(router, /inventory:cloth:out/)
  assert.match(router, /inventory:warning:list/)
  assert.match(router, /inventory:record:list/)
  assert.match(inventory, /inventory:list/)
  assert.match(inventory, /inventory:warning:list/)
  assert.match(detail, /inventory:detail/)
})

test('approval center uses the unified approval and order audit permissions', () => {
  const router = read('src/router/index.js')
  const approval = read('src/views/function/approval/approvalCenter.vue')

  assert.match(router, /approval:list/)
  assert.match(approval, /approval:list/)
  assert.match(approval, /quality:audit/)
  assert.match(approval, /order:audit:material/)
})

test('business-code forbidden errors and canonical response shapes are handled directly', () => {
  const badProduct = read('src/views/function/badProduct/badProduct.vue')
  const installation = read('src/views/function/installationTask/installationTask.vue')
  const inventory = read('src/views/function/inventory/inventory.vue')
  const employeePermission = read('src/views/function/employee/EmployeePermissionDrawer.vue')
  const permissionLoaders = read('src/views/function/role/permissionLoaders.js')
  const label = read('src/views/function/label.vue')
  const labelOverview = read('src/views/function/label/labelOverviewAccess.js')
  const source = [badProduct, installation, inventory, employeePermission].join('\n')

  assert.match(source, /error\?\.code|error\.code/)
  assert.doesNotMatch(`${employeePermission}\n${permissionLoaders}`, /response\?\.data\?\.data|response\.data\.data/)
  assert.doesNotMatch(`${label}\n${labelOverview}`, /\.records\b|totalCount/)
})

test('empty backend feature list never grants every management module', () => {
  const userStore = read('src/stores/user.js')

  assert.doesNotMatch(userStore, /Old sessions do not have feature data|normalizedFeatureCode\.startsWith\(['"]module\./)
  assert.doesNotMatch(userStore, /ORDER_FEATURE_CODE[^}]+order:list/s)
})
