<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-bold tracking-[0.2em] text-primary/70 uppercase">经营总览</p>
        <h1 class="mt-2 text-3xl md:text-4xl font-black tracking-tight text-on-surface">
          {{ greetingText }}，{{ userName }}
        </h1>
        <p class="mt-2 text-base text-on-surface-variant max-w-2xl">
          待处理审批 <span class="font-bold text-primary">{{ summary.pendingApprovalCount }}</span> 项，
          待打印出库单 <span class="font-bold text-primary">{{ summary.pendingPrintCount }}</span> 单。
        </p>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3 w-full lg:w-auto lg:min-w-[500px]">
        <button
            v-for="action in quickActions"
            :key="action.route"
            @click="openQuickAction(action)"
            class="text-left rounded-xl bg-surface-container-lowest px-4 py-3.5 shadow-sm ring-1 ring-outline-variant/20 hover:shadow-md hover:-translate-y-0.5 transition-all group"
        >
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary transition-colors">
              <span class="material-symbols-outlined text-[30px] text-primary group-hover:text-white leading-none transition-colors">
                {{ action.icon }}
              </span>
            </div>
            <div class="min-w-0">
              <p class="text-sm font-bold text-on-surface truncate">{{ action.title }}</p>
              <p class="text-xs text-on-surface-variant truncate mt-1">{{ action.description }}</p>
            </div>
          </div>
        </button>
      </div>
    </section>

    <section class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <article class="rounded-2xl p-5 bg-primary text-white shadow-md shadow-primary/20 overflow-hidden relative flex flex-col justify-between min-h-[120px]">
        <div class="absolute -right-2 -top-2 text-white/10">
          <span class="material-symbols-outlined text-[90px]">receipt_long</span>
        </div>
        <p class="text-xs font-bold tracking-widest uppercase text-white/80 z-10">本月新增订单</p>
        <div class="mt-auto z-10">
          <p class="text-4xl font-black leading-none">{{ summary.monthOrderCount }}</p>
          <p class="text-xs text-white/70 mt-1.5 truncate">销售单与生产单合并</p>
        </div>
      </article>

      <article class="rounded-2xl p-5 bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 flex flex-col justify-between min-h-[120px]">
        <div class="flex items-center justify-between mb-2">
          <p class="text-xs font-bold tracking-widest uppercase text-on-surface-variant">库存预警项</p>
          <div class="w-11 h-11 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <span class="material-symbols-outlined text-[30px] leading-none">warning</span>
          </div>
        </div>
        <div class="mt-auto">
          <p class="text-4xl font-black text-on-surface leading-none">{{ summary.inventoryWarningCount }}</p>
          <p class="text-xs text-on-surface-variant mt-1.5 truncate">低于安全阈值的型号</p>
        </div>
      </article>

      <article class="rounded-2xl p-5 bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 flex flex-col justify-between min-h-[120px]">
        <div class="flex items-center justify-between mb-2">
          <p class="text-xs font-bold tracking-widest uppercase text-on-surface-variant">待我审批</p>
          <div class="w-11 h-11 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <span class="material-symbols-outlined text-[30px] leading-none">fact_check</span>
          </div>
        </div>
        <div class="mt-auto">
          <p class="text-4xl font-black text-on-surface leading-none">{{ summary.pendingApprovalCount }}</p>
          <p class="text-xs text-on-surface-variant mt-1.5 truncate">请假与财务审批</p>
        </div>
      </article>

      <article class="rounded-2xl p-5 bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 flex flex-col justify-between min-h-[120px]">
        <div class="flex items-center justify-between mb-2">
          <p class="text-xs font-bold tracking-widest uppercase text-on-surface-variant">待打出库单</p>
          <div class="w-11 h-11 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <span class="material-symbols-outlined text-[30px] leading-none">print</span>
          </div>
        </div>
        <div class="mt-auto">
          <p class="text-4xl font-black text-on-surface leading-none">{{ summary.pendingPrintCount }}</p>
          <p class="text-xs text-on-surface-variant mt-1.5 truncate">已提交待打印</p>
        </div>
      </article>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[minmax(0,1.2fr)_minmax(350px,1fr)] gap-5">
      <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 xl:col-span-2">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-lg font-black text-on-surface leading-tight">企业通知公告</h2>
            <p class="text-xs text-on-surface-variant mt-1">展示公司最新通知、制度公告和业务提醒，帮助团队统一节奏。</p>
          </div>
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-1 rounded-md text-xs font-bold bg-primary/10 text-primary">{{ announcements.length }} 条</span>
            <button @click="openAnnouncementCenter" class="px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-bold hover:bg-primary/90">
              发布通知
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div
              v-if="announcementLoading && !announcements.length"
              class="lg:col-span-2 rounded-2xl border border-dashed border-primary/25 bg-primary/5 px-4 py-6 text-sm font-bold text-primary flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined animate-spin text-[22px]">progress_activity</span>
            正在同步企业通知公告
          </div>
          <div
              v-else-if="!announcements.length"
              class="lg:col-span-2 rounded-2xl border border-dashed border-outline-variant/40 bg-surface-container-low px-4 py-6 text-sm font-bold text-on-surface-variant text-center"
          >
            暂无企业通知公告
          </div>
          <div
              v-for="item in announcements"
              :key="item.id || `${item.title}-${item.updateTime}`"
              class="rounded-2xl p-4 border cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-md"
              :class="announcementCardClass(item.level)"
              @click="openAnnouncementCenter"
          >
            <div class="flex items-start gap-3">
              <div class="w-11 h-11 rounded-xl flex items-center justify-center shrink-0" :class="announcementIconClass(item.level)">
                <span class="material-symbols-outlined text-[26px] leading-none">
                  campaign
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2">
                  <p class="text-sm font-black truncate">{{ item.title }}</p>
                  <span class="text-[10px] font-bold uppercase tracking-widest opacity-70 shrink-0">{{ announcementLevelText(item.level) }}</span>
                </div>
                <p class="mt-2 text-sm leading-6 opacity-90 line-clamp-2">{{ item.content }}</p>
                <p class="mt-3 text-xs leading-6 font-medium opacity-70">{{ formatAnnouncementTime(item.updateTime) }}</p>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-lg font-black text-on-surface leading-tight">近 7 日出入库趋势</h2>
            <p class="text-xs text-on-surface-variant mt-1">按日汇总出入库米数</p>
          </div>
          <div class="flex items-center gap-4 text-xs font-bold">
            <span class="inline-flex items-center gap-1.5 text-primary">
              <span class="w-2.5 h-2.5 rounded-full bg-primary"></span>入库
            </span>
            <span class="inline-flex items-center gap-1.5 text-emerald-600">
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>出库
            </span>
          </div>
        </div>

        <div v-if="!visibility.trendVisible" class="flex-1 rounded-xl bg-surface-container-low flex items-center justify-center text-sm text-on-surface-variant min-h-[160px]">
          暂无查看权限
        </div>
        <div v-else-if="chartReady" class="flex-1 flex flex-col gap-3">
          <div class="h-40 rounded-xl bg-[radial-gradient(circle_at_top,_rgba(25,118,210,0.06),_transparent_60%)] border border-outline-variant/20 p-3 relative">
            <svg viewBox="0 0 100 56" preserveAspectRatio="none" class="w-full h-full overflow-visible">
              <g v-for="grid in [0, 25, 50, 75, 100]" :key="grid">
                <line
                    x1="0"
                    :x2="100"
                    :y1="56 - (grid * 0.56)"
                    :y2="56 - (grid * 0.56)"
                    stroke="rgba(120, 144, 156, 0.15)"
                    stroke-width="0.3"
                />
              </g>
              <polyline
                  :points="inLinePoints"
                  fill="none"
                  stroke="rgb(25,118,210)"
                  stroke-width="1.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
              />
              <polyline
                  :points="outLinePoints"
                  fill="none"
                  stroke="rgb(16,185,129)"
                  stroke-width="1.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
              />
              <g v-for="(point, index) in chartPoints" :key="`point-${index}`">
                <circle :cx="point.x" :cy="point.inY" r="1.5" fill="rgb(25,118,210)" />
                <circle :cx="point.x" :cy="point.outY" r="1.5" fill="rgb(16,185,129)" />
              </g>
            </svg>
          </div>

          <div class="grid grid-cols-7 gap-1.5 text-center">
            <div v-for="(date, index) in trendDates" :key="date" class="rounded-lg bg-surface-container-low px-1 py-2">
              <p class="text-[11px] font-bold text-on-surface-variant origin-bottom">{{ date }}</p>
              <p class="mt-1 text-sm font-black text-primary leading-tight">{{ formatTrendValue(trendInMeters[index]) }}</p>
              <p class="text-[11px] font-medium text-emerald-600 leading-none mt-1">{{ formatTrendValue(trendOutMeters[index]) }}</p>
            </div>
          </div>
        </div>
        <div v-else class="flex-1 rounded-xl bg-surface-container-low flex items-center justify-center text-sm text-on-surface-variant min-h-[160px]">
          暂无数据
        </div>
      </article>

      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-1 gap-5">
        <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 flex flex-col">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-black text-on-surface">业务提醒</h2>
            <span class="px-2.5 py-1 rounded-md text-xs font-bold bg-primary/10 text-primary">
              {{ businessAlerts.length }} 条
            </span>
          </div>

          <div v-if="businessAlerts.length" class="space-y-3 flex-1 overflow-y-auto pr-1 no-scrollbar max-h-[180px] xl:max-h-[220px]">
            <div
                v-for="(item, index) in businessAlerts"
                :key="`${item.type}-${index}`"
                :class="alertCardClass(item.level)"
                class="rounded-xl p-3.5 border-l-4"
            >
              <div class="flex items-center justify-between gap-3 mb-1.5">
                <p class="text-sm font-bold truncate">{{ item.title }}</p>
                <span class="text-xs font-medium opacity-70 shrink-0">{{ item.time }}</span>
              </div>
              <p class="text-xs leading-relaxed opacity-90 line-clamp-2">{{ item.content }}</p>
            </div>
          </div>
          <div v-else class="flex-1 rounded-xl bg-surface-container-low flex items-center justify-center text-sm text-on-surface-variant min-h-[120px]">
            暂无提醒
          </div>
        </article>

        <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 flex flex-col">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-black text-on-surface">今日考勤异常</h2>
            <span :class="attendanceBadgeClass" class="px-2.5 py-1 rounded-md text-xs font-bold">
              {{ attendanceSummary.abnormalCount || 0 }} 人
            </span>
          </div>

          <div v-if="visibility.attendanceVisible && attendanceAlerts.length" class="space-y-3 flex-1 overflow-y-auto pr-1 no-scrollbar max-h-[180px] xl:max-h-[220px]">
            <div
                v-for="item in attendanceAlerts"
                :key="`${item.userId}-${item.time}`"
                class="flex items-center justify-between gap-3 rounded-xl bg-surface-container-low px-4 py-3"
            >
              <div class="min-w-0 flex-1">
                <div class="flex flex-col gap-0.5">
                  <p class="text-sm font-bold text-on-surface truncate">{{ item.userName }}</p>
                  <p class="text-xs text-on-surface-variant truncate">{{ item.departmentName }}</p>
                </div>
              </div>
              <div class="text-right shrink-0 flex flex-col items-end gap-1">
                <span class="inline-block px-2 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-600">
                  {{ item.statusText }}
                </span>
                <p class="text-xs text-on-surface-variant font-medium">{{ item.time }}</p>
              </div>
            </div>
          </div>
          <div v-else class="flex-1 rounded-xl bg-surface-container-low flex flex-col items-center justify-center text-sm text-on-surface-variant min-h-[120px] px-4 text-center">
            <p class="font-bold text-on-surface">{{ attendanceStatusText }}</p>
            <p v-if="visibility.attendanceVisible" class="mt-2 text-xs">
              应考勤 {{ attendanceSummary.totalEmployeeCount || 0 }} 人，已打卡 {{ attendanceSummary.actualCount || 0 }} 人
            </p>
          </div>
        </article>
      </div>
    </section>

    <div
        v-if="loading"
        class="fixed inset-0 bg-white/45 backdrop-blur-sm z-50 flex items-center justify-center"
    >
      <div class="rounded-2xl bg-white px-6 py-5 shadow-xl flex items-center gap-3 text-primary border border-surface-variant/20">
        <span class="material-symbols-outlined animate-spin text-[24px]">progress_activity</span>
        <span class="text-base font-bold">同步数据中...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardOverview } from './api/dashboard.js'
import { getAnnouncements } from '@/api/notification.js'
import { trackBehavior } from '@/utils/behavior'

defineOptions({ name: 'DashboardOverview' })

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const announcementLoading = ref(false)
const summary = ref({
  monthOrderCount: 0,
  totalInventoryMeters: 0,
  pendingApprovalCount: 0,
  pendingPrintCount: 0,
  inventoryWarningCount: 0
})
const visibility = ref({
  orderVisible: false,
  inventoryVisible: false,
  approvalVisible: false,
  receiptVisible: false,
  trendVisible: false,
  attendanceVisible: false,
  aiAdviceVisible: false
})
const trendDates = ref([])
const trendInMeters = ref([])
const trendOutMeters = ref([])
const businessAlerts = ref([])
const attendanceAlerts = ref([])
const attendanceSummary = ref({
  totalEmployeeCount: 0,
  actualCount: 0,
  abnormalCount: 0,
  statusText: '暂无考勤数据',
  statusType: 'empty'
})
const quickActions = ref([])
const announcements = ref([])

const userName = computed(() => userStore.userInfo?.userName || '管理员')

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const chartPoints = computed(() => {
  const values = [...trendInMeters.value, ...trendOutMeters.value].map((item) => Number(item || 0))
  const maxValue = Math.max(...values, 1)
  const count = Math.max(trendDates.value.length, 1)
  return trendDates.value.map((_, index) => {
    const x = count === 1 ? 50 : (index / (count - 1)) * 100
    const inValue = Number(trendInMeters.value[index] || 0)
    const outValue = Number(trendOutMeters.value[index] || 0)
    const inY = 56 - (inValue / maxValue) * 50
    const outY = 56 - (outValue / maxValue) * 50
    return {x, inY, outY}
  })
})

const inLinePoints = computed(() => chartPoints.value.map((item) => `${item.x},${item.inY}`).join(' '))
const outLinePoints = computed(() => chartPoints.value.map((item) => `${item.x},${item.outY}`).join(' '))
const chartReady = computed(() => chartPoints.value.length > 1)

const attendanceStatusText = computed(() => {
  if (!visibility.value.attendanceVisible) {
    return '暂无查看权限'
  }
  return attendanceSummary.value?.statusText || '暂无考勤数据'
})

const attendanceBadgeClass = computed(() => {
  const statusType = attendanceSummary.value?.statusType
  if (!visibility.value.attendanceVisible) {
    return 'bg-slate-100 text-slate-500'
  }
  if (statusType === 'warning') {
    return 'bg-rose-50 text-rose-700'
  }
  if (statusType === 'normal') {
    return 'bg-emerald-50 text-emerald-700'
  }
  if (statusType === 'waiting') {
    return 'bg-amber-50 text-amber-700'
  }
  return 'bg-slate-100 text-slate-500'
})

const fetchOverview = async () => {
  loading.value = true
  try {
    const data = await getDashboardOverview()
    summary.value = data?.summary || summary.value
    visibility.value = data?.visibility || visibility.value
    trendDates.value = Array.isArray(data?.trendDates) ? data.trendDates : []
    trendInMeters.value = Array.isArray(data?.trendInMeters) ? data.trendInMeters : []
    trendOutMeters.value = Array.isArray(data?.trendOutMeters) ? data.trendOutMeters : []
    businessAlerts.value = Array.isArray(data?.businessAlerts) ? data.businessAlerts : []
    attendanceAlerts.value = Array.isArray(data?.attendanceAlerts) ? data.attendanceAlerts : []
    attendanceSummary.value = data?.attendanceSummary || attendanceSummary.value
    quickActions.value = Array.isArray(data?.quickActions) ? data.quickActions : []
    fetchAnnouncements()
  } catch (error) {
    ElMessage.error(error?.msg || '总览数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function fetchAnnouncements() {
  if (announcementLoading.value) {
    return
  }
  announcementLoading.value = true
  try {
    const data = await getAnnouncements({ limit: 4 })
    announcements.value = Array.isArray(data) ? data : []
  } catch (error) {
    announcements.value = []
  } finally {
    announcementLoading.value = false
  }
}

function openQuickAction(action) {
  trackBehavior({
    eventType: 'quick_action_click',
    pagePath: '/dashboard',
    module: 'dashboard',
    targetType: 'quick_action',
    targetId: action.route,
    action: 'click',
    source: 'dashboard',
    metadata: {
      title: action.title,
      route: action.route
    }
  })
  router.push(action.route)
}

function openAnnouncementCenter() {
  trackBehavior({
    eventType: 'announcement_center_click',
    pagePath: '/dashboard',
    module: 'notification',
    targetType: 'route',
    targetId: '/function/announcement',
    action: 'click',
    source: 'dashboard'
  })
  router.push('/function/announcement')
}

const formatTrendValue = (value) => {
  const num = Number(value || 0);
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`;
  return `${num.toLocaleString('zh-CN', {maximumFractionDigits: 0})}`;
}

const alertCardClass = (level) => {
  if (level === 'warning') {
    return 'bg-amber-50/50 border-amber-300 text-amber-900'
  }
  if (level === 'error') {
    return 'bg-rose-50/50 border-rose-300 text-rose-900'
  }
  return 'bg-sky-50/50 border-sky-300 text-sky-900'
}

const announcementCardClass = (level) => {
  if (level === 'warning') {
    return 'border-amber-200 bg-amber-50/60 text-amber-950'
  }
  if (level === 'critical') {
    return 'border-rose-200 bg-rose-50/60 text-rose-950'
  }
  return 'border-sky-200 bg-sky-50/60 text-sky-950'
}

const announcementIconClass = (level) => {
  if (level === 'warning') {
    return 'bg-amber-100 text-amber-700'
  }
  if (level === 'critical') {
    return 'bg-rose-100 text-rose-700'
  }
  return 'bg-sky-100 text-sky-700'
}

const announcementLevelText = (level) => {
  if (level === 'critical') return '重要'
  if (level === 'warning') return '提醒'
  return '公告'
}

const formatAnnouncementTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(fetchOverview)
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
