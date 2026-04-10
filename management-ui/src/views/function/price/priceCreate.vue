<template>
  <Teleport defer to="body">
    <transition name="fade">
      <div v-if="isVisible" @click="close" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60]"></div>
    </transition>

    <transition name="slide">
      <aside v-if="isVisible" class="fixed right-0 top-0 h-full w-full sm:w-[640px] bg-white/95 backdrop-blur-2xl z-[70] shadow-[-20px_0_40px_rgba(0,32,69,0.1)] border-l-4 border-primary flex flex-col">
        <div class="p-6 flex items-center justify-between border-b border-outline-variant/20 bg-white/80">
          <div>
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">account_tree</span>
              <h2 class="text-xl font-bold text-primary tracking-tight">{{ form.id ? '调整价格矩阵' : '新增价格矩阵' }}</h2>
            </div>
            <p class="text-xs text-on-surface-variant mt-1">维护基准价、客户等级价和指定客户特价。</p>
          </div>
          <button @click="close" class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant">
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-8">
          <section class="space-y-4">
            <h3 class="text-sm font-bold text-primary flex items-center gap-2"><span class="w-1 h-4 bg-primary rounded-full"></span>基础信息</h3>
            <div class="grid grid-cols-2 gap-4 p-4 bg-white border border-outline-variant/20 rounded-xl shadow-sm">
              <div class="col-span-2">
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">面料型号 <span class="text-error">*</span></label>
                <input v-model.trim="form.modelCode" list="model-options" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm font-bold text-primary" placeholder="例如 NV-2024-OXFORD" />
                <datalist id="model-options">
                  <option v-for="item in modelOptions" :key="item.modelCode" :value="item.modelCode">{{ item.spec }}</option>
                </datalist>
              </div>
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">批号</label>
                <input v-model.trim="form.batchNo" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm" placeholder="例如 #88219" />
              </div>
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">分类</label>
                <input v-model.trim="form.category" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm" placeholder="例如 牛津布" />
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">规格说明</label>
                <input v-model.trim="form.spec" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm" placeholder="例如 100% 优质纯棉" />
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <h3 class="text-sm font-bold text-primary flex items-center gap-2"><span class="w-1 h-4 bg-primary rounded-full"></span>全局基准价</h3>
            <div class="grid grid-cols-2 gap-4 p-4 bg-white border border-outline-variant/20 rounded-xl shadow-sm">
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">基准价 <span class="text-error">*</span></label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-primary font-bold">¥</span>
                  <input v-model="form.basePrice" type="number" step="0.01" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 pl-7 pr-3 text-sm font-black text-primary" placeholder="0.00" />
                </div>
              </div>
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">生效日期 <span class="text-error">*</span></label>
                <input v-model="form.effectiveDate" type="date" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm font-bold text-primary" />
              </div>
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">币种</label>
                <select v-model="form.currency" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm">
                  <option value="CNY">CNY 人民币</option>
                  <option value="USD">USD 美元</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5">图片地址</label>
                <input v-model.trim="form.imageUrl" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm" placeholder="可选" />
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex justify-between items-end">
              <h3 class="text-sm font-bold text-primary flex items-center gap-2"><span class="w-1 h-4 bg-primary rounded-full"></span>客户等级价</h3>
              <span class="text-[10px] font-bold text-on-surface-variant bg-surface-container-high px-2 py-0.5 rounded">不填一口价时按折扣自动计算</span>
            </div>
            <div class="bg-white border border-outline-variant/20 rounded-xl overflow-hidden shadow-sm">
              <table class="w-full text-xs text-left">
                <thead class="bg-surface-container-low/50">
                  <tr class="text-on-surface-variant">
                    <th class="px-4 py-3 font-bold">客户等级</th>
                    <th class="px-4 py-3 font-bold">一口价</th>
                    <th class="px-4 py-3 font-bold">折扣率</th>
                    <th class="px-4 py-3 font-bold">预估售价</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-outline-variant/10">
                  <tr v-for="tier in form.tierPrices" :key="tier.tierCode">
                    <td class="px-4 py-3 font-bold text-primary">{{ tier.tierName }}</td>
                    <td class="px-4 py-2"><input v-model="tier.fixedPrice" type="number" step="0.01" class="w-full bg-surface-container-lowest border border-outline-variant/20 rounded py-1.5 px-2" placeholder="自动" /></td>
                    <td class="px-4 py-2"><input v-model="tier.discountRate" type="number" step="0.01" class="w-20 bg-surface-container-lowest border border-outline-variant/20 rounded py-1.5 px-2" /> %</td>
                    <td class="px-4 py-2 font-black text-primary">¥{{ calculatedTierPrice(tier) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex justify-between items-end">
              <h3 class="text-sm font-bold text-tertiary flex items-center gap-2"><span class="w-1 h-4 bg-tertiary rounded-full"></span>指定客户特价</h3>
              <button @click="addOverride" class="text-xs font-bold text-tertiary bg-tertiary-fixed px-2 py-1 rounded-md active:scale-95"><span class="material-symbols-outlined text-[16px] align-middle">add</span>添加特价</button>
            </div>
            <div class="space-y-3">
              <div v-for="(override, index) in form.overrides" :key="index" class="flex items-center gap-3 bg-tertiary-fixed/10 p-3 rounded-lg border border-tertiary/20">
                <select v-model="override.customerId" @change="syncCustomerName(override)" class="flex-1 bg-white border border-outline-variant/20 rounded text-xs font-bold text-primary py-2 px-2">
                  <option value="">请选择客户</option>
                  <option v-for="customer in customers" :key="customer.id" :value="customer.id">{{ customer.customerName }}</option>
                </select>
                <div class="w-32 relative"><span class="absolute left-2 top-1/2 -translate-y-1/2 text-tertiary font-bold text-xs">¥</span><input v-model="override.price" type="number" step="0.01" class="w-full bg-white border border-outline-variant/20 rounded py-2 pl-6 pr-2 text-xs font-black text-tertiary" placeholder="0.00" /></div>
                <button @click="removeOverride(index)" class="p-1.5 text-on-surface-variant hover:text-error"><span class="material-symbols-outlined text-[18px]">close</span></button>
              </div>
              <div v-if="form.overrides.length === 0" class="text-center py-6 border border-dashed border-outline-variant/30 rounded-xl text-xs text-on-surface-variant/60 bg-surface-container-lowest/50">暂无客户特价，将按基准价和等级价执行。</div>
            </div>
          </section>

          <section>
            <label class="block text-xs font-bold text-on-surface-variant mb-1.5">备注</label>
            <textarea v-model.trim="form.remark" rows="3" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm resize-none" placeholder="记录本次调价原因"></textarea>
          </section>
        </div>

        <div class="p-6 border-t border-outline-variant/20 bg-surface-container-lowest flex items-center justify-between shrink-0">
          <div class="text-xs text-on-surface-variant">保存后会同步给出库金额计算使用。</div>
          <div class="flex gap-3">
            <button @click="close" class="px-5 py-2.5 text-sm font-bold text-secondary hover:bg-surface-container-high rounded-lg">取消</button>
            <button @click="submit" :disabled="submitting || !form.modelCode || !form.basePrice || !form.effectiveDate" class="px-6 py-2.5 text-sm font-bold bg-primary text-on-primary rounded-lg shadow-md disabled:opacity-50 flex items-center gap-2">
              <span v-if="submitting" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
              {{ submitting ? '保存中...' : '发布价格' }}
            </button>
          </div>
        </div>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getPriceCustomers, getPriceDetail, getPriceModels, publishPrice } from './api/price.js'

const props = defineProps({
  isVisible: { type: Boolean, default: false },
  skuData: { type: Object, default: () => null }
})
const emit = defineEmits(['close', 'success'])
const submitting = ref(false)
const customers = ref([])
const modelOptions = ref([])
const form = reactive(defaultForm())

watch(() => props.isVisible, async (visible) => {
  if (!visible) return
  resetForm()
  await Promise.all([loadCustomers(), loadModels()])
  if (props.skuData?.id) {
    const detail = await getPriceDetail(props.skuData.id)
    Object.assign(form, {
      id: detail.id,
      modelCode: detail.modelCode,
      batchNo: detail.batchNo || '',
      category: detail.category || '',
      spec: detail.spec || '',
      basePrice: detail.basePrice,
      currency: detail.currency || 'CNY',
      effectiveDate: detail.effectiveDate,
      imageUrl: detail.imageUrl || '',
      remark: detail.remark || '',
      tierPrices: normalizeTiers(detail.tierPrices),
      overrides: (detail.overrides || []).map((item) => ({ customerId: item.customerId, customerName: item.customerName, price: item.price }))
    })
  }
})

watch(() => form.modelCode, (value) => {
  const model = modelOptions.value.find((item) => item.modelCode === value)
  if (model && !form.spec) {
    form.spec = model.spec || ''
  }
})

async function loadCustomers() {
  customers.value = await getPriceCustomers()
}

async function loadModels() {
  modelOptions.value = await getPriceModels({ limit: 50 })
}

function defaultForm() {
  return {
    id: null,
    modelCode: '',
    batchNo: '',
    category: '',
    spec: '',
    basePrice: null,
    currency: 'CNY',
    effectiveDate: new Date().toISOString().slice(0, 10),
    imageUrl: '',
    remark: '',
    tierPrices: normalizeTiers(),
    overrides: []
  }
}

function normalizeTiers(rows = []) {
  const defaults = [
    { tierCode: 'T1', tierName: '战略客户', fixedPrice: null, discountRate: 90 },
    { tierCode: 'T2', tierName: '大宗采购', fixedPrice: null, discountRate: 95 },
    { tierCode: 'T3', tierName: '标准客户', fixedPrice: null, discountRate: 100 }
  ]
  return defaults.map((item) => {
    const exists = rows.find((row) => row.tierCode === item.tierCode)
    return exists ? { ...item, fixedPrice: exists.fixedPrice, discountRate: exists.discountRate } : { ...item }
  })
}

function resetForm() {
  Object.assign(form, defaultForm())
}

function close() {
  emit('close')
  resetForm()
}

function calculatedTierPrice(tier) {
  if (tier.fixedPrice) return Number(tier.fixedPrice).toFixed(2)
  if (!form.basePrice || !tier.discountRate) return '0.00'
  return (Number(form.basePrice) * Number(tier.discountRate) / 100).toFixed(2)
}

function addOverride() {
  form.overrides.push({ customerId: '', customerName: '', price: null })
}

function removeOverride(index) {
  form.overrides.splice(index, 1)
}

function syncCustomerName(override) {
  const customer = customers.value.find((item) => Number(item.id) === Number(override.customerId))
  override.customerName = customer?.customerName || ''
}

async function submit() {
  if (!form.modelCode || !form.basePrice || !form.effectiveDate) {
    ElMessage.warning('请填写型号、基准价和生效日期。')
    return
  }
  submitting.value = true
  try {
    await publishPrice({
      ...form,
      basePrice: Number(form.basePrice),
      tierPrices: form.tierPrices.map((item) => ({
        ...item,
        fixedPrice: item.fixedPrice === '' || item.fixedPrice == null ? null : Number(item.fixedPrice),
        discountRate: item.discountRate === '' || item.discountRate == null ? null : Number(item.discountRate)
      })),
      overrides: form.overrides
        .filter((item) => item.customerId && item.price)
        .map((item) => ({ ...item, customerId: Number(item.customerId), price: Number(item.price) }))
    })
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
input[type='number'] { -moz-appearance: textfield; }
</style>