<template>
  <div class="flex h-screen bg-surface text-on-surface font-sans antialiased overflow-hidden">
    <Sidebar />

    <div class="flex-1 flex flex-col min-w-0">
      <Navbar />

      <main class="flex-1 min-h-0 overflow-y-auto p-4 md:p-8">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
// Vue 3.3+ 宏定义组件名
defineOptions({ name: 'Layout' });

import Sidebar from './components/Sidebar.vue';
import Navbar from './components/Navbar.vue';
</script>
