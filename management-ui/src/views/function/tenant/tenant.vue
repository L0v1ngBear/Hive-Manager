<template>
  <section class="tenant-page function-page-shell h-full min-h-0">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
      <div>
        <div class="function-page-eyebrow"><span class="material-symbols-outlined">domain</span>租户管理</div>
        <h1 class="function-page-title">企业租户管理</h1>
        <p class="function-page-desc">查看企业授权、启停状态、账号容量和功能开关。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadTenants">刷新</el-button>
    </header>
      <section class="function-list-panel">

    <el-result v-if="requestError" :icon="requestError.icon" :title="requestError.title" :sub-title="requestError.message"><template #extra><el-button type="primary" @click="retry">重试</el-button></template></el-result>
    <el-alert v-else-if="featureError" :title="featureError.title" :description="featureError.message" type="warning" show-icon :closable="false"><template #default><el-button link type="primary" @click="retryFeatures">重试功能目录</el-button></template></el-alert><div v-else-if="featureLoading">功能目录加载中…</div><el-empty v-else-if="featuresLoaded && !features.length" description="暂无功能目录" />
    <div v-else class="function-table-scroll">
    <el-table v-loading="loading" :data="tenants" class="tenant-table" row-key="id" empty-text="暂无租户数据">
      <el-table-column label="企业" min-width="190">
        <template #default="{ row }">
          <div class="tenant-table__company">
            <el-avatar v-if="row.logoUrl" :src="row.logoUrl" :alt="`${row.tenantName} logo`" />
            <div><strong>{{ row.tenantName || '未命名企业' }}</strong><small>{{ row.tenantCode }}</small></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="联系人" min-width="150"><template #default="{ row }">{{ row.contactPerson || '--' }}<br>{{ row.contactPhone || '--' }}</template></el-table-column>
      <el-table-column label="负责人" min-width="150"><template #default="{ row }">{{ row.ownerName || '--' }}<br>{{ row.ownerLoginName || '--' }}</template></el-table-column>
      <el-table-column label="授权" min-width="170"><template #default="{ row }">{{ row.packageName || row.packageCode || '--' }}<br>{{ subscriptionLabel(row.subscriptionStatus) }}</template></el-table-column>
      <el-table-column label="容量" min-width="130"><template #default="{ row }">用户 {{ row.maxUsers ?? '--' }}<br>存储 {{ row.maxStorageMb ?? '--' }} MB</template></el-table-column>
      <el-table-column label="到期时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.subscriptionEndTime) }}</template></el-table-column>
      <el-table-column label="功能" min-width="180">
        <template #default="{ row }"><el-tag v-for="feature in visibleFeatures(row)" :key="feature" class="tenant-feature">{{ featureLabel(feature) }}</el-tag><span v-if="!visibleFeatures(row).length">未配置功能</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '启用中' : '已停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="260">
        <template #default="{ row }"><el-button link type="primary" @click="openProfileEditor(row)">企业信息</el-button><el-button link type="primary" @click="openLicenseEditor(row)">授权配置</el-button><el-button link type="primary" @click="openOwnerEditor(row)">负责人账号</el-button><el-button link :type="Number(row.status) === 1 ? 'danger' : 'success'" :loading="statusPending.has(row.id)" :disabled="statusPending.size > 0" @click="toggleStatus(row)">{{ Number(row.status) === 1 ? '停用' : '启用' }}</el-button></template>
      </el-table-column>
      <template #empty><el-empty description="暂无租户数据" /></template>
    </el-table>
    </div>

      </section>

      <el-drawer :model-value="Boolean(profileTenant)" :with-header="false" size="680px" @update:model-value="(visible) => !visible && closeProfileEditor()">
      <template #default>
        <h2>企业信息</h2><p>{{ profileTenant?.tenantName }} / {{ profileTenant?.tenantCode }}</p>
        <el-form :model="profileForm" label-position="top" class="tenant-form">
          <el-form-item label="租户编码"><el-input :model-value="profileTenant?.tenantCode" readonly /></el-form-item>
          <el-form-item label="公司 Logo" class="tenant-form__wide"><el-upload drag action="#" accept="image/png,image/jpeg,image/webp" :auto-upload="false" :show-file-list="false" :disabled="logoUploading" :on-change="handleLogoUpload"><el-image v-if="profileForm.logoUrl" :src="profileForm.logoUrl" fit="contain" class="tenant-logo-preview" /><el-icon v-else><Picture /></el-icon><div class="el-upload__text">拖拽或点击上传 Logo</div></el-upload></el-form-item>
          <el-form-item label="租户类型"><el-select v-model="profileForm.tenantType"><el-option :value="1" label="企业客户" /><el-option :value="2" label="内部试用" /></el-select></el-form-item>
          <el-form-item label="企业名称" class="tenant-form__wide"><el-input v-model.trim="profileForm.tenantName" maxlength="80" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model.trim="profileForm.contactPerson" maxlength="50" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model.trim="profileForm.contactPhone" maxlength="30" /></el-form-item>
        </el-form>
      </template>
      <template #footer><el-button @click="closeProfileEditor">取消</el-button><el-button type="primary" :loading="profileSaving" @click="saveProfile">保存企业信息</el-button></template>
    </el-drawer>

    <el-drawer :model-value="Boolean(editingTenant)" :with-header="false" size="680px" @update:model-value="(visible) => !visible && closeEditor()">
      <h2>授权配置</h2><p>{{ editingTenant?.tenantName }} / {{ editingTenant?.tenantCode }}</p>
      <el-form :model="licenseForm" label-position="top" class="tenant-form">
        <el-form-item label="授权方案"><el-select v-model="licenseForm.packageCode"><el-option value="trial" label="试用版" /><el-option value="basic" label="基础版" /><el-option value="standard" label="标准版" /><el-option value="enterprise" label="企业版" /></el-select></el-form-item>
        <el-form-item label="授权状态"><el-select v-model="licenseForm.subscriptionStatus"><el-option value="trial" label="试用中" /><el-option value="active" label="正常服务" /><el-option value="expired" label="已到期" /><el-option value="suspended" label="已暂停" /></el-select></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="licenseForm.subscriptionStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm" /></el-form-item>
        <el-form-item label="到期时间"><el-date-picker v-model="licenseForm.subscriptionEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm" /></el-form-item>
        <el-form-item label="员工容量"><el-input-number v-model="licenseForm.maxUsers" :min="0" :precision="0" /></el-form-item>
        <el-form-item label="存储容量(MB)"><el-input-number v-model="licenseForm.maxStorageMb" :min="0" :precision="0" /></el-form-item>
        <el-form-item label="功能开关配置" class="tenant-form__wide"><el-input v-model="licenseForm.featureFlags" type="textarea" :rows="10" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="closeEditor">取消</el-button><el-button type="primary" :loading="saving" @click="saveLicense">保存配置</el-button></template>
    </el-drawer>

    <el-drawer :model-value="Boolean(ownerTenant)" :with-header="false" size="680px" @update:model-value="(visible) => !visible && closeOwnerEditor()">
      <h2>企业负责人账号</h2><p>{{ ownerTenant?.tenantName }} / {{ ownerTenant?.tenantCode }}</p>
      <el-form :model="ownerForm" label-position="top" class="tenant-form"><el-alert title="保存后该账号成为企业最高负责人，原负责人不再保留企业负责人身份。" type="warning" :closable="false" class="tenant-form__wide" /><el-form-item label="负责人姓名"><el-input v-model.trim="ownerForm.ownerName" maxlength="50" /></el-form-item><el-form-item label="登录账号"><el-input v-model.trim="ownerForm.loginName" maxlength="64" /></el-form-item><el-form-item label="手机号"><el-input v-model.trim="ownerForm.phone" maxlength="20" /></el-form-item><el-form-item label="初始密码"><el-input v-model.trim="ownerForm.initialPassword" type="password" maxlength="64" show-password /></el-form-item><el-form-item label="考勤" class="tenant-form__wide"><el-checkbox-group v-model="ownerAttendance"><el-checkbox value="required">该负责人需要参与考勤打卡</el-checkbox></el-checkbox-group><el-switch v-model="ownerForm.attendanceRequired" /></el-form-item></el-form>
      <template #footer><el-button @click="closeOwnerEditor">取消</el-button><el-button type="primary" :loading="ownerSaving" @click="saveOwnerAccount">保存负责人账号</el-button></template>
    </el-drawer>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElAvatar,
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDatePicker,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElResult,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElUpload
} from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import {
  listTenantFeatures,
  listTenants,
  reassignTenantOwnerAccount,
  uploadTenantLogo,
  updateTenantProfile,
  updateTenantLicense,
  updateTenantStatus
} from '@/api/tenantManage'
import { useUserStore } from '@/stores/user'
import { createLatestLoadingController } from './tenantRequestState.js'

defineOptions({ name: 'TenantManage' })

const userStore = useUserStore()
const tenants = ref([])
const features = ref([])
const loading = ref(false)
const requestError = ref(null)
const featureError = ref(null)
const featureLoading = ref(false), featuresLoaded = ref(false)
const statusPending = reactive(new Set())
const saving = ref(false)
const profileSaving = ref(false)
const ownerSaving = ref(false)
const logoUploading = ref(false)
const logoRequest = createLatestLoadingController((value) => { logoUploading.value = value })
let tenantListRequestId = 0, featureRequestId = 0
const profileTenant = ref(null)
const editingTenant = ref(null)
const ownerTenant = ref(null)
const profileForm = reactive({
  tenantName: '',
  tenantType: 1,
  contactPerson: '',
  contactPhone: '',
  logoUrl: ''
})
const licenseForm = reactive({
  packageCode: 'trial',
  subscriptionStatus: 'trial',
  subscriptionStartTime: '',
  subscriptionEndTime: '',
  maxUsers: 0,
  maxStorageMb: 0,
  featureFlags: ''
})
const ownerForm = reactive({
  ownerUserId: null,
  ownerName: '',
  loginName: '',
  phone: '',
  initialPassword: '',
  attendanceRequired: false
})
const ownerAttendance = computed({
  get: () => ownerForm.attendanceRequired ? ['required'] : [],
  set: (value) => { ownerForm.attendanceRequired = value.includes('required') }
})

const featureLabelMap = computed(() => {
  const map = new Map()
  features.value.forEach((item) => map.set(item.code, item.name || item.code))
  return map
})

onMounted(async () => {
  await Promise.all([loadFeatures(), loadTenants()])
})

async function loadFeatures() {
  const requestId = ++featureRequestId
  featureLoading.value = true
  featureError.value = null
  try {
    const result = await listTenantFeatures()
    if (requestId !== featureRequestId) return
    features.value = result
    featuresLoaded.value = true
  } catch (error) {
    if (requestId !== featureRequestId) return
    features.value = []
    featureError.value = errorState(error, '功能目录加载失败')
  } finally {
    if (requestId === featureRequestId) featureLoading.value = false
  }
}

async function loadTenants() {
  const requestId = ++tenantListRequestId
  loading.value = true
  requestError.value = null
  try {
    const result = await listTenants()
    if (requestId !== tenantListRequestId) return
    tenants.value = result
  } catch (error) {
    if (requestId !== tenantListRequestId) return
    tenants.value = []
    requestError.value = errorState(error, '租户列表加载失败')
  } finally {
    if (requestId === tenantListRequestId) loading.value = false
  }
}

function openLicenseEditor(tenant) {
  editingTenant.value = tenant
  licenseForm.packageCode = tenant.packageCode || 'trial'
  licenseForm.subscriptionStatus = tenant.subscriptionStatus || 'trial'
  licenseForm.subscriptionStartTime = toDateTimeLocal(tenant.subscriptionStartTime)
  licenseForm.subscriptionEndTime = toDateTimeLocal(tenant.subscriptionEndTime)
  licenseForm.maxUsers = tenant.maxUsers ?? 0
  licenseForm.maxStorageMb = tenant.maxStorageMb ?? 0
  licenseForm.featureFlags = formatFeatureFlags(tenant.featureFlags)
}

function openProfileEditor(tenant) {
  logoRequest.invalidate()
  profileTenant.value = tenant
  profileForm.tenantName = tenant.tenantName || ''
  profileForm.tenantType = tenant.tenantType ?? 1
  profileForm.contactPerson = tenant.contactPerson || ''
  profileForm.contactPhone = tenant.contactPhone || ''
  profileForm.logoUrl = tenant.logoUrl || ''
}

function closeProfileEditor() {
  logoRequest.invalidate()
  profileTenant.value = null
}

function closeEditor() {
  editingTenant.value = null
}

function openOwnerEditor(tenant) {
  ownerTenant.value = tenant
  ownerForm.ownerUserId = tenant.ownerUserId || null
  ownerForm.ownerName = tenant.ownerName || tenant.contactPerson || ''
  ownerForm.loginName = tenant.ownerLoginName || ''
  ownerForm.phone = ''
  ownerForm.initialPassword = ''
  ownerForm.attendanceRequired = Number(tenant.ownerAttendanceRequired) === 1
}

function closeOwnerEditor() {
  ownerTenant.value = null
}

async function saveLicense() {
  if (saving.value) return
  if (!editingTenant.value) {
    return
  }
  saving.value = true
  try {
    await updateTenantLicense(editingTenant.value.id, {
      id: editingTenant.value.id,
      packageCode: licenseForm.packageCode,
      subscriptionStatus: licenseForm.subscriptionStatus,
      subscriptionStartTime: fromDateTimeLocal(licenseForm.subscriptionStartTime),
      subscriptionEndTime: fromDateTimeLocal(licenseForm.subscriptionEndTime),
      maxUsers: numericOrZero(licenseForm.maxUsers),
      maxStorageMb: numericOrZero(licenseForm.maxStorageMb),
      featureFlags: licenseForm.featureFlags
    })
    ElMessage.success('授权配置已保存')
    closeEditor()
    await loadTenants()
  } finally {
    saving.value = false
  }
}

async function saveProfile() {
  if (profileSaving.value) return
  if (!profileTenant.value) {
    return
  }
  if (!profileForm.tenantName) {
    ElMessage.warning('请填写企业名称')
    return
  }
  profileSaving.value = true
  try {
    const result = await updateTenantProfile(profileTenant.value.id, {
      tenantName: profileForm.tenantName,
      tenantType: profileForm.tenantType,
      contactPerson: profileForm.contactPerson,
      contactPhone: profileForm.contactPhone
    })
    syncTenantBrand(result)
    ElMessage.success('企业信息已保存')
    closeProfileEditor()
    await loadTenants()
  } finally {
    profileSaving.value = false
  }
}

async function handleLogoUpload(uploadFile) {
  await uploadLogoFile(uploadFile.raw)
}

function retry() { return loadTenants() }
function retryFeatures() { return loadFeatures() }
function errorState(error, title) {
  const status = Number(error?.response?.status)
  if (status === 401) return { icon: 'warning', title: '登录已失效', message: '请重新登录后重试。' }
  if (status === 403) return { icon: 'warning', title: '无权访问', message: '当前账号不是平台租户账号。' }
  if (status >= 500) return { icon: 'error', title, message: '服务器处理失败，请稍后重试。' }
  return { icon: 'error', title, message: '网络异常，请检查连接后重试。' }
}

async function uploadLogoFile(file) {
  if (!file || !profileTenant.value) {
    return
  }
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('公司Logo不能超过2MB')
    return
  }
  const targetTenant = profileTenant.value
  const requestId = logoRequest.begin()
  const formData = new FormData()
  formData.append('file', file)
  try {
    const result = await uploadTenantLogo(targetTenant.id, formData)
    if (!logoRequest.isCurrent(requestId) || profileTenant.value?.id !== targetTenant.id) return
    profileForm.logoUrl = result.logoUrl || ''
    profileTenant.value = { ...profileTenant.value, logoUrl: profileForm.logoUrl }
    syncTenantBrand(result)
    ElMessage.success('公司Logo已上传')
    await loadTenants()
  } finally {
    logoRequest.finish(requestId)
  }
}

function syncTenantBrand(tenant) {
  if (!tenant || tenant.tenantCode !== userStore.currentTenantCode) {
    return
  }
  userStore.updateTenantBrand({
    tenantName: tenant.tenantName,
    tenantLogoUrl: tenant.logoUrl
  })
}

async function toggleStatus(tenant) {
  if (statusPending.size > 0) return
  const nextStatus = Number(tenant.status) === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${nextStatus === 1 ? '启用' : '停用'}「${tenant.tenantName || tenant.tenantCode}」？`,
    '确认操作',
    { type: 'warning' }
  )
  statusPending.add(tenant.id)
  try {
    await updateTenantStatus(tenant.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '租户已启用' : '租户已停用')
    await loadTenants()
  } finally {
    statusPending.delete(tenant.id)
  }
}

async function saveOwnerAccount() {
  if (ownerSaving.value) return
  if (!ownerTenant.value) {
    return
  }
  if (!ownerForm.ownerName) {
    ElMessage.warning('请填写负责人姓名')
    return
  }
  if (!ownerForm.loginName && !ownerForm.phone) {
    ElMessage.warning('请填写登录账号或手机号')
    return
  }
  if (!ownerForm.initialPassword || ownerForm.initialPassword.length < 6) {
    ElMessage.warning('初始密码至少6位')
    return
  }
  await ElMessageBox.confirm(
    `确认将「${ownerForm.ownerName}」设置为「${ownerTenant.value.tenantName || ownerTenant.value.tenantCode}」的企业负责人账号？`,
    '重新分配负责人',
    { type: 'warning' }
  )
  ownerSaving.value = true
  try {
    await reassignTenantOwnerAccount(ownerTenant.value.id, {
      ownerUserId: ownerForm.ownerUserId,
      ownerName: ownerForm.ownerName,
      loginName: ownerForm.loginName,
      phone: ownerForm.phone,
      initialPassword: ownerForm.initialPassword,
      attendanceRequired: ownerForm.attendanceRequired
    })
    ElMessage.success('企业负责人账号已重新分配')
    closeOwnerEditor()
    await loadTenants()
  } finally {
    ownerSaving.value = false
  }
}

function visibleFeatures(tenant) {
  return Array.isArray(tenant.enabledFeatures) ? tenant.enabledFeatures.slice(0, 10) : []
}

function featureLabel(code) {
  return featureLabelMap.value.get(code) || code
}

function subscriptionLabel(value) {
  const map = {
    trial: '试用中',
    active: '正常服务',
    expired: '已到期',
    suspended: '已暂停'
  }
  return map[value] || value || '--'
}

function toDateTimeLocal(value) {
  if (!value) {
    return ''
  }
  return String(value).replace(' ', 'T').slice(0, 16)
}

function fromDateTimeLocal(value) {
  if (!value) {
    return null
  }
  return `${value}:00`
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--'
}

function formatFeatureFlags(value) {
  if (!value) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function numericOrZero(value) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : 0
}
</script>

<style scoped>
.tenant-page {
  --function-section-gap: 1rem;
}

.tenant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 20px;
}

.tenant-card,
.empty-card {
  border: 1px solid rgba(15, 47, 111, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 20px 60px rgba(15, 47, 111, 0.08);
  padding: 24px;
}

.tenant-card--disabled {
  border-color: var(--ys-disabled-bg);
  background: var(--ys-disabled-bg);
  color: var(--ys-disabled-text);
  box-shadow: none;
  opacity: 1;
}

.tenant-card--disabled * {
  color: var(--ys-disabled-text);
}

.tenant-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.tenant-card__logo {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  border-radius: 16px;
  object-fit: contain;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(15, 47, 111, 0.1), 0 12px 28px rgba(15, 47, 111, 0.08);
}

.tenant-card__title {
  min-width: 0;
  flex: 1;
}

.tenant-card h2 {
  margin: 0;
  color: #0f233d;
  font-size: 22px;
  font-weight: 900;
}

.tenant-card p {
  margin: 6px 0 0;
  color: #64748b;
  font-weight: 700;
}

.tenant-status,
.feature-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 900;
}

.tenant-status--on {
  background: #dcfce7;
  color: #047857;
}

.tenant-status--off {
  background: #fee2e2;
  color: #b91c1c;
}

.tenant-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  margin-top: 22px;
  color: #475569;
  font-weight: 700;
}

.tenant-card__features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}

.feature-pill {
  background: #eef6ff;
  color: #17456f;
}

.feature-pill--empty {
  background: #f8fafc;
  color: #94a3b8;
}

.tenant-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.ghost-btn,
.icon-btn {
  border: 1px solid rgba(15, 47, 111, 0.14);
  border-radius: 16px;
  background: #fff;
  color: #17456f;
  font-weight: 900;
  transition: all 0.18s ease;
}

.ghost-btn {
  padding: 12px 18px;
}

.ghost-btn:hover,
.icon-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 30px rgba(15, 47, 111, 0.12);
}

.ghost-btn--danger {
  color: #b91c1c;
}

.ghost-btn--success {
  color: #047857;
}

.ghost-btn--profile {
  color: #0f233d;
  background: #f8fafc;
}

.ghost-btn--owner {
  color: #17456f;
  background: #eef6ff;
}

.tenant-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.32);
  backdrop-filter: blur(10px);
}

.tenant-drawer {
  width: min(720px, 100vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-shadow: -28px 0 80px rgba(15, 23, 42, 0.18);
}

.tenant-drawer header,
.tenant-drawer footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 32px;
  border-bottom: 1px solid rgba(15, 47, 111, 0.1);
}

.tenant-drawer footer {
  border-top: 1px solid rgba(15, 47, 111, 0.1);
  border-bottom: none;
}

.tenant-drawer h2 {
  margin: 0;
  color: #0f233d;
  font-size: 28px;
  font-weight: 900;
}

.tenant-drawer p {
  margin: 6px 0 0;
  color: #64748b;
}

.icon-btn {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
}

.tenant-form {
  flex: 1;
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  padding: 30px 32px;
}

.tenant-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-weight: 900;
}

.tenant-form input,
.tenant-form select,
.tenant-form textarea {
  width: 100%;
  border: 1px solid rgba(15, 47, 111, 0.12);
  border-radius: 16px;
  background: #f8fafc;
  color: #0f233d;
  font-weight: 800;
  padding: 14px 16px;
  outline: none;
}

.tenant-form textarea {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.tenant-form input[readonly] {
  color: #64748b;
  cursor: not-allowed;
}

.tenant-logo-uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-weight: 900;
}

.tenant-logo-uploader__box {
  width: 112px;
  height: 112px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px dashed rgba(15, 47, 111, 0.22);
  border-radius: 22px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.tenant-logo-uploader__box:hover,
.tenant-logo-uploader__box--dragging {
  border-color: rgba(31, 111, 255, 0.72);
  box-shadow: 0 16px 34px rgba(31, 111, 255, 0.14);
  transform: translateY(-1px);
}

.tenant-logo-uploader__box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 10px;
}

.tenant-logo-uploader__box .material-symbols-outlined {
  color: #94a3b8;
  font-size: 34px;
}

.tenant-logo-uploader small {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.tenant-form__wide {
  grid-column: 1 / -1;
}

.tenant-form__hint {
  margin: 0;
  border: 1px solid rgba(15, 47, 111, 0.12);
  border-radius: 18px;
  background: #eef6ff;
  color: #17456f;
  font-weight: 800;
  line-height: 1.7;
  padding: 16px 18px;
}

.tenant-checkbox {
  flex-direction: row !important;
  align-items: center;
  border: 1px solid rgba(15, 47, 111, 0.12);
  border-radius: 16px;
  background: #f8fafc;
  padding: 14px 16px;
}

.tenant-checkbox input {
  width: 18px;
  height: 18px;
  padding: 0;
}

@media (max-width: 768px) {
  .tenant-page__header {
    align-items: stretch;
    flex-direction: column;
  }

  .tenant-card__meta,
  .tenant-form {
    grid-template-columns: 1fr;
  }
}
</style>
