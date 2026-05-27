<template>
  <teleport to="body">
    <transition name="global-request-fade">
      <div v-if="requestStatus.visible" class="global-request-overlay">
        <div class="global-request-card">
          <span class="material-symbols-outlined global-request-icon">progress_activity</span>
          <span>{{ requestStatus.message }}</span>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { useRequestStatusStore } from '@/stores/requestStatus'

const requestStatus = useRequestStatusStore()
</script>

<style scoped>
.global-request-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  pointer-events: none;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 18px;
  background: linear-gradient(180deg, rgb(15 23 42 / 0.10), transparent 160px);
}

.global-request-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 164px;
  padding: 12px 18px;
  border: 1px solid rgb(245 158 11 / 0.28);
  border-radius: 999px;
  color: #1f2937;
  background: rgb(255 251 235 / 0.96);
  box-shadow: 0 18px 42px rgb(31 41 55 / 0.18);
  font-weight: 700;
}

.global-request-icon {
  color: #1f3f5f;
  font-size: 22px;
  animation: global-request-spin 1s linear infinite;
}

.global-request-fade-enter-active,
.global-request-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.global-request-fade-enter-from,
.global-request-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@keyframes global-request-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
