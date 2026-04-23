<template>
  <header ref="navbarRef" class="ys-navbar h-20 bg-surface flex items-center justify-between px-4 md:px-8 shrink-0 relative z-10">
    <button
      class="md:hidden p-2 text-on-surface-variant rounded-full hover:bg-surface-container-highest"
      @click="emit('toggle-mobile-menu')"
    >
      <span class="material-symbols-outlined">menu</span>
    </button>

    <div class="flex items-center gap-6 flex-1 max-w-2xl ml-4 md:ml-0">
      <h2 class="text-xl font-bold text-on-surface hidden lg:block">{{ pageTitle }}</h2>

      <div class="relative flex-1 group max-w-md hidden md:block">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg group-focus-within:text-primary transition-colors">search</span>
        <input
          v-model.trim="keyword"
          type="text"
          placeholder="搜索订单、库存、客户、员工..."
          class="w-full bg-surface-container-highest border-none rounded-xl py-2.5 pl-12 pr-4 text-sm text-on-surface focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-on-surface-variant/60"
          @focus="searchPanelOpen = true"
          @keydown.enter.prevent="goFirstSearchResult"
        >
        <div
          v-if="searchPanelOpen && filteredMenus.length"
          class="absolute left-0 right-0 top-[calc(100%+10px)] overflow-hidden rounded-2xl border border-outline-variant/40 bg-white/95 shadow-xl shadow-primary/10 backdrop-blur-xl"
        >
          <button
            v-for="item in filteredMenus"
            :key="`${item.path}-${item.label || item.name}`"
            class="flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition-colors hover:bg-primary-container"
            @click="goRoute(item.to || item.path)"
          >
            <span class="material-symbols-outlined text-primary text-[20px]">{{ item.icon }}</span>
            <span class="min-w-0 flex-1">
              <strong class="block truncate text-on-surface">{{ item.label || item.name }}</strong>
              <small class="block truncate text-on-surface-variant">{{ item.desc }}</small>
            </span>
            <span class="material-symbols-outlined text-on-surface-variant text-[18px]">arrow_forward</span>
          </button>
        </div>
      </div>
    </div>

    <div class="flex items-center gap-2 md:gap-4">
      <div class="relative">
      <button
        class="w-10 h-10 flex items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-container-highest transition-colors relative"
        @click="toggleNotifications"
      >
        <span class="material-symbols-outlined">notifications</span>
        <span v-if="pendingNotifications.length" class="absolute top-2 right-2.5 w-2 h-2 bg-error rounded-full ring-2 ring-surface"></span>
      </button>
        <div
          v-if="notificationOpen"
          class="absolute right-0 top-[calc(100%+12px)] w-[320px] overflow-hidden rounded-2xl border border-outline-variant/40 bg-white/95 shadow-xl shadow-primary/10 backdrop-blur-xl"
        >
          <div class="flex items-center justify-between border-b border-outline-variant/30 px-4 py-3">
            <div>
              <p class="text-sm font-black text-on-surface">待办通知</p>
              <p class="text-xs text-on-surface-variant">{{ pendingNotifications.length ? `有 ${pendingNotifications.length} 条需要处理` : '当前没有新的待办' }}</p>
            </div>
            <button class="rounded-lg px-2 py-1 text-xs font-bold text-primary hover:bg-primary-container" @click="refreshNotifications">
              刷新
            </button>
          </div>
          <div class="max-h-[360px] overflow-y-auto p-2">
            <button
              v-for="item in pendingNotifications"
              :key="item.key"
              class="w-full rounded-xl px-3 py-3 text-left transition-colors hover:bg-primary-container"
              @click="goApproval"
            >
              <div class="flex items-center justify-between gap-3">
                <strong class="text-sm text-on-surface">{{ item.title }}</strong>
                <span class="rounded-full bg-primary-container px-2 py-0.5 text-[10px] font-bold text-primary">{{ item.type }}</span>
              </div>
              <p class="mt-1 line-clamp-2 text-xs leading-5 text-on-surface-variant">{{ item.desc }}</p>
            </button>
            <div v-if="!pendingNotifications.length" class="px-4 py-8 text-center">
              <span class="material-symbols-outlined text-4xl text-primary/40">task_alt</span>
              <p class="mt-2 text-sm font-bold text-on-surface">待办已清空</p>
              <p class="text-xs text-on-surface-variant">审批和业务提醒会展示在这里。</p>
            </div>
          </div>
        </div>
      </div>

      <div class="relative flex items-center gap-3 pl-2 md:pl-4 border-l border-outline-variant/30">
        <button class="flex items-center gap-3 rounded-2xl px-2 py-1 transition-colors hover:bg-surface-container-highest" @click="userMenuOpen = !userMenuOpen">
          <div class="hidden md:block text-right">
            <p class="text-sm font-bold text-on-surface">{{ displayName }}</p>
            <p class="text-xs text-on-surface-variant">{{ roleLabel }}</p>
          </div>
          <img :src="avatarUrl" alt="头像" class="w-10 h-10 rounded-xl object-cover">
        </button>
        <div
          v-if="userMenuOpen"
          class="absolute right-0 top-[calc(100%+12px)] w-64 overflow-hidden rounded-2xl border border-outline-variant/40 bg-white/95 shadow-xl shadow-primary/10 backdrop-blur-xl"
        >
          <div class="border-b border-outline-variant/30 px-4 py-4">
            <p class="text-sm font-black text-on-surface">{{ displayName }}</p>
            <p class="mt-1 text-xs text-on-surface-variant">租户：{{ tenantCode }}</p>
          </div>
          <button class="navbar-menu-item" @click="goRoute('/dashboard')">
            <span class="material-symbols-outlined">dashboard</span>回到总览大盘
          </button>
          <button class="navbar-menu-item" @click="goApproval">
            <span class="material-symbols-outlined">approval</span>查看审批中心
          </button>
          <button class="navbar-menu-item text-error" @click="handleLogout">
            <span class="material-symbols-outlined">logout</span>退出登录
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listFinanceApprovals, listLeaveApprovals } from '@/views/function/approval/api/approval'

defineOptions({ name: 'Navbar' });

const emit = defineEmits<{
  (event: 'toggle-mobile-menu'): void
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const navbarRef = ref<HTMLElement | null>(null)
const keyword = ref('')
const searchPanelOpen = ref(false)
const notificationOpen = ref(false)
const userMenuOpen = ref(false)
const pendingNotifications = ref<Array<{ key: string; title: string; desc: string; type: string }>>([])

// 动态获取路由元信息中的中文标题
const pageTitle = computed<string>(() => (route.meta.title as string) || '高管总览大盘')
const displayName = computed(() => userStore.userInfo?.userName || '当前用户')
const tenantCode = computed(() => userStore.userInfo?.tenantCode || '--')
const roleLabel = computed(() => (userStore.isDeveloper ? '平台超管' : '运营管理'))
const avatarUrl = computed(() => {
  const name = encodeURIComponent(displayName.value || 'User')
  return `https://ui-avatars.com/api/?name=${name}&background=1f6fff&color=fff`
})

const searchableMenus = computed(() => filterMenus([
  { name: '总览大盘', path: '/dashboard', icon: 'dashboard', desc: '查看经营总览、AI 建议和关键待办' },
  { name: 'AI 经营建议', path: '/dashboard/ai-advices', icon: 'psychology', desc: '查看库存、订单、客户、质量等经营洞察' },
  { name: '订单管理', path: '/function/order', icon: 'list_alt', desc: '销售订单、生产订单和状态流转', permissions: ['sales:order:list', 'production:order:list'] },
  { name: '库存管理', path: '/function/inventory', icon: 'inventory_2', desc: '布匹入库、出库、库存预警和流水', permissions: ['inventory:list', 'inventory:record:recent', 'inventory:cloth:in', 'inventory:cloth:out'] },
  { name: '次品管理', path: '/function/bad-product', icon: 'report_problem', desc: '质量异常登记、处理闭环和损失跟踪' },
  { name: '客户管理', path: '/function/customer', icon: 'handshake', desc: '客户档案、联系人和合作项目维护', permissions: ['customer:page'] },
  { name: '价格管理', path: '/function/price', icon: 'sell', desc: 'SKU 基准价、客户等级价和特价维护', permissions: ['price:list'] },
  { name: '出库单打印', path: '/function/receipt', icon: 'print', desc: '待打印出库单、连续纸模板和打印确认', permissions: ['receipt:print:list'] },
  { name: '审批中心', path: '/function/approval', icon: 'approval', desc: '请假审批、财务审批和待办处理', permissions: ['approval:leave', 'approval:finance', 'approval:leave:submit', 'approval:finance:submit'] },
  { name: '考勤管理', path: '/function/attendance', icon: 'fingerprint', desc: '小程序打卡记录、规则配置和异常统计', permissions: ['attendance:record:list', 'attendance:*', 'attendance:list'] },
  { name: '员工管理', path: '/function/employee', icon: 'groups', desc: '员工名录、组织架构和人员状态', permissions: ['employee:list'] },
  { name: '角色管理', path: '/function/role', icon: 'admin_panel_settings', desc: '角色权限配置和员工授权', permissions: ['role:list'] },
  { name: '标签模板', path: '/function/label', icon: 'sell', desc: '标签模板可视化设计与小程序打印联动', permissions: ['label:template:list'] },
  { name: '文档管理', path: '/function/document', icon: 'folder_open', desc: '企业文档目录和文件管理', permissions: ['document:list'] },
  { name: '租户管理', path: '/platform/tenant', icon: 'apartment', desc: '平台超管维护租户和初始账号', permissions: ['platform:tenant:view'], developerOnly: true }
]))

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

function filterMenus(menus: Array<{ name: string; path: string; icon: string; desc: string; permissions?: string[]; developerOnly?: boolean }>) {
  return menus.filter((item) => {
    if (item.developerOnly && !userStore.isDeveloper) {
      return false
    }
    return !item.permissions || userStore.hasAnyPermission(item.permissions)
  })
}

function buildSmartSearchResults(rawKeyword: string) {
  const value = rawKeyword.trim()
  const upper = value.toUpperCase()
  const directResults = []

  if (/^(SO|XS|SALE|PO|SC|PROD|CK)\w+/i.test(value)) {
    const orderTab = upper.startsWith('PO') || upper.startsWith('SC') || upper.startsWith('PROD') ? 'production' : 'sales'
    directResults.push({
      name: '订单管理',
      label: `查找订单：${value}`,
      path: '/function/order',
      to: { path: '/function/order', query: { keyword: value, tab: orderTab } },
      icon: 'list_alt',
      desc: '直接进入订单管理并按订单号筛选'
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

  const moduleResults = searchableMenus.value.map((item) => ({
    ...item,
    label: `进入${item.name}`,
    to: item.path
  }))
  return [...directResults, ...moduleResults]
}

function goFirstSearchResult() {
  if (filteredMenus.value.length) {
    goRoute(filteredMenus.value[0].to || filteredMenus.value[0].path)
  }
}

function goRoute(target: string | { path: string; query?: Record<string, string> }) {
  searchPanelOpen.value = false
  notificationOpen.value = false
  userMenuOpen.value = false
  keyword.value = ''
  router.push(target)
}

function goApproval() {
  goRoute('/function/approval')
}

async function toggleNotifications() {
  notificationOpen.value = !notificationOpen.value
  userMenuOpen.value = false
  searchPanelOpen.value = false
  if (notificationOpen.value) {
    await refreshNotifications()
  }
}

async function refreshNotifications() {
  const list: Array<{ key: string; title: string; desc: string; type: string }> = []
  const currentUserId = Number(userStore.userInfo?.userId || 0)
  try {
    if (userStore.hasAnyPermission(['approval:leave', 'approval:leave:submit'])) {
      const leaves = await listLeaveApprovals()
      ;(leaves || [])
        .filter((item: any) => item.status === 1 && Number(item.auditorId) === currentUserId)
        .slice(0, 5)
        .forEach((item: any) => {
          list.push({
            key: `leave-${item.leaveCode}`,
            title: `${item.applyUserName || '员工'} 的请假审批`,
            desc: `${item.leaveTypeText || '请假'}：${formatText(item.startTime)} 至 ${formatText(item.endTime)}`,
            type: '请假'
          })
        })
    }
    if (userStore.hasAnyPermission(['approval:finance', 'approval:finance:submit'])) {
      const finances = await listFinanceApprovals()
      ;(finances || [])
        .filter((item: any) => item.status === 1 && Number(item.auditorId) === currentUserId)
        .slice(0, 5)
        .forEach((item: any) => {
          list.push({
            key: `finance-${item.approvalCode}`,
            title: `${item.applyUserName || '员工'} 的财务审批`,
            desc: `${item.category || '费用'}：￥${item.amount || 0} / ${item.reason || '未填写原因'}`,
            type: '财务'
          })
        })
    }
  } catch (error) {
    ElMessage.warning('通知加载失败，请稍后再试')
  }
  pendingNotifications.value = list.slice(0, 8)
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

function formatText(value: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--'
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node
  if (navbarRef.value && !navbarRef.value.contains(target)) {
    searchPanelOpen.value = false
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
  background: rgba(247, 251, 255, 0.86);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(191, 215, 245, 0.56);
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
  color: #0b2a6f;
  transition: background 0.18s ease;
}

.navbar-menu-item:hover {
  background: #e8f2ff;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
