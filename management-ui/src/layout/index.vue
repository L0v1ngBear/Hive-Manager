<template>
  <div
    ref="shellRef"
    :class="[
      'ys-app-shell flex h-full min-h-0 bg-surface text-on-surface font-sans antialiased overflow-hidden',
      `ys-app-shell--${viewportMode}`
    ]"
  >
    <Sidebar />

    <div class="flex-1 flex flex-col min-w-0">
      <Navbar @toggle-mobile-menu="mobileMenuOpen = true" />

      <main ref="mainRef" class="ys-app-main flex-1 min-h-0 overflow-y-auto">
        <div class="ys-app-content-frame">
          <router-view v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </div>
      </main>
    </div>

    <div v-if="mobileMenuOpen" class="fixed inset-0 z-50 md:hidden">
      <button class="absolute inset-0 bg-slate-950/35 backdrop-blur-sm" @click="mobileMenuOpen = false"></button>
      <div class="relative h-full w-[min(18rem,86vw)]">
        <Sidebar mobile />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Vue 3.3+ 宏定义组件名
defineOptions({ name: 'Layout' });

import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import Sidebar from './components/Sidebar.vue';
import Navbar from './components/Navbar.vue';

const route = useRoute();
const mobileMenuOpen = ref(false);
const shellRef = ref<HTMLElement | null>(null);
const mainRef = ref<HTMLElement | null>(null);
const viewportWidth = ref(1440);

// 父组件统一识别当前视口，子页面通过父级 class 与 CSS 变量自然获得自适应能力。
const viewportMode = computed(() => {
  if (viewportWidth.value < 640) {
    return 'compact';
  }
  if (viewportWidth.value < 1024) {
    return 'comfortable';
  }
  return 'wide';
});

function syncViewportWidth() {
  viewportWidth.value = window.innerWidth || document.documentElement.clientWidth || 1440;
}

function resetMainScroll() {
  nextTick(() => {
    const el = mainRef.value;
    if (!el) {
      return;
    }
    el.scrollTop = 0;
    el.scrollLeft = 0;
  });
}

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false;
    resetMainScroll();
  }
);

onMounted(() => {
  syncViewportWidth();
  resetMainScroll();
  window.addEventListener('resize', syncViewportWidth, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewportWidth);
});
</script>

<style scoped>
.ys-app-shell {
  --ys-app-page-padding: clamp(0.75rem, 1.8vw, 2rem);
  --ys-app-content-gap: clamp(0.75rem, 1.4vw, 1.5rem);
  background:
      radial-gradient(circle at 10% -8%, rgba(31, 63, 95, 0.14), transparent 34%),
      radial-gradient(circle at 88% 4%, rgba(143, 110, 61, 0.08), transparent 30%),
      linear-gradient(180deg, #fbfcfe 0%, #ffffff 100%);
}

.ys-app-shell--compact {
  --ys-app-page-padding: 0.75rem;
  --ys-app-content-gap: 0.75rem;
}

.ys-app-shell--comfortable {
  --ys-app-page-padding: 1rem;
  --ys-app-content-gap: 1rem;
}

.ys-app-main {
  padding: var(--ys-app-page-padding);
  scroll-padding: var(--ys-app-page-padding);
}

.ys-app-content-frame {
  width: 100%;
  min-width: 0;
  min-height: auto;
}

@media (min-width: 1280px) {
  .ys-app-shell--wide .ys-app-main {
    padding-inline: max(2rem, calc((100vw - 1680px) / 2 + 2rem));
  }
}
</style>
