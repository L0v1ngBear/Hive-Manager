<template>
  <main class="min-h-full bg-[#f7f9fc] text-slate-900">
    <section class="relative min-h-full overflow-hidden px-4 py-10 sm:px-6 lg:px-8">
      <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_30%_8%,rgba(15,118,110,0.16),transparent_32%),linear-gradient(180deg,#ffffff_0%,#f2f6fb_100%)]"></div>

      <div class="relative mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-6xl items-center">
        <div class="grid w-full gap-8 lg:grid-cols-[0.9fr_1.1fr]">
          <aside class="rounded-[2rem] border border-white/70 bg-white/70 p-8 shadow-[0_28px_80px_rgba(15,118,110,0.12)] backdrop-blur-xl lg:p-10">
            <el-button
              class="mb-10"
              round
              @click="goLogin"
            >
              <span class="material-symbols-outlined text-lg">arrow_back</span>
              返回登录
            </el-button>

            <div class="mb-8 inline-flex items-center gap-3 rounded-2xl bg-white px-4 py-3 shadow-sm ring-1 ring-slate-100">
              <img src="../../images/logo.png" alt="蜂巢 logo" class="h-11 w-11 rounded-xl object-contain" />
              <div>
                <p class="text-sm font-black uppercase tracking-[0.28em] text-slate-400">HIVE</p>
                <p class="text-lg font-extrabold text-slate-900">组织加入</p>
              </div>
            </div>

            <h1 class="text-4xl font-black leading-tight tracking-tight text-slate-950 lg:text-5xl">
              填写姓名和组织码，
              <span class="text-primary">加入企业</span>
            </h1>
            <p class="mt-5 max-w-md text-base leading-8 text-slate-600">
              企业负责人在员工管理页生成 15 分钟有效的组织码。新成员提交姓名、手机号和验证码后，会自动创建普通员工账号，岗位和权限由企业负责人统一维护。
            </p>

            <div class="mt-10 grid gap-3 text-sm text-slate-600">
              <div class="flex items-center gap-3 rounded-2xl bg-white/80 p-4 ring-1 ring-slate-100">
                <span class="material-symbols-outlined text-primary">verified_user</span>
                <span>组织码短时有效，避免外部人员随意加入。</span>
              </div>
              <div class="flex items-center gap-3 rounded-2xl bg-white/80 p-4 ring-1 ring-slate-100">
                <span class="material-symbols-outlined text-primary">badge</span>
                <span>姓名会同步到员工档案和小程序首页展示。</span>
              </div>
            </div>
          </aside>

          <section class="rounded-[2rem] border border-white bg-white p-8 shadow-[0_28px_80px_rgba(15,118,110,0.12)] lg:p-10">
            <div class="mb-8">
              <p class="text-sm font-black uppercase tracking-[0.24em] text-primary">Join Organization</p>
              <h2 class="mt-2 text-3xl font-black text-slate-950">创建并加入账号</h2>
              <p class="mt-2 text-sm text-slate-500">手机号将作为网页登录账号，密码用于账号密码登录。</p>
            </div>

            <el-form :model="form" label-position="top" @submit.prevent="handleJoin">
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

              <el-form-item label="手机号">
                <el-input
                  v-model.trim="form.phone"
                  maxlength="11"
                  placeholder="请输入手机号"
                  size="large"
                />
              </el-form-item>

              <el-form-item label="短信验证码">
                <div class="flex gap-3">
                  <el-input
                    v-model.trim="form.smsCode"
                    maxlength="6"
                    placeholder="6位验证码"
                    class="min-w-0 flex-1"
                    size="large"
                  />
                  <el-button
                    class="w-36"
                    size="large"
                    :disabled="codeSending || codeCountdown > 0"
                    :loading="codeSending"
                    @click="handleSendCode"
                  >
                    {{ codeButtonText }}
                  </el-button>
                </div>
              </el-form-item>

              <div class="grid gap-4 sm:grid-cols-2">
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

              <el-button
                native-type="submit"
                type="primary"
                size="large"
                class="mt-4 w-full"
                :disabled="submitting"
                :loading="submitting"
              >
                {{ submitting ? '正在加入...' : '确认加入组织' }}
              </el-button>
            </el-form>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage } from 'element-plus'
import { joinOrganization, sendOrganizationJoinCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'

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
