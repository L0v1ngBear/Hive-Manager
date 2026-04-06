<template>
  <Teleport defer to="body">
    <transition name="fade">
      <div v-if="isVisible" @click="close" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60]"></div>
    </transition>

    <transition name="slide">
      <div v-if="isVisible" class="fixed right-0 top-0 h-full w-full sm:w-[600px] bg-white/95 backdrop-blur-2xl z-[70] shadow-[-20px_0_40px_rgba(0,32,69,0.1)] border-l-4 border-primary flex flex-col">

        <div class="p-6 flex items-center justify-between border-b border-outline-variant/20 bg-white/80">
          <div>
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">account_tree</span>
              <h2 class="text-xl font-bold text-primary tracking-tight">SKU 定价矩阵</h2>
            </div>
            <p class="text-xs text-on-surface-variant mt-1">配置全局基准、等级策略与客户专属特批价</p>
          </div>
          <button @click="close" class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant">
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-8">

          <section class="bg-surface-container-low p-4 rounded-xl border border-outline-variant/10 flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-white border border-outline-variant/20 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-primary/40 text-3xl">texture</span>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-sm font-black text-primary truncate">{{ skuData?.model || 'NV-2024-OXFORD (优质纯棉)' }}</h3>
              <p class="text-xs text-on-surface-variant mt-0.5">批号: {{ skuData?.lot || '#88219' }} | 现行基准价: ¥{{ skuData?.price || '42.50' }}</p>
            </div>
          </section>

          <section class="space-y-4">
            <h3 class="text-sm font-bold text-primary flex items-center gap-2">
              <span class="w-1 h-4 bg-primary rounded-full"></span>第一层级: 全局基准价
            </h3>
            <div class="p-4 bg-white border border-outline-variant/20 rounded-xl shadow-sm">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">新基准底价 (Base) <span class="text-error">*</span></label>
                  <div class="relative">
                    <span class="absolute left-3 top-1/2 -translate-y-1/2 text-primary font-bold">¥</span>
                    <input v-model="formData.basePrice" type="number" step="0.01" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 pl-7 pr-3 text-sm font-black text-primary transition-colors placeholder-on-surface-variant/40" placeholder="0.00" />
                  </div>
                </div>
                <div>
                  <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">全局生效日期</label>
                  <input v-model="formData.effectiveDate" type="date" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm font-bold text-primary transition-colors cursor-pointer" />
                </div>
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex justify-between items-end">
              <h3 class="text-sm font-bold text-primary flex items-center gap-2">
                <span class="w-1 h-4 bg-primary rounded-full"></span>第二层级: 客户等级策略
              </h3>
              <span class="text-[10px] font-bold text-on-surface-variant bg-surface-container-high px-2 py-0.5 rounded">覆盖 80% 标准客户</span>
            </div>

            <div class="bg-white border border-outline-variant/20 rounded-xl overflow-hidden shadow-sm">
              <table class="w-full text-xs text-left">
                <thead class="bg-surface-container-low/50">
                <tr class="text-on-surface-variant">
                  <th class="px-4 py-3 font-bold uppercase tracking-wider w-1/3">客户等级</th>
                  <th class="px-4 py-3 font-bold uppercase tracking-wider">一口价 (¥)</th>
                  <th class="px-4 py-3 font-bold uppercase tracking-wider">或 自动折扣</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-outline-variant/10">
                <tr v-for="grade in formData.gradePrices" :key="grade.code" class="hover:bg-surface-container-high/30 transition-colors">
                  <td class="px-4 py-3 font-bold text-primary flex flex-col gap-0.5">
                    <span>{{ grade.name }}</span>
                    <span class="text-[10px] text-on-surface-variant/60 font-normal">梯队代码: {{ grade.code }}</span>
                  </td>
                  <td class="px-4 py-2">
                    <div class="relative">
                      <span class="absolute left-2 top-1/2 -translate-y-1/2 text-on-surface-variant/50 font-bold text-[10px]">¥</span>
                      <input v-model="grade.fixedPrice" type="number" :placeholder="getCalculatedPrice(grade)" class="w-full bg-surface-container-lowest border border-outline-variant/20 rounded py-1.5 pl-5 pr-1.5 text-primary font-bold focus:ring-1 focus:ring-primary focus:border-primary placeholder-primary/30" />
                    </div>
                  </td>
                  <td class="px-4 py-2">
                    <div class="flex items-center gap-1">
                      <input v-model="grade.discountRate" type="number" placeholder="100" class="w-16 bg-surface-container-lowest border border-outline-variant/20 rounded py-1.5 px-2 text-primary font-bold focus:ring-1 focus:ring-primary focus:border-primary" />
                      <span class="text-on-surface-variant font-medium">%</span>
                    </div>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
            <p class="text-[10px] text-on-surface-variant mt-1 ml-1 flex items-center gap-1">
              <span class="material-symbols-outlined text-[12px]">tips_and_updates</span>
              如果不填一口价，系统将按照“新基准底价 × 折扣”自动计算各等级售价。
            </p>
          </section>

          <section class="space-y-4">
            <div class="flex justify-between items-end">
              <div>
                <h3 class="text-sm font-bold text-tertiary flex items-center gap-2">
                  <span class="w-1 h-4 bg-tertiary rounded-full"></span>第三层级: 指定客户特批价
                </h3>
                <p class="text-[10px] text-on-surface-variant mt-1 ml-3">此列表享有系统最高优先级，无视全局涨跌。</p>
              </div>
              <button @click="addOverride" class="text-xs font-bold text-tertiary hover:opacity-80 transition-opacity flex items-center gap-1 bg-tertiary-fixed px-2 py-1 rounded-md active:scale-95">
                <span class="material-symbols-outlined text-[16px]">add</span> 添加特批
              </button>
            </div>

            <div class="space-y-3">
              <div v-for="(override, index) in formData.overrides" :key="index" class="flex items-center gap-3 bg-tertiary-fixed/10 p-3 rounded-lg border border-tertiary/20">
                <div class="flex-1">
                  <select v-model="override.customerId" class="w-full bg-white border border-outline-variant/20 focus:ring-1 focus:ring-tertiary rounded text-xs font-bold text-primary py-2 px-2 cursor-pointer shadow-sm">
                    <option value="" disabled>请选择特批大客户...</option>
                    <option v-for="customer in customerDict" :key="customer.id" :value="customer.id">
                      {{ customer.name }}
                    </option>
                  </select>
                </div>
                <div class="w-28 relative">
                  <span class="absolute left-2 top-1/2 -translate-y-1/2 text-tertiary font-bold text-xs">¥</span>
                  <input v-model="override.price" type="number" step="0.01" class="w-full bg-white border border-outline-variant/20 focus:ring-1 focus:ring-tertiary rounded py-2 pl-6 pr-2 text-xs font-black text-tertiary shadow-sm" placeholder="0.00" />
                </div>
                <button @click="removeOverride(index)" class="p-1.5 text-on-surface-variant hover:text-error hover:bg-error-container rounded transition-colors" title="移除特批">
                  <span class="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>

              <div v-if="formData.overrides.length === 0" class="text-center py-6 border border-dashed border-outline-variant/30 rounded-xl text-xs text-on-surface-variant/60 font-medium bg-surface-container-lowest/50">
                暂无特批客户，将按基准价与客户等级策略执行。
              </div>
            </div>
          </section>

        </div>

        <div class="p-6 border-t border-outline-variant/20 bg-surface-container-lowest flex items-center justify-between shrink-0">
          <div class="text-xs text-on-surface-variant flex items-center gap-1">
            <span class="material-symbols-outlined text-[16px] text-orange-500">info</span>
            保存后将立即更新系统报价库
          </div>
          <div class="flex gap-3">
            <button @click="close" class="px-5 py-2.5 text-sm font-bold text-secondary hover:bg-surface-container-high rounded-lg transition-colors active:scale-95">
              取消
            </button>
            <button @click="submit" :disabled="!formData.basePrice" class="px-6 py-2.5 text-sm font-bold bg-primary text-on-primary rounded-lg shadow-md hover:shadow-lg hover:bg-primary/90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 active:scale-95">
              <span class="material-symbols-outlined text-[18px]">save</span>
              发布价格矩阵
            </button>
          </div>
        </div>

      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { reactive } from 'vue'

// 1. 定义组件属性
const props = defineProps({
  isVisible: {
    type: Boolean,
    default: false
  },
  skuData: {
    type: Object,
    default: () => null
  }
})

// 2. 事件定义
const emit = defineEmits(['close', 'success'])

const close = () => {
  emit('close')
  resetForm()
}

// 模拟系统中的大客户字典库
const customerDict = [
  { id: 'CUST-01', name: '优衣库 (Uniqlo) 采购部' },
  { id: 'CUST-02', name: 'ZARA 华东大区' },
  { id: 'CUST-03', name: 'H&M 供应链' },
  { id: 'CUST-04', name: '安踏体育 (ANTA)' }
]

// 3. 核心价格矩阵数据模型
const formData = reactive({
  basePrice: null,
  effectiveDate: new Date().toISOString().split('T')[0],

  // 优化后的 B2B 制造行业客户分级
  gradePrices: [
    { code: 'T1', name: '战略级客企 (Strategic)', fixedPrice: null, discountRate: 85 },
    { code: 'T2', name: '大宗采购商 (Bulk Buyer)', fixedPrice: null, discountRate: 90 },
    { code: 'T3', name: '标准合作方 (Standard)', fixedPrice: null, discountRate: 95 }
  ],

  overrides: []
})

// ==========================================
// 方法与逻辑
// ==========================================

// 辅助计算函数
const getCalculatedPrice = (grade) => {
  if (!formData.basePrice || !grade.discountRate) return '0.00'
  const calcPrice = (formData.basePrice * (grade.discountRate / 100)).toFixed(2)
  return `(自动) ${calcPrice}`
}

const addOverride = () => formData.overrides.push({ customerId: '', price: null })
const removeOverride = (index) => formData.overrides.splice(index, 1)

const submit = () => {
  if (!formData.basePrice) return

  const payload = JSON.parse(JSON.stringify(formData))

  if (props.skuData && props.skuData.id) {
    payload.skuId = props.skuData.id
  }

  // 清理数据
  payload.overrides = payload.overrides.filter(item => item.customerId && item.price)

  console.log('即将发布的价格矩阵配置:', payload)
  emit('success', payload)
  close()
}

const resetForm = () => {
  formData.basePrice = null
  formData.effectiveDate = new Date().toISOString().split('T')[0]
  formData.gradePrices.forEach(g => g.fixedPrice = null)
  formData.overrides = []
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
}
</style>
