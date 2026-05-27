<template>
  <div class="table-column-settings" ref="rootRef">
    <button v-if="exportable" type="button" class="column-settings-trigger column-export-trigger" @click="handleExportTable">
      <span class="material-symbols-outlined text-[18px]">file_download</span>
      导出当前页
    </button>

    <button v-if="exportAllable" type="button" class="column-settings-trigger column-export-trigger" @click="handleExportAll">
      <span class="material-symbols-outlined text-[18px]">download_for_offline</span>
      导出全部页
    </button>

    <button type="button" class="column-settings-trigger" @click="open = !open">
      <span class="material-symbols-outlined text-[18px]">view_column</span>
      列设置
    </button>

    <div v-if="open" class="column-settings-panel">
      <div class="column-settings-head">
        <div>
          <p class="column-settings-title">表格列顺序</p>
          <p class="column-settings-desc">仅保存在当前浏览器，本机刷新后仍生效。</p>
        </div>
        <button type="button" class="column-settings-reset" @click="$emit('reset')">恢复默认</button>
      </div>

      <div class="column-settings-list">
        <div v-for="(column, index) in columns" :key="column.key" class="column-settings-item">
          <span class="column-settings-index">{{ index + 1 }}</span>
          <span class="column-settings-label">{{ column.label }}</span>
          <div class="column-settings-actions">
            <button type="button" :disabled="index === 0" @click="$emit('move', column.key, -1)">
              上移
            </button>
            <button type="button" :disabled="index === columns.length - 1" @click="$emit('move', column.key, 1)">
              下移
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { exportTableElementToExcel } from '@/utils/tableExport'

const props = defineProps({
  columns: {
    type: Array,
    required: true
  },
  exportable: {
    type: Boolean,
    default: true
  },
  exportFileName: {
    type: String,
    default: ''
  },
  exportSheetName: {
    type: String,
    default: ''
  },
  exportModule: {
    type: String,
    default: ''
  },
  exportAllable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['move', 'reset', 'export-all'])

const open = ref(false)
const rootRef = ref(null)

const closeOnOutsideClick = (event) => {
  if (!rootRef.value || rootRef.value.contains(event.target)) return
  open.value = false
}

function findExportTable() {
  const root = rootRef.value
  if (!root) return null
  const containers = [
    root.closest('.responsive-table-wrap')?.parentElement,
    root.closest('section'),
    root.closest('.function-page-container'),
    root.closest('.function-page-shell'),
    document
  ].filter(Boolean)

  for (const container of containers) {
    const table = container.querySelector?.('table.responsive-data-table, table')
    if (table) return table
  }
  return null
}

async function handleExportTable() {
  try {
    await exportTableElementToExcel(findExportTable(), {
      fileName: props.exportFileName,
      sheetName: props.exportSheetName,
      sourceModule: props.exportModule
    })
    ElMessage.success('Excel 已导出')
  } catch (error) {
    if (!error?.__shown) {
      ElMessage.warning(error?.message || '导出失败，请稍后重试')
    }
  }
}

function handleExportAll() {
  emit('export-all')
}

onMounted(() => {
  document.addEventListener('click', closeOnOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeOnOutsideClick)
})
</script>

<style scoped>
.table-column-settings {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: max-content;
}

.column-settings-trigger {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  min-width: 104px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid rgba(31, 63, 95, .18);
  background: rgba(255, 255, 255, .92);
  color: #1f3f5f;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  transition: all .18s ease;
}

.column-settings-trigger:hover {
  background: #eef4fb;
  border-color: rgba(31, 63, 95, .34);
  color: #0b1f33;
}

.column-export-trigger {
  border-color: rgba(22, 101, 52, .18);
  color: #166534;
}

.column-export-trigger:hover {
  background: #ecfdf5;
  border-color: rgba(22, 101, 52, .34);
  color: #14532d;
}

.column-settings-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  z-index: 40;
  width: 340px;
  max-height: 520px;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid rgba(200, 211, 223, .82);
  background: rgba(255, 255, 255, .98);
  box-shadow: 0 24px 60px rgba(15, 23, 42, .14);
}

.column-settings-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid rgba(200, 211, 223, .58);
  background: linear-gradient(135deg, rgba(238, 244, 251, .94), rgba(255, 255, 255, .96));
}

.column-settings-title {
  font-size: 14px;
  font-weight: 900;
  color: #111827;
}

.column-settings-desc {
  margin-top: 3px;
  font-size: 11px;
  color: #7c6f5b;
}

.column-settings-reset {
  flex-shrink: 0;
  color: #1f3f5f;
  font-size: 12px;
  font-weight: 900;
}

.column-settings-list {
  max-height: 420px;
  overflow-y: auto;
  padding: 10px;
}

.column-settings-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
}

.column-settings-item:hover {
  background: rgba(255, 248, 230, .78);
}

.column-settings-index {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff7dd;
  color: #b77900;
  font-size: 11px;
  font-weight: 900;
}

.column-settings-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111827;
  font-size: 13px;
  font-weight: 800;
}

.column-settings-actions {
  display: inline-flex;
  gap: 6px;
}

.column-settings-actions button {
  border-radius: 9px;
  background: #f8fafc;
  color: #475569;
  padding: 5px 8px;
  font-size: 11px;
  font-weight: 800;
}

.column-settings-actions button:not(:disabled):hover {
  background: #fff0bf;
  color: #b77900;
}

.column-settings-actions button:disabled {
  cursor: not-allowed;
  opacity: .35;
}
</style>
