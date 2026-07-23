<template>
  <main class="login-stage">
    <section class="login-shell">
      <aside class="login-brand-panel">
        <div class="login-watermark" aria-hidden="true">
          <span>HIVE</span>
        </div>
        <div class="login-brand-orb login-brand-orb--top" aria-hidden="true"></div>
        <div class="login-brand-orb login-brand-orb--bottom" aria-hidden="true"></div>
        <div class="login-brand-lockup">
          <span class="login-product-logo">
            <img src="/logo.png" :alt="`${siteConfig.productName} Logo`" />
          </span>
          <span class="login-brand-title">
            <small>HIVE WORKSPACE</small>
            <strong>蜂巢 Hive</strong>
          </span>
        </div>
        <div class="login-brand-copy">
          <span class="login-brand-badge">数字化工厂管理系统</span>
          <h1>企业信息管理</h1>
          <p>专业、高效、可靠、价值，协同工业生产效率。<br />把经验和流程变成可追踪、可复盘、可优化的数据资产。</p>
          <ul class="login-feature-list" aria-label="系统能力">
            <li><span class="material-symbols-outlined" aria-hidden="true">check_circle</span>订单与生产协同</li>
            <li><span class="material-symbols-outlined" aria-hidden="true">check_circle</span>库存全过程追踪</li>
            <li><span class="material-symbols-outlined" aria-hidden="true">check_circle</span>数据权限分级</li>
          </ul>
        </div>
        <div class="login-provider">
          <span>开发与技术服务</span>
          <strong>{{ siteConfig.companyName }}</strong>
        </div>
      </aside>

      <section class="login-auth-panel">
        <div class="login-auth-intro">
          <span class="login-auth-kicker">WELCOME BACK</span>
          <span class="login-security-note">
            <span class="material-symbols-outlined" aria-hidden="true">verified_user</span>
            安全登录
          </span>
        </div>
        <div class="login-mode-tabs" role="tablist" aria-label="登录方式">
          <button id="login-account-tab" ref="accountLoginTab" type="button" role="tab" :aria-selected="loginMode === 'account'" :tabindex="loginMode === 'account' ? 0 : -1" aria-controls="login-account-panel" @click="loginMode = 'account'" @keydown="handleLoginModeKeydown($event, 'account')">账号登录</button>
          <button id="login-scan-tab" ref="scanLoginTab" type="button" role="tab" :aria-selected="loginMode === 'scan'" :tabindex="loginMode === 'scan' ? 0 : -1" aria-controls="login-scan-panel" @click="loginMode = 'scan'" @keydown="handleLoginModeKeydown($event, 'scan')">扫码登录</button>
        </div>
        <div id="login-account-panel" role="tabpanel" aria-labelledby="login-account-tab" :hidden="loginMode !== 'account'">
          <div class="login-account-heading">
            <h3>账号登录</h3>
            <p>欢迎回来，请输入账号信息</p>
          </div>
          <el-form :model="loginForm" label-position="top" class="space-y-2" @submit.prevent="handleLogin">
            <el-form-item label="账号">
                <el-input
                    id="username"
                    v-model.trim="loginForm.username"
                    autocomplete="username"
                    placeholder="请输入工号、手机号或登录账号"
                    size="large"
                >
                  <template #prefix><span class="material-symbols-outlined text-lg">person</span></template>
                </el-input>
            </el-form-item>

            <el-form-item>
              <template #label>
              <div class="flex justify-between items-center">
                <span>密码</span>
                <a href="#" class="text-sm font-semibold text-primary hover:text-on-primary-container transition-colors" @click.prevent="openResetPasswordDialog">首次登录 / 忘记密码</a>
              </div>
              </template>
                <el-input
                    id="password"
                    v-model="loginForm.password"
                    type="password"
                    autocomplete="current-password"
                    show-password
                    placeholder="••••••••"
                    size="large"
                >
                  <template #prefix><span class="material-symbols-outlined text-lg">lock</span></template>
                </el-input>
            </el-form-item>

            <div class="flex flex-col gap-2 rounded-2xl border border-slate-100 bg-slate-50/70 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <el-checkbox v-model="rememberLogin">记住账号</el-checkbox>
              <span class="text-xs font-medium text-slate-500">仅保存账号，不保存密码和登录密钥</span>
            </div>

            <div class="pt-4 space-y-4">
              <div v-show="isError" class="flex items-center gap-2 text-error text-sm font-medium bg-error/10 p-3 rounded-lg animate-fade-in">
                <span class="material-symbols-outlined text-base">error</span>
                {{ errorMessage }}
              </div>

              <el-button
                  native-type="submit"
                  type="primary"
                  size="large"
                  class="w-full"
                  :loading="isLoading"
                  :disabled="isLoading"
              >
                <span>{{ isLoading ? '正在验证身份...' : '立即登录系统' }}</span>
                <span v-if="!isLoading" class="material-symbols-outlined text-lg ml-1">arrow_forward</span>
              </el-button>

              <el-button
                  class="w-full"
                  size="large"
                  @click="goJoinOrganization"
              >
                <span class="material-symbols-outlined text-lg text-primary">group_add</span>
                <span>使用组织码加入组织</span>
              </el-button>
            </div>
          </el-form>
        </div>

        <div id="login-scan-panel" class="login-scan-panel" role="tabpanel" aria-labelledby="login-scan-tab" :hidden="loginMode !== 'scan'">
          <h3>快捷登录</h3>
          <p>使用 Hive 移动端小程序扫码</p>
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
          <p id="login-scan-status" class="login-scan-live-region" role="status" aria-live="polite" aria-atomic="true">{{ scanStatusText }}</p>
        </div>
      </section>
    </section>

    <el-dialog v-model="resetDialogVisible" title="首次登录 / 忘记密码" width="440px" destroy-on-close @closed="closeResetPasswordDialog">
        <div class="mb-6">
          <div>
            <p class="mt-2 text-sm text-slate-500">首次登录或忘记密码时，通过绑定手机号接收短信验证码后设置登录密码。</p>
          </div>
        </div>

        <el-form :model="resetForm" label-position="top" @submit.prevent="handleResetPassword">
          <el-form-item label="绑定手机号">
            <el-input
              v-model.trim="resetForm.phone"
              maxlength="11"
              placeholder="请输入绑定手机号"
            />
          </el-form-item>

          <el-form-item label="登录账号（可选）">
            <el-input
              v-model.trim="resetForm.account"
              maxlength="64"
              placeholder="手机号唯一时可不填"
            />
          </el-form-item>

          <el-form-item label="短信验证码">
            <div class="flex gap-3">
              <el-input
                v-model.trim="resetForm.code"
                maxlength="6"
                placeholder="6位验证码"
                class="min-w-0 flex-1"
              />
              <el-button
                class="w-32"
                :disabled="codeSending || codeCountdown > 0"
                :loading="codeSending"
                @click="handleSendResetCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : (codeSending ? '发送中' : '获取验证码') }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="resetForm.newPassword"
              type="password"
              show-password
              maxlength="64"
              placeholder="至少8位，包含字母和数字"
            />
          </el-form-item>

          <el-form-item label="确认新密码">
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              show-password
              maxlength="64"
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <el-button
            native-type="submit"
            type="primary"
            class="mt-2 w-full"
            :loading="resetSubmitting"
            :disabled="resetSubmitting"
          >
            {{ resetSubmitting ? '提交中...' : '确认修改密码' }}
          </el-button>
        </el-form>
    </el-dialog>

  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScanLoginSession, getScanLoginStatus, login, resetPassword, sendPasswordResetCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { normalizeLoginRedirect } from '@/utils/redirect'
import { readLoginMemory, saveLoginMemory } from '@/utils/loginMemory'
import { ElButton, ElCheckbox, ElDialog, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { siteConfig } from '@/config/site'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginMemory = readLoginMemory()

const loginForm = reactive({
  username: loginMemory.username,
  password: ''
})
const rememberLogin = ref(loginMemory.remember)

const resetForm = reactive({
  phone: '',
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const scanSession = reactive({
  sceneKey: '',
  qrCodeDataUrl: '',
  expireAt: 0
})

const isLoading = ref(false)
const resetDialogVisible = ref(false)
const codeSending = ref(false)
const resetSubmitting = ref(false)
const codeCountdown = ref(0)
const isRefreshingScan = ref(false)
const isError = ref(false)
const errorMessage = ref('')
const loginMode = ref('account')
const accountLoginTab = ref(null)
const scanLoginTab = ref(null)
const scanStatus = ref('IDLE')
const scanMessage = ref('请使用已登录的小程序扫码确认')

let pollTimer = null
let refreshTimer = null
let codeCountdownTimer = null

const scanStatusText = computed(() => {
  if (scanStatus.value === 'CONFIRMED') {
    return '已在小程序确认，正在登录...'
  }
  if (scanStatus.value === 'EXPIRED') {
    return '二维码已过期，自动刷新中...'
  }
  return scanMessage.value || '请使用小程序扫码登录'
})

function focusLoginModeTab(mode) {
  const tab = mode === 'account' ? accountLoginTab.value : scanLoginTab.value
  tab?.focus()
}

function handleLoginModeKeydown(event, currentMode) {
  let nextMode = currentMode

  switch (event.key) {
    case 'ArrowRight':
      nextMode = currentMode === 'account' ? 'scan' : 'account'
      break
    case 'ArrowLeft':
      nextMode = currentMode === 'account' ? 'scan' : 'account'
      break
    case 'Home':
      nextMode = 'account'
      break
    case 'End':
      nextMode = 'scan'
      break
    default:
      return
  }

  event.preventDefault()
  loginMode.value = nextMode
  focusLoginModeTab(nextMode)
}

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
    saveLoginMemory({
      remember: rememberLogin.value,
      username: loginForm.username
    })
    finishLogin(loginData, { remember: rememberLogin.value })
  } catch (error) {
    errorMessage.value = error?.msg || error?.message || '用户名或密码错误，请重试。'
    triggerErrorState()
  } finally {
    isLoading.value = false
  }
}

function openResetPasswordDialog() {
  resetDialogVisible.value = true
}

function goJoinOrganization() {
  clearPolling()
  router.push('/join-organization')
}

function closeResetPasswordDialog() {
  resetDialogVisible.value = false
  resetForm.phone = ''
  resetForm.account = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  stopCodeCountdown()
}

async function handleSendResetCode() {
  if (codeSending.value || codeCountdown.value > 0) {
    return
  }
  const phone = normalizePhone(resetForm.phone)
  if (!phone) {
    ElMessage.warning('请输入有效的11位手机号')
    return
  }

  codeSending.value = true
  try {
    await sendPasswordResetCode({ phone, account: normalizeResetAccount(resetForm.account) })
    resetForm.phone = phone
    ElMessage.success('验证码已发送，请查收短信')
    startCodeCountdown()
  } catch (error) {
    ElMessage.error(error?.msg || error?.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

async function handleResetPassword() {
  if (resetSubmitting.value) {
    return
  }
  const phone = normalizePhone(resetForm.phone)
  if (!phone) {
    ElMessage.warning('请输入有效的11位手机号')
    return
  }
  if (!/^\d{6}$/.test(resetForm.code)) {
    ElMessage.warning('请输入6位短信验证码')
    return
  }
  const passwordError = validateResetPassword()
  if (passwordError) {
    ElMessage.warning(passwordError)
    return
  }

  resetSubmitting.value = true
  try {
    await resetPassword({
      phone,
      account: normalizeResetAccount(resetForm.account),
      code: resetForm.code,
      newPassword: resetForm.newPassword,
      confirmPassword: resetForm.confirmPassword
    })
    ElMessage.success('密码已修改，请使用新密码登录')
    closeResetPasswordDialog()
    loginForm.username = phone
    loginForm.password = ''
  } catch (error) {
    ElMessage.error(error?.msg || error?.message || '密码修改失败')
  } finally {
    resetSubmitting.value = false
  }
}

function normalizePhone(value) {
  const digits = String(value || '').replace(/\D/g, '')
  return digits.length === 11 ? digits : ''
}

function normalizeResetAccount(value) {
  const account = String(value || '').trim()
  return account.length > 64 ? account.slice(0, 64) : account
}

function validateResetPassword() {
  const password = resetForm.newPassword || ''
  const confirmPassword = resetForm.confirmPassword || ''
  if (password.length < 8 || password.length > 64) {
    return '新密码长度需为8-64位'
  }
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    return '新密码需同时包含字母和数字'
  }
  if (password !== confirmPassword) {
    return '两次输入的新密码不一致'
  }
  return ''
}

function startCodeCountdown() {
  stopCodeCountdown()
  codeCountdown.value = 60
  codeCountdownTimer = window.setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      stopCodeCountdown()
    }
  }, 1000)
}

function stopCodeCountdown() {
  window.clearInterval(codeCountdownTimer)
  codeCountdownTimer = null
  codeCountdown.value = 0
}

async function refreshScanSession() {
  if (hasActiveWebSession()) {
    stopScanLoginFlow()
    return
  }
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
  if (!scanSession.sceneKey || pollTimer || hasActiveWebSession()) {
    if (hasActiveWebSession()) {
      stopScanLoginFlow()
    }
    return
  }

  pollTimer = window.setInterval(async () => {
    try {
      if (hasActiveWebSession()) {
        stopScanLoginFlow()
        return
      }
      const currentSceneKey = scanSession.sceneKey
      if (!currentSceneKey) {
        clearPolling()
        return
      }
      const statusData = await getScanLoginStatus({ sceneKey: currentSceneKey })
      if (hasActiveWebSession()) {
        stopScanLoginFlow()
        return
      }
      if (currentSceneKey !== scanSession.sceneKey) {
        return
      }
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
      if (scanStatus.value === 'CONFIRMED' || scanStatus.value === 'USED') {
        clearPolling()
        scanMessage.value = statusData.message || '电脑端已完成登录'
        return
      }
      if (scanStatus.value === 'EXPIRED') {
        clearPolling()
        refreshScanSession()
      }
    } catch (error) {
      clearPolling()
      if (hasActiveWebSession()) {
        stopScanLoginFlow()
        return
      }
      scanStatus.value = 'EXPIRED'
      scanMessage.value = error?.msg || error?.message || '扫码状态查询失败，正在尝试刷新二维码。'
      refreshScanSession()
    }
  }, 2000)
}

function scheduleAutoRefresh() {
  window.clearTimeout(refreshTimer)
  if (hasActiveWebSession()) {
    stopScanLoginFlow()
    return
  }
  const remainMs = Math.max(scanSession.expireAt * 1000 - Date.now(), 0)
  refreshTimer = window.setTimeout(() => {
    if (hasActiveWebSession()) {
      stopScanLoginFlow()
      return
    }
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

function hasActiveWebSession() {
  return Boolean(userStore.token)
}

function stopScanLoginFlow() {
  clearPolling()
  scanSession.sceneKey = ''
  scanSession.qrCodeDataUrl = ''
  scanSession.expireAt = 0
  scanStatus.value = 'CONFIRMED'
  scanMessage.value = '电脑端已登录'
}

function finishLogin(loginData, options = {}) {
  clearPolling()
  scanStatus.value = 'CONFIRMED'
  userStore.setLoginInfo(loginData, { remember: options.remember ?? rememberLogin.value })
  const redirect = userStore.mustChangePassword
    ? { path: '/force-password-change', query: { redirect: resolveLoginRedirect() } }
    : resolveLoginRedirect()
  window.setTimeout(() => {
    router.replace(redirect)
  }, 300)
}

function resolveLoginRedirect() {
  return normalizeLoginRedirect(route.query.redirect)
}

function triggerErrorState() {
  isError.value = true
  window.setTimeout(() => {
    isError.value = false
  }, 3000)
}

onMounted(async () => {
  if (userStore.token) {
    await router.replace(resolveLoginRedirect())
    return
  }
  refreshScanSession()
})

onUnmounted(() => {
  clearPolling()
  stopCodeCountdown()
})
</script>

<style scoped>
/* 精简了大量不必要的自定义CSS，将大部分样式抽离到了Tailwind工具类中 */

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}

.login-stage {
  min-height: 100%;
  display: grid;
  place-items: center;
  overflow-x: hidden;
  padding: clamp(1rem, 4vw, 3rem);
  background:
    radial-gradient(circle at 12% 12%, rgba(13, 148, 136, 0.16), transparent 32%),
    radial-gradient(circle at 88% 78%, rgba(245, 158, 11, 0.10), transparent 28%),
    linear-gradient(135deg, #f8fafc 0%, #eef5f4 52%, #f8fafc 100%);
}

.login-shell {
  width: min(100%, 70rem);
  min-height: 40rem;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.92);
  border-radius: 2rem;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 34px 90px rgba(15, 23, 42, 0.16);
}

.login-brand-panel {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: clamp(2rem, 4.5vw, 3.5rem);
  color: white;
  background:
    linear-gradient(150deg, rgba(15, 118, 110, 0.98), rgba(15, 76, 72, 0.99)),
    #0f766e;
}

.login-watermark {
  position: absolute;
  top: 0.5rem;
  left: 1rem;
  color: rgba(255, 255, 255, 0.045);
  font-size: clamp(6rem, 14vw, 11rem);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.1em;
  pointer-events: none;
  user-select: none;
}

.login-brand-orb {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.045);
  pointer-events: none;
}

.login-brand-orb--top {
  top: -5rem;
  right: -7rem;
  width: 18rem;
  height: 18rem;
}

.login-brand-orb--bottom {
  right: 3rem;
  bottom: -8rem;
  width: 20rem;
  height: 20rem;
}

.login-brand-lockup,
.login-brand-copy,
.login-provider {
  position: relative;
  z-index: 1;
}

.login-auth-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: clamp(2rem, 5vw, 4.25rem);
}

.login-brand-lockup {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.login-product-logo {
  display: inline-flex;
  width: 3.25rem;
  height: 3.25rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(3, 28, 26, 0.22);
}

.login-product-logo img {
  width: 88%;
  height: 88%;
  object-fit: contain;
}

.login-brand-title {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.login-brand-title small {
  color: rgba(255, 255, 255, 0.65);
  font-size: 0.62rem;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.login-brand-title strong {
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.login-brand-badge {
  display: inline-flex;
  margin-bottom: 1.1rem;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 0.42rem 0.78rem;
  color: rgba(255, 255, 255, 0.88);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  background: rgba(255, 255, 255, 0.08);
}

.login-brand-copy h1 {
  margin: 0;
  font-size: clamp(2.25rem, 4vw, 3.15rem);
  line-height: 1.12;
  letter-spacing: -0.04em;
}

.login-brand-copy > p {
  max-width: 30rem;
  margin-top: 1rem;
  line-height: 1.75;
  opacity: 0.78;
}

.login-feature-list {
  display: grid;
  gap: 0.65rem;
  margin: 1.6rem 0 0;
  padding: 0;
  list-style: none;
}

.login-feature-list li {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: rgba(255, 255, 255, 0.88);
  font-size: 0.86rem;
  font-weight: 650;
}

.login-feature-list .material-symbols-outlined {
  color: #fbbf24;
  font-size: 1.05rem;
}

.login-provider {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: rgba(255, 255, 255, 0.64);
  font-size: 0.68rem;
  letter-spacing: 0.04em;
}

.login-provider strong {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0;
}

.login-auth-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.login-auth-kicker {
  color: #0f766e;
  font-size: 0.68rem;
  font-weight: 850;
  letter-spacing: 0.16em;
}

.login-security-note {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 650;
}

.login-security-note .material-symbols-outlined {
  color: #0f766e;
  font-size: 1rem;
}

.login-account-heading {
  margin-bottom: 1.65rem;
}

.login-account-heading h3 {
  margin: 0;
  color: #0f172a;
  font-size: 2rem;
}

.login-account-heading p {
  line-height: 1.75;
  opacity: 0.82;
  color: #64748b;
}

.login-mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin-bottom: 1.75rem;
  padding: 0.25rem;
  border-radius: 0.75rem;
  background: #f1f5f9;
}

.login-mode-tabs button {
  padding: 0.7rem 0.75rem;
  border: 0;
  border-radius: 0.5rem;
  color: #475569;
  background: transparent;
  cursor: pointer;
}

.login-mode-tabs button[aria-selected='true'] {
  color: #0f766e;
  font-weight: 700;
  background: white;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
}

.login-scan-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.login-scan-panel h3 {
  margin: 0;
  color: #0f172a;
  font-size: 1.5rem;
}

.login-scan-panel > p:not(.login-scan-live-region) {
  margin: 0.5rem 0 2rem;
  color: #64748b;
}

.login-scan-live-region {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .login-brand-panel {
    min-height: 17rem;
  }

  .login-feature-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 0.75rem;
  }

  .login-provider {
    margin-top: 1.5rem;
  }
}

@media (max-width: 640px) {
  .login-stage {
    place-items: start center;
    padding: 0;
  }

  .login-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .login-brand-panel {
    min-height: 13rem;
    padding: 1.5rem;
  }

  .login-brand-copy {
    margin-top: 1.5rem;
  }

  .login-brand-badge,
  .login-feature-list,
  .login-provider,
  .login-brand-copy > p {
    display: none;
  }

  .login-brand-copy h1 {
    font-size: 2rem;
  }

  .login-auth-panel {
    padding: 2rem 1.5rem 2.5rem;
  }
}
</style>
