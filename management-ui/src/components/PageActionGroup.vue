<template>
  <div class="page-action-group" role="group" :aria-label="label">
    <span class="page-action-group-label">{{ label }}</span>
    <div class="page-action-group-controls"><slot /></div>
  </div>
</template>

<script setup>
defineProps({ label: { type: String, required: true } })
</script>

<style scoped>
.page-action-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .5rem 1rem;
  min-width: 0;
  max-width: 100%;
}
.page-action-group-label { flex: 0 0 auto; font-size: .8125rem; font-weight: 600; }
.page-action-group-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .5rem;
  min-width: 0;
  max-width: 100%;
}
.page-action-group-controls :deep(.el-button) { margin-left: 0; min-height: 2.25rem; max-width: 100%; }
@media (max-width: 767px) {
  .page-action-group-controls { flex: 1 1 100%; }
  .page-action-group-controls :deep(.el-button) { min-height: 2.5rem; }
}
@media print { .page-action-group { display: none; } }
</style>
