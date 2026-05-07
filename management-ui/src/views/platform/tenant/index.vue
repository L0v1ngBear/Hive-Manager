<template>
  <div class="h-full min-h-0 flex flex-col gap-4 overflow-hidden bg-surface text-on-surface">
    <section class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-[11px] font-black tracking-[0.28em] uppercase text-primary/70">Platform</p>
          <h1 class="mt-1 text-3xl font-black tracking-tight text-primary">租户与授权管理</h1>
          <p class="mt-1 text-sm text-on-surface-variant">
            仅 super / 平台权限可见，用来管理客户套餐、到期时间、额度和客户专属定制功能。
          </p>
        </div>
        <button
          v-permission="'platform:tenant:create'"
          class="shrink-0 rounded-2xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition-all hover:opacity-90 active:scale-95"
          @click="openCreateDialog"
        >
          <span class="material-symbols-outlined mr-1.5 text-[18px] align-[-3px]">add</span>
          新增租户
        </button>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article
        v-for="card in statCards"
        :key="card.title"
        class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-[11px] font-black tracking-[0.2em] uppercase text-on-surface-variant">{{ card.title }}</p>
            <p class="mt-3 text-4xl font-black tracking-tight text-primary">{{ card.value }}</p>
            <p class="mt-2 text-xs font-bold" :class="card.subTextClass">{{ card.subText }}</p>
          </div>
          <div :class="card.iconClass" class="flex h-11 w-11 items-center justify-center rounded-2xl">
            <span class="material-symbols-outlined text-[22px]">{{ card.icon }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/15">
      <div class="flex flex-col gap-3 border-b border-outline-variant/10 px-5 py-3 xl:flex-row xl:items-center xl:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <label class="relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-on-surface-variant">search</span>
            <input
              v-model.trim="filters.keyword"
              type="text"
              placeholder="搜索租户名称、编码、联系人"
              class="w-72 rounded-2xl bg-surface-container-low py-2.5 pl-10 pr-4 text-sm outline-none ring-1 ring-transparent transition-all focus:ring-primary/30"
              @keyup.enter="reloadTenants"
            />
          </label>
          <select v-model="filters.status" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" @change="reloadTenants">
            <option value="">全部启用状态</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
            <option value="2">冻结</option>
          </select>
          <button class="rounded-2xl bg-primary px-4 py-2.5 text-sm font-black text-white" @click="reloadTenants">
            查询
          </button>
        </div>
        <div class="text-xs font-bold text-on-surface-variant">
          当前 {{ tenants.length }} 条，本页 {{ page.current }} / {{ page.pages || 1 }}，共 {{ page.total }} 条
        </div>
      </div>

      <div v-loading="loading" class="min-h-0 flex-1 overflow-auto">
        <table class="w-full min-w-[1180px] border-collapse text-left">
          <thead class="sticky top-0 z-10 bg-surface-container-low">
            <tr>
              <th class="tenant-th">租户</th>
              <th class="tenant-th">联系人</th>
              <th class="tenant-th">套餐</th>
              <th class="tenant-th">授权状态</th>
              <th class="tenant-th">额度</th>
              <th class="tenant-th">到期时间</th>
              <th class="tenant-th">启用状态</th>
              <th class="tenant-th text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="tenant in tenants" :key="tenant.id || tenant.tenantCode" class="transition-colors hover:bg-surface-container-low/55">
              <td class="px-5 py-3">
                <p class="truncate text-sm font-black text-primary">{{ tenant.tenantName || '-' }}</p>
                <p class="mt-0.5 text-xs font-bold text-on-surface-variant">{{ tenant.tenantCode || '-' }}</p>
              </td>
              <td class="px-5 py-3">
                <p class="text-sm font-bold text-on-surface">{{ tenant.contactPerson || '-' }}</p>
                <p class="mt-0.5 text-xs text-on-surface-variant">{{ tenant.contactPhone || '-' }}</p>
              </td>
              <td class="px-5 py-3">
                <span class="rounded-xl bg-primary/10 px-2.5 py-1 text-xs font-black text-primary">
                  {{ displayPackage(tenant) }}
                </span>
              </td>
              <td class="px-5 py-3">
                <span class="inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-xs font-black" :class="subscriptionBadge(tenant.subscriptionStatus)">
                  <span class="h-2 w-2 rounded-full" :class="subscriptionDot(tenant.subscriptionStatus)"></span>
                  {{ subscriptionLabel(tenant.subscriptionStatus) }}
                </span>
              </td>
              <td class="px-5 py-3 text-xs font-bold leading-6 text-on-surface-variant">
                员工 {{ formatLimit(tenant.maxUsers) }} 人<br />
                AI {{ formatLimit(tenant.aiAdviceUsedThisMonth) }} / {{ formatLimit(tenant.maxAiAdvicePerMonth) }} 次/月<br />
                存储 {{ formatLimit(tenant.maxStorageMb) }} MB
              </td>
              <td class="px-5 py-3 text-sm font-medium text-on-surface">{{ formatDateTime(tenant.subscriptionEndTime) }}</td>
              <td class="px-5 py-3">
                <span class="rounded-full px-2.5 py-1 text-xs font-black" :class="tenant.status === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'">
                  {{ tenant.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right">
                <button
                  v-permission="'platform:tenant:license'"
                  class="rounded-xl px-3 py-1.5 text-xs font-black text-primary transition-colors hover:bg-primary/10"
                  @click="openLicenseDialog(tenant)"
                >
                  调整授权
                </button>
              </td>
            </tr>
            <tr v-if="!loading && tenants.length === 0">
              <td colspan="8" class="px-5 py-12 text-center text-sm text-on-surface-variant">暂无租户数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between border-t border-outline-variant/10 px-5 py-3">
        <p class="text-xs text-on-surface-variant">授权调整会立即清理租户运行缓存，接口层会按新授权拦截。</p>
        <div class="flex items-center gap-2">
          <button class="pager-btn" :disabled="page.current <= 1 || loading" @click="changePage(page.current - 1)">上一页</button>
          <button class="pager-btn" :disabled="page.current >= page.pages || loading" @click="changePage(page.current + 1)">下一页</button>
        </div>
      </div>
    </section>

    <el-dialog v-model="createDialogVisible" title="新增租户" width="640px" destroy-on-close>
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label class="space-y-1.5">
          <span class="form-label">租户名称</span>
          <input v-model.trim="createForm.tenantName" class="tenant-input" placeholder="例如：蜂巢纺织有限公司" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">租户编码</span>
          <input v-model.trim="createForm.tenantCode" class="tenant-input uppercase" placeholder="例如：HIVE_TEXTILE" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">联系人</span>
          <input v-model.trim="createForm.contactPerson" class="tenant-input" placeholder="企业负责人或管理员" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">联系电话</span>
          <input v-model.trim="createForm.contactPhone" class="tenant-input" placeholder="手机号或座机" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">所在城市</span>
          <input v-model.trim="createForm.companyCity" class="tenant-input" placeholder="例如：杭州" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">初始密码</span>
          <input v-model.trim="createForm.password" class="tenant-input" type="password" placeholder="不填则使用系统默认密码" />
        </label>
        <label class="space-y-1.5 md:col-span-2">
          <span class="form-label">公司详细地址</span>
          <input v-model.trim="createForm.companyAddress" class="tenant-input" placeholder="配置天地图 key 后可自动解析为打卡坐标" />
        </label>
      </div>
      <template #footer>
        <button class="rounded-xl px-4 py-2 text-sm font-black text-on-surface-variant hover:bg-surface-container-low" @click="createDialogVisible = false">
          取消
        </button>
        <button class="rounded-xl bg-primary px-5 py-2 text-sm font-black text-white disabled:opacity-60" :disabled="creating" @click="submitCreateTenant">
          {{ creating ? '创建中...' : '提交创建' }}
        </button>
      </template>
    </el-dialog>

    <el-dialog v-model="licenseDialogVisible" title="调整租户授权" width="920px" destroy-on-close>
      <div class="mb-4 rounded-2xl bg-amber-50 px-4 py-3 text-xs leading-6 text-amber-800">
        这里是平台商业化和客户定制控制入口。定制功能码必须以 <b>custom.</b> 开头；未开通的租户不会看到菜单，也不能访问被注解保护的接口。
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label class="space-y-1.5">
          <span class="form-label">套餐</span>
          <select v-model="licenseForm.packageCode" class="tenant-input" @change="applyPackageDefaults">
            <option v-for="option in packageOptions" :key="option.code" :value="option.code">{{ option.name }}</option>
          </select>
        </label>
        <label class="space-y-1.5">
          <span class="form-label">授权状态</span>
          <select v-model="licenseForm.subscriptionStatus" class="tenant-input">
            <option v-for="option in subscriptionOptions" :key="option.code" :value="option.code">{{ option.name }}</option>
          </select>
        </label>
        <label class="space-y-1.5">
          <span class="form-label">开始时间</span>
          <input v-model="licenseForm.subscriptionStartTime" class="tenant-input" type="datetime-local" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">到期时间</span>
          <input v-model="licenseForm.subscriptionEndTime" class="tenant-input" type="datetime-local" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">员工上限</span>
          <input v-model.number="licenseForm.maxUsers" class="tenant-input" min="0" type="number" />
        </label>
        <label class="space-y-1.5">
          <span class="form-label">AI 建议/月</span>
          <input v-model.number="licenseForm.maxAiAdvicePerMonth" class="tenant-input" min="0" type="number" />
        </label>
        <label class="space-y-1.5 md:col-span-2">
          <span class="form-label">存储 MB</span>
          <input v-model.number="licenseForm.maxStorageMb" class="tenant-input" min="0" type="number" />
        </label>
      </div>

      <section class="mt-5 rounded-3xl bg-surface-container-low p-4 ring-1 ring-outline-variant/15">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 class="text-base font-black text-primary">功能开通清单</h3>
            <p class="mt-1 text-xs leading-5 text-on-surface-variant">基础模块默认开通，可以按客户套餐关闭；定制功能必须显式添加。</p>
          </div>
          <div class="flex items-center gap-2">
            <input
              v-model.trim="customFeatureInput"
              class="tenant-input h-10 w-72"
              placeholder="custom.tenantA.specialBoard"
              @keyup.enter="addCustomFeature"
            />
            <button class="rounded-xl bg-primary px-4 py-2 text-xs font-black text-white" @click="addCustomFeature">添加定制</button>
          </div>
        </div>

        <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            v-for="group in featureGroups"
            :key="group.category"
            class="rounded-2xl bg-white/75 p-3 ring-1 ring-outline-variant/20"
          >
            <p class="mb-2 text-xs font-black tracking-[0.2em] text-on-surface-variant">{{ group.category }}</p>
            <label
              v-for="feature in group.items"
              :key="feature.code"
              class="mb-2 flex cursor-pointer items-start gap-3 rounded-xl px-3 py-2 transition-colors hover:bg-primary-container/50"
            >
              <input
                v-model="selectedFeatureCodes"
                type="checkbox"
                :value="feature.code"
                class="mt-1 h-4 w-4 accent-primary"
                @change="syncFeatureFlagsFromSelection"
              />
              <span class="min-w-0 flex-1">
                <strong class="block text-sm text-on-surface">{{ feature.name }}</strong>
                <small class="block text-xs leading-5 text-on-surface-variant">{{ feature.code }} · {{ feature.description }}</small>
              </span>
            </label>
          </div>

          <div class="rounded-2xl bg-white/75 p-3 ring-1 ring-outline-variant/20">
            <p class="mb-2 text-xs font-black tracking-[0.2em] text-on-surface-variant">客户定制功能</p>
            <div v-if="customSelectedFeatures.length" class="flex flex-wrap gap-2">
              <button
                v-for="code in customSelectedFeatures"
                :key="code"
                class="rounded-full bg-primary/10 px-3 py-1 text-xs font-black text-primary hover:bg-rose-50 hover:text-rose-700"
                title="点击移除"
                @click="removeCustomFeature(code)"
              >
                {{ code }} ×
              </button>
            </div>
            <p v-else class="rounded-xl bg-surface-container-low px-3 py-4 text-center text-xs text-on-surface-variant">
              当前租户暂无专属定制功能。后续定制路由/接口统一使用 custom.xxx 功能码。
            </p>
          </div>
        </div>

        <label class="mt-4 block space-y-1.5">
          <span class="form-label">高级 JSON（系统会根据勾选自动生成，必要时可手动调整）</span>
          <textarea
            v-model.trim="licenseForm.featureFlags"
            class="tenant-input min-h-28 font-mono text-xs leading-5"
            spellcheck="false"
            @blur="applyFeatureJsonToSelection"
          />
        </label>
      </section>

      <template #footer>
        <button class="rounded-xl px-4 py-2 text-sm font-black text-on-surface-variant hover:bg-surface-container-low" @click="licenseDialogVisible = false">
          取消
        </button>
        <button class="rounded-xl bg-primary px-5 py-2 text-sm font-black text-white disabled:opacity-60" :disabled="licensing" @click="submitLicense">
          {{ licensing ? '保存中...' : '保存授权' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTenant, getTenantFeatureCatalog, getTenantPage, updateTenantLicense } from './api/tenant'

const packageOptions = [
  { code: 'TRIAL', name: '试用版', maxUsers: 5, aiQuota: 30, storageMb: 512 },
  { code: 'STARTER', name: '入门版', maxUsers: 10, aiQuota: 80, storageMb: 1024 },
  { code: 'STANDARD', name: '标准版', maxUsers: 30, aiQuota: 300, storageMb: 5120 },
  { code: 'PROFESSIONAL', name: '专业版', maxUsers: 80, aiQuota: 1000, storageMb: 20480 },
  { code: 'PRIVATE', name: '私有化版', maxUsers: 9999, aiQuota: 100000, storageMb: 102400 }
]

const subscriptionOptions = [
  { code: 'TRIAL', name: '试用中' },
  { code: 'ACTIVE', name: '已开通' },
  { code: 'EXPIRED', name: '已到期' },
  { code: 'SUSPENDED', name: '已暂停' }
]

const fallbackFeatureCatalog = [
  { code: 'module.dashboard', name: '总览大盘', category: '基础模块', description: '经营总览、关键指标和待办提醒', defaultEnabled: true },
  { code: 'module.order', name: '订单管理', category: '基础模块', description: '销售订单、生产订单和订单流转', defaultEnabled: true },
  { code: 'module.inventory', name: '库存管理', category: '基础模块', description: '库存流水、库存预警和出入库', defaultEnabled: true },
  { code: 'module.badProduct', name: '次品管理', category: '基础模块', description: '次品登记、处理闭环和损失跟踪', defaultEnabled: true },
  { code: 'module.customer', name: '客户管理', category: '基础模块', description: '客户档案、联系人和合作信息', defaultEnabled: true },
  { code: 'module.price', name: '价格管理', category: '基础模块', description: 'SKU 价格、客户等级价和特价', defaultEnabled: true },
  { code: 'module.receipt', name: '出库单打印', category: '基础模块', description: '出库单模板、打印确认和回执', defaultEnabled: true },
  { code: 'module.approval', name: '审批中心', category: '基础模块', description: '请假、财务等审批流程', defaultEnabled: true },
  { code: 'module.attendance', name: '考勤管理', category: '基础模块', description: '小程序打卡、规则和统计', defaultEnabled: true },
  { code: 'module.employee', name: '员工管理', category: '基础模块', description: '员工档案、组织和状态', defaultEnabled: true },
  { code: 'module.role', name: '角色管理', category: '基础模块', description: '角色权限和人员授权', defaultEnabled: true },
  { code: 'module.label', name: '标签打印', category: '基础模块', description: '标签模板和小程序打印联动', defaultEnabled: true },
  { code: 'module.document', name: '文档管理', category: '基础模块', description: '企业目录、文件和 OSS 存储', defaultEnabled: true },
  { code: 'module.manual', name: '使用手册', category: '基础模块', description: '网页端用户使用说明', defaultEnabled: true },
  { code: 'aiAdvice', name: 'AI 建议', category: '智能能力', description: '经营、员工、客户和风险建议', defaultEnabled: true },
  { code: 'advancedAi', name: '高级 AI', category: '智能能力', description: '高维建议、闭环进化和高级分析', defaultEnabled: false }
]

const featureKeyPattern = /^[A-Za-z][A-Za-z0-9_.:-]{0,100}$/
const customFeaturePattern = /^custom\.[A-Za-z0-9_.:-]{1,90}$/

const filters = reactive({
  keyword: '',
  status: ''
})
const page = reactive({
  current: 1,
  size: 20,
  total: 0,
  pages: 1
})
const tenants = ref([])
const loading = ref(false)
const createDialogVisible = ref(false)
const licenseDialogVisible = ref(false)
const creating = ref(false)
const licensing = ref(false)
const featureCatalog = ref([...fallbackFeatureCatalog])
const selectedFeatureCodes = ref([])
const customFeatureInput = ref('')
const createForm = reactive(defaultCreateForm())
const licenseForm = reactive(defaultLicenseForm())

const featureGroups = computed(() => {
  const groups = new Map()
  featureCatalog.value.forEach((feature) => {
    if (String(feature.code || '').startsWith('custom.')) {
      return
    }
    const category = feature.category || '其他能力'
    if (!groups.has(category)) {
      groups.set(category, [])
    }
    groups.get(category).push(feature)
  })
  return Array.from(groups, ([category, items]) => ({ category, items }))
})

const customSelectedFeatures = computed(() => selectedFeatureCodes.value.filter((code) => String(code).startsWith('custom.')))

const statCards = computed(() => {
  const activeCount = tenants.value.filter((tenant) => tenant.subscriptionStatus === 'ACTIVE').length
  const riskCount = tenants.value.filter((tenant) => ['EXPIRED', 'SUSPENDED'].includes(tenant.subscriptionStatus)).length
  const expiringSoon = tenants.value.filter((tenant) => isExpiringSoon(tenant.subscriptionEndTime)).length
  return [
    { title: '总租户数', value: page.total, subText: '来自真实租户表', subTextClass: 'text-on-surface-variant', icon: 'groups', iconClass: 'bg-primary/10 text-primary' },
    { title: '已开通', value: activeCount, subText: '本页活跃授权', subTextClass: 'text-emerald-600', icon: 'verified', iconClass: 'bg-emerald-100 text-emerald-700' },
    { title: '30天内到期', value: expiringSoon, subText: '建议主动续费跟进', subTextClass: 'text-amber-700', icon: 'event_busy', iconClass: 'bg-amber-100 text-amber-700' },
    { title: '受限租户', value: riskCount, subText: '到期或暂停', subTextClass: 'text-rose-700', icon: 'block', iconClass: 'bg-rose-100 text-rose-700' }
  ]
})

onMounted(async () => {
  await fetchFeatureCatalog()
  await fetchTenants()
})

function defaultCreateForm() {
  return {
    tenantName: '',
    tenantCode: '',
    tenantType: 1,
    contactPerson: '',
    contactPhone: '',
    password: '',
    companyCity: '',
    companyAddress: ''
  }
}

function defaultLicenseForm() {
  return {
    id: null,
    tenantName: '',
    packageCode: 'STANDARD',
    subscriptionStatus: 'ACTIVE',
    subscriptionStartTime: '',
    subscriptionEndTime: '',
    maxUsers: 30,
    maxAiAdvicePerMonth: 300,
    maxStorageMb: 5120,
    featureFlags: buildFeatureFlagsJson(defaultSelectedFeatures())
  }
}

async function fetchFeatureCatalog() {
  try {
    const result = await getTenantFeatureCatalog()
    if (Array.isArray(result) && result.length) {
      featureCatalog.value = result
    }
  } catch (error) {
    console.warn('功能清单加载失败，使用前端兜底清单', error)
    featureCatalog.value = [...fallbackFeatureCatalog]
  }
}

async function fetchTenants() {
  loading.value = true
  try {
    const result = await getTenantPage({
      current: page.current,
      size: page.size,
      keyword: filters.keyword || undefined,
      status: filters.status === '' ? undefined : Number(filters.status)
    })
    tenants.value = Array.isArray(result?.data) ? result.data : []
    page.total = Number(result?.total || tenants.value.length || 0)
    page.pages = Math.max(1, Number(result?.pages || Math.ceil(page.total / page.size) || 1))
    page.current = Number(result?.current || page.current || 1)
  } catch (error) {
    console.error('加载租户失败', error)
  } finally {
    loading.value = false
  }
}

function reloadTenants() {
  page.current = 1
  fetchTenants()
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > page.pages || loading.value) {
    return
  }
  page.current = nextPage
  fetchTenants()
}

function openCreateDialog() {
  Object.assign(createForm, defaultCreateForm())
  createDialogVisible.value = true
}

function validateCreateForm() {
  if (!createForm.tenantName) {
    ElMessage.warning('请填写租户名称')
    return false
  }
  if (!createForm.tenantCode) {
    ElMessage.warning('请填写租户编码')
    return false
  }
  if (!/^[A-Za-z0-9_]+$/.test(createForm.tenantCode)) {
    ElMessage.warning('租户编码仅支持字母、数字和下划线')
    return false
  }
  return true
}

async function submitCreateTenant() {
  if (!validateCreateForm() || creating.value) {
    return
  }
  creating.value = true
  try {
    await createTenant({
      ...createForm,
      tenantCode: createForm.tenantCode.toUpperCase()
    })
    ElMessage.success('租户已创建，默认进入试用授权')
    createDialogVisible.value = false
    reloadTenants()
  } catch (error) {
    console.error('租户创建失败', error)
  } finally {
    creating.value = false
  }
}

function openLicenseDialog(tenant) {
  const selectedPackage = findPackage(tenant.packageCode)
  Object.assign(licenseForm, {
    id: tenant.id,
    tenantName: tenant.tenantName || '',
    packageCode: tenant.packageCode || selectedPackage.code,
    subscriptionStatus: tenant.subscriptionStatus || 'ACTIVE',
    subscriptionStartTime: toDatetimeLocal(tenant.subscriptionStartTime),
    subscriptionEndTime: toDatetimeLocal(tenant.subscriptionEndTime),
    maxUsers: numberOrDefault(tenant.maxUsers, selectedPackage.maxUsers),
    maxAiAdvicePerMonth: numberOrDefault(tenant.maxAiAdvicePerMonth, selectedPackage.aiQuota),
    maxStorageMb: numberOrDefault(tenant.maxStorageMb, selectedPackage.storageMb),
    featureFlags: tenant.featureFlags || buildFeatureFlagsJson(defaultSelectedFeatures())
  })
  selectedFeatureCodes.value = parseFeatureFlagsToCodes(licenseForm.featureFlags)
  syncFeatureFlagsFromSelection()
  licenseDialogVisible.value = true
}

function applyPackageDefaults() {
  const selectedPackage = findPackage(licenseForm.packageCode)
  licenseForm.maxUsers = selectedPackage.maxUsers
  licenseForm.maxAiAdvicePerMonth = selectedPackage.aiQuota
  licenseForm.maxStorageMb = selectedPackage.storageMb
  if (licenseForm.packageCode === 'PROFESSIONAL' || licenseForm.packageCode === 'PRIVATE') {
    addFeatureCode('advancedAi', false)
  }
  syncFeatureFlagsFromSelection()
}

async function submitLicense() {
  if (!licenseForm.id || licensing.value) {
    return
  }
  syncFeatureFlagsFromSelection()
  if (!validateLicenseForm()) {
    return
  }
  licensing.value = true
  try {
    await updateTenantLicense({
      id: licenseForm.id,
      packageCode: licenseForm.packageCode,
      subscriptionStatus: licenseForm.subscriptionStatus,
      subscriptionStartTime: licenseForm.subscriptionStartTime || null,
      subscriptionEndTime: licenseForm.subscriptionEndTime || null,
      maxUsers: numberOrDefault(licenseForm.maxUsers, 0),
      maxAiAdvicePerMonth: numberOrDefault(licenseForm.maxAiAdvicePerMonth, 0),
      maxStorageMb: numberOrDefault(licenseForm.maxStorageMb, 0),
      featureFlags: licenseForm.featureFlags || null
    })
    ElMessage.success('授权已更新')
    licenseDialogVisible.value = false
    fetchTenants()
  } catch (error) {
    console.error('授权更新失败', error)
  } finally {
    licensing.value = false
  }
}

function validateLicenseForm() {
  const numericFields = [
    ['员工上限', licenseForm.maxUsers],
    ['AI 建议额度', licenseForm.maxAiAdvicePerMonth],
    ['存储额度', licenseForm.maxStorageMb]
  ]
  for (const [name, value] of numericFields) {
    if (!Number.isFinite(Number(value)) || Number(value) < 0) {
      ElMessage.warning(`${name}不能小于 0`)
      return false
    }
  }
  try {
    const parsed = JSON.parse(licenseForm.featureFlags || '{}')
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      ElMessage.warning('功能开关必须是 JSON 对象')
      return false
    }
  } catch (error) {
    ElMessage.warning('功能开关必须是合法 JSON')
    return false
  }
  return true
}

function applyFeatureJsonToSelection() {
  try {
    selectedFeatureCodes.value = parseFeatureFlagsToCodes(licenseForm.featureFlags)
    syncFeatureFlagsFromSelection()
  } catch (error) {
    ElMessage.warning('功能 JSON 暂未生效，请修正格式后再保存')
  }
}

function syncFeatureFlagsFromSelection() {
  selectedFeatureCodes.value = Array.from(new Set(selectedFeatureCodes.value.filter(isLegalFeatureCode)))
  licenseForm.featureFlags = buildFeatureFlagsJson(selectedFeatureCodes.value)
}

function addCustomFeature() {
  const code = customFeatureInput.value.trim()
  if (!code) {
    return
  }
  if (!customFeaturePattern.test(code)) {
    ElMessage.warning('定制功能码必须以 custom. 开头，且只能包含字母、数字、下划线、点、冒号和横线')
    return
  }
  addFeatureCode(code, true)
  customFeatureInput.value = ''
  syncFeatureFlagsFromSelection()
}

function removeCustomFeature(code) {
  selectedFeatureCodes.value = selectedFeatureCodes.value.filter((item) => item !== code)
  syncFeatureFlagsFromSelection()
}

function addFeatureCode(code, sync = true) {
  if (!isLegalFeatureCode(code)) {
    return
  }
  if (!selectedFeatureCodes.value.includes(code)) {
    selectedFeatureCodes.value.push(code)
  }
  if (sync) {
    syncFeatureFlagsFromSelection()
  }
}

function parseFeatureFlagsToCodes(flags) {
  const selected = new Set(defaultSelectedFeatures())
  if (!flags) {
    return Array.from(selected)
  }
  const root = JSON.parse(flags)
  if (!root || Array.isArray(root) || typeof root !== 'object') {
    return Array.from(selected)
  }
  walkFeatureNode(root, '', (code, enabled) => {
    const normalized = normalizeFeatureCode(code)
    if (!normalized) {
      return
    }
    if (enabled) {
      selected.add(normalized)
    } else {
      selected.delete(normalized)
    }
  })
  return Array.from(selected)
}

function walkFeatureNode(node, path, callback) {
  if (typeof node === 'boolean') {
    callback(path, node)
    return
  }
  if (Array.isArray(node)) {
    node.forEach((item) => {
      if (typeof item === 'string') {
        callback(item, true)
      }
    })
    return
  }
  if (!node || typeof node !== 'object') {
    return
  }
  Object.entries(node).forEach(([key, value]) => {
    const childPath = path ? `${path}.${key}` : key
    walkFeatureNode(value, childPath, callback)
  })
}

function buildFeatureFlagsJson(codes) {
  const selected = new Set((codes || []).filter(isLegalFeatureCode))
  const modules = {}
  featureCatalog.value
    .filter((feature) => String(feature.code).startsWith('module.'))
    .forEach((feature) => {
      modules[String(feature.code).slice('module.'.length)] = selected.has(feature.code)
    })

  const custom = {}
  selected.forEach((code) => {
    if (String(code).startsWith('custom.')) {
      custom[String(code).slice('custom.'.length)] = true
    }
  })

  return JSON.stringify({
    modules,
    aiAdvice: selected.has('aiAdvice'),
    advancedAi: selected.has('advancedAi'),
    custom
  }, null, 2)
}

function defaultSelectedFeatures() {
  return featureCatalog.value
    .filter((feature) => feature.defaultEnabled !== false)
    .map((feature) => feature.code)
    .filter(isLegalFeatureCode)
}

function normalizeFeatureCode(code) {
  if (!code || typeof code !== 'string') {
    return null
  }
  let normalized = code.trim()
  if (normalized.startsWith('modules.')) {
    normalized = `module.${normalized.slice('modules.'.length)}`
  }
  if (normalized === 'modules' || normalized === 'custom' || normalized.startsWith('platform.')) {
    return null
  }
  return isLegalFeatureCode(normalized) ? normalized : null
}

function isLegalFeatureCode(code) {
  return typeof code === 'string' && featureKeyPattern.test(code) && code !== 'module' && code !== 'custom' && !code.startsWith('platform.')
}

function findPackage(code) {
  return packageOptions.find((item) => item.code === code) || packageOptions[2]
}

function displayPackage(tenant) {
  const option = findPackage(tenant.packageCode)
  return tenant.packageName || option.name
}

function subscriptionLabel(status) {
  return subscriptionOptions.find((item) => item.code === status)?.name || '未配置'
}

function subscriptionBadge(status) {
  if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700'
  if (status === 'TRIAL') return 'bg-amber-50 text-amber-700'
  return 'bg-rose-50 text-rose-700'
}

function subscriptionDot(status) {
  if (status === 'ACTIVE') return 'bg-emerald-500'
  if (status === 'TRIAL') return 'bg-amber-500'
  return 'bg-rose-500'
}

function formatLimit(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return '-'
  }
  return number >= 9999 ? '不限' : number
}

function formatDateTime(value) {
  if (!value) {
    return '长期'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function toDatetimeLocal(value) {
  if (!value) {
    return ''
  }
  return String(value).replace(' ', 'T').slice(0, 16)
}

function numberOrDefault(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}

function isExpiringSoon(value) {
  if (!value) {
    return false
  }
  const timestamp = new Date(value).getTime()
  if (!Number.isFinite(timestamp)) {
    return false
  }
  const diffDays = (timestamp - Date.now()) / 86400000
  return diffDays >= 0 && diffDays <= 30
}
</script>

<style scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24;
}

.tenant-th {
  padding: 0.75rem 1.25rem;
  font-size: 0.6875rem;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgb(var(--color-on-surface-variant, 100 116 139));
}

.tenant-input {
  width: 100%;
  border-radius: 1rem;
  background: #f8fafc;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  outline: none;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.2);
  transition: box-shadow 0.2s ease, background-color 0.2s ease;
}

.tenant-input:focus {
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.6), 0 0 0 4px rgba(245, 158, 11, 0.12);
}

.form-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 900;
  color: #64748b;
}

.pager-btn {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  padding: 0.45rem 0.8rem;
  font-size: 0.75rem;
  font-weight: 900;
  color: #64748b;
}

.pager-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
</style>
