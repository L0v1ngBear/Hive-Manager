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
