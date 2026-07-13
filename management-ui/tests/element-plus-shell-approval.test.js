import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

function read(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertElementComponents(source, components, label) {
  for (const component of components) {
    const tag = component.replace(/^El/, '').replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`).slice(1)
    assert.match(source, new RegExp(`<el-${tag}(?:\\s|>)`), `${label} must render ${component}`)
    assert.match(
      source,
      new RegExp(`import\\s*\\{[^}]*\\b${component}\\b[^}]*\\}\\s*from\\s*['\"]element-plus['\"]`, 's'),
      `${label} must explicitly import ${component}`
    )
  }
}

const approval = read('src/views/function/approval/approvalCenter.vue')
const dashboard = read('src/views/dashboard/index.vue')
const navbar = read('src/layout/components/Navbar.vue')
const sidebar = read('src/layout/components/Sidebar.vue')
const login = read('src/views/Login.vue')
const joinOrganization = read('src/views/JoinOrganization.vue')
const forcePasswordChange = read('src/views/ForcePasswordChange.vue')
const noPermission = read('src/views/NoPermission.vue')

test('approval center uses explicit Element Plus workflow controls', () => {
  assertElementComponents(
    approval,
    [
      'ElTabs',
      'ElTabPane',
      'ElBadge',
      'ElTable',
      'ElTableColumn',
      'ElPagination',
      'ElForm',
      'ElFormItem',
      'ElDescriptions',
      'ElDescriptionsItem',
      'ElInput',
      'ElSelect',
      'ElOption',
      'ElButton',
      'ElDatePicker',
      'ElInputNumber',
      'ElTag',
      'ElEmpty'
    ],
    'approval center'
  )
})

test('approval center retains all five approval dispatch families and permission guards', () => {
  for (const type of ['order', 'quality', 'finance', 'leave', 'resignation']) {
    assert.match(approval, new RegExp(`value:\\s*['\"]${type}['\"]`), `approval type ${type} must remain configured`)
  }
  for (const api of [
    'listOrderApprovals',
    'listQualityApprovals',
    'listFinanceApprovals',
    'listLeaveApprovals',
    'listResignationApprovals',
    'auditOrderApproval',
    'auditQualityApproval',
    'auditFinanceApproval',
    'auditLeaveApproval',
    'auditResignationApproval'
  ]) {
    assert.match(approval, new RegExp(`\\b${api}\\b`), `approval API ${api} must remain wired`)
  }
  assert.match(approval, /accessibleTabs/)
  assert.match(approval, /activeTabCanViewList/)
  assert.match(approval, /ElMessage\.warning\(['\"]当前账号暂无权限/)
})

test('approval list clears stale content and exposes exclusive retryable load states', () => {
  assert.match(approval, /const listLoadError = ref\(null\)/)
  assert.match(approval, /const listLoaded = ref\(false\)/)
  assert.match(approval, /const requestId = \+\+listRequestId/)
  assert.match(approval, /rows\.value = \[\][\s\S]*listLoadError\.value = null[\s\S]*listLoaded\.value = false/)
  assert.match(approval, /if \(requestId !== listRequestId\)/)
  assert.match(approval, /listLoadError\.value = resolveLoadFailure\(error, '审批列表'\)/)
  assert.match(approval, /v-else-if="!activeTabCanViewList"/)
  assert.match(approval, /v-else-if="listLoadError"/)
  assert.match(approval, /v-else-if="listLoaded"/)
  assert.match(approval, /@click="fetchList"/)
  for (const kind of ['authentication', 'permission', 'network', 'server']) {
    assert.match(approval, new RegExp(`kind:\\s*['"]${kind}['"]`), `approval must distinguish ${kind} failures`)
  }
})

test('dashboard announcements distinguish failures from successful empty responses', () => {
  assert.match(dashboard, /Promise\.allSettled\(/)
  assert.match(dashboard, /const announcementLoadError = ref\(null\)/)
  assert.match(dashboard, /const importantAnnouncementLoadError = ref\(null\)/)
  assert.match(dashboard, /const announcementsLoaded = ref\(false\)/)
  assert.match(dashboard, /const importantAnnouncementsLoaded = ref\(false\)/)
  assert.match(dashboard, /announcementLoadError\.value = resolveLoadFailure\(/)
  assert.match(dashboard, /importantAnnouncementLoadError\.value = resolveLoadFailure\(/)
  assert.match(dashboard, /v-else-if="announcementLoadError"/)
  assert.match(dashboard, /v-else-if="importantAnnouncementLoadError"/)
  assert.match(dashboard, /announcementsLoaded && !announcements\.length/)
  assert.match(dashboard, /importantAnnouncementsLoaded && !importantAnnouncements\.length/)
  assert.match(dashboard, /@click="fetchAnnouncements"/)
  for (const kind of ['authentication', 'permission', 'network', 'server']) {
    assert.match(dashboard, new RegExp(`kind:\\s*['"]${kind}['"]`), `dashboard must distinguish ${kind} failures`)
  }
})

test('dashboard and navigation use explicit Element Plus shell controls', () => {
  assertElementComponents(dashboard, ['ElButton', 'ElEmpty'], 'dashboard')
  assertElementComponents(navbar, ['ElPopover', 'ElDropdown', 'ElDropdownMenu', 'ElDropdownItem', 'ElBadge', 'ElButton'], 'navbar')
  assertElementComponents(sidebar, ['ElBadge', 'ElButton'], 'sidebar')

  assert.match(navbar, /ElMessage\.warning\(/)
  assert.match(navbar, /handleSearchItemClick/)
  assert.match(sidebar, /decorateAccessItems/)
  assert.match(sidebar, /handleMenuNavigate/)
})

test('authentication views use explicitly imported Element Plus form controls', () => {
  assertElementComponents(login, ['ElForm', 'ElFormItem', 'ElInput', 'ElCheckbox', 'ElButton', 'ElDialog'], 'login')
  assertElementComponents(joinOrganization, ['ElForm', 'ElFormItem', 'ElInput', 'ElButton'], 'join organization')
  assertElementComponents(forcePasswordChange, ['ElForm', 'ElFormItem', 'ElInput', 'ElButton'], 'force password change')
  assertElementComponents(noPermission, ['ElButton'], 'no permission')
})

test('login keeps password, memory, scan, and redirect flows unchanged', () => {
  for (const contract of [
    'login({',
    'password: loginForm.password',
    'saveLoginMemory({',
    'createScanLoginSession()',
    'getScanLoginStatus(',
    'normalizeLoginRedirect(',
    "router.push('/join-organization')",
    'resetPassword({'
  ]) {
    assert.ok(login.includes(contract), `login contract must retain ${contract}`)
  }
  assert.match(joinOrganization, /joinOrganization\(\{/)
  assert.match(forcePasswordChange, /changeInitialPassword\(\{/)
})

test('approval detail commands enforce real detail permissions and latest request state', () => {
  for (const permission of ['approval:leave:detail', 'approval:finance:detail', 'approval:resignation:detail', 'order:detail']) {
    assert.match(approval, new RegExp(permission.replace(':', '\\:')))
  }
  assert.match(approval, /:disabled="!canViewDetail\(item\)"/)
  assert.match(approval, /if \(!canViewDetail\(item\)\) return/)
  assert.match(approval, /const requestId = \+\+detailRequestId/)
  assert.match(approval, /detailData\.value = null[\s\S]*detailLoadError\.value = null[\s\S]*detailLoading\.value = true/)
  assert.match(approval, /if \(requestId !== detailRequestId\) return/)
  assert.match(approval, /detailLoadError\.value = resolveLoadFailure\(error, '审批详情'\)/)
  assert.match(approval, /requireUiPermission\('approval:finance:detail'\)/)
})

test('dashboard overview clears stale state and commits only the latest retryable result', () => {
  assert.match(dashboard, /const overviewLoadError = ref\(null\)/)
  assert.match(dashboard, /const requestId = \+\+overviewRequestId/)
  assert.match(dashboard, /resetOverviewState\(\)[\s\S]*overviewLoadError\.value = null[\s\S]*getDashboardOverview/)
  assert.match(dashboard, /if \(requestId !== overviewRequestId\) return/)
  assert.match(dashboard, /overviewLoadError\.value = resolveOverviewFailure\(error\)/)
  assert.match(dashboard, /v-if="overviewLoadError"[\s\S]*@click="fetchOverview"/)
})
