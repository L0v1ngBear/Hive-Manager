import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repositoryRoot, relativePath), 'utf8')

const compose = read('deploy/docker-compose.yml')
const envExample = read('deploy/.env.example')
const productionConfig = read('management/src/main/resources/application-prod.yaml')
const deploymentGuide = read('docs/deployment/unified-backend-deployment.md')

function serviceBlock(name, nextName) {
  const start = compose.indexOf(`  ${name}:`)
  const end = nextName ? compose.indexOf(`  ${nextName}:`, start + 1) : compose.indexOf('\nnetworks:', start)
  assert.notEqual(start, -1, `missing Compose service ${name}`)
  assert.notEqual(end, -1, `cannot find the end of Compose service ${name}`)
  return compose.slice(start, end)
}

test('Compose uses one shared internal subnet for proxy trust and hive-net IPAM', () => {
  assert.match(envExample, /^HIVE_DOCKER_SUBNET=172\.30\.0\.0\/24$/m)
  assert.match(
    compose,
    /hive-net:\r?\n\s+name: hive-net\r?\n\s+ipam:\r?\n\s+config:\r?\n\s+- subnet: \$\{HIVE_DOCKER_SUBNET:-172\.30\.0\.0\/24\}/
  )

  const backend = serviceBlock('backend', 'nginx')
  assert.match(
    backend,
    /TRUSTED_PROXY_CIDRS:\s*["']127\.0\.0\.0\/8,::1\/128,\$\{HIVE_DOCKER_SUBNET:-172\.30\.0\.0\/24\}["']/
  )
  assert.match(deploymentGuide, /HIVE_DOCKER_SUBNET/)
  assert.match(deploymentGuide, /(?:collision|overlap|VPN)/i)
})

test('Nginx is the only externally published HTTP entry', () => {
  const backend = serviceBlock('backend', 'nginx')
  const nginx = serviceBlock('nginx')

  assert.match(backend, /\n    expose:\r?\n\s+- "8080"/)
  assert.doesNotMatch(backend, /\n    ports:/)
  assert.match(nginx, /\n    ports:\r?\n\s+- "80:80"\r?\n\s+- "443:443"/)
  assert.doesNotMatch(compose, /0\.0\.0\.0:8080|(?:^|:)8080:8080/m)
})

test('operation audit defaults include order auth and organization everywhere', () => {
  assert.match(
    compose,
    /OPERATION_LOG_RECORDED_MODULES:\s*\$\{OPERATION_LOG_RECORDED_MODULES:-order,auth,organization\}/
  )
  assert.match(envExample, /^OPERATION_LOG_RECORDED_MODULES=order,auth,organization$/m)
  assert.match(
    productionConfig,
    /recorded-modules:\s*\$\{OPERATION_LOG_RECORDED_MODULES:order,auth,organization\}/
  )
})
