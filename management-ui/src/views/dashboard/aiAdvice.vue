<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto space-y-6">
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <p class="text-sm font-bold tracking-[0.2em] text-primary/70 uppercase">全局决策洞察</p>
        <h1 class="mt-2 text-3xl md:text-4xl font-black tracking-tight text-on-surface">AI 经营建议中心</h1>
        <p class="mt-3 text-base text-on-surface-variant max-w-3xl">
          深度穿透业务链路，为您提供各个维度的精细化异常诊断与执行建议。
        </p>
      </div>
      <button @click="router.push('/dashboard')" class="px-5 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg hover:bg-surface-container-highest transition-colors text-sm">
        返回总览
      </button>
    </header>

    <section class="grid grid-cols-1 lg:grid-cols-[1.25fr_0.75fr] gap-5">
      <article class="rounded-3xl bg-primary text-white p-7 shadow-md shadow-primary/20 overflow-hidden relative">
        <div class="absolute -right-8 -top-8 w-48 h-48 rounded-full bg-white/10"></div>
        <p class="text-xs font-black tracking-[0.24em] uppercase text-white/70">Decision Support</p>
        <h2 class="mt-3 text-2xl md:text-3xl font-black leading-tight">从异常识别到执行闭环</h2>
        <p class="mt-4 text-sm md:text-base leading-7 text-white/80 max-w-2xl">
          当前阶段基于系统已接入的库存、订单、客户、次品、审批与出库打印数据，优先提供可解释、可追踪、可落地的经营建议。
        </p>
        <div class="mt-6 grid grid-cols-3 gap-3 max-w-xl">
          <div class="rounded-2xl bg-white/12 p-4">
            <p class="text-3xl font-black">{{ advices.length }}</p>
            <p class="text-xs text-white/70 mt-1">经营建议</p>
          </div>
          <div class="rounded-2xl bg-white/12 p-4">
            <p class="text-3xl font-black">{{ warningCount }}</p>
            <p class="text-xs text-white/70 mt-1">重点关注</p>
          </div>
          <div class="rounded-2xl bg-white/12 p-4">
            <p class="text-3xl font-black">{{ categoryStats.filter((item) => item.count > 0).length }}</p>
            <p class="text-xs text-white/70 mt-1">覆盖维度</p>
          </div>
        </div>
      </article>

      <article class="rounded-3xl bg-surface-container-lowest p-6 shadow-sm ring-1 ring-outline-variant/20">
        <h2 class="text-lg font-black text-on-surface">管理层阅读指引</h2>
        <div class="mt-4 space-y-3 text-sm text-on-surface-variant leading-6">
          <p>🔴/🟡 红色/黄色建议优先进入晨会或周会跟进，明确责任人与截止时间。</p>
          <p>🔵 蓝色建议用于优化流程，适合安排到部门例会或专项复盘中推进。</p>
          <p>🟢 绿色建议说明当前维度相对平稳，但仍建议保留巡检节奏。</p>
        </div>
      </article>
    </section>

    <section class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <article
          v-for="item in categoryStats"
          :key="item.category"
          class="rounded-2xl p-5 bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 cursor-pointer hover:-translate-y-0.5 hover:shadow-md transition-all flex flex-col justify-between"
          :class="activeCategory === item.category ? 'ring-primary bg-primary/5' : ''"
          @click="activeCategory = item.category"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-black tracking-widest uppercase text-primary">{{ categoryTitle(item.category) }}</p>
            <p class="text-xs text-on-surface-variant mt-2 leading-5">{{ categorySubtitle(item.category) }}</p>
          </div>
          <div class="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center shrink-0">
            <span class="material-symbols-outlined text-primary text-[34px] leading-none">
              {{ categoryIcon(item.category) }}
            </span>
          </div>
        </div>
        <div class="mt-5 flex items-end justify-between">
          <p class="text-4xl font-black text-on-surface leading-none">{{ item.count }}</p>
          <p class="text-xs text-on-surface-variant font-medium">条洞察</p>
        </div>
      </article>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[260px_minmax(0,1fr)] gap-6">
      <aside class="rounded-2xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/20 p-4 h-fit sticky top-4">
        <button
            v-for="item in filters"
            :key="item.value"
            @click="activeCategory = item.value"
            class="w-full flex items-center justify-between rounded-xl px-4 py-3 text-left text-sm font-bold transition-colors"
            :class="activeCategory === item.value ? 'bg-primary text-white shadow-md' : 'text-on-surface hover:bg-surface-container-high'"
        >
          <span>{{ item.label }}</span>
          <span class="text-xs opacity-80">{{ item.count }}</span>
        </button>
      </aside>

      <main class="space-y-4 relative min-h-[360px]">
        <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center rounded-2xl">
          <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
        </div>

        <article
            v-for="(item, index) in filteredAdvices"
            :key="`${item.category}-${item.title}-${index}`"
            class="rounded-2xl p-5 border bg-surface-container-lowest shadow-sm hover:shadow-md transition-all cursor-pointer"
            :class="adviceCardClass(item.level)"
            @click="item.route && router.push(item.route)"
        >
          <div class="flex flex-col md:flex-row md:items-start gap-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0" :class="adviceIconClass(item.level)">
              <span class="material-symbols-outlined text-[26px] leading-none">
                {{ item.icon || 'tips_and_updates' }}
              </span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-primary/10 text-primary">{{ categoryTitle(item.category) }}</span>
                <span class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-surface-container-high text-on-surface-variant">{{ adviceLevelText(item.level) }}</span>
              </div>
              <h2 class="mt-3 text-lg font-black text-on-surface">{{ item.title }}</h2>
              <p class="mt-1 text-xs font-bold text-on-surface-variant">{{ categorySubtitle(item.category) }}</p>
              <p class="mt-2 text-sm leading-6 text-on-surface">{{ item.summary }}</p>
              <div class="mt-4 rounded-xl bg-white/70 border border-outline-variant/20 p-4">
                <p class="text-xs font-black text-on-surface-variant tracking-widest uppercase mb-2">决策建议 / 执行抓手</p>
                <p class="text-sm leading-7 text-on-surface-variant">{{ item.suggestion }}</p>
              </div>
            </div>
          </div>
        </article>

        <div v-if="!loading && filteredAdvices.length === 0" class="rounded-2xl bg-surface-container-lowest p-12 text-center text-on-surface-variant flex flex-col items-center justify-center gap-3">
          <span class="material-symbols-outlined text-4xl opacity-50">done_all</span>
          <p>当前维度暂无明显异动。建议继续保持日常巡检，待更多业务数据沉淀后会生成更细颗粒度的洞察。</p>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getDashboardAiAdvices } from './api/dashboard.js'

defineOptions({ name: 'DashboardAiAdvice' })

const router = useRouter()
const loading = ref(false)
const advices = ref([])
const activeCategory = ref('all')

const filters = computed(() => {
  const stats = categoryStats.value
  return [
    { value: 'all', label: '全部建议', count: advices.value.length },
    ...stats.map((item) => ({ value: item.category, label: categoryTitle(item.category), count: item.count }))
  ]
})

const categoryStats = computed(() => {
  const map = new Map()
  advices.value.forEach((item) => map.set(item.category || 'overview', (map.get(item.category || 'overview') || 0) + 1))
  return moduleCatalog.map((item) => ({ ...item, count: map.get(item.category) || 0 }))
})

const filteredAdvices = computed(() => {
  if (activeCategory.value === 'all') {
    return advices.value
  }
  return advices.value.filter((item) => (item.category || 'overview') === activeCategory.value)
})

onMounted(fetchAdvices)

async function fetchAdvices() {
  loading.value = true
  try {
    advices.value = await getDashboardAiAdvices()
  } catch (error) {
    ElMessage.error(error?.msg || 'AI 建议加载失败')
  } finally {
    loading.value = false
  }
}

const moduleCatalog = [
  { category: 'inventory' },
  { category: 'order' },
  { category: 'customer' },
  { category: 'quality' },
  { category: 'finance' },
  { category: 'operation' }
]

const warningCount = computed(() => advices.value.filter((item) => item.level === 'warning').length)

// 优化后的文案体系：更贴合管理者的视角与价值导向
const categoryMeta = {
  inventory: {
    title: '库存水位与周转',
    subtitle: '释放资金占用，优化物料流转率。',
    icon: 'inventory_2'
  },
  order: {
    title: '订单履约与交付',
    subtitle: '守住交付底线，打破产能瓶颈。',
    icon: 'receipt_long'
  },
  customer: {
    title: '客户客情与风控',
    subtitle: '守护核心资产，防范商业信用风险。',
    icon: 'handshake'
  },
  delivery: {
    title: '物流运输与发货',
    subtitle: '监控物流节点，确保成品准时送达。',
    icon: 'local_shipping'
  },
  quality: {
    title: '质量管控与溯源',
    subtitle: '锁定良率波动，从源头切断客诉。',
    icon: 'assignment_late'
  },
  finance: {
    title: '财务健康与成本',
    subtitle: '洞察现金流向，精算每一笔制造成本。',
    icon: 'payments'
  },
  operation: {
    title: '生产运营节奏',
    subtitle: '消除产线浪费，维持系统高效运转。',
    icon: 'fact_check'
  },
  overview: {
    title: '全局经营总览',
    subtitle: '大盘异动深度分析，协助逐项跟进处理。',
    icon: 'monitoring'
  }
}

function categoryTitle(category) {
  return categoryMeta[category]?.title || '其他经营洞察'
}

function categorySubtitle(category) {
  return categoryMeta[category]?.subtitle || '补齐关键业务信息，形成可执行管理闭环。'
}

function categoryIcon(category) {
  return categoryMeta[category]?.icon || 'tips_and_updates'
}

function adviceCardClass(level) {
  if (level === 'warning') return 'border-amber-200 bg-amber-50/50'
  if (level === 'success') return 'border-emerald-200 bg-emerald-50/50'
  return 'border-sky-200 bg-sky-50/50'
}

function adviceIconClass(level) {
  if (level === 'warning') return 'bg-amber-100 text-amber-700'
  if (level === 'success') return 'bg-emerald-100 text-emerald-700'
  return 'bg-sky-100 text-sky-700'
}

function adviceLevelText(level) {
  if (level === 'warning') return '重点关注'
  if (level === 'success') return '运行平稳'
  return '建议动作'
}
</script>