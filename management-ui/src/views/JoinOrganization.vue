<template>
  <main class="join-stage">
    <section class="join-shell">
      <aside class="join-brand-panel">
        <div class="join-brand-lockup">
          <span class="join-product-logo">
            <img src="/logo.png" :alt="`${siteConfig.productName} Logo`" />
          </span>
          <span class="join-brand-title">
            <small>HIVE WORKSPACE</small>
            <strong>蜂巢 Hive</strong>
          </span>
        </div>

        <div class="join-brand-copy">
          <span class="join-brand-badge">企业成员接入</span>
          <h1>加入企业组织</h1>
          <p>使用管理员提供的组织码创建员工账号，加入后由企业负责人统一维护岗位和权限。</p>
          <ul class="join-feature-list" aria-label="组织加入说明">
            <li><span class="material-symbols-outlined" aria-hidden="true">verified_user</span>组织码仅在有效期内可用</li>
            <li><span class="material-symbols-outlined" aria-hidden="true">badge</span>姓名同步到员工档案</li>
            <li><span class="material-symbols-outlined" aria-hidden="true">admin_panel_settings</span>岗位与权限由管理员分配</li>
          </ul>
        </div>

        <div class="join-provider">
          <span>开发与技术服务</span>
          <strong>{{ siteConfig.companyName }}</strong>
        </div>
      </aside>

      <section class="join-form-panel">
        <div class="join-mobile-lockup">
          <span class="join-mobile-logo">
            <img src="/logo.png" :alt="`${siteConfig.productName} Logo`" />
          </span>
          <strong>蜂巢 Hive</strong>
          <span>企业信息管理平台</span>
        </div>

        <button type="button" class="join-back-link" @click="goLogin">
          <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
          返回登录
        </button>

        <div class="join-heading">
          <h2>加入组织</h2>
          <p>填写成员信息并使用组织码创建账号</p>
        </div>

        <el-form :model="form" label-position="top" class="join-form" @submit.prevent="handleJoin">
          <div class="join-form-grid">
            <el-form-item label="姓名">
              <el-input
                v-model.trim="form.name"
                maxlength="30"
                placeholder="请输入真实姓名"
                size="large"
              />
            </el-form-item>

            <el-form-item label="组织码">
              <el-input
                v-model.trim="form.organizationCode"
                maxlength="32"
                placeholder="请输入管理员提供的组织码"
                size="large"
              />
            </el-form-item>
          </div>

          <el-form-item label="手机号">
            <el-input
              v-model.trim="form.phone"
              maxlength="11"
              placeholder="请输入手机号"
              size="large"
            />
          </el-form-item>

          <el-form-item label="短信验证码">
            <div class="join-code-row">
              <el-input
                v-model.trim="form.smsCode"
                maxlength="6"
                placeholder="6位验证码"
                class="min-w-0 flex-1"
                size="large"
              />
              <el-button
                class="join-code-button"
                size="large"
                :disabled="codeSending || codeCountdown > 0"
                :loading="codeSending"
                @click="handleSendCode"
              >
                {{ codeButtonText }}
              </el-button>
            </div>
          </el-form-item>

          <div class="join-form-grid">
            <el-form-item label="登录密码">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                maxlength="64"
                placeholder="至少8位，含字母数字"
                size="large"
              />
            </el-form-item>

            <el-form-item label="确认密码">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                maxlength="64"
                placeholder="再次输入密码"
                size="large"
              />
            </el-form-item>
          </div>

          <p class="join-account-note">手机号将作为网页登录账号，请妥善保管登录密码。</p>

          <el-button
            native-type="submit"
            type="primary"
            size="large"
            class="join-submit-button"
            :disabled="submitting"
            :loading="submitting"
          >
            {{ submitting ? '正在加入...' : '确认加入组织' }}
            <span v-if="!submitting" class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
          </el-button>
        </el-form>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { joinOrganization, sendOrganizationJoinCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { siteConfig } from '@/config/site'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  name: '',
  organizationCode: '',
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: ''
})

const codeSending = ref(false)
const submitting = ref(false)
const codeCountdown = ref(0)
let codeCountdownTimer = null

const codeButtonText = computed(() => {
  if (codeCountdown.value > 0) {
    return `${codeCountdown.value}s`
  }
  return codeSending.value ? '发送中' : '获取验证码'
})

async function handleSendCode() {
  if (codeSending.value || codeCountdown.value > 0) {
    return
  }
  const phone = normalizePhone(form.phone)
  if (!phone) {
    ElMessage.warning('请输入有效的11位手机号')
    return
  }

  codeSending.value = true
  try {
    await sendOrganizationJoinCode({ phone })
    form.phone = phone
    ElMessage.success('验证码已发送，请查收短信')
    startCodeCountdown()
  } catch (error) {
    ElMessage.error(error?.msg || error?.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

async function handleJoin() {
  if (submitting.value) {
    return
  }
  const validationMessage = validateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  submitting.value = true
  try {
    const loginData = await joinOrganization({
      name: form.name.trim(),
      organizationCode: form.organizationCode.trim().toUpperCase(),
      phone: normalizePhone(form.phone),
      smsCode: form.smsCode.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    userStore.setLoginInfo(loginData)
    ElMessage.success('加入成功，欢迎进入系统')
    await router.replace(userStore.mustChangePassword ? '/force-password-change' : '/dashboard')
  } catch (error) {
    ElMessage.error(error?.msg || error?.message || '加入组织失败')
  } finally {
    submitting.value = false
  }
}

function validateForm() {
  if (!form.name.trim()) {
    return '请输入姓名'
  }
  if (form.name.trim().length > 30) {
    return '姓名不能超过30个字符'
  }
  if (!/^[A-Za-z0-9]{4,32}$/.test(form.organizationCode.trim())) {
    return '请输入有效的组织码'
  }
  const phone = normalizePhone(form.phone)
  if (!phone) {
    return '请输入有效的11位手机号'
  }
  if (!/^\d{6}$/.test(form.smsCode.trim())) {
    return '请输入6位短信验证码'
  }
  if (form.password.length < 8 || form.password.length > 64) {
    return '密码长度需为8-64位'
  }
  if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
    return '密码需同时包含字母和数字'
  }
  if (form.password !== form.confirmPassword) {
    return '两次输入的密码不一致'
  }
  if (form.password === phone) {
    return '密码不能与手机号相同'
  }
  return ''
}

function normalizePhone(value) {
  const digits = String(value || '').replace(/\D/g, '')
  return digits.length === 11 ? digits : ''
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

function goLogin() {
  router.push('/login')
}

onUnmounted(() => {
  stopCodeCountdown()
})
</script>

<style scoped>
.join-stage {
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  color: #0f172a;
  background: #ffffff;
}

.join-shell {
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(36rem, 1fr);
  background: #ffffff;
}

.join-brand-panel {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  padding: clamp(2.5rem, 4vw, 3.5rem);
  color: #ffffff;
  background:
    radial-gradient(70% 60% at 86% 8%, rgba(33, 155, 145, 0.25), transparent 68%),
    radial-gradient(70% 60% at 16% 100%, rgba(15, 118, 110, 0.2), transparent 70%),
    linear-gradient(145deg, #07162f 0%, #0a2340 56%, #06142a 100%);
}

.join-brand-lockup {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.join-product-logo {
  display: inline-flex;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(2, 12, 28, 0.24);
}

.join-product-logo img,
.join-mobile-logo img {
  width: 88%;
  height: 88%;
  object-fit: contain;
}

.join-brand-title {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.join-brand-title small {
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.62rem;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.join-brand-title strong {
  font-size: 1.35rem;
  font-weight: 800;
}

.join-brand-copy {
  margin-top: auto;
  margin-bottom: clamp(3.5rem, 8vh, 6.5rem);
}

.join-brand-badge {
  display: block;
  width: fit-content;
  margin-bottom: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.48);
  padding-top: 1.4rem;
  color: rgba(255, 255, 255, 0.82);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.join-brand-copy h1 {
  margin: 0;
  font-size: clamp(2.5rem, 4vw, 3.4rem);
  line-height: 1.08;
  letter-spacing: -0.03em;
}

.join-brand-copy > p {
  max-width: 28rem;
  margin: 1.1rem 0 0;
  color: rgba(255, 255, 255, 0.66);
  font-size: 0.84rem;
  line-height: 1.85;
}

.join-feature-list {
  display: grid;
  gap: 0.55rem;
  margin: 1.4rem 0 0;
  padding: 0;
  list-style: none;
}

.join-feature-list li {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.78rem;
  font-weight: 600;
}

.join-feature-list .material-symbols-outlined {
  color: #5eead4;
  font-size: 1rem;
}

.join-provider {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.68rem;
  letter-spacing: 0.04em;
}

.join-provider strong {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0;
}

.join-form-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: clamp(2.5rem, 5vw, 4.5rem);
  background: #ffffff;
}

.join-form-panel > * {
  width: min(100%, 32rem);
  margin-right: auto;
  margin-left: auto;
}

.join-mobile-lockup {
  display: none;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.join-mobile-logo {
  display: inline-flex;
  width: 3.15rem;
  height: 3.15rem;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 0.8rem;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
}

.join-mobile-lockup strong {
  margin-top: 0.5rem;
  color: #07162f;
  font-size: 1.5rem;
  font-weight: 850;
}

.join-mobile-lockup > span:last-child {
  margin-top: 0.1rem;
  color: #7b8798;
  font-size: 0.75rem;
}

.join-back-link {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 1.25rem;
  border: 0;
  padding: 0.25rem 0;
  color: #52657b;
  font-size: 0.76rem;
  font-weight: 700;
  background: transparent;
  cursor: pointer;
}

.join-back-link:hover {
  color: #0f766e;
}

.join-back-link .material-symbols-outlined {
  font-size: 1rem;
}

.join-heading {
  margin-bottom: 1.3rem;
}

.join-heading h2 {
  margin: 0;
  color: #07162f;
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1.2;
}

.join-heading p {
  margin: 0.45rem 0 0;
  color: #7b8798;
  font-size: 0.8rem;
  line-height: 1.6;
}

.join-form {
  display: grid;
  gap: 0.1rem;
}

.join-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.join-form :deep(.el-form-item) {
  margin-bottom: 0.75rem;
}

.join-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 0.4rem;
  padding: 0;
  color: #30445f;
  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1.4;
}

.join-form :deep(.el-input__wrapper) {
  min-height: 2.65rem;
  border-radius: 0.35rem;
  padding: 0 0.8rem;
  box-shadow: 0 0 0 1px #d7dee7 inset;
  transition: box-shadow 160ms ease;
}

.join-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #9cabbc inset;
}

.join-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 3px rgba(15, 118, 110, 0.1);
}

.join-form :deep(.el-input__inner) {
  font-size: 0.8rem;
}

.join-code-row {
  display: flex;
  width: 100%;
  gap: 0.75rem;
}

.join-code-button {
  width: 8rem;
  flex: 0 0 auto;
  min-height: 2.65rem;
  border-radius: 0.35rem;
}

.join-account-note {
  margin: 0.1rem 0 0.8rem;
  color: #98a2b3;
  font-size: 0.68rem;
  line-height: 1.5;
}

.join-submit-button {
  width: 100%;
  min-height: 2.75rem;
  margin-left: 0 !important;
  border-radius: 0.35rem;
  box-shadow: 0 8px 20px rgba(15, 118, 110, 0.18);
}

.join-submit-button .material-symbols-outlined {
  margin-left: 0.3rem;
  font-size: 1.05rem;
}

@media (max-width: 900px) {
  .join-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .join-brand-panel {
    display: none;
  }

  .join-form-panel {
    min-height: 100vh;
    min-height: 100dvh;
  }
}

@media (max-width: 640px) {
  .join-form-panel {
    justify-content: flex-start;
    padding: 3.25rem 1.25rem 2rem;
  }

  .join-mobile-lockup {
    display: flex;
    margin-bottom: 2rem;
  }

  .join-form-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
  }

  .join-code-button {
    width: 7.25rem;
  }
}
</style>
