import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const page = read('src/views/function/organization/organization.vue')
const controller = read('../management/src/main/java/my/hive/api/organization/OrganizationController.java')
const service = read('../management/src/main/java/my/hive/domain/organization/service/OrganizationService.java')
const migration = read('../db-migrations/migrations/V20260731_001_organization_delete_only.sql')
const manifest = read('../db-migrations/migration_manifest.txt')
const checksums = read('../db-migrations/migration_checksums.sha256')

test('department and position editors use delete-only lifecycle controls', () => {
  assert.doesNotMatch(page, /<el-switch\b/)
  assert.doesNotMatch(page, /Number\(row\.status\) === 1 \? '启用' : '停用'/)
  assert.doesNotMatch(page, /props\.node\.status/)
  assert.match(page, /:can-delete="canDepartmentDelete"/)
  assert.match(page, /@delete="handleDepartmentDelete"/)
  assert.match(page, /emit\('delete', props\.node\)/)
  assert.match(page, /positionForm\.id[\s\S]*?handlePositionDelete\(positionForm\)/)
  assert.match(page, /hasPermission\('organization:department:delete'\) \|\| userStore\.hasPermission\('organization:department:manage'\)/)
  assert.match(page, /hasPermission\('organization:position:delete'\) \|\| userStore\.hasPermission\('organization:position:manage'\)/)
})

test('organization saves always reactivate records and delete accepts manage permission', () => {
  assert.equal((service.match(/setStatus\(CommonStatusEnum\.ENABLED\.getCode\(\)\)/g) || []).length, 2)
  assert.match(
    controller,
    /CODE_ORGANIZATION_DEPARTMENT_DELETE,[\s\S]*?CODE_ORGANIZATION_DEPARTMENT_MANAGE/
  )
  assert.match(
    controller,
    /CODE_ORGANIZATION_POSITION_DELETE,[\s\S]*?CODE_ORGANIZATION_POSITION_MANAGE/
  )
  assert.match(service, /departmentMapper\.deleteById\(department\.getId\(\)\)/)
  assert.match(service, /positionMapper\.deleteById\(position\.getId\(\)\)/)
  assert.doesNotMatch(service, /setIsDeleted\(DeleteFlagEnum\.DELETED\.getCode\(\)\)/)
  assert.match(service, /部门删除失败，请刷新后重试/)
  assert.match(service, /职位删除失败，请刷新后重试/)
})

test('migration reactivates historic non-deleted organization records', () => {
  assert.match(migration, /UPDATE `emp_department`[\s\S]*?SET `status` = 1[\s\S]*?WHERE `is_deleted` = 0/)
  assert.match(migration, /UPDATE `emp_position`[\s\S]*?SET `status` = 1[\s\S]*?WHERE `is_deleted` = 0/)
  assert.match(manifest, /migrations\/V20260731_001_organization_delete_only\.sql\s*$/)
  assert.match(checksums, /194ed405d8887b177b5f5dea281fb1562d41b981999a1eaacae6ac3b50315bb6\s+migrations\/V20260731_001_organization_delete_only\.sql/)
})
