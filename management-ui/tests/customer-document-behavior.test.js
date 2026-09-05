import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readRepo = (path) => readFileSync(new URL(`../../${path}`, import.meta.url), 'utf8')

async function loadRequiredModule(path, description) {
  try {
    return await import(path)
  } catch (error) {
    assert.fail(`${description} 尚未实现：${error.message}`)
  }
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

test('客户列表仅提交最后一次请求并由最后一次请求结束 loading', async () => {
  const { createLatestRequestRunner } = await loadRequiredModule(
    '../src/utils/latestRequest.js',
    '最新请求执行器'
  )
  const first = deferred()
  const second = deferred()
  const committed = []
  const loading = []
  const runner = createLatestRequestRunner({
    onLoading: (value) => loading.push(value),
    onSuccess: (value) => committed.push(value)
  })

  const firstRun = runner.run(() => first.promise)
  const secondRun = runner.run(() => second.promise)
  first.resolve('旧筛选')
  await firstRun
  assert.deepEqual(committed, [])
  assert.deepEqual(loading, [true, true])

  second.resolve('新筛选')
  await secondRun
  assert.deepEqual(committed, ['新筛选'])
  assert.deepEqual(loading, [true, true, false])
})

test('关闭客户详情会使在途响应失效', async () => {
  const { createLatestRequestRunner } = await loadRequiredModule(
    '../src/utils/latestRequest.js',
    '最新请求执行器'
  )
  const pending = deferred()
  const committed = []
  const runner = createLatestRequestRunner({ onSuccess: (value) => committed.push(value) })

  const request = runner.run(() => pending.promise)
  runner.invalidate()
  pending.resolve({ id: 1 })
  await request

  assert.deepEqual(committed, [])
})

test('客户详情错误分类区分 HTTP 与业务状态，并保留真正空响应', async () => {
  const { resolveCustomerDetailOutcome } = await loadRequiredModule(
    '../src/views/function/customer/customerState.js',
    '客户详情状态分类器'
  )

  assert.equal(resolveCustomerDetailOutcome({ response: { status: 401 } }).error.title, '登录状态已失效')
  assert.equal(resolveCustomerDetailOutcome({ code: 403 }).error.title, '暂无客户详情权限')
  assert.equal(resolveCustomerDetailOutcome({ response: { data: { code: 403 } } }).error.title, '暂无客户详情权限')
  assert.equal(resolveCustomerDetailOutcome({ response: { status: 503 } }).error.title, '客户服务暂时不可用')
  assert.equal(resolveCustomerDetailOutcome(new Error('network')).error.title, '客户详情加载失败')
  assert.deepEqual(resolveCustomerDetailOutcome(null), { empty: true, error: null })
})

test('客户删除使用独立权限，并保护已有业务关联的数据', () => {
  const page = readRepo('management-ui/src/views/function/customer/customer.vue')
  const api = readRepo('management-ui/src/views/function/customer/api/customer.js')
  const controller = readRepo('management/src/main/java/my/hive/api/customer/CustomerController.java')
  const service = readRepo('management/src/main/java/my/hive/domain/customer/service/CustomerService.java')

  assert.match(page, /userStore\.hasPermission\('customer:delete'\)/)
  assert.match(page, /确认删除客户/)
  assert.match(api, /url: `\/customer\/\$\{id\}`/)
  assert.match(api, /method: 'delete'/)
  assert.match(controller, /@DeleteMapping\("\/\{id\}"\)/)
  assert.match(controller, /CODE_CUSTOMER_DELETE/)
  assert.match(service, /SalesOrder::getCustomerName, customer\.getCustomerName\(\)/)
  assert.match(service, /AfterSalesTicket::getCustomerName, customer\.getCustomerName\(\)/)
  assert.match(service, /PriceCustomerOverride::getCustomerId, customer\.getId\(\)/)
})

test('文档面包屑无权限时所有目录导航均不调用 API', async () => {
  const { createDocumentNavigator } = await loadRequiredModule(
    '../src/views/function/document/documentNavigation.js',
    '文档目录导航器'
  )
  const calls = []
  const navigator = createDocumentNavigator({
    canNavigate: () => false,
    fetchDocuments: async (id) => calls.push(id)
  })

  assert.equal(await navigator.navigateUp(3), false)
  assert.equal(await navigator.navigateTo(2), false)
  assert.equal(await navigator.openFolder(9), false)
  assert.equal(await navigator.goRoot(), false)
  assert.deepEqual(calls, [])
})

test('结构化导出按当前动态列顺序生成真实 headers 与 rows', async () => {
  const { buildStructuredExportData } = await loadRequiredModule(
    '../src/utils/structuredTableExport.js',
    '结构化表格导出映射器'
  )
  const columns = [
    { key: 'type', label: '类型' },
    { key: 'name', label: '名称' }
  ]
  const rows = [{ name: '合同.pdf', type: 1 }]

  assert.deepEqual(
    buildStructuredExportData(columns, rows, (row, column) => row[column.key]),
    { headers: ['类型', '名称'], rows: [[1, '合同.pdf']] }
  )
})
