<template>
  <section
    ref="frameRef"
    class="responsive-page-frame"
    :class="[
      `responsive-page-frame--${viewportMode}`,
      route?.meta?.pageFluid ? 'responsive-page-frame--fluid' : '',
      route?.meta?.pageDense ? 'responsive-page-frame--dense' : ''
    ]"
    :data-route-name="route?.name || ''"
  >
    <slot />
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

defineOptions({ name: 'ResponsivePageFrame' })

const props = defineProps({
  route: {
    type: Object,
    required: true
  }
})

const frameRef = ref(null)
const frameWidth = ref(1440)

let resizeObserver = null
let mutationObserver = null
let enhanceFrame = 0

const viewportMode = computed(() => {
  if (frameWidth.value < 640) return 'compact'
  if (frameWidth.value < 1024) return 'tablet'
  if (frameWidth.value < 1360) return 'narrow'
  return 'wide'
})

function syncFrameWidth() {
  const el = frameRef.value
  frameWidth.value = el?.clientWidth || window.innerWidth || document.documentElement.clientWidth || 1440
}

function scheduleEnhance() {
  if (typeof window === 'undefined') return
  window.cancelAnimationFrame(enhanceFrame)
  enhanceFrame = window.requestAnimationFrame(() => {
    syncFrameWidth()
    enhanceResponsiveTables()
  })
}

function enhanceResponsiveTables() {
  const root = frameRef.value
  if (!root) return

  root.querySelectorAll('table.responsive-data-table').forEach((table) => {
    const labels = Array.from(table.querySelectorAll('thead th')).map((th) => cleanHeaderText(th.textContent))
    if (!labels.length) return

    table.querySelectorAll('tbody tr').forEach((row) => {
      Array.from(row.children)
        .filter((cell) => cell.tagName === 'TD')
        .forEach((cell, index) => {
          const isExplicit = cell.hasAttribute('data-label') && cell.dataset.responsiveAutoLabel !== '1'
          if (isExplicit || cell.getAttribute('colspan')) return

          const label = labels[index] || (index === row.children.length - 1 ? '操作' : '')
          if (!label) return
          cell.setAttribute('data-label', label)
          cell.dataset.responsiveAutoLabel = '1'
        })
    })
  })
}

function cleanHeaderText(text) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
}

function observeFrame() {
  const root = frameRef.value
  if (!root || typeof window === 'undefined') return

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(scheduleEnhance)
    resizeObserver.observe(root)
  }

  if (typeof MutationObserver !== 'undefined') {
    mutationObserver = new MutationObserver(scheduleEnhance)
    mutationObserver.observe(root, {
      childList: true,
      subtree: true
    })
  }
}

watch(
  () => props.route.fullPath,
  async () => {
    await nextTick()
    scheduleEnhance()
  }
)

onMounted(async () => {
  await nextTick()
  observeFrame()
  scheduleEnhance()
  window.addEventListener('resize', scheduleEnhance, { passive: true })
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(enhanceFrame)
  window.removeEventListener('resize', scheduleEnhance)
  resizeObserver?.disconnect()
  mutationObserver?.disconnect()
})
</script>
