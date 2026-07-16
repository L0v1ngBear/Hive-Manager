import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

function collectSource(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolute = path.join(directory, entry.name)
    if (entry.isDirectory()) return collectSource(absolute)
    return /\.(?:js|vue)$/.test(entry.name) ? [fs.readFileSync(absolute, 'utf8')] : []
  }).join('\n')
}

test('management UI uses only the unified API prefix', () => {
  const source = collectSource(path.join(projectRoot, 'src'))
  const vite = read('vite.config.js')
  const request = read('src/utils/request.js')

  assert.doesNotMatch(`${source}\n${vite}`, /['"`]\/web(?:\/|['"`])/)
  assert.match(request, /VITE_API_BASE_URL\s*\|\|\s*['"]\/api['"]/)
  for (const environment of ['.env', '.env.development', '.env.production']) {
    assert.match(read(environment), /^VITE_API_BASE_URL=\/api\s*$/m)
  }
  assert.match(vite, /['"]\/api['"]\s*:\s*\{/)
})

test('authentication, orders, approvals, quality and installation use canonical routes', () => {
  const contracts = new Map([
    ['src/api/auth.js', [
      '/auth/admin/login',
      '/auth/admin/password-reset/code',
      '/auth/admin/password-reset',
      '/auth/admin/join-organization/code',
      '/auth/admin/join-organization',
      '/auth/admin/initial-password',
      '/auth/admin/scan-login/session',
      '/auth/admin/scan-login/status'
    ]],
    ['src/views/function/order/api/order.js', [
      "url: '/orders'",
      '/orders/status-summary',
      '/orders/attachment',
      '/orders/flow-print-task',
      '/status-log/',
      '/operation-logs'
    ]],
    ['src/views/function/approval/api/approval.js', [
      "url: '/approval/leave'",
      "url: '/approval/finance'",
      "url: '/approval/finance/attachment'",
      "url: '/approval/resignation'",
      "url: '/approval/quality'",
      "url: '/approval/order'"
    ]],
    ['src/views/function/badProduct/api/badProduct.js', [
      '/quality/list',
      '/quality/save',
      '/quality/attachment/upload',
      '/quality/attachment/download',
      '/quality/process'
    ]],
    ['src/views/function/installationTask/api/installationTask.js', [
      '/installation-tasks/page',
      '/installation-tasks/status',
      '/installation-tasks/attachment/upload',
      '/installation-tasks/attachment/download'
    ]]
  ])

  for (const [file, requiredRoutes] of contracts) {
    const source = read(file)
    for (const route of requiredRoutes) {
      assert.ok(source.includes(route), `${file} is missing canonical route ${route}`)
    }
  }

  const apiSource = [...contracts.keys()].map(read).join('\n')
  assert.doesNotMatch(apiSource, /['"`]\/(?:order|bad-product|installation-task)(?:\/|['"`])/)
  assert.doesNotMatch(apiSource, /\/approval\/(?:leave|finance|resignation|quality|order)\/(?:list|submit)(?:['"`])/)
  assert.doesNotMatch(apiSource, /\/approval\/finance\/attachment\/(?:upload|download)/)
  assert.doesNotMatch(read('src/views/function/order/api/order.js'), /checkOrderModuleHealth|\/order\/health/)
})
