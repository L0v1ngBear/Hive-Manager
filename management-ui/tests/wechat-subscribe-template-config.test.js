import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const repoRoot = path.resolve(import.meta.dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

test('after-sales assignment subscription template uses the approved template and field keys', () => {
  const templateId = 'v_vEvmaNMXDHYEHdm0FNY8j_YqCmdT_Tx7-BHOCRVBU'
  const application = read('management/src/main/resources/application.yaml')
  const compose = read('deploy/docker-compose.yml')
  const environmentExample = read('deploy/.env.example')
  const service = read('management/src/main/java/my/hive/infrastructure/wechat/WechatSubscribeService.java')

  assert.match(application, new RegExp(`todo-template-id: \\$\\{WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID:${templateId}\\}`))
  assert.match(application, /todo-time-key: \$\{WECHAT_SUBSCRIBE_TODO_TIME_KEY:time1\}/)
  assert.match(compose, new RegExp(`WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID: \\$\\{WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID:-${templateId}\\}`))
  assert.match(compose, /WECHAT_SUBSCRIBE_TODO_TIME_KEY: \$\{WECHAT_SUBSCRIBE_TODO_TIME_KEY:-time1\}/)
  assert.match(environmentExample, new RegExp(`WECHAT_SUBSCRIBE_TODO_TEMPLATE_ID=${templateId}`))
  assert.match(environmentExample, /WECHAT_SUBSCRIBE_TODO_TIME_KEY=time1/)
  assert.match(service, /@Value\("\$\{wechat\.mini-program\.subscribe\.todo-time-key:time1\}"\)/)
})
