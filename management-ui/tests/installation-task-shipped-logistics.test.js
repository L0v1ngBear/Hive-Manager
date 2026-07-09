import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const uiRoot = path.resolve('D:/HiveManager/management-ui')
const managementRoot = path.resolve('D:/HiveManager/management')

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

function assertContains(file, expected, message) {
  assert.ok(read(file).includes(expected), `${message}\nMissing: ${expected}\nFile: ${file}`)
}

const vueFile = path.join(uiRoot, 'src/views/function/installationTask/installationTask.vue')
assertContains(vueFile, '物流公司', 'edit dialog should expose logistics company')
assertContains(vueFile, '物流单号', 'edit dialog should expose logistics number')
assertContains(vueFile, 'editorForm.expressCompany', 'edit dialog should bind expressCompany')
assertContains(vueFile, 'editorForm.expressNo', 'edit dialog should bind expressNo')
assertContains(vueFile, "editorForm.status === 'shipped_pending_install'", 'front-end should validate shipped status')
assertContains(vueFile, '已发货待安装状态需要填写物流信息', 'front-end warning should explain missing logistics')
assertContains(vueFile, 'expressCompany: editorForm.expressCompany', 'save payload should include expressCompany')
assertContains(vueFile, 'expressNo: editorForm.expressNo', 'save payload should include expressNo')

const dtoFile = path.join(
  managementRoot,
  'src/main/java/my/management/module/installation/model/dto/InstallationTaskStatusUpdateRequest.java'
)
assertContains(dtoFile, 'private String expressCompany;', 'status update request should include expressCompany')
assertContains(dtoFile, 'private String expressNo;', 'status update request should include expressNo')

const serviceFile = path.join(
  managementRoot,
  'src/main/java/my/management/module/installation/service/InstallationTaskService.java'
)
assertContains(serviceFile, 'setExpressCompany', 'service should persist expressCompany')
assertContains(serviceFile, 'setExpressNo', 'service should persist expressNo')
assertContains(serviceFile, 'InstallationTaskStatusEnum.SHIPPED_PENDING_INSTALL', 'service should validate shipped status')
assertContains(serviceFile, '已发货待安装状态需要填写物流信息', 'service should reject shipped status without logistics')

console.log('installation task shipped logistics alignment passed')
