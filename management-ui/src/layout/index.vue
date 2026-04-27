<template>
  <div class="ys-app-shell flex h-screen bg-surface text-on-surface font-sans antialiased overflow-hidden">
    <Sidebar />

    <div class="flex-1 flex flex-col min-w-0">
      <Navbar @toggle-mobile-menu="mobileMenuOpen = true" />

      <main class="flex-1 min-h-0 overflow-y-auto p-4 md:p-8">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
        <footer class="mt-8 flex flex-col md:flex-row items-center justify-center gap-2 md:gap-4 text-xs text-on-surface-variant/70">
          <span>{{ siteConfig.copyright }}</span>
          <a
            :href="siteConfig.icpUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="font-semibold hover:text-primary transition-colors"
          >
            {{ siteConfig.icpNumber }}
          </a>
        </footer>
      </main>
    </div>

    <div v-if="mobileMenuOpen" class="fixed inset-0 z-50 md:hidden">
      <button class="absolute inset-0 bg-slate-950/35 backdrop-blur-sm" @click="mobileMenuOpen = false"></button>
      <div class="relative h-full w-72">
        <Sidebar mobile />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Vue 3.3+ 宏定义组件名
defineOptions({ name: 'Layout' });

import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import Sidebar from './components/Sidebar.vue';
import Navbar from './components/Navbar.vue';
import { siteConfig } from '@/config/site';

const route = useRoute();
const mobileMenuOpen = ref(false);

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false;
  }
);
</script>

<style scoped>
.ys-app-shell {
  background:
      radial-gradient(circle at 10% 0%, rgba(255, 196, 41, 0.20), transparent 32%),
      radial-gradient(circle at 88% 8%, rgba(245, 164, 0, 0.12), transparent 30%),
      linear-gradient(180deg, #fffdf8 0%, #ffffff 100%);
}
</style>
