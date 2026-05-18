<template>
  <Teleport defer to="body">
    <transition name="fade">
      <div
        v-if="isVisible"
        class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm"
        @click="close"
      ></div>
    </transition>

    <transition name="slide">
      <aside
        v-if="isVisible"
        class="fixed top-0 right-0 z-[70] flex h-full w-full flex-col border-l-4 border-primary bg-white/95 shadow-[-20px_0_40px_rgba(0,32,69,0.1)] backdrop-blur-2xl sm:w-[640px]"
      >
        <div class="flex items-center justify-between border-b border-outline-variant/20 bg-white/80 p-6">
          <div>
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">account_tree</span>
              <h2 class="text-xl font-bold tracking-tight text-primary">{{ form.id ? '调整价格矩阵' : '新增价格矩阵' }}</h2>
            </div>
            <p class="mt-1 text-xs text-on-surface-variant">维护基准价、客户等级价和指定客户特价。</p>
          </div>
          <button
            class="rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container-high"
            @click="close"
          >
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div class="flex-1 space-y-8 overflow-y-auto p-6">
          <section class="space-y-4">
            <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
              <span class="h-4 w-1 rounded-full bg-primary"></span>基础信息
            </h3>
            <div class="grid grid-cols-2 gap-4 rounded-xl border border-outline-variant/20 bg-white p-4 shadow-sm">
              <div class="col-span-2">
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">
                  面料型号 <span class="text-error">*</span>
                </label>
                <input
                  v-model.trim="form.modelCode"
                  data-field="price.modelCode"
                  list="model-options"
                  class="w-full rounded-lg bg-surface-container-low px-3 py-2.5 text-sm font-bold text-primary focus:ring-2 focus:ring-primary"
                  placeholder="例如 NV-2024-OXFORD"
                />
                <datalist id="model-options">
                  <option
                    v-for="item in modelOptions"
                    :key="item.modelCode"
                    :value="item.modelCode"
                  >
                    {{ item.spec }}
                  </option>
                </datalist>
              </div>
              <div>
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">批号</label>
                <input
                  v-model.trim="form.batchNo"
                  class="w-full rounded-lg bg-surface-container-low px-3 py-2.5 text-sm focus:ring-2 focus:ring-primary"
                  placeholder="例如 #88219"
                />
              </div>
              <div class="col-span-2">
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">规格说明</label>
                <input
                  v-model.trim="form.spec"
                  class="w-full rounded-lg bg-surface-container-low px-3 py-2.5 text-sm focus:ring-2 focus:ring-primary"
                  placeholder="例如 100% 优质纯棉"
                />
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
              <span class="h-4 w-1 rounded-full bg-primary"></span>全局基准价
            </h3>
            <div class="grid grid-cols-2 gap-4 rounded-xl border border-outline-variant/20 bg-white p-4 shadow-sm">
              <div>
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">
                  基准价 <span class="text-error">*</span>
                </label>
                <div class="relative">
                  <span class="absolute top-1/2 left-3 -translate-y-1/2 font-bold text-primary">楼</span>
                  <input
                    v-model="form.basePrice"
                    data-field="price.basePrice"
                    type="number"
                    step="0.01"
                    class="w-full rounded-lg bg-surface-container-low py-2.5 pr-3 pl-7 text-sm font-black text-primary focus:ring-2 focus:ring-primary"
                    placeholder="0.00"
                  />
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">
                  生效日期 <span class="text-error">*</span>
                </label>
                <input
                  v-model="form.effectiveDate"
                  data-field="price.effectiveDate"
                  type="date"
                  class="w-full rounded-lg bg-surface-container-low px-3 py-2.5 text-sm font-bold text-primary focus:ring-2 focus:ring-primary"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">币种</label>
                <select
                  v-model="form.currency"
                  class="w-full rounded-lg bg-surface-container-low px-3 py-2.5 text-sm focus:ring-2 focus:ring-primary"
                >
                  <option value="CNY">CNY 人民币</option>
                  <option value="USD">USD 美元</option>
                </select>
              </div>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-end justify-between">
              <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
                <span class="h-4 w-1 rounded-full bg-primary"></span>客户等级价
              </h3>
              <span class="rounded bg-surface-container-high px-2 py-0.5 text-[10px] font-bold text-on-surface-variant">
                不填一口价时按折扣自动计算
              </span>
            </div>
            <div class="overflow-hidden rounded-xl border border-outline-variant/20 bg-white shadow-sm">
              <table class="w-full text-left text-xs">
                <thead class="bg-surface-container-low/50">
                  <tr class="text-on-surface-variant">
                    <th class="px-4 py-3 font-bold">客户等级</th>
                    <th class="px-4 py-3 font-bold">一口价</th>
                    <th class="px-4 py-3 font-bold">折扣率</th>
                    <th class="px-4 py-3 font-bold">预计售价</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-outline-variant/10">
                  <tr v-for="tier in form.tierPrices" :key="tier.tierCode">
                    <td class="px-4 py-3 font-bold text-primary">{{ tier.tierName }}</td>
                    <td class="px-4 py-2">
                      <input
                        v-model="tier.fixedPrice"
                        type="number"
                        step="0.01"
                        class="w-full rounded border border-outline-variant/20 bg-surface-container-lowest px-2 py-1.5"
                        placeholder="自动"
                      />
                    </td>
                    <td class="px-4 py-2">
                      <input
                        v-model="tier.discountRate"
                        type="number"
                        step="0.01"
                        class="w-20 rounded border border-outline-variant/20 bg-surface-container-lowest px-2 py-1.5"
                      />
                      %
                    </td>
                    <td class="px-4 py-2 font-black text-primary">楼{{ calculatedTierPrice(tier) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="space-y-4">
            <div class="flex items-end justify-between">
              <h3 class="flex items-center gap-2 text-sm font-bold text-tertiary">
                <span class="h-4 w-1 rounded-full bg-tertiary"></span>指定客户特价
              </h3>
              <button
                class="rounded-md bg-tertiary-fixed px-2 py-1 text-xs font-bold text-tertiary active:scale-95"
                @click="addOverride"
              >
                <span class="material-symbols-outlined align-middle text-[16px]">add</span>添加特价
              </button>
            </div>
            <div class="space-y-3">
              <div
                v-for="(override, index) in form.overrides"
                :key="index"
                class="flex items-center gap-3 rounded-lg border border-tertiary/20 bg-tertiary-fixed/10 p-3"
              >
                <select
                  v-model="override.customerId"
                  class="flex-1 rounded border border-outline-variant/20 bg-white px-2 py-2 text-xs font-bold text-primary"
                  @change="syncCustomerName(override)"
                >
                  <option value="">请选择客户</option>
                  <option v-for="customer in customers" :key="customer.id" :value="customer.id">
                    {{ customer.customerName }}
                  </option>
                </select>
                <div class="relative w-32">
                  <span class="absolute top-1/2 left-2 -translate-y-1/2 text-xs font-bold text-tertiary">楼</span>
                  <input
                    v-model="override.price"
                    type="number"
                    step="0.01"
                    class="w-full rounded border border-outline-variant/20 bg-white py-2 pr-2 pl-6 text-xs font-black text-tertiary"
                    placeholder="0.00"
                  />
                </div>
                <button
                  class="p-1.5 text-on-surface-variant hover:text-error"
                  @click="removeOverride(index)"
                >
                  <span class="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>
              <div
                v-if="form.overrides.length === 0"
                class="rounded-xl border border-dashed border-outline-variant/30 bg-surface-container-lowest/50 py-6 text-center text-xs text-on-surface-variant/60"
              >
                暂无客户特价，将按基准价和等级价执行。
              </div>
            </div>
          </section>

          <section>
            <label class="mb-1.5 block text-xs font-bold text-on-surface-variant">备注</label>
            <textarea
              v-model.trim="form.remark"
              rows="3"
              class="w-full resize-none rounded-lg bg-surface-container-low px-3 py-2.5 text-sm focus:ring-2 focus:ring-primary"
              placeholder="记录本次调价原因"
            ></textarea>
          </section>
        </div>

        <div class="flex shrink-0 items-center justify-between border-t border-outline-variant/20 bg-surface-container-lowest p-6">
          <div class="text-xs text-on-surface-variant">保存后会同步给出库金额计算使用。</div>
          <div class="flex gap-3">
            <button
              class="rounded-lg px-5 py-2.5 text-sm font-bold text-secondary hover:bg-surface-container-high"
              @click="close"
            >
              取消
            </button>
            <button
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-bold text-on-primary shadow-md disabled:opacity-50"
              @click="submit"
            >
              <span v-if="submitting" class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
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
import { warnAndFocusField } from '@/utils/formFocus'

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
      spec: detail.spec || '',
      basePrice: detail.basePrice,
      currency: detail.currency || 'CNY',
      effectiveDate: detail.effectiveDate,
      remark: detail.remark || '',
      tierPrices: normalizeTiers(detail.tierPrices),
      overrides: (detail.overrides || []).map((item) => ({
        customerId: item.customerId,
        customerName: item.customerName,
        price: item.price
      }))
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
    spec: '',
    basePrice: null,
    currency: 'CNY',
    effectiveDate: new Date().toISOString().slice(0, 10),
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
  if (!form.modelCode) {
    return warnAndFocusField('请填写面料型号。', 'price.modelCode')
  }
  if (!form.basePrice || Number(form.basePrice) <= 0) {
    return warnAndFocusField('请填写有效的基准价。', 'price.basePrice')
  }
  if (!form.effectiveDate) {
    return warnAndFocusField('请选择生效日期。', 'price.effectiveDate')
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
