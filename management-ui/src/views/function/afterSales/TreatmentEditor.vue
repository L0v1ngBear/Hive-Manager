<template>
  <el-dialog :model-value="visible" :title="record ? '编辑本次处理' : '追加处理记录'" width="min(94vw, 720px)" append-to-body :close-on-click-modal="false" @close="$emit('close')">
    <el-alert type="info" :closable="false" title="沿用原工单质保期限，不因补发或换电机重新起算；原有处理记录不会被覆盖。" />
    <el-form label-position="top" class="editor">
      <el-form-item label="本次处理方式" required>
        <el-select v-model="form.treatmentType" aria-label="本次处理方式" :disabled="partsLocked"><el-option v-for="(label, value) in treatmentTypes" :key="value" :label="label" :value="value" /></el-select>
      </el-form-item>
      <el-form-item label="本次处理说明" required><el-input v-model="form.description" aria-label="本次处理说明" type="textarea" maxlength="2000" /></el-form-item>
      <div v-if="form.treatmentType !== 'resend_parts'" class="grid">
        <el-form-item label="旧电机信息"><el-input v-model="form.oldMotorInfo" aria-label="旧电机信息" maxlength="1000" /></el-form-item>
        <el-form-item label="新电机型号" required><el-input v-model="form.newMotorModel" aria-label="新电机型号" maxlength="200" /></el-form-item>
        <el-form-item label="更换数量" required><el-input-number v-model="form.motorQuantity" aria-label="更换数量" :min="1" :max="10000" :precision="0" /></el-form-item>
        <el-form-item label="旧电机退还数量"><el-input-number v-model="form.returnOldMotorQuantity" aria-label="旧电机退还数量" :min="0" :max="10000" :precision="0" /></el-form-item>
      </div>
      <h3>本次配件明细</h3>
      <el-alert v-if="partsLocked" type="info" :closable="false" title="已进入处理，配件和处理方式不可修改；再次补发请新增处理记录。" />
      <div v-for="(line, index) in form.parts" :key="index" class="part-row">
        <el-select v-model="line.partId" :aria-label="'配件' + (index + 1)" filterable remote :remote-method="searchParts" :loading="partsLoading" :disabled="partsLocked" placeholder="搜索配件">
          <el-option v-for="part in partOptions" :key="part.id" :label="part.partName + ' / ' + (part.modelSpec || '')" :value="part.id" />
        </el-select>
        <el-input-number v-model="line.quantity" :aria-label="'配件数量' + (index + 1)" :min="1" :precision="0" :disabled="partsLocked" />
        <el-select v-model="line.partLocation" :aria-label="'配件车间' + (index + 1)" :disabled="partsLocked"><el-option v-for="location in locations" :key="location" :value="location" /></el-select>
        <el-button v-if="!partsLocked" link type="danger" @click="form.parts.splice(index, 1)">移除</el-button>
      </div>
      <el-button v-if="!partsLocked" :disabled="form.parts.length >= 50" @click="form.parts.push({ partId: null, quantity: 1, partLocation: '其他', remark: '' })">添加配件</el-button>
      <div class="grid">
        <el-form-item :label="form.treatmentType === 'motor_replacement' ? '客户寄出物流公司' : '补发物流公司'"><el-input v-model="form.logisticsCompany" aria-label="本次物流公司" maxlength="100" /></el-form-item>
        <el-form-item label="本次物流单号"><el-input v-model="form.waybillNo" aria-label="本次物流单号" maxlength="100" /></el-form-item>
        <el-form-item v-if="form.treatmentType !== 'resend_parts'" label="厂家返回物流公司"><el-input v-model="form.manufacturerReturnLogisticsCompany" maxlength="100" /></el-form-item>
        <el-form-item v-if="form.treatmentType !== 'resend_parts'" label="厂家返回物流单号"><el-input v-model="form.manufacturerReturnWaybillNo" maxlength="100" /></el-form-item>
      </div>
      <el-form-item label="本次故障反馈图片"><treatment-images v-model="form.repairImages" @preview="$emit('preview', $event)" @busy="uploading = $event" /></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="saving || uploading" @click="$emit('close')">取消</el-button><el-button type="primary" :loading="saving" :disabled="uploading" @click="save">保存本次处理</el-button></template>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption, ElMessage } from 'element-plus'
import { getAfterSalesParts } from './api/afterSales'
import { createTreatment, updateTreatment } from './api/treatments'
import { blankTreatment, treatmentTypes, validateTreatment } from './treatmentHelpers'
import TreatmentImages from './TreatmentImages.vue'
const props = defineProps({ visible: Boolean, ticketId: { type: Number, required: true }, record: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved', 'preview'])
const form = reactive(blankTreatment())
const saving = ref(false), uploading = ref(false), partsLoading = ref(false), partOptions = ref([])
const locations = ['一车间', '二车间', '三车间', '其他']
const partsLocked = computed(() => Boolean(props.record && props.record.status !== 'waiting_outbound'))
let searchId = 0
async function searchParts(keyword = '') {
  const id = ++searchId; partsLoading.value = true
  try {
    const result = await getAfterSalesParts({ pageNum: 1, pageSize: 100, keyword })
    if (id === searchId) {
      const selected = (props.record?.parts || []).map(p => ({ id: p.partId, partName: p.partName, modelSpec: p.modelSpec }))
      partOptions.value = [...new Map([...selected, ...(result?.data || []).filter(p => p.status === 1)].map(p => [p.id, p])).values()]
    }
  } catch { if (id === searchId) ElMessage.error('配件加载失败，请重新搜索') }
  finally { if (id === searchId) partsLoading.value = false }
}
watch(() => props.visible, visible => {
  if (!visible) return
  for (const key of Object.keys(form)) delete form[key]
  Object.assign(form, props.record ? JSON.parse(JSON.stringify(props.record.details)) : blankTreatment())
  if (props.record) { form.version = props.record.version; form.requestKey = props.record.requestKey }
  form.parts ||= []; form.repairImages ||= []
  searchParts()
})
async function save() {
  if (saving.value || uploading.value) return
  const error = validateTreatment(form); if (error) return ElMessage.warning(error)
  saving.value = true
  try {
    if (props.record) await updateTreatment(props.ticketId, props.record.id, form)
    else await createTreatment(props.ticketId, form)
    ElMessage.success('处理记录已保存'); emit('saved'); emit('close')
  } catch { /* The request interceptor presents the business error. Keep the same intent for retries. */ }
  finally { saving.value = false }
}
</script>
<style scoped>
.editor { margin-top:1rem; }
.grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.75rem; margin-top:1rem; }
.part-row { display:grid; grid-template-columns:minmax(0,2fr) minmax(0,1fr) minmax(0,1fr) auto; gap:.5rem; margin:.75rem 0; }
.editor :deep(.el-select), .editor :deep(.el-input-number) { width:100%; min-width:0; }
@media(max-width:600px) { .grid,.part-row { grid-template-columns:1fr; } }
</style>
