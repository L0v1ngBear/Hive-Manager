<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="hero-badge">HIVE</div>
      <div class="hero-copy">
        <span class="hero-line"></span>
        <p class="hero-kicker">安全登录网关</p>
        <h1>Hive 蜂巢<br />数字化工厂管理系统</h1>
        <p class="hero-desc">网页端支持账号密码登录，也支持使用已登录的小程序扫码确认登录，避免重复走微信网页认证费用。</p>
      </div>
    </section>

    <section class="login-panel-wrap">
      <div class="login-panel">
        <div class="panel-header">
          <div class="brand-mark">H</div>
          <div>
            <h2>欢迎回来</h2>
            <p>选择适合当前场景的登录方式</p>
          </div>
        </div>

        <div class="panel-grid">
          <form class="login-card" @submit.prevent="handleLogin">
            <div class="card-head">
              <h3>账号密码登录</h3>
              <p>适合管理员直接登录</p>
            </div>

            <label class="input-label" for="username">账号</label>
            <input
              id="username"
              v-model.trim="loginForm.username"
              class="text-input"
              type="text"
              required
              placeholder="请输入登录账号或手机号"
            />

            <label class="input-label" for="password">密码</label>
            <input
              id="password"
              v-model="loginForm.password"
              class="text-input"
              type="password"
              required
              placeholder="请输入登录密码"
            />

            <button type="submit" class="primary-button" :disabled="isLoading">
              <span v-if="isLoading" class="material-symbols-outlined spin">progress_activity</span>
              {{ isLoading ? '登录中...' : '账号密码登录' }}
            </button>

            <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
          </form>

          <section class="login-card qr-card">
            <div class="card-head">
              <h3>小程序扫码登录</h3>
              <p>打开已登录的小程序，点击“扫码登录网页端”后扫描下方二维码。</p>
            </div>

            <div class="qr-shell">
              <img v-if="scanSession.qrCodeDataUrl" :src="scanSession.qrCodeDataUrl" alt="扫码登录二维码" class="qr-image" />
              <div v-else class="qr-placeholder">正在生成二维码...</div>
            </div>

            <div class="qr-status">
              <p>{{ scanStatusText }}</p>
              <small v-if="countdownText">{{ countdownText }}</small>
            </div>

            <button type="button" class="secondary-button" @click="refreshScanSession" :disabled="isRefreshingScan">
              {{ isRefreshingScan ? '刷新中...' : '刷新二维码' }}
            </button>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createScanLoginSession, getScanLoginStatus, login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

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
const errorMessage = ref('')
const scanStatus = ref('IDLE')
const scanMessage = ref('正在准备扫码登录...')
const countdown = ref(0)

let pollTimer = null
let countdownTimer = null

const scanStatusText = computed(() => {
  if (scanStatus.value === 'CONFIRMED') {
    return '已在小程序确认，正在为你登录网页端...'
  }
  if (scanStatus.value === 'EXPIRED') {
    return '二维码已过期，请刷新二维码后重新扫码。'
  }
  return scanMessage.value || '请使用小程序扫码登录网页端'
})

const countdownText = computed(() => {
  if (!countdown.value || scanStatus.value === 'CONFIRMED') {
    return ''
  }
  return `二维码剩余 ${countdown.value} 秒`
})

async function handleLogin() {
  if (!loginForm.username || !loginForm.password || isLoading.value) {
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const loginData = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    finishLogin(loginData)
  } catch (error) {
    errorMessage.value = error?.msg || error?.message || '账号或密码错误，请重试'
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
    syncCountdown()
    startPolling()
  } catch (error) {
    scanStatus.value = 'EXPIRED'
    scanMessage.value = error?.msg || error?.message || '二维码生成失败，请稍后重试'
  } finally {
    isRefreshingScan.value = false
  }
}

function startPolling() {
  if (!scanSession.sceneKey) {
    return
  }
  pollTimer = window.setInterval(async () => {
    try {
      const statusData = await getScanLoginStatus({ sceneKey: scanSession.sceneKey })
      scanStatus.value = statusData.status || 'PENDING'
      scanMessage.value = statusData.message || '请使用小程序扫码确认'
      if (statusData.expireAt) {
        scanSession.expireAt = Number(statusData.expireAt)
        syncCountdown()
      }
      if (statusData.loginInfo?.token) {
        finishLogin(statusData.loginInfo)
        return
      }
      if (scanStatus.value === 'EXPIRED') {
        clearPolling()
      }
    } catch (error) {
      clearPolling()
      scanStatus.value = 'EXPIRED'
      scanMessage.value = error?.msg || error?.message || '扫码状态查询失败，请刷新二维码'
    }
  }, 2000)
}

function syncCountdown() {
  window.clearInterval(countdownTimer)
  const tick = () => {
    const remain = Math.max(0, scanSession.expireAt - Math.floor(Date.now() / 1000))
    countdown.value = remain
    if (remain <= 0 && scanStatus.value !== 'CONFIRMED') {
      scanStatus.value = 'EXPIRED'
      scanMessage.value = '二维码已过期，请刷新二维码后重新扫码。'
      clearPolling()
    }
  }
  tick()
  countdownTimer = window.setInterval(tick, 1000)
}

function clearPolling() {
  window.clearInterval(pollTimer)
  window.clearInterval(countdownTimer)
  pollTimer = null
  countdownTimer = null
}

function finishLogin(loginData) {
  clearPolling()
  userStore.setLoginInfo(loginData)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  router.replace(redirect)
}

onMounted(async () => {
  if (userStore.token) {
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
    return
  }
  await refreshScanSession()
})

onUnmounted(() => {
  clearPolling()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(540px, 720px);
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.14), transparent 28%),
    linear-gradient(135deg, #eaf1fb, #f7f9fc 52%, #ffffff);
  color: #0f172a;
}

.login-hero {
  position: relative;
  padding: 64px 56px;
  display: flex;
  align-items: flex-end;
}

.hero-badge {
  position: absolute;
  top: 56px;
  left: 56px;
  font-size: 72px;
  font-weight: 900;
  letter-spacing: 0.22em;
  color: rgba(15, 23, 42, 0.08);
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 520px;
}

.hero-line {
  display: inline-block;
  width: 48px;
  height: 4px;
  background: #1d4ed8;
  margin-bottom: 20px;
}

.hero-kicker {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: #475569;
}

.hero-copy h1 {
  margin: 0;
  font-size: 52px;
  line-height: 1.05;
  font-weight: 900;
  color: #0f172a;
}

.hero-desc {
  margin: 22px 0 0;
  font-size: 16px;
  line-height: 1.8;
  color: #475569;
}

.login-panel-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px;
}

.login-panel {
  width: 100%;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  box-shadow: 0 20px 80px rgba(15, 23, 42, 0.12);
  padding: 28px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #fff;
  font-weight: 800;
  font-size: 22px;
}

.panel-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
}

.panel-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.login-card {
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #fff;
  padding: 24px;
}

.card-head {
  margin-bottom: 22px;
}

.card-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.card-head p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.input-label {
  display: block;
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #475569;
  font-weight: 700;
}

.text-input {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 14px;
  margin-bottom: 16px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.text-input:focus {
  outline: none;
  border-color: rgba(37, 99, 235, 0.5);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.primary-button,
.secondary-button {
  width: 100%;
  min-height: 48px;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.15s ease, opacity 0.15s ease, box-shadow 0.2s ease;
}

.primary-button {
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #fff;
  box-shadow: 0 12px 24px rgba(29, 78, 216, 0.22);
}

.secondary-button {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid rgba(37, 99, 235, 0.15);
}

.primary-button:hover,
.secondary-button:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.primary-button:disabled,
.secondary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.error-text {
  margin: 14px 0 0;
  font-size: 13px;
  color: #b91c1c;
}

.qr-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-shell {
  width: 252px;
  height: 252px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(15, 23, 42, 0.04)),
    #ffffff;
  display: grid;
  place-items: center;
  padding: 16px;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.18);
}

.qr-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 18px;
  background: #fff;
}

.qr-placeholder {
  font-size: 14px;
  color: #64748b;
}

.qr-status {
  text-align: center;
  margin: 18px 0 16px;
  min-height: 56px;
}

.qr-status p {
  margin: 0;
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}

.qr-status small {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.spin {
  animation: spin 0.9s linear infinite;
  margin-right: 6px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: 280px;
  }
}

@media (max-width: 760px) {
  .login-panel-wrap,
  .login-hero {
    padding: 24px;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }

  .qr-shell {
    width: min(100%, 252px);
    height: min(100vw - 120px, 252px);
  }
}
</style>
