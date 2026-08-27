import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readRepo = (path) => readFileSync(new URL(`../../${path}`, import.meta.url), 'utf8')
const controller = readRepo('management/src/main/java/my/hive/api/aftersales/AfterSalesController.java')
const service = readRepo('management/src/main/java/my/hive/domain/aftersales/service/AfterSalesService.java')
const roles = readRepo('management/src/main/java/my/hive/domain/permission/service/BuiltInRoleCatalog.java')
const page = readRepo('management-ui/src/views/function/afterSales/afterSales.vue')
const migrationPath = 'db-migrations/migrations/V20260826_001_after_sales_sales_part_read.sql'
const migration = readRepo(migrationPath)
const assigneeMigrationPath = 'db-migrations/migrations/V20260826_003_after_sales_ticket_assignee.sql'
const assigneeMigration = readRepo(assigneeMigrationPath)
const repairFieldsMigrationPath = 'db-migrations/migrations/V20260826_006_after_sales_repair_amount_and_motor_return_quantity.sql'
const repairFieldsMigration = readRepo(repairFieldsMigrationPath)
const manifest = readRepo('db-migrations/migration_manifest.txt')
const checksums = readRepo('db-migrations/migration_checksums.sha256')
const customerProjectLabelMigrationPath = 'db-migrations/migrations/V20260826_007_customer_project_label_rename.sql'
const customerProjectLabelMigration = readRepo(customerProjectLabelMigrationPath)
const ticketPartLocationMigrationPath = 'db-migrations/migrations/V20260826_008_after_sales_ticket_part_location.sql'
const ticketPartLocationMigration = readRepo(ticketPartLocationMigrationPath)
const followUpMigrationPath = 'db-migrations/migrations/V20260826_009_after_sales_ticket_follow_up.sql'
const followUpMigration = readRepo(followUpMigrationPath)
const customerPage = readRepo('management-ui/src/views/function/customer/customer.vue')
const customerCreate = readRepo('management-ui/src/views/function/customer/customerCreate.vue')

test('after-sales saves distinguish create and update permissions', () => {
  assert.match(controller, /requireSavePermission\(request\.getId\(\) == null/)
  assert.match(controller, /requireSavePartPermission\(request\.getId\(\) == null/)
  assert.match(controller, /CODE_AFTER_SALES_CREATE : PermissionCatalogV3\.CODE_AFTER_SALES_UPDATE/)
  assert.match(controller, /CODE_AFTER_SALES_PART_CREATE : PermissionCatalogV3\.CODE_AFTER_SALES_PART_UPDATE/)
})

test('after-sales state transitions and parts are guarded', () => {
  assert.match(service, /requireTicketStatus\(ticket, Set\.of\(STATUS_DRAFT\), "只有草稿工单可以开始处理"\)/)
  assert.match(service, /requireTicketStatus\(ticket, Set\.of\(STATUS_PROCESSING\), "只有处理中的工单可以结案"\)/)
  assert.match(service, /requireTicketStatus\(ticket, Set\.of\(STATUS_WAITING_OUTBOUND\), "只有待配件出库的工单可以出库"\)/)
  assert.match(service, /if \(item == null\) throw new BusinessException\("配件明细不能为空"\)/)
  assert.match(page, /await loadParts\(\); ticketEditorVisible\.value = true/)
})

test('sales roles can read parts without receiving warehouse permissions', () => {
  assert.match(roles, /"after_sales:create", "after_sales:update", "after_sales:part:list"/)
  assert.match(migration, /role_item\.role_code IN \('SALES_STAFF', 'SALES_MANAGER'\)/)
  assert.doesNotMatch(migration, /after_sales:part:outbound/)
  assert.match(manifest, /migrations\/V20260826_001_after_sales_sales_part_read\.sql/)
  const hash = createHash('sha256').update(migration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_001_after_sales_sales_part_read\\.sql`))
})

test('after-sales tickets can be assigned and notify the assigned employee', () => {
  assert.match(controller, /@GetMapping\("\/assignee-options"\)/)
  assert.match(controller, /@PostMapping\("\/tickets\/\{id\}\/assignee"\)/)
  assert.match(controller, /CODE_AFTER_SALES_PROCESS, message = "当前账号没有指派售后工单权限"/)
  assert.match(service, /public AfterSalesTicket assignTicket\(Long ticketId, AfterSalesTicketAssignRequest request\)/)
  assert.match(service, /WechatSubscribeService/)
  assert.match(service, /sendTodoAfterCommit\(assignee\.getId\(\), "售后工单已指派"/)
  assert.match(page, /指派人员/)
  assert.match(page, /assignAfterSalesTicket/)
  assert.match(assigneeMigration, /assignee_user_id/)
  assert.match(manifest, /migrations\/V20260826_003_after_sales_ticket_assignee\.sql/)
  const hash = createHash('sha256').update(assigneeMigration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_003_after_sales_ticket_assignee\\.sql`))
})

test('after-sales records repair cost and returned motor quantity without changing existing motor recovery data', () => {
  assert.match(service, /ticket\.setRepairAmount\(normalizeNonNegativeAmount\(request\.getRepairAmount\(\)/)
  assert.match(service, /ticket\.setReturnOldMotorQuantity\(normalizeNonNegativeInteger\(request\.getReturnOldMotorQuantity\(\)/)
  assert.match(page, /维修金额（元）/)
  assert.match(page, /旧电机退还数量/)
  assert.match(repairFieldsMigration, /repair_amount DECIMAL\(12,2\)/)
  assert.match(repairFieldsMigration, /return_old_motor_quantity INT NOT NULL DEFAULT 0/)
  assert.match(manifest, /migrations\/V20260826_006_after_sales_repair_amount_and_motor_return_quantity\.sql/)
  const hash = createHash('sha256').update(repairFieldsMigration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_006_after_sales_repair_amount_and_motor_return_quantity\\.sql`))
})

test('customer projects use the project-name wording and after-sales pre-fills linked customer and project information', () => {
  assert.match(customerPage, /客户基础信息、联系人和项目名称/)
  assert.match(customerCreate, /fieldLabel\('projectName', '项目名称'\)/)
  assert.match(page, /label="客户名称"/)
  assert.match(page, /ticketForm\.actualAddress = selectedOrder\.value\.projectName \? `（\$\{selectedOrder\.value\.projectName\}）` : ''/)
  assert.match(page, /ticketForm\.contactName = selectedOrder\.value\.contactName \|\| ''/)
  assert.match(service, /enrichOrderContacts\(tenantCode, orders\)/)
  assert.match(customerProjectLabelMigration, /field_label = '项目名称'/)
  assert.match(customerProjectLabelMigration, /field_label = '合作项目'/)
  assert.match(manifest, /migrations\/V20260826_007_customer_project_label_rename\.sql/)
  const hash = createHash('sha256').update(customerProjectLabelMigration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_007_customer_project_label_rename\\.sql`))
})

test('after-sales ticket parts retain the selected stock location', () => {
  assert.match(service, /PART_LOCATIONS = Set\.of\("三车间", "二车间", "其他"\)/)
  assert.match(service, /line\.setPartLocation\(normalizePartLocation\(item\.getPartLocation\(\)\)\)/)
  assert.match(page, /配件地点/)
  assert.match(page, /const partLocationOptions = \['三车间', '二车间', '其他'\]/)
  assert.match(ticketPartLocationMigration, /part_location VARCHAR\(20\)/)
  assert.match(manifest, /migrations\/V20260826_008_after_sales_ticket_part_location\.sql/)
  const hash = createHash('sha256').update(ticketPartLocationMigration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_008_after_sales_ticket_part_location\\.sql`))
})

test('closed after-sales tickets only expose follow-up and retain the visit record', () => {
  assert.match(controller, /@PostMapping\("\/tickets\/\{id\}\/follow-up"\)/)
  assert.match(service, /public AfterSalesTicket followUpTicket\(Long ticketId, AfterSalesTicketFollowUpRequest request\)/)
  assert.match(service, /requireTicketStatus\(ticket, Set\.of\(STATUS_CLOSED\), "只有已结案工单可以回访"\)/)
  assert.match(page, /v-if="row\.status === 'closed'"[^]*?回访/)
  assert.match(page, /v-else><el-button link type="primary" @click\.stop="openDetail\(row\)">详情/)
  assert.match(page, /客户满意度/)
  assert.match(page, /回访内容/)
  assert.match(followUpMigration, /follow_up_time DATETIME NULL/)
  assert.match(followUpMigration, /follow_up_content TEXT NULL/)
  assert.match(manifest, /migrations\/V20260826_009_after_sales_ticket_follow_up\.sql/)
  const hash = createHash('sha256').update(followUpMigration).digest('hex')
  assert.match(checksums, new RegExp(`${hash}  migrations/V20260826_009_after_sales_ticket_follow_up\\.sql`))
})

test('after-sales warranty prompts calculate one-year and six-year coverage from opening date', () => {
  assert.match(page, /function warrantyStates\(openingDate\)/)
  assert.match(page, /return \[1, 6\]\.map\(years =>/)
  assert.match(page, /已过保/)
  assert.match(page, /今日到期/)
  assert.match(page, /质保中（剩余\$\{days\}天）/)
  assert.match(page, /label="质保提示"/)
})
