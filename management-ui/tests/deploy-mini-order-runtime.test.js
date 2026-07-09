import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const deployRoot = path.resolve('C:/Users/HUAWEI/Desktop/hive部署_全新配置')

function read(relativePath) {
  return fs.readFileSync(path.join(deployRoot, relativePath), 'utf8')
}

const smokeTest = read('scripts/smoke-test.sh')

assert.ok(
  smokeTest.includes('BOOT-INF/classes/my/hive_back/api/order/OrderController.class'),
  'online smoke test must verify the running mini backend jar contains the unified order controller'
)

assert.ok(
  smokeTest.includes("jar tf '${jar_path}'") && smokeTest.includes('"/app/app.jar"'),
  'online smoke test must inspect the running mini backend jar instead of only checking the package file exists'
)

assert.ok(
  smokeTest.includes('mini unified order list api exists')
    && smokeTest.includes('https://hellohive.top/api/orders/list')
    && smokeTest.includes('mini unified order summary api exists')
    && smokeTest.includes('https://hellohive.top/api/orders/status-summary'),
  'online smoke test must fail if the unified mini order list or summary API returns 404'
)

console.log('deploy mini order runtime checks passed')
