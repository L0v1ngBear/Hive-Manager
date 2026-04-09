<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden relative">
    <div class="max-w-7xl mx-auto space-y-8">
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 class="text-3xl font-black text-primary tracking-tight">Employee Directory</h2>
          <p class="text-on-surface-variant mt-1">Manage employee records, onboarding, and staffing status.</p>
        </div>
        <div class="flex gap-3">
          <button
            @click="handleExport"
            class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">upload_file</span>Export List
          </button>
          <button
            @click="showBatchTip"
            class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">edit_square</span>Batch Edit
          </button>
          <button
            @click="isDrawerOpen = true"
            class="px-5 py-2 bg-primary text-white font-bold rounded-lg flex items-center gap-2 shadow-md hover:bg-primary/90 transition-all text-sm active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">person_add</span>Add Employee
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-primary-container p-6 rounded-xl relative overflow-hidden group">
          <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-[80px]">groups</span>
          </div>
          <p class="text-on-primary-container text-sm font-bold uppercase tracking-widest">Total Employees</p>
          <h3 class="text-4xl font-black text-white mt-2">{{ stats.totalEmployees }}</h3>
          <p class="text-on-primary-container text-xs mt-3">Realtime employee base</p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">Today Attendance</p>
          <div class="flex items-end justify-between mt-2">
            <h3 class="text-4xl font-black text-primary">{{ formatPercent(stats.todayAttendanceRate) }}</h3>
            <div class="h-10 w-20 bg-surface-container-high rounded flex items-end p-1 gap-0.5">
              <div class="w-full bg-primary/20 h-[60%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[70%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[85%] rounded-t-sm"></div>
              <div class="w-full bg-primary h-full rounded-t-sm"></div>
            </div>
          </div>
          <p class="text-on-surface-variant text-xs mt-3 uppercase font-bold tracking-tighter">Synced from attendance records</p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">Departments</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.departmentCount }}</h3>
          <p class="text-on-surface-variant text-xs mt-3">Active org units available for assignment</p>
        </div>

        <div class="bg-white/70 backdrop-blur-md p-6 rounded-xl shadow-sm border border-orange-200">
          <p class="text-orange-900 text-sm font-bold uppercase tracking-widest">Pending Onboard</p>
          <h3 class="text-4xl font-black text-orange-700 mt-2">{{ stats.pendingOnboardCount }}</h3>
          <p class="text-orange-900/70 text-xs mt-3 flex items-center gap-1 font-medium">
            <span class="material-symbols-outlined text-xs">schedule</span> Future entry date records
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
              placeholder="Search by name, phone, or employee number"
            />
          </div>
          <div class="flex items-center gap-3 flex-wrap">
            <select
              v-model="query.departmentId"
              @change="handleFilterChange"
              class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
            >
              <option value="">All Departments</option>
              <option v-for="department in departments" :key="department.id" :value="department.id">
                {{ department.name }}
              </option>
            </select>
            <select
              v-model="query.status"
              @change="handleFilterChange"
              class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[160px] font-medium appearance-none"
            >
              <option value="">All Status</option>
              <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
            <button
              @click="fetchEmployees"
              class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold hover:bg-primary/90 transition-colors"
            >
              Apply
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
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Employee</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Employee No</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Department</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Position</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Contact</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Status</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">Entry Date</th>
                <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-right whitespace-nowrap">Actions</th>
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
                    <button @click="showEmployeeDetail(emp.id)" class="p-1.5 hover:bg-white rounded-md text-primary" title="View">
                      <span class="material-symbols-outlined text-[18px]">visibility</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && employees.length === 0">
                <td colspan="8" class="px-6 py-12 text-center text-sm text-on-surface-variant">No employee records found.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex flex-wrap items-center justify-between gap-4 text-sm text-on-surface-variant border-t border-surface-variant/50">
          <div class="flex items-center gap-2">
            <span>Rows per page</span>
            <select v-model="query.size" @change="handlePageSizeChange" class="bg-white border border-surface-variant/50 rounded-md py-1 px-2 text-xs focus:outline-none">
              <option :value="10">10</option>
              <option :value="25">25</option>
              <option :value="50">50</option>
            </select>
          </div>
          <div class="flex items-center gap-4">
            <p class="hidden sm:block">Showing {{ pageStart }}-{{ pageEnd }} of {{ pagination.total }}</p>
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
  </div>

  <EmployeeCreate :visible="isDrawerOpen" @close="isDrawerOpen = false" @success="handleCreateSuccess" />
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmployeeCreate from './employeeCreate.vue'
import { exportEmployees, getEmployeeDetail, getEmployeeFormOptions, getEmployeePage, getEmployeeStats } from './api/employee.js'

const isDrawerOpen = ref(false)
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
  isDrawerOpen.value = false
  await Promise.all([fetchEmployees(), fetchStats()])
}

const showBatchTip = () => {
  ElMessage.info('Batch edit endpoint is ready. The current page still needs row selection UI.')
}

const showEmployeeDetail = async (id) => {
  const detail = await getEmployeeDetail(id)
  ElMessageBox.alert(
    `Employee No: ${detail.empNo || '--'}\nDepartment: ${detail.departmentName || '--'}\nPosition: ${detail.positionName || '--'}\nLeader: ${detail.leaderName || '--'}\nStatus: ${statusMeta(detail.status).label}`,
    detail.name,
    { confirmButtonText: 'Close' }
  )
}

const handleExport = async () => {
  const rows = await exportEmployees(normalizeQuery())
  const header = ['Name', 'Employee No', 'Department', 'Position', 'Email', 'Phone', 'Status', 'Entry Date']
  const body = (rows || []).map((item) => [
    item.name || '',
    item.empNo || '',
    item.departmentName || '',
    item.positionName || '',
    item.email || '',
    item.phone || '',
    statusMeta(item.status).label,
    item.entryDate || ''
  ])
  const csv = [header, ...body]
    .map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `employees-${Date.now()}.csv`
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
const formatEmployeeType = (value) => ({ FULL_TIME: 'Full Time', CONTRACT: 'Contract', PROBATION: 'Probation' }[value] || value || '--')
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
  if (Number(status) === 1) return { label: 'Active', text: 'text-emerald-600', dot: 'bg-emerald-600' }
  if (Number(status) === 2) return { label: 'Probation', text: 'text-amber-600', dot: 'bg-amber-500' }
  if (Number(status) === 0) return { label: 'Resigned', text: 'text-slate-500', dot: 'bg-slate-400' }
  return { label: 'Unknown', text: 'text-slate-500', dot: 'bg-slate-400' }
}

onMounted(async () => {
  await Promise.all([fetchEmployees(), fetchStats(), fetchFormOptions()])
})
</script>
