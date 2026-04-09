<template>
  <div class="flex h-screen bg-surface text-on-surface font-sans antialiased overflow-hidden">
    <Sidebar />

    <div class="flex-1 flex flex-col min-w-0">
      <Navbar />

      <main class="flex-1 overflow-y-auto p-4 md:p-8">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
// Vue 3.3+ 宏定义组件名
defineOptions({ name: 'Layout' });

import Sidebar from './components/Sidebar.vue';
import Navbar from './components/Navbar.vue';

const route = useRoute();
</script>

<style scoped>
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
