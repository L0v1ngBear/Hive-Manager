import assert from 'node:assert/strict'
import test from 'node:test'

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
