<template>
  <header ref="navbarRef" class="ys-navbar min-h-16 md:h-20 bg-surface flex flex-wrap md:flex-nowrap items-center justify-between gap-3 px-3 py-3 md:px-8 md:py-0 shrink-0 relative z-30 isolate overflow-visible">
    <el-button
      class="md:hidden p-2 text-on-surface-variant rounded-full hover:bg-surface-container-highest"
      text
      circle
      @click="emit('toggle-mobile-menu')"
    >
      <span class="material-symbols-outlined">menu</span>
    </el-button>

    <div class="flex min-w-0 flex-1 items-center gap-3 md:gap-6 max-w-2xl md:ml-0">
      <h2 class="text-xl font-bold text-on-surface hidden lg:block">{{ pageTitle }}</h2>

      <div v-if="!userStore.isPlatformTenant" class="relative flex-1 group max-w-md hidden md:block">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg group-focus-within:text-primary transition-colors">search</span>
        <el-input
          v-model.trim="keyword"
          placeholder="搜索订单、库存、客户、员工..."
          clearable
          class="w-full"
          @focus="searchPanelOpen = true"
          @keydown.enter.prevent="goFirstSearchResult"
        />
        <div
          v-if="searchPanelOpen && filteredMenus.length"
          class="absolute left-0 right-0 top-[calc(100%+10px)] z-[1200] overflow-hidden rounded-2xl border border-outline-variant/40 bg-white shadow-2xl shadow-primary/10"
        >
          <el-button
            v-for="item in filteredMenus"
            :key="`${item.path}-${item.label || item.name}`"
            text
            class="flex h-auto w-full justify-start gap-3 px-4 py-3 text-left text-sm transition-colors"
            :class="searchItemClass(item)"
            :disabled="item.disabled"
            :title="item.disabled ? item.disabledReason : item.desc"
            @click="handleSearchItemClick(item)"
          >
            <span class="material-symbols-outlined text-[20px]" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-primary'">{{ item.icon }}</span>
            <span class="min-w-0 flex-1">
              <strong class="block truncate" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-on-surface'">{{ item.label || item.name }}</strong>
              <small class="block truncate" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-on-surface-variant'">{{ item.disabled ? item.disabledReason : item.desc }}</small>
            </span>
            <span class="material-symbols-outlined text-on-surface-variant text-[18px]">arrow_forward</span>
          </el-button>
        </div>
      </div>
    </div>

    <div class="flex items-center gap-2 md:gap-4">
      <div class="tenant-chip tenant-chip--branded" :title="brandConfig.companyName">
        <span class="tenant-chip__logo-frame">
          <img :src="brandConfig.logoUrl" :alt="brandConfig.logoAlt" class="tenant-chip__logo brand-logo-image">
        </span>
        <span class="tenant-chip__text">
          <span class="tenant-chip__name">{{ brandConfig.companyName }}</span>
        </span>
      </div>

      <el-button
        v-if="!userStore.isPlatformTenant"
        class="md:hidden w-10 h-10 flex items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-container-highest transition-colors"
        text
        circle
        @click.stop="toggleMobileSearch"
      >
        <span class="material-symbols-outlined">search</span>
      </el-button>

      <el-popover
        v-if="!userStore.isPlatformTenant"
        v-model:visible="notificationOpen"
        placement="bottom-end"
        trigger="click"
        :width="320"
        :teleported="false"
        popper-class="navbar-notification-popover"
        @show="handleNotificationShow"
      >
        <template #reference>
          <el-badge :value="pendingNotifications.length" :hidden="pendingNotifications.length === 0" :max="99">
            <el-button text circle aria-label="待办通知">
              <span class="material-symbols-outlined">notifications</span>
            </el-button>
          </el-badge>
        </template>
        <div class="overflow-hidden">
          <div class="flex items-center justify-between border-b border-outline-variant/30 px-4 py-3">
            <div>
              <p class="text-sm font-black text-on-surface">待办通知</p>
              <p class="text-xs text-on-surface-variant">{{ pendingNotifications.length ? `有 ${pendingNotifications.length} 条需要处理` : '当前没有新的待办' }}</p>
            </div>
            <el-button
              v-permission="'notification:announcement:publish'"
              link
              type="primary"
              @click="refreshNotifications(true)"
            >
              刷新
            </el-button>
          </div>
          <div class="max-h-[360px] overflow-y-auto p-2">
            <div
              v-for="item in pendingNotifications"
              :key="item.key"
              class="rounded-xl px-3 py-3 transition-colors hover:bg-primary-container"
            >
              <el-button text class="h-auto w-full justify-start p-0 text-left" @click="openNotification(item)">
                <div class="flex items-center justify-between gap-3">
                  <strong class="text-sm text-on-surface">{{ item.title }}</strong>
                  <span class="rounded-full bg-primary-container px-2 py-0.5 text-[10px] font-bold text-primary">{{ item.type }}</span>
                </div>
                <p class="mt-1 line-clamp-4 text-xs leading-5 text-on-surface-variant">{{ item.desc }}</p>
              </el-button>
              <div class="mt-2 flex items-center gap-2">
                <el-button
                  type="primary"
                  size="small"
                  round
                  @click.stop="closeNotification(item, 'DONE')"
                >
                  完成
                </el-button>
                <el-button
                  size="small"
                  round
                  @click.stop="closeNotification(item, 'IGNORED')"
                >
                  跳过
                </el-button>
              </div>
            </div>
            <div v-if="!pendingNotifications.length" class="px-4 py-8 text-center">
              <span class="material-symbols-outlined text-4xl text-primary">task_alt</span>
              <p class="mt-2 text-sm font-bold text-on-surface">待办已清空</p>
              <p class="text-xs text-on-surface-variant">审批和业务提醒会展示在这里。</p>
            </div>
          </div>
        </div>
      </el-popover>

      <el-dropdown class="pl-2 md:pl-4 border-l border-outline-variant/30" trigger="click" @visible-change="userMenuOpen = $event">
        <el-button text class="h-auto px-2 py-1">
          <div class="hidden md:block text-right">
            <p class="text-sm font-bold text-on-surface">{{ displayName }}</p>
            <p class="text-xs text-on-surface-variant">{{ roleLabel }}</p>
          </div>
          <span class="local-avatar">{{ avatarText }}</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <div class="user-menu-brand border-b border-outline-variant/30 px-4 py-4">
              <img :src="brandConfig.logoUrl" :alt="brandConfig.logoAlt" :title="brandConfig.companyName" class="user-menu-brand__logo brand-logo-image">
              <div class="min-w-0">
                <p class="truncate text-sm font-black text-on-surface">{{ displayName }}</p>
                <p class="mt-1 truncate text-xs font-bold text-on-surface-variant">组织：{{ tenantName }}</p>
              </div>
            </div>
            <el-dropdown-item v-if="canAccessSearchTarget('/dashboard')" :disabled="isSearchTargetDisabled('/dashboard')" @click="goSearchTarget('/dashboard')">
              <span class="material-symbols-outlined">dashboard</span>回到总览大盘
            </el-dropdown-item>
            <el-dropdown-item v-if="canAccessSearchTarget('/function/approval')" :disabled="isSearchTargetDisabled('/function/approval')" @click="goApproval">
              <span class="material-symbols-outlined">approval</span>查看审批中心
            </el-dropdown-item>
            <el-dropdown-item v-if="canAccessSearchTarget('/manual')" :disabled="isSearchTargetDisabled('/manual')" @click="goSearchTarget('/manual')">
              <span class="material-symbols-outlined">menu_book</span>使用手册
            </el-dropdown-item>
            <el-dropdown-item divided class="text-error" @click="handleLogout">
              <span class="material-symbols-outlined">logout</span>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div v-if="!userStore.isPlatformTenant && mobileSearchOpen" class="w-full md:hidden">
      <div class="relative">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
        <el-input
          v-model.trim="keyword"
          placeholder="搜索订单、库存、客户、员工..."
          clearable
          class="w-full"
          @focus="searchPanelOpen = true"
          @keydown.enter.prevent="goFirstSearchResult"
        />
        <div
          v-if="searchPanelOpen && filteredMenus.length"
          class="absolute left-0 right-0 top-[calc(100%+10px)] z-[1200] max-h-[60vh] overflow-y-auto rounded-2xl border border-outline-variant/40 bg-white shadow-2xl shadow-primary/10"
        >
          <el-button
            v-for="item in filteredMenus"
            :key="`mobile-${item.path}-${item.label || item.name}`"
            text
            class="flex h-auto w-full justify-start gap-3 px-4 py-3 text-left text-sm transition-colors"
            :class="searchItemClass(item)"
            :disabled="item.disabled"
            :title="item.disabled ? item.disabledReason : item.desc"
            @click="handleSearchItemClick(item)"
          >
            <span class="material-symbols-outlined text-[20px]" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-primary'">{{ item.icon }}</span>
            <span class="min-w-0 flex-1">
              <strong class="block truncate" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-on-surface'">{{ item.label || item.name }}</strong>
              <small class="block truncate" :class="item.disabled ? 'text-[var(--ys-disabled-text)]' : 'text-on-surface-variant'">{{ item.disabled ? item.disabledReason : item.desc }}</small>
            </span>
            <span class="material-symbols-outlined text-on-surface-variant text-[18px]">arrow_forward</span>
          </el-button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ElBadge,
  ElButton,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElPopover
} from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { closeNotificationTask, getUnreadNotifications, markNotificationRead, syncNotifications } from '@/api/notification.js'
import {decorateAccessItems, resolveAccessState} from '@/utils/access'
import {brandConfig} from '@/config/brand'

defineOptions({ name: 'Navbar' });

const emit = defineEmits(['toggle-mobile-menu'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const navbarRef = ref(null)
const ANNOUNCEMENT_PERMISSIONS = [
  'notification:announcement:list',
  'notification:announcement:publish'
]
const menuFeatureMap = {
  '/dashboard': 'module.dashboard',
  '/function/announcement': 'module.dashboard',
  '/function/order': 'module.order',
  '/function/inventory': 'module.inventory',
  '/function/bad-product': 'module.badProduct',
  '/function/customer': 'module.customer',
  '/function/price': 'module.price',
  '/function/receipt': 'module.receipt',
  '/function/approval': 'module.approval',
  '/function/attendance': 'module.attendance',
  '/function/employee': 'module.employee',
  '/function/organization': 'module.employee',
  '/function/role': 'module.role',
  '/function/tenant': '',
  '/function/label': 'module.label',
  '/function/document': 'module.document',
  '/manual': 'module.manual'
}
const keyword = ref('')
const searchPanelOpen = ref(false)
const mobileSearchOpen = ref(false)
const notificationOpen = ref(false)
const userMenuOpen = ref(false)
const pendingNotifications = ref([])

// 动态获取路由元信息中的中文标题
const pageTitle = computed(() => route.meta.title || '高管总览大盘')
const displayName = computed(() => userStore.userInfo?.userName || '当前用户')
const tenantName = computed(() => userStore.currentTenantName)
const roleLabel = computed(() => '运营管理')
const canSyncNotifications = computed(() => userStore.hasPermission('notification:announcement:publish'))
const avatarText = computed(() => {
  const name = displayName.value.trim()
  return name ? name.slice(0, 1).toUpperCase() : 'U'
})

const searchableMenus = computed(() => {
  return resolveMenus([
  { name: '总览大盘', path: '/dashboard', icon: 'dashboard', desc: '查看经营总览、企业公告和关键待办' },
  { name: '使用手册', path: '/manual', icon: 'menu_book', desc: '查看系统使用流程、打印说明和常见问题' },
  { name: '公告查看', path: '/function/announcement', icon: 'campaign', desc: '查看企业公告和员工已读状态', permissions: ANNOUNCEMENT_PERMISSIONS },
  { name: '发布公告', path: '/function/announcement/publish', icon: 'edit_notifications', desc: '发布普通、紧急或重要公告', permissions: ['notification:announcement:publish'] },
  { name: '订单列表', path: '/function/order', icon: 'list_alt', desc: '订单创建、履约和状态流转', permissions: ['order:list'] },
  { name: '安装任务', path: '/function/installation-task', icon: 'engineering', desc: '安装状态跟进和验收记录', permissions: ['installation:list'] },
  { name: '库存管理', path: '/function/inventory', icon: 'inventory_2', desc: '布匹入库、出库、库存预警和流水', permissions: ['inventory:warning:list', 'inventory:record:list', 'inventory:cloth:in', 'inventory:cloth:out'] },
  { name: '质量管理', path: '/function/bad-product', icon: 'report_problem', desc: '质量异常登记、处理闭环和损失跟踪', permissions: ['quality:list'] },
  { name: '客户管理', path: '/function/customer', icon: 'handshake', desc: '客户档案、联系人和合作项目维护', permissions: ['customer:list'] },
  { name: '价格管理', path: '/function/price', icon: 'sell', desc: 'SKU 基准价、客户等级价和特价维护', permissions: ['price:list'] },
  { name: '出库单打印', path: '/function/receipt', icon: 'print', desc: '待打印出库单、连续纸模板和打印确认', permissions: ['print:receipt:list'] },
  { name: '审批中心', path: '/function/approval', icon: 'approval', desc: '请假、财务、离职和订单审批待办处理', permissions: ['approval:leave:list', 'approval:finance:list', 'approval:resignation:list', 'approval:leave:submit', 'approval:finance:submit', 'approval:resignation:submit', 'order:list', 'order:audit:shipment', 'order:audit:cancel', 'quality:audit'] },
  { name: '考勤管理', path: '/function/attendance', icon: 'fingerprint', desc: '移动打卡记录、规则配置和异常统计', permissions: ['attendance:record:list', 'attendance:rule:list', 'attendance:rule:update', 'attendance:export'] },
  { name: '员工管理', path: '/function/employee', icon: 'groups', desc: '员工名录、组织架构和人员状态', permissions: ['employee:list'] },
  { name: '组织管理', path: '/function/organization', icon: 'account_tree', desc: '维护部门层级、职位体系和员工归属', permissions: ['organization:view'] },
  { name: '角色管理', path: '/function/role', icon: 'admin_panel_settings', desc: '角色权限配置和员工授权', permissions: ['role:list'] },
  { name: '企业授权', path: '/function/tenant', icon: 'domain', desc: '查看企业授权、启停状态和功能开关', developerOnly: true },
  { name: '标签模板', path: '/function/label', icon: 'sell', desc: '标签模板设计与现场打印联动', permissions: ['print:label:list'] },
  { name: '文档管理', path: '/function/document', icon: 'folder_open', desc: '企业文档目录和文件管理', permissions: ['document:list'] }
])
})

const filteredMenus = computed(() => {
  const query = keyword.value.toLowerCase()
  const menus = keyword.value
    ? buildSmartSearchResults(keyword.value)
    : searchableMenus.value.map((item) => ({ ...item, label: item.name, to: item.path }))
  if (!query) {
    return menus.slice(0, 6)
  }
  return menus
    .filter((item) => `${item.name}${item.label || ''}${item.desc}${item.path}`.toLowerCase().includes(query))
    .slice(0, 8)
})

function resolveMenus(menus) {
  if (userStore.isPlatformTenant) {
    return decorateAccessItems(userStore, menus, menuFeatureMap).filter((item) => item.path === '/function/tenant')
  }
  return decorateAccessItems(userStore, menus, menuFeatureMap)
}

function buildSmartSearchResults(rawKeyword) {
  const value = rawKeyword.trim()
  const upper = value.toUpperCase()
  const directResults = []

  if (/^(SO|XS|SALE|PO|SC|PROD|CK)\w+/i.test(value)) {
    const orderTab = upper.startsWith('PO') || upper.startsWith('SC') || upper.startsWith('PROD') ? 'production' : 'sales'
    directResults.push({
      name: '订单列表',
      label: `查找订单：${value}`,
      path: '/function/order',
      to: { path: '/function/order', query: { keyword: value, tab: orderTab } },
      icon: 'list_alt',
      desc: '直接进入订单列表并按订单号筛选'
    })
  }

  if (/^(CLANT|BAR|BC)\w+/i.test(value) || /\d{8,}/.test(value)) {
    directResults.push({
      name: '库存管理',
      label: `查找库存/条码：${value}`,
      path: '/function/inventory',
      to: { path: '/function/inventory', query: { keyword: value } },
      icon: 'inventory_2',
      desc: '直接筛选库存条码、型号和库存数据'
    })
  }

  directResults.push(
    {
      name: '客户管理',
      label: `查找客户/项目：${value}`,
      path: '/function/customer',
      to: { path: '/function/customer', query: { keyword: value } },
      icon: 'handshake',
      desc: '按客户名称、联系人或合作项目筛选'
    },
    {
      name: '员工管理',
      label: `查找员工：${value}`,
      path: '/function/employee',
      to: { path: '/function/employee', query: { keyword: value } },
      icon: 'groups',
      desc: '按员工姓名、手机号或工号筛选'
    },
    {
      name: '价格管理',
      label: `查找价格/SKU：${value}`,
      path: '/function/price',
      to: { path: '/function/price', query: { keyword: value } },
      icon: 'sell',
      desc: '按型号、批号或规格筛选价格'
    },
    {
      name: '考勤管理',
      label: `查找考勤人员：${value}`,
      path: '/function/attendance',
      to: { path: '/function/attendance', query: { keyword: value } },
      icon: 'fingerprint',
      desc: '按姓名、手机号或工号筛选考勤记录'
    }
  )

  const allowedDirectResults = directResults.map((item) => decorateSearchItem(item))
  const moduleResults = searchableMenus.value.map((item) => ({
    ...item,
    label: `进入${item.name}`,
    to: item.path
  }))
  return [...allowedDirectResults, ...moduleResults]
}

function decorateSearchItem(item) {
  const menuItem = targetMenu(item.path)
  if (menuItem) {
    return {
      ...item,
      disabled: menuItem.disabled,
      disabledReason: menuItem.disabledReason
    }
  }
  return resolveAccessState(userStore, item, menuFeatureMap)
}

function targetMenu(path) {
  return searchableMenus.value.find((item) => item.path === path)
}

function canAccessSearchTarget(path) {
  return Boolean(targetMenu(path))
}

function isSearchTargetDisabled(path) {
  return Boolean(targetMenu(path)?.disabled)
}

function searchItemClass(item) {
  return item.disabled
    ? 'cursor-not-allowed grayscale'
    : 'hover:bg-primary-container'
}

function goFirstSearchResult() {
  const first = filteredMenus.value.find((item) => !item.disabled)
  if (first) {
    handleSearchItemClick(first)
  } else if (filteredMenus.value.length) {
    ElMessage.warning('当前账号暂无权限打开这些入口')
  }
}

function handleSearchItemClick(item) {
  if (item.disabled) {
    ElMessage.warning(item.disabledReason || '当前账号暂无权限')
    return
  }
  goRoute(item.to || item.path)
}

function goSearchTarget(path) {
  const item = targetMenu(path)
  if (item?.disabled) {
    ElMessage.warning(item.disabledReason || '当前账号暂无权限')
    return
  }
  goRoute(path)
}

function goRoute(target) {
  searchPanelOpen.value = false
  mobileSearchOpen.value = false
  notificationOpen.value = false
  userMenuOpen.value = false
  keyword.value = ''
  router.push(target)
}

function toggleMobileSearch() {
  mobileSearchOpen.value = !mobileSearchOpen.value
  searchPanelOpen.value = mobileSearchOpen.value
  notificationOpen.value = false
  userMenuOpen.value = false
}

function goApproval() {
  goSearchTarget('/function/approval')
}

async function handleNotificationShow() {
  userMenuOpen.value = false
  searchPanelOpen.value = false
  mobileSearchOpen.value = false
  await refreshNotifications()
}

async function refreshNotifications(sync = false) {
  if (userStore.isPlatformTenant) {
    pendingNotifications.value = []
    return
  }
  try {
    if (sync && canSyncNotifications.value) {
      await syncNotifications()
    }
    const list = await getUnreadNotifications()
    pendingNotifications.value = (list || []).slice(0, 8).map((item) => ({
      key: `notification-${item.id}`,
      id: Number(item.id),
      title: item.title || '业务提醒',
      desc: item.content || '请进入对应业务页面查看详情',
      type: resolveNotificationType(item),
      route: item.route || '/dashboard',
      level: item.level,
      taskStatus: item.taskStatus || 'PENDING'
    }))
  } catch (error) {
    ElMessage.warning('通知加载失败，请稍后再试')
  }
}

async function openNotification(item) {
  try {
    await markNotificationRead(item.id)
  } catch {
    // 已读失败不阻断跳转，避免提醒入口不可用。
  }
  goRoute(item.route || '/dashboard')
  await refreshNotifications()
}

async function closeNotification(item, taskStatus) {
  try {
    const closeNote = taskStatus === 'DONE'
      ? '已在待办入口标记完成，并记录处理结果。'
      : '已在待办入口标记跳过，系统会减少类似提醒。'
    await closeNotificationTask(item.id, { taskStatus, closeNote })
    ElMessage.success(taskStatus === 'DONE' ? '已完成，系统会同步记录处理结果' : '已跳过，系统会减少类似提醒')
    await refreshNotifications()
  } catch (error) {
    ElMessage.warning('待办处理失败，请稍后再试')
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } catch {
    // 用户取消退出时不做提示，避免打断操作。
  }
}

function resolveNotificationType(item) {
  if (item.level === 'critical') {
    return '紧急'
  }
  if (item.level === 'warning') {
    return '预警'
  }
  return '提醒'
}

function handleClickOutside(event) {
  const target = event.target
  if (navbarRef.value && !navbarRef.value.contains(target)) {
    searchPanelOpen.value = false
    mobileSearchOpen.value = false
    notificationOpen.value = false
    userMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  refreshNotifications()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.ys-navbar {
  background: rgba(251, 252, 254, 0.88);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(200, 211, 223, 0.58);
}

.navbar-menu-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 800;
  color: #0f172a;
  transition: background 0.18s ease;
}

.navbar-menu-item:hover {
  background: var(--ys-primary-container);
}

.navbar-menu-item--disabled {
  cursor: not-allowed;
  border-color: var(--ys-disabled-bg);
  background: var(--ys-disabled-bg);
  color: var(--ys-disabled-text);
  box-shadow: none;
  opacity: 1;
}

.navbar-menu-item--disabled:hover {
  background: var(--ys-disabled-bg);
  transform: none;
}

.local-avatar {
  display: inline-flex;
  width: 2.5rem;
  height: 2.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, var(--ys-primary-dark) 0%, var(--ys-primary) 100%);
  color: #ffffff;
  font-size: 1rem;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
}

.tenant-chip {
  display: inline-flex;
  max-width: min(24rem, 38vw);
  align-items: center;
  gap: 0.68rem;
  overflow: hidden;
  border: 1px solid rgb(var(--ys-primary-rgb) / 0.16);
  border-radius: 999px;
  background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.94), var(--ys-primary-container)),
      radial-gradient(circle at 12% 18%, rgb(var(--ys-primary-rgb) / 0.12), transparent 38%);
  padding: 0.42rem 0.9rem 0.42rem 0.48rem;
  color: var(--ys-on-primary-container);
  box-shadow: 0 14px 30px rgb(var(--ys-primary-rgb) / 0.1);
}

.tenant-chip--branded {
  padding-left: 0.42rem;
}

.tenant-chip__logo-frame,
.tenant-chip__fallback-icon {
  display: inline-flex;
  width: 4rem;
  height: 2.25rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 0.65rem;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgb(var(--ys-primary-rgb) / 0.08), 0 8px 18px rgb(var(--ys-primary-rgb) / 0.1);
}

.tenant-chip__fallback-icon {
  font-size: 1.05rem;
  color: var(--ys-primary);
}

.tenant-chip__logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tenant-chip__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.08;
}

.tenant-chip__label {
  font-size: 0.62rem;
  font-weight: 950;
  letter-spacing: 0.12em;
  color: var(--ys-on-primary-container);
}

.tenant-chip__name {
  min-width: 0;
  max-width: 11.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
  font-weight: 950;
}

.user-menu-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-menu-brand__logo,
.user-menu-brand__icon {
  width: 4.6rem;
  height: 2.6rem;
  flex: 0 0 auto;
  border-radius: 0.9rem;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgb(var(--ys-primary-rgb) / 0.08), 0 10px 20px rgb(var(--ys-primary-rgb) / 0.1);
}

.user-menu-brand__logo {
  object-fit: cover;
}

.user-menu-brand__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ys-primary);
  font-size: 1.25rem;
}

@media (max-width: 767px) {
  .tenant-chip {
    max-width: calc(100vw - 12rem);
    padding: 0.4rem 0.55rem;
    gap: 0.45rem;
  }

  .tenant-chip__logo-frame,
  .tenant-chip__fallback-icon {
    width: 3.2rem;
    height: 1.8rem;
  }

  .tenant-chip__label {
    display: none;
  }

  .tenant-chip__name {
    max-width: 6.5rem;
  }
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
