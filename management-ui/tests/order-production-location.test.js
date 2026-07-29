import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const readRepo = (relativePath) => readFileSync(path.join(repoRoot, relativePath), 'utf8')

const orderPage = readRepo('management-ui/src/views/function/order/order.vue')
const entity = readRepo('management/src/main/java/my/hive/domain/order/model/entity/SalesOrder.java')
const request = readRepo('management/src/main/java/my/hive/domain/order/model/dto/SalesOrderSaveRequest.java')
const detail = readRepo('management/src/main/java/my/hive/domain/order/model/vo/SalesOrderDetailVO.java')
const mapper = readRepo('management/src/main/java/my/hive/domain/order/mapper/SalesOrderMapper.java')
const service = readRepo('management/src/main/java/my/hive/domain/order/service/OrderService.java')
const migrationEntry = 'migrations/V20260729_001_sales_order_production_location.sql'
const migrationPath = `db-migrations/${migrationEntry}`
const migration = readRepo(migrationPath)

test('order editor offers only Beijing and Haining branch production locations', () => {
  const options = orderPage.match(
    /const productionLocationOptions = \[([\s\S]*?)\]/,
  )?.[1] || ''

  assert.match(options, /\{value: '北京', label: '北京'\}/)
  assert.match(options, /\{value: '海宁分公司', label: '海宁分公司'\}/)
  assert.equal((options.match(/value:/g) || []).length, 2)
  assert.match(orderPage, /v-model="orderForm\.productionLocation"/)
})

test('production location is saved, restored, and shown only in order detail', () => {
  assert.match(orderPage, /productionLocation: blank\(orderForm\.productionLocation\)/)
  assert.match(orderPage, /orderForm\.productionLocation = detail\.productionLocation \|\| ''/)
  assert.match(orderPage, /orderDetail\.productionLocation \|\| '未设置'/)

  const columns = orderPage.match(
    /const defaultOrderTableColumns = \[([\s\S]*?)\]\s*const \{/,
  )?.[1] || ''
  const filters = orderPage.match(
    /const filters = reactive\(\{([\s\S]*?)\}\)/,
  )?.[1] || ''
  assert.doesNotMatch(columns, /productionLocation/)
  assert.doesNotMatch(filters, /productionLocation/)
})

test('confirming a pending order requires acknowledgement of its production location', () => {
  assert.match(
    orderPage,
    /editingOrderStatus\.value === 'pending_confirm'[\s\S]*?advanceIntent\.value\?\.targetStatus === 'pending_pay'/,
  )
  assert.match(
    orderPage,
    /`本订单生产地点为“\$\{productionLocation\}”，请确认选择无误。`/,
  )
  assert.match(orderPage, /confirmButtonText: '确认并推进'/)
  assert.match(orderPage, /cancelButtonText: '返回修改'/)
  assert.match(
    orderPage,
    /requiresProductionLocationConfirmation\.value && !\(await confirmOrderProductionLocation\(\)\)[\s\S]*?return[\s\S]*?const basePayload = buildOrderPayload\(\)/,
  )
  assert.match(
    orderPage,
    /请先选择生产地点，再确认订单', 'order\.productionLocation'/,
  )
})

test('backend persists the optional field and constrains new values', () => {
  assert.match(entity, /@TableField\("production_location"\)\s+private String productionLocation;/)
  assert.match(request, /@Pattern\(regexp = "北京\|海宁分公司"/)
  assert.match(detail, /private String productionLocation;/)
  assert.match(mapper, /production_location AS productionLocation/)
  assert.match(service, /order\.setProductionLocation\(normalizeSalesProductionLocation\(request\.getProductionLocation\(\)\)\)/)
  assert.match(service, /SALES_PRODUCTION_LOCATIONS = Set\.of\("北京", "海宁分公司"\)/)
})

test('additive migration keeps existing order data compatible and is checksum registered', () => {
  assert.match(migration, /ADD COLUMN production_location VARCHAR\(16\) DEFAULT NULL/)
  assert.doesNotMatch(migration, /\bUPDATE\s+sales_order\b/i)

  const manifest = readRepo('db-migrations/migration_manifest.txt')
  const checksums = readRepo('db-migrations/migration_checksums.sha256')
  const hash = createHash('sha256').update(readFileSync(path.join(repoRoot, migrationPath))).digest('hex')

  assert.match(manifest, new RegExp(`${migrationEntry.replaceAll('/', '\\/')}$`, 'm'))
  assert.match(checksums, new RegExp(`^${hash}  ${migrationEntry.replaceAll('/', '\\/')}$`, 'm'))
})
