<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto space-y-6">
    <header class="relative overflow-hidden rounded-[2rem] bg-primary text-white p-7 md:p-8 shadow-lg shadow-primary/20">
      <div class="absolute -right-16 -top-20 h-64 w-64 rounded-full bg-white/10"></div>
      <div class="absolute right-20 bottom-0 h-28 w-28 rounded-full bg-white/5"></div>
      <div class="relative z-10 flex flex-col lg:flex-row lg:items-end justify-between gap-6">
        <div>
          <p class="text-xs font-black tracking-[0.24em] uppercase text-white/70">Global Decision Insights</p>
          <h1 class="mt-3 text-3xl md:text-5xl font-black tracking-tight">AI 全局决策建议中心</h1>
          <p class="mt-4 max-w-3xl text-sm md:text-base leading-7 text-white/80">
            基于库存、订单、客户、员工、次品和审批数据生成全局洞察，帮助管理层快速识别风险、明确责任部门，并推动处理闭环。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            @click="fetchAdvices(true)"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white text-primary font-black text-sm hover:bg-white/90 transition-colors"
          >
            <span class="material-symbols-outlined text-[20px] leading-none">refresh</span>
            刷新洞察
          </button>
          <button
            @click="router.push('/dashboard')"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white/12 text-white font-black text-sm hover:bg-white/18 transition-colors"
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
        <p class="mt-2 text-xs text-on-surface-variant">来自真实业务数据</p>
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

    <section class="grid grid-cols-1 xl:grid-cols-[280px_minmax(0,1fr)] gap-6">
      <aside class="space-y-4 xl:sticky xl:top-4 h-fit">
        <div class="rounded-3xl bg-surface-container-lowest p-4 shadow-sm ring-1 ring-outline-variant/20">
          <p class="px-2 text-xs font-black tracking-widest uppercase text-on-surface-variant">业务维度</p>
          <div class="mt-3 space-y-2">
            <button
              v-for="item in filters"
              :key="item.value"
              @click="activeCategory = item.value"
              class="w-full flex items-center justify-between rounded-2xl px-4 py-3 text-left text-sm font-black transition-colors"
              :class="activeCategory === item.value ? 'bg-primary text-white shadow-md' : 'text-on-surface hover:bg-surface-container-high'"
            >
              <span>{{ item.label }}</span>
              <span class="text-xs opacity-80">{{ item.count }}</span>
            </button>
          </div>
        </div>

        <div class="rounded-3xl bg-surface-container-lowest p-4 shadow-sm ring-1 ring-outline-variant/20">
          <p class="px-2 text-xs font-black tracking-widest uppercase text-on-surface-variant">管理优先级</p>
          <div class="mt-3 grid grid-cols-2 gap-2">
            <button
              v-for="item in priorityFilters"
              :key="item.value"
              @click="activePriority = item.value"
              class="rounded-2xl px-3 py-3 text-sm font-black transition-colors"
              :class="activePriority === item.value ? 'bg-on-surface text-white' : 'bg-surface-container-high text-on-surface'"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="rounded-3xl bg-[#0f172a] text-white p-5 shadow-sm">
          <p class="text-xs font-black tracking-widest uppercase text-white/50">闭环提示</p>
          <p class="mt-3 text-sm leading-6 text-white/80">
            P1 建议优先进入晨会或周会，明确责任部门、截止时间和复盘结果；P2 建议纳入部门例会推动。
          </p>
        </div>
      </aside>

      <main class="relative min-h-[420px] space-y-4">
        <div v-if="loading" class="absolute inset-0 bg-white/70 backdrop-blur-sm z-10 flex items-center justify-center rounded-3xl">
          <span class="material-symbols-outlined text-primary text-4xl animate-spin">progress_activity</span>
        </div>

        <article
          v-for="(item, index) in filteredAdvices"
          :key="`${item.category}-${item.title}-${index}`"
          class="group rounded-3xl border bg-surface-container-lowest p-5 md:p-6 shadow-sm hover:shadow-md transition-all"
          :class="adviceCardClass(item.level)"
        >
          <div class="flex flex-col md:flex-row gap-5">
            <div class="flex md:flex-col items-center md:items-start gap-3 md:w-28 shrink-0">
              <div class="w-14 h-14 rounded-2xl flex items-center justify-center" :class="adviceIconClass(item.level)">
                <span class="material-symbols-outlined text-[34px] leading-none">{{ item.icon || categoryIcon(item.category) }}</span>
              </div>
              <span class="rounded-full px-3 py-1 text-xs font-black" :class="priorityClass(item.priority)">
                {{ item.priority || priorityText(item.level) }}
              </span>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-primary/10 text-primary">{{ categoryTitle(item.category) }}</span>
                <span v-if="item.decisionType" class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-white/80 text-primary">{{ item.decisionType }}</span>
                <span class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-surface-container-high text-on-surface-variant">{{ adviceLevelText(item.level) }}</span>
                <span class="px-2 py-0.5 rounded-md text-[10px] font-black tracking-widest bg-white/70 text-on-surface-variant">{{ item.generatedAt || '实时生成' }}</span>
              </div>

              <h2 class="mt-3 text-xl font-black text-on-surface">{{ item.title }}</h2>
              <p class="mt-2 text-sm leading-7 text-on-surface">{{ item.summary }}</p>

              <div class="mt-4 grid grid-cols-1 lg:grid-cols-3 gap-3">
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">责任牵头</p>
                  <p class="mt-2 text-sm font-bold text-on-surface">{{ item.ownerDepartment || ownerByCategory(item.category) }}</p>
                </div>
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4 lg:col-span-2">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">数据口径</p>
                  <p class="mt-2 text-sm font-bold text-on-surface">{{ item.metricText || categorySubtitle(item.category) }}</p>
                </div>
              </div>

              <div
                v-if="item.riskScore !== undefined || item.impactText || item.timeWindow || item.reviewMetric"
                class="mt-3 grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3"
              >
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">风险评分</p>
                  <p class="mt-2 text-2xl font-black" :class="riskScoreClass(item.riskScore)">{{ item.riskScore ?? '--' }}<span class="text-xs">/100</span></p>
                </div>
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">影响范围</p>
                  <p class="mt-2 text-xs leading-5 font-bold text-on-surface">{{ item.impactText || categorySubtitle(item.category) }}</p>
                </div>
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">处理窗口</p>
                  <p class="mt-2 text-sm font-black text-on-surface">{{ item.timeWindow || '本周内跟进' }}</p>
                </div>
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">复盘指标</p>
                  <p class="mt-2 text-xs leading-5 font-bold text-on-surface">{{ item.reviewMetric || '异常关闭率、复盘完成率' }}</p>
                </div>
              </div>

              <div v-if="item.firstAction" class="mt-4 rounded-2xl bg-primary/10 border border-primary/20 p-4">
                <p class="text-xs font-black text-primary tracking-widest uppercase mb-2">第一动作</p>
                <p class="text-sm leading-7 font-bold text-on-surface">{{ item.firstAction }}</p>
              </div>

              <div class="mt-4 rounded-2xl bg-white/75 border border-outline-variant/20 p-4">
                <p class="text-xs font-black text-on-surface-variant tracking-widest uppercase mb-2">决策建议 / 执行抓手</p>
                <p class="text-sm leading-7 text-on-surface-variant">{{ item.suggestion }}</p>
              </div>

              <div v-if="item.reasoning || item.confidence" class="mt-3 flex flex-col lg:flex-row gap-3">
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4 lg:flex-1">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">分析依据</p>
                  <p class="mt-2 text-xs leading-6 text-on-surface-variant">{{ item.reasoning || '基于经营快照与本地规则生成。' }}</p>
                </div>
                <div class="rounded-2xl bg-white/70 border border-outline-variant/20 p-4 lg:w-36">
                  <p class="text-[10px] font-black tracking-widest text-on-surface-variant uppercase">置信度</p>
                  <p class="mt-2 text-2xl font-black text-primary">{{ item.confidence || '--' }}<span class="text-xs">%</span></p>
                  <p class="mt-1 text-[10px] text-on-surface-variant">{{ sourceText(item.sourceType) }}</p>
                </div>
              </div>

              <div class="mt-4 flex flex-col md:flex-row md:items-center justify-between gap-3">
                <p class="text-xs text-on-surface-variant leading-5">{{ item.trackingHint || '建议纳入部门例会跟进，形成处理和复盘记录。' }}</p>
                <div class="flex flex-wrap items-center gap-2 shrink-0">
                  <button
                    @click="submitAdviceFeedback(item, 'useful')"
                    class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-white/80 text-primary text-xs font-black border border-primary/20 hover:bg-primary/10 transition-colors"
                  >
                    <span class="material-symbols-outlined text-[17px] leading-none">thumb_up</span>
                    有价值
                  </button>
                  <button
                    @click="submitAdviceFeedback(item, 'irrelevant')"
                    class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-white/80 text-on-surface-variant text-xs font-black border border-outline-variant/40 hover:bg-surface-container-high transition-colors"
                  >
                    <span class="material-symbols-outlined text-[17px] leading-none">thumb_down</span>
                    不准确
                  </button>
                  <button
                    @click="submitAdviceFeedback(item, 'resolved')"
                    class="inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-600 text-white text-xs font-black hover:bg-emerald-700 transition-colors"
                  >
                    <span class="material-symbols-outlined text-[17px] leading-none">task_alt</span>
                    已处理
                  </button>
                  <button
                    v-if="item.route"
                    @click="openAdviceRoute(item)"
                    class="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-primary text-white text-sm font-black hover:bg-primary/90 transition-colors"
                  >
                    {{ item.actionLabel || '查看详情' }}
                    <span class="material-symbols-outlined text-[18px] leading-none">arrow_forward</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </article>

        <div v-if="!loading && filteredAdvices.length === 0" class="rounded-3xl bg-surface-container-lowest p-12 text-center text-on-surface-variant flex flex-col items-center justify-center gap-3">
          <span class="material-symbols-outlined text-5xl opacity-50">done_all</span>
          <p>当前筛选条件下暂无明显异动。建议保持日常巡检，等待更多业务数据沉淀后继续观察。</p>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { feedbackDashboardAiAdvice, getDashboardAiAdvices } from './api/dashboard.js'
import { trackBehavior } from '@/utils/behavior'

defineOptions({ name: 'DashboardAiAdvice' })

const router = useRouter()
const loading = ref(false)
const advices = ref([])
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
  { category: 'operation' }
]

const filters = computed(() => [
  { value: 'all', label: '全部建议', count: advices.value.length },
  ...categoryStats.value.map((item) => ({ value: item.category, label: categoryTitle(item.category), count: item.count }))
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
  advices.value.forEach((item) => map.set(item.category || 'overview', (map.get(item.category || 'overview') || 0) + 1))
  return moduleCatalog.map((item) => ({ ...item, count: map.get(item.category) || 0 }))
})

const filteredAdvices = computed(() => {
  return advices.value
    .filter((item) => activeCategory.value === 'all' || (item.category || 'overview') === activeCategory.value)
    .filter((item) => activePriority.value === 'all' || (item.priority || priorityText(item.level)) === activePriority.value)
    .slice()
    .sort((a, b) => priorityWeight(a.priority || priorityText(a.level)) - priorityWeight(b.priority || priorityText(b.level)))
})

const warningCount = computed(() => advices.value.filter((item) => item.level === 'warning').length)
const successCount = computed(() => advices.value.filter((item) => item.level === 'success').length)
const activeModuleCount = computed(() => categoryStats.value.filter((item) => item.count > 0).length)

onMounted(() => fetchAdvices(false))

async function fetchAdvices(refresh = false) {
  loading.value = true
  try {
    advices.value = await getDashboardAiAdvices(refresh ? { refresh: true } : {})
    trackAdviceExposure()
  } catch (error) {
    ElMessage.error(error?.msg || 'AI 建议加载失败')
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
        sourceType: item.sourceType,
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
    ElMessage.warning('该建议暂未生成训练样本，请刷新后再反馈')
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
  if (feedbackType === 'irrelevant') return '已记录为不准确，后续会降低类似建议权重'
  if (feedbackType === 'resolved') return '已记录为已处理，后续会用于闭环评估'
  return '已记录为有价值，后续会强化类似建议'
}

const categoryMeta = {
  inventory: { title: '库存水位与周转', subtitle: '库存余量、低库存型号、近期开单消耗', icon: 'inventory_2' },
  order: { title: '订单履约与交付', subtitle: '销售订单、生产订单、履约状态', icon: 'receipt_long' },
  delivery: { title: '物流运输与发货', subtitle: '交付日期、发货状态、物流完整度', icon: 'local_shipping' },
  customer: { title: '客户客情与风控', subtitle: '客户复购周期、活跃客户、核心客户贡献', icon: 'handshake' },
  employee: { title: '员工组织与效率', subtitle: '员工状态、考勤异常、请假审批与上下级关系', icon: 'groups' },
  quality: { title: '质量管控与溯源', subtitle: '近 30 天次品数量与损失金额', icon: 'assignment_late' },
  finance: { title: '财务健康与成本', subtitle: '订单金额、损耗金额、审批事项', icon: 'payments' },
  operation: { title: '生产运营节奏', subtitle: '库存、订单、客户、员工、审批、打印任务联动', icon: 'fact_check' },
  overview: { title: '全局经营总览', subtitle: '总览大盘核心指标', icon: 'monitoring' }
}

function categoryTitle(category) {
  return categoryMeta[category]?.title || '其他经营洞察'
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
  if (score >= 85) return 'text-red-600'
  if (score >= 70) return 'text-amber-600'
  if (score <= 35) return 'text-emerald-600'
  return 'text-primary'
}

function adviceLevelText(level) {
  if (level === 'warning') return '重点关注'
  if (level === 'success') return '运行稳定'
  return '建议动作'
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
  return advices.value.filter((item) => (item.priority || priorityText(item.level)) === priority).length
}

function sourceText(sourceType) {
  if (sourceType === 'llm') return '大模型增强'
  return '本地规则分析'
}
</script>
