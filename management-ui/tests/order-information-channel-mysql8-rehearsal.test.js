import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import { deployRoot } from './deploy-test-root.js'

const migrationPath = path.join(
  deployRoot,
  'db-migrations/migrations/V20260713_001_order_information_channel_and_cancel_reason.sql'
)
const migration = fs.readFileSync(migrationPath, 'utf8')
const dockerTimeout = 180_000

function docker(args, options = {}) {
  return spawnSync('docker', args, {
    encoding: 'utf8',
    timeout: dockerTimeout,
    maxBuffer: 8 * 1024 * 1024,
    ...options
  })
}

const dockerVersion = docker(['version', '--format', '{{.Server.Version}}'])
if (dockerVersion.status !== 0) {
  console.log('order information-channel MySQL 8 rehearsal skipped: Docker daemon unavailable')
  process.exit(0)
}

const containerName = `hive-order-migration-${process.pid}-${Date.now()}`
const rootPassword = 'HiveMigrationTest_20260713'
let containerStarted = false

function mysql(database, sql) {
  const args = [
    'exec', '-i', containerName,
    'mysql', '-uroot', `-p${rootPassword}`,
    '--default-character-set=utf8mb4', '--batch', '--skip-column-names'
  ]
  if (database) args.push(database)
  return docker(args, { input: sql })
}

function mysqlOk(database, sql, message) {
  const result = mysql(database, sql)
  assert.equal(result.status, 0, `${message}\n${result.stderr || result.stdout}`)
  return result.stdout.trim()
}

function runMigration(database) {
  return mysql(database, `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;\n${migration}`)
}

function createDatabase(database, setupSql) {
  mysqlOk(
    null,
    `DROP DATABASE IF EXISTS \`${database}\`; CREATE DATABASE \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`,
    `create isolated scenario database ${database}`
  )
  mysqlOk(database, setupSql, `prepare isolated scenario ${database}`)
}

function assertFinalColumns(database) {
  const definitions = mysqlOk(database, `
SELECT CONCAT(table_name, ':', data_type, ':', character_maximum_length)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('sales_order', 'production_order', 'installation_task')
  AND column_name = 'information_channel'
ORDER BY table_name;
`, `inspect information_channel definitions in ${database}`).split(/\r?\n/)

  assert.deepEqual(definitions, [
    'installation_task:varchar:100',
    'production_order:varchar:100',
    'sales_order:varchar:100'
  ])
  assert.equal(
    mysqlOk(database, `
SELECT COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('sales_order', 'production_order', 'installation_task')
  AND column_name = 'delivery_date';
`, `inspect retired columns in ${database}`),
    '0'
  )
}

async function waitForMysql() {
  for (let attempt = 1; attempt <= 60; attempt += 1) {
    const ping = docker([
      'exec', containerName,
      'mysqladmin', 'ping', '-uroot', `-p${rootPassword}`, '--silent'
    ])
    if (ping.status === 0) return
    await delay(1000)
  }
  const logs = docker(['logs', containerName])
  assert.fail(`temporary MySQL 8 did not become ready\n${logs.stdout}\n${logs.stderr}`)
}

try {
  const start = docker([
    'run', '--detach', '--rm',
    '--name', containerName,
    '--network', 'none',
    '--env', `MYSQL_ROOT_PASSWORD=${rootPassword}`,
    'mysql:8.0',
    '--character-set-server=utf8mb4',
    '--collation-server=utf8mb4_0900_ai_ci'
  ])
  assert.equal(start.status, 0, `start isolated MySQL 8 container\n${start.stderr || start.stdout}`)
  containerStarted = true
  await waitForMysql()

  createDatabase('hive_old_schema', `
CREATE TABLE sales_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  delivery_date varchar(50) DEFAULT NULL,
  KEY idx_sales_delivery (delivery_date)
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  delivery_date datetime DEFAULT NULL,
  KEY idx_production_delivery (delivery_date)
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  delivery_date varchar(40) DEFAULT NULL,
  KEY idx_installation_delivery (delivery_date)
);
INSERT INTO sales_order (delivery_date) VALUES ('2026-07-01');
INSERT INTO production_order (delivery_date) VALUES ('2026-07-02 10:20:30');
INSERT INTO installation_task (delivery_date) VALUES ('待定');
`)
  let result = runMigration('hive_old_schema')
  assert.equal(result.status, 0, `migrate pure old schema\n${result.stderr || result.stdout}`)
  assertFinalColumns('hive_old_schema')
  assert.equal(
    mysqlOk('hive_old_schema', 'SELECT information_channel FROM sales_order WHERE id = 1;', 'read old sales row'),
    '历史交付日期：2026-07-01'
  )
  assert.equal(
    mysqlOk('hive_old_schema', 'SELECT information_channel FROM production_order WHERE id = 1;', 'read old production row'),
    '历史交付日期：2026-07-02'
  )
  assert.equal(
    mysqlOk('hive_old_schema', 'SELECT information_channel FROM installation_task WHERE id = 1;', 'read old installation row'),
    '历史交付日期：待定'
  )
  result = runMigration('hive_old_schema')
  assert.equal(result.status, 0, `rerun completed old-schema migration\n${result.stderr || result.stdout}`)

  createDatabase('hive_partial_schema', `
CREATE TABLE sales_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(80) DEFAULT NULL,
  delivery_date varchar(50) DEFAULT NULL
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel text DEFAULT NULL,
  delivery_date datetime DEFAULT NULL
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(200) DEFAULT NULL
);
INSERT INTO sales_order (information_channel, delivery_date) VALUES ('线下', '2026-07-03');
INSERT INTO production_order (information_channel, delivery_date) VALUES (NULL, '2026-07-04 08:00:00');
INSERT INTO installation_task (information_channel) VALUES ('现场登记');
`)
  result = runMigration('hive_partial_schema')
  assert.equal(result.status, 0, `migrate partial new schema\n${result.stderr || result.stdout}`)
  assertFinalColumns('hive_partial_schema')
  assert.equal(
    mysqlOk('hive_partial_schema', 'SELECT information_channel FROM sales_order WHERE id = 1;', 'read merged partial row'),
    '线下；历史交付日期：2026-07-03'
  )

  createDatabase('hive_completed_schema', `
CREATE TABLE sales_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(100) DEFAULT NULL,
  cancel_reason varchar(500) DEFAULT NULL
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(100) DEFAULT NULL
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(100) DEFAULT NULL
);
INSERT INTO sales_order (information_channel, cancel_reason) VALUES ('线上', '客户取消');
`)
  result = runMigration('hive_completed_schema')
  assert.equal(result.status, 0, `migrate already completed schema\n${result.stderr || result.stdout}`)
  assertFinalColumns('hive_completed_schema')
  assert.equal(
    mysqlOk('hive_completed_schema', 'SELECT CONCAT(information_channel, \'|\', cancel_reason) FROM sales_order WHERE id = 1;', 'read completed row'),
    '线上|客户取消'
  )

  createDatabase('hive_oversized_schema', `
CREATE TABLE sales_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(200) DEFAULT NULL,
  delivery_date varchar(50) DEFAULT NULL
);
INSERT INTO sales_order (information_channel, delivery_date) VALUES (REPEAT('x', 101), '2026-07-05');
`)
  result = runMigration('hive_oversized_schema')
  assert.notEqual(result.status, 0, 'oversized existing information_channel must fail')
  assert.match(result.stderr, /Existing information_channel value exceeds VARCHAR\(100\)/)
  assert.equal(
    mysqlOk('hive_oversized_schema', `
SELECT CONCAT(data_type, ':', character_maximum_length)
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'sales_order' AND column_name = 'information_channel';
`, 'verify oversized column was not narrowed'),
    'varchar:200'
  )
  assert.equal(
    mysqlOk('hive_oversized_schema', `
SELECT COUNT(*) FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'sales_order' AND column_name = 'delivery_date';
`, 'verify oversized scenario retained delivery_date'),
    '1'
  )

  createDatabase('hive_merge_conflict_schema', `
CREATE TABLE sales_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  information_channel varchar(100) DEFAULT NULL,
  delivery_date varchar(50) DEFAULT NULL
);
INSERT INTO sales_order (information_channel, delivery_date) VALUES (REPEAT('y', 90), '2026-07-06');
`)
  result = runMigration('hive_merge_conflict_schema')
  assert.notEqual(result.status, 0, 'lossy two-column merge must fail')
  assert.match(result.stderr, /Historical delivery data cannot fit in information_channel VARCHAR\(100\)/)
  assert.equal(
    mysqlOk('hive_merge_conflict_schema', `
SELECT CONCAT(CHAR_LENGTH(information_channel), ':', delivery_date) FROM sales_order WHERE id = 1;
`, 'verify merge conflict retained both source values'),
    '90:2026-07-06'
  )

  console.log('order information-channel MySQL 8 rehearsal passed')
} finally {
  if (containerStarted) docker(['rm', '--force', containerName])
}
