<template>
  <aside
    class="bg-surface-container-low hidden md:flex flex-col relative z-20 transition-all duration-300 ease-in-out border-r border-outline-variant/20"
    :class="isCollapsed ? 'w-[88px]' : 'w-64'"
  >
    <div class="h-20 flex items-center shrink-0 overflow-hidden" :class="isCollapsed ? 'justify-center px-0' : 'px-8'">
      <span v-if="isCollapsed" class="material-symbols-outlined text-primary text-3xl">corporate_fare</span>
      <h1 v-else class="font-black text-xl tracking-tight text-primary whitespace-nowrap">数字工坊</h1>
    </div>

    <nav class="flex-1 py-6 space-y-2 overflow-y-auto scrollbar-hide" :class="isCollapsed ? 'px-2' : 'px-4'">
      <router-link
        v-for="item in menuList"
        :key="item.path"
        :to="item.path"
        class="flex rounded-xl transition-all duration-200 overflow-hidden"
        :class="[
          route.path === item.path
            ? 'bg-primary text-white shadow-sm' // 激活状态
            : 'text-on-surface-variant hover:bg-surface-container-highest hover:text-primary', // 默认状态
          // 核心排版切换：折叠时上下排列，展开时左右排列
          isCollapsed ? 'flex-col items-center justify-center py-3 gap-1' : 'flex-row items-center gap-3 px-4 py-3'
        ]"
      >
        <span class="material-symbols-outlined shrink-0 transition-all" :class="isCollapsed ? 'text-[24px]' : 'text-[20px]'">
          {{ item.icon }}
        </span>

        <span
          class="whitespace-nowrap transition-all duration-200"
          :class="[
            isCollapsed ? 'text-[10px] font-bold tracking-tighter' : 'text-sm font-medium',
            // 如果名字太长（比如四个字），在折叠状态下允许它稍微缩放，防止溢出
            isCollapsed && item.name.length > 3 ? 'scale-90 origin-top' : ''
          ]"
        >
          {{ item.name }}
        </span>
      </router-link>
    </nav>

    <div class="p-4 border-t border-outline-variant/20 flex shrink-0" :class="isCollapsed ? 'justify-center' : 'justify-end'">
      <button
        @click="toggleSidebar"
        class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container-highest hover:text-primary transition-colors flex flex-col items-center gap-1"
      >
        <span class="material-symbols-outlined text-[20px] transition-transform duration-300" :class="isCollapsed ? '' : 'rotate-180'">
          keyboard_double_arrow_right
        </span>
        <span v-if="isCollapsed" class="text-[10px] font-bold tracking-tighter scale-90">展开</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';

defineOptions({ name: 'Sidebar' });

const route = useRoute();

// 侧边栏状态，默认折叠看效果
const isCollapsed = ref(true);

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

interface MenuItem {
  name: string;
  path: string;
  icon: string;
}

const menuList = ref<MenuItem[]>([
  { name: '总览大盘', path: '/dashboard', icon: 'dashboard' },
  { name: '生产订单', path: '/function/production', icon: 'precision_manufacturing' },
  { name: '库存管理', path: '/function/inventory', icon: 'inventory_2' },
  { name: '员工管理', path: '/function/employee', icon: 'people' },
  { name: '角色管理', path: '/function/role', icon: 'settings_accessibility' },
  { name: '客户管理', path: '/function/customer', icon: 'handshake' },
  { name: '价格管理', path: '/function/price', icon: 'price_change' },
  { name: '审批中心', path: '/function/approval', icon: 'fact_check' },
  { name: '出库单打印', path: '/function/receipt', icon: 'print' },
  { name: '标签模板', path: '/function/label', icon: 'settings' },
  { name: '文档管理', path: '/function/document', icon: 'folder_open' },
]);
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
