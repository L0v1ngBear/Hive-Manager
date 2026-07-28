import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = path.resolve(projectRoot, '..')
const readUi = (relativePath) => fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')
const readRepo = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

test('web login exposes WeChat quick login with first-use binding and tenant selection', () => {
  const login = readUi('src/views/Login.vue')
  const api = readUi('src/api/auth.js')

  for (const route of [
    '/auth/admin/wechat-login/config',
    '/auth/admin/wechat-login/session',
    '/auth/admin/wechat-login/complete',
    '/auth/admin/wechat-login/bind',
    '/auth/admin/wechat-login/select'
  ]) {
    assert.match(api, new RegExp(route.replaceAll('/', '\\/')))
  }
  assert.match(login, /微信快捷登录/)
  assert.match(login, /首次使用需验证并绑定现有 Hive 账号/)
  assert.match(login, /finishWebWechatCallback/)
  assert.match(login, /BIND_REQUIRED/)
  assert.match(login, /TENANT_SELECTION_REQUIRED/)
  assert.match(login, /hostname !== 'open\.weixin\.qq\.com'/)
  assert.doesNotMatch(login, /WECHAT_WEB_LOGIN_APP_SECRET|appSecret/)
})

test('web WeChat identity persistence stores only a keyed subject hash', () => {
  const migration = readRepo('db-migrations/migrations/V20260728_001_web_wechat_login.sql')
  const mapper = readRepo('management/src/main/java/my/hive/domain/auth/mapper/AuthMapper.java')
  const client = readRepo('management/src/main/java/my/hive/infrastructure/wechat/WechatWebLoginClient.java')
  const manifest = readRepo('db-migrations/migration_manifest.txt')

  assert.match(migration, /subject_hash CHAR\(64\) NOT NULL/i)
  assert.match(migration, /uk_wechat_identity_subject_tenant/)
  assert.match(migration, /uk_wechat_identity_user_tenant/)
  assert.doesNotMatch(migration, /^\s*(?:openid|unionid|access_token|refresh_token)\s+(?:varchar|text|char)/im)
  assert.match(mapper, /insertWebWechatIdentity/)
  assert.match(client, /scope=snsapi_login/)
  assert.doesNotMatch(client, /log\.(?:info|debug|warn|error).*appSecret/)
  assert.match(manifest, /migrations\/V20260728_001_web_wechat_login\.sql/)
})

test('deployment keeps website WeChat credentials server-side and disabled by default', () => {
  const environment = readRepo('deploy/.env.example')
  const compose = readRepo('deploy/docker-compose.yml')
  const application = readRepo('management/src/main/resources/application.yaml')

  assert.match(environment, /^WECHAT_WEB_LOGIN_ENABLED=false$/m)
  assert.match(environment, /^WECHAT_WEB_LOGIN_APP_ID=$/m)
  assert.match(environment, /^WECHAT_WEB_LOGIN_APP_SECRET=$/m)
  assert.match(environment, /^WECHAT_WEB_LOGIN_CALLBACK_URI=https:\/\//m)
  assert.match(compose, /WECHAT_WEB_LOGIN_APP_SECRET: \$\{WECHAT_WEB_LOGIN_APP_SECRET:-\}/)
  assert.match(application, /app-secret: \$\{WECHAT_WEB_LOGIN_APP_SECRET:\}/)
  assert.doesNotMatch(`${environment}\n${compose}\n${application}`, /wx[0-9a-f]{16}|server-only-secret/i)
})
