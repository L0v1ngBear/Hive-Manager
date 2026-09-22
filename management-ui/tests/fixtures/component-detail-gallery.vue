<template>
  <main class="hive-office-ui gallery">
    <h1>组件细节验收</h1>
    <div class="gallery-actions">
      <el-button @click="dialog = true">打开长表单</el-button>
      <el-button @click="drawer = true">打开标准抽屉</el-button>
      <el-button @click="confirm">打开确认框</el-button>
      <el-button disabled>无权限操作</el-button>
      <el-button loading>正在保存</el-button>
      <el-button link type="primary">查看详情</el-button>
      <el-button text type="primary">辅助操作</el-button>
    </div>
    <el-form label-position="top">
      <el-form-item label="日期"><DateFilterInput v-model="date" /></el-form-item>
      <el-form-item label="说明"><el-input v-model="text" type="textarea" placeholder="请输入补充说明" /></el-form-item>
    </el-form>
    <TableColumnSettings :columns="columns" @move="move" />
    <DragAttachmentUpload title="上传业务附件" helper-text="支持图片和文档" />
    <el-alert title="请核对资料后保存，必填字段会在提交时提示。" type="info" show-icon :closable="false" />
    <el-empty description="暂无符合条件的记录，可以调整筛选条件后重新查询。"><el-button>重置筛选</el-button></el-empty>
    <el-result icon="warning" title="暂时无法加载" sub-title="请检查网络连接后重试。"><template #extra><el-button>重新加载</el-button></template></el-result>

    <el-dialog v-model="dialog" title="编辑业务资料" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="客户名称" error="请输入客户完整名称"><el-input aria-label="客户名称" /></el-form-item>
        <el-form-item label="业务类型"><el-select v-model="type" aria-label="业务类型"><el-option label="普通订单" value="normal" /><el-option label="增补订单" value="extra" /></el-select></el-form-item>
        <el-form-item label="业务日期"><el-date-picker v-model="businessDate" aria-label="业务日期" /></el-form-item>
        <el-form-item label="日期范围"><el-date-picker v-model="dateRange" type="daterange" start-placeholder="起始日期" end-placeholder="结束日期" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="quantity" :min="1" /></el-form-item>
        <el-form-item label="选项"><el-checkbox-group v-model="checked"><el-checkbox label="长选项名称在较窄的窗口中也应该完整显示" value="one" /><el-checkbox label="同时通知相关人员" value="two" /></el-checkbox-group></el-form-item>
        <el-form-item label="开关"><el-switch v-model="enabled" aria-label="开启提醒" /></el-form-item>
        <el-form-item v-for="n in 12" :key="n" :label="`补充资料 ${n}`"><el-input :placeholder="`填写第 ${n} 项资料`" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="saved++">保存资料</el-button></template>
    </el-dialog>
    <el-drawer v-model="drawer" title="业务资料详情" size="520px">
      <el-alert title="资料仅供核对" :closable="false" show-icon />
      <el-descriptions :column="1" border><el-descriptions-item label="客户">某某酒店管理有限公司</el-descriptions-item><el-descriptions-item label="业务编号">SO-20260922-012345678901234567890123456789</el-descriptions-item></el-descriptions>
      <el-form label-position="top"><el-form-item v-for="n in 18" :key="n" :label="`资料 ${n}`"><el-input /></el-form-item></el-form>
      <template #footer><el-button @click="drawer = false">关闭</el-button><el-button type="primary" @click="saved++">确认资料</el-button></template>
    </el-drawer>
    <output aria-label="保存次数">{{ saved }}</output>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import DragAttachmentUpload from '../../src/components/DragAttachmentUpload.vue'
import TableColumnSettings from '../../src/components/TableColumnSettings.vue'
import DateFilterInput from '../../src/components/DateFilterInput.vue'
const dialog = ref(false), drawer = ref(false), text = ref(''), date = ref(''), businessDate = ref('')
const type = ref('normal'), quantity = ref(1), checked = ref([]), enabled = ref(true), saved = ref(0)
const dateRange = ref([])
const columns = ref(Array.from({ length: 24 }, (_, i) => ({ key: `col${i}`, label: `业务信息 ${i + 1}` })))
function move(key, direction) {
  const index = columns.value.findIndex(column => column.key === key)
  const target = index + direction
  if (target < 0 || target >= columns.value.length) return
  ;[columns.value[index], columns.value[target]] = [columns.value[target], columns.value[index]]
}
async function confirm() {
  try { await ElMessageBox.confirm('确认保存本次修改的资料？请检查客户名称和业务日期。', '保存确认', { confirmButtonText: '确认保存', cancelButtonText: '取消' }) } catch { /* cancellation is expected */ }
}
</script>

<style scoped>
.gallery { max-width: 60rem; padding: 1rem; margin: auto; }
.gallery > :not(.el-overlay) { margin-bottom: 1rem; }
.gallery-actions { display: flex; flex-wrap: wrap; gap: .5rem; }
.gallery-actions .el-button { margin-left: 0; }
</style>
