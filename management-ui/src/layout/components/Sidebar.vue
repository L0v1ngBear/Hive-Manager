<template>
  <aside
    class="bg-surface-container-low hidden md:flex flex-col relative z-20 transition-all duration-300 ease-in-out border-r border-outline-variant/20"
    :class="isCollapsed ? 'w-[88px]' : 'w-64'"
  >
    <div class="h-20 flex items-center shrink-0 overflow-hidden" :class="isCollapsed ? 'justify-center px-0' : 'px-8'">
      <span v-if="isCollapsed" class="material-symbols-outlined text-primary text-3xl">corporate_fare</span>
      <h1 v-else class="font-black text-xl tracking-tight text-primary whitespace-nowrap">数字工坊</h1>
    </div>

    <nav class="flex-1 py-4 overflow-y-auto scrollbar-hide" :class="isCollapsed ? 'px-2' : 'px-4'">
      <div class="space-y-2">
        <router-link
          v-for="item in primaryMenus"
          :key="item.path"
          :to="item.path"
          class="flex rounded-xl transition-all duration-200 overflow-hidden"
          :class="linkClass(item.path)"
        >
          <span class="material-symbols-outlined shrink-0 transition-all" :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
          <span class="whitespace-nowrap transition-all duration-200" :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{ item.name }}</span>
        </router-link>
      </div>

      <div v-if="secondaryMenus.length" class="mt-4 border-t border-outline-variant/20 pt-4">
        <button
          @click="toggleMore"
          class="w-full flex rounded-xl transition-all duration-200 text-on-surface-variant hover:bg-surface-container-highest hover:text-primary"
          :class="isCollapsed ? 'flex-col items-center justify-center py-3 gap-1' : 'flex-row items-center justify-between px-4 py-3'"
        >
          <div class="flex items-center" :class="isCollapsed ? 'flex-col gap-1' : 'flex-row gap-3'">
            <span class="material-symbols-outlined shrink-0" :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">apps</span>
            <span class="whitespace-nowrap" :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">更多功能</span>
          </div>
          <span v-if="!isCollapsed" class="material-symbols-outlined text-[18px] transition-transform" :class="showMore ? 'rotate-90' : ''">chevron_right</span>
        </button>

        <div v-show="showMore" class="mt-2 space-y-2">
          <router-link
            v-for="item in secondaryMenus"
            :key="item.path"
            :to="item.path"
            class="flex rounded-xl transition-all duration-200 overflow-hidden"
            :class="linkClass(item.path)"
          >
            <span class="material-symbols-outlined shrink-0 transition-all" :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">{{ item.icon }}</span>
            <span class="whitespace-nowrap transition-all duration-200" :class="isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium'">{{ item.name }}</span>
          </router-link>
        </div>
      </div>
    </nav>

    <div class="p-4 border-t border-outline-variant/20 flex shrink-0" :class="isCollapsed ? 'justify-center' : 'justify-end'">
      <button
        @click="toggleSidebar"
        class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container-highest hover:text-primary transition-colors flex flex-col items-center gap-1"
      >
        <span class="material-symbols-outlined text-[20px] transition-transform duration-300" :class="isCollapsed ? '' : 'rotate-180'">keyboard_double_arrow_right</span>
        <span v-if="isCollapsed" class="text-[10px] font-bold tracking-tighter scale-90">展开</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'Sidebar' })

const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(true)
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

interface MenuItem {
  name: string
  path: string
  icon: string
  permissions?: string[]
  developerOnly?: boolean
}

const primaryMenus = computed<MenuItem[]>(() => filterMenus([
  { name: '总览大盘', path: '/dashboard', icon: 'dashboard' },
  { name: '订单管理', path: '/function/order', icon: 'list_alt', permissions: ['sales:order:list', 'production:order:list'] },
  { name: '次品管理', path: '/function/bad-product', icon: 'warning' },
  { name: '客户管理', path: '/function/customer', icon: 'handshake', permissions: ['customer:page'] },
  { name: '价格管理', path: '/function/price', icon: 'price_change', permissions: ['price:list'] },
  { name: '出库单打印', path: '/function/receipt', icon: 'print', permissions: ['receipt:print:list'] },
  { name: '审批中心', path: '/function/approval', icon: 'approval', permissions: ['approval:leave', 'approval:finance', 'approval:leave:submit', 'approval:finance:submit'] },
]))

const secondaryMenus = computed<MenuItem[]>(() => filterMenus([
  { name: '员工管理', path: '/function/employee', icon: 'people', permissions: ['employee:list'] },
  { name: '角色管理', path: '/function/role', icon: 'settings_accessibility', permissions: ['role:list'] },
  { name: '标签模板', path: '/function/label', icon: 'sell', permissions: ['label:template:list'] },
  { name: '文档管理', path: '/function/document', icon: 'folder_open', permissions: ['document:list'] },
  { name: '租户管理', path: '/platform/tenant', icon: 'apartment', permissions: ['platform:tenant:view'], developerOnly: true },
]))

function filterMenus(menus: MenuItem[]) {
  return menus.filter((item) => {
    if (item.developerOnly && !userStore.isDeveloper) {
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
  { immediate: true }
)

watch(
  () => route.path,
  (path) => {
    if (secondaryPaths.value.some((menuPath) => path.startsWith(menuPath))) {
      showMore.value = true
    }
  },
  { immediate: true }
)

const toggleMore = () => {
  if (!secondaryMenus.value.length) {
    return
  }
  showMore.value = !showMore.value
}

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
</style>
