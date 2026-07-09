<template>
  <div class="min-h-fit max-w-5xl mx-auto space-y-6">
    <section class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-2 text-xs font-black tracking-[0.18em] text-primary">
          <span class="material-symbols-outlined text-[18px]">campaign</span>
          企业通知公告
        </p>
        <h1 class="mt-4 text-3xl md:text-4xl font-black tracking-tight text-on-surface">
          发布公告
        </h1>
        <p class="mt-2 text-base text-on-surface-variant max-w-2xl">
          面向当前组织发布公告，发布后员工可在电脑端和手机端查看，系统会记录已读未读状态。
        </p>
      </div>
      <button class="rounded-2xl border border-outline-variant/30 bg-white px-6 py-4 text-sm font-black text-on-surface-variant transition hover:border-primary/40 hover:text-primary" @click="goList">
        查看公告
      </button>
    </section>

    <section class="rounded-3xl border border-primary/20 bg-white p-6 shadow-sm">
      <h2 class="text-xl font-black text-on-surface">公告内容</h2>
      <div class="mt-5 space-y-5">
        <label class="block">
          <span class="text-sm font-bold text-on-surface">公告类型</span>
          <select v-model="form.level" class="mt-2 w-full rounded-2xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm font-bold outline-none focus:border-primary">
            <option value="normal">普通公告</option>
            <option value="urgent">紧急公告</option>
            <option value="important">重要公告</option>
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
              rows="10"
              class="mt-2 w-full resize-none rounded-2xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm leading-7 outline-none focus:border-primary"
              placeholder="请写清楚公告事项、时间、责任人和需要员工完成的动作。"
          ></textarea>
          <span class="mt-1 block text-right text-xs text-on-surface-variant">{{ form.content.length }}/1000</span>
        </label>
      </div>

      <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
        <button class="rounded-2xl border border-outline-variant/30 bg-white px-6 py-3 text-sm font-black text-on-surface-variant transition hover:border-primary/40 hover:text-primary" @click="resetForm">
          重置
        </button>
        <button
            class="rounded-2xl bg-primary px-7 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="publishing"
            @click="handlePublish"
        >
          {{ publishing ? '发布中...' : '发布公告' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishAnnouncement } from '@/api/notification.js'

defineOptions({ name: 'AnnouncementPublish' })

const router = useRouter()
const publishing = ref(false)
const form = reactive({
  level: 'normal',
  title: '',
  content: ''
})

function resetForm() {
  form.level = 'normal'
  form.title = ''
  form.content = ''
}

function goList() {
  router.push('/function/announcement')
}

async function handlePublish() {
  if (!form.title) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!form.content) {
    ElMessage.warning('请填写公告内容')
    return
  }
  publishing.value = true
  try {
    await publishAnnouncement({
      level: form.level,
      title: form.title,
      content: form.content
    })
    ElMessage.success('公告已发布')
    resetForm()
    router.push('/function/announcement')
  } catch (error) {
    ElMessage.error(error?.msg || '发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}
</script>
