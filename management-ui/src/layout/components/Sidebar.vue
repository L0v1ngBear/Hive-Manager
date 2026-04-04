<template>
  <aside class="w-64 bg-surface-container-low hidden md:flex flex-col relative z-20">
    <div class="h-20 flex items-center px-8 shrink-0">
      <div class="w-8 h-8 bg-primary rounded-br-xl rounded-tl-xl mr-3 flex items-center justify-center">
        <span class="material-symbols-outlined text-white text-sm">filter_vintage</span>
      </div>
      <h1 class="font-black text-xl tracking-tight text-primary">数字工坊</h1>
    </div>

    <nav class="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
      <router-link
        v-for="item in menuList"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 px-4 py-3 text-sm font-medium rounded-xl transition-colors duration-200"
        :class="[
          route.path === item.path
            ? 'bg-primary text-white shadow-sm' // 激活状态：主色背景
            : 'text-on-surface-variant hover:bg-surface-container-highest hover:text-on-surface' // 默认状态
        ]"
      >
        <span class="material-symbols-outlined text-[20px]">{{ item.icon }}</span>
        {{ item.name }}
      </router-link>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';

defineOptions({ name: 'Sidebar' });

const route = useRoute();

// TS 5.0+ 强类型约束菜单结构
interface MenuItem {
  name: string;
  path: string;
  icon: string;
}

// 本地化菜单数据
const menuList = ref<MenuItem[]>([
  { name: '总览大盘', path: '/dashboard', icon: 'dashboard' },
  { name: '生产订单', path: '/production', icon: 'precision_manufacturing' },
  { name: '库存管理', path: '/inventory', icon: 'inventory_2' },
  { name: '客户管理', path: '/customer', icon: 'handshake' },
  { name: '审批中心', path: '/approval', icon: 'fact_check' },
]);
</script>
