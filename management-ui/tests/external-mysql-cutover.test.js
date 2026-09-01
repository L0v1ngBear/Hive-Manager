import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

test('external MySQL mode is explicit and keeps the managed database as the default', () => {
  const envExample = read('deploy/.env.example')
  const compose = read('deploy/docker-compose.yml')
  const common = read('deploy/scripts/common.sh')

  assert.match(envExample, /^HIVE_DATABASE_MODE=COMPOSE$/m)
  for (const key of [
    'HIVE_DATABASE_JDBC_URL',
    'HIVE_DATABASE_APP_USERNAME',
    'HIVE_DATABASE_APP_PASSWORD',
    'HIVE_DATABASE_ADMIN_HOST',
    'HIVE_DATABASE_ADMIN_PORT',
    'HIVE_DATABASE_ADMIN_USERNAME',
    'HIVE_DATABASE_ADMIN_PASSWORD',
    'HIVE_DATABASE_NAME'
  ]) {
    assert.match(envExample, new RegExp(`^${key}=`, 'm'))
  }
  assert.match(compose, /SPRING_DATASOURCE_URL: \$\{HIVE_DATABASE_JDBC_URL:-jdbc:mysql:\/\/mysql:3306\/hive/)
  assert.match(compose, /SPRING_DATASOURCE_USERNAME: \$\{HIVE_DATABASE_APP_USERNAME:-\$\{DB_APP_USERNAME\}\}/)
  assert.match(compose, /SPRING_DATASOURCE_PASSWORD: \$\{HIVE_DATABASE_APP_PASSWORD:-\$\{DB_APP_PASSWORD\}\}/)
  const backendBlock = compose.slice(compose.indexOf('  backend:'), compose.indexOf('\n  nginx:'))
  assert.doesNotMatch(backendBlock, /^      mysql:\s*$/m)
  assert.match(common, /database_mode\(\)/)
  assert.match(common, /using_external_mysql\(\)/)
})

test('external cutover has a guarded preflight and routes release migration tools through one database boundary', () => {
  const preflight = read('deploy/scripts/verify-external-mysql.sh')
  const library = read('db-migrations/scripts/lib/database.sh')
  const migration = read('deploy/scripts/migrate-db.sh')
  const health = read('deploy/scripts/check-deploy-health.sh')
  const start = read('deploy/scripts/start.sh')
  const restart = read('deploy/scripts/restart.sh')

  assert.match(preflight, /HIVE_DATABASE_MODE=EXTERNAL/)
  assert.match(preflight, /check-database-state\.sh/)
  assert.match(preflight, /read-only/i)
  assert.match(library, /mysql_root_no_db\(\)/)
  assert.match(library, /MYSQL_PWD="\$\{HIVE_DATABASE_ADMIN_PASSWORD\}" mysql/)
  assert.match(library, /MYSQL_PWD="\$\{HIVE_DATABASE_ADMIN_PASSWORD\}" mysqldump/)
  assert.match(library, /docker compose exec -T mysql/)
  assert.match(migration, /source db-migrations\/scripts\/lib\/database\.sh/)
  assert.match(migration, /load_database_env \|\| exit 1/)
  assert.match(health, /HIVE_DATABASE_MODE/)
  assert.match(health, /HIVE_DATABASE_JDBC_URL HIVE_DATABASE_APP_USERNAME HIVE_DATABASE_APP_PASSWORD HIVE_DATABASE_ADMIN_HOST HIVE_DATABASE_ADMIN_PORT HIVE_DATABASE_ADMIN_USERNAME HIVE_DATABASE_ADMIN_PASSWORD/)
  assert.match(start, /if using_external_mysql; then[\s\S]*bundled mysql service will not be started/)
  assert.match(restart, /if using_external_mysql; then[\s\S]*redis backend nginx/)
})

test('external database boundary uses the host client and never Compose MySQL', () => {
  const bash = process.platform === 'win32' ? 'D:/Git/bin/bash.exe' : 'bash'
  if (!fs.existsSync(bash) && process.platform === 'win32') return

  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'hive-external-mysql-'))
  const binDir = path.join(root, 'bin')
  const logPath = path.join(root, 'mysql-commands.log')
  fs.mkdirSync(binDir)
  for (const command of ['mysql', 'mysqldump']) {
    const scriptPath = path.join(binDir, command)
    fs.writeFileSync(scriptPath, '#!/bin/sh\nprintf "%s %s\\n" "$(basename "$0")" "$*" >> "$MYSQL_COMMAND_LOG"\n')
    fs.chmodSync(scriptPath, 0o755)
  }
  fs.writeFileSync(path.join(root, '.env'), [
    'HIVE_DATABASE_MODE=EXTERNAL',
    'HIVE_DATABASE_JDBC_URL=jdbc:mysql://database.example:3306/hive?useSSL=true',
    'HIVE_DATABASE_APP_USERNAME=hive_app',
    'HIVE_DATABASE_APP_PASSWORD=app-secret',
    'HIVE_DATABASE_ADMIN_HOST=database.example',
    'HIVE_DATABASE_ADMIN_PORT=3307',
    'HIVE_DATABASE_ADMIN_USERNAME=hive_admin',
    'HIVE_DATABASE_ADMIN_PASSWORD=admin-secret',
    'HIVE_DATABASE_NAME=hive_external'
  ].join('\n'))

  try {
    const library = path.join(repoRoot, 'db-migrations/scripts/lib/database.sh').replaceAll('\\', '/')
    const result = spawnSync(bash, ['-c', [
      'source "$DATABASE_LIBRARY"',
      'load_database_env',
      'ensure_database_available',
      'mysql_root_db -N -B -e "SELECT 1"',
      'mysql_root_dump --no-data "$DATABASE_NAME"'
    ].join('; ')], {
      cwd: root,
      encoding: 'utf8',
      env: {
        ...process.env,
        DATABASE_LIBRARY: library,
        DEPLOY_DIR: root.replaceAll('\\', '/'),
        MYSQL_COMMAND_LOG: logPath.replaceAll('\\', '/'),
        PATH: `${binDir}${path.delimiter}${process.env.PATH || ''}`
      }
    })
    assert.equal(result.status, 0, result.stderr || result.stdout)
    const commands = fs.readFileSync(logPath, 'utf8')
    assert.match(commands, /mysql .*--host=database\.example --port=3307 --user=hive_admin/)
    assert.match(commands, /mysqldump .*--host=database\.example --port=3307 --user=hive_admin/)
    assert.match(commands, /hive_external/)
    assert.doesNotMatch(commands, /docker compose|mysql:3306/)
  } finally {
    fs.rmSync(root, { recursive: true, force: true })
  }
})
