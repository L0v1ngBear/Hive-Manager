<template>
  <aside
      class="ys-sidebar bg-surface-container-low flex-col relative z-20 transition-all duration-300 ease-in-out border-r border-outline-variant/20"
      :class="[props.mobile ? 'flex h-full w-72' : 'hidden md:flex', props.mobile ? '' : isCollapsed ? 'w-[88px]' : 'w-64']"
  >
    <div class="h-20 flex items-center shrink-0 overflow-hidden" :class="isCollapsed ? 'justify-center px-0' : 'px-6'">
      <div class="sidebar-brand" :class="isCollapsed ? 'sidebar-brand--collapsed' : ''">
        <span class="sidebar-brand-logo" :class="{ 'sidebar-brand-logo--tenant': tenantLogoUrl }">
          <img :src="brandLogoUrl" :alt="brandLogoAlt">
        </span>
        <div v-if="!isCollapsed" class="min-w-0">
          <h1 class="sidebar-brand-title">{{ brandTitle }}</h1>
          <p class="sidebar-brand-subtitle">{{ brandSubtitle }}</p>
        </div>
      </div>
    </div>

    <div
      v-if="!isCollapsed && !userStore.isPlatformTenant"
      class="sidebar-tenant-card"
      :title="tenantName"
    >
      <img v-if="tenantLogoUrl" :src="tenantLogoUrl" :alt="`${tenantName} logo`" class="sidebar-tenant-card__logo">
      <span v-else class="material-symbols-outlined sidebar-tenant-card__icon">domain</span>
      <div class="min-w-0">
        <p class="sidebar-tenant-card__eyebrow">欢迎你</p>
        <p class="sidebar-tenant-card__name">{{ tenantName }}</p>
      </div>
    </div>

    <nav class="flex-1 py-4 overflow-y-auto scrollbar-hide" :class="isCollapsed ? 'px-2' : 'px-4'">
      <div class="space-y-2">
        <router-link
            v-for="item in primaryMenus"
            :key="item.path"
            :to="item.path"
            custom
            v-slot="{ navigate }"
        >
          <el-button
              native-type="button"
              text
              class="relative flex h-auto w-full justify-start overflow-hidden rounded-xl text-left transition-all duration-200"
              :class="linkClass(item)"
              :disabled="item.disabled"
              :title="item.disabled ? item.disabledReason : item.name"
              @click="handleMenuNavigate(item, navigate)"
          >
          <span class="material-symbols-outlined shrink-0 transition-all"
                :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
          <span class="whitespace-nowrap transition-all duration-200"
                :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{
              item.name
            }}</span>
          <el-badge
              v-if="item.path === '/function/approval' && approvalPendingCount > 0"
              :value="approvalPendingCount"
              :max="99"
              class="approval-menu-badge"
              :class="isCollapsed ? 'absolute right-1.5 top-1.5' : 'ml-auto'"
          />
          </el-button>
        </router-link>
      </div>

      <div v-if="secondaryMenus.length" class="mt-4 border-t border-outline-variant/20 pt-4">
        <el-button
            @click="toggleMore"
            text
            class="w-full rounded-xl text-on-surface-variant transition-all duration-200 hover:bg-surface-container-highest hover:text-primary"
            :class="isCollapsed ? 'flex-col items-center justify-center py-3 gap-1' : 'flex-row items-center justify-between px-4 py-3'"
        >
          <div class="flex items-center" :class="isCollapsed ? 'flex-col gap-1' : 'flex-row gap-3'">
            <span class="material-symbols-outlined shrink-0"
                  :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">apps</span>
            <span class="whitespace-nowrap"
                  :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">更多功能</span>
          </div>
          <span v-if="!isCollapsed" class="material-symbols-outlined text-[18px] transition-transform"
                :class="showMore ? 'rotate-90' : ''">chevron_right</span>
        </el-button>

        <div v-show="showMore" class="mt-2 space-y-2">
          <router-link
              v-for="item in secondaryMenus"
              :key="item.path"
              :to="item.path"
              custom
              v-slot="{ navigate }"
          >
            <el-button
                native-type="button"
                text
                class="flex h-auto w-full justify-start overflow-hidden rounded-xl text-left transition-all duration-200"
                :class="linkClass(item)"
                :disabled="item.disabled"
                :title="item.disabled ? item.disabledReason : item.name"
                @click="handleMenuNavigate(item, navigate)"
            >
            <span class="material-symbols-outlined shrink-0 transition-all"
                  :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
            <span class="whitespace-nowrap transition-all duration-200"
                  :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{
                item.name
              }}</span>
            </el-button>
          </router-link>
        </div>
      </div>
    </nav>

    <div v-if="!props.mobile" class="p-4 border-t border-outline-variant/20 flex shrink-0"
         :class="isCollapsed ? 'justify-center' : 'justify-end'">
      <el-button
          @click="toggleSidebar"
          text
          class="h-auto p-2 text-on-surface-variant hover:bg-surface-container-highest hover:text-primary"
      >
        <span class="material-symbols-outlined text-[20px] transition-transform duration-300"
              :class="isCollapsed ? '' : 'rotate-180'">keyboard_double_arrow_right</span>
        <span v-if="isCollapsed" class="text-[10px] font-bold tracking-tighter scale-90">展开</span>
      </el-button>
    </div>
  </aside>
</template>

<script setup>
import {computed, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {ElBadge, ElButton} from 'element-plus'
import {useUserStore} from '@/stores/user'
import {getApprovalSummary} from '@/views/function/approval/api/approval'
import {decorateAccessItems} from '@/utils/access'
import defaultLogo from '../../../images/logo.png'

defineOptions({name: 'Sidebar'})

const props = defineProps({
  mobile: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(!props.mobile)
const approvalPendingCount = ref(0)
const tenantName = computed(() => userStore.currentTenantName)
const tenantLogoUrl = computed(() => userStore.currentTenantLogoUrl)
const brandLogoUrl = computed(() => tenantLogoUrl.value || defaultLogo)
const brandLogoAlt = computed(() => tenantLogoUrl.value ? `${tenantName.value} logo` : '轻巢 Hive logo')
const brandTitle = computed(() => tenantLogoUrl.value && !userStore.isPlatformTenant ? tenantName.value : '轻巢 Hive')
const brandSubtitle = computed(() => tenantLogoUrl.value && !userStore.isPlatformTenant ? '企业工作台' : '业务协同系统')
const ANNOUNCEMENT_PERMISSIONS = [
  'notification:announcement:list',
  'notification:announcement:publish'
]
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const menuFeatureMap = {
  '/dashboard': 'module.dashboard',
  '/function/announcement': 'module.dashboard',
  '/function/order': 'module.order',
  '/function/installation-task': 'module.order',
  '/function/inventory': 'module.inventory',
  '/function/bad-product': 'module.badProduct',
  '/function/customer': 'module.customer',
  '/function/price': 'module.price',
  '/function/receipt': 'module.receipt',
  '/function/approval': 'module.approval',
  '/function/attendance': 'module.attendance',
  '/function/equipment': 'module.equipment',
  '/function/employee': 'module.employee',
  '/function/organization': 'module.employee',
  '/function/role': 'module.role',
  '/function/tenant': '',
  '/function/label': 'module.label',
  '/function/document': 'module.document',
  '/manual': 'module.manual'
}

const platformTenantMenu = {name: '企业授权', path: '/function/tenant', icon: 'domain', developerOnly: true}

const primaryMenus = computed(() => {
  if (userStore.isPlatformTenant) {
    return [platformTenantMenu]
  }
  return resolveMenus([
  {name: '总览大盘', path: '/dashboard', icon: 'dashboard'},
  {
    name: '订单列表',
    path: '/function/order',
    icon: 'list_alt',
    permissions: ['order:list']
  },
  {
    name: '安装任务',
    path: '/function/installation-task',
    icon: 'engineering',
    permissions: ['installation:list']
  },
  {name: '库存管理', path: '/function/inventory', icon: 'storage', permissions: ['inventory:warning:list', 'inventory:record:list', 'inventory:cloth:in', 'inventory:cloth:out']},
  {name: '质量管理', path: '/function/bad-product', icon: 'warning', permissions: ['quality:list']},
  {name: '客户管理', path: '/function/customer', icon: 'handshake', permissions: ['customer:list']},
  {name: '价格管理', path: '/function/price', icon: 'price_change', permissions: ['price:list']},
  {name: '出库单打印', path: '/function/receipt', icon: 'print', permissions: ['print:receipt:list']},
  {
    name: '审批中心',
    path: '/function/approval',
    icon: 'approval',
    permissions: ['approval:leave:list', 'approval:finance:list', 'approval:resignation:list', 'approval:leave:submit', 'approval:finance:submit', 'approval:resignation:submit', 'order:list', 'order:audit:shipment', 'order:audit:cancel', 'quality:audit']
  },
])
})

const secondaryMenus = computed(() => {
  if (userStore.isPlatformTenant) {
    return []
  }
  return resolveMenus([
  {name: '公告查看', path: '/function/announcement', icon: 'campaign', permissions: ANNOUNCEMENT_PERMISSIONS},
  {name: '考勤管理', path: '/function/attendance', icon: 'timer', permissions: ['attendance:record:list', 'attendance:rule:list', 'attendance:rule:update', 'attendance:export']},
  {name: '设备巡检', path: '/function/equipment', icon: 'qr_code_scanner', permissions: ['equipment:list', 'equipment:detail', 'equipment:inspection:list']},
  {name: '员工管理', path: '/function/employee', icon: 'people', permissions: ['employee:list']},
  {name: '部门管理', path: '/function/organization', icon: 'account_tree', permissions: ['employee:list']},
  {name: '角色管理', path: '/function/role', icon: 'settings_accessibility', permissions: ['role:list']},
  {name: '企业授权', path: '/function/tenant', icon: 'domain', developerOnly: true},
  {name: '标签模板', path: '/function/label', icon: 'sell', permissions: ['print:label:list']},
  {name: '文档管理', path: '/function/document', icon: 'folder_open', permissions: ['document:list']},
  {name: '使用手册', path: '/manual', icon: 'menu_book'},
])
})

function resolveMenus(menus) {
  if (userStore.isPlatformTenant) {
    return decorateAccessItems(userStore, menus, menuFeatureMap).filter((item) => item.path === '/function/tenant')
  }
  return decorateAccessItems(userStore, menus, menuFeatureMap)
}

function handleMenuNavigate(item, navigate) {
  if (item.disabled) {
    return
  }
  navigate()
}

const showMore = ref(false)
const secondaryPaths = computed(() => secondaryMenus.value.map((item) => item.path))

watch(
    () => secondaryMenus.value.length,
    (count) => {
      if (count === 0) {
        showMore.value = false
      }
    },
    {immediate: true}
)

watch(
    () => route.path,
    (path) => {
      if (secondaryPaths.value.some((menuPath) => path.startsWith(menuPath))) {
        showMore.value = true
      }
    },
    {immediate: true}
)

const toggleMore = () => {
  if (!secondaryMenus.value.length) {
    return
  }
  showMore.value = !showMore.value
}

const refreshApprovalPendingCount = async () => {
  if (userStore.isPlatformTenant) {
    approvalPendingCount.value = 0
    return
  }
  if (!userStore.hasAnyFeature(['module.approval']) ||
      !userStore.hasAnyPermission(['approval:leave:list', 'approval:finance:list', 'approval:resignation:list', 'order:list'])) {
    approvalPendingCount.value = 0
    return
  }
  try {
    const data = await getApprovalSummary()
    approvalPendingCount.value = Number(data?.totalPending || 0)
  } catch (error) {
    approvalPendingCount.value = 0
  }
}

watch(
    () => [userStore.permissions, userStore.features],
    () => refreshApprovalPendingCount(),
    {immediate: true, deep: true}
)

const linkClass = (item) => {
  const active = route.path.startsWith(item.path)
  if (item.disabled) {
    return [
      'cursor-not-allowed grayscale',
      isCollapsed.value ? 'flex-col items-center justify-center py-3 gap-1' : 'flex-row items-center gap-3 px-4 py-3',
    ]
  }
  return [
    active
        ? 'bg-primary text-white shadow-sm'
        : 'text-on-surface-variant hover:bg-surface-container-highest hover:text-primary',
    isCollapsed.value ? 'flex-col items-center justify-center py-3 gap-1' : 'flex-row items-center gap-3 px-4 py-3',
  ]
}
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.ys-sidebar {
  background:
      linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(255, 255, 255, 0.94)),
      radial-gradient(circle at 20% -4%, rgb(var(--ys-primary-rgb) / 0.14), transparent 38%);
  box-shadow: 18px 0 42px rgba(15, 23, 42, 0.07);
}

.sidebar-brand {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 0.75rem;
  overflow: hidden;
}

.sidebar-brand--collapsed {
  width: auto;
  justify-content: center;
}

.sidebar-brand-logo {
  display: inline-flex;
  width: 3.25rem;
  height: 3.25rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 1.1rem;
  background: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 31, 51, 0.14), inset 0 0 0 1px rgb(var(--ys-primary-rgb) / 0.08);
}

.sidebar-brand-logo--tenant {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 1.25rem;
  box-shadow: 0 18px 34px rgb(var(--ys-primary-rgb) / 0.18), inset 0 0 0 1px rgb(var(--ys-primary-rgb) / 0.14);
}

.sidebar-brand-logo img {
  width: 86%;
  height: 86%;
  object-fit: contain;
}

.sidebar-brand-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 1.05rem;
  font-weight: 950;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.sidebar-brand-subtitle {
  margin-top: 0.15rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.68rem;
  font-weight: 900;
  letter-spacing: 0.16em;
  color: var(--ys-on-surface-variant);
}

.sidebar-tenant-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.75rem;
  margin: 0 1rem 0.6rem;
  border: 1px solid rgb(var(--ys-primary-rgb) / 0.12);
  border-radius: 1.25rem;
  background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.92), var(--ys-primary-container)),
      radial-gradient(circle at 10% 10%, rgb(var(--ys-primary-rgb) / 0.12), transparent 42%);
  padding: 0.75rem;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

.sidebar-tenant-card__logo,
.sidebar-tenant-card__icon {
  width: 2.35rem;
  height: 2.35rem;
  flex: 0 0 auto;
  border-radius: 0.9rem;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgb(var(--ys-primary-rgb) / 0.08);
}

.sidebar-tenant-card__logo {
  object-fit: contain;
  padding: 0.22rem;
}

.sidebar-tenant-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ys-primary);
  font-size: 1.25rem;
}

.sidebar-tenant-card__eyebrow {
  font-size: 0.62rem;
  font-weight: 950;
  letter-spacing: 0.22em;
  color: var(--ys-on-primary-container);
}

.sidebar-tenant-card__name {
  margin-top: 0.1rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
  font-weight: 950;
  color: #0f172a;
}
</style>
