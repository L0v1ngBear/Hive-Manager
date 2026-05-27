<template>
  <main class="min-h-screen overflow-hidden bg-[#f8fafc] text-slate-900">
    <section class="relative flex min-h-screen items-center justify-center px-4 py-10">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_30%_10%,rgba(31,63,95,0.18),transparent_32%),linear-gradient(135deg,#fbfcfe_0%,#eef4fb_45%,#f8fbff_100%)]"></div>
      <div class="absolute -left-16 top-20 h-56 w-56 rounded-full bg-primary/20 blur-3xl"></div>
      <div class="absolute -right-10 bottom-16 h-72 w-72 rounded-full bg-blue-300/20 blur-3xl"></div>

      <section class="relative z-10 w-full max-w-3xl overflow-hidden rounded-[2rem] border border-white/70 bg-white/85 shadow-[0_32px_80px_-28px_rgba(15,23,42,0.45)] backdrop-blur-xl">
        <div class="grid gap-0 md:grid-cols-[0.9fr_1.1fr]">
          <aside class="flex flex-col justify-between bg-slate-950 p-8 text-white">
            <div>
              <div class="mb-8 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-2xl font-black text-white">H</div>
              <p class="text-sm font-bold uppercase tracking-[0.32em] text-primary">Security Check</p>
              <h1 class="mt-4 text-3xl font-black leading-tight">首次登录需要修改密码</h1>
              <p class="mt-4 text-sm leading-7 text-slate-300">
                为了避免默认密码长期使用，请先设置一个只有你知道的新密码。修改完成后会自动进入系统。
              </p>
            </div>
            <div class="mt-10 rounded-2xl border border-white/10 bg-white/5 p-4 text-xs leading-6 text-slate-300">
              建议使用 8-64 位密码，并同时包含字母和数字。不要使用手机号、默认密码或过于简单的组合。
            </div>
          </aside>

          <section class="p-8 md:p-10">
            <div class="mb-8">
              <p class="text-sm font-bold text-primary">账号安全</p>
              <h2 class="mt-2 text-2xl font-black text-slate-900">设置新登录密码</h2>
              <p class="mt-2 text-sm text-slate-500">当前账号：{{ userName }}</p>
            </div>

            <form class="space-y-5" @submit.prevent="handleSubmit">
              <label class="block">
                <span class="mb-2 block text-sm font-bold text-slate-700">原密码</span>
                <input
                  v-model="form.oldPassword"
                  type="password"
                  autocomplete="current-password"
                  class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-slate-900 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
                  placeholder="请输入当前登录密码"
                />
              </label>

              <label class="block">
                <span class="mb-2 block text-sm font-bold text-slate-700">新密码</span>
                <input
                  v-model="form.newPassword"
                  type="password"
                  autocomplete="new-password"
                  maxlength="64"
                  class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-slate-900 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
                  placeholder="8-64位，包含字母和数字"
                />
              </label>

              <label class="block">
                <span class="mb-2 block text-sm font-bold text-slate-700">确认新密码</span>
                <input
                  v-model="form.confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  maxlength="64"
                  class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-slate-900 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
                  placeholder="请再次输入新密码"
                />
              </label>

              <div class="flex flex-col gap-3 pt-3 sm:flex-row">
                <button
                  type="submit"
                  :disabled="submitting"
                  class="inline-flex flex-1 items-center justify-center rounded-2xl bg-primary px-5 py-3.5 text-base font-black text-white shadow-lg shadow-primary/25 transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-slate-300"
                >
                  {{ submitting ? '提交中...' : '确认修改并进入系统' }}
                </button>
                <button
                  type="button"
                  class="rounded-2xl border border-slate-200 px-5 py-3.5 text-sm font-bold text-slate-600 transition hover:bg-slate-50"
                  @click="handleLogout"
                >
                  退出登录
                </button>
              </div>
            </form>
          </section>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changeInitialPassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { normalizeLoginRedirect } from '@/utils/redirect'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const submitting = ref(false)

const userName = computed(() => userStore.userInfo?.userName || '当前用户')

function validateForm() {
  const oldPassword = String(form.oldPassword || '').trim()
  const newPassword = String(form.newPassword || '').trim()
  const confirmPassword = String(form.confirmPassword || '').trim()
  if (!oldPassword) {
    return '请输入原密码'
  }
  if (newPassword.length < 8 || newPassword.length > 64) {
    return '新密码长度需要为8-64位'
  }
  if (!/[A-Za-z]/.test(newPassword) || !/\d/.test(newPassword)) {
    return '新密码需要同时包含字母和数字'
  }
  if (newPassword !== confirmPassword) {
    return '两次输入的新密码不一致'
  }
  if (oldPassword === newPassword) {
    return '新密码不能与原密码相同'
  }
  return ''
}

function resolveTargetPath() {
  const target = normalizeLoginRedirect(route.query.redirect, '/dashboard')
  return target === '/force-password-change' ? '/dashboard' : target
}

async function handleSubmit() {
  if (submitting.value) {
    return
  }
  const error = validateForm()
  if (error) {
    ElMessage.warning(error)
    return
  }

  submitting.value = true
  try {
    await changeInitialPassword({
      oldPassword: form.oldPassword.trim(),
      newPassword: form.newPassword.trim(),
      confirmPassword: form.confirmPassword.trim()
    })
    userStore.markPasswordChanged()
    ElMessage.success('密码修改成功')
    await router.replace(resolveTargetPath())
  } catch (error) {
    ElMessage.error(error?.msg || error?.message || '密码修改失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}
</script>
