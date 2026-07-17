import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function tableBlock(schema, tableName) {
  const match = schema.match(new RegExp('CREATE TABLE `' + tableName + '` \\(([\\s\\S]*?)\\n\\) ENGINE=', 'u'))
  assert.ok(match, `schema must define ${tableName}`)
  return match[1]
}

function sourceFiles(relativeRoot) {
  const absoluteRoot = path.join(repoRoot, relativeRoot)
  return fs.readdirSync(absoluteRoot, { withFileTypes: true }).flatMap(entry => {
    const relativePath = path.join(relativeRoot, entry.name).replaceAll('\\', '/')
    if (entry.isDirectory()) return sourceFiles(relativePath)
    return /\.(?:java|js|vue)$/u.test(entry.name) ? [relativePath] : []
  })
}

const activeOrderContractPaths = [
  'management/src/main/java/my/hive/api/order',
  'management/src/main/java/my/hive/domain/order',
  'management/src/main/java/my/hive/infrastructure/logistics',
  'management-ui/src/views/function/order'
].flatMap(sourceFiles)

test('active order contracts contain only shipment-list logistics', () => {
  for (const relativePath of activeOrderContractPaths) {
    assert.doesNotMatch(
      read(relativePath),
      /expressCompany|expressNo|getExpressCompany|getExpressNo|setExpressCompany|setExpressNo/u,
      `${relativePath} must not expose order-wide logistics fields`
    )
  }

  const saveRequest = read('management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java')
  const updateRequest = read('management/src/main/java/my/hive/domain/order/model/dto/SalesOrderUpdateRequest.java')
  const trackingResponse = read('management/src/main/java/my/hive/domain/order/model/vo/OrderLogisticsTrackingVO.java')
  const orderVue = read('management-ui/src/views/function/order/order.vue')
  const orderApi = read('management-ui/src/views/function/order/api/order.js')

  assert.match(saveRequest, /List<SalesOrderShipmentSaveRequest> shipments/u)
  assert.match(updateRequest, /List<SalesOrderShipmentSaveRequest> shipments/u)
  assert.match(trackingResponse, /private String trackingNo;/u)
  assert.match(orderVue, /orderForm\.shipments/u)
  assert.match(orderApi, /shipments\/\$\{encodeURIComponent\(shipmentId\)\}\/logistics-tracking/u)
})

test('database contracts normalize order logistics without rewriting migration history', () => {
  const baseline = read('db-migrations/baseline/hive_schema_baseline.sql')
  const migration = read('db-migrations/migrations/V20260717_001_order_multi_shipment.sql')
  const salesOrder = tableBlock(baseline, 'sales_order')
  const shipment = tableBlock(baseline, 'sales_order_shipment')

  assert.doesNotMatch(salesOrder, /`express_company`|`express_no`/u)
  assert.match(shipment, /`logistics_company`/u)
  assert.match(shipment, /`tracking_no`/u)
  assert.match(migration, /CREATE TABLE `sales_order_shipment`/u)
  assert.match(migration, /DROP COLUMN `express_company`/u)
  assert.match(migration, /DROP COLUMN `express_no`/u)
})

test('installation-task logistics remain an explicitly excluded independent domain', () => {
  const installationRequest = read(
    'management/src/main/java/my/hive/domain/installation/model/dto/InstallationTaskStatusUpdateRequest.java'
  )
  const installationPage = read('management-ui/src/views/function/installationTask/installationTask.vue')

  assert.match(installationRequest, /expressCompany/u)
  assert.match(installationRequest, /expressNo/u)
  assert.match(installationPage, /expressCompany/u)
  assert.match(installationPage, /expressNo/u)
  assert.ok(activeOrderContractPaths.every(relativePath => !relativePath.includes('/installation')))
})

test('advance endpoint activates nested shipment validation', () => {
  const controller = read('management/src/main/java/my/hive/api/order/OrderController.java')
  assert.match(
    controller,
    /@PostMapping\("\/\{orderId\}\/advance"\)[\s\S]*?@RequestBody\(required = false\) @Valid SalesOrderUpdateRequest request/u
  )
})
