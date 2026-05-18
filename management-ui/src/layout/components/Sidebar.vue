<template>
  <aside
      class="ys-sidebar bg-surface-container-low flex-col relative z-20 transition-all duration-300 ease-in-out border-r border-outline-variant/20"
      :class="[props.mobile ? 'flex h-full w-72' : 'hidden md:flex', props.mobile ? '' : isCollapsed ? 'w-[88px]' : 'w-64']"
  >
    <div class="h-20 flex items-center shrink-0 overflow-hidden" :class="isCollapsed ? 'justify-center px-0' : 'px-8'">
      <div class="flex items-center overflow-hidden" :class="isCollapsed ? 'justify-center' : 'gap-3'">
        <img src="../../../images/logo.png" alt="蜂巢 logo" class="w-12 h-12 rounded-xl object-contain drop-shadow-sm">
        <h1 v-if="!isCollapsed" class="font-black text-xl tracking-tight text-slate-900 whitespace-nowrap">蜂巢 Hive</h1>
      </div>
    </div>

    <div
      v-if="!isCollapsed"
      class="mx-4 mb-2 rounded-2xl border border-primary/10 bg-white/70 px-4 py-3 shadow-sm shadow-primary/5"
      :title="userStore.currentTenantName"
    >
      <p class="text-[10px] font-black uppercase tracking-[0.24em] text-primary/55">欢迎你</p>
      <p class="mt-1 truncate text-sm font-black text-slate-900">{{ userStore.currentTenantName }}</p>
    </div>

    <nav class="flex-1 py-4 overflow-y-auto scrollbar-hide" :class="isCollapsed ? 'px-2' : 'px-4'">
      <div class="space-y-2">
        <router-link
            v-for="item in primaryMenus"
            :key="item.path"
            :to="item.path"
            class="relative flex rounded-xl transition-all duration-200 overflow-hidden"
            :class="linkClass(item.path)"
        >
          <span class="material-symbols-outlined shrink-0 transition-all"
                :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
          <span class="whitespace-nowrap transition-all duration-200"
                :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{
              item.name
            }}</span>
          <span
              v-if="item.path === '/function/approval' && approvalPendingCount > 0"
              class="inline-flex min-w-[18px] h-[18px] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] leading-none text-white shadow-sm shadow-rose-500/25"
              :class="isCollapsed ? 'absolute right-1.5 top-1.5' : 'ml-auto'"
          >{{ approvalPendingCount > 99 ? '99+' : approvalPendingCount }}</span>
        </router-link>
      </div>

      <div v-if="secondaryMenus.length" class="mt-4 border-t border-outline-variant/20 pt-4">
        <button
            @click="toggleMore"
            class="w-full flex rounded-xl transition-all duration-200 text-on-surface-variant hover:bg-surface-container-highest hover:text-primary"
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
        </button>

        <div v-show="showMore" class="mt-2 space-y-2">
          <router-link
              v-for="item in secondaryMenus"
              :key="item.path"
              :to="item.path"
              class="flex rounded-xl transition-all duration-200 overflow-hidden"
              :class="linkClass(item.path)"
          >
            <span class="material-symbols-outlined shrink-0 transition-all"
                  :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
            <span class="whitespace-nowrap transition-all duration-200"
                  :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{
                item.name
              }}</span>
          </router-link>
        </div>
      </div>
    </nav>

    <div v-if="!props.mobile" class="p-4 border-t border-outline-variant/20 flex shrink-0"
         :class="isCollapsed ? 'justify-center' : 'justify-end'">
      <button
          @click="toggleSidebar"
          class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container-highest hover:text-primary transition-colors flex flex-col items-center gap-1"
      >
        <span class="material-symbols-outlined text-[20px] transition-transform duration-300"
              :class="isCollapsed ? '' : 'rotate-180'">keyboard_double_arrow_right</span>
        <span v-if="isCollapsed" class="text-[10px] font-bold tracking-tighter scale-90">展开</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {useUserStore} from '@/stores/user'
import {getApprovalSummary} from '@/views/function/approval/api/approval'

defineOptions({name: 'Sidebar'})

const props = withDefaults(defineProps<{
  mobile?: boolean
}>(), {
  mobile: false
})

const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(!props.mobile)
const approvalPendingCount = ref(0)
const AI_ADVICE_PERMISSIONS = [
  'dashboard:ai:view',
  'dashboard:ai:*',
  'dashboard:*',
  'dashboard:ai:inventory',
  'dashboard:ai:order',
  'dashboard:ai:customer',
  'dashboard:ai:quality',
  'dashboard:ai:finance',
  'dashboard:ai:employee',
  'dashboard:ai:operation'
]
const ANNOUNCEMENT_PERMISSIONS = [
  'notification:announcement:list',
  'notification:announcement:publish',
  'dashboard:*'
]
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

interface MenuItem {
  name: string
  path: string
  icon: string
  permissions?: string[]
  features?: string[]
}

const menuFeatureMap: Record<string, string> = {
  '/dashboard': 'module.dashboard',
  '/dashboard/ai-advices': 'aiAdvice',
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
  '/function/label': 'module.label',
  '/function/document': 'module.document',
  '/manual': 'module.manual'
}

const primaryMenus = computed<MenuItem[]>(() => {
  return filterMenus([
  {name: '总览大盘', path: '/dashboard', icon: 'dashboard'},
  {
    name: '订单管理',
    path: '/function/order',
    icon: 'list_alt',
    permissions: ['sales:order:list', 'production:order:list']
  },
  {name: '库存管理', path: '/function/inventory', icon: 'storage', permissions: ['inventory:warning:list', 'inventory:record:recent', 'inventory:cloth:in', 'inventory:cloth:out']},
  {name: '质量管理', path: '/function/bad-product', icon: 'warning', permissions: ['badproduct:list', 'badproduct:save', 'badproduct:process']},
  {name: '客户管理', path: '/function/customer', icon: 'handshake', permissions: ['customer:page']},
  {name: '价格管理', path: '/function/price', icon: 'price_change', permissions: ['price:list']},
  {name: '出库单打印', path: '/function/receipt', icon: 'print', permissions: ['receipt:print:list']},
  {
    name: '审批中心',
    path: '/function/approval',
    icon: 'approval',
    permissions: ['approval:leave', 'approval:finance', 'approval:resignation', 'approval:leave:submit', 'approval:finance:submit', 'approval:resignation:submit', 'sales:order:list', 'production:order:list']
  },
])
})

const secondaryMenus = computed<MenuItem[]>(() => {
  return filterMenus([
  {name: '企业通知公告', path: '/function/announcement', icon: 'campaign', permissions: ANNOUNCEMENT_PERMISSIONS},
  {name: 'AI 经营建议', path: '/dashboard/ai-advices', icon: 'psychology', permissions: AI_ADVICE_PERMISSIONS},
  {name: '考勤管理', path: '/function/attendance', icon: 'timer', permissions: ['attendance:record:list', 'attendance:*']},
  {name: '员工管理', path: '/function/employee', icon: 'people', permissions: ['employee:list']},
  {name: '部门管理', path: '/function/organization', icon: 'account_tree', permissions: ['employee:list']},
  {name: '角色管理', path: '/function/role', icon: 'settings_accessibility', permissions: ['role:list']},
  {name: '标签模板', path: '/function/label', icon: 'sell', permissions: ['label:template:list']},
  {name: '文档管理', path: '/function/document', icon: 'folder_open', permissions: ['document:list']},
  {name: '使用手册', path: '/manual', icon: 'menu_book'},
])
})

function filterMenus(menus: MenuItem[]) {
  return menus.filter((item) => {
    const requiredFeatures = item.features || (menuFeatureMap[item.path] ? [menuFeatureMap[item.path]] : [])
    if (requiredFeatures.length && !userStore.hasAnyFeature(requiredFeatures)) {
      return false
    }
    return !item.permissions || userStore.hasAnyPermission(item.permissions)
  })
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
  if (!userStore.hasAnyFeature(['module.approval']) ||
      !userStore.hasAnyPermission(['approval:leave', 'approval:finance', 'approval:resignation', 'sales:order:list', 'production:order:list'])) {
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

const linkClass = (path: string) => {
  const active = route.path.startsWith(path)
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
      linear-gradient(180deg, rgba(255, 250, 240, 0.96), rgba(255, 255, 255, 0.92)),
      radial-gradient(circle at 20% 0%, rgba(255, 196, 41, 0.24), transparent 36%);
  box-shadow: 18px 0 42px rgba(245, 164, 0, 0.08);
}
</style>
