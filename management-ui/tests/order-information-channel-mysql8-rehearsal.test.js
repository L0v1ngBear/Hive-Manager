import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import { deployRoot } from './deploy-test-root.js'

const migrationPath = path.join(
  deployRoot,
  'db-migrations/migrations/V20260713_001_order_information_channel_and_cancel_reason.sql'
)
const baselinePath = path.join(deployRoot, 'db-migrations/baseline/hive_schema_baseline.sql')
const manifestPath = path.join(deployRoot, 'db-migrations/migration_manifest.txt')
const migration = fs.readFileSync(migrationPath, 'utf8')
const baseline = fs.readFileSync(baselinePath, 'utf8')
const mysqlImage = 'mysql:8.0.42'
const dockerTimeout = 180_000
const containerName = `hive-order-migration-${process.pid}-${Date.now()}`
const rootPassword = 'HiveMigrationTest_20260713'

function docker(args, options = {}) {
  return spawnSync('docker', args, {
    encoding: 'utf8',
    timeout: dockerTimeout,
    maxBuffer: 16 * 1024 * 1024,
    ...options
  })
}

const dockerVersion = docker(['version', '--format', '{{.Server.Version}}'])
const dockerSkip = dockerVersion.status === 0
  ? false
  : 'Docker daemon unavailable; isolated MySQL 8 rehearsal not executed'

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

function createDatabase(database, setupSql = '') {
  mysqlOk(
    null,
    `DROP DATABASE IF EXISTS \`${database}\`; CREATE DATABASE \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`,
    `create isolated scenario database ${database}`
  )
  if (setupSql) mysqlOk(database, setupSql, `prepare isolated scenario ${database}`)
}

function sqlEscape(value) {
  return value.replaceAll("'", "''")
}

function registerBaselineHistory(database) {
  const manifestEntries = fs.readFileSync(manifestPath, 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))
  const rows = [{
    version: 'baseline/hive_schema_baseline',
    fileName: 'baseline/hive_schema_baseline.sql',
    checksum: createHash('sha256').update(fs.readFileSync(baselinePath)).digest('hex')
  }]
  for (const entry of manifestEntries) {
    rows.push({
      version: entry.slice(0, -4),
      fileName: entry,
      checksum: createHash('sha256')
        .update(fs.readFileSync(path.join(deployRoot, 'db-migrations', entry)))
        .digest('hex')
    })
  }
  const values = rows.map((row) =>
    `('${sqlEscape(row.version)}','${sqlEscape(row.fileName)}','${row.checksum}','SUCCESS')`
  ).join(',\n')
  mysqlOk(database, `
CREATE TABLE schema_migration_history (
  id bigint NOT NULL AUTO_INCREMENT,
  version varchar(120) NOT NULL,
  file_name varchar(255) NOT NULL,
  checksum_sha256 varchar(64) NOT NULL,
  status varchar(20) NOT NULL,
  error_message text DEFAULT NULL,
  executed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_schema_migration_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO schema_migration_history (version, file_name, checksum_sha256, status)
VALUES ${values};
`, 'register checksummed baseline migration state')
  return rows.length
}

function assertFinalColumns(database) {
  const definitions = mysqlOk(database, `
SELECT CONCAT(table_name, ':', column_name, ':', data_type, ':',
              character_maximum_length, ':', is_nullable, ':',
              IF(column_default IS NULL, 'NULL', column_default))
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (table_name IN ('sales_order', 'production_order', 'installation_task')
      AND column_name = 'information_channel')
    OR (table_name = 'sales_order' AND column_name = 'cancel_reason')
  )
ORDER BY table_name, column_name;
`, `inspect final order definitions in ${database}`).split(/\r?\n/)

  assert.deepEqual(definitions, [
    'installation_task:information_channel:varchar:100:YES:NULL',
    'production_order:information_channel:varchar:100:YES:NULL',
    'sales_order:cancel_reason:varchar:500:YES:NULL',
    'sales_order:information_channel:varchar:100:YES:NULL'
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

test('rehearses baseline install and historical upgrade on isolated MySQL 8', {
  skip: dockerSkip,
  timeout: 240_000
}, async () => {
  docker(['rm', '--force', containerName])
  try {
    const start = docker([
      'run', '--detach', '--rm',
      '--name', containerName,
      '--network', 'none',
      '--env', `MYSQL_ROOT_PASSWORD=${rootPassword}`,
      mysqlImage,
      '--character-set-server=utf8mb4',
      '--collation-server=utf8mb4_0900_ai_ci'
    ])
    assert.equal(start.status, 0, `start isolated ${mysqlImage}\n${start.stderr || start.stdout}`)
    await waitForMysql()
    assert.match(
      mysqlOk(null, 'SELECT VERSION();', 'read isolated MySQL version'),
      /^8\.0\.42(?:[-.]|$)/
    )

    createDatabase('hive_baseline_install')
    mysqlOk('hive_baseline_install', baseline, 'import latest schema-only baseline')
    const registeredRows = registerBaselineHistory('hive_baseline_install')
    assertFinalColumns('hive_baseline_install')
    assert.equal(
      mysqlOk('hive_baseline_install', 'SELECT COUNT(*) FROM schema_migration_history;', 'count baseline history'),
      String(registeredRows)
    )
    assert.equal(
      mysqlOk('hive_baseline_install', `
SELECT CONCAT(status, ':', checksum_sha256)
FROM schema_migration_history
WHERE version = 'baseline/hive_schema_baseline';
`, 'verify baseline marker'),
      `SUCCESS:${createHash('sha256').update(fs.readFileSync(baselinePath)).digest('hex')}`
    )

    createDatabase('hive_old_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  delivery_date varchar(50) DEFAULT NULL,
  KEY idx_sales_delivery (delivery_date)
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  order_id varchar(50) NOT NULL,
  delivery_date datetime DEFAULT NULL,
  UNIQUE KEY uk_production_order_id (order_id),
  KEY idx_production_delivery (delivery_date)
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  tenant_code varchar(64) NOT NULL,
  order_id varchar(64) NOT NULL,
  delivery_date date DEFAULT NULL,
  UNIQUE KEY uk_installation_task_order (tenant_code, order_id),
  KEY idx_installation_delivery (delivery_date)
);
INSERT INTO sales_order (order_id, delivery_date) VALUES ('SO-1', '2026-07-01 09:08:07');
INSERT INTO production_order (order_id, delivery_date) VALUES ('PO-1', '2026-07-02 10:20:30');
INSERT INTO installation_task (tenant_code, order_id, delivery_date) VALUES ('T1', 'IT-1', '2026-07-03');
`)
    let result = runMigration('hive_old_schema')
    assert.equal(result.status, 0, `migrate historical schema\n${result.stderr || result.stdout}`)
    assertFinalColumns('hive_old_schema')
    assert.equal(
      mysqlOk('hive_old_schema', 'SELECT information_channel FROM sales_order WHERE order_id = \'SO-1\';', 'read string datetime'),
      '历史交付日期：2026-07-01 09:08:07'
    )
    assert.equal(
      mysqlOk('hive_old_schema', 'SELECT information_channel FROM production_order WHERE order_id = \'PO-1\';', 'read DATETIME'),
      '历史交付日期：2026-07-02 10:20:30'
    )
    assert.equal(
      mysqlOk('hive_old_schema', 'SELECT information_channel FROM installation_task WHERE order_id = \'IT-1\';', 'read DATE'),
      '历史交付日期：2026-07-03'
    )
    result = runMigration('hive_old_schema')
    assert.equal(result.status, 0, `rerun completed historical migration\n${result.stderr || result.stdout}`)

    createDatabase('hive_partial_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  information_channel varchar(80) NOT NULL DEFAULT 'unknown',
  cancel_reason varchar(700) NOT NULL DEFAULT 'unknown',
  delivery_date varchar(50) DEFAULT NULL
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  order_id varchar(50) NOT NULL,
  information_channel text DEFAULT NULL,
  delivery_date datetime DEFAULT NULL,
  UNIQUE KEY uk_production_order_id (order_id)
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  tenant_code varchar(64) NOT NULL,
  order_id varchar(64) NOT NULL,
  information_channel varchar(200) DEFAULT NULL,
  UNIQUE KEY uk_installation_task_order (tenant_code, order_id)
);
INSERT INTO sales_order (order_id, information_channel, cancel_reason, delivery_date)
VALUES ('SO-2', '线下', '客户调整', '2026-07-04');
INSERT INTO production_order (order_id, delivery_date) VALUES ('PO-2', '2026-07-05 08:00:00');
INSERT INTO installation_task (tenant_code, order_id, information_channel) VALUES ('T1', 'IT-2', '现场登记');
`)
    result = runMigration('hive_partial_schema')
    assert.equal(result.status, 0, `migrate partial new schema\n${result.stderr || result.stdout}`)
    assertFinalColumns('hive_partial_schema')
    assert.equal(
      mysqlOk('hive_partial_schema', 'SELECT information_channel FROM sales_order WHERE order_id = \'SO-2\';', 'read merged row'),
      '线下；历史交付日期：2026-07-04'
    )

    createDatabase('hive_completed_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  information_channel varchar(100) DEFAULT NULL,
  cancel_reason varchar(500) DEFAULT NULL
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  order_id varchar(50) NOT NULL,
  information_channel varchar(100) DEFAULT NULL,
  UNIQUE KEY uk_production_order_id (order_id)
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  tenant_code varchar(64) NOT NULL,
  order_id varchar(64) NOT NULL,
  information_channel varchar(100) DEFAULT NULL,
  UNIQUE KEY uk_installation_task_order (tenant_code, order_id)
);
INSERT INTO sales_order (order_id, information_channel, cancel_reason)
VALUES ('SO-DONE', '线上', '客户取消');
`)
    result = runMigration('hive_completed_schema')
    assert.equal(result.status, 0, `rerun migration on completed schema\n${result.stderr || result.stdout}`)
    assertFinalColumns('hive_completed_schema')
    assert.equal(
      mysqlOk(
        'hive_completed_schema',
        "SELECT CONCAT(information_channel, '|', cancel_reason) FROM sales_order WHERE order_id = 'SO-DONE';",
        'verify completed values remain unchanged'
      ),
      '线上|客户取消'
    )

    createDatabase('hive_oversized_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  information_channel varchar(200) DEFAULT NULL,
  delivery_date varchar(50) DEFAULT NULL
);
INSERT INTO sales_order (order_id, information_channel, delivery_date)
VALUES ('SO-3', REPEAT('x', 101), '2026-07-06');
`)
    result = runMigration('hive_oversized_schema')
    assert.notEqual(result.status, 0, 'oversized information_channel must fail')
    assert.match(result.stderr, /Existing information_channel value exceeds VARCHAR\(100\)/)
    assert.equal(
      mysqlOk('hive_oversized_schema', `
SELECT CONCAT(character_maximum_length, ':',
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'sales_order' AND column_name = 'delivery_date'))
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'sales_order' AND column_name = 'information_channel';
`, 'verify information conflict retained old schema'),
      '200:1'
    )

    createDatabase('hive_merge_conflict_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  information_channel varchar(100) DEFAULT NULL,
  delivery_date varchar(50) DEFAULT NULL
);
INSERT INTO sales_order (order_id, information_channel, delivery_date)
VALUES ('SO-4', REPEAT('y', 90), '2026-07-07');
`)
    result = runMigration('hive_merge_conflict_schema')
    assert.notEqual(result.status, 0, 'lossy two-column merge must fail')
    assert.match(result.stderr, /Historical delivery data cannot fit in information_channel VARCHAR\(100\)/)
    assert.equal(
      mysqlOk('hive_merge_conflict_schema', `
SELECT CONCAT(CHAR_LENGTH(information_channel), ':', delivery_date) FROM sales_order WHERE order_id = 'SO-4';
`, 'verify merge conflict retained both source values'),
      '90:2026-07-07'
    )

    createDatabase('hive_cancel_conflict_schema', `
CREATE TABLE sales_order (
  order_id varchar(50) PRIMARY KEY,
  information_channel varchar(100) DEFAULT NULL,
  cancel_reason text DEFAULT NULL
);
CREATE TABLE production_order (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  order_id varchar(50) NOT NULL,
  information_channel varchar(100) DEFAULT NULL,
  UNIQUE KEY uk_production_order_id (order_id)
);
CREATE TABLE installation_task (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  tenant_code varchar(64) NOT NULL,
  order_id varchar(64) NOT NULL,
  information_channel varchar(100) DEFAULT NULL,
  UNIQUE KEY uk_installation_task_order (tenant_code, order_id)
);
INSERT INTO sales_order (order_id, cancel_reason) VALUES ('SO-5', REPEAT('z', 501));
`)
    result = runMigration('hive_cancel_conflict_schema')
    assert.notEqual(result.status, 0, 'oversized cancel_reason must fail')
    assert.match(result.stderr, /Existing cancel_reason value exceeds VARCHAR\(500\)/)
    assert.equal(
      mysqlOk('hive_cancel_conflict_schema', 'SELECT CHAR_LENGTH(cancel_reason) FROM sales_order WHERE order_id = \'SO-5\';', 'verify cancel reason retained'),
      '501'
    )
  } finally {
    docker(['rm', '--force', containerName])
  }
})
