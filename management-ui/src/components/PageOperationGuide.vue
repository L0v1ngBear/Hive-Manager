<template>
  <div v-if="guide" class="page-operation-guide">
    <nav class="page-operation-guide__location" aria-label="当前位置">
      <span>{{ guide.group }}</span>
      <span aria-hidden="true">/</span>
      <strong aria-current="page">{{ route.meta?.title }}</strong>
    </nav>
    <details :key="route.name" class="page-operation-guide__help">
      <summary>操作指引</summary>
      <ol aria-label="本页操作步骤">
        <li v-for="step in guide.steps" :key="step">{{ step }}</li>
      </ol>
    </details>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { pageGuides } from '@/config/pageGuides.js'

const props = defineProps({ route: { type: Object, required: true } })
const guide = computed(() => pageGuides[props.route.name])
</script>

<style scoped>
.page-operation-guide {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: .5rem 1rem;
  margin-bottom: .75rem;
  font-size: .8125rem;
}
.page-operation-guide__location {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: .5rem;
}
.page-operation-guide__help { min-width: 0; }
.page-operation-guide__help summary { cursor: pointer; padding: .375rem 0; font-weight: 600; }
.page-operation-guide__help[open] { flex-basis: 100%; }
.page-operation-guide__help ol {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  list-style: decimal;
  padding: .75rem 0 .75rem 1.25rem;
  gap: .5rem 2rem;
  line-height: 1.65;
}
@media (max-width: 768px) {
  .page-operation-guide__help ol { grid-template-columns: minmax(0, 1fr); }
}
@media print { .page-operation-guide { display: none; } }
</style>
