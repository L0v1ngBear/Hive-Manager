<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">report_problem</span>
            质量追踪中心
          </div>
          <h1 class="function-page-title">次品管理</h1>
          <p class="function-page-desc">
            统一登记质量问题、运输破损和其他异常次品记录，支持处理闭环和损失跟踪。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            @click="openCreate"
            class="function-action-primary"
          >
            <span class="material-symbols-outlined text-lg align-middle mr-1">add_circle</span>新增次品
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">总记录数</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ pagination.total }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前页待处理</p>
          <h3 class="text-4xl font-black text-amber-600 mt-2">{{ stats.pending }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前页已处理</p>
          <h3 class="text-4xl font-black text-emerald-600 mt-2">{{ stats.processed }}</h3>
        </div>
        <div class="bg-[#1a365d] text-white p-6 rounded-xl shadow-md">
          <p class="text-xs font-bold uppercase tracking-widest opacity-80">当前页损失金额</p>
          <h3 class="text-4xl font-black mt-2">¥{{ money(stats.lossAmount) }}</h3>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden ring-1 ring-outline-variant/20">
        <div class="px-6 py-4 border-b border-surface-variant/50 flex flex-wrap items-center justify-between gap-4">
          <div class="flex flex-wrap items-center gap-3">
            <select v-model="query.status" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部状态</option>
              <option value="pending">待处理</option>
              <option value="processed">已处理</option>
            </select>
            <select v-model="query.type" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部类型</option>
              <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
            <input
              v-model="query.date"
              type="date"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
            />
            <button @click="handleFilter" class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold">查询</button>
            <button @click="resetFilter" class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold">
              重置
            </button>
          </div>
          <span class="text-xs text-on-surface-variant">共 {{ pagination.total }} 条次品记录</span>
        </div>

        <div class="overflow-x-auto relative min-h-[260px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
          </div>
          <table class="w-full text-left border-collapse min-w-[1120px]">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">次品编号</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">关联订单</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">次品类型</th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">数量</th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">损失金额</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">登记人</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">状态</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">登记时间</th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/30">
              <tr
                v-for="item in rows"
                :key="item.defectiveId"
                class="cursor-pointer hover:bg-surface-container-high/40 transition-colors"
                @click="openDetail(item)"
              >
                <td class="px-6 py-4">
                  <p class="text-sm font-bold text-primary">{{ item.defectiveId }}</p>
                  <p class="text-[10px] text-on-surface-variant line-clamp-1">{{ item.description || '未填写问题描述' }}</p>
                </td>
                <td class="px-6 py-4 text-sm">{{ item.orderId || '未关联' }}</td>
                <td class="px-6 py-4 text-sm">{{ typeLabel(item.type) }}</td>
                <td class="px-6 py-4 text-right text-sm font-bold">{{ money(item.quantity) }}</td>
                <td class="px-6 py-4 text-right text-sm font-black text-primary">¥{{ money(item.lossAmount) }}</td>
                <td class="px-6 py-4 text-sm">{{ item.creator || '--' }}</td>
                <td class="px-6 py-4">
                  <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">
                    {{ statusLabel(item.status) }}
                  </span>
                </td>
                <td class="px-6 py-4 text-xs text-on-surface-variant">{{ formatDateTime(item.createTime) }}</td>
                <td class="px-6 py-4 text-right space-x-2">
                  <button @click.stop="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">
                    详情
                  </button>
                  <button @click.stop="openEdit(item)" class="text-secondary hover:bg-surface-container-high px-3 py-1.5 rounded-lg text-xs font-bold">
                    编辑
                  </button>
                  <button
                    v-if="item.status !== 'processed'"
                    @click.stop="openProcess(item)"
                    class="text-emerald-700 hover:bg-emerald-50 px-3 py-1.5 rounded-lg text-xs font-bold"
                  >
                    处理
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td colspan="9" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无次品记录。</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex items-center justify-between text-sm text-on-surface-variant border-t border-surface-variant/50">
          <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">
              上一页
            </button>
            <button
              @click="changePage(query.pageNum + 1)"
              :disabled="query.pageNum >= totalPages"
              class="px-3 py-1.5 rounded bg-white border disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </div>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || formVisible || processVisible" class="fixed inset-0 bg-black/20 backdrop-blur-[2px] z-40" @click="closePanels"></div>
    </transition>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="detailVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">次品详情</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ detailRecord?.defectiveId || '--' }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto" v-if="detailRecord">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">次品数量</span>
            <p class="text-xl font-black text-primary">{{ money(detailRecord.quantity) }}</p>
          </div>
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">损失金额</span>
            <p class="text-xl font-black text-primary">¥{{ money(detailRecord.lossAmount) }}</p>
          </div>
        </div>

        <section class="space-y-3">
          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">闭环信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">负责人员：</span>{{ detailRecord.responsiblePerson || '未填写' }}</p>
              <p><span class="text-on-surface-variant">处理措施：</span>{{ detailRecord.processMeasure || '未填写' }}</p>
              <p><span class="text-on-surface-variant">改进方案：</span>{{ detailRecord.improvementPlan || '未填写' }}</p>
            </div>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">基础信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">关联订单：</span>{{ detailRecord.orderId || '未关联' }}</p>
              <p><span class="text-on-surface-variant">次品类型：</span>{{ typeLabel(detailRecord.type) }}</p>
              <p><span class="text-on-surface-variant">登记人：</span>{{ detailRecord.creator || '--' }}</p>
              <p><span class="text-on-surface-variant">状态：</span>{{ statusLabel(detailRecord.status) }}</p>
              <p><span class="text-on-surface-variant">登记时间：</span>{{ formatDateTime(detailRecord.createTime) }}</p>
            </div>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">问题描述</p>
            <p class="text-sm leading-6">{{ detailRecord.description || '未填写问题描述。' }}</p>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">处理信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">处理方式：</span>{{ detailRecord.processMethod || '未处理' }}</p>
              <p><span class="text-on-surface-variant">处理备注：</span>{{ detailRecord.processRemark || '未填写处理备注。' }}</p>
            </div>
          </div>
        </section>
      </div>
    </aside>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="formVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">{{ editingRecord ? '编辑次品记录' : '新增次品记录' }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">保存后会写入真实次品台账。</p>
        </div>
        <button @click="closeForm" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">关联订单</span>
          <input v-model.trim="form.orderId" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入订单号" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">次品类型</span>
          <select v-model="form.type" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary">
            <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">次品数量</span>
          <input v-model.trim="form.quantity" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入次品数量" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">损失金额</span>
          <input v-model.trim="form.lossAmount" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入损失金额" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">问题描述</span>
          <textarea
            v-model.trim="form.description"
            rows="5"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入次品问题说明"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">负责人员</span>
          <input
            v-model.trim="form.responsiblePerson"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="请输入负责人员"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理措施</span>
          <textarea
            v-model.trim="form.processMeasure"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入处理措施"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">改进方案</span>
          <textarea
            v-model.trim="form.improvementPlan"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入改进方案"
          />
        </label>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="closeForm" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button @click="submitForm" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">保存</button>
      </div>
    </aside>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="processVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">处理次品记录</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ processingRecord?.defectiveId || '--' }}</p>
        </div>
        <button @click="closeProcess" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <div class="rounded-xl bg-surface-container-low p-4 text-sm space-y-2" v-if="processingRecord">
          <p><span class="text-on-surface-variant">关联订单：</span>{{ processingRecord.orderId || '未关联' }}</p>
          <p><span class="text-on-surface-variant">当前状态：</span>{{ statusLabel(processingRecord.status) }}</p>
        </div>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理方式</span>
          <input
            v-model.trim="processForm.method"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="例如报废、返工、让步接收"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理备注</span>
          <textarea
            v-model.trim="processForm.remark"
            rows="5"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入处理说明"
          />
        </label>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="closeProcess" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button @click="submitProcess" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">确认处理</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBadProductPage, processBadProduct, saveBadProduct } from './api/badProduct.js'

const typeOptions = [
  { value: 'quality', label: '质量问题' },
  { value: 'damage', label: '运输破损' },
  { value: 'wrong', label: '生产错误' },
  { value: 'other', label: '其他原因' }
]

const rows = ref([])
const loading = ref(false)
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: '', type: '', date: '' })
const detailVisible = ref(false)
const detailRecord = ref(null)
const formVisible = ref(false)
const editingRecord = ref(null)
const processVisible = ref(false)
const processingRecord = ref(null)
const form = reactive(createEmptyForm())
const processForm = reactive({ method: '', remark: '' })

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const stats = computed(() => {
  const pending = rows.value.filter((item) => item.status === 'pending').length
  const processed = rows.value.filter((item) => item.status === 'processed').length
  const lossAmount = rows.value.reduce((total, item) => total + Number(item.lossAmount || 0), 0)
  return { pending, processed, lossAmount }
})

fetchData()

async function fetchData() {
  loading.value = true
  try {
    const data = await getBadProductPage({
      ...query,
      status: query.status || undefined,
      type: query.type || undefined,
      date: query.date || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  query.pageNum = 1
  fetchData()
}

function resetFilter() {
  query.status = ''
  query.type = ''
  query.date = ''
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

function openCreate() {
  resetForm()
  editingRecord.value = null
  formVisible.value = true
}

function openEdit(record) {
  editingRecord.value = record
  form.defectiveId = record.defectiveId
  form.orderId = record.orderId || ''
  form.type = record.type || 'quality'
  form.quantity = record.quantity == null ? '' : String(record.quantity)
  form.lossAmount = record.lossAmount == null ? '' : String(record.lossAmount)
  form.description = record.description || ''
  form.responsiblePerson = record.responsiblePerson || ''
  form.processMeasure = record.processMeasure || ''
  form.improvementPlan = record.improvementPlan || ''
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  editingRecord.value = null
  resetForm()
}

function openProcess(record) {
  processingRecord.value = record
  processForm.method = ''
  processForm.remark = ''
  processVisible.value = true
}

function closeProcess() {
  processVisible.value = false
  processingRecord.value = null
  processForm.method = ''
  processForm.remark = ''
}

function closePanels() {
  detailVisible.value = false
  closeForm()
  closeProcess()
}

async function submitForm() {
  if (!form.quantity || Number(form.quantity) <= 0) {
    ElMessage.warning('请填写有效的次品数量')
    return
  }
  if (!form.lossAmount || Number(form.lossAmount) <= 0) {
    ElMessage.warning('请填写有效的损失金额')
    return
  }

  await saveBadProduct({
    defectiveId: form.defectiveId || undefined,
    orderId: form.orderId || undefined,
    type: form.type,
    quantity: Number(form.quantity),
    lossAmount: Number(form.lossAmount),
    description: form.description || undefined,
    responsiblePerson: form.responsiblePerson || undefined,
    processMeasure: form.processMeasure || undefined,
    improvementPlan: form.improvementPlan || undefined
  })
  ElMessage.success(editingRecord.value ? '次品记录已更新' : '次品记录已新增')
  closeForm()
  await fetchData()
}

async function submitProcess() {
  if (!processForm.method) {
    ElMessage.warning('请填写处理方式')
    return
  }

  await processBadProduct({
    defectiveId: processingRecord.value?.defectiveId,
    method: processForm.method,
    remark: processForm.remark || undefined
  })
  ElMessage.success('次品记录已处理')
  closeProcess()
  await fetchData()
}

function typeLabel(value) {
  return typeOptions.find((item) => item.value === value)?.label || '其他原因'
}

function statusLabel(value) {
  return value === 'processed' ? '已处理' : '待处理'
}

function statusClass(value) {
  return value === 'processed' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function createEmptyForm() {
  return {
    defectiveId: '',
    orderId: '',
    type: 'quality',
    quantity: '',
    lossAmount: '',
    description: '',
    responsiblePerson: '',
    processMeasure: '',
    improvementPlan: ''
  }
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
