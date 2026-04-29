<template>
  <main class="login-stage min-h-full bg-slate-50 text-slate-800 overflow-hidden font-sans relative">
    <section class="absolute inset-0 z-0 overflow-hidden bg-[#fffdf8]">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_50%_12%,rgba(255,196,41,0.16),transparent_34%),linear-gradient(180deg,#ffffff_0%,#fffaf0_100%)] pointer-events-none"></div>

      <div class="absolute top-12 left-12 z-10 pointer-events-none">
        <span class="text-primary/5 text-[12rem] font-black tracking-widest select-none leading-none">HIVE</span>
      </div>

      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex gap-16 z-20 pointer-events-none opacity-40 mix-blend-color-burn">
        <svg ref="char1Ref" :class="['pixel-char w-40 h-40 drop-shadow-xl', { 'error-shake': isError }]" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect fill="#f5a400" height="80" width="80" x="10" y="10" rx="4"></rect>
          <rect fill="#101418" height="10" width="80" x="10" y="80" rx="2"></rect>
          <g class="eye-container">
            <rect class="eye-white" fill="white" height="15" width="15" x="25" y="30" rx="2"></rect>
            <rect class="eye-pupil" fill="#101418" height="8" width="8" x="28" y="33" rx="1" :transform="pupil1Transform"></rect>
            <rect class="eye-white" fill="white" height="15" width="15" x="60" y="30" rx="2"></rect>
            <rect class="eye-pupil" fill="#101418" height="8" width="8" x="63" y="33" rx="1" :transform="pupil1Transform"></rect>
          </g>
          <path class="mouth" :d="mouth1Path" stroke="white" stroke-linecap="round" stroke-width="4"></path>
        </svg>

        <svg ref="char2Ref" :class="['pixel-char w-32 h-32 mt-16 drop-shadow-xl', { 'error-shake': isError }]" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect fill="#ffd43b" height="80" width="80" x="10" y="10" rx="4"></rect>
          <rect fill="#f5a400" height="10" width="80" x="10" y="80" rx="2"></rect>
          <g class="eye-container">
            <rect class="eye-white" fill="white" height="12" width="12" x="30" y="35" rx="2"></rect>
            <rect class="eye-pupil" fill="#101418" height="6" width="6" x="33" y="38" rx="1" :transform="pupil2Transform"></rect>
            <rect class="eye-white" fill="white" height="12" width="12" x="58" y="35" rx="2"></rect>
            <rect class="eye-pupil" fill="#101418" height="6" width="6" x="61" y="38" rx="1" :transform="pupil2Transform"></rect>
          </g>
          <path class="mouth" :d="mouth2Path" stroke="#101418" stroke-linecap="round" stroke-width="3"></path>
        </svg>
      </div>
    </section>

    <section class="relative z-30 min-h-full flex flex-col justify-center items-center px-4 py-12 lg:px-8">

      <div class="text-center mb-10 max-w-2xl mx-auto z-40">
        <div class="inline-flex items-center justify-center gap-3 mb-6 bg-white/60 backdrop-blur-md px-6 py-2 rounded-full shadow-sm border border-white/40">
          <img src="../../images/logo.png" alt="蜂巢 logo" class="h-10 w-10 rounded-xl object-contain drop-shadow-sm ring-1 ring-primary/10" />
          <span class="text-xl font-bold tracking-tight text-slate-800">蜂巢 Hive</span>
        </div>
        <h1 class="text-4xl md:text-5xl font-extrabold text-slate-900 tracking-tight leading-tight mb-4">
          企业信息管理
        </h1>
        <p class="text-slate-600 text-lg font-medium leading-relaxed">
          专业、高效、可靠、价值，协同工业生产效率。
          把经验和流程变成可追踪、可复盘、可优化的数据资产。
        </p>
      </div>

      <div class="w-full max-w-5xl bg-white/80 backdrop-blur-xl rounded-3xl shadow-[0_32px_64px_-12px_rgba(15,23,42,0.1)] border border-white flex flex-col lg:flex-row overflow-hidden transform transition-all">

        <aside class="w-full lg:w-2/5 bg-gradient-to-br from-slate-50 to-slate-100/50 p-10 lg:p-12 border-b lg:border-b-0 lg:border-r border-slate-200/60 flex flex-col items-center justify-center text-center relative">
          <div class="absolute top-6 left-6 text-slate-300">
            <span class="material-symbols-outlined text-4xl opacity-50">qr_code_scanner</span>
          </div>

          <h2 class="text-2xl font-bold text-slate-800 mb-2">快捷登录</h2>
          <p class="text-slate-500 text-sm mb-8 font-medium">使用 Hive 移动端小程序扫码</p>

          <div v-if="scanStatus === 'CONFIRMED'" class="w-full h-64 bg-emerald-50/80 rounded-2xl border border-emerald-100 flex flex-col items-center justify-center gap-4 p-6 transition-all">
            <div class="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-emerald-600">task_alt</span>
            </div>
            <div>
              <p class="text-lg font-bold text-emerald-700">扫码确认成功</p>
              <p class="text-sm text-emerald-600 mt-2">正在安全接入系统，请稍候...</p>
            </div>
          </div>

          <div v-else class="flex flex-col items-center w-full">
            <div class="relative group">
              <div class="absolute -inset-1 bg-gradient-to-r from-primary/20 to-blue-400/20 rounded-3xl blur opacity-50 group-hover:opacity-100 transition duration-500"></div>
              <div class="relative bg-white p-4 rounded-2xl shadow-sm border border-slate-100 w-56 h-56 flex items-center justify-center">
                <img v-if="scanSession.qrCodeDataUrl" :src="scanSession.qrCodeDataUrl" alt="扫码登录二维码" class="w-full h-full object-contain" />
                <div v-else class="flex flex-col items-center justify-center text-slate-400 gap-2">
                  <span class="material-symbols-outlined animate-spin">refresh</span>
                  <span class="text-sm">生成中...</span>
                </div>
              </div>
            </div>
            <div class="mt-8 px-4 py-2 bg-white/60 rounded-full border border-slate-200 shadow-sm inline-flex items-center gap-2">
              <span class="w-2 h-2 rounded-full" :class="scanStatus === 'EXPIRED' ? 'bg-error animate-pulse' : 'bg-primary animate-pulse'"></span>
              <p class="text-sm font-medium text-slate-600">{{ scanStatusText }}</p>
            </div>
          </div>
        </aside>

        <section class="w-full lg:w-3/5 p-10 lg:p-16 flex flex-col justify-center bg-white">
          <div class="mb-10">
            <h3 class="text-3xl font-extrabold text-slate-900 tracking-tight">账号登录</h3>
            <p class="text-slate-500 mt-2 font-medium">欢迎回来，请输入您的管理员凭证</p>
          </div>

          <form class="space-y-6" @submit.prevent="handleLogin">
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700 block" for="username">账号</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-slate-400 text-lg">person</span>
                </div>
                <input
                    id="username"
                    v-model.trim="loginForm.username"
                    type="text"
                    required
                    placeholder="请输入员工编号或邮箱"
                    :class="['w-full pl-11 pr-4 py-3.5 bg-slate-50 border rounded-xl text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-4 transition-all duration-200',
                          isError ? 'border-error/50 focus:ring-error/20 bg-error/5' : 'border-slate-200 focus:border-primary focus:ring-primary/10']"
                />
              </div>
            </div>

            <div class="space-y-2">
              <div class="flex justify-between items-center">
                <label class="text-sm font-bold text-slate-700 block" for="password">密码</label>
                <a href="#" class="text-sm font-semibold text-primary hover:text-primary/80 transition-colors">忘记密码?</a>
              </div>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <span class="material-symbols-outlined text-slate-400 text-lg">lock</span>
                </div>
                <input
                    id="password"
                    v-model="loginForm.password"
                    type="password"
                    required
                    placeholder="••••••••"
                    :class="['w-full pl-11 pr-4 py-3.5 bg-slate-50 border rounded-xl text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-4 transition-all duration-200 tracking-widest',
                          isError ? 'border-error/50 focus:ring-error/20 bg-error/5' : 'border-slate-200 focus:border-primary focus:ring-primary/10']"
                />
              </div>
            </div>

            <div class="pt-4 space-y-4">
              <div v-show="isError" class="flex items-center gap-2 text-error text-sm font-medium bg-error/10 p-3 rounded-lg animate-fade-in">
                <span class="material-symbols-outlined text-base">error</span>
                {{ errorMessage }}
              </div>

              <button
                  type="submit"
                  class="w-full py-4 bg-primary hover:bg-primary/90 text-white font-bold text-base rounded-xl shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all duration-200 active:scale-[0.98] flex items-center justify-center gap-2"
                  :disabled="isLoading"
              >
                <span v-if="isLoading" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
                <span>{{ isLoading ? '正在验证身份...' : '立即登录系统' }}</span>
                <span v-if="!isLoading" class="material-symbols-outlined text-lg ml-1">arrow_forward</span>
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>

  </main>
</template>

<script setup>
// [此处脚本逻辑与你的原版代码完全一致，未作任何修改]
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScanLoginSession, getScanLoginStatus, login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { normalizeLoginRedirect } from '@/utils/redirect'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = reactive({
  username: '',
  password: ''
})

const scanSession = reactive({
  sceneKey: '',
  qrCodeDataUrl: '',
  expireAt: 0
})

const isLoading = ref(false)
const isRefreshingScan = ref(false)
const isError = ref(false)
const errorMessage = ref('')
const scanStatus = ref('IDLE')
const scanMessage = ref('请使用已登录的小程序扫码确认')

const char1Ref = ref(null)
const char2Ref = ref(null)
const pupil1Transform = ref('translate(0, 0)')
const pupil2Transform = ref('translate(0, 0)')
const mouth1Path = ref('M35 65 H65')
const mouth2Path = ref('M40 65 H60')

let pollTimer = null
let refreshTimer = null

const scanStatusText = computed(() => {
  if (scanStatus.value === 'CONFIRMED') {
    return '已在小程序确认，正在登录...'
  }
  if (scanStatus.value === 'EXPIRED') {
    return '二维码已过期，自动刷新中...'
  }
  return scanMessage.value || '请使用小程序扫码登录'
})

async function handleLogin() {
  if (!loginForm.username || !loginForm.password || isLoading.value) {
    return
  }

  isLoading.value = true
  isError.value = false
  errorMessage.value = ''

  try {
    const loginData = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    finishLogin(loginData)
  } catch (error) {
    errorMessage.value = error?.msg || error?.message || '用户名或密码错误，请重试。'
    triggerErrorState()
  } finally {
    isLoading.value = false
  }
}

async function refreshScanSession() {
  if (isRefreshingScan.value) {
    return
  }

  isRefreshingScan.value = true
  clearPolling()
  try {
    const session = await createScanLoginSession()
    scanSession.sceneKey = session.sceneKey || ''
    scanSession.qrCodeDataUrl = session.qrCodeDataUrl || ''
    scanSession.expireAt = Number(session.expireAt || 0)
    scanStatus.value = 'PENDING'
    scanMessage.value = '请用已登录的小程序扫码确认'
    scheduleAutoRefresh()
    startPolling()
  } catch (error) {
    scanStatus.value = 'EXPIRED'
    scanMessage.value = error?.msg || error?.message || '二维码生成失败，请稍后重试。'
  } finally {
    isRefreshingScan.value = false
  }
}

function startPolling() {
  if (!scanSession.sceneKey || pollTimer) {
    return
  }

  pollTimer = window.setInterval(async () => {
    try {
      const statusData = await getScanLoginStatus({ sceneKey: scanSession.sceneKey })
      scanStatus.value = statusData.status || 'PENDING'
      scanMessage.value = statusData.message || '请使用小程序扫码确认'
      if (statusData.expireAt) {
        scanSession.expireAt = Number(statusData.expireAt)
        scheduleAutoRefresh()
      }
      if (statusData.loginInfo?.token) {
        finishLogin(statusData.loginInfo)
        return
      }
      if (scanStatus.value === 'EXPIRED') {
        clearPolling()
        refreshScanSession()
      }
    } catch (error) {
      clearPolling()
      scanStatus.value = 'EXPIRED'
      scanMessage.value = error?.msg || error?.message || '扫码状态查询失败，正在尝试刷新二维码。'
      refreshScanSession()
    }
  }, 2000)
}

function scheduleAutoRefresh() {
  window.clearTimeout(refreshTimer)
  const remainMs = Math.max(scanSession.expireAt * 1000 - Date.now(), 0)
  refreshTimer = window.setTimeout(() => {
    scanStatus.value = 'EXPIRED'
    scanMessage.value = '二维码已过期，系统正在自动刷新...'
    refreshScanSession()
  }, remainMs)
}

function clearPolling() {
  window.clearInterval(pollTimer)
  window.clearTimeout(refreshTimer)
  pollTimer = null
  refreshTimer = null
}

function finishLogin(loginData) {
  clearPolling()
  scanStatus.value = 'CONFIRMED'
  userStore.setLoginInfo(loginData)
  const redirect = resolveLoginRedirect()
  window.setTimeout(() => {
    router.replace(redirect)
  }, 300)
}

function resolveLoginRedirect() {
  if (userStore.isDeveloper) {
    return '/platform/tenant'
  }
  return normalizeLoginRedirect(route.query.redirect)
}

function triggerErrorState() {
  isError.value = true
  mouth1Path.value = 'M 40,65 a 10,10 0 1,0 20,0 a 10,10 0 1,0 -20,0'
  mouth2Path.value = 'M 45,65 a 5,5 0 1,0 10,0 a 5,5 0 1,0 -10,0'
  window.setTimeout(() => {
    isError.value = false
    mouth1Path.value = 'M35 65 H65'
    mouth2Path.value = 'M40 65 H60'
  }, 3000)
}

function onMouseMove(event) {
  const { clientX, clientY } = event

  if (char1Ref.value) {
    const rect1 = char1Ref.value.getBoundingClientRect()
    const centerX1 = rect1.left + rect1.width / 2
    const centerY1 = rect1.top + rect1.height / 2
    const angle1 = Math.atan2(clientY - centerY1, clientX - centerX1)
    const distance1 = Math.min(3, Math.hypot(clientX - centerX1, clientY - centerY1) / 100)
    pupil1Transform.value = `translate(${Math.cos(angle1) * distance1}, ${Math.sin(angle1) * distance1})`
  }

  if (char2Ref.value) {
    const rect2 = char2Ref.value.getBoundingClientRect()
    const centerX2 = rect2.left + rect2.width / 2
    const centerY2 = rect2.top + rect2.height / 2
    const angle2 = Math.atan2(clientY - centerY2, clientX - centerX2)
    const distance2 = Math.min(3, Math.hypot(clientX - centerX2, clientY - centerY2) / 100)
    pupil2Transform.value = `translate(${Math.cos(angle2) * distance2}, ${Math.sin(angle2) * distance2})`
  }
}

onMounted(async () => {
  if (userStore.token) {
    await router.replace(resolveLoginRedirect())
    return
  }
  window.addEventListener('mousemove', onMouseMove)
  refreshScanSession()
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  clearPolling()
})
</script>

<style scoped>
/* 精简了大量不必要的自定义CSS，将大部分样式抽离到了Tailwind工具类中 */

.pixel-char {
  transition: transform 0.2s ease-out;
}

.eye-pupil {
  transition: transform 0.1s ease-out;
}

.mouth {
  transition: d 0.3s ease-in-out;
}

/* 保留原有的报错抖动动效 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px) rotate(-2deg); }
  75% { transform: translateX(6px) rotate(2deg); }
}

.error-shake {
  animation: shake 0.3s cubic-bezier(.36,.07,.19,.97) both;
}

/* 简单的渐入动效用于报错提示出现时 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}
</style>
