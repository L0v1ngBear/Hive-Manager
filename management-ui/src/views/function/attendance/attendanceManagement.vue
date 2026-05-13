<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <div class="function-page-container space-y-6 p-2 md:p-4">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined text-[16px]">fingerprint</span>
            考勤中心
          </div>
          <h1 class="function-page-title">考勤管理</h1>
          <p class="function-page-desc">
            对齐小程序打卡记录，按日期、部门、员工和异常状态查看每日考勤情况。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <button
              v-permission="'attendance:*'"
              class="function-action-dark"
              @click="openRuleDrawer"
          >
            <span class="material-symbols-outlined text-[20px]">rule</span>规则配置
          </button>

          <button
              class="function-action-primary"
              @click="refreshAll"
          >
            <span class="material-symbols-outlined text-[20px]">refresh</span>刷新数据
          </button>
          <button
              v-permission="'attendance:record:list'"
              class="function-action-secondary"
              @click="exportExcel"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>导出当前页
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-4">
        <div
            v-for="stat in stats"
            :key="stat.label"
            class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all"
        >
          <span
              class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] opacity-50 group-hover:scale-110 transition-transform"
              :class="stat.iconClass"
          >
            {{ stat.icon }}
          </span>
          <p class="text-xs font-bold text-slate-500 tracking-widest relative z-10">{{ stat.label }}</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black truncate" :class="stat.valueClass">{{ stat.value }}</h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ stat.unit }}</span>
          </div>
          <p v-if="stat.desc" class="relative z-10 text-xs text-slate-400 mt-3">{{ stat.desc }}</p>
        </div>
      </section>

      <section class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-5 border-b border-slate-100 bg-slate-50/50">
          <div class="flex flex-wrap items-end gap-3">
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">日期</span>
              <input
                  v-model="query.date"
                  type="date"
                  class="w-44 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              />
            </label>
            <label class="block flex-1 min-w-[220px] max-w-sm">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">员工搜索</span>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
                <input
                    v-model.trim="query.keyword"
                    class="w-full pl-10 pr-4 py-2.5 bg-white rounded-xl border border-slate-200 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500"
                    placeholder="姓名、手机号或工号"
                    @keyup.enter="handleFilter"
                />
              </div>
            </label>
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">部门</span>
              <select
                  v-model="query.departmentName"
                  class="w-44 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              >
                <option value="">全部部门</option>
                <option v-for="item in departments" :key="item.name" :value="item.name">{{ item.name }}</option>
              </select>
            </label>
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">状态</span>
              <select
                  v-model="query.status"
                  class="w-36 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              >
                <option value="">全部状态</option>
                <option value="normal">正常</option>
                <option value="late">迟到</option>
                <option value="early">早退</option>
                <option value="missing">缺勤/缺卡</option>
                <option value="leave">请假</option>
                <option value="overtime">加班</option>
              </select>
            </label>
            <button class="px-5 py-2.5 bg-blue-50 text-blue-600 rounded-xl text-sm font-bold hover:bg-blue-100 transition-colors" @click="handleFilter">查询</button>
            <button class="px-5 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors" @click="resetFilter">重置</button>
          </div>
        </div>

        <div class="overflow-x-auto relative min-h-[420px]">
          <div v-if="loading" class="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center gap-3">
            <span class="material-symbols-outlined text-blue-600 text-4xl animate-spin">progress_activity</span>
            <span class="text-sm font-medium text-blue-600">加载考勤数据中...</span>
          </div>
          <table class="w-full text-left border-collapse min-w-[980px]">
            <thead class="bg-slate-50/80 sticky top-0 z-0">
            <tr>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">员工</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">工号</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">部门</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">上班打卡</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">下班打卡</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">更新时间</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
            <tr v-for="row in rows" :key="row.id" class="hover:bg-blue-50/40 transition-colors">
              <td class="px-6 py-4">
                <div class="font-bold text-slate-800">{{ row.employeeName || '未命名员工' }}</div>
                <div class="text-xs text-slate-400 mt-0.5">{{ row.phone || '--' }}</div>
              </td>
              <td class="px-6 py-4 text-sm font-mono text-slate-500">{{ row.empNo || `UID-${row.userId}` }}</td>
              <td class="px-6 py-4 text-sm text-slate-600">{{ row.departmentName || '未分配部门' }}</td>
              <td class="px-6 py-4 text-sm font-mono text-slate-700">{{ formatTime(row.signInTime) }}</td>
              <td class="px-6 py-4 text-sm font-mono text-slate-700">{{ formatTime(row.signOutTime) }}</td>
              <td class="px-6 py-4">
                <span :class="statusClass(row.status)" class="inline-flex px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wider">
                  {{ row.statusText || '正常' }}
                </span>
              </td>
              <td class="px-6 py-4 text-xs text-slate-500">{{ formatDateTime(row.updateTime || row.createTime) }}</td>
            </tr>
            <tr v-if="!loading && rows.length === 0">
              <td colspan="7" class="px-6 py-16 text-center">
                <div class="flex flex-col items-center justify-center text-slate-400">
                  <span class="material-symbols-outlined text-5xl mb-2 opacity-50">event_busy</span>
                  <p class="text-sm">当前筛选条件下暂无考勤记录</p>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-slate-50 flex items-center justify-between text-sm text-slate-500 border-t border-slate-100">
          <span>共 <b class="text-slate-800">{{ pagination.total }}</b> 条，第 <b class="text-slate-800">{{ query.pageNum }}</b> / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">上一页</button>
            <button @click="changePage(query.pageNum + 1)" :disabled="query.pageNum >= totalPages" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <el-drawer
        v-model="ruleDrawerVisible"
        size="480px"
        :with-header="false"
        class="!bg-transparent"
    >
      <div class="flex flex-col h-full bg-white/90 backdrop-blur-2xl border-t-[4px] border-blue-600 shadow-[-20px_0px_40px_rgba(0,32,69,0.06)] font-sans">

        <div class="px-8 py-6 border-b border-slate-200/50 flex justify-between items-center bg-white">
          <div>
            <h2 class="text-xl font-bold text-slate-800 tracking-tight">考勤规则配置</h2>
          </div>
          <button @click="ruleDrawerVisible = false" class="text-slate-400 hover:text-slate-800 transition-colors">
            <span class="material-symbols-outlined text-[24px]">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 space-y-10 bg-gradient-to-b from-white to-slate-50/50">

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">schedule</span>
              <h3 class="text-sm font-bold text-slate-800">上下班时间</h3>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">上班开始时间</label>
                <input v-model="ruleForm.workStartTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">上班结束时间</label>
                <input v-model="ruleForm.workEndTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">下班开始时间</label>
                <input v-model="ruleForm.offWorkStartTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">下班结束时间</label>
                <input v-model="ruleForm.offWorkEndTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">more_time</span>
              <h3 class="text-sm font-bold text-slate-800">加班打卡时间段</h3>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">加班开始时间</label>
                <input v-model="ruleForm.overTimeStartTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">加班结束时间</label>
                <input v-model="ruleForm.overTimeEndTime" type="time" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors outline-none"/>
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">timelapse</span>
              <h3 class="text-sm font-bold text-slate-800">考勤弹性</h3>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-1 relative">
                <label class="block text-xs font-bold text-slate-500">迟到容差</label>
                <div class="relative">
                  <input v-model.number="ruleForm.lateToleranceMinutes" type="number" min="0" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors pr-10 outline-none"/>
                  <span class="absolute right-3 top-2 text-xs text-slate-400">分钟</span>
                </div>
              </div>
              <div class="space-y-1 relative">
                <label class="block text-xs font-bold text-slate-500">早退容差</label>
                <div class="relative">
                  <input v-model.number="ruleForm.earlyToleranceMinutes" type="number" min="0" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 focus:ring-0 text-sm text-slate-800 px-3 py-2 rounded-t transition-colors pr-10 outline-none"/>
                  <span class="absolute right-3 top-2 text-xs text-slate-400">分钟</span>
                </div>
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">calendar_month</span>
              <h3 class="text-sm font-bold text-slate-800">工作日设置</h3>
            </div>
            <div class="flex flex-wrap gap-2">
              <label v-for="day in weekDays" :key="day.value" class="cursor-pointer">
                <input v-model="ruleForm.workDays" type="checkbox" :value="day.value" class="peer sr-only"/>
                <div class="px-4 py-2 text-xs font-bold rounded-lg bg-slate-100 border border-slate-200/50 text-slate-500 peer-checked:bg-blue-100 peer-checked:text-blue-700 peer-checked:border-blue-300 transition-all select-none">
                  {{ day.label }}
                </div>
              </label>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">location_on</span>
              <h3 class="text-sm font-bold text-slate-800">打卡地点</h3>
            </div>
            <div class="space-y-3">
              <label class="flex items-center justify-between p-4 rounded-xl border border-slate-200 bg-white cursor-pointer hover:bg-slate-50 transition-colors shadow-sm">
                <div>
                  <div class="text-sm font-bold text-slate-800">GPS 地理围栏</div>
                  <div class="text-xs text-slate-500 mt-0.5">要求在工厂规定的地理范围内打卡</div>
                </div>
                <div class="relative inline-flex items-center cursor-pointer">
                  <input v-model="ruleForm.enableGps" type="checkbox" class="sr-only peer" />
                  <div class="w-10 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </div>
              </label>
              <div v-if="ruleForm.enableGps" class="grid grid-cols-2 gap-4">
                <div class="space-y-1">
                  <label class="block text-xs font-bold text-slate-500">纬度</label>
                  <input v-model.number="ruleForm.latitude" type="number" step="0.000001" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 text-sm text-slate-800 px-3 py-2 rounded-t outline-none" placeholder="例如 30.27415"/>
                </div>
                <div class="space-y-1">
                  <label class="block text-xs font-bold text-slate-500">经度</label>
                  <input v-model.number="ruleForm.longitude" type="number" step="0.000001" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 text-sm text-slate-800 px-3 py-2 rounded-t outline-none" placeholder="例如 120.15515"/>
                </div>
                <div class="space-y-1">
                  <label class="block text-xs font-bold text-slate-500">允许半径</label>
                  <input v-model.number="ruleForm.radius" type="number" min="1" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 text-sm text-slate-800 px-3 py-2 rounded-t outline-none" placeholder="米"/>
                </div>
                <div class="space-y-1">
                  <label class="block text-xs font-bold text-slate-500">地点名称</label>
                  <input v-model.trim="ruleForm.address" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 text-sm text-slate-800 px-3 py-2 rounded-t outline-none" placeholder="工厂地址"/>
                </div>
              </div>

              <label class="flex items-center justify-between p-4 rounded-xl border border-slate-200 bg-white cursor-pointer hover:bg-slate-50 transition-colors shadow-sm">
                <div>
                  <div class="text-sm font-bold text-slate-800">Wi-Fi 验证</div>
                  <div class="text-xs text-slate-500 mt-0.5">必须连接到公司指定的内网 Wi-Fi</div>
                </div>
                <div class="relative inline-flex items-center cursor-pointer">
                  <input v-model="ruleForm.enableWifi" type="checkbox" class="sr-only peer" />
                  <div class="w-10 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </div>
              </label>
              <div v-if="ruleForm.enableWifi" class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">Wi-Fi 名称</label>
                <input v-model.trim="ruleForm.wifiSsid" class="w-full bg-slate-50 border-0 border-b border-slate-300 focus:border-blue-600 text-sm text-slate-800 px-3 py-2 rounded-t outline-none" placeholder="预留配置，后续小程序可接入 Wi-Fi 校验"/>
              </div>
            </div>
          </section>

        </div>

        <div class="px-8 py-4 border-t border-slate-200/50 bg-white flex justify-end gap-3 shrink-0">
          <button @click="ruleDrawerVisible = false" class="px-5 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors">取消</button>
          <button @click="submitRule" class="px-5 py-2 text-sm font-bold bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-md shadow-blue-600/20 active:scale-95">保存配置</button>
        </div>

      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElDrawer } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  exportAttendanceExcel,
  getAttendanceDepartments,
  getAttendancePage,
  getAttendanceRule,
  getAttendanceSummary,
  saveAttendanceRule
} from './api/attendance.js'

const route = useRoute()
// 控制抽屉显示隐藏的变量
const ruleDrawerVisible = ref(false)
const weekDays = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]
const ruleForm = reactive({
  workStartTime: '08:00',
  workEndTime: '12:00',
  offWorkStartTime: '13:00',
  offWorkEndTime: '17:00',
  overTimeStartTime: '18:00',
  overTimeEndTime: '21:00',
  lateToleranceMinutes: 0,
  earlyToleranceMinutes: 0,
  workDays: [1, 2, 3, 4, 5],
  enableGps: false,
  latitude: undefined,
  longitude: undefined,
  radius: 200,
  address: '',
  enableWifi: false,
  wifiSsid: ''
})

const today = new Date().toISOString().slice(0, 10)
const rows = ref([])
const departments = ref([])
const loading = ref(false)
const summary = reactive({ totalEmployeeCount: 0, actualCount: 0, lateCount: 0, earlyCount: 0, missingCount: 0, attendanceRate: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', departmentName: '', status: '', date: today })

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const stats = computed(() => [
  { label: '应出勤员工', value: summary.totalEmployeeCount, unit: '人', desc: '当前启用员工数', icon: 'groups', iconClass: 'text-blue-50', valueClass: 'text-slate-800' },
  { label: '实际打卡', value: summary.actualCount, unit: '人', desc: `出勤率 ${summary.attendanceRate || 0}%`, icon: 'how_to_reg', iconClass: 'text-emerald-50', valueClass: 'text-emerald-600' },
  { label: '迟到', value: summary.lateCount, unit: '人', desc: '上班打卡晚于规则时间', icon: 'schedule', iconClass: 'text-orange-50', valueClass: 'text-orange-600' },
  { label: '早退', value: summary.earlyCount, unit: '人', desc: '下班打卡早于规则时间', icon: 'logout', iconClass: 'text-amber-50', valueClass: 'text-amber-600' },
  { label: '缺勤/缺卡', value: summary.missingCount, unit: '人', desc: '缺勤或缺少打卡记录', icon: 'error', iconClass: 'text-rose-50', valueClass: 'text-rose-600' }
])

applyRouteKeyword()
refreshAll()

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await refreshAll()
  }
)

function applyRouteKeyword() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== query.keyword) {
    query.keyword = routeKeyword
    query.pageNum = 1
  }
}

async function refreshAll() {
  await Promise.all([fetchSummary(), fetchData(), fetchDepartments()])
}

async function fetchSummary() {
  Object.assign(summary, await getAttendanceSummary({ date: query.date }))
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getAttendancePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      departmentName: query.departmentName || undefined,
      status: query.status || undefined,
      date: query.date || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

async function fetchDepartments() {
  departments.value = await getAttendanceDepartments()
}

async function openRuleDrawer() {
  const data = await getAttendanceRule()
  Object.assign(ruleForm, {
    ...data,
    workDays: Array.isArray(data.workDays) && data.workDays.length ? data.workDays : [1, 2, 3, 4, 5],
    enableGps: data.enableGps !== false,
    enableWifi: data.enableWifi === true,
    radius: data.radius || 200
  })
  ruleDrawerVisible.value = true
}

async function submitRule() {
  if (!ruleForm.workDays.length) {
    ElMessage.warning('请至少选择一个工作日')
    return
  }
  if (ruleForm.enableGps && (!ruleForm.latitude || !ruleForm.longitude || !ruleForm.radius)) {
    ElMessage.warning('启用 GPS 围栏时，请填写经纬度和允许半径')
    return
  }
  await saveAttendanceRule({
    ...ruleForm,
    lateToleranceMinutes: Number(ruleForm.lateToleranceMinutes || 0),
    earlyToleranceMinutes: Number(ruleForm.earlyToleranceMinutes || 0),
    radius: ruleForm.radius == null ? undefined : Number(ruleForm.radius)
  })
  ElMessage.success('考勤规则已保存，小程序打卡规则已同步')
  ruleDrawerVisible.value = false
}

function handleFilter() {
  query.pageNum = 1
  refreshAll()
}

function resetFilter() {
  Object.assign(query, { pageNum: 1, keyword: '', departmentName: '', status: '', date: today })
  refreshAll()
}

function changePage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) {
    return
  }
  query.pageNum = pageNum
  fetchData()
}

async function exportExcel() {
  const blob = await exportAttendanceExcel({
    keyword: query.keyword || undefined,
    departmentName: query.departmentName || undefined,
    status: query.status || undefined,
    date: query.date || undefined
  })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([blob], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
  link.download = `考勤记录_${query.date || today}.xlsx`
  link.click()
  URL.revokeObjectURL(link.href)
}

function statusClass(status) {
  if (status === 'late') return 'bg-orange-50 text-orange-700 ring-1 ring-inset ring-orange-200/60'
  if (status === 'early') return 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-200/60'
  if (status === 'missing') return 'bg-rose-50 text-rose-700 ring-1 ring-inset ring-rose-200/60'
  if (status === 'leave') return 'bg-sky-50 text-sky-700 ring-1 ring-inset ring-sky-200/60'
  if (status === 'overtime') return 'bg-violet-50 text-violet-700 ring-1 ring-inset ring-violet-200/60'
  return 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200/60'
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : '--:--'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--'
}
</script>

<style>
/* 针对 el-drawer 的全局样式覆盖，使其实现玻璃态无边框设计 */
.el-drawer {
  background-color: transparent !important;
  box-shadow: none !important;
}
.el-overlay {
  background-color: rgba(15, 23, 42, 0.4) !important;
  backdrop-filter: blur(2px);
}
</style>
