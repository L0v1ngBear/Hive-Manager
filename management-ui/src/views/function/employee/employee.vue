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
          <button
              @click="openOrganizationDrawer"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">account_tree</span>组织架构
          </button>
          <button
              v-permission="'employee:create'"
              @click="handleCreateJoinCode"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">vpn_key</span>组织码
          </button>
          <button
              v-permission="'employee:export'"
              @click="handleTemplateDownload"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>导入模板
          </button>
          <button
              v-permission="'employee:create'"
              @click="triggerImport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>导入员工
          </button>
          <button
              v-permission="'employee:export'"
              @click="handleExport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>导出 Excel
          </button>
          <button
              v-permission="'employee:create'"
              @click="openCreateDrawer"
              class="px-5 py-2 bg-primary text-white font-bold rounded-lg flex items-center gap-2 shadow-md hover:bg-primary/90 transition-all text-sm active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">person_add</span>添加员工
          </button>
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
            <input
                v-model.trim="query.keyword"
                @keyup.enter="fetchEmployees"
                type="text"
                class="w-full pl-10 pr-4 py-2 bg-white border-none ring-1 ring-outline-variant/30 focus:ring-2 focus:ring-primary rounded-lg text-sm transition-all"
                placeholder="按姓名、电话或工号搜索"
            />
          </div>
          <div class="flex items-center gap-3 flex-wrap">
            <select
                v-model="query.departmentId"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
            >
              <option value="">所有部门</option>
              <option v-for="department in departments" :key="department.id" :value="department.id">
                {{ department.name }}
              </option>
            </select>
            <select
                v-model="query.status"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
            >
              <option value="">所有状态</option>
              <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
            <select
                v-model="query.employeeType"
                @change="handleFilterChange"
                class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[150px] font-medium appearance-none"
            >
              <option value="">所有用工类型</option>
              <option value="FULL_TIME">全职</option>
              <option value="PROBATION">试用期</option>
              <option value="CONTRACT">合同工</option>
            </select>
            <DateFilterInput
                v-model="query.entryDateStart"
                placeholder="入职开始"
                class="px-3 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary"
                @change="handleFilterChange"
            />
            <DateFilterInput
                v-model="query.entryDateEnd"
                placeholder="入职结束"
                class="px-3 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary"
                @change="handleFilterChange"
            />
            <button
                @click="fetchEmployees"
                class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold hover:bg-primary/90 transition-colors"
            >
              查询
            </button>
            <button
                @click="resetFilter"
                class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold"
            >
              重置
            </button>
            <TableColumnSettings
                :columns="visibleEmployeeColumns"
                :exportable="false"
                @move="moveEmployeeTableColumn"
                @reset="resetEmployeeTableColumns"
            />
          </div>
        </div>

        <div class="employee-table-wrap responsive-table-wrap relative min-h-[240px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-primary animate-spin">progress_activity</span>
          </div>

          <table class="employee-table responsive-data-table w-full text-left border-collapse">
            <colgroup>
              <col v-for="field in visibleEmployeeColumns" :key="field.key" :style="employeeColumnStyle(field.key)" />
              <col style="width: 124px" />
            </colgroup>
            <thead>
            <tr class="bg-surface-container/30 text-on-surface-variant border-b border-surface-variant/50">
              <th
                  v-for="field in visibleEmployeeColumns"
                  :key="field.key"
                  class="px-3 py-3 text-xs font-bold uppercase tracking-wider whitespace-nowrap"
              >
                {{ field.label }}
              </th>
              <th class="px-3 py-3 text-xs font-bold uppercase tracking-wider text-center whitespace-nowrap">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/50">
            <tr v-for="emp in employees" :key="emp.id" class="cursor-pointer hover:bg-surface-container-high/50 transition-colors group" @click="showEmployeeDetail(emp.id)">
              <td
                  v-for="field in visibleEmployeeColumns"
                  :key="field.key"
                  :data-label="field.label"
                  :class="employeeCellClass(field.key)"
              >
                <template v-if="field.key === 'name'">
                <div>
                  <p class="font-bold text-primary leading-none whitespace-nowrap">{{ emp.name }}</p>
                  <p v-if="isEmployeeFieldVisible('employeeType')" class="text-[10px] text-on-surface-variant uppercase mt-1">{{ formatEmployeeType(emp.employeeType) }}</p>
                </div>
                </template>
                <template v-else-if="field.key === 'departmentName'">
                  <span :class="`inline-block max-w-full truncate px-2 py-0.5 rounded text-[11px] font-bold border ${departmentBadge(emp.departmentName)}`">
                    {{ emp.departmentName || '--' }}
                  </span>
                </template>
                <template v-else-if="field.key === 'attendanceRequired'">
                  <span
                      class="inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-black"
                      :class="Number(emp.attendanceRequired ?? 1) === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'"
                  >
                    {{ Number(emp.attendanceRequired ?? 1) === 1 ? '需要打卡' : '免打卡' }}
                  </span>
                </template>
                <template v-else-if="field.key === 'attendanceLocationNames'">
                  <span class="employee-cell-text">
                    {{ Array.isArray(emp.attendanceLocationNames) && emp.attendanceLocationNames.length ? emp.attendanceLocationNames.join('、') : '全部地点' }}
                  </span>
                </template>
                <template v-else-if="field.key === 'status'">
                  <div :class="`flex items-center gap-1.5 font-bold text-xs ${statusMeta(emp.status).text}`">
                    <span :class="`w-1.5 h-1.5 rounded-full ${statusMeta(emp.status).dot}`"></span>
                    {{ statusMeta(emp.status).label }}
                  </div>
                </template>
                <template v-else>
                  <span class="employee-cell-text">{{ employeeColumnText(emp, field.key) }}</span>
                </template>
              </td>
              <td class="px-3 py-3 text-center" data-label="操作">
                <div class="flex justify-center gap-1 opacity-100">
                  <button @click.stop="showEmployeeDetail(emp.id)" class="p-1.5 hover:bg-white rounded-md text-primary" title="查看">
                    <span class="material-symbols-outlined text-[18px]">visibility</span>
                  </button>
                  <button v-permission="'employee:update'" @click.stop="openEditDrawer(emp.id)" class="p-1.5 hover:bg-white rounded-md text-primary" title="编辑">
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                  <button v-permission="'employee:update'" @click.stop="openPermissionDrawer(emp)" class="p-1.5 hover:bg-white rounded-md text-primary" title="单独权限">
                    <span class="material-symbols-outlined text-[18px]">admin_panel_settings</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && employees.length === 0">
              <td :colspan="employeeTableColumnCount" class="px-6 py-12 text-center text-sm text-on-surface-variant">未找到员工记录。</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex flex-wrap items-center justify-between gap-4 text-sm text-on-surface-variant border-t border-surface-variant/50">
          <div class="flex items-center gap-2">
            <span>每页行数</span>
            <select v-model="query.size" @change="handlePageSizeChange" class="bg-white border border-surface-variant/50 rounded-md py-1 px-2 text-xs focus:outline-none">
              <option :value="10">10</option>
              <option :value="25">25</option>
              <option :value="50">50</option>
            </select>
          </div>
          <div class="flex items-center gap-4">
            <p class="hidden sm:block">显示第 {{ pageStart }}-{{ pageEnd }} 条，共 {{ pagination.total }} 条</p>
            <div class="flex gap-1">
              <button
                  @click="changePage(query.page - 1)"
                  :disabled="query.page <= 1"
                  class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 disabled:opacity-50 hover:bg-slate-50 transition-colors"
              >
                <span class="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <button class="min-w-8 h-8 px-2 flex items-center justify-center rounded bg-primary text-white font-bold">{{ query.page }}</button>
              <button
                  @click="changePage(query.page + 1)"
                  :disabled="query.page >= totalPages"
                  class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 disabled:opacity-50 hover:bg-slate-50 transition-colors"
              >
                <span class="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
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
          <button class="rounded-full p-2 text-on-surface-variant hover:bg-surface-container-high hover:text-primary" @click="closeOrganizationDrawer">
            <span class="material-symbols-outlined">close</span>
          </button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
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
import DateFilterInput from '@/components/DateFilterInput.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import EmployeeCreate from './employeeCreate.vue'
import EmployeePermissionDrawer from './EmployeePermissionDrawer.vue'
import { buildEmployeeHierarchy, buildOrganizationChart } from './employeeOrganization.js'
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
// --- 状态定义 ---
const isDrawerOpen = ref(false)
const editingEmployeeId = ref(null)
const permissionDrawerRef = ref(null)
const importInputRef = ref(null)
const loading = ref(false)
const isOrganizationDrawerOpen = ref(false)
const organizationLoading = ref(false)
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
const pageStart = computed(() => (pagination.total === 0 ? 0 : (query.page - 1) * query.size + 1))
const pageEnd = computed(() => Math.min(query.page * query.size, pagination.total || 0))
const employeeColumnRenderers = new Set(['name', 'empNo', 'departmentName', 'positionName', 'phone', 'email', 'leaderName', 'entryDate', 'attendanceRequired', 'attendanceLocationNames', 'status'])
const defaultEmployeeColumns = computed(() => visibleTenantFields(employeeFieldConfig.value, 'name').filter((field) => employeeColumnRenderers.has(field.key)))
const {
  orderedColumns: visibleEmployeeColumns,
  moveColumn: moveEmployeeTableColumn,
  resetColumns: resetEmployeeTableColumns
} = useLocalTableColumns('employee.list', defaultEmployeeColumns)
const employeeTableColumnCount = computed(() => visibleEmployeeColumns.value.length + 1)
const employeeColumnWidths = {
  name: '140px',
  empNo: '92px',
  departmentName: '112px',
  positionName: '112px',
  phone: '128px',
  email: '150px',
  leaderName: '112px',
  entryDate: '106px',
  attendanceRequired: '96px',
  attendanceLocationNames: '140px',
  status: '82px'
}

// 核心逻辑：构建层级结构
const employeeHierarchy = computed(() => buildEmployeeHierarchy(organizationEmployees.value))

const organizationChart = computed(() => buildOrganizationChart(employeeHierarchy.value))
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

const fetchEmployees = async () => {
  loading.value = true
  try {
    const data = await getEmployeePage(normalizeQuery())
    employees.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
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

const employeeCellClass = (key) => {
  const base = 'px-3 py-3 text-sm whitespace-nowrap overflow-hidden text-ellipsis'
  if (key === 'empNo') return `${base} font-mono text-sm text-secondary`
  if (key === 'positionName') return `${base} text-sm font-bold text-primary`
  if (key === 'entryDate') return `${base} text-sm text-on-surface-variant font-medium`
  return base
}

const employeeColumnStyle = (key) => ({
  width: employeeColumnWidths[key] || '104px'
})

const employeeDetailLines = (detail) => visibleEmployeeColumns.value
    .filter((field) => field.key !== 'name')
    .map((field) => `${field.label}: ${field.key === 'status' ? statusMeta(detail.status).label : employeeColumnText(detail, field.key)}`)

const showEmployeeDetail = async (id) => {
  const detail = await getEmployeeDetail(id)
  const detailLines = employeeDetailLines(detail)
  ElMessageBox.alert(
      detailLines.length ? detailLines.join('\n') : '暂无可展示信息',
      detail.name || tenantFieldLabel(employeeFieldConfig.value, 'name', '员工详情'),
      { confirmButtonText: '关闭' }
  )
}

const openCreateDrawer = () => {
  editingEmployeeId.value = null
  isDrawerOpen.value = true
}

const openEditDrawer = (id) => {
  editingEmployeeId.value = id
  isDrawerOpen.value = true
}

const openPermissionDrawer = (employee) => {
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
  organizationLoading.value = true
  try {
    // 获取全部员工用于构建树
    const data = await getEmployeePage({ page: 1, size: 2000 })
    organizationEmployees.value = data.data || []
  } finally {
    organizationLoading.value = false
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

const departmentBadge = (name) => {
  const palettes = [
    'bg-blue-50 text-blue-700 border-blue-200',
    'bg-amber-50 text-amber-700 border-amber-200',
    'bg-emerald-50 text-emerald-700 border-emerald-200',
    'bg-slate-100 text-slate-700 border-slate-200'
  ]
  const index = Math.abs((name || '').split('').reduce((sum, ch) => sum + ch.charCodeAt(0), 0)) % palettes.length
  return palettes[index]
}

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
