<template>
  <div class="h-full min-h-0 bg-surface text-on-surface overflow-x-hidden font-body">
    <div class="max-w-7xl mx-auto space-y-6">
      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">库存管理</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-2xl">
            管理布匹入库、出库和库存预警，网页端已接入真实库存接口，方便仓库人员快速查条码和看流水。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button @click="openInDrawer" class="px-5 py-2 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 transition-colors text-sm shadow-md active:scale-95">
            <span class="material-symbols-outlined text-lg align-middle mr-1">add_box</span>新增入库
          </button>
          <button @click="openOutDrawer()" class="px-5 py-2 bg-[#1a365d] text-white font-bold rounded-lg hover:bg-[#24456f] transition-colors text-sm shadow-md active:scale-95">
            <span class="material-symbols-outlined text-lg align-middle mr-1">local_shipping</span>扫码出库
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">可用库存</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ meter(summary.totalMeters) }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">米</p>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">在库布匹</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ summary.clothCount }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">卷 / 匹</p>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">低库存型号</p>
          <h3 class="text-4xl font-black text-amber-600 mt-2">{{ summary.warningCount }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">低于 100 米</p>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">今日入库</p>
          <h3 class="text-4xl font-black text-emerald-600 mt-2">{{ meter(summary.todayInMeters) }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">米</p>
        </div>
        <div class="bg-[#1a365d] text-white p-6 rounded-xl shadow-md">
          <p class="text-xs font-bold uppercase tracking-widest opacity-80">今日出库</p>
          <h3 class="text-4xl font-black mt-2">{{ meter(summary.todayOutMeters) }}</h3>
          <p class="text-xs opacity-80 mt-1">米</p>
        </div>
      </section>

      <section class="grid grid-cols-1 xl:grid-cols-[1fr_360px] gap-6">
        <div class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden ring-1 ring-outline-variant/20">
          <div class="px-6 py-4 border-b border-surface-variant/50 flex flex-wrap items-center justify-between gap-4">
            <div class="flex flex-wrap items-center gap-3">
              <input
                v-model.trim="query.keyword"
                @keyup.enter="handleFilter"
                class="w-64 max-w-full px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none focus:ring-primary/40"
                placeholder="搜索条码或型号"
              />
              <select v-model="query.status" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
                <option value="">全部状态</option>
                <option value="0">在库</option>
                <option value="2">部分出库</option>
                <option value="1">已出库</option>
              </select>
              <button @click="handleFilter" class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold">查询</button>
              <button @click="resetFilter" class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold">重置</button>
            </div>
            <span class="text-xs text-on-surface-variant">共 {{ pagination.total }} 条库存记录</span>
          </div>

          <div class="overflow-x-auto relative min-h-[360px]">
            <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
              <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
            </div>
            <table class="w-full text-left border-collapse min-w-[1040px]">
              <thead class="bg-surface-container-low/50">
                <tr>
                  <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">条码</th>
                  <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">型号</th>
                  <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">规格</th>
                  <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">总米数</th>
                  <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">剩余米数</th>
                  <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">状态</th>
                  <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">更新时间</th>
                  <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-surface-variant/30">
                <tr v-for="item in rows" :key="item.id" class="cursor-pointer hover:bg-surface-container-high/40 transition-colors" @click="openDetail(item)">
                  <td class="px-6 py-4">
                    <p class="text-sm font-black text-primary">{{ item.barcode }}</p>
                    <p class="text-[10px] text-on-surface-variant">{{ item.inType || 'manual' }}</p>
                  </td>
                  <td class="px-6 py-4 text-sm font-bold">{{ item.modelCode }}</td>
                  <td class="px-6 py-4 text-right text-sm">{{ meter(item.spec) }}</td>
                  <td class="px-6 py-4 text-right text-sm">{{ meter(item.totalMeters) }}</td>
                  <td class="px-6 py-4 text-right text-sm font-black text-primary">{{ meter(item.remainingMeters) }}</td>
                  <td class="px-6 py-4">
                    <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">
                      {{ item.statusName || statusLabel(item.status) }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-xs text-on-surface-variant">{{ formatDateTime(item.updateTime) }}</td>
                  <td class="px-6 py-4 text-right space-x-2">
                    <button @click.stop="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">详情</button>
                    <button
                      @click.stop="openOutDrawer(item)"
                      :disabled="Number(item.remainingMeters || 0) <= 0"
                      class="text-emerald-700 hover:bg-emerald-50 disabled:opacity-40 px-3 py-1.5 rounded-lg text-xs font-bold"
                    >
                      出库
                    </button>
                  </td>
                </tr>
                <tr v-if="!loading && rows.length === 0">
                  <td colspan="8" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无库存记录。</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="p-4 bg-surface-container/20 flex items-center justify-between text-sm text-on-surface-variant border-t border-surface-variant/50">
            <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">上一页</button>
              <button @click="changePage(query.pageNum + 1)" :disabled="query.pageNum >= totalPages" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">下一页</button>
            </div>
          </div>
        </div>

        <aside class="space-y-6">
          <section class="bg-surface-container-lowest rounded-xl shadow-sm ring-1 ring-outline-variant/20 p-6">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-black text-primary">7 天出入库趋势</h2>
              <span class="material-symbols-outlined text-primary">monitoring</span>
            </div>
            <div class="space-y-3">
              <div v-for="item in trendRows" :key="item.statDate">
                <div class="flex justify-between text-xs text-on-surface-variant mb-1">
                  <span>{{ item.statDate }}</span>
                  <span>入 {{ meter(item.inMeters) }} / 出 {{ meter(item.outMeters) }}</span>
                </div>
                <div class="h-2 bg-surface-container-high rounded-full overflow-hidden flex">
                  <div class="bg-emerald-500" :style="{ width: trendWidth(item.inMeters) }"></div>
                  <div class="bg-primary" :style="{ width: trendWidth(item.outMeters) }"></div>
                </div>
              </div>
              <p v-if="trendRows.length === 0" class="text-sm text-on-surface-variant">暂无趋势数据。</p>
            </div>
          </section>

          <section class="bg-surface-container-lowest rounded-xl shadow-sm ring-1 ring-outline-variant/20 p-6">
            <h2 class="text-lg font-black text-primary mb-4">低库存预警</h2>
            <div class="space-y-3">
              <div v-for="item in warningRows" :key="item.modelCode" class="p-3 rounded-xl bg-amber-50 border border-amber-100">
                <p class="text-sm font-black text-amber-800">{{ item.modelCode }}</p>
                <p class="text-xs text-amber-700 mt-1">剩余 {{ meter(item.totalMeters) }} 米，建议尽快核对补货。</p>
              </div>
              <p v-if="warningRows.length === 0" class="text-sm text-on-surface-variant">当前没有低库存预警。</p>
            </div>
          </section>

          <section class="bg-surface-container-lowest rounded-xl shadow-sm ring-1 ring-outline-variant/20 p-6">
            <h2 class="text-lg font-black text-primary mb-4">最近流水</h2>
            <div class="space-y-3 max-h-80 overflow-y-auto pr-1">
              <div v-for="item in recordRows" :key="item.id" class="flex gap-3">
                <div :class="item.operateType === 1 ? 'bg-primary' : 'bg-emerald-500'" class="w-2 rounded-full"></div>
                <div class="flex-1">
                  <p class="text-sm font-bold">{{ item.operateTypeName }} {{ meter(item.operateMeters) }} 米</p>
                  <p class="text-xs text-on-surface-variant">{{ item.modelCode }} / {{ item.barcode || '--' }}</p>
                  <p class="text-[10px] text-on-surface-variant">{{ item.operatorName || '系统' }} · {{ formatDateTime(item.createTime) }}</p>
                </div>
              </div>
              <p v-if="recordRows.length === 0" class="text-sm text-on-surface-variant">暂无库存流水。</p>
            </div>
          </section>
        </aside>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || inVisible || outVisible" class="fixed inset-0 bg-black/20 backdrop-blur-[2px] z-40" @click="closePanels"></div>
    </transition>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300" :class="detailVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">库存详情</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ detailRecord?.barcode || '--' }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1 hover:bg-surface-container-high rounded-full"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto" v-if="detailRecord">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">剩余米数</span>
            <p class="text-xl font-black text-primary">{{ meter(detailRecord.remainingMeters) }}</p>
          </div>
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">库存状态</span>
            <p class="text-xl font-black text-primary">{{ detailRecord.statusName || statusLabel(detailRecord.status) }}</p>
          </div>
        </div>
        <div class="rounded-xl bg-surface-container-low p-4 space-y-2 text-sm">
          <p><span class="text-on-surface-variant">型号：</span>{{ detailRecord.modelCode }}</p>
          <p><span class="text-on-surface-variant">规格：</span>{{ meter(detailRecord.spec) }}</p>
          <p><span class="text-on-surface-variant">总米数：</span>{{ meter(detailRecord.totalMeters) }}</p>
          <p><span class="text-on-surface-variant">入库时间：</span>{{ formatDateTime(detailRecord.inTime) }}</p>
          <p><span class="text-on-surface-variant">最近出库：</span>{{ formatDateTime(detailRecord.outTime) }}</p>
        </div>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300" :class="inVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">新增入库</h3>
          <p class="text-xs text-on-surface-variant mt-1">条码不填时系统会自动生成。</p>
        </div>
        <button @click="inVisible = false" class="p-1 hover:bg-surface-container-high rounded-full"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">条码</span>
          <input v-model.trim="inForm.barcode" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="可不填，自动生成" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">型号</span>
          <input v-model.trim="inForm.modelCode" @input="loadModelOptions" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入布匹型号" />
          <div v-if="modelOptions.length" class="mt-2 flex flex-wrap gap-2">
            <button v-for="item in modelOptions" :key="`${item.modelCode}-${item.spec}`" @click="pickModel(item)" class="px-3 py-1 rounded-full bg-surface-container-high text-xs font-bold text-on-surface-variant">
              {{ item.modelCode }} / {{ meter(item.spec) }}
            </button>
          </div>
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">规格</span>
          <input v-model.trim="inForm.spec" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入规格" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">入库米数</span>
          <input v-model.trim="inForm.meters" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入入库米数" />
        </label>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="inVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button @click="submitIn" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">确认入库</button>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300" :class="outVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">库存出库</h3>
          <p class="text-xs text-on-surface-variant mt-1">按条码扣减库存米数。</p>
        </div>
        <button @click="outVisible = false" class="p-1 hover:bg-surface-container-high rounded-full"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">条码</span>
          <input v-model.trim="outForm.barcode" @change="lookupBarcode" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入或扫码条码" />
        </label>
        <div v-if="outPreview" class="rounded-xl bg-surface-container-low p-4 text-sm space-y-2">
          <p><span class="text-on-surface-variant">型号：</span>{{ outPreview.modelCode }}</p>
          <p><span class="text-on-surface-variant">可出库：</span>{{ meter(outPreview.remainingMeters) }} 米</p>
        </div>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">出库米数</span>
          <input v-model.trim="outForm.meters" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入出库米数" />
        </label>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="outVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button @click="submitOut" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">确认出库</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInventoryPage,
  getInventorySummary,
  getInventoryTrend,
  getInventoryWarnings,
  getRecentInventoryRecords,
  inCloth,
  outCloth,
  searchInventoryBarcode,
  searchInventoryModels
} from './api/inventory.js'

const rows = ref([])
const warningRows = ref([])
const recordRows = ref([])
const trendRows = ref([])
const modelOptions = ref([])
const loading = ref(false)
const summary = reactive({ totalMeters: 0, clothCount: 0, warningCount: 0, todayInMeters: 0, todayOutMeters: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const detailVisible = ref(false)
const inVisible = ref(false)
const outVisible = ref(false)
const detailRecord = ref(null)
const outPreview = ref(null)
const inForm = reactive({ barcode: '', modelCode: '', spec: '', meters: '' })
const outForm = reactive({ barcode: '', meters: '' })

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const maxTrendValue = computed(() => Math.max(1, ...trendRows.value.flatMap((item) => [Number(item.inMeters || 0), Number(item.outMeters || 0)])))

refreshAll()

async function refreshAll() {
  await Promise.all([fetchData(), fetchSummary(), fetchWarnings(), fetchRecords(), fetchTrend()])
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getInventoryPage({
      ...query,
      status: query.status === '' ? undefined : Number(query.status),
      keyword: query.keyword || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  Object.assign(summary, await getInventorySummary())
}

async function fetchWarnings() {
  warningRows.value = await getInventoryWarnings()
}

async function fetchRecords() {
  recordRows.value = await getRecentInventoryRecords()
}

async function fetchTrend() {
  trendRows.value = await getInventoryTrend()
}

function handleFilter() {
  query.pageNum = 1
  fetchData()
}

function resetFilter() {
  query.keyword = ''
  query.status = ''
  query.pageNum = 1
  fetchData()
}

function changePage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) {
    return
  }
  query.pageNum = pageNum
  fetchData()
}

function openDetail(record) {
  detailRecord.value = record
  detailVisible.value = true
}

function openInDrawer() {
  Object.assign(inForm, { barcode: '', modelCode: '', spec: '', meters: '' })
  modelOptions.value = []
  inVisible.value = true
}

function openOutDrawer(record) {
  Object.assign(outForm, { barcode: record?.barcode || '', meters: record?.remainingMeters ? String(record.remainingMeters) : '' })
  outPreview.value = record || null
  outVisible.value = true
}

function closePanels() {
  detailVisible.value = false
  inVisible.value = false
  outVisible.value = false
}

async function loadModelOptions() {
  if (!inForm.modelCode) {
    modelOptions.value = []
    return
  }
  modelOptions.value = await searchInventoryModels({ keyword: inForm.modelCode })
}

function pickModel(item) {
  inForm.modelCode = item.modelCode
  inForm.spec = item.spec == null ? '' : String(item.spec)
  modelOptions.value = []
}

async function lookupBarcode() {
  if (!outForm.barcode) {
    outPreview.value = null
    return
  }
  outPreview.value = await searchInventoryBarcode({ barCode: outForm.barcode })
}

async function submitIn() {
  if (!inForm.modelCode || Number(inForm.spec) <= 0 || Number(inForm.meters) <= 0) {
    ElMessage.warning('请填写型号、规格和有效入库米数')
    return
  }
  await inCloth({
    barcode: inForm.barcode || undefined,
    modelCode: inForm.modelCode,
    spec: Number(inForm.spec),
    meters: Number(inForm.meters),
    inType: 'manual'
  })
  ElMessage.success('入库成功')
  inVisible.value = false
  await refreshAll()
}

async function submitOut() {
  if (!outForm.barcode || Number(outForm.meters) <= 0) {
    ElMessage.warning('请填写条码和有效出库米数')
    return
  }
  await outCloth({ barcode: outForm.barcode, meters: Number(outForm.meters) })
  ElMessage.success('出库成功')
  outVisible.value = false
  await refreshAll()
}

function trendWidth(value) {
  return `${Math.min(100, Math.round((Number(value || 0) / maxTrendValue.value) * 100))}%`
}

function statusLabel(value) {
  if (Number(value) === 1) return '已出库'
  if (Number(value) === 2) return '部分出库'
  return '在库'
}

function statusClass(value) {
  if (Number(value) === 1) return 'bg-slate-100 text-slate-600'
  if (Number(value) === 2) return 'bg-amber-100 text-amber-700'
  return 'bg-emerald-100 text-emerald-700'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}

function meter(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
