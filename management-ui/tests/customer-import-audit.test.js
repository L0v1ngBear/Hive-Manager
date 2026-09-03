import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readRepo = (path) => readFileSync(new URL(`../../${path}`, import.meta.url), 'utf8')
const customerEntity = readRepo('management/src/main/java/my/hive/domain/customer/model/entity/Customer.java')
const customerService = readRepo('management/src/main/java/my/hive/domain/customer/service/CustomerService.java')
const customerPage = readRepo('management-ui/src/views/function/customer/customer.vue')
const migrationPath = 'db-migrations/migrations/V20260902_002_customer_import_audit.sql'
const migration = readRepo(migrationPath)
const manifest = readRepo('db-migrations/migration_manifest.txt')
const checksums = readRepo('db-migrations/migration_checksums.sha256')

test('new customer imports retain an auditable source, time and operator snapshot', () => {
  for (const field of ['sourceType', 'importTime', 'importUserId', 'importUserName']) {
    assert.match(customerEntity, new RegExp(`private [^;]+ ${field};`))
  }
  assert.match(customerService, /addCustomer\(request, SOURCE_IMPORT, importTime, importUserId, importUserName\)/)
  assert.match(customerService, /customer\.setSourceType\(sourceType\)/)
  assert.match(customerService, /customer\.setImportTime\(importTime\)/)
  assert.match(customerService, /customer\.setImportUserName\(importUserName\)/)
  assert.match(customerService, /customer\.setSourceType\(SOURCE_AFTER_SALES\)/)
})

test('customer list marks imported customers and can locate every recorded source', () => {
  assert.match(customerPage, /placeholder="全部客户来源"/)
  assert.match(customerPage, /value="import"/)
  assert.match(customerPage, /value="unknown"/)
  assert.match(customerPage, /customer\.sourceType === 'import'/)
  assert.match(customerPage, /customer\.importTime/)
  assert.match(customerPage, /customer\.importUserName/)
  assert.match(customerPage, /sourceType: filters\.sourceType \|\| undefined/)
})

test('customer import audit migration backfills only the explicitly confirmed historical range', () => {
  assert.match(migration, /`source_type` VARCHAR\(20\) NULL/)
  assert.match(migration, /`import_time` DATETIME NULL/)
  assert.match(migration, /`import_user_id` BIGINT NULL/)
  assert.match(migration, /`import_user_name` VARCHAR\(80\) NULL/)
  assert.match(migration, /UPDATE `customer` AS `target`[\s\S]*?JOIN `customer` AS `anchor`/)
  assert.match(migration, /`anchor`\.`id` = 53[\s\S]*?`anchor`\.`tenant_code` = `target`\.`tenant_code`/)
  assert.match(migration, /`target`\.`import_time` = '2026-09-02 00:00:00'/)
  assert.match(migration, /`target`\.`import_user_name` = '历史导入（人工确认）'/)
  assert.match(migration, /WHERE `target`\.`id` > 53;/)
  assert.doesNotMatch(migration, /WHERE `target`\.`id` >= 53;/)
  assert.match(manifest, /migrations\/V20260902_002_customer_import_audit\.sql/)
  const hash = createHash('sha256').update(migration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260902_002_customer_import_audit\\.sql`))
})
