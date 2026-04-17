<template>
  <div class="h-full flex flex-col bg-surface font-body p-8">

    <div class="mb-8 flex flex-col gap-6 shrink-0">
      <div class="flex justify-between items-end">
        <div>
          <h2 class="text-2xl font-bold text-on-surface font-headline tracking-tight mb-1">订单管理</h2>
          <p class="text-sm text-on-surface-variant font-label">管理并跟踪所有销售和生产流程。</p>
        </div>
        <button class="px-4 py-2 bg-surface-container-lowest text-on-surface border border-outline-variant/50 rounded text-sm font-medium hover:bg-surface-container transition-colors shadow-sm flex items-center gap-2">
          <span class="material-symbols-outlined text-[18px]">download</span>
          导出
        </button>
      </div>

      <div class="flex border-b border-surface-variant">
        <button v-for="tab in tabs" :key="tab.id" @click="currentTab = tab.id"
                :class="['px-6 py-3 text-sm transition-colors relative',
                         currentTab === tab.id ? 'font-semibold text-primary' : 'font-medium text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low']">
          {{ tab.name }}
          <div v-if="currentTab === tab.id" class="absolute bottom-0 left-0 right-0 h-[3px] bg-primary rounded-t-sm"></div>
        </button>
      </div>
    </div>

    <div class="bg-surface-container-lowest p-4 rounded mb-6 flex flex-wrap items-center gap-4 border border-outline-variant/20 shadow-sm shrink-0">
      <div class="flex-1 min-w-[200px]">
        <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">客户</label>
        <div class="relative">
          <select v-model="filters.customer" class="w-full bg-transparent border-b border-surface-variant text-sm py-1.5 pl-3 pr-8 focus:outline-none focus:border-primary focus:bg-blue-50/50 transition-colors appearance-none outline-none">
            <option value="all">全部客户</option>
            <option value="stitch">Stitch & Co.</option>
            <option value="global">Global Textiles Ltd.</option>
          </select>
          <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-[18px] text-on-surface-variant pointer-events-none">arrow_drop_down</span>
        </div>
      </div>

      <div class="flex-1 min-w-[200px]">
        <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">日期范围</label>
        <div class="relative">
          <span class="material-symbols-outlined absolute left-2 top-1/2 -translate-y-1/2 text-[16px] text-on-surface-variant">calendar_today</span>
          <input v-model="filters.dateRange" type="text" class="w-full bg-transparent border-b border-surface-variant text-sm py-1.5 pl-8 pr-3 focus:outline-none focus:border-primary focus:bg-blue-50/50 transition-colors outline-none" placeholder="选择日期">
        </div>
      </div>

      <div class="flex-1 min-w-[200px]">
        <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">状态</label>
        <div class="flex flex-wrap gap-2">
          <span v-for="status in statusOptions" :key="status.value" @click="filters.status = status.value"
                :class="['px-3 py-1.5 text-xs font-medium rounded cursor-pointer border transition-colors',
                         filters.status === status.value ? 'bg-primary-fixed-dim text-primary border-primary-fixed' : 'bg-surface text-on-surface-variant border-surface-variant hover:bg-surface-container-low']">
            {{ status.label }}
          </span>
        </div>
      </div>

      <div class="ml-auto pl-4 flex items-end h-full">
        <button class="p-2 text-on-surface-variant hover:text-primary hover:bg-blue-50 rounded transition-colors flex items-center justify-center">
          <span class="material-symbols-outlined text-[20px]">filter_list</span>
        </button>
      </div>
    </div>

    <div class="bg-surface-container-lowest rounded shadow-[0px_8px_24px_rgba(0,32,69,0.03)] border border-outline-variant/10 flex flex-col flex-1 min-h-0">

      <div class="flex-1 overflow-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead class="bg-surface-container-low text-on-surface-variant font-semibold border-b border-surface-variant sticky top-0 z-10">
          <tr>
            <th class="py-3 px-4 w-12"><input type="checkbox" class="rounded border-outline-variant text-primary focus:ring-primary cursor-pointer" @change="toggleAll" :checked="isAllSelected"></th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase whitespace-nowrap">订单编号</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase whitespace-nowrap">客户名称</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase whitespace-nowrap">面料型号</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase text-right whitespace-nowrap">数量 (米)</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase whitespace-nowrap">下单日期</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase whitespace-nowrap">状态</th>
            <th class="py-3 px-4 font-headline text-xs tracking-wider uppercase text-right whitespace-nowrap">操作</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-surface-variant/50">
          <tr v-for="order in tableData" :key="order.id" class="hover:bg-slate-50 transition-colors group cursor-pointer relative">

            <td class="py-3 px-4 w-12 relative">
              <div v-if="order.status === 'error'" class="absolute left-0 top-0 bottom-0 w-[3px] bg-red-500 rounded-r-sm"></div>
              <input type="checkbox" v-model="selectedOrders" :value="order.id" class="rounded border-outline-variant text-primary focus:ring-primary cursor-pointer">
            </td>

            <td class="py-3 px-4 font-medium text-on-surface whitespace-nowrap">{{ order.id }}</td>
            <td class="py-3 px-4 text-on-surface-variant whitespace-nowrap">{{ order.customer }}</td>
            <td class="py-3 px-4 whitespace-nowrap">
                <span class="inline-flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full" :class="order.fabricColor"></span>
                  <span class="font-mono text-xs text-on-surface">{{ order.fabricModel }}</span>
                </span>
            </td>

            <td class="py-3 px-4 text-right font-medium whitespace-nowrap">{{ formatNumber(order.quantity) }}</td>

            <td class="py-3 px-4 text-on-surface-variant text-xs whitespace-nowrap">{{ order.date }}</td>

            <td class="py-3 px-4 whitespace-nowrap">
                <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border', getStatusStyle(order.status)]">
                  {{ getStatusLabel(order.status) }}
                </span>
            </td>

            <td class="py-3 px-4 text-right whitespace-nowrap">
              <div class="flex items-center justify-end gap-1 text-slate-300 group-hover:text-primary transition-colors">
                <button class="p-1 hover:bg-blue-100 hover:text-blue-800 rounded transition-colors" title="编辑">
                  <span class="material-symbols-outlined text-[18px]">edit</span>
                </button>
                <button class="p-1 hover:bg-blue-100 hover:text-blue-800 rounded transition-colors" title="详情">
                  <span class="material-symbols-outlined text-[18px]">visibility</span>
                </button>
              </div>
            </td>
          </tr>

          <tr v-if="tableData.length === 0">
            <td colspan="8" class="py-12 text-center text-on-surface-variant text-sm">暂无数据</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="px-4 py-3 border-t border-surface-variant bg-surface-container-lowest flex items-center justify-between shrink-0">
        <span class="text-xs text-on-surface-variant font-label">显示 1 至 {{ tableData.length }} 项，共 {{ totalItems }} 项</span>
        <div class="flex items-center gap-1">
          <button class="p-1 rounded text-on-surface-variant hover:bg-surface-container disabled:opacity-50 transition-colors" disabled>
            <span class="material-symbols-outlined text-[18px]">chevron_left</span>
          </button>

          <button class="w-7 h-7 rounded flex items-center justify-center text-xs font-semibold bg-primary text-on-primary shadow-sm">1</button>
          <button class="w-7 h-7 rounded flex items-center justify-center text-xs font-medium text-on-surface-variant hover:bg-surface-container transition-colors">2</button>
          <button class="w-7 h-7 rounded flex items-center justify-center text-xs font-medium text-on-surface-variant hover:bg-surface-container transition-colors">3</button>

          <span class="text-on-surface-variant text-xs px-2">...</span>
          <button class="p-1 rounded text-on-surface-variant hover:bg-surface-container transition-colors">
            <span class="material-symbols-outlined text-[18px]">chevron_right</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

// --- Tabs 状态 ---
const tabs = [
  { id: 'sales', name: '销售订单' },
  { id: 'production', name: '生产订单' }
]
const currentTab = ref('sales')

// --- 筛选栏状态 ---
const filters = reactive({
  customer: 'all',
  dateRange: '2024-10-01 - 2024-10-31',
  status: 'all'
})

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待处理', value: 'pending' },
  { label: '已发货', value: 'shipped' }
]

// --- 表格数据状态 ---
const selectedOrders = ref([])
const totalItems = ref(124)

const tableData = ref([
  { id: 'SO-2024-001', customer: 'Global Textiles Ltd.', fabricModel: 'M-COTTON-01', fabricColor: 'bg-slate-400', quantity: 12500, date: '2024-10-24', status: 'pending' },
  { id: 'PO20260318-02', customer: 'Stitch & Co.', fabricModel: 'M-FLANNEL-02', fabricColor: 'bg-blue-600', quantity: 8200, date: '2024-10-22', status: 'shipped' },
  { id: 'SO-2024-088', customer: 'Nordic Weavers', fabricModel: 'M-LINEN-14', fabricColor: 'bg-amber-700', quantity: 4500, date: '2024-10-20', status: 'error' },
  { id: 'SO-2024-042', customer: 'Urban Outfitters Supply', fabricModel: 'M-DENIM-09', fabricColor: 'bg-slate-800', quantity: 25000, date: '2024-10-15', status: 'completed' }
])

// --- 计算属性 ---
const isAllSelected = computed(() => {
  return tableData.value.length > 0 && selectedOrders.value.length === tableData.value.length
})

// --- 方法 ---
const toggleAll = (e) => {
  if (e.target.checked) {
    selectedOrders.value = tableData.value.map(order => order.id)
  } else {
    selectedOrders.value = []
  }
}

const formatNumber = (num) => {
  if (!num) return '0'
  return new Intl.NumberFormat('en-US').format(num)
}

const getStatusLabel = (status) => {
  const map = {
    pending: '待处理',
    shipped: '已发货',
    error: '异常记录',
    completed: '已完成'
  }
  return map[status] || status
}

// 统一并优化后的状态样式 (低保和背景色 + 高对比度文字 + 微边框)
const getStatusStyle = (status) => {
  const styles = {
    pending: 'bg-orange-50 text-orange-700 border-orange-200/60',
    shipped: 'bg-blue-50 text-blue-700 border-blue-200/60',
    error: 'bg-red-50 text-red-700 border-red-200/60',
    completed: 'bg-slate-100 text-slate-700 border-slate-200/60'
  }
  return styles[status] || 'bg-slate-100 text-slate-700 border-slate-200'
}
</script>

<style scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 20;
}
</style>