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
      <el-button
          type="primary"
          class="rounded-2xl bg-primary px-6 py-4 text-sm font-black text-white shadow-lg shadow-primary/20 transition"
          :class="canPublishAnnouncement ? 'hover:bg-primary/90' : 'cursor-not-allowed grayscale'"
          :disabled="!canPublishAnnouncement"
          :title="canPublishAnnouncement ? '发布公告' : '当前账号暂无发布公告权限'"
          @click="goPublish"
      >
        发布公告
      </el-button>
    </section>

    <section class="rounded-3xl border border-outline-variant/20 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div class="flex flex-wrap gap-2">
          <el-button
              v-for="item in levelTabs"
              :key="item.value"
              :type="activeLevel === item.value ? 'primary' : 'default'"
              class="rounded-full border px-4 py-2 text-sm font-black transition"
              :class="activeLevel === item.value ? 'border-primary bg-primary text-white shadow-md shadow-primary/15' : 'border-outline-variant/30 bg-surface-container-lowest text-on-surface-variant hover:border-primary/40 hover:text-primary'"
              @click="selectLevel(item.value)"
          >
            {{ item.label }}
          </el-button>
        </div>
        <el-button class="rounded-xl bg-primary/10 px-4 py-2 text-xs font-black text-primary" :disabled="!canReadAnnouncements" @click="loadAnnouncements">
          刷新
        </el-button>
      </div>

      <el-skeleton v-if="loading" class="mt-6" :rows="4" animated />
      <div v-else-if="announcementLoadError" class="mt-6 space-y-4 rounded-2xl border border-red-200 bg-red-50 p-5">
        <el-alert
            title="公告加载失败"
            :description="announcementLoadError"
            type="error"
            :closable="false"
            show-icon
        />
        <el-button type="primary" plain @click="loadAnnouncements">
          <span class="material-symbols-outlined text-[18px]">refresh</span>
          重新加载
        </el-button>
      </div>
      <el-empty v-else-if="!announcements.length" class="mt-6" description="暂无公告" />
      <div v-else class="mt-5 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div
            v-for="item in announcements"
            :key="item.id || `${item.title}-${item.updateTime}`"
            class="rounded-2xl border p-4"
            :class="announcementCardClass(item.level)"
        >
          <div class="flex items-start justify-between gap-3">
            <h3 class="text-base font-black leading-6">{{ item.title }}</h3>
            <el-tag class="shrink-0 rounded-full bg-white/70 px-2.5 py-1 text-[11px] font-black" effect="plain">
              {{ announcementLevelText(item.level) }}
            </el-tag>
          </div>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-7 opacity-90">{{ item.content }}</p>
          <div class="mt-4 rounded-2xl border border-white/70 bg-white/55 p-3">
            <div class="flex flex-wrap items-center gap-2 text-xs font-black">
              <el-tag class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-700" type="success" effect="light">
                已读 {{ numberText(item.readCount) }}
              </el-tag>
              <el-tag class="rounded-full bg-amber-100 px-3 py-1 text-amber-700" type="warning" effect="light">
                未读 {{ numberText(item.unreadCount) }}
              </el-tag>
              <el-tag class="rounded-full bg-slate-100 px-3 py-1 text-slate-600" type="info" effect="light">
                共 {{ numberText(item.totalReceiverCount) }} 人
              </el-tag>
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
                  <el-tag
                      class="shrink-0 rounded-full px-2 py-1 font-black"
                      :class="receiverRead(receiver) ? 'bg-emerald-100' : 'bg-amber-100'"
                      :type="receiverRead(receiver) ? 'success' : 'warning'"
                      effect="light"
                  >
                    {{ receiverRead(receiver) ? '已读' : '未读' }}
                  </el-tag>
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
import { ElAlert, ElButton, ElEmpty, ElMessage, ElSkeleton, ElTag } from 'element-plus'
import { getAnnouncements } from '@/api/notification.js'
import { useUserStore } from '@/stores/user.js'
import { createLatestRequestGuard } from './latestRequestGuard.js'

defineOptions({ name: 'AnnouncementCenter' })

const ANNOUNCEMENT_PUBLISH_PERMISSION = 'notification:announcement:publish'
const ANNOUNCEMENT_LIST_PERMISSION = 'notification:announcement:list'
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const announcementLoadError = ref('')
const activeLevel = ref('all')
const announcements = ref([])
const announcementRequestGuard = createLatestRequestGuard()
const canPublishAnnouncement = computed(() => userStore.hasPermission(ANNOUNCEMENT_PUBLISH_PERMISSION))
const canReadAnnouncements = computed(() => userStore.hasPermission(ANNOUNCEMENT_LIST_PERMISSION))

const levelTabs = [
  { value: 'all', label: '全部公告' },
  { value: 'normal', label: '普通公告' },
  { value: 'urgent', label: '紧急公告' },
  { value: 'important', label: '重要公告' }
]

async function loadAnnouncements() {
  const requestId = announcementRequestGuard.begin()
  if (!canReadAnnouncements.value) {
    loading.value = false
    announcements.value = []
    announcementLoadError.value = '当前账号暂无公告查看权限。'
    return
  }
  announcementRequestGuard.commit(requestId, () => {
    announcementLoadError.value = ''
    loading.value = true
  })
  try {
    const params = { limit: 30 }
    if (activeLevel.value !== 'all') {
      params.levels = activeLevel.value
    }
    const data = await getAnnouncements(params)
    announcementRequestGuard.commit(requestId, () => {
      announcements.value = Array.isArray(data) ? data : []
    })
  } catch (error) {
    announcementRequestGuard.commit(requestId, () => {
      announcements.value = []
      const code = Number(error?.code || error?.response?.data?.code || error?.response?.status || error?.status || 0)
      announcementLoadError.value = code === 403
        ? '当前账号暂无公告查看权限。'
        : error?.msg || error?.message || '公告加载失败，请稍后重试。'
    })
  } finally {
    announcementRequestGuard.commit(requestId, () => {
      loading.value = false
    })
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
