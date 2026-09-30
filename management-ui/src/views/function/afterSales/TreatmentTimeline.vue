<template>
  <section class="treatments" aria-label="追加处理时间线" v-loading="loading">
    <header><h3>追加处理记录</h3><el-button v-if="canOperate && ['processing', 'closed'].includes(ticket.status)" type="primary" @click="edit(null)">新增处理记录</el-button></header>
    <el-alert :closable="false" type="info" title="每次补发、换电机和回访独立留档；质保沿用原期限。全部处理完成且最近一次回访确认解决后，再结案。" />
    <el-alert v-if="error" type="error" :closable="false" :title="error"><el-button @click="load">重新加载</el-button></el-alert>
    <el-empty v-else-if="!loading && !records.length" description="暂无追加处理，原处理信息保留在上方" />
    <article v-for="record in records" :id="`treatment-${record.id}`" :key="record.id" class="treatment" :class="{ selected: String(record.id) === String(selectedId) }" tabindex="-1">
      <header><h4>第 {{ record.sequenceNo }} 次追加 · {{ treatmentTypes[record.treatmentType] }}</h4><el-tag>{{ treatmentStates[record.status] }}</el-tag></header>
      <p class="muted">{{ record.createTime }} · {{ record.creatorName }} · 原质保起算：{{ record.originalOpeningDate || '未填写' }}</p>
      <p>{{ record.details?.description }}</p>
      <p v-if="record.details?.newMotorModel">电机：{{ record.details.oldMotorInfo || '未填写旧电机' }} → {{ record.details.newMotorModel }}，更换 {{ record.details.motorQuantity }} 台；退还 {{ record.details.returnOldMotorQuantity || 0 }} 台</p>
      <p>本次物流：{{ [record.details?.logisticsCompany, record.details?.waybillNo].filter(Boolean).join(' · ') || '未填写' }}</p>
      <p v-if="record.details?.manufacturerReturnWaybillNo">厂家返回：{{ record.details.manufacturerReturnLogisticsCompany }} · {{ record.details.manufacturerReturnWaybillNo }}</p>
      <ul v-if="record.parts?.length"><li v-for="part in record.parts" :key="part.partId">{{ part.partName }} / {{ part.modelSpec || '--' }} × {{ part.quantity }} {{ part.unit }} · {{ part.partLocation || '其他' }} · {{ part.lineStatus === 'outbound' ? '已出库' : '待出库' }}</li></ul>
      <treatment-images :model-value="record.details?.repairImages || []" readonly @preview="$emit('preview', $event)" />
      <p v-if="record.completedTime">完成：{{ record.completedTime }} · {{ record.resolution }}</p>
      <div v-if="record.followUp" class="follow-up"><b>本次回访：{{ record.followUp.resolved ? '已解决' : '未解决' }} / {{ record.followUp.satisfaction }}</b><p>{{ record.followUp.content }}</p><small>{{ record.followUpTime }} · {{ record.followUpOperatorName }}</small><treatment-images :model-value="record.followUp.followUpImages || []" readonly @preview="$emit('preview', $event)" /></div>
      <div class="actions">
        <el-button v-if="canOperate && ['waiting_outbound','processing'].includes(record.status)" :disabled="busy" @click="edit(record)">补充本次资料</el-button>
        <el-button v-if="canOutbound && record.status === 'waiting_outbound'" :disabled="busy" @click="outbound(record)">本次配件出库</el-button>
        <el-button v-if="canOperate && record.status === 'processing'" :disabled="busy" @click="complete(record)">完成本次处理</el-button>
        <el-button v-if="canOperate && record.status === 'waiting_follow_up'" :disabled="busy" @click="openFollowUp(record)">本次回访</el-button>
      </div>
    </article>
    <treatment-editor :visible="editorVisible" :ticket-id="ticket.id" :record="editing" @close="editorVisible = false" @saved="changed" @preview="$emit('preview', $event)" />
    <el-dialog v-model="followVisible" title="本次处理回访" width="min(94vw, 560px)" append-to-body :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="客户满意度" required><el-select v-model="follow.satisfaction" aria-label="客户满意度"><el-option v-for="item in ['满意','一般','不满意']" :key="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="是否解决" required><el-radio-group v-model="follow.resolved"><el-radio :value="true">已解决</el-radio><el-radio :value="false">未解决，继续处理</el-radio></el-radio-group></el-form-item>
        <el-form-item label="回访内容" required><el-input v-model="follow.content" aria-label="回访内容" type="textarea" maxlength="2000" /></el-form-item>
        <treatment-images v-model="follow.followUpImages" @preview="$emit('preview', $event)" @busy="uploading = $event" />
      </el-form>
      <template #footer><el-button :disabled="busy || uploading" @click="followVisible = false">取消</el-button><el-button type="primary" :loading="busy" :disabled="uploading" @click="saveFollowUp">保存回访</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElEmpty, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElRadio, ElRadioGroup, ElTag, ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getTreatments, advanceTreatment } from './api/treatments'
import { treatmentTypes, treatmentStates } from './treatmentHelpers'
import TreatmentEditor from './TreatmentEditor.vue'
import TreatmentImages from './TreatmentImages.vue'
const props = defineProps({ ticket: { type: Object, required: true }, selectedId: { type: [String, Number], default: '' } })
const emit = defineEmits(['changed', 'preview'])
const user = useUserStore()
const canOperate = computed(() => user.hasPermission('after_sales:update') || (user.hasPermission('after_sales:process') && user.userInfo?.userId != null && Number(props.ticket.assigneeUserId) === Number(user.userInfo.userId)))
const canOutbound = computed(() => user.hasPermission('after_sales:part:outbound'))
const records = ref([]), loading = ref(false), error = ref(''), busy = ref(false), uploading = ref(false)
const editorVisible = ref(false), editing = ref(null), followVisible = ref(false), following = ref(null)
const follow = reactive({ satisfaction: '', resolved: true, content: '', followUpImages: [] })
let requestId = 0
async function load() {
  const id = ++requestId; loading.value = true; error.value = ''
  try { const data = await getTreatments(props.ticket.id); if (id === requestId) records.value = data || [] }
  catch { if (id === requestId) error.value = '处理记录加载失败，请重试' }
  finally { if (id === requestId) loading.value = false }
  await nextTick()
  if (props.selectedId) document.getElementById(`treatment-${props.selectedId}`)?.focus()
}
watch(() => props.ticket.id, () => { records.value = []; editorVisible.value = false; followVisible.value = false; load() }, { immediate: true })
watch(() => props.selectedId, async id => { await nextTick(); document.getElementById(`treatment-${id}`)?.focus() })
function edit(record) { editing.value = record; editorVisible.value = true }
async function changed() { await load(); emit('changed') }
async function act(record, action, payload = {}) {
  if (busy.value) return false
  busy.value = true
  try { await advanceTreatment(props.ticket.id, record.id, action, { version: record.version, ...payload }); await changed(); return true }
  catch { return false }
  finally { busy.value = false }
}
async function outbound(record) {
  try { await ElMessageBox.confirm('确认本次配件出库？将扣减对应配件库存。', '本次配件出库'); await act(record, 'outbound') }
  catch { /* cancelled */ }
}
async function complete(record) {
  try {
    const { value } = await ElMessageBox.prompt('请填写本次处理结果，完成后进入待回访。', '完成本次处理', { inputType: 'textarea', inputValidator: v => Boolean(v?.trim()) || '请填写处理结果' })
    await act(record, 'complete', { resolution: value })
  } catch { /* cancelled */ }
}
function openFollowUp(record) { following.value = record; Object.assign(follow, { satisfaction: '', resolved: true, content: '', followUpImages: [] }); followVisible.value = true }
async function saveFollowUp() {
  if (uploading.value) return
  if (!follow.satisfaction || !follow.content.trim()) return ElMessage.warning('请填写满意度和回访内容')
  if (await act(following.value, 'follow-up', { followUp: follow })) followVisible.value = false
}
</script>
<style scoped>
.treatments { margin-top:1.5rem; }
header,.actions { display:flex; justify-content:space-between; align-items:center; gap:.5rem; flex-wrap:wrap; }
.treatment { margin-top:1rem; padding:1rem; border:1px solid var(--el-border-color); border-radius:var(--el-border-radius-base); overflow-wrap:anywhere; }
.treatment.selected,.treatment:focus { outline:2px solid var(--el-color-primary); }
.treatment h4 { margin:0; }
.muted { color:var(--el-text-color-secondary); font-size:.8125rem; }
.follow-up { margin:.75rem 0; padding:.75rem; background:var(--el-fill-color-light); }
.actions { justify-content:flex-start; margin-top:.75rem; }
</style>
