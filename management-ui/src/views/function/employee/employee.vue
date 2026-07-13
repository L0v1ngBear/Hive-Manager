<template>
  <div class="function-page-shell h-full min-h-0 relative">
    <div class="function-page-container space-y-6">
      <div class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">groups</span>
            人员组织中心
          </div>
          <h2 class="function-page-title">员工名录</h2>
          <p class="function-page-desc">管理员工记录、入职及人事状态，联动组织架构和角色权限。</p>
        </div>
        <div class="flex gap-3">
          <el-button
              @click="openOrganizationDrawer"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">account_tree</span>组织架构
          </el-button>
          <el-button
              v-permission="'employee:create'"
              @click="handleCreateJoinCode"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">vpn_key</span>组织码
          </el-button>
          <el-button
              v-permission="'employee:export'"
              @click="handleTemplateDownload"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>导入模板
          </el-button>
          <el-button
              v-permission="'employee:create'"
              @click="triggerImport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>导入员工
          </el-button>
          <el-button
              v-permission="'employee:export'"
              @click="handleExport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>导出 Excel
          </el-button>
          <el-button
              v-permission="'employee:create'"
              @click="openCreateDrawer"
              class="px-5 py-2 bg-primary text-white font-bold rounded-lg flex items-center gap-2 shadow-md hover:bg-primary/90 transition-all text-sm active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">person_add</span>添加员工
          </el-button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-primary-container p-6 rounded-xl relative overflow-hidden group">
          <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-[80px]">groups</span>
          </div>
          <p class="text-on-primary-container text-sm font-bold uppercase tracking-widest">员工总数</p>
          <h3 class="text-4xl font-black text-black mt-2">{{ stats.totalEmployees }}</h3>
          <p class="text-on-primary-container text-xs mt-3">实时员工基数</p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">今日出勤</p>
          <div class="flex items-end justify-between mt-2">
            <h3 class="text-4xl font-black text-primary">{{ formatPercent(stats.todayAttendanceRate) }}</h3>
            <div class="h-10 w-20 bg-surface-container-high rounded flex items-end p-1 gap-0.5">
              <div class="w-full bg-primary/20 h-[60%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[70%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[85%] rounded-t-sm"></div>
              <div class="w-full bg-primary h-full rounded-t-sm"></div>
            </div>
          </div>
          <p class="text-on-surface-variant text-xs mt-3 uppercase font-bold tracking-tighter">同步自考勤记录</p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">部门数量</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.departmentCount }}</h3>
          <p class="text-on-surface-variant text-xs mt-3">可供分配的活跃组织架构</p>
        </div>

        <div class="bg-white/70 backdrop-blur-md p-6 rounded-xl shadow-sm border border-orange-200">
          <p class="text-orange-900 text-sm font-bold uppercase tracking-widest">待入职</p>
          <h3 class="text-4xl font-black text-orange-700 mt-2">{{ stats.pendingOnboardCount }}</h3>
          <p class="text-orange-900/70 text-xs mt-3 flex items-center gap-1 font-medium">
            <span class="material-symbols-outlined text-xs">schedule</span> 未来入职日期的记录
          </p>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col border border-surface-variant/50">
        <div class="p-4 bg-surface-container-low flex flex-wrap items-center gap-4 border-b border-surface-variant/50">
          <div class="flex-1 min-w-[300px] relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant">search</span>
            <el-input
                v-model.trim="query.keyword"
                @keyup.enter="fetchEmployees"
                class="w-full pl-10 pr-4 py-2 bg-white border-none ring-1 ring-outline-variant/30 focus:ring-2 focus:ring-primary rounded-lg text-sm transition-all"
                placeholder="按姓名、电话或工号搜索"
            />
          </div>
          <div class="flex items-center gap-3 flex-wrap">
            <el-select
                v-model="query.departmentId"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
                placeholder="所有部门"
            >
              <el-option label="所有部门" value="" />
              <el-option v-for="department in departments" :key="department.id" :label="department.name" :value="department.id" />
            </el-select>
            <el-select
                v-model="query.status"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
                placeholder="所有状态"
            >
              <el-option label="所有状态" value="" />
              <el-option v-for="status in statusOptions" :key="status.value" :label="status.label" :value="status.value" />
            </el-select>
            <el-select
                v-model="query.employeeType"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[150px] font-medium appearance-none"
                placeholder="所有用工类型"
            >
              <el-option label="所有用工类型" value="" />
              <el-option label="全职" value="FULL_TIME" />
              <el-option label="试用期" value="PROBATION" />
              <el-option label="合同工" value="CONTRACT" />
            </el-select>
            <el-date-picker
                v-model="query.entryDateStart"
                placeholder="入职开始"
                type="date"
                value-format="YYYY-MM-DD"
                class="px-3 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary"
                @change="handleFilterChange"
            />
            <el-date-picker
                v-model="query.entryDateEnd"
                placeholder="入职结束"
                type="date"
                value-format="YYYY-MM-DD"
                class="px-3 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary"
                @change="handleFilterChange"
            />
            <el-button
                @click="fetchEmployees"
                class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold hover:bg-primary/90 transition-colors"
            >
              查询
            </el-button>
            <el-button
                @click="resetFilter"
                class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold"
            >
              重置
            </el-button>
            <TableColumnSettings
                :columns="visibleEmployeeColumns"
                :exportable="false"
                @move="moveEmployeeTableColumn"
                @reset="resetEmployeeTableColumns"
            />
          </div>
        </div>

        <div class="employee-table-wrap responsive-table-wrap relative min-h-[240px]">
          <div v-if="listError" class="flex min-h-[240px] flex-col items-center justify-center gap-3 px-6 text-center">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant">
              {{ listError.type === 'permission' ? 'lock' : 'cloud_off' }}
            </span>
            <h3 class="text-base font-black text-primary">{{ listError.title }}</h3>
            <p class="max-w-lg text-sm text-on-surface-variant">{{ listError.message }}</p>
            <el-button type="primary" @click="fetchEmployees">重新加载</el-button>
          </div>
          <el-table
              v-else
              v-loading="loading"
              :data="employees"
              class="w-full"
              @row-click="handleEmployeeRowClick"
          >
            <el-table-column
                v-for="field in visibleEmployeeColumns"
                :key="field.key"
                :label="field.label"
                :min-width="employeeColumnWidths[field.key] || '104'"
            >
              <template #default="{ row: employee }">
                <span v-if="field.key === 'status'">{{ statusMeta(employee.status).label }}</span>
                <span v-else-if="field.key === 'departmentName'">{{ employee.departmentName || '--' }}</span>
                <div v-else-if="field.key === 'name'">
                  <p class="font-bold text-primary leading-none whitespace-nowrap">{{ employee.name }}</p>
                  <p
                      v-if="isEmployeeFieldVisible('employeeType')"
                      class="text-[10px] text-on-surface-variant uppercase mt-1"
                  >
                    {{ formatEmployeeType(employee.employeeType) }}
                  </p>
                </div>
                <span v-else>{{ employeeColumnText(employee, field.key) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="124" align="center" fixed="right">
              <template #default="{ row: employee }">
                <el-button :disabled="!canViewEmployeeDetail" :title="detailPermissionTitle" text type="primary" @click.stop="showEmployeeDetail(employee.id)">查看</el-button>
                <el-button :disabled="!canEditEmployee" :title="editPermissionTitle" text type="primary" @click.stop="openEditDrawer(employee.id)">编辑</el-button>
                <el-button :disabled="!canManageEmployeePermissions" :title="permissionManagementTitle" text type="primary" @click.stop="openPermissionDrawer(employee)">权限</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty v-if="!loading" description="暂无员工记录" />
            </template>
          </el-table>

        </div>

        <div class="p-4 bg-surface-container/20 flex flex-wrap items-center justify-between gap-4 text-sm text-on-surface-variant border-t border-surface-variant/50">
          <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :page-sizes="[10, 25, 50]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="changePage"
              @size-change="handlePageSizeChange"
          />
        </div>
      </div>
    </div>

    <input ref="importInputRef" type="file" accept=".xlsx" class="hidden" @change="handleImportChange" />
    <EmployeeCreate :visible="isDrawerOpen" :employee-id="editingEmployeeId" @close="closeDrawer" @success="handleCreateSuccess" />
    <EmployeePermissionDrawer ref="permissionDrawerRef" @updated="fetchEmployees" />

    <transition name="fade">
      <div
        v-if="isOrganizationDrawerOpen"
        class="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-[2px]"
        @click="closeOrganizationDrawer"
      ></div>
    </transition>
    <aside
      class="fixed top-0 right-0 z-50 h-full w-full max-w-[1180px] overflow-hidden border-l border-outline-variant/30 bg-surface shadow-2xl transition-transform duration-300"
      :class="isOrganizationDrawerOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="flex h-full flex-col">
        <div class="flex items-start justify-between border-b border-outline-variant/20 bg-white/95 px-6 py-4 backdrop-blur">
          <div>
            <h3 class="text-xl font-black text-primary">组织架构</h3>
            <p class="mt-1 text-sm text-on-surface-variant">根据员工展示上下级汇报关系，仅用于查看。</p>
          </div>
          <el-button text circle class="rounded-full p-2 text-on-surface-variant hover:bg-surface-container-high hover:text-primary" @click="closeOrganizationDrawer">
            <span class="material-symbols-outlined">close</span>
          </el-button>
        </div>
        <div class="flex-1 overflow-y-auto bg-surface p-6">
          <div class="mb-5 grid grid-cols-2 gap-4 md:grid-cols-4">
            <article class="org-stat-card">
              <span>人员总数</span>
              <strong>{{ organizationEmployees.length }}</strong>
            </article>
            <article class="org-stat-card">
              <span>顶层人员</span>
              <strong>{{ organizationTopLevelCount }}</strong>
            </article>
            <article class="org-stat-card">
              <span>有下级</span>
              <strong>{{ managerCount }}</strong>
            </article>
            <article class="org-stat-card warning">
              <span>未设置上级</span>
              <strong>{{ unassignedEmployeeCount }}</strong>
            </article>
          </div>

          <div class="org-tree-panel">
            <div v-if="organizationLoading" class="flex min-h-[360px] items-center justify-center text-primary">
              <span class="material-symbols-outlined animate-spin text-4xl">progress_activity</span>
            </div>
            <div v-else-if="organizationError" class="flex min-h-[360px] flex-col items-center justify-center gap-3 text-center">
              <span class="material-symbols-outlined text-5xl text-primary">{{ organizationError.type === 'permission' ? 'lock' : 'cloud_off' }}</span>
              <p class="text-sm font-bold text-on-surface">{{ organizationError.title }}</p>
              <p class="text-xs text-on-surface-variant">{{ organizationError.message }}</p>
              <el-button type="primary" @click="fetchOrganizationTree">重新加载</el-button>
            </div>
            <div v-else-if="employeeHierarchy.length" class="org-chart-wrap">
              <Vue3TreeOrg
                :data="orgChartData"
                :props="orgChartProps"
                :horizontal="false"
                :collapsable="true"
                :draggable="false"
                :node-draggable="false"
                :disabled="true"
                :define-menus="[]"
                :tool-bar="{ expand: true, scale: true, zoom: true, restore: true, fullscreen: false }"
                :default-expand-level="4"
                center
              >
                <template #default="{ node }">
                  <div class="org-chart-card" :class="{ root: node.$$data?.isOrganizationRoot }">
                    <div class="org-chart-icon">
                      <span class="material-symbols-outlined">{{ node.children?.length ? 'supervisor_account' : 'person' }}</span>
                    </div>
                    <div class="org-chart-content">
                      <p class="org-chart-name">{{ node.label }}</p>
                      <p class="org-chart-meta">
                        {{ node.$$data?.departmentName || '未分配部门' }} · {{ node.$$data?.positionName || '未设置职位' }}
                      </p>
                    </div>
                    <span :class="['org-chart-status', Number(node.$$data?.status) === 1 ? 'enabled' : 'disabled']">
                      {{ employeeStatusLabel(node.$$data?.status) }}
                    </span>
                  </div>
                </template>
              </Vue3TreeOrg>
            </div>
            <div v-else class="flex min-h-[360px] flex-col items-center justify-center rounded-2xl bg-surface-container-low text-center">
              <span class="material-symbols-outlined text-5xl text-primary">account_tree</span>
              <p class="mt-3 text-sm font-bold text-on-surface">暂无上下级关系</p>
              <p class="mt-1 text-xs text-on-surface-variant">还没有员工数据或直属负责人关系尚未维护。</p>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElEmpty,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { createLatestRequest } from '@/utils/latestRequest'
import { Vue3TreeOrg } from 'vue3-tree-org'
import 'vue3-tree-org/lib/vue3-tree-org.css'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import {
  defaultTenantFieldConfig,
  mergeTenantFieldConfig,
  tenantFieldLabel,
  tenantFieldVisible,
  visibleTenantFields
} from '@/utils/tenantFieldConfig'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import EmployeeCreate from './employeeCreate.vue'
import EmployeePermissionDrawer from './EmployeePermissionDrawer.vue'
import { buildEmployeeHierarchy, buildOrganizationChart as buildEmployeeOrganizationChart } from './employeeOrganization.js'
import {
  downloadEmployeeImportTemplate,
  createOrganizationJoinCode,
  exportEmployeesExcel,
  getEmployeeDetail,
  getEmployeeFormOptions,
  getEmployeePage,
  getEmployeeStats,
  importEmployees
} from './api/employee.js'

defineOptions({ name: 'EmployeeManagement' })

const route = useRoute()
const userStore = useUserStore()
const employeeListRequest = createLatestRequest()
const organizationRequest = createLatestRequest()
// --- 状态定义 ---
const isDrawerOpen = ref(false)
const editingEmployeeId = ref(null)
const permissionDrawerRef = ref(null)
const importInputRef = ref(null)
const loading = ref(false)
const listError = ref(null)
const isOrganizationDrawerOpen = ref(false)
const organizationLoading = ref(false)
const organizationError = ref(null)
const organizationEmployees = ref([])
const employees = ref([])
const departments = ref([])
const statusOptions = ref([])
const employeeFieldConfig = ref(defaultTenantFieldConfig('employee'))
const stats = reactive({
  totalEmployees: 0,
  todayAttendanceRate: 0,
  departmentCount: 0,
  pendingOnboardCount: 0
})
const pagination = reactive({
  total: 0,
  pages: 0
})
const canViewEmployeeDetail = computed(() => userStore.hasPermission('employee:detail'))
const canEditEmployee = computed(() => userStore.hasPermission('employee:update') && canViewEmployeeDetail.value)
const canManageEmployeePermissions = computed(() => userStore.hasPermission('employee:update') && userStore.hasPermission('role:permission:list'))
const detailPermissionTitle = computed(() => canViewEmployeeDetail.value ? '查看员工详情' : '需要员工详情权限')
const editPermissionTitle = computed(() => canEditEmployee.value ? '编辑员工' : '需要员工编辑和详情权限')
const permissionManagementTitle = computed(() => canManageEmployeePermissions.value ? '配置员工单独权限' : '需要员工编辑和角色权限查看权限')
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  departmentId: '',
  status: '',
  employeeType: '',
  entryDateStart: '',
  entryDateEnd: ''
})

// --- 计算属性 ---
const totalPages = computed(() => Math.max(pagination.pages || 1, 1))
const employeeColumnRenderers = new Set(['name', 'empNo', 'departmentName', 'positionName', 'phone', 'email', 'leaderName', 'entryDate', 'employeeType', 'attendanceRequired', 'attendanceLocationNames', 'status'])
const defaultEmployeeColumns = computed(() => visibleTenantFields(employeeFieldConfig.value, 'name').filter((field) => employeeColumnRenderers.has(field.key)))
const {
  orderedColumns: visibleEmployeeColumns,
  moveColumn: moveEmployeeTableColumn,
  resetColumns: resetEmployeeTableColumns
} = useLocalTableColumns('employee.list', defaultEmployeeColumns)
const employeeColumnWidths = {
  name: '140px',
  empNo: '92px',
  departmentName: '112px',
  positionName: '112px',
  phone: '128px',
  email: '150px',
  leaderName: '112px',
  entryDate: '106px',
  employeeType: '96px',
  attendanceRequired: '96px',
  attendanceLocationNames: '140px',
  status: '82px'
}

// 核心逻辑：构建层级结构
const employeeHierarchy = computed(() => buildEmployeeHierarchy(organizationEmployees.value))

const organizationChart = computed(() => buildEmployeeOrganizationChart(employeeHierarchy.value))
const orgChartData = computed(() => organizationChart.value.data)
const organizationTopLevelCount = computed(() => organizationChart.value.topLevelCount)
const unassignedEmployeeCount = computed(() => organizationChart.value.unassignedCount)

const managerCount = computed(() => organizationEmployees.value.filter((item) => employeeHierarchyHasChildren(item.id, employeeHierarchy.value)).length)
const orgChartProps = { id: 'id', pid: 'pid', label: 'label', children: 'children', expand: 'expand' }

// --- 方法定义 ---

const fetchEmployeeFieldConfig = async () => {
  try {
    const rows = await getCurrentTenantFieldConfig('employee')
    employeeFieldConfig.value = mergeTenantFieldConfig('employee', rows)
  } catch (error) {
    employeeFieldConfig.value = defaultTenantFieldConfig('employee')
  }
}

const resolveListError = (error) => {
  const status = Number(error?.response?.status ?? error?.status ?? error?.code ?? 0)
  if (status === 401 || status === 403) {
    return {
      type: 'permission',
      title: status === 401 ? '登录状态已失效' : '暂无员工列表权限',
      message: status === 401 ? '请重新登录后查看员工列表。' : '请联系管理员分配员工列表权限后重试。'
    }
  }
  return {
    type: 'request',
    title: status >= 500 ? '员工服务暂时不可用' : '网络连接异常',
    message: status >= 500 ? '服务处理失败，请稍后重新加载。' : '请检查网络连接后重新加载员工列表。'
  }
}

const fetchEmployees = async () => {
  const request = employeeListRequest.begin()
  loading.value = true
  listError.value = null
  employees.value = []
  pagination.total = 0
  pagination.pages = 0
  try {
    const data = await getEmployeePage(normalizeQuery())
    request.commit(() => {
      employees.value = data.data || []
      pagination.total = Number(data.total || 0)
      pagination.pages = Number(data.pages || 0)
    })
  } catch (error) {
    request.commit(() => {
      employees.value = []
      pagination.total = 0
      pagination.pages = 0
      listError.value = resolveListError(error)
    })
  } finally {
    request.commit(() => { loading.value = false })
  }
}

const fetchStats = async () => {
  const data = await getEmployeeStats()
  Object.assign(stats, data)
}

const fetchFormOptions = async () => {
  const data = await getEmployeeFormOptions()
  departments.value = data.departments || []
  statusOptions.value = (data.employmentStatuses || []).map((item) => ({
    label: item.label,
    value: Number(item.value)
  }))
}

const handleFilterChange = () => {
  query.page = 1
  fetchEmployees()
}

const resetFilter = () => {
  query.keyword = ''
  query.departmentId = ''
  query.status = ''
  query.employeeType = ''
  query.entryDateStart = ''
  query.entryDateEnd = ''
  query.page = 1
  fetchEmployees()
}

const handlePageSizeChange = () => {
  query.page = 1
  fetchEmployees()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  query.page = page
  fetchEmployees()
}

const handleCreateSuccess = async () => {
  closeDrawer()
  await Promise.all([fetchEmployees(), fetchStats()])
}

const isEmployeeFieldVisible = (key) => tenantFieldVisible(employeeFieldConfig.value, key)

const employeeColumnText = (emp, key) => {
  if (!emp) return '--'
  if (key === 'empNo') return emp.empNo || '--'
  if (key === 'positionName') return emp.positionName || '--'
  if (key === 'phone') return emp.phone || '--'
  if (key === 'email') return emp.email || '--'
  if (key === 'leaderName') return emp.leaderName || '--'
  if (key === 'entryDate') return emp.entryDate || '--'
  if (key === 'attendanceRequired') return Number(emp.attendanceRequired ?? 1) === 1 ? '需要打卡' : '免打卡'
  if (key === 'attendanceLocationNames') return Array.isArray(emp.attendanceLocationNames) && emp.attendanceLocationNames.length ? emp.attendanceLocationNames.join('、') : '全部地点'
  if (key === 'employeeType') return formatEmployeeType(emp.employeeType)
  if (key === 'remark') return emp.remark || '--'
  return emp[key] || '--'
}

const employeeDetailLines = (detail) => visibleEmployeeColumns.value
    .filter((field) => field.key !== 'name')
    .map((field) => `${field.label}: ${field.key === 'status' ? statusMeta(detail.status).label : employeeColumnText(detail, field.key)}`)

const showEmployeeDetail = async (id) => {
  if (!canViewEmployeeDetail.value) return
  const detail = await getEmployeeDetail(id)
  const detailLines = employeeDetailLines(detail)
  ElMessageBox.alert(
      detailLines.length ? detailLines.join('\n') : '暂无可展示信息',
      detail.name || tenantFieldLabel(employeeFieldConfig.value, 'name', '员工详情'),
      { confirmButtonText: '关闭' }
  )
}

const handleEmployeeRowClick = (employee) => {
  if (canViewEmployeeDetail.value) showEmployeeDetail(employee.id)
}

const openCreateDrawer = () => {
  editingEmployeeId.value = null
  isDrawerOpen.value = true
}

const openEditDrawer = (id) => {
  if (!canEditEmployee.value) return
  editingEmployeeId.value = id
  isDrawerOpen.value = true
}

const openPermissionDrawer = (employee) => {
  if (!canManageEmployeePermissions.value) return
  permissionDrawerRef.value?.open(employee)
}

const closeDrawer = () => {
  isDrawerOpen.value = false
  editingEmployeeId.value = null
}

const openOrganizationDrawer = async () => {
  isOrganizationDrawerOpen.value = true
  await fetchOrganizationTree()
}

const closeOrganizationDrawer = async () => {
  isOrganizationDrawerOpen.value = false
}

const fetchOrganizationTree = async () => {
  const request = organizationRequest.begin()
  organizationLoading.value = true
  organizationError.value = null
  organizationEmployees.value = []
  try {
    // 获取全部员工用于构建树
    const data = await getEmployeePage({ page: 1, size: 2000 })
    request.commit(() => { organizationEmployees.value = data.data || [] })
  } catch (error) {
    request.commit(() => { organizationError.value = resolveListError(error) })
  } finally {
    request.commit(() => { organizationLoading.value = false })
  }
}

const employeeHierarchyHasChildren = (employeeId, nodes) => {
  for (const node of nodes) {
    if (Number(node.id) === Number(employeeId)) {
      return (node.children || []).length > 0
    }
    if (employeeHierarchyHasChildren(employeeId, node.children || [])) {
      return true
    }
  }
  return false
}

const handleExport = async () => {
  const blob = await exportEmployeesExcel(normalizeQuery())
  downloadBlob(blob, `员工列表-${Date.now()}.xlsx`)
}

const handleCreateJoinCode = async () => {
  const data = await createOrganizationJoinCode()
  const code = data?.organizationCode || ''
  const expiresInMinutes = Math.ceil(Number(data?.expiresInSeconds || 900) / 60)
  await ElMessageBox.alert(
    `组织码：${code}\n有效期：${expiresInMinutes} 分钟\n\n员工在手机端或电脑端登录页加入组织时，需要填写姓名和该组织码。`,
    '组织加入码',
    { confirmButtonText: '知道了' }
  )
}

const handleTemplateDownload = async () => {
  const blob = await downloadEmployeeImportTemplate()
  downloadBlob(blob, '员工导入模板.xlsx')
}

const triggerImport = () => {
  importInputRef.value?.click()
}

const handleImportChange = async (event) => {
  const [file] = event.target.files || []
  if (!file) return
  try {
    const result = await importEmployees(file)
    const failText = (result.failMessages || []).slice(0, 5).join('\n')
    await ElMessageBox.alert(
        `导入结果：成功 ${result.successCount} 条，失败 ${result.failCount} 条。${failText ? `\n\n部分失败原因：\n${failText}` : ''}`,
        '导入结果'
    )
    await Promise.all([fetchEmployees(), fetchStats()])
  } finally {
    event.target.value = ''
  }
}

const downloadBlob = (blob, fileName) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

const normalizeQuery = () => ({
  page: query.page,
  size: query.size,
  keyword: query.keyword || undefined,
  departmentId: query.departmentId === '' ? undefined : Number(query.departmentId),
  status: query.status === '' ? undefined : Number(query.status),
  employeeType: query.employeeType || undefined,
  entryDateStart: query.entryDateStart || undefined,
  entryDateEnd: query.entryDateEnd || undefined
})

const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatEmployeeType = (value) => ({ FULL_TIME: '全职', CONTRACT: '合同工', PROBATION: '试用期' }[value] || value || '--')

const statusMeta = (status) => {
  const s = Number(status)
  if (s === 1) return { label: '在职', text: 'text-emerald-600', dot: 'bg-emerald-600' }
  if (s === 2) return { label: '试用', text: 'text-amber-600', dot: 'bg-amber-500' }
  if (s === 0) return { label: '离职', text: 'text-slate-500', dot: 'bg-slate-400' }
  return { label: '未知', text: 'text-slate-500', dot: 'bg-slate-400' }
}

const employeeStatusLabel = (status) => statusMeta(status).label

function applyRouteKeyword() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== query.keyword) {
    query.keyword = routeKeyword
    query.page = 1
  }
}

onMounted(async () => {
  applyRouteKeyword()
  await Promise.all([fetchEmployeeFieldConfig(), fetchEmployees(), fetchStats(), fetchFormOptions()])
})

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await fetchEmployees()
  }
)
</script>

<style scoped>
.org-stat-card {
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 1rem;
  background: rgba(255, 255, 255, .86);
  padding: 1rem;
  box-shadow: 0 8px 24px rgba(15, 23, 42, .05);
}

.employee-table {
  min-width: 0;
}

.employee-table-wrap {
  width: 100%;
}

.employee-cell-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-stat-card span {
  display: block;
  color: rgb(var(--on-surface-variant));
  font-size: .72rem;
  font-weight: 900;
  letter-spacing: .14em;
  text-transform: uppercase;
}

.org-stat-card strong {
  display: block;
  margin-top: .55rem;
  color: rgb(var(--primary));
  font-size: 2rem;
  line-height: 2.1rem;
  font-weight: 900;
}

.org-stat-card.warning {
  border-color: rgba(31, 63, 95, .20);
  background: rgba(238, 244, 251, .9);
}

.org-tree-panel {
  min-height: 440px;
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 1.25rem;
  background:
    radial-gradient(circle at 12% 12%, rgba(0, 82, 204, .08), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, .96), rgba(248, 250, 252, .96));
  padding: 1.25rem;
}

.org-chart-wrap {
  min-height: 560px;
  overflow: hidden;
}

.org-chart-card {
  display: flex;
  align-items: center;
  gap: .75rem;
  min-width: 230px;
  max-width: 270px;
  border: 1px solid rgba(69, 95, 136, .18);
  border-radius: 1rem;
  background: linear-gradient(180deg, #fff, #f8fafc);
  padding: .85rem;
  box-shadow: 0 10px 28px rgba(15, 23, 42, .08);
}

.org-chart-card.root {
  border-color: rgba(69, 95, 136, .35);
  background: linear-gradient(135deg, rgb(var(--primary)), #6c86b3);
  color: white;
}

.org-chart-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 2.55rem;
  height: 2.55rem;
  border-radius: .85rem;
  background: rgba(69, 95, 136, .1);
  color: rgb(var(--primary));
}

.org-chart-card.root .org-chart-icon {
  background: rgba(255, 255, 255, .16);
  color: white;
}

.org-chart-icon .material-symbols-outlined {
  font-size: 1.55rem;
}

.org-chart-content {
  min-width: 0;
  flex: 1;
}

.org-chart-name {
  overflow: hidden;
  font-size: .95rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-chart-meta {
  margin-top: .25rem;
  overflow: hidden;
  color: rgb(var(--on-surface-variant));
  font-size: .72rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-chart-card.root .org-chart-meta {
  color: rgba(255, 255, 255, .78);
}

.org-chart-status {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: .22rem .5rem;
  font-size: .66rem;
  font-weight: 900;
}

.org-chart-status.enabled {
  background: rgba(16, 185, 129, .12);
  color: rgb(4, 120, 87);
}

.org-chart-status.disabled {
  background: rgba(100, 116, 139, .12);
  color: rgb(71, 85, 105);
}

.org-chart-card.root .org-chart-status {
  background: rgba(255, 255, 255, .16);
  color: white;
}

:deep(.zm-tree-org) {
  width: 100%;
  height: 560px;
  background: transparent;
}

:deep(.zoom-container) {
  background: transparent;
}

:deep(.tree-org-node__inner) {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

:deep(.tree-org-node__content) {
  padding: 0 10px;
}

:deep(.tree-org-node__children::before),
:deep(.tree-org-node::before),
:deep(.tree-org-node::after) {
  border-color: rgba(69, 95, 136, .35) !important;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
