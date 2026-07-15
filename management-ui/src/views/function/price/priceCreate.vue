<template>
  <el-drawer :model-value="isVisible" :title="form.id ? '调整价格矩阵' : '新增价格矩阵'" size="640px" @update:model-value="(visible) => !visible && close()">
    <el-result v-if="requestError" :icon="requestError.icon" :title="requestError.title" :sub-title="requestError.message"><template #extra><el-button @click="retry">重试</el-button></template></el-result>
    <el-form v-else v-loading="loading" :model="form" label-position="top">
      <el-row :gutter="16"><el-col :span="12"><el-form-item label="面料型号"><el-select v-model="form.modelCode" filterable allow-create default-first-option><el-option v-for="item in modelOptions" :key="item.modelCode" :label="`${item.modelCode} ${item.spec || ''}`" :value="item.modelCode" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="批号"><el-input v-model.trim="form.batchNo" /></el-form-item></el-col></el-row>
      <el-form-item label="规格说明"><el-input v-model.trim="form.spec" /></el-form-item>
      <el-row :gutter="16"><el-col :span="8"><el-form-item label="基准价"><el-input-number v-model="form.basePrice" :min="0" :precision="2" :step="0.01" /></el-form-item></el-col><el-col :span="8"><el-form-item label="生效日期"><el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col><el-col :span="8"><el-form-item label="币种"><el-select v-model="form.currency"><el-option value="CNY" label="CNY 人民币" /><el-option value="USD" label="USD 美元" /></el-select></el-form-item></el-col></el-row>
      <el-divider content-position="left">客户等级价</el-divider><el-table :data="form.tierPrices" size="small"><el-table-column prop="tierName" label="客户等级" /><el-table-column label="一口价"><template #default="{ row }"><el-input-number v-model="row.fixedPrice" :min="0" :precision="2" :step="0.01" /></template></el-table-column><el-table-column label="折扣率"><template #default="{ row }"><el-input-number v-model="row.discountRate" :min="0" :max="100" :precision="2" :step="0.01" /></template></el-table-column><el-table-column label="预计售价"><template #default="{ row }">¥{{ calculatedTierPrice(row) }}</template></el-table-column></el-table>
      <el-divider content-position="left">指定客户特价</el-divider><el-button @click="addOverride">添加特价</el-button><el-row v-for="(override, index) in form.overrides" :key="index" :gutter="12" class="mt-3"><el-col :span="12"><el-select v-model="override.customerId" placeholder="请选择客户" @change="syncCustomerName(override)"><el-option v-for="customer in customers" :key="customer.id" :value="customer.id" :label="customer.customerName" /></el-select></el-col><el-col :span="8"><el-input-number v-model="override.price" :min="0" :precision="2" :step="0.01" /></el-col><el-col :span="4"><el-button type="danger" link @click="removeOverride(index)">删除</el-button></el-col></el-row>
      <el-form-item label="备注" class="mt-5"><el-input v-model.trim="form.remark" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="close">取消</el-button><el-tooltip :disabled="canPublish" content="缺少价格发布权限"><span><el-button type="primary" :loading="submitting" :disabled="!canPublish || loading" @click="submit">发布价格</el-button></span></el-tooltip></template>
  </el-drawer>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElButton, ElCol, ElDatePicker, ElDivider, ElDrawer, ElForm, ElFormItem, ElInput, ElInputNumber, ElOption, ElResult, ElRow, ElSelect, ElTable, ElTableColumn, ElTooltip } from 'element-plus'
import { getPriceCustomers, getPriceDetail, getPriceModels, publishPrice } from './api/price.js'
import { warnAndFocusField } from '@/utils/formFocus'
import { presentPriceOverrides } from './priceBehavior.js'

const props = defineProps({
  isVisible: { type: Boolean, default: false },
  skuData: { type: Object, default: () => null },
  canPublish: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'success'])
const submitting = ref(false)
const loading = ref(false)
const requestError = ref(null)
let detailRequestId = 0
const customers = ref([])
const modelOptions = ref([])
const form = reactive(defaultForm())

watch(() => props.isVisible, async (visible) => {
  if (!visible) { detailRequestId += 1; return }
  resetForm()
  requestError.value = null
  loading.value = true
  const requestId = ++detailRequestId
  try {
    await Promise.all([loadCustomers(), loadModels()])
    if (!props.skuData?.id) return
    const detail = await getPriceDetail(props.skuData.id)
    if (requestId !== detailRequestId) return
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
  } catch (error) {
    if (requestId === detailRequestId) requestError.value = errorState(error)
  } finally {
    if (requestId === detailRequestId) loading.value = false
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
    effectiveDate: formatLocalDate(),
    remark: '',
    tierPrices: normalizeTiers(),
    overrides: []
  }
}

function formatLocalDate(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
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
  detailRequestId += 1
  emit('close')
  resetForm()
}

function retry() {
  const visible = props.isVisible
  if (!visible) return
  requestError.value = null
  loadEditor()
}

async function loadEditor() {
  loading.value = true
  const requestId = ++detailRequestId
  try {
    await Promise.all([loadCustomers(), loadModels()])
    if (props.skuData?.id) {
      const detail = await getPriceDetail(props.skuData.id)
      if (requestId === detailRequestId) Object.assign(form, { ...detail, tierPrices: normalizeTiers(detail.tierPrices), overrides: (detail.overrides || []).map((item) => ({ ...item })) })
    }
  } catch (error) { if (requestId === detailRequestId) requestError.value = errorState(error) } finally { if (requestId === detailRequestId) loading.value = false }
}

function errorState(error) {
  const status = Number(error?.response?.status)
  if (status === 401) return { icon: 'warning', title: '登录已失效', message: '请重新登录后重试。' }
  if (status === 403) return { icon: 'warning', title: '无权访问', message: '当前账号缺少价格详情权限。' }
  if (status >= 500) return { icon: 'error', title: '服务暂时不可用', message: '服务器处理失败，请稍后重试。' }
  return { icon: 'error', title: '加载失败', message: '网络异常，请检查连接后重试。' }
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
  if (!props.canPublish || submitting.value) return
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
      overrides: presentPriceOverrides(form.overrides)
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
