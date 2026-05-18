<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-xs font-black tracking-[0.18em] text-primary">
          <span class="material-symbols-outlined text-[18px]">campaign</span>
          企业通知公告
        </p>
        <h1 class="mt-4 text-3xl md:text-4xl font-black tracking-tight text-on-surface">发布企业通知</h1>
        <p class="mt-2 text-base text-on-surface-variant max-w-2xl">
          面向当前组织发布制度、业务安排和临时提醒。公告会展示在总览大盘，不进入待办闭环。
        </p>
      </div>
      <button
          class="rounded-2xl bg-primary px-6 py-4 text-sm font-black text-white shadow-lg shadow-primary/20 transition hover:bg-primary/90"
          :disabled="publishing"
          @click="handlePublish"
      >
        {{ publishing ? '发布中...' : '发布通知' }}
      </button>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[minmax(0,0.9fr)_minmax(360px,1.1fr)] gap-5">
      <article class="rounded-3xl border border-primary/20 bg-white p-6 shadow-sm">
        <h2 class="text-xl font-black text-on-surface">通知内容</h2>
        <div class="mt-5 space-y-5">
          <label class="block">
            <span class="text-sm font-bold text-on-surface">通知级别</span>
            <select v-model="form.level" class="mt-2 w-full rounded-2xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm font-bold outline-none focus:border-primary">
              <option value="info">普通公告</option>
              <option value="warning">重要提醒</option>
              <option value="critical">紧急通知</option>
            </select>
          </label>

          <label class="block">
            <span class="text-sm font-bold text-on-surface">标题 <b class="text-primary">*</b></span>
            <input
                v-model.trim="form.title"
                maxlength="80"
                class="mt-2 w-full rounded-2xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm outline-none focus:border-primary"
                placeholder="例如：本周六设备盘点通知"
            />
            <span class="mt-1 block text-right text-xs text-on-surface-variant">{{ form.title.length }}/80</span>
          </label>

          <label class="block">
            <span class="text-sm font-bold text-on-surface">内容 <b class="text-primary">*</b></span>
            <textarea
                v-model.trim="form.content"
                maxlength="1000"
                rows="9"
                class="mt-2 w-full resize-none rounded-2xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm leading-7 outline-none focus:border-primary"
                placeholder="请写清楚通知事项、时间、责任人和需要员工完成的动作。"
            ></textarea>
            <span class="mt-1 block text-right text-xs text-on-surface-variant">{{ form.content.length }}/1000</span>
          </label>
        </div>
      </article>

      <article class="rounded-3xl border border-outline-variant/20 bg-white p-6 shadow-sm">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-xl font-black text-on-surface">最近公告</h2>
            <p class="mt-1 text-xs text-on-surface-variant">总览大盘会展示最近 4 条。</p>
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
        <div v-else class="mt-5 space-y-3">
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
            <p class="mt-3 text-xs font-bold opacity-60">{{ formatTime(item.updateTime) }}</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAnnouncements, publishAnnouncement } from '@/api/notification.js'

defineOptions({ name: 'AnnouncementCenter' })

const loading = ref(false)
const publishing = ref(false)
const announcements = ref([])

const form = reactive({
  level: 'info',
  title: '',
  content: ''
})

async function loadAnnouncements() {
  loading.value = true
  try {
    const data = await getAnnouncements({ limit: 20 })
    announcements.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.msg || '公告加载失败')
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  if (!form.title) {
    ElMessage.warning('请填写通知标题')
    return
  }
  if (!form.content) {
    ElMessage.warning('请填写通知内容')
    return
  }
  publishing.value = true
  try {
    await publishAnnouncement({
      level: form.level,
      title: form.title,
      content: form.content
    })
    ElMessage.success('企业通知已发布')
    form.level = 'info'
    form.title = ''
    form.content = ''
    await loadAnnouncements()
  } catch (error) {
    ElMessage.error(error?.msg || '发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

const announcementCardClass = (level) => {
  if (level === 'critical') {
    return 'border-rose-200 bg-rose-50/70 text-rose-950'
  }
  if (level === 'warning') {
    return 'border-amber-200 bg-amber-50/70 text-amber-950'
  }
  return 'border-sky-200 bg-sky-50/70 text-sky-950'
}

const announcementLevelText = (level) => {
  if (level === 'critical') return '紧急'
  if (level === 'warning') return '重要'
  return '公告'
}

const formatTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadAnnouncements)
</script>
