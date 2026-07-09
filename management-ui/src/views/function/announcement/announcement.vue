<template>
  <div class="min-h-fit max-w-7xl mx-auto space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-xs font-black tracking-[0.18em] text-primary">
          <span class="material-symbols-outlined text-[18px]">campaign</span>
          企业通知公告
        </p>
        <h1 class="mt-4 text-3xl md:text-4xl font-black tracking-tight text-on-surface">
          公告查看
        </h1>
        <p class="mt-2 text-base text-on-surface-variant max-w-2xl">
          查看当前组织发布的普通公告、紧急公告和重要公告，并同步展示员工已读未读情况。
        </p>
      </div>
      <button
          class="rounded-2xl bg-primary px-6 py-4 text-sm font-black text-white shadow-lg shadow-primary/20 transition"
          :class="canPublishAnnouncement ? 'hover:bg-primary/90' : 'cursor-not-allowed opacity-50 grayscale'"
          :disabled="!canPublishAnnouncement"
          :title="canPublishAnnouncement ? '发布公告' : '当前账号暂无发布公告权限'"
          @click="goPublish"
      >
        发布公告
      </button>
    </section>

    <section class="rounded-3xl border border-outline-variant/20 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap gap-2">
          <button
              v-for="item in levelTabs"
              :key="item.value"
              class="rounded-full border px-4 py-2 text-sm font-black transition"
              :class="activeLevel === item.value ? 'border-primary bg-primary text-white shadow-md shadow-primary/15' : 'border-outline-variant/30 bg-surface-container-lowest text-on-surface-variant hover:border-primary/40 hover:text-primary'"
              @click="selectLevel(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <button class="rounded-xl bg-primary/10 px-4 py-2 text-xs font-black text-primary" @click="loadAnnouncements">
          刷新
        </button>
      </div>

      <div v-if="loading" class="mt-6 rounded-2xl border border-dashed border-primary/20 bg-primary/5 px-4 py-8 text-center text-sm font-bold text-primary">
        正在加载公告...
      </div>
      <div v-else-if="!announcements.length" class="mt-6 rounded-2xl border border-dashed border-outline-variant/30 bg-surface-container-low px-4 py-8 text-center text-sm font-bold text-on-surface-variant">
        暂无公告
      </div>
      <div v-else class="mt-5 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div
            v-for="item in announcements"
            :key="item.id || `${item.title}-${item.updateTime}`"
            class="rounded-2xl border p-4"
            :class="announcementCardClass(item.level)"
        >
          <div class="flex items-start justify-between gap-3">
            <h3 class="text-base font-black leading-6">{{ item.title }}</h3>
            <span class="shrink-0 rounded-full bg-white/70 px-2.5 py-1 text-[11px] font-black">
              {{ announcementLevelText(item.level) }}
            </span>
          </div>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-7 opacity-90">{{ item.content }}</p>
          <div class="mt-4 rounded-2xl border border-white/70 bg-white/55 p-3">
            <div class="flex flex-wrap items-center gap-2 text-xs font-black">
              <span class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-700">
                已读 {{ numberText(item.readCount) }}
              </span>
              <span class="rounded-full bg-amber-100 px-3 py-1 text-amber-700">
                未读 {{ numberText(item.unreadCount) }}
              </span>
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">
                共 {{ numberText(item.totalReceiverCount) }} 人
              </span>
            </div>
            <div v-if="receiverList(item).length" class="mt-3 max-h-36 overflow-y-auto pr-1">
              <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <div
                    v-for="receiver in receiverList(item)"
                    :key="receiver.userId"
                    class="flex items-center justify-between gap-2 rounded-xl border px-3 py-2 text-xs"
                    :class="receiverRead(receiver) ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-amber-200 bg-amber-50 text-amber-800'"
                >
                  <div class="min-w-0">
                    <p class="truncate font-black">{{ receiver.userName || '未命名员工' }}</p>
                    <p class="truncate opacity-70">{{ receiver.departmentName || '未分部门' }} · {{ receiver.positionName || '未设职位' }}</p>
                  </div>
                  <span class="shrink-0 rounded-full px-2 py-1 font-black" :class="receiverRead(receiver) ? 'bg-emerald-100' : 'bg-amber-100'">
                    {{ receiverRead(receiver) ? '已读' : '未读' }}
                  </span>
                </div>
              </div>
            </div>
            <p v-else class="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-xs font-bold text-slate-500">
              当前暂无可统计人员。
            </p>
          </div>
          <p class="mt-3 text-xs font-bold opacity-60">{{ formatTime(item.updateTime) }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnnouncements } from '@/api/notification.js'
import { useUserStore } from '@/stores/user.js'

defineOptions({ name: 'AnnouncementCenter' })

const ANNOUNCEMENT_PUBLISH_PERMISSION = 'notification:announcement:publish'
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const activeLevel = ref('all')
const announcements = ref([])
const canPublishAnnouncement = computed(() => userStore.hasPermission(ANNOUNCEMENT_PUBLISH_PERMISSION))

const levelTabs = [
  { value: 'all', label: '全部公告' },
  { value: 'normal', label: '普通公告' },
  { value: 'urgent', label: '紧急公告' },
  { value: 'important', label: '重要公告' }
]

async function loadAnnouncements() {
  loading.value = true
  try {
    const params = { limit: 30 }
    if (activeLevel.value !== 'all') {
      params.levels = activeLevel.value
    }
    const data = await getAnnouncements(params)
    announcements.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.msg || '公告加载失败')
  } finally {
    loading.value = false
  }
}

function selectLevel(level) {
  activeLevel.value = level
  loadAnnouncements()
}

function goPublish() {
  if (!canPublishAnnouncement.value) {
    ElMessage.warning('当前账号暂无发布公告权限')
    return
  }
  router.push('/function/announcement/publish')
}

const announcementCardClass = (level) => {
  if (level === 'urgent' || level === 'critical') {
    return 'border-rose-200 bg-rose-50/70 text-rose-950'
  }
  if (level === 'important' || level === 'warning') {
    return 'border-amber-200 bg-amber-50/70 text-amber-950'
  }
  return 'border-sky-200 bg-sky-50/70 text-sky-950'
}

const announcementLevelText = (level) => {
  if (level === 'urgent' || level === 'critical') return '紧急公告'
  if (level === 'important' || level === 'warning') return '重要公告'
  return '普通公告'
}

const receiverList = (item) => Array.isArray(item?.receivers) ? item.receivers : []

const receiverRead = (receiver) => Number(receiver?.readFlag) === 1

const numberText = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

const formatTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadAnnouncements)
</script>
