<template>
  <div class="h-full min-h-0 bg-surface text-on-surface overflow-x-hidden relative">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 class="text-3xl font-black text-primary tracking-tight">员工名录</h2>
          <p class="text-on-surface-variant mt-1">管理员工记录、入职及人事状态。</p>
        </div>
        <div class="flex gap-3">
          <button
              @click="handleTemplateDownload"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>导入模板
          </button>
          <button
              @click="triggerImport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>导入员工
          </button>
          <button
              @click="handleExport"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>导出 Excel
          </button>
          <button
              @click="showBatchTip"
              class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">edit_square</span>批量编辑
          </button>
          <button
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
          <h3 class="text-4xl font-black text-white mt-2">{{ stats.totalEmployees }}</h3>
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
            <button
                @click="fetchEmployees"
                class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold hover:bg-primary/90 transition-colors"
            >
              查询
            </button>
          </div>
        </div>

        <div class="overflow-x-auto relative min-h-[240px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-primary animate-spin">progress_activity</span>
          </div>

          <table class="w-full text-left border-collapse">
            <thead>
            <tr class="bg-surface-container/30 text-on-surface-variant border-b border-surface-variant/50">
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">员工</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">工号</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">部门</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">职位</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">联系方式</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">状态</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">入职日期</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-right whitespace-nowrap">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/50">
            <tr v-for="emp in employees" :key="emp.id" class="hover:bg-surface-container-high/50 transition-colors group">
              <td class="px-6 py-3">
                <div class="flex items-center gap-3">
                  <img :src="emp.avatarUrl || fallbackAvatar(emp.name)" class="w-9 h-9 rounded-full object-cover bg-slate-200 shrink-0" />
                  <div>
                    <p class="font-bold text-primary leading-none whitespace-nowrap">{{ emp.name }}</p>
                    <p class="text-[10px] text-on-surface-variant uppercase mt-1">{{ formatEmployeeType(emp.employeeType) }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-3 font-mono text-sm text-secondary whitespace-nowrap">{{ emp.empNo || '--' }}</td>
              <td class="px-6 py-3 whitespace-nowrap">
                  <span :class="`px-2 py-0.5 rounded text-[11px] font-bold border ${departmentBadge(emp.departmentName)}`">
                    {{ emp.departmentName || '--' }}
                  </span>
              </td>
              <td class="px-6 py-3 text-sm font-bold text-primary whitespace-nowrap">{{ emp.positionName || '--' }}</td>
              <td class="px-6 py-3 whitespace-nowrap">
                <div class="text-xs font-medium text-primary">{{ emp.email || '--' }}</div>
                <div class="text-xs text-on-surface-variant mt-0.5">{{ emp.phone || '--' }}</div>
              </td>
              <td class="px-6 py-3 whitespace-nowrap">
                <div :class="`flex items-center gap-1.5 font-bold text-xs ${statusMeta(emp.status).text}`">
                  <span :class="`w-1.5 h-1.5 rounded-full ${statusMeta(emp.status).dot}`"></span>
                  {{ statusMeta(emp.status).label }}
                </div>
              </td>
              <td class="px-6 py-3 text-sm text-on-surface-variant font-medium whitespace-nowrap">{{ emp.entryDate || '--' }}</td>
              <td class="px-6 py-3 text-right">
                <div class="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button @click="showEmployeeDetail(emp.id)" class="p-1.5 hover:bg-white rounded-md text-primary" title="查看">
                    <span class="material-symbols-outlined text-[18px]">visibility</span>
                  </button>
                  <button @click="openEditDrawer(emp.id)" class="p-1.5 hover:bg-white rounded-md text-primary" title="编辑">
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && employees.length === 0">
              <td colspan="8" class="px-6 py-12 text-center text-sm text-on-surface-variant">未找到员工记录。</td>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmployeeCreate from './employeeCreate.vue'
import {
  downloadEmployeeImportTemplate,
  exportEmployeesExcel,
  getEmployeeDetail,
  getEmployeeFormOptions,
  getEmployeePage,
  getEmployeeStats,
  importEmployees
} from './api/employee.js'

const isDrawerOpen = ref(false)
const editingEmployeeId = ref(null)
const importInputRef = ref(null)
const loading = ref(false)
const employees = ref([])
const departments = ref([])
const statusOptions = ref([])
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
  status: ''
})

const totalPages = computed(() => Math.max(pagination.pages || 1, 1))
const pageStart = computed(() => (pagination.total === 0 ? 0 : (query.page - 1) * query.size + 1))
const pageEnd = computed(() => Math.min(query.page * query.size, pagination.total || 0))

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
  Object.assign(stats, await getEmployeeStats())
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

const showBatchTip = () => {
  ElMessage.info('批量编辑接口已准备就绪。当前页面仍需实现行选择 UI。')
}

const showEmployeeDetail = async (id) => {
  const detail = await getEmployeeDetail(id)
  ElMessageBox.alert(
      `工号: ${detail.empNo || '--'}\n部门: ${detail.departmentName || '--'}\n职位: ${detail.positionName || '--'}\n角色: ${(detail.roleNames || []).join('、') || '--'}\n直属领导: ${detail.leaderName || '--'}\n状态: ${statusMeta(detail.status).label}`,
      detail.name,
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

const closeDrawer = () => {
  isDrawerOpen.value = false
  editingEmployeeId.value = null
}

const handleExport = async () => {
  const blob = await exportEmployeesExcel(normalizeQuery())
  downloadBlob(blob, `员工列表-${Date.now()}.xlsx`)
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
      `总行数：${result.totalCount}\n成功：${result.successCount}\n失败：${result.failCount}${failText ? `\n\n失败明细：\n${failText}` : ''}`,
      '员工导入结果',
      { confirmButtonText: '关闭' }
    )
    await Promise.all([fetchEmployees(), fetchStats(), fetchFormOptions()])
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
  status: query.status === '' ? undefined : Number(query.status)
})

const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatEmployeeType = (value) => ({ FULL_TIME: '全职', CONTRACT: '合同工', PROBATION: '试用期' }[value] || value || '--')
const fallbackAvatar = (name) => `https://placehold.co/100x100/e2e8f0/64748b?text=${encodeURIComponent((name || 'U').slice(0, 1).toUpperCase())}`
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
  if (Number(status) === 1) return { label: '在职', text: 'text-emerald-600', dot: 'bg-emerald-600' }
  if (Number(status) === 2) return { label: '试用', text: 'text-amber-600', dot: 'bg-amber-500' }
  if (Number(status) === 0) return { label: '离职', text: 'text-slate-500', dot: 'bg-slate-400' }
  return { label: '未知', text: 'text-slate-500', dot: 'bg-slate-400' }
}

onMounted(async () => {
  await Promise.all([fetchEmployees(), fetchStats(), fetchFormOptions()])
})
</script>
