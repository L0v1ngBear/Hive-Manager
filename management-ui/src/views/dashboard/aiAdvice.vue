<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto space-y-6">
    <header class="relative overflow-hidden rounded-[2rem] bg-primary text-white p-6 md:p-8 shadow-lg shadow-primary/20">
      <div class="absolute -right-16 -top-20 h-64 w-64 rounded-full bg-white/10"></div>
      <div class="absolute right-20 bottom-0 h-28 w-28 rounded-full bg-white/5"></div>
      <div class="relative z-10 flex flex-col lg:flex-row lg:items-end justify-between gap-6">
        <div class="min-w-0">
          <p class="text-xs font-black tracking-[0.24em] uppercase text-white/70">Business Advice</p>
          <h1 class="mt-3 text-3xl md:text-5xl font-black tracking-tight">AI 经营建议</h1>
          <p class="mt-4 max-w-3xl text-sm md:text-base leading-7 text-white/80">
            系统会结合库存、订单、客户、员工、质量和审批数据，整理成管理层可以直接跟进的建议、责任方向和复盘指标。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white text-primary font-black text-sm hover:bg-white/90 transition-colors"
            @click="fetchAdvices(true)"
          >
            <span class="material-symbols-outlined text-[20px] leading-none">refresh</span>
            刷新建议
          </button>
          <button
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white/12 text-white font-black text-sm hover:bg-white/18 transition-colors"
            @click="router.push('/dashboard')"
          >
            <span class="material-symbols-outlined text-[20px] leading-none">arrow_back</span>
            返回总览
          </button>
        </div>
      </div>
    </header>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <article class="rounded-3xl bg-surface-container-lowest p-5 shadow-sm ring-1 ring-outline-variant/20">
        <p class="text-xs font-black tracking-widest text-on-surface-variant uppercase">建议总数</p>
        <p class="mt-3 text-4xl font-black text-on-surface">{{ advices.length }}</p>
        <p class="mt-2 text-xs text-on-surface-variant">当前可见建议</p>
      </article>
      <article class="rounded-3xl bg-amber-50 p-5 shadow-sm ring-1 ring-amber-200">
        <p class="text-xs font-black tracking-widest text-amber-700 uppercase">重点关注</p>
        <p class="mt-3 text-4xl font-black text-amber-700">{{ warningCount }}</p>
        <p class="mt-2 text-xs text-amber-700/80">建议今日内跟进</p>
      </article>
      <article class="rounded-3xl bg-sky-50 p-5 shadow-sm ring-1 ring-sky-200">
        <p class="text-xs font-black tracking-widest text-sky-700 uppercase">覆盖维度</p>
        <p class="mt-3 text-4xl font-black text-sky-700">{{ activeModuleCount }}</p>
        <p class="mt-2 text-xs text-sky-700/80">库存、订单、客户、员工等</p>
      </article>
      <article class="rounded-3xl bg-emerald-50 p-5 shadow-sm ring-1 ring-emerald-200">
        <p class="text-xs font-black tracking-widest text-emerald-700 uppercase">稳定项</p>
        <p class="mt-3 text-4xl font-black text-emerald-700">{{ successCount }}</p>
        <p class="mt-2 text-xs text-emerald-700/80">继续保持巡检</p>
      </article>
    </section>

    <section v-if="dailyBrief" class="relative overflow-hidden rounded-[2rem] bg-[#172033] text-white p-6 md:p-7 shadow-lg">
      <div class="absolute -right-12 -top-16 h-48 w-48 rounded-full bg-white/8"></div>
      <div class="absolute right-24 bottom-0 h-24 w-24 rounded-full bg-amber-300/10"></div>
      <div class="relative z-10 grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_320px] gap-6">
        <div>
          <p class="text-xs font-black tracking-[0.22em] uppercase text-white/45">今日简报</p>
          <h2 class="mt-3 text-2xl md:text-3xl font-black">{{ dailyBrief.title || '今日经营简报' }}</h2>
          <p class="mt-3 text-sm leading-7 text-white/75">{{ dailyBrief.executiveSummary }}</p>
          <div class="mt-5 rounded-2xl bg-white/8 border border-white/10 p-4">
            <p class="text-[10px] font-black tracking-widest text-white/45 uppercase">优先动作</p>
            <p class="mt-2 text-sm leading-7 font-bold text-white">{{ dailyBrief.firstAction || '保持日常数据录入和巡检节奏。' }}</p>
          </div>
        </div>
        <div class="grid grid-cols-3 xl:grid-cols-1 gap-3">
          <div class="rounded-2xl bg-white/8 border border-white/10 p-4">
            <p class="text-[10px] font-black tracking-widest text-white/45 uppercase">重点风险</p>
            <p class="mt-2 text-2xl font-black text-amber-200">{{ dailyBrief.riskCount || 0 }}</p>
          </div>
          <div class="rounded-2xl bg-white/8 border border-white/10 p-4">
            <p class="text-[10px] font-black tracking-widest text-white/45 uppercase">行动项</p>
            <p class="mt-2 text-2xl font-black text-sky-200">{{ dailyBrief.urgentActionCount || 0 }}</p>
          </div>
          <div class="rounded-2xl bg-white/8 border border-white/10 p-4">
            <p class="text-[10px] font-black tracking-widest text-white/45 uppercase">观察项</p>
            <p class="mt-2 text-2xl font-black text-emerald-200">{{ dailyBrief.watchCount || 0 }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-[2rem] bg-surface-container-lowest p-4 md:p-5 shadow-sm ring-1 ring-outline-variant/20">
      <div class="flex flex-col xl:flex-row xl:items-center justify-between gap-4">
        <div class="min-w-0">
          <h2 class="text-xl font-black text-on-surface">建议列表</h2>
          <p class="mt-1 text-sm text-on-surface-variant">只展示当前用户有权限查看的经营建议。</p>
        </div>
        <div class="flex flex-col sm:flex-row gap-3">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="item in categoryFilters"
              :key="item.value"
              class="px-3 py-2 rounded-xl text-xs font-black border transition-colors"
              :class="activeCategory === item.value ? 'bg-primary text-white border-primary' : 'bg-white text-on-surface-variant border-outline-variant/30 hover:bg-primary/5'"
              @click="activeCategory = item.value"
            >
              {{ item.label }}
              <span v-if="item.count !== undefined" class="opacity-75">({{ item.count }})</span>
            </button>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="item in priorityFilters"
              :key="item.value"
              class="px-3 py-2 rounded-xl text-xs font-black border transition-colors"
              :class="activePriority === item.value ? 'bg-[#172033] text-white border-[#172033]' : 'bg-white text-on-surface-variant border-outline-variant/30 hover:bg-surface-container-high'"
              @click="activePriority = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-if="loading" class="grid grid-cols-1 xl:grid-cols-2 gap-5">
      <article v-for="index in 4" :key="index" class="rounded-[2rem] bg-surface-container-lowest p-6 shadow-sm ring-1 ring-outline-variant/20 animate-pulse">
        <div class="h-5 w-36 rounded-full bg-surface-container-high"></div>
        <div class="mt-5 h-8 w-3/4 rounded-full bg-surface-container-high"></div>
        <div class="mt-4 h-4 w-full rounded-full bg-surface-container-high"></div>
        <div class="mt-2 h-4 w-5/6 rounded-full bg-surface-container-high"></div>
      </article>
    </section>

    <section v-else class="grid grid-cols-1 xl:grid-cols-2 gap-5">
      <article
        v-for="item in filteredAdvices"
        :key="item.sampleKey || `${item.category}-${item.title}`"
        class="rounded-[2rem] border p-5 md:p-6 shadow-sm transition-colors"
        :class="adviceCardClass(item.level)"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex items-start gap-3 min-w-0">
            <div class="h-12 w-12 rounded-2xl flex items-center justify-center shrink-0" :class="adviceIconClass(item.level)">
              <span class="material-symbols-outlined text-[26px] leading-none">{{ item.icon || categoryIcon(item.category) }}</span>
            </div>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="px-2.5 py-1 rounded-full text-[11px] font-black" :class="priorityClass(displayPriority(item))">
                  {{ displayPriority(item) }}
                </span>
                <span class="text-xs font-bold text-on-surface-variant">{{ categoryTitle(item.category) }}</span>
              </div>
              <h3 class="mt-3 text-xl md:text-2xl font-black text-on-surface leading-tight">{{ item.title || '未命名经营建议' }}</h3>
              <p class="mt-2 text-sm leading-7 text-on-surface-variant">{{ item.summary || categorySubtitle(item.category) }}</p>
            </div>
          </div>
          <div class="shrink-0 text-right">
            <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">风险分</p>
            <p class="mt-1 text-2xl font-black" :class="riskScoreClass(item.riskScore)">{{ item.riskScore ?? '--' }}</p>
          </div>
        </div>

        <div class="mt-5 rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
          <p class="text-xs font-black text-on-surface-variant tracking-widest uppercase">建议动作</p>
          <p class="mt-2 text-sm leading-7 text-on-surface-variant">{{ item.suggestion || '请先补齐相关业务数据，系统会继续观察并生成更明确的建议。' }}</p>
        </div>

        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
          <div v-if="item.firstAction" class="rounded-2xl bg-primary/10 border border-primary/20 p-4">
            <p class="text-[10px] font-black tracking-widest text-primary uppercase">第一步</p>
            <p class="mt-2 text-xs leading-6 font-bold text-on-surface">{{ item.firstAction }}</p>
          </div>
          <div class="rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
            <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">责任方向</p>
            <p class="mt-2 text-xs leading-6 font-bold text-on-surface">{{ item.ownerDepartment || ownerByCategory(item.category) }}</p>
          </div>
          <div v-if="item.impactText" class="rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
            <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">影响范围</p>
            <p class="mt-2 text-xs leading-6 font-bold text-on-surface">{{ item.impactText }}</p>
          </div>
          <div v-if="item.reviewMetric" class="rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
            <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">复盘指标</p>
            <p class="mt-2 text-xs leading-6 font-bold text-on-surface">{{ item.reviewMetric }}</p>
          </div>
        </div>

        <div v-if="safeList(item.actionSteps).length" class="mt-4 rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
          <p class="text-xs font-black text-on-surface-variant tracking-widest uppercase">执行步骤</p>
          <div class="mt-3 grid grid-cols-1 gap-2">
            <p
              v-for="(step, index) in safeList(item.actionSteps).slice(0, 5)"
              :key="`${item.sampleKey || item.title}-step-${index}`"
              class="rounded-xl bg-surface-container-low px-3 py-2 text-xs leading-6 text-on-surface-variant"
            >
              {{ index + 1 }}. {{ step }}
            </p>
          </div>
        </div>

        <div v-if="item.trackingHint || item.timeWindow || item.reviewDeadline" class="mt-4 rounded-2xl bg-amber-50/80 border border-amber-100 p-4">
          <p class="text-xs font-black text-amber-800 tracking-widest uppercase">跟进提醒</p>
          <p v-if="item.timeWindow" class="mt-2 text-xs leading-6 text-amber-950/80">处理窗口：{{ item.timeWindow }}</p>
          <p v-if="item.reviewDeadline" class="mt-1 text-xs leading-6 text-amber-950/80">复盘时间：{{ item.reviewDeadline }}</p>
          <p v-if="item.trackingHint" class="mt-1 text-xs leading-6 text-amber-950/80">{{ item.trackingHint }}</p>
        </div>

        <div class="mt-5 flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-2">
            <span
              v-for="tag in safeList(item.stakeholderTags).slice(0, 5)"
              :key="tag"
              class="rounded-full bg-white/75 px-2.5 py-1 text-[11px] font-bold text-on-surface-variant border border-outline-variant/20"
            >
              {{ tag }}
            </span>
          </div>
          <div class="flex flex-wrap items-center gap-2 shrink-0">
            <button
              class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-white/80 text-primary text-xs font-black border border-primary/20 hover:bg-primary/10 transition-colors"
              @click="submitAdviceFeedback(item, 'useful')"
            >
              有帮助
            </button>
            <button
              class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-white/80 text-on-surface-variant text-xs font-black border border-outline-variant/40 hover:bg-surface-container-high transition-colors"
              @click="submitAdviceFeedback(item, 'irrelevant')"
            >
              不准确
            </button>
            <button
              class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-600 text-white text-xs font-black hover:bg-emerald-700 transition-colors"
              @click="submitAdviceFeedback(item, 'resolved')"
            >
              已处理
            </button>
            <button
              v-if="item.route"
              class="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-primary text-white text-sm font-black hover:bg-primary/90 transition-colors"
              @click="openAdviceRoute(item)"
            >
              {{ item.actionLabel || '查看详情' }}
              <span class="material-symbols-outlined text-[18px] leading-none">arrow_forward</span>
            </button>
          </div>
        </div>
      </article>

      <div v-if="filteredAdvices.length === 0" class="xl:col-span-2 rounded-3xl bg-surface-container-lowest p-12 text-center text-on-surface-variant flex flex-col items-center justify-center gap-3">
        <span class="material-symbols-outlined text-5xl opacity-50">done_all</span>
        <p>当前筛选条件下暂无建议。请继续沉淀业务数据，系统会在识别到有效信号后生成经营建议。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { feedbackDashboardAiAdvice, getDashboardAiAdvices, getDashboardAiBrief } from './api/dashboard.js'
import { trackBehavior } from '@/utils/behavior'

defineOptions({ name: 'DashboardAiAdvice' })

const router = useRouter()
const loading = ref(false)
const advices = ref([])
const dailyBrief = ref(null)
const activeCategory = ref('all')
const activePriority = ref('all')

const moduleCatalog = [
  { category: 'inventory' },
  { category: 'order' },
  { category: 'delivery' },
  { category: 'customer' },
  { category: 'employee' },
  { category: 'quality' },
  { category: 'finance' },
  { category: 'operation' },
  { category: 'overview' }
]

const categoryFilters = computed(() => [
  { value: 'all', label: '全部', count: advices.value.length },
  ...categoryStats.value.filter((item) => item.count > 0).map((item) => ({
    value: item.category,
    label: categoryTitle(item.category),
    count: item.count
  }))
])

const priorityFilters = computed(() => [
  { value: 'all', label: '全部' },
  { value: 'P0', label: `P0 ${countByPriority('P0')}` },
  { value: 'P1', label: `P1 ${countByPriority('P1')}` },
  { value: 'P2', label: `P2 ${countByPriority('P2')}` },
  { value: 'P3', label: `P3 ${countByPriority('P3')}` }
])

const categoryStats = computed(() => {
  const map = new Map()
  advices.value.forEach((item) => {
    const category = item?.category || 'overview'
    map.set(category, (map.get(category) || 0) + 1)
  })
  return moduleCatalog.map((item) => ({ ...item, count: map.get(item.category) || 0 }))
})

const filteredAdvices = computed(() => advices.value
  .filter((item) => activeCategory.value === 'all' || (item.category || 'overview') === activeCategory.value)
  .filter((item) => activePriority.value === 'all' || displayPriority(item) === activePriority.value)
  .slice()
  .sort((a, b) => priorityWeight(displayPriority(a)) - priorityWeight(displayPriority(b))))

const warningCount = computed(() => advices.value.filter((item) => item.level === 'warning').length)
const successCount = computed(() => advices.value.filter((item) => item.level === 'success').length)
const activeModuleCount = computed(() => categoryStats.value.filter((item) => item.count > 0).length)

onMounted(() => fetchAdvices(false))

async function fetchAdvices(refresh = false) {
  loading.value = true
  try {
    const params = refresh ? { refresh: true } : {}
    const [adviceResult, briefResult] = await Promise.all([
      getDashboardAiAdvices(params),
      getDashboardAiBrief(params)
    ])
    advices.value = Array.isArray(adviceResult) ? adviceResult.filter(Boolean) : []
    dailyBrief.value = briefResult || null
    trackAdviceExposure()
  } catch (error) {
    ElMessage.error(error?.msg || 'AI 经营建议加载失败')
  } finally {
    loading.value = false
  }
}

function trackAdviceExposure() {
  advices.value.slice(0, 20).forEach((item) => {
    trackBehavior({
      eventType: 'ai_advice_view',
      pagePath: '/dashboard/ai-advices',
      module: 'ai_advice',
      targetType: 'advice',
      targetId: `${item.category || 'overview'}:${item.title || ''}`,
      action: 'view',
      source: 'ai_center',
      metadata: {
        category: item.category,
        level: item.level,
        priority: item.priority,
        route: item.route
      }
    })
  })
}

function openAdviceRoute(item) {
  trackBehavior({
    eventType: 'ai_advice_click',
    pagePath: '/dashboard/ai-advices',
    module: 'ai_advice',
    targetType: 'advice',
    targetId: `${item.category || 'overview'}:${item.title || ''}`,
    action: 'click',
    source: 'ai_center',
    metadata: {
      category: item.category,
      level: item.level,
      priority: item.priority,
      route: item.route
    }
  })
  router.push(item.route)
}

async function submitAdviceFeedback(item, feedbackType) {
  if (!item?.sampleKey) {
    ElMessage.warning('该建议正在生成样本，请稍后再反馈')
    return
  }
  try {
    await feedbackDashboardAiAdvice({
      sampleKey: item.sampleKey,
      feedbackType
    })
    trackBehavior({
      eventType: 'ai_advice_feedback',
      pagePath: '/dashboard/ai-advices',
      module: 'ai_advice',
      targetType: 'advice',
      targetId: item.sampleKey,
      action: feedbackType,
      source: 'ai_center',
      metadata: {
        category: item.category,
        level: item.level,
        priority: item.priority,
        title: item.title
      }
    })
    ElMessage.success(resolveFeedbackMessage(feedbackType))
  } catch (error) {
    ElMessage.error(error?.msg || '反馈提交失败')
  }
}

function resolveFeedbackMessage(feedbackType) {
  if (feedbackType === 'irrelevant') return '已记录为不准确，后续会减少类似建议'
  if (feedbackType === 'resolved') return '已记录为已处理，后续会用于闭环复盘'
  return '已记录为有帮助'
}

const categoryMeta = {
  inventory: { title: '库存周转', subtitle: '库存余量、低库存型号、近期出入库', icon: 'inventory_2' },
  order: { title: '订单履约', subtitle: '销售订单、生产订单、履约状态', icon: 'receipt_long' },
  delivery: { title: '交付发货', subtitle: '交付日期、发货状态、物流完整度', icon: 'local_shipping' },
  customer: { title: '客户经营', subtitle: '复购周期、客户活跃度、重点客户贡献', icon: 'handshake' },
  employee: { title: '员工效率', subtitle: '员工状态、考勤异常、请假审批与上下级关系', icon: 'groups' },
  quality: { title: '质量管控', subtitle: '质量异常数量、损失金额、重复问题', icon: 'assignment_late' },
  finance: { title: '财务健康', subtitle: '订单金额、损耗金额、审批事项', icon: 'payments' },
  operation: { title: '运营节奏', subtitle: '跨部门协同和经营闭环', icon: 'fact_check' },
  overview: { title: '经营总览', subtitle: '总览大盘核心指标', icon: 'monitoring' }
}

function categoryTitle(category) {
  return categoryMeta[category]?.title || '经营建议'
}

function categorySubtitle(category) {
  return categoryMeta[category]?.subtitle || '补齐关键业务信息，形成可执行管理闭环'
}

function categoryIcon(category) {
  return categoryMeta[category]?.icon || 'tips_and_updates'
}

function ownerByCategory(category) {
  const map = {
    inventory: '仓库 / 采购 / 销售',
    order: '销售 / 生产 / 仓库',
    delivery: '销售 / 仓库',
    customer: '销售负责人',
    employee: '人事 / 部门负责人',
    quality: '质检 / 生产',
    finance: '财务 / 经营管理',
    operation: '运营负责人'
  }
  return map[category] || '管理层'
}

function displayPriority(item) {
  return item?.priority || priorityText(item?.level)
}

function priorityText(level) {
  if (level === 'warning') return 'P1'
  if (level === 'success') return 'P3'
  return 'P2'
}

function priorityWeight(priority) {
  return { P0: 0, P1: 1, P2: 2, P3: 3 }[priority] || 9
}

function countByPriority(priority) {
  return advices.value.filter((item) => displayPriority(item) === priority).length
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

function priorityClass(priority) {
  if (priority === 'P0') return 'bg-red-100 text-red-800'
  if (priority === 'P1') return 'bg-amber-100 text-amber-800'
  if (priority === 'P3') return 'bg-emerald-100 text-emerald-800'
  return 'bg-sky-100 text-sky-800'
}

function riskScoreClass(score) {
  const value = Number(score ?? 0)
  if (value >= 85) return 'text-red-600'
  if (value >= 70) return 'text-amber-600'
  if (value <= 35) return 'text-emerald-600'
  return 'text-primary'
}

function safeList(value) {
  return Array.isArray(value) ? value.filter(Boolean) : []
}
</script>
