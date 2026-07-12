<template>
  <div class="min-h-fit max-w-7xl mx-auto space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-bold tracking-[0.2em] text-primary/70 uppercase">经营总览</p>
        <h1 class="mt-2 text-3xl md:text-4xl font-black tracking-tight text-on-surface">
          {{ greetingText }}，{{ userName }}
        </h1>
        <p class="mt-2 text-base text-on-surface-variant max-w-2xl">
          预警订单 <span class="font-bold text-primary">{{ summary.orderWarningCount }}</span> 单，
          待我审批 <span class="font-bold text-primary">{{ summary.pendingApprovalCount }}</span> 项。
        </p>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3 w-full lg:w-auto lg:min-w-[500px]">
        <el-button
            v-for="action in quickActions"
            :key="action.route"
            @click="openQuickAction(action)"
            plain
            class="h-auto justify-start px-4 py-3.5 text-left"
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
        </el-button>
      </div>
    </section>

    <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <article class="rounded-2xl p-5 bg-primary text-white shadow-md shadow-primary/20 overflow-hidden relative flex flex-col justify-between min-h-[120px]">
        <div class="absolute -right-2 -top-2 text-white/10">
          <span class="material-symbols-outlined text-[90px]">receipt_long</span>
        </div>
        <p class="text-xs font-bold tracking-widest uppercase text-white/80 z-10">本月新增订单</p>
        <div class="mt-auto z-10">
          <p class="text-4xl font-black leading-none">{{ summary.monthOrderCount }}</p>
          <p class="text-xs text-white/70 mt-1.5 truncate">本月创建的订单总数</p>
        </div>
      </article>

      <article class="rounded-2xl p-5 bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 flex flex-col justify-between min-h-[120px]">
        <div class="flex items-center justify-between mb-2">
          <p class="text-xs font-bold tracking-widest uppercase text-on-surface-variant">预警订单</p>
          <div class="w-11 h-11 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <span class="material-symbols-outlined text-[30px] leading-none">warning</span>
          </div>
        </div>
        <div class="mt-auto">
          <p class="text-4xl font-black text-on-surface leading-none">{{ summary.orderWarningCount }}</p>
          <p class="text-xs text-on-surface-variant mt-1.5 truncate">超过预警天数未更新</p>
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
          <p class="text-xs text-on-surface-variant mt-1.5 truncate">等待当前账号处理</p>
        </div>
      </article>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[minmax(0,1.2fr)_minmax(350px,1fr)] gap-5">
      <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 xl:col-span-2">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-lg font-black text-on-surface leading-tight">企业通知公告</h2>
            <p class="text-xs text-on-surface-variant mt-1">展示少量普通公告和紧急公告，帮助团队快速同步日常安排。</p>
          </div>
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-1 rounded-md text-xs font-bold bg-primary/10 text-primary">{{ announcements.length }} 条</span>
            <el-button
                v-permission="'notification:announcement:publish'"
                @click="openAnnouncementPublish"
                type="primary"
                size="small"
            >
              发布公告
            </el-button>
          </div>
        </div>

        <div class="grid min-h-32 grid-cols-1 gap-4 lg:grid-cols-2">
          <div v-if="announcementLoading" v-loading="true" class="min-h-32 lg:col-span-2" element-loading-text="正在同步企业通知公告"></div>
          <div v-else-if="announcementLoadError" class="flex min-h-32 flex-col items-center justify-center px-4 text-center lg:col-span-2">
            <span class="material-symbols-outlined text-4xl text-error/70">{{ announcementLoadError.kind === 'permission' ? 'lock' : 'cloud_off' }}</span>
            <p class="mt-2 text-sm font-black text-on-surface">{{ announcementLoadError.title }}</p>
            <p class="mt-1 text-xs leading-5 text-on-surface-variant">{{ announcementLoadError.message }}</p>
            <el-button class="mt-3" size="small" type="primary" plain @click="fetchAnnouncements">重试</el-button>
          </div>
          <el-empty v-else-if="announcementsLoaded && !announcements.length" class="lg:col-span-2" description="暂无企业通知公告" />
          <template v-else>
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
          </template>
        </div>
      </article>

      <article class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-5 flex flex-col">
        <div class="flex items-center justify-between gap-3 mb-4">
          <div>
            <h2 class="text-lg font-black text-on-surface leading-tight">重要公告</h2>
            <p class="text-xs text-on-surface-variant mt-1">集中展示更多需要重点关注的公告。</p>
          </div>
          <el-button link type="primary" @click="openAnnouncementCenter">
            查看全部
          </el-button>
        </div>

        <div v-if="announcementLoading" v-loading="true" class="min-h-44" element-loading-text="正在同步重要公告"></div>
        <div v-else-if="importantAnnouncementLoadError" class="flex min-h-44 flex-col items-center justify-center px-4 text-center">
          <span class="material-symbols-outlined text-4xl text-error/70">{{ importantAnnouncementLoadError.kind === 'permission' ? 'lock' : 'cloud_off' }}</span>
          <p class="mt-2 text-sm font-black text-on-surface">{{ importantAnnouncementLoadError.title }}</p>
          <p class="mt-1 text-xs leading-5 text-on-surface-variant">{{ importantAnnouncementLoadError.message }}</p>
          <el-button class="mt-3" size="small" type="primary" plain @click="fetchAnnouncements">重试</el-button>
        </div>
        <el-empty v-else-if="importantAnnouncementsLoaded && !importantAnnouncements.length" class="min-h-44" description="暂无重要公告" />
        <div v-else class="space-y-3 flex-1 overflow-y-auto pr-1 no-scrollbar max-h-[260px]">
          <div
              v-for="item in importantAnnouncements"
              :key="item.id || `${item.title}-${item.updateTime}`"
              class="rounded-2xl p-4 border cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-md"
              :class="announcementCardClass(item.level)"
              @click="openAnnouncementCenter"
          >
            <div class="flex items-start gap-3">
              <div class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0" :class="announcementIconClass(item.level)">
                <span class="material-symbols-outlined text-[24px] leading-none">priority_high</span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2">
                  <p class="text-sm font-black truncate">{{ item.title }}</p>
                  <span class="text-[10px] font-bold uppercase tracking-widest opacity-70 shrink-0">{{ announcementLevelText(item.level) }}</span>
                </div>
                <p class="mt-2 text-sm leading-6 opacity-90 line-clamp-3">{{ item.content }}</p>
                <p class="mt-3 text-xs leading-6 font-medium opacity-70">{{ formatAnnouncementTime(item.updateTime) }}</p>
              </div>
            </div>
          </div>
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
import { ElButton, ElEmpty, ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardOverview } from './api/dashboard.js'
import { getAnnouncements } from '@/api/notification.js'

defineOptions({ name: 'DashboardOverview' })

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const announcementLoading = ref(false)
const announcementLoadError = ref(null)
const importantAnnouncementLoadError = ref(null)
const announcementsLoaded = ref(false)
const importantAnnouncementsLoaded = ref(false)
let announcementRequestId = 0
const summary = ref({
  monthOrderCount: 0,
  totalInventoryMeters: 0,
  pendingApprovalCount: 0,
  pendingPrintCount: 0,
  inventoryWarningCount: 0,
  orderWarningCount: 0
})
const visibility = ref({
  orderVisible: false,
  inventoryVisible: false,
  approvalVisible: false,
  receiptVisible: false,
  attendanceVisible: false
})
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
const importantAnnouncements = ref([])

const userName = computed(() => userStore.userInfo?.userName || '当前用户')

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

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

function resolveLoadFailure(error, resourceLabel) {
  const status = Number(error?.response?.status || error?.status || error?.code || 0)
  const serverMessage = error?.response?.data?.msg || error?.response?.data?.message || error?.msg || error?.message
  if (status === 401) {
    return {
      kind: 'authentication',
      title: '登录状态已失效',
      message: '请重新登录后再查看公告。'
    }
  }
  if (status === 403) {
    return {
      kind: 'permission',
      title: '暂无公告查看权限',
      message: serverMessage || `当前账号无权加载${resourceLabel}，请联系管理员确认权限。`
    }
  }
  if (status >= 500) {
    return {
      kind: 'server',
      title: '公告服务暂不可用',
      message: serverMessage || `${resourceLabel}加载失败，请稍后重试。`
    }
  }
  if (!status && !error?.response) {
    return {
      kind: 'network',
      title: '网络连接异常',
      message: '无法连接公告服务，请检查网络后重试。'
    }
  }
  return {
    kind: 'server',
    title: '公告加载失败',
    message: serverMessage || `${resourceLabel}加载失败，请稍后重试。`
  }
}

async function fetchAnnouncements() {
  if (announcementLoading.value) {
    return
  }
  const requestId = ++announcementRequestId
  announcementLoading.value = true
  announcements.value = []
  importantAnnouncements.value = []
  announcementLoadError.value = null
  importantAnnouncementLoadError.value = null
  announcementsLoaded.value = false
  importantAnnouncementsLoaded.value = false
  try {
    const [dailyResult, importantResult] = await Promise.allSettled([
      getAnnouncements({ limit: 4, levels: 'normal,urgent' }),
      getAnnouncements({ limit: 8, levels: 'important' })
    ])
    if (requestId !== announcementRequestId) {
      return
    }
    if (dailyResult.status === 'fulfilled') {
      announcements.value = Array.isArray(dailyResult.value) ? dailyResult.value : []
      announcementsLoaded.value = true
    } else {
      announcementLoadError.value = resolveLoadFailure(dailyResult.reason, '企业通知公告')
    }
    if (importantResult.status === 'fulfilled') {
      importantAnnouncements.value = Array.isArray(importantResult.value) ? importantResult.value : []
      importantAnnouncementsLoaded.value = true
    } else {
      importantAnnouncementLoadError.value = resolveLoadFailure(importantResult.reason, '重要公告')
    }
  } finally {
    if (requestId === announcementRequestId) {
      announcementLoading.value = false
    }
  }
}

function openQuickAction(action) {
  router.push(action.route)
}

function openAnnouncementCenter() {
  router.push('/function/announcement')
}

function openAnnouncementPublish() {
  router.push('/function/announcement/publish')
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
  if (level === 'urgent' || level === 'critical') {
    return 'border-rose-200 bg-rose-50/60 text-rose-950'
  }
  if (level === 'important' || level === 'warning') {
    return 'border-amber-200 bg-amber-50/60 text-amber-950'
  }
  return 'border-sky-200 bg-sky-50/60 text-sky-950'
}

const announcementIconClass = (level) => {
  if (level === 'urgent' || level === 'critical') {
    return 'bg-rose-100 text-rose-700'
  }
  if (level === 'important' || level === 'warning') {
    return 'bg-amber-100 text-amber-700'
  }
  return 'bg-sky-100 text-sky-700'
}

const announcementLevelText = (level) => {
  if (level === 'urgent' || level === 'critical') return '紧急公告'
  if (level === 'important' || level === 'warning') return '重要公告'
  return '普通公告'
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
