import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const installationTask = readFileSync(
  new URL('../src/views/function/installationTask/installationTask.vue', import.meta.url),
  'utf8'
)
const quality = readFileSync(
  new URL('../src/views/function/badProduct/badProduct.vue', import.meta.url),
  'utf8'
)
const attachmentUpload = readFileSync(new URL('../src/components/DragAttachmentUpload.vue', import.meta.url), 'utf8')

const requiredElements = [
  ['el-table', 'ElTable'],
  ['el-pagination', 'ElPagination'],
  ['el-form', 'ElForm'],
  ['el-input', 'ElInput'],
  ['el-select', 'ElSelect'],
  ['el-tag', 'ElTag'],
  ['el-empty', 'ElEmpty']
]

function assertElementPlusPage(source, pageName) {
  const elementPlusImport = source.match(
    /import\s*\{([\s\S]*?)\}\s*from\s*['"]element-plus['"]/
  )

  assert.ok(elementPlusImport, `${pageName} should explicitly import Element Plus controls`)
  assert.match(source, /<el-(?:dialog|drawer)\b/, `${pageName} should use an Element Plus overlay`)
  assert.match(source, /v-loading\b/, `${pageName} should bind Element Plus loading state`)

  for (const [tag, component] of requiredElements) {
    assert.match(source, new RegExp(`<${tag}\\b`), `${pageName} should render ${tag}`)
    assert.match(
      elementPlusImport[1],
      new RegExp(`\\b${component}\\b`),
      `${pageName} should explicitly import ${component}`
    )
  }
}

function assertChineseProductionCopy(source, pageName, requiredCopy, forbiddenCopy) {
  for (const copy of requiredCopy) {
    assert.ok(source.includes(copy), `${pageName} should retain Chinese copy: ${copy}`)
  }

  for (const placeholder of forbiddenCopy) {
    assert.ok(
      !source.includes(placeholder),
      `${pageName} should not retain English placeholder: ${placeholder}`
    )
  }

  assert.doesNotMatch(
    source,
    /鐢熶骇|瀹夎|璐ㄩ噺|鐗╂祦|闄勪欢/,
    `${pageName} should not contain mojibake`
  )
}

test('migrates installation task controls to Element Plus without losing attachments', () => {
  assertElementPlusPage(installationTask, 'installation task')
  assert.match(installationTask, /import DragAttachmentUpload from /)
})

test('migrates quality controls to Element Plus without losing process dependencies', () => {
  assertElementPlusPage(quality, 'quality')
  assert.match(quality, /import DragAttachmentUpload from /)
  assert.match(quality, /import BusinessTimeCorrectionPanel from /)
})

test('retains Chinese production copy without English placeholders or mojibake', () => {
  assertChineseProductionCopy(
    installationTask,
    'installation task',
    ['安装任务', '刷新', '综合搜索', '客户', '安装状态', '暂无安装任务', '操作', '处理', '物流公司', '物流单号', '施工人员', '特殊及异常情况说明', '验收附件', '取消', '保存'],
    ['>Installation Tasks<', '>Refresh<', 'label="Search"', 'label="Customer"', 'label="Status"', 'description="No tasks"', 'label="Actions"', '>Edit<', 'title="Installation task"', '>Cancel<', '>Save<']
  )
  assertChineseProductionCopy(
    quality,
    'quality',
    ['查询', '重置', '全部状态', '待处理', '审核中', '已处理', '全部类型', '操作', '详情', '编辑', '关联订单', '质量类型', '异常数量', '损失金额', '问题描述', '附件凭证', '负责人', '处理方式', '处理措施', '改进方案', '处理备注', '取消', '保存', '提交审核'],
    ['label="Search"', 'label="Status"', 'label="Pending"', 'label="In review"', 'label="Processed"', 'label="Type"', 'label="Date"', 'label="Actions"', '>Detail<', '>Edit<', '>Process<', 'label="Order"', 'label="Description"', 'label="Attachment"', '>Cancel<', '>Save<', '>Submit<']
  )
})

test('separates cancel click handlers from Element Plus before-close callbacks', () => {
  assert.match(installationTask, /:before-close="beforeCloseEditor"/)
  assert.match(installationTask, /@click="closeEditor"/)
  assert.match(installationTask, /function closeEditor\(\)\s*\{/)
  assert.match(installationTask, /function beforeCloseEditor\(done\)\s*\{/)
  assert.match(installationTask, /typeof done === 'function'/)
  assert.doesNotMatch(installationTask, /function closeEditor\(done\)/)

  for (const [clickHandler, beforeCloseHandler] of [
    ['closeForm', 'beforeCloseForm'],
    ['closeProcess', 'beforeCloseProcess']
  ]) {
    assert.match(quality, new RegExp(`@click="${clickHandler}"`))
    assert.match(quality, new RegExp(`:before-close="${beforeCloseHandler}"`))
    assert.match(quality, new RegExp(`function ${clickHandler}\\(\\)\\s*\\{`))
    assert.match(quality, new RegExp(`function ${beforeCloseHandler}\\(done\\)\\s*\\{`))
    assert.doesNotMatch(quality, new RegExp(`function ${clickHandler}\\(done\\)`))
  }
  assert.match(quality, /typeof done === 'function'/)
})

function assertPersistentRequestStates(source, pageName, retryHandler) {
  assert.match(source, /const requestState = ref\('loading'\)/, `${pageName} should persist request state`)
  assert.match(source, /const requestErrorMessage = ref\(''\)/, `${pageName} should persist request errors`)
  assert.match(source, /rows\.value = \[\]/, `${pageName} should clear stale rows`)
  assert.match(source, /status === 401/, `${pageName} should identify unauthenticated requests`)
  assert.match(source, /status === 403/, `${pageName} should identify forbidden requests`)
  assert.match(source, /status >= 500/, `${pageName} should identify server failures`)
  assert.match(source, /网络连接异常/, `${pageName} should identify network failures`)
  assert.match(source, /服务暂时不可用/, `${pageName} should identify server failures separately`)
  assert.match(source, /state: 'permission'/)
  assert.match(source, /state: 'error'/)
  assert.match(source, /requestState\.value = failure\.state/)
  assert.match(source, /requestState\.value = 'ready'/)
  assert.match(source, /v-if="requestState === 'loading'"/)
  assert.match(source, /v-else-if="requestState === 'permission'"/)
  assert.match(source, /v-else-if="requestState === 'error'"/)
  assert.match(source, /重新加载/)
  assert.match(source, new RegExp(`@click="${retryHandler}"`))
}

test('keeps mutually exclusive persistent request states and retry controls', () => {
  assertPersistentRequestStates(installationTask, 'installation task', 'loadTasks')
  assertPersistentRequestStates(quality, 'quality', 'fetchData')
})

test('latest request ownership rejects stale success, failure and loading writes', async () => {
  const { createLatestRequest } = await import('../src/utils/task7LatestRequest.js')
  const gate = createLatestRequest()
  const state = { rows: [], error: '', loading: false }
  const first = gate.begin()
  const second = gate.begin()
  assert.equal(first.commit(() => { state.rows = ['旧']; state.error = '旧错误'; state.loading = false }), false)
  assert.equal(second.commit(() => { state.rows = ['新']; state.loading = false }), true)
  assert.deepEqual(state, { rows: ['新'], error: '', loading: false })
})

test('installation commands use their real permission combinations', async () => {
  const { resolveInstallationAccess } = await import('../src/views/function/installationTask/installationAccess.js')
  const access = resolveInstallationAccess((code) => ['installation:update', 'installation:attachment:download'].includes(code))
  assert.deepEqual(access, {
    canUpdate: true,
    canUpload: false,
    canDownload: true,
    canAttach: false
  })
  assert.match(installationTask, /useUserStore/)
  for (const permission of ['installation:update', 'installation:attachment:upload', 'installation:attachment:download']) {
    assert.match(installationTask, new RegExp(permission.replace('*', '\\*')))
  }
  assert.match(installationTask, /if \(!canUpdate\.value\) return/)
  assert.match(installationTask, /if \(!canAttach\.value\) return/)
  assert.match(installationTask, /if \(!canDownload\.value\) return/)
  assert.match(attachmentUpload, /:disabled="downloadDisabled"/)
  assert.match(attachmentUpload, /:disabled="removeDisabled"/)
})

test('attachment upload has an independent disabled state without faking upload progress', () => {
  assert.match(attachmentUpload, /disabled:\s*\{\s*type: Boolean,\s*default: false\s*\}/)
  assert.match(attachmentUpload, /:aria-disabled="disabled \|\| uploading"/)
  assert.match(attachmentUpload, /:class="\{[^}]*'is-disabled': disabled[^}]*'is-uploading': uploading[^}]*\}"/)
  assert.match(attachmentUpload, /if \(props\.disabled \|\| props\.uploading\) return/)
  assert.match(attachmentUpload, /:disabled="disabled \|\| uploading"/)
  assert.match(attachmentUpload, /:title="disabled \? disabledReason/)

  assert.match(installationTask, /:disabled="!canAttach"/)
  assert.match(installationTask, /:uploading="attachmentUploading"/)
  assert.doesNotMatch(installationTask, /:uploading="attachmentUploading \|\| !canAttach"/)
})

test('both lists commit asynchronous results only through latest request ownership', () => {
  for (const source of [installationTask, quality]) {
    assert.match(source, /createLatestRequest/)
    assert.match(source, /const request = .*\.begin\(\)/)
    assert.match(source, /request\.commit\(/)
  }
})
