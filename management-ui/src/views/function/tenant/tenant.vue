<template>
  <section class="tenant-page function-page-shell">
    <header class="tenant-page__header">
      <div>
        <div class="function-page-eyebrow">
          <span class="material-symbols-outlined">domain</span>
          租户管理
        </div>
        <h1 class="function-page-title">企业租户管理</h1>
        <p class="function-page-desc">查看企业授权、启停状态、账号容量和功能开关。</p>
      </div>
      <button class="function-action-primary" :disabled="loading" @click="loadTenants">
        <span class="material-symbols-outlined" :class="{ 'animate-spin': loading }">sync</span>
        刷新
      </button>
    </header>

    <div class="tenant-grid">
      <article
        v-for="tenant in tenants"
        :key="tenant.id"
        class="tenant-card"
        :class="{ 'tenant-card--disabled': Number(tenant.status) !== 1 }"
      >
        <div class="tenant-card__top">
          <img v-if="tenant.logoUrl" :src="tenant.logoUrl" alt="公司logo" class="tenant-card__logo">
          <div class="tenant-card__title">
            <h2>{{ tenant.tenantName || '未命名企业' }}</h2>
            <p>{{ tenant.tenantCode }}</p>
          </div>
          <span class="tenant-status" :class="Number(tenant.status) === 1 ? 'tenant-status--on' : 'tenant-status--off'">
            {{ Number(tenant.status) === 1 ? '启用中' : '已停用' }}
          </span>
        </div>

        <div class="tenant-card__meta">
          <span>联系人：{{ tenant.contactPerson || '--' }}</span>
          <span>联系电话：{{ tenant.contactPhone || '--' }}</span>
          <span>企业负责人：{{ tenant.ownerName || '--' }}</span>
          <span>负责人账号：{{ tenant.ownerLoginName || '--' }}</span>
          <span>授权方案：{{ tenant.packageName || tenant.packageCode || '--' }}</span>
          <span>授权状态：{{ subscriptionLabel(tenant.subscriptionStatus) }}</span>
          <span>员工容量：{{ tenant.maxUsers ?? '--' }}</span>
          <span>存储容量：{{ tenant.maxStorageMb ?? '--' }} MB</span>
          <span>到期时间：{{ formatDateTime(tenant.subscriptionEndTime) }}</span>
        </div>

        <div class="tenant-card__features">
          <span v-for="feature in visibleFeatures(tenant)" :key="feature" class="feature-pill">{{ featureLabel(feature) }}</span>
          <span v-if="!visibleFeatures(tenant).length" class="feature-pill feature-pill--empty">未配置功能</span>
        </div>

        <div class="tenant-card__actions">
          <button class="ghost-btn ghost-btn--profile" @click="openProfileEditor(tenant)">企业信息</button>
          <button class="ghost-btn" @click="openLicenseEditor(tenant)">授权配置</button>
          <button class="ghost-btn ghost-btn--owner" @click="openOwnerEditor(tenant)">负责人账号</button>
          <button
            class="ghost-btn"
            :class="Number(tenant.status) === 1 ? 'ghost-btn--danger' : 'ghost-btn--success'"
            @click="toggleStatus(tenant)"
          >
            {{ Number(tenant.status) === 1 ? '停用企业' : '启用企业' }}
          </button>
        </div>
      </article>

      <div v-if="!loading && !tenants.length" class="empty-card">
        暂无租户数据
      </div>
    </div>

    <Teleport to="body">
      <div v-if="profileTenant" class="tenant-drawer-mask" @click.self="closeProfileEditor">
        <aside class="tenant-drawer">
          <header>
            <div>
              <h2>企业信息</h2>
              <p>{{ profileTenant.tenantName }} / {{ profileTenant.tenantCode }}</p>
            </div>
            <button class="icon-btn" @click="closeProfileEditor">
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <div class="tenant-form">
            <label>
              <span>租户编码</span>
              <input :value="profileTenant.tenantCode" readonly>
            </label>

            <div class="tenant-logo-uploader">
              <span>公司Logo</span>
              <div
                class="tenant-logo-uploader__box"
                :class="{ 'tenant-logo-uploader__box--dragging': logoDragging }"
                @click="triggerLogoUpload"
                @dragenter.prevent="logoDragging = true"
                @dragover.prevent="logoDragging = true"
                @dragleave.prevent="logoDragging = false"
                @drop.prevent="handleLogoDrop"
              >
                <img v-if="profileForm.logoUrl" :src="profileForm.logoUrl" alt="公司logo预览">
                <span v-else class="material-symbols-outlined">image</span>
              </div>
              <input
                ref="profileLogoInput"
                class="hidden"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                @change="handleLogoFileChange"
              >
              <button class="ghost-btn" :disabled="logoUploading" @click="triggerLogoUpload">
                {{ logoUploading ? '上传中...' : '上传Logo' }}
              </button>
              <small>支持点击或拖拽上传 PNG、JPG、WEBP，建议 400×400，最大 2MB。</small>
            </div>

            <label>
              <span>租户类型</span>
              <select v-model.number="profileForm.tenantType">
                <option :value="1">企业客户</option>
                <option :value="2">内部试用</option>
              </select>
            </label>

            <label class="tenant-form__wide">
              <span>企业名称</span>
              <input v-model.trim="profileForm.tenantName" maxlength="80" placeholder="请输入企业名称">
            </label>

            <label>
              <span>联系人</span>
              <input v-model.trim="profileForm.contactPerson" maxlength="50" placeholder="请输入联系人">
            </label>

            <label>
              <span>联系电话</span>
              <input v-model.trim="profileForm.contactPhone" maxlength="30" placeholder="请输入联系电话">
            </label>
          </div>

          <footer>
            <button class="ghost-btn" @click="closeProfileEditor">取消</button>
            <button class="function-action-primary" :disabled="profileSaving" @click="saveProfile">
              保存企业信息
            </button>
          </footer>
        </aside>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="editingTenant" class="tenant-drawer-mask" @click.self="closeEditor">
        <aside class="tenant-drawer">
          <header>
            <div>
              <h2>授权配置</h2>
              <p>{{ editingTenant.tenantName }} / {{ editingTenant.tenantCode }}</p>
            </div>
            <button class="icon-btn" @click="closeEditor">
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <div class="tenant-form">
            <label>
              <span>授权方案</span>
              <select v-model="licenseForm.packageCode">
                <option value="trial">试用版</option>
                <option value="basic">基础版</option>
                <option value="standard">标准版</option>
                <option value="enterprise">企业版</option>
              </select>
            </label>

            <label>
              <span>授权状态</span>
              <select v-model="licenseForm.subscriptionStatus">
                <option value="trial">试用中</option>
                <option value="active">正常服务</option>
                <option value="expired">已到期</option>
                <option value="suspended">已暂停</option>
              </select>
            </label>

            <label>
              <span>开始时间</span>
              <input v-model="licenseForm.subscriptionStartTime" type="datetime-local">
            </label>

            <label>
              <span>到期时间</span>
              <input v-model="licenseForm.subscriptionEndTime" type="datetime-local">
            </label>

            <label>
              <span>员工容量</span>
              <input v-model.number="licenseForm.maxUsers" type="number" min="0">
            </label>

            <label>
              <span>存储容量(MB)</span>
              <input v-model.number="licenseForm.maxStorageMb" type="number" min="0">
            </label>

            <label class="tenant-form__wide">
              <span>功能开关配置</span>
              <textarea v-model="licenseForm.featureFlags" rows="10" spellcheck="false"></textarea>
            </label>
          </div>

          <footer>
            <button class="ghost-btn" @click="closeEditor">取消</button>
            <button class="function-action-primary" :disabled="saving" @click="saveLicense">保存配置</button>
          </footer>
        </aside>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="ownerTenant" class="tenant-drawer-mask" @click.self="closeOwnerEditor">
        <aside class="tenant-drawer">
          <header>
            <div>
              <h2>企业负责人账号</h2>
              <p>{{ ownerTenant.tenantName }} / {{ ownerTenant.tenantCode }}</p>
            </div>
            <button class="icon-btn" @click="closeOwnerEditor">
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <div class="tenant-form">
            <p class="tenant-form__wide tenant-form__hint">
              交付给企业老板时使用。保存后该账号成为企业最高负责人，原负责人不再保留企业负责人身份。
            </p>

            <label>
              <span>负责人姓名</span>
              <input v-model.trim="ownerForm.ownerName" maxlength="50" placeholder="请输入老板或负责人姓名">
            </label>

            <label>
              <span>登录账号</span>
              <input v-model.trim="ownerForm.loginName" maxlength="64" placeholder="可用手机号或自定义账号">
            </label>

            <label>
              <span>手机号</span>
              <input v-model.trim="ownerForm.phone" maxlength="20" placeholder="用于后续登录或找回密码">
            </label>

            <label>
              <span>初始密码</span>
              <input v-model.trim="ownerForm.initialPassword" type="password" maxlength="64" placeholder="交付后首次登录需修改">
            </label>

            <label class="tenant-form__wide tenant-checkbox">
              <input v-model="ownerForm.attendanceRequired" type="checkbox">
              <span>该负责人需要参与考勤打卡</span>
            </label>
          </div>

          <footer>
            <button class="ghost-btn" @click="closeOwnerEditor">取消</button>
            <button class="function-action-primary" :disabled="ownerSaving" @click="saveOwnerAccount">
              保存负责人账号
            </button>
          </footer>
        </aside>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

defineOptions({ name: 'TenantManage' })

const userStore = useUserStore()
const tenants = ref([])
const features = ref([])
const loading = ref(false)
const saving = ref(false)
const profileSaving = ref(false)
const ownerSaving = ref(false)
const logoUploading = ref(false)
const logoDragging = ref(false)
const profileTenant = ref(null)
const editingTenant = ref(null)
const ownerTenant = ref(null)
const profileLogoInput = ref(null)
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

const featureLabelMap = computed(() => {
  const map = new Map()
  features.value.forEach((item) => map.set(item.code, item.name || item.code))
  return map
})

onMounted(async () => {
  await Promise.all([loadFeatures(), loadTenants()])
})

async function loadFeatures() {
  try {
    features.value = await listTenantFeatures()
  } catch {
    features.value = []
  }
}

async function loadTenants() {
  loading.value = true
  try {
    tenants.value = await listTenants()
  } finally {
    loading.value = false
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
  profileTenant.value = tenant
  profileForm.tenantName = tenant.tenantName || ''
  profileForm.tenantType = tenant.tenantType ?? 1
  profileForm.contactPerson = tenant.contactPerson || ''
  profileForm.contactPhone = tenant.contactPhone || ''
  profileForm.logoUrl = tenant.logoUrl || ''
}

function closeProfileEditor() {
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

function triggerLogoUpload() {
  profileLogoInput.value?.click()
}

async function handleLogoFileChange(event) {
  const file = event.target?.files?.[0]
  event.target.value = ''
  await uploadLogoFile(file)
}

async function handleLogoDrop(event) {
  logoDragging.value = false
  await uploadLogoFile(event.dataTransfer?.files?.[0])
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
  const formData = new FormData()
  formData.append('file', file)
  logoUploading.value = true
  try {
    const result = await uploadTenantLogo(profileTenant.value.id, formData)
    profileForm.logoUrl = result.logoUrl || ''
    profileTenant.value = { ...profileTenant.value, logoUrl: profileForm.logoUrl }
    syncTenantBrand(result)
    ElMessage.success('公司Logo已上传')
    await loadTenants()
  } finally {
    logoUploading.value = false
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
  const nextStatus = Number(tenant.status) === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${nextStatus === 1 ? '启用' : '停用'}「${tenant.tenantName || tenant.tenantCode}」？`,
    '确认操作',
    { type: 'warning' }
  )
  await updateTenantStatus(tenant.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? '租户已启用' : '租户已停用')
  await loadTenants()
}

async function saveOwnerAccount() {
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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tenant-page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
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
  opacity: 0.72;
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
