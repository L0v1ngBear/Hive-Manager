<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <div class="function-page-container space-y-4 p-2 md:p-4">
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
          <el-button
              v-permission="['attendance:rule:list', 'attendance:rule:update']"
              class="function-action-dark"
              :disabled="!canReadRule"
              :title="canReadRule ? '考勤规则配置' : '暂无 attendance:rule:list 权限'"
              @click="openRuleDrawer"
          >
            <span class="material-symbols-outlined text-[20px]">rule</span>规则配置
          </el-button>

          <el-button
              class="function-action-primary"
              :disabled="!canReadRecords"
              @click="refreshAll"
          >
            <span class="material-symbols-outlined text-[20px]">refresh</span>刷新数据
          </el-button>
          <el-button
              v-permission="'attendance:export'"
              class="function-action-secondary"
              @click="exportExcel"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>导出当前页
          </el-button>
        </div>
      </header>

      <section v-if="summaryLoading" v-loading="true" class="function-stats-grid min-h-20 rounded-lg bg-white" />
      <section v-else-if="summaryError" class="flex min-h-20 flex-col items-center justify-center gap-2 rounded-lg bg-white text-center">
        <p class="font-bold text-slate-800">{{ summaryError.title }}</p>
        <p class="text-sm text-slate-500">{{ summaryError.message }}</p>
        <el-button type="primary" @click="fetchSummary(currentQuerySnapshot())">重新加载统计</el-button>
      </section>
      <section v-else-if="summaryEmpty" class="flex min-h-20 items-center justify-center rounded-lg bg-white text-sm text-slate-500">暂无考勤统计</section>
      <section v-else class="function-stats-grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5">
        <div
            v-for="stat in stats"
            :key="stat.label"
            class="function-stat-card relative overflow-hidden bg-white group hover:shadow-md transition-all"
        >
          <span
              class="material-symbols-outlined absolute -right-3 -bottom-3 text-[80px] opacity-50 group-hover:scale-110 transition-transform"
              :class="stat.iconClass"
          >
            {{ stat.icon }}
          </span>
          <p class="text-xs font-bold text-slate-500 tracking-widest relative z-10">{{ stat.label }}</p>
          <div class="mt-2 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-2xl font-black truncate" :class="stat.valueClass">{{ stat.value }}</h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ stat.unit }}</span>
          </div>
          <p v-if="stat.desc" class="relative z-10 text-xs text-slate-400 mt-2">{{ stat.desc }}</p>
        </div>
      </section>

      <section class="function-list-panel shadow-sm border-slate-200">
        <div v-filter-collapse class="function-filter-form border-b border-slate-100 bg-slate-50/50 p-4">
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">日期</span>
              <el-date-picker
                  v-model="query.date"
                  type="date"
                  value-format="YYYY-MM-DD"
                  class="w-44 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              />
            </label>
            <label class="block flex-1 min-w-[220px] max-w-sm">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">员工搜索</span>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
                <el-input
                    v-model.trim="query.keyword"
                    class="w-full pl-10 pr-4 py-2.5 bg-white rounded-xl border border-slate-200 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500"
                    placeholder="姓名、手机号或工号"
                    @keyup.enter="handleFilter"
                />
              </div>
            </label>
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">部门</span>
              <el-select
                  v-model="query.departmentName"
                  class="w-44 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              >
                <el-option label="全部部门" value="" />
                <el-option v-for="item in departments" :key="item.name" :label="item.name" :value="item.name" />
              </el-select>
            </label>
            <label class="block">
              <span class="block text-xs text-slate-500 font-bold mb-1.5">状态</span>
              <el-select
                  v-model="query.status"
                  class="w-36 rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 bg-white"
                  @change="handleFilter"
              >
                <el-option label="全部状态" value="" />
                <el-option label="正常" value="normal" />
                <el-option label="迟到" value="late" />
                <el-option label="早退" value="early" />
                <el-option label="缺勤/缺卡" value="missing" />
                <el-option label="请假" value="leave" />
                <el-option label="加班" value="overtime" />
              </el-select>
            </label>
            <div class="function-filter-actions">
            <el-button class="px-5 py-2.5 bg-blue-50 text-blue-600 rounded-xl text-sm font-bold hover:bg-blue-100 transition-colors" @click="handleFilter">查询</el-button>
            <el-button class="px-5 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors" @click="resetFilter">重置</el-button>
            <TableColumnSettings
                :columns="attendanceTableColumns"
                :exportable="false"
                @move="moveAttendanceTableColumn"
                @reset="resetAttendanceTableColumns"
            />
            </div>
        </div>

        <div class="function-table-scroll responsive-table-wrap relative">
          <div v-if="listError" class="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
            <span class="material-symbols-outlined text-4xl text-slate-400">
              {{ listError.type === 'permission' ? 'lock' : 'cloud_off' }}
            </span>
            <h3 class="text-base font-black text-slate-800">{{ listError.title }}</h3>
            <p class="max-w-lg text-sm text-slate-500">{{ listError.message }}</p>
            <el-button type="primary" @click="fetchData">重新加载</el-button>
          </div>
          <el-table v-else v-loading="loading" :data="rows" class="w-full">
            <el-table-column
                v-for="column in attendanceTableColumns"
                :key="column.key"
                :label="column.label"
                min-width="120"
            >
              <template #default="{ row }">
                <span v-if="column.key === 'employee'">{{ row.employeeName || '未命名员工' }}</span>
                <span v-else-if="column.key === 'empNo'">{{ row.empNo || `UID-${row.userId}` }}</span>
                <span v-else-if="column.key === 'department'">{{ row.departmentName || '未分配部门' }}</span>
                <span v-else-if="column.key === 'signIn'">{{ formatTime(row.signInTime) }}</span>
                <span v-else-if="column.key === 'signOut'">{{ formatTime(row.signOutTime) }}</span>
                <span v-else-if="column.key === 'status'" :class="statusClass(row.status)">{{ row.statusText || '正常' }}</span>
                <span v-else>{{ formatDateTime(row.updateTime || row.createTime) }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty v-if="!loading" description="暂无考勤记录" />
            </template>
          </el-table>
        </div>

        <div class="p-4 bg-slate-50 flex items-center justify-end border-t border-slate-100">
          <el-pagination
              v-model:current-page="query.pageNum"
              :page-size="query.pageSize"
              :total="pagination.total"
              layout="total, prev, pager, next"
              @current-change="changePage"
          />
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
          <el-button @click="ruleDrawerVisible = false" class="text-slate-400 hover:text-slate-800 transition-colors">
            <span class="material-symbols-outlined text-[24px]">close</span>
          </el-button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 space-y-10 bg-gradient-to-b from-white to-slate-50/50">
          <div v-if="ruleLoading" v-loading="true" class="min-h-[320px]" />
          <div v-else-if="ruleError" class="flex min-h-[320px] flex-col items-center justify-center gap-3 text-center">
            <p class="font-bold text-slate-800">{{ ruleError.title }}</p>
            <p class="text-sm text-slate-500">{{ ruleError.message }}</p>
            <el-button type="primary" @click="loadRule">重新加载</el-button>
          </div>
          <div v-else-if="ruleEmpty" class="flex min-h-[320px] flex-col items-center justify-center gap-3 text-center">
            <el-empty description="暂无考勤规则" />
            <el-button type="primary" @click="initializeDefaultRule">使用默认规则创建</el-button>
          </div>
          <template v-else>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">schedule</span>
              <h3 class="text-sm font-bold text-slate-800">上下班时间</h3>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">上班开始时间</label>
                <el-time-picker v-model="ruleForm.workStartTime" value-format="HH:mm" format="HH:mm" class="w-full" />
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">上班结束时间</label>
                <el-time-picker v-model="ruleForm.workEndTime" value-format="HH:mm" format="HH:mm" class="w-full" />
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">下班开始时间</label>
                <el-time-picker v-model="ruleForm.offWorkStartTime" value-format="HH:mm" format="HH:mm" class="w-full" />
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">下班结束时间</label>
                <el-time-picker v-model="ruleForm.offWorkEndTime" value-format="HH:mm" format="HH:mm" class="w-full" />
              </div>
            </div>
          </section>
          </template>

          <section class="space-y-4">
            <div class="flex items-center gap-2 pb-2 border-b border-slate-200/50">
              <span class="material-symbols-outlined text-blue-600 text-[18px]">more_time</span>
              <h3 class="text-sm font-bold text-slate-800">加班打卡时间段</h3>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">加班开始时间</label>
                <el-time-picker v-model="ruleForm.overTimeStartTime" value-format="HH:mm" format="HH:mm" class="w-full" />
              </div>
              <div class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">加班结束时间</label>
                <el-time-picker v-model="ruleForm.overTimeEndTime" value-format="HH:mm" format="HH:mm" class="w-full" />
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
                  <el-input-number v-model="ruleForm.lateToleranceMinutes" :min="0" class="w-full" />
                  <span class="absolute right-3 top-2 text-xs text-slate-400">分钟</span>
                </div>
              </div>
              <div class="space-y-1 relative">
                <label class="block text-xs font-bold text-slate-500">早退容差</label>
                <div class="relative">
                  <el-input-number v-model="ruleForm.earlyToleranceMinutes" :min="0" class="w-full" />
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
            <el-checkbox-group v-model="ruleForm.workDays" class="flex flex-wrap gap-2">
              <el-checkbox
                  v-for="day in weekDays"
                  :key="day.value"
                  :value="day.value"
                  class="!m-0 !h-auto [&_.el-checkbox__input]:hidden [&_.el-checkbox__label]:p-0"
              >
                <div
                    class="px-4 py-2 text-xs font-bold rounded-lg border transition-all select-none"
                    :class="ruleForm.workDays.includes(day.value)
                      ? 'border-blue-300 bg-blue-100 text-blue-700'
                      : 'border-slate-200/50 bg-slate-100 text-slate-500'"
                >
                  {{ day.label }}
                </div>
              </el-checkbox>
            </el-checkbox-group>
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
                <el-checkbox v-model="ruleForm.enableGps">启用</el-checkbox>
              </label>
              <div v-if="ruleForm.enableGps" class="space-y-3">
                <div
                    v-for="(location, index) in ruleForm.locations"
                    :key="`attendance-location-${index}`"
                    class="rounded-2xl border border-blue-100 bg-blue-50/40 p-4 space-y-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <div class="text-sm font-black text-slate-800">打卡点 {{ index + 1 }}</div>
                    <el-button
                        class="text-xs font-bold text-rose-500 hover:text-rose-600"
                        @click.prevent="removeAttendanceLocation(index)"
                    >
                      删除
                    </el-button>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div class="space-y-1">
                      <label class="block text-xs font-bold text-slate-500">地点名称</label>
                      <el-input v-model.trim="location.locationName" placeholder="例如：工厂南门" />
                    </div>
                    <div class="space-y-1">
                      <label class="block text-xs font-bold text-slate-500">允许半径（米）</label>
                      <el-input-number v-model="location.radius" :min="1" :max="5000" :step="1" class="w-full" />
                    </div>
                    <div class="space-y-1">
                      <label class="block text-xs font-bold text-slate-500">纬度</label>
                      <el-input-number v-model="location.latitude" :step="0.000001" :precision="6" :controls="false" class="w-full" />
                    </div>
                    <div class="space-y-1">
                      <label class="block text-xs font-bold text-slate-500">经度</label>
                      <el-input-number v-model="location.longitude" :step="0.000001" :precision="6" :controls="false" class="w-full" />
                    </div>
                    <div class="space-y-1 col-span-2">
                      <label class="block text-xs font-bold text-slate-500">地址备注</label>
                      <el-input v-model.trim="location.address" placeholder="可填写园区、楼栋或门岗位置" />
                    </div>
                  </div>
                </div>
                <el-button
                    class="w-full rounded-xl border border-dashed border-blue-300 bg-white py-3 text-sm font-black text-blue-700 hover:bg-blue-50"
                    @click.prevent="addAttendanceLocation"
                >
                  + 新增打卡地点
                </el-button>
              </div>

              <label class="flex items-center justify-between p-4 rounded-xl border border-slate-200 bg-white cursor-pointer hover:bg-slate-50 transition-colors shadow-sm">
                <div>
                  <div class="text-sm font-bold text-slate-800">Wi-Fi 验证</div>
                  <div class="text-xs text-slate-500 mt-0.5">必须连接到公司指定的内网 Wi-Fi</div>
                </div>
                <el-checkbox v-model="ruleForm.enableWifi">启用</el-checkbox>
              </label>
              <div v-if="ruleForm.enableWifi" class="space-y-1">
                <label class="block text-xs font-bold text-slate-500">Wi-Fi 名称</label>
                <el-input v-model.trim="ruleForm.wifiSsid" placeholder="请输入 Wi-Fi 名称（可选）" />
              </div>
            </div>
          </section>

        </div>

        <div class="px-8 py-4 border-t border-slate-200/50 bg-white flex justify-end gap-3 shrink-0">
          <el-button @click="ruleDrawerVisible = false" class="px-5 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors">取消</el-button>
          <el-button :disabled="!canUpdateRule || ruleLoading || !!ruleError || ruleEmpty || ruleSubmitting" :loading="ruleSubmitting" @click="submitRule" class="px-5 py-2 text-sm font-bold bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-md shadow-blue-600/20 active:scale-95">保存配置</el-button>
        </div>

      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDatePicker,
  ElDrawer,
  ElEmpty,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTimePicker
} from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { createLatestRequest, createSubmitGuard } from '@/utils/latestRequest'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import {
  exportAttendanceExcel,
  getAttendanceDepartments,
  getAttendancePage,
  getAttendanceRule,
  getAttendanceSummary,
  saveAttendanceRule
} from './api/attendance.js'

const route = useRoute()
const userStore = useUserStore()
const attendanceRequest = createLatestRequest()
const ruleRequest = createLatestRequest()
const ruleSubmitGuard = createSubmitGuard()
const defaultAttendanceTableColumns = [
  { key: 'employee', label: '员工' },
  { key: 'empNo', label: '工号' },
  { key: 'department', label: '部门' },
  { key: 'signIn', label: '上班打卡' },
  { key: 'signOut', label: '下班打卡' },
  { key: 'status', label: '状态' },
  { key: 'updateTime', label: '更新时间' }
]
const {
  orderedColumns: attendanceTableColumns,
  moveColumn: moveAttendanceTableColumn,
  resetColumns: resetAttendanceTableColumns
} = useLocalTableColumns('attendance.record.list', defaultAttendanceTableColumns)
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
  locations: [],
  enableWifi: false,
  wifiSsid: ''
})

const today = new Date().toISOString().slice(0, 10)
const rows = ref([])
const departments = ref([])
const loading = ref(false)
const listError = ref(null)
const summaryLoading = ref(false)
const summaryError = ref(null)
const summaryEmpty = ref(false)
const ruleLoading = ref(false)
const ruleError = ref(null)
const ruleEmpty = ref(false)
const ruleSubmitting = ref(false)
const summary = reactive({ totalEmployeeCount: 0, actualCount: 0, lateCount: 0, earlyCount: 0, missingCount: 0, attendanceRate: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', departmentName: '', status: '', date: today })
const canReadRecords = computed(() => userStore.hasPermission('attendance:record:list'))
const canReadRule = computed(() => userStore.hasPermission('attendance:rule:list'))
const canUpdateRule = computed(() => userStore.hasPermission('attendance:rule:update'))

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
  const request = attendanceRequest.begin()
  if (!canReadRecords.value) {
    const permissionError = attendanceRecordPermissionError()
    rows.value = []
    departments.value = []
    pagination.total = 0
    pagination.pages = 0
    listError.value = permissionError
    summaryError.value = permissionError
    loading.value = false
    summaryLoading.value = false
    return
  }
  const snapshot = currentQuerySnapshot()
  await Promise.all([fetchSummary(snapshot, request), fetchData(snapshot, request), fetchDepartments()])
}

function currentQuerySnapshot() {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    departmentName: query.departmentName || undefined,
    status: query.status || undefined,
    date: query.date || undefined
  }
}

async function fetchSummary(snapshot = currentQuerySnapshot(), request = attendanceRequest.begin()) {
  if (!canReadRecords.value) return
  summaryLoading.value = true
  summaryError.value = null
  summaryEmpty.value = false
  try {
    const data = await getAttendanceSummary({ date: snapshot.date })
    request.commit(() => {
      Object.assign(summary, data || {})
      summaryEmpty.value = !data || Object.keys(data).length === 0
    })
  } catch (error) {
    request.commit(() => {
      Object.assign(summary, { totalEmployeeCount: 0, actualCount: 0, lateCount: 0, earlyCount: 0, missingCount: 0, attendanceRate: 0 })
      summaryError.value = resolveListError(error)
    })
  } finally {
    request.commit(() => { summaryLoading.value = false })
  }
}

function resolveListError(error) {
  const status = Number(error?.response?.status ?? error?.status ?? error?.code ?? 0)
  if (status === 401 || status === 403) {
    return {
      type: 'permission',
      title: status === 401 ? '登录状态已失效' : '暂无考勤记录权限',
      message: status === 401 ? '请重新登录后查看考勤记录。' : '请联系管理员分配考勤记录权限后重试。'
    }
  }
  return {
    type: 'request',
    title: status >= 500 ? '考勤服务暂时不可用' : '网络连接异常',
    message: status >= 500 ? '服务处理失败，请稍后重新加载。' : '请检查网络连接后重新加载考勤记录。'
  }
}

function attendanceRecordPermissionError() {
  return {
    type: 'permission',
    title: '暂无考勤记录权限',
    message: '请联系管理员分配 attendance:record:list 权限后重试。'
  }
}

async function fetchData(snapshot = currentQuerySnapshot(), request = attendanceRequest.begin()) {
  if (!canReadRecords.value) return
  loading.value = true
  listError.value = null
  rows.value = []
  pagination.total = 0
  pagination.pages = 0
  try {
    const data = await getAttendancePage(snapshot)
    request.commit(() => {
      rows.value = data.data || []
      pagination.total = Number(data.total || 0)
      pagination.pages = Number(data.pages || 0)
    })
  } catch (error) {
    request.commit(() => {
      rows.value = []
      pagination.total = 0
      pagination.pages = 0
      listError.value = resolveListError(error)
    })
  } finally {
    request.commit(() => { loading.value = false })
  }
}

async function fetchDepartments() {
  if (!canReadRecords.value) return
  departments.value = await getAttendanceDepartments()
}

function createDefaultLocation(source = {}) {
  return {
    id: source.id || undefined,
    locationName: source.locationName || source.address || '公司打卡点',
    latitude: source.latitude ?? undefined,
    longitude: source.longitude ?? undefined,
    radius: Number(source.radius || 300),
    address: source.address || ''
  }
}

function normalizeRuleLocations(data = {}) {
  const locations = Array.isArray(data.locations) ? data.locations.filter(Boolean) : []
  if (locations.length > 0) {
    return locations.map((item) => createDefaultLocation(item))
  }
  if (data.latitude !== undefined && data.latitude !== null && data.longitude !== undefined && data.longitude !== null) {
    return [createDefaultLocation(data)]
  }
  return [createDefaultLocation()]
}

function addAttendanceLocation() {
  ruleForm.locations.push(createDefaultLocation({ locationName: `打卡点${ruleForm.locations.length + 1}` }))
}

function removeAttendanceLocation(index) {
  if (!Array.isArray(ruleForm.locations)) {
    ruleForm.locations = []
  }
  if (ruleForm.locations.length <= 1) {
    if (ruleForm.locations.length === 0) {
      ruleForm.locations.push(createDefaultLocation())
    } else {
      Object.assign(ruleForm.locations[0], createDefaultLocation())
    }
    return
  }
  ruleForm.locations.splice(index, 1)
}

function normalizeLocationPayload() {
  return (ruleForm.locations || [])
    .map((item) => ({
      id: item.id || undefined,
      locationName: String(item.locationName || item.address || '公司打卡点').trim(),
      latitude: item.latitude === '' || item.latitude == null ? undefined : Number(item.latitude),
      longitude: item.longitude === '' || item.longitude == null ? undefined : Number(item.longitude),
      radius: item.radius === '' || item.radius == null ? 300 : Number(item.radius),
      address: String(item.address || '').trim()
    }))
}

async function openRuleDrawer() {
  if (!canReadRule.value) {
    ElMessage.warning('当前账号暂无考勤规则查看权限')
    return
  }
  ruleDrawerVisible.value = true
  await loadRule()
}

async function loadRule() {
  if (!canReadRule.value) return
  const request = ruleRequest.begin()
  ruleLoading.value = true
  ruleError.value = null
  ruleEmpty.value = false
  try {
  const data = await getAttendanceRule()
  if (!data || Object.keys(data).length === 0) {
    request.commit(() => { ruleEmpty.value = true })
    return
  }
  const locations = normalizeRuleLocations(data)
  const firstLocation = locations[0] || createDefaultLocation()
  Object.assign(ruleForm, {
    ...data,
    workDays: Array.isArray(data.workDays) && data.workDays.length ? data.workDays : [1, 2, 3, 4, 5],
    enableGps: data.enableGps !== false,
    enableWifi: data.enableWifi === true,
    latitude: firstLocation.latitude,
    longitude: firstLocation.longitude,
    radius: firstLocation.radius || 300,
    address: firstLocation.address || firstLocation.locationName || '',
    locations
  })
  } catch (error) {
    request.commit(() => { ruleError.value = resolveListError(error) })
  } finally {
    request.commit(() => { ruleLoading.value = false })
  }
}

function initializeDefaultRule() {
  Object.assign(ruleForm, { locations: [createDefaultLocation()] })
  ruleEmpty.value = false
}

async function submitRule() {
  if (!canUpdateRule.value) {
    ElMessage.warning('当前账号暂无考勤规则保存权限')
    return
  }
  if (ruleSubmitGuard.pending) return
  if (!ruleForm.workDays.length) {
    ElMessage.warning('请至少选择一个工作日')
    return
  }
  const locations = ruleForm.enableGps ? normalizeLocationPayload() : []
  if (ruleForm.enableGps && locations.length === 0) {
    ElMessage.warning('启用 GPS 围栏时，请至少填写一个有效打卡点')
    return
  }
  if (ruleForm.enableGps && locations.some((item) => !Number.isFinite(item.latitude) || !Number.isFinite(item.longitude) || Number(item.radius) <= 0)) {
    ElMessage.warning('请检查每个打卡点的经纬度和允许半径')
    return
  }
  const firstLocation = locations[0] || {}
  ruleSubmitting.value = true
  await ruleSubmitGuard.run(async () => {
  await saveAttendanceRule({
    ...ruleForm,
    lateToleranceMinutes: Number(ruleForm.lateToleranceMinutes || 0),
    earlyToleranceMinutes: Number(ruleForm.earlyToleranceMinutes || 0),
    latitude: firstLocation.latitude,
    longitude: firstLocation.longitude,
    radius: firstLocation.radius,
    address: firstLocation.address || firstLocation.locationName,
    locations
  })
  ElMessage.success('考勤规则已保存，小程序打卡规则已同步')
  ruleDrawerVisible.value = false
  }).finally(() => { ruleSubmitting.value = false })
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
  refreshAll()
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
