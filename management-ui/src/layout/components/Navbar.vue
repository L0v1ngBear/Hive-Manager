<template>
  <header class="h-20 bg-surface flex items-center justify-between px-4 md:px-8 shrink-0 relative z-10">
    <button class="md:hidden p-2 text-on-surface-variant rounded-full hover:bg-surface-container-highest">
      <span class="material-symbols-outlined">menu</span>
    </button>

    <div class="flex items-center gap-6 flex-1 max-w-2xl ml-4 md:ml-0">
      <h2 class="text-xl font-bold text-on-surface hidden lg:block">{{ pageTitle }}</h2>

      <div class="relative flex-1 group max-w-md hidden md:block">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg group-focus-within:text-primary transition-colors">search</span>
        <input
          type="text"
          placeholder="搜索订单、织机或布匹型号..."
          class="w-full bg-surface-container-highest border-none rounded-xl py-2.5 pl-12 pr-4 text-sm text-on-surface focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-on-surface-variant/60"
        >
      </div>
    </div>

    <div class="flex items-center gap-2 md:gap-4">
      <button class="w-10 h-10 flex items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-container-highest transition-colors relative">
        <span class="material-symbols-outlined">notifications</span>
        <span class="absolute top-2 right-2.5 w-2 h-2 bg-error rounded-full ring-2 ring-surface"></span>
      </button>

      <div class="flex items-center gap-3 pl-2 md:pl-4 border-l border-outline-variant/30">
        <div class="hidden md:block text-right">
          <p class="text-sm font-bold text-on-surface">陈亚历</p>
          <p class="text-xs text-on-surface-variant">运营总监</p>
        </div>
        <img src="https://ui-avatars.com/api/?name=Chen&background=455f88&color=fff" alt="头像" class="w-10 h-10 rounded-xl object-cover cursor-pointer hover:opacity-80 transition-opacity">
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

defineOptions({ name: 'Navbar' });

const route = useRoute();
// 动态获取路由元信息中的中文标题
const pageTitle = computed<string>(() => (route.meta.title as string) || '高管总览大盘');
</script>
