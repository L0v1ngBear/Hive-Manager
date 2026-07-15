import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'

const repositoryRoot = path.resolve(process.cwd(), '..')
const deployRoot = path.join(repositoryRoot, 'deploy')
const read = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')

function collectFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolute = path.join(directory, entry.name)
    if (entry.isDirectory()) return collectFiles(absolute)
    return [absolute]
  })
}

test('Compose defines exactly one Hive backend business service', () => {
  const compose = read('docker-compose.yml')
  const businessBuilds = [...compose.matchAll(/^  ([a-zA-Z0-9_-]+):\r?\n(?:^(?:    .*)?\r?\n)*?^    build:\s*\.\/backend\s*$/gm)]

  assert.equal(businessBuilds.length, 1)
  assert.equal(businessBuilds[0][1], 'backend')
  assert.match(compose, /^    container_name:\s*hive-backend\s*$/m)
  assert.match(compose, /^      - "8080"\s*$/m)
  assert.match(compose, /\.\/logs\/backend:\/app\/logs/)
  assert.match(compose, /\.\/uploads:\/app\/uploads/)
  assert.match(compose, /XXL_JOB_EXECUTOR_APP_NAME:\s*\$\{XXL_JOB_EXECUTOR_APP_NAME:-hive-backend\}/)
  assert.doesNotMatch(compose, /mini-backend|management-backend|backend-1|SERVER_SERVLET_CONTEXT_PATH|\/web/i)
})

test('nginx and operational scripts target only the unified backend', () => {
  const nginx = read('nginx/conf.d/hive.conf')
  assert.match(nginx, /location \/api\//)
  assert.match(nginx, /proxy_pass\s+http:\/\/backend:8080/)
  assert.doesNotMatch(nginx, /location \/web\/|mini-backend|management-backend|backend-1/i)

  const scripts = collectFiles(path.join(deployRoot, 'scripts'))
    .filter((file) => /\.(?:sh|ps1|cmd)$/.test(file))
    .map((file) => fs.readFileSync(file, 'utf8'))
    .join('\n')

  assert.match(scripts, /hive-backend/)
  assert.doesNotMatch(scripts, /mini-backend|management-backend|backend-1|management-backend-1/i)
})

test('repository deployment source excludes runtime secrets and artifacts', () => {
  const files = collectFiles(deployRoot).map((file) => path.relative(deployRoot, file).replaceAll('\\', '/'))
  assert.ok(files.includes('.env.example'))
  assert.ok(files.includes('backend/Dockerfile'))
  assert.ok(!files.includes('.env'))
  assert.ok(!files.some((file) => /\.(?:jar|key|pem|p12|pfx)$/i.test(file)))
  assert.ok(!files.some((file) => /(?:^|\/)(?:data|logs|uploads|letsencrypt)(?:\/|$)/i.test(file)))
})
