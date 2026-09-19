<template>
  <section aria-label="分次处理待办" class="treatment-todos" v-loading="loading">
    <h3>分次处理待办</h3>
    <el-alert v-if="error" type="error" :closable="false" title="处理待办加载失败"><el-button @click="load">重试</el-button></el-alert>
    <el-table v-else :data="rows" empty-text="暂无分次处理待办">
      <el-table-column prop="ticketNo" label="工单号" min-width="150" />
      <el-table-column label="处理记录" min-width="180"><template #default="{ row }">第 {{ row.sequenceNo }} 次追加 · {{ treatmentTypes[row.treatmentType] }}</template></el-table-column>
      <el-table-column label="待做事项" min-width="120"><template #default="{ row }">{{ treatmentStates[row.status] }}</template></el-table-column>
      <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="$emit('open', row)">进入处理</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </section>
</template>
<script setup>
import { ref, watch } from 'vue'
import { ElAlert, ElButton, ElPagination, ElTable, ElTableColumn } from 'element-plus'
import { getTreatmentTodos } from './api/treatments'
import { treatmentTypes, treatmentStates } from './treatmentHelpers'
const props = defineProps({ refreshKey: { type: Number, default: 0 } })
defineEmits(['open'])
const rows = ref([]), page = ref(1), total = ref(0), loading = ref(false), error = ref(false)
let requestId = 0
async function load() {
  const id = ++requestId; loading.value = true; error.value = false
  try { const result = await getTreatmentTodos({ pageNum: page.value, pageSize: 10 }); if (id === requestId) { rows.value = result?.data || []; total.value = Number(result?.total || 0) } }
  catch { if (id === requestId) error.value = true }
  finally { if (id === requestId) loading.value = false }
}
watch(() => props.refreshKey, load, { immediate: true })
</script>
<style scoped>
.treatment-todos { margin-top:1.5rem; }
</style>
