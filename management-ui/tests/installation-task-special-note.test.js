import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const uiRoot = path.resolve('D:/HiveManager/management-ui')
const managementRoot = path.resolve('D:/HiveManager/management')
const deployRoot = path.resolve('C:/Users/HUAWEI/Desktop/hive部署_全新配置')

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

function assertContains(file, expected, message) {
  assert.ok(read(file).includes(expected), `${message}\nMissing: ${expected}\nFile: ${file}`)
}

const vueFile = path.join(uiRoot, 'src/views/function/installationTask/installationTask.vue')
assertContains(vueFile, '特殊及异常情况说明', 'edit dialog should expose the special/exception note field')
assertContains(vueFile, 'editorForm.specialExceptionNote', 'edit dialog should bind specialExceptionNote')
assertContains(vueFile, 'specialExceptionNote: editorForm.specialExceptionNote', 'save payload should include specialExceptionNote')

const javaFiles = [
  'src/main/java/my/management/module/installation/model/entity/InstallationTask.java',
  'src/main/java/my/management/module/installation/model/dto/InstallationTaskStatusUpdateRequest.java',
  'src/main/java/my/management/module/installation/model/vo/InstallationTaskVO.java'
].map((file) => path.join(managementRoot, file))

for (const file of javaFiles) {
  assertContains(file, 'private String specialExceptionNote;', 'backend model should carry specialExceptionNote')
}

const serviceFile = path.join(
  managementRoot,
  'src/main/java/my/management/module/installation/service/InstallationTaskService.java'
)
assertContains(serviceFile, 'setSpecialExceptionNote', 'service should persist and return specialExceptionNote')

const migrationFile = path.join(
  deployRoot,
  'db-migrations/migrations/V20260706_001_installation_task_special_exception_note.sql'
)
assert.ok(fs.existsSync(migrationFile), `migration should exist: ${migrationFile}`)
assertContains(migrationFile, 'special_exception_note', 'migration should add special_exception_note')

const verifySchemaFile = path.join(deployRoot, 'db-migrations/scripts/verify-online-schema.sh')
assertContains(verifySchemaFile, "SELECT 'installation_task', 'special_exception_note'", 'online schema check should verify special_exception_note')

console.log('installation task special note alignment passed')
